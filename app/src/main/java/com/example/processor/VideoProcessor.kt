package com.example.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.example.data.Job
import com.example.data.JobRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VideoProcessor(
    private val context: Context,
    private val jobRepository: JobRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun process(
        videoUri: Uri,
        notes: String?,
        frameInterval: Int,
        whisperModel: String,
        whisperMode: String, // "LOCAL", "REMOTE"
        onProgressUpdate: (Int, String) -> Unit
    ): String {
        val jobId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            try {
                // 1. Initial State
                onProgressUpdate(5, "Inicializando procesamiento...")
                val parentDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Vontext"
                )
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
                
                val outputDir = File(parentDir, "job_$jobId")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                // Copy video content to a reliable local file
                onProgressUpdate(10, "Copiando archivo de video...")
                val tempVideoFile = File(context.cacheDir, "temp_video_$jobId.mp4")
                context.contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(tempVideoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val videoPath = tempVideoFile.absolutePath
                val videoName = "video_${jobId.take(6)}.mp4"

                // 2. Extract Metadata
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 1000L
                val durationSec = durationMs / 1000f
                val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                val hasAudio = hasAudioStr == "yes"

                // Insert pending/analyzing job in database
                val initialJob = Job(
                    jobId = jobId,
                    status = "PROCESSING",
                    videoPath = videoPath,
                    videoFilename = videoName,
                    hasAudio = hasAudio,
                    videoDuration = durationSec,
                    additionalNotes = notes,
                    progress = 10,
                    progressMessage = "Analizando video...",
                    errorMessage = null,
                    createdAt = createdAt,
                    completedAt = null,
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                jobRepository.insertJob(initialJob)

                // 3. Frame Extraction
                onProgressUpdate(20, "Extrayendo frames del video...")
                val extractedFrames = mutableListOf<FrameInfo>()
                val stepTimesMs = mutableListOf<Long>()

                if (frameInterval == 0) {
                    // Auto Mode - extract 10 equidistant frames
                    val count = 10
                    val gap = durationMs / (count + 1)
                    for (i in 1..count) {
                        stepTimesMs.add(i * gap)
                    }
                } else {
                    // Specific Seconds interval - extract a frame every X seconds
                    val stepMs = frameInterval * 1000L
                    var currentMs = stepMs
                    while (currentMs < durationMs && extractedFrames.size < 40) {
                        stepTimesMs.add(currentMs)
                        currentMs += stepMs
                    }
                    if (stepTimesMs.isEmpty()) {
                        stepTimesMs.add(durationMs / 2) // fallback center frame
                    }
                }

                // Perform frame extraction
                stepTimesMs.forEachIndexed { index, timeMs ->
                    coroutineContext.ensureActive()
                    val progressPercent = 20 + ((index.toFloat() / stepTimesMs.size) * 30).toInt()
                    onProgressUpdate(progressPercent, "Extrayendo frame ${index + 1} de ${stepTimesMs.size}...")

                    val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeMs * 1000)
                    
                    if (bitmap != null) {
                        // Resize bitmap if very large to prevent PDF bloat & OOM
                        val resized = if (bitmap.width > 1280) {
                            val ratio = 1280f / bitmap.width
                            Bitmap.createScaledBitmap(bitmap, 1280, (bitmap.height * ratio).toInt(), true)
                        } else {
                            bitmap
                        }

                        val frameFile = File(outputDir, "frame_${String.format("%03d", index + 1)}.jpg")
                        FileOutputStream(frameFile).use { fos ->
                            resized.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        
                        extractedFrames.add(
                            FrameInfo(
                                frameNum = index + 1,
                                timestampMs = timeMs,
                                path = frameFile.absolutePath
                            )
                        )

                        if (resized != bitmap) {
                            resized.recycle()
                        }
                        bitmap.recycle()
                    }
                }
                retriever.release()

                // 4. Transcription Stage
                onProgressUpdate(55, "Iniciando transcripción y análisis...")
                val transcriptionText = performTranscription(
                    whisperMode = whisperMode,
                    tempVideoFile = tempVideoFile,
                    notes = notes,
                    durationSec = durationSec,
                    extractedFrames = extractedFrames,
                    hasAudio = hasAudio,
                    onProgressUpdate = onProgressUpdate
                )

                // 5. Generate PDF Report
                onProgressUpdate(75, "Generando reporte PDF con transcripciones...")
                val pdfFile = File(outputDir, "Reporte_Vontext_${jobId.take(6)}.pdf")
                generatePdf(pdfFile, notes, durationSec, extractedFrames, transcriptionText, whisperMode)

                // Save plain text transcription as requested by the user
                try {
                    val txtFile = File(outputDir, "transcripcion.txt")
                    txtFile.writeText(transcriptionText)
                } catch (e: Exception) {
                    // Ignore or fallback
                }

                // 6. Create ZIP Archive
                onProgressUpdate(90, "Empaquetando todo en ZIP...")
                val zipFile = File(outputDir.parent, "Vontext_${jobId.take(6)}.zip")
                createZip(outputDir, zipFile)

                // 7. Update final Job Entity in database
                onProgressUpdate(100, "¡Trabajo completado con éxito!")
                val completedJob = Job(
                    jobId = jobId,
                    status = "COMPLETED",
                    videoPath = videoPath,
                    videoFilename = tempVideoFile.name,
                    hasAudio = hasAudio,
                    videoDuration = durationSec,
                    additionalNotes = notes,
                    progress = 100,
                    progressMessage = "Guardado en Downloads/Vontext/",
                    errorMessage = null,
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = pdfFile.absolutePath,
                    zipPath = zipFile.absolutePath,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode,
                    transcriptionText = transcriptionText
                )
                jobRepository.insertJob(completedJob)

            } catch (e: kotlinx.coroutines.CancellationException) {
                onProgressUpdate(100, "Procesamiento cancelado por el usuario.")
                val cancelledJob = Job(
                    jobId = jobId,
                    status = "FAILED",
                    videoPath = "",
                    videoFilename = "",
                    hasAudio = false,
                    videoDuration = 0f,
                    additionalNotes = notes,
                    progress = 0,
                    progressMessage = "Cancelado por el usuario",
                    errorMessage = "Procesamiento cancelado por el usuario",
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                try {
                    jobRepository.insertJob(cancelledJob)
                } catch (dbEx: Exception) {}
                throw e
            } catch (e: Exception) {
                // Handle failure
                onProgressUpdate(100, "Error: ${e.message}")
                val failedJob = Job(
                    jobId = jobId,
                    status = "FAILED",
                    videoPath = "",
                    videoFilename = "",
                    hasAudio = false,
                    videoDuration = 0f,
                    additionalNotes = notes,
                    progress = 100,
                    progressMessage = "Error: ${e.localizedMessage}",
                    errorMessage = e.message,
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                jobRepository.insertJob(failedJob)
            }
        }

        return jobId
    }

    suspend fun processMultipleTogether(
        videoUris: List<Uri>,
        notes: String?,
        frameInterval: Int,
        whisperModel: String,
        whisperMode: String, // "LOCAL", "REMOTE"
        onProgressUpdate: (Int, String) -> Unit
    ): String {
        val jobId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            try {
                onProgressUpdate(5, "Inicializando procesamiento de ${videoUris.size} archivos...")
                val parentDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Vontext"
                )
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
                
                val outputDir = File(parentDir, "job_$jobId")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                // Initial DB Entry
                val initialJob = Job(
                    jobId = jobId,
                    status = "PROCESSING",
                    videoPath = "",
                    videoFilename = videoUris.joinToString(", ") { it.lastPathSegment ?: "video.mp4" },
                    hasAudio = false,
                    videoDuration = 0f,
                    additionalNotes = notes,
                    progress = 5,
                    progressMessage = "Iniciando procesamiento de múltiples videos...",
                    errorMessage = null,
                    createdAt = createdAt,
                    completedAt = null,
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                jobRepository.insertJob(initialJob)

                val consolidatedExtractedFrames = mutableListOf<FrameInfo>()
                val sbTranscription = StringBuilder()
                var totalDurationSec = 0f
                var globalFrameNum = 1
                var hasAnyAudio = false
                var firstVideoPath = ""

                videoUris.forEachIndexed { index, videoUri ->
                    coroutineContext.ensureActive()
                    val videoIndexLabel = "Video ${index + 1} de ${videoUris.size}"
                    onProgressUpdate(10 + (index * 80 / videoUris.size), "Procesando $videoIndexLabel: copiando archivo...")

                    val tempVideoFile = File(context.cacheDir, "temp_video_${jobId}_$index.mp4")
                    try {
                        context.contentResolver.openInputStream(videoUri)?.use { input ->
                            FileOutputStream(tempVideoFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        sbTranscription.append("\n\n=== Archivo: ${videoUri.lastPathSegment ?: "video.mp4"} ===\n[Error al abrir el archivo: ${e.message}]\n")
                        return@forEachIndexed
                    }

                    val videoPath = tempVideoFile.absolutePath
                    if (firstVideoPath.isEmpty()) {
                        firstVideoPath = videoPath
                    }

                    // Metadata extraction
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(videoPath)
                    } catch (e: Exception) {
                        sbTranscription.append("\n\n=== Archivo: ${videoUri.lastPathSegment ?: "video.mp4"} ===\n[Error al leer metadatos: ${e.message}]\n")
                        return@forEachIndexed
                    }
                    
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 1000L
                    val durationSec = durationMs / 1000f
                    totalDurationSec += durationSec
                    val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    val hasAudio = hasAudioStr == "yes"
                    if (hasAudio) hasAnyAudio = true

                    // Setup step times for frame extraction on this video
                    val stepTimesMs = mutableListOf<Long>()
                    if (frameInterval == 0) {
                        val count = 10
                        val gap = durationMs / (count + 1)
                        for (i in 1..count) {
                            stepTimesMs.add(i * gap)
                        }
                    } else {
                        val stepMs = frameInterval * 1000L
                        var currentMs = stepMs
                        while (currentMs < durationMs && stepTimesMs.size < 30) {
                            stepTimesMs.add(currentMs)
                            currentMs += stepMs
                        }
                        if (stepTimesMs.isEmpty()) {
                            stepTimesMs.add(durationMs / 2)
                        }
                    }

                    // Extract frames
                    val currentVideoExtractedFrames = mutableListOf<FrameInfo>()
                    stepTimesMs.forEachIndexed { frameIdx, timeMs ->
                        coroutineContext.ensureActive()
                        val bitmap = try {
                            retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.getFrameAtTime(timeMs * 1000)
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (bitmap != null) {
                            val resized = if (bitmap.width > 1280) {
                                val ratio = 1280f / bitmap.width
                                Bitmap.createScaledBitmap(bitmap, 1280, (bitmap.height * ratio).toInt(), true)
                            } else {
                                bitmap
                            }

                            val frameFile = File(outputDir, "frame_v${index + 1}_${String.format("%03d", frameIdx + 1)}.jpg")
                            FileOutputStream(frameFile).use { fos ->
                                resized.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                            }
                            
                            val fInfo = FrameInfo(
                                frameNum = globalFrameNum++,
                                timestampMs = timeMs,
                                path = frameFile.absolutePath
                            )
                            consolidatedExtractedFrames.add(fInfo)
                            currentVideoExtractedFrames.add(fInfo)

                            if (resized != bitmap) {
                                resized.recycle()
                            }
                            bitmap.recycle()
                        }
                    }
                    retriever.release()

                    // Transcription of this video
                    val videoTranscription = performTranscription(
                        whisperMode = whisperMode,
                        tempVideoFile = tempVideoFile,
                        notes = notes,
                        durationSec = durationSec,
                        extractedFrames = currentVideoExtractedFrames,
                        hasAudio = hasAudio,
                        onProgressUpdate = onProgressUpdate
                    )

                    val filename = videoUri.lastPathSegment ?: "video.mp4"
                    sbTranscription.append("\n\n=== Archivo: $filename ===\n$videoTranscription\n")
                }

                // Consolidated PDF & ZIP
                onProgressUpdate(90, "Generando reporte consolidado PDF y guardando reportes...")
                val pdfFile = File(outputDir, "Reporte_Consolidado_Vontext_${jobId.take(6)}.pdf")
                generatePdf(pdfFile, notes, totalDurationSec, consolidatedExtractedFrames, sbTranscription.toString(), whisperMode)

                // Save plain text transcription as requested by the user
                try {
                    val txtFile = File(outputDir, "transcripcion.txt")
                    txtFile.writeText(sbTranscription.toString())
                } catch (e: Exception) {
                    // Ignore or fallback
                }

                val zipFile = File(outputDir.parent, "Reporte_Consolidado_Vontext_${jobId.take(6)}.zip")
                createZip(outputDir, zipFile)

                // Update final Job Entity in database
                onProgressUpdate(100, "¡Trabajo grupal completado con éxito!")
                val completedJob = Job(
                    jobId = jobId,
                    status = "COMPLETED",
                    videoPath = firstVideoPath,
                    videoFilename = videoUris.joinToString(", ") { it.lastPathSegment ?: "video.mp4" },
                    hasAudio = hasAnyAudio,
                    videoDuration = totalDurationSec,
                    additionalNotes = notes,
                    progress = 100,
                    progressMessage = "Guardado en Downloads/Vontext/",
                    errorMessage = null,
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = pdfFile.absolutePath,
                    zipPath = zipFile.absolutePath,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode,
                    transcriptionText = sbTranscription.toString()
                )
                jobRepository.insertJob(completedJob)

            } catch (e: kotlinx.coroutines.CancellationException) {
                onProgressUpdate(100, "Procesamiento cancelado por el usuario.")
                val cancelledJob = Job(
                    jobId = jobId,
                    status = "FAILED",
                    videoPath = "",
                    videoFilename = videoUris.joinToString(", ") { it.lastPathSegment ?: "video.mp4" },
                    hasAudio = false,
                    videoDuration = 0f,
                    additionalNotes = notes,
                    progress = 0,
                    progressMessage = "Cancelado por el usuario",
                    errorMessage = "Procesamiento cancelado por el usuario",
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                try {
                    jobRepository.insertJob(cancelledJob)
                } catch (dbEx: Exception) {}
                throw e
            } catch (e: Exception) {
                onProgressUpdate(100, "Error: ${e.message}")
                val failedJob = Job(
                    jobId = jobId,
                    status = "FAILED",
                    videoPath = "",
                    videoFilename = videoUris.joinToString(", ") { it.lastPathSegment ?: "video.mp4" },
                    hasAudio = false,
                    videoDuration = 0f,
                    additionalNotes = notes,
                    progress = 100,
                    progressMessage = "Error: ${e.localizedMessage}",
                    errorMessage = e.message,
                    createdAt = createdAt,
                    completedAt = System.currentTimeMillis(),
                    pdfPath = null,
                    zipPath = null,
                    frameInterval = frameInterval,
                    whisperMode = whisperMode
                )
                jobRepository.insertJob(failedJob)
            }
        }

        return jobId
    }

    private fun performTranscription(
        whisperMode: String,
        tempVideoFile: File,
        notes: String?,
        durationSec: Float,
        extractedFrames: List<FrameInfo>,
        hasAudio: Boolean,
        onProgressUpdate: (Int, String) -> Unit
    ): String {
        val customModels = settingsRepository.getCustomModels()
        val selectedModel = customModels.find { it.id == whisperMode }

        return if (whisperMode == "LOCAL") {
            onProgressUpdate(60, "Iniciando transcripción local offline...")
            val activeLang = settingsRepository.getActiveLocalLanguage()
            var modelRootDir = File(context.filesDir, "vosk-model-$activeLang")
            var hasModel = modelRootDir.exists() && modelRootDir.isDirectory && 
                           (modelRootDir.list()?.isNotEmpty() ?: false)
            
            if (!hasModel && activeLang == "es") {
                val legacyDir = File(context.filesDir, "vosk-model")
                if (legacyDir.exists() && legacyDir.isDirectory && (legacyDir.list()?.isNotEmpty() ?: false)) {
                    modelRootDir = legacyDir
                    hasModel = true
                }
            }
            
            if (!hasModel) {
                onProgressUpdate(60, "Modelo local ($activeLang) no descargado aún. Usando estimación...")
                "**[TRANCRIPCIÓN LOCAL DE SEGURIDAD - REQUIERE DESCARGA]**\n" +
                "El modelo de voz offline para el idioma '$activeLang' no se ha detectado. Por favor ve a Ajustes y selecciona y descarga el modelo local para poder transcribir offline sin conexión.\n\n" +
                generateFallbackTranscript(notes, durationSec, extractedFrames)
            } else if (!hasAudio) {
                onProgressUpdate(60, "El video no tiene pista de audio audible.")
                "**[VIDEO SIN AUDIO DETECTADO]**\n" +
                "No se pudo extraer una pista de audio para transcribir.\n\n" +
                generateFallbackTranscript(notes, durationSec, extractedFrames)
            } else {
                try {
                    onProgressUpdate(65, "Extrayendo y combinando canales de audio...")
                    val pcmFile = File(context.cacheDir, "decoded_audio_${System.currentTimeMillis()}.pcm")
                    val decodedOk = decodeAndResampleAudio(tempVideoFile, pcmFile)
                    if (!decodedOk || !pcmFile.exists() || pcmFile.length() == 0L) {
                        onProgressUpdate(65, "No se pudo decodificar el audio a PCM.")
                        "**[ERROR EN DECODIFICACIÓN LOCAL]**\n" +
                        "No se pudo decodificar la pista de audio del archivo. Confirma si el formato es compatible o usa la opción de Whisper Remoto.\n\n" +
                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                    } else {
                        onProgressUpdate(75, "Inicializando motor de voz local ($activeLang)...")
                        val innerModelDir = modelRootDir.listFiles()?.find { it.isDirectory && File(it, "am").exists() } ?: modelRootDir
                        
                        val voskModel = org.vosk.Model(innerModelDir.absolutePath)
                        val recognizer = org.vosk.Recognizer(voskModel, 16000.0f)
                        recognizer.setWords(true)

                        onProgressUpdate(80, "Transcribiendo audio offline (Procesando ondas)...")
                        
                        val inputStream = java.io.FileInputStream(pcmFile)
                        val bStream = java.io.BufferedInputStream(inputStream)
                        val buffer = ByteArray(4096)
                        var len: Int
                        
                        val jsonResults = java.util.ArrayList<String>()
                        while (bStream.read(buffer).also { len = it } != -1) {
                            if (recognizer.acceptWaveForm(buffer, len)) {
                                jsonResults.add(recognizer.result)
                            }
                        }
                        jsonResults.add(recognizer.finalResult)
                        
                        bStream.close()
                        inputStream.close()
                        pcmFile.delete() // Cleanup raw pcm
                        
                        onProgressUpdate(90, "Formateando y estructurando marcas de tiempo...")
                        val transcript = buildTranscriptWithTimestamps(jsonResults)
                        if (transcript.trim().isEmpty()) {
                            "**[FIN DE LA TRANSCRIPCIÓN LOCAL]**\n" +
                            "El audio del video fue procesado, pero no se reconoció ninguna palabra en español o en el idioma del audio.\n\n" +
                            generateFallbackTranscript(notes, durationSec, extractedFrames)
                        } else {
                            "Transcripción local del dispositivo:\n\n$transcript"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onProgressUpdate(65, "Fallo en motor local: ${e.message}")
                    "**[FALLO EN MOTOR LOCAL DE VOZ]**\n" +
                    "Error: ${e.localizedMessage}.\n\n" +
                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                }
            }
        } else if (selectedModel != null) {
            if (selectedModel.type == "GEMINI") {
                onProgressUpdate(60, "Analizando con Gemini (${selectedModel.name})...")
                val apiKey = selectedModel.apiKey
                if (apiKey.isEmpty()) {
                    onProgressUpdate(60, "Error: API key vacía. Usando estimación...")
                    "**[ERROR DE CONFIGURACIÓN]**\nLa API Key para Gemini (${selectedModel.name}) está vacía en Ajustes.\n\n" +
                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                } else {
                    try {
                        executeGeminiMultimodal(extractedFrames, notes, apiKey, selectedModel.endpoint, selectedModel.modelName)
                    } catch (e: Exception) {
                        onProgressUpdate(60, "Error en Gemini API: ${e.message}")
                        "**[ERROR EN LLAMADA GEMINI API - ${selectedModel.name}]**\n" +
                        "La llamada no se completó debido al siguiente error: ${e.localizedMessage}. " +
                        "Por favor verifica la API Key, el endpoint o si el modelo '${selectedModel.modelName}' es soportado.\n\n" +
                        "Marcas de tiempo de navegación estimadas sugeridas:\n" +
                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                    }
                }
            } else {
                if (hasAudio) {
                    val apiKey = selectedModel.apiKey
                    if (apiKey.isEmpty()) {
                        onProgressUpdate(60, "Error: API key vacía. Usando de estimación...")
                        "**[ERROR DE CONFIGURACIÓN]**\nLa API Key para Whisper (${selectedModel.name}) está vacía en Ajustes.\n\n" +
                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                    } else {
                        try {
                            onProgressUpdate(60, "Transcribiendo vía API remota (${selectedModel.name})...")
                            executeRemoteWhisper(
                                tempVideoFile,
                                apiKey,
                                selectedModel.endpoint,
                                selectedModel.modelName
                            )
                        } catch (e: Exception) {
                            onProgressUpdate(60, "Error en API Whisper: ${e.message}")
                            "**[ERROR EN TRANSCRIPCIÓN WHISPER REMOTE - ${selectedModel.name}]**\n" +
                            "Error: ${e.localizedMessage}. Confirma si tu API Key, endpoint de red '${selectedModel.endpoint}' o el nombre del modelo '${selectedModel.modelName}' son correctos.\n\n" +
                            "Marcas de tiempo de navegación estimadas sugeridas:\n" +
                            generateFallbackTranscript(notes, durationSec, extractedFrames)
                        }
                    }
                } else {
                    onProgressUpdate(60, "Video sin audio para Whisper Remoto. Generando estimación...")
                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                }
            }
        } else {
            // Fallbacks for standard/legacy values
            if (whisperMode == "GEMINI") {
                onProgressUpdate(60, "Analizando capturas con Gemini API...")
                val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY
                val geminiKey = if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.length > 10) buildConfigKey else settingsRepository.getApiKey()
                if (geminiKey.isEmpty() || geminiKey == "MY_GEMINI_API_KEY") {
                    onProgressUpdate(60, "API key no disponible. Usando estimación...")
                    "**[ERROR DE CONFIGURACIÓN]**\nNo se ha configurado la API Key de Gemini en los ajustes o BuildConfig.\n\n" +
                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                } else {
                    try {
                        executeGeminiMultimodal(extractedFrames, notes, geminiKey, "", "gemini-1.5-flash")
                    } catch (e: Exception) {
                        onProgressUpdate(60, "Error en Gemini API: ${e.message}")
                        "**[ERROR EN GEMINI API PLATA-FORMA]**\n" +
                        "Error: ${e.localizedMessage}.\n\n" +
                        "Marcas de tiempo de navegación estimadas sugeridas:\n" +
                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                    }
                }
            } else if (whisperMode == "REMOTE" && hasAudio) {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isEmpty()) {
                    onProgressUpdate(60, "API key no configurada. Usando estimación...")
                    "**[ERROR DE CONFIGURACIÓN]**\nNo se ha configurado la API Key de Whisper estándar de Ajustes.\n\n" +
                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                } else {
                    try {
                        onProgressUpdate(60, "Transcribiendo vía API remota...")
                        executeRemoteWhisper(
                            tempVideoFile,
                            apiKey,
                            settingsRepository.getEndpoint(),
                            settingsRepository.getModel()
                        )
                    } catch (e: Exception) {
                        onProgressUpdate(60, "Error en API remota: ${e.message}")
                        "**[ERROR EN WHISPER REMOTO ESTÁNDAR]**\n" +
                        "Error: ${e.localizedMessage}.\n\n" +
                        "Marcas de tiempo de navegación estimadas sugeridas:\n" +
                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                    }
                }
            } else {
                generateFallbackTranscript(notes, durationSec, extractedFrames)
            }
        }
    }

    private fun generateFallbackTranscript(notes: String?, durationSec: Float, frames: List<FrameInfo>): String {
        val sb = java.lang.StringBuilder()
        sb.append("[00:00] [Inicio] Iniciando grabación de la pantalla.\n")
        
        frames.forEach { frame ->
            val totalSec = frame.timestampMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val timeStr = String.format("%02d:%02d", min, sec)
            sb.append("[$timeStr] Captura del Frame ${frame.frameNum} - Demostración de actividad y navegación en el dispositivo.\n")
        }

        val footerMin = durationSec.toInt() / 60
        val footerSec = durationSec.toInt() % 60
        val footerTime = String.format("%02d:%02d", footerMin, footerSec)
        sb.append("[$footerTime] [Fin] Finalización del proceso de captura de pantalla.")

        return sb.toString()
    }

    private fun getCompressedBase64Frame(path: String): String {
        val bitmap = BitmapFactory.decodeFile(path) ?: return ""
        val maxDim = 512
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        val baos = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val byteArray = baos.toByteArray()
        if (scaled != bitmap) {
            scaled.recycle()
        }
        bitmap.recycle()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    private fun executeGeminiMultimodal(
        frames: List<FrameInfo>,
        notes: String?,
        apiKey: String,
        endpoint: String,
        modelName: String
    ): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val jsonRequest = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        val systemPrompt = "Eres un asistente técnico experto en análisis de pantallas de dispositivos móviles/web. " +
                "Se te proporciona una secuencia cronológica de capturas de pantalla de un video (${frames.size} capturas en total) y notas adicionales del usuario: '${notes ?: "Ninguna"}'. " +
                "Tu objetivo es generar el reporte de transcripción contextual en español más exacto y descriptivo posible. " +
                "Genera un reporte líneal de navegación de actividad en formato de línea de tiempo con marcas de tiempo en formato [mm:ss] que correspondan exactamente a los tiempos de las capturas que se te pasan. " +
                "Para cada tiempo, escribe exactamente una frase/oración muy concisa (menos de 100 caracteres) que describa la interfaz mostrada, la acción técnica detectada en la imagen o el paso del usuario. " +
                "Usa únicamente formato del estilo: '[00:05] Se muestra el menú principal y el formulario de inicio.' " +
                "Sé sumamente profesional e inteligente basándote en la información visual real de las capturas de pantalla, evitando frases genéricas."

        partsArray.put(JSONObject().apply { put("text", systemPrompt) })

        frames.forEach { frame ->
            val totalSec = frame.timestampMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val timeStr = String.format("%02d:%02d", min, sec)

            val base64Data = getCompressedBase64Frame(frame.path)
            if (base64Data.isNotEmpty()) {
                partsArray.put(JSONObject().apply {
                    put("text", "Captura del Frame ${frame.frameNum} en el tiempo [$timeStr]:")
                })
                partsArray.put(JSONObject().apply {
                    val inlineDataObj = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    }
                    put("inlineData", inlineDataObj)
                })
            }
        }

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        jsonRequest.put("contents", contentsArray)

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val finalModel = if (modelName.isNotBlank()) modelName else "gemini-1.5-flash"
        val requestUrl = if (endpoint.isNotBlank() && endpoint != "https://api.openai.com/v1" && endpoint != "https://generativelanguage.googleapis.com") {
            if (endpoint.contains("models/")) {
                "$endpoint?key=$apiKey"
            } else {
                val base = endpoint.removeSuffix("/")
                "$base/v1beta/models/$finalModel:generateContent?key=$apiKey"
            }
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$finalModel:generateContent?key=$apiKey"
        }

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw Exception("HTTP ${response.code}: $errBody")
            }
            val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía de Gemini")
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            return firstPart?.optString("text") ?: throw Exception("No se encontró texto en la respuesta de Gemini")
        }
    }

    private fun extractAudioTrack(videoFile: File, outputFile: File): Boolean {
        val extractor = android.media.MediaExtractor()
        var muxer: android.media.MediaMuxer? = null
        try {
            extractor.setDataSource(videoFile.absolutePath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    extractor.selectTrack(i)
                    break
                }
            }
            if (audioTrackIndex == -1) {
                return false
            }
            val format = extractor.getTrackFormat(audioTrackIndex)
            muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeTrackIndex = muxer.addTrack(format)
            muxer.start()

            val maxBufferSize = if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                64 * 1024
            }
            val buffer = java.nio.ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
        }
    }

    private fun executeRemoteWhisper(videoFile: File, apiKey: String, baseUrl: String, model: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()

        // Extract audio track to make upload 100x faster and bullet-proof
        val tempAudioFile = File(videoFile.parent, "extracted_audio_${System.currentTimeMillis()}.m4a")
        val extractSuccess = extractAudioTrack(videoFile, tempAudioFile)
        val fileToUpload = if (extractSuccess && tempAudioFile.exists() && tempAudioFile.length() > 0) {
            tempAudioFile
        } else {
            // fallback to original video file if extraction fails
            videoFile
        }

        val mediaType = if (fileToUpload == tempAudioFile) {
            "audio/m4a".toMediaTypeOrNull()
        } else {
            "video/mp4".toMediaTypeOrNull()
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart(
                    "file",
                    fileToUpload.name,
                    fileToUpload.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(if (baseUrl.endsWith("/")) "${baseUrl}audio/transcriptions" else "$baseUrl/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    throw Exception("HTTP ${response.code}: $errorBody")
                }
                val jsonStr = response.body?.string() ?: throw Exception("Respuesta de API vacía")
                val json = JSONObject(jsonStr)
                return json.optString("text", "")
            }
        } finally {
            // Clean up temp audio file
            if (tempAudioFile.exists()) {
                tempAudioFile.delete()
            }
        }
    }

    private fun generatePdf(
        pdfFile: File,
        notes: String?,
        durationSec: Float,
        frames: List<FrameInfo>,
        transcriptionText: String,
        whisperMode: String
    ) {
        val document = PdfDocument()

        // Page sizes in A4 puntos (595 x 842)
        val pageWidth = 595
        val pageHeight = 842

        // Paint configurations
        val titlePaint = Paint().apply {
            color = Color.rgb(30, 82, 179) // Vontext primary blue!
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(44, 62, 80)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val includeTimestamps = settingsRepository.getIncludeTimestamps()

        // --- PAGES: GRID OF FRAMES & TEXT ---
        var pageNum = 1
        frames.forEach { frame ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header of Frame page
            canvas.drawRect(Rect(0, 0, pageWidth, 40), Paint().apply { color = Color.rgb(232, 245, 238) })
            
            val frameHeader = if (includeTimestamps) {
                "Frame ${frame.frameNum} - Timestamp: ${String.format("%.2f", frame.timestampMs / 1000f)}s"
            } else {
                "Frame ${frame.frameNum}"
            }
            canvas.drawText(frameHeader, 30f, 25f, Paint().apply {
                color = Color.rgb(13, 59, 40)
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            })

            // Load frame image and draw centered
            val bitmap = BitmapFactory.decodeFile(frame.path)
            if (bitmap != null) {
                // Calculate size to preserve aspect ratio within 450x450 box
                val maxImgWidth = 450f
                val maxImgHeight = 450f
                val scale = Math.min(maxImgWidth / bitmap.width, maxImgHeight / bitmap.height)
                val w = (bitmap.width * scale).toInt()
                val h = (bitmap.height * scale).toInt()

                val startX = (pageWidth - w) / 2
                val startY = 80

                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = Rect(startX, startY, startX + w, startY + h)
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
                bitmap.recycle()

                // Draw transcription context block below image
                val textY = (startY + h + 40).toFloat()
                
                // If this is a custom model ID, resolve actual type for header
                val customModels = settingsRepository.getCustomModels()
                val matched = customModels.find { it.id == whisperMode }
                val isGemini = whisperMode == "GEMINI" || (matched != null && matched.type == "GEMINI")
                val isLocal = whisperMode == "LOCAL"

                val blockTitle = when {
                    isGemini -> "Transcripción y análisis de pantalla por IA (Gemini):"
                    isLocal -> "Transcripción del audio del dispositivo (Whisper Local):"
                    else -> "Transcripción del audio del dispositivo (Whisper Remoto):"
                }
                canvas.drawText(blockTitle, 50f, textY, headerPaint)

                // Match specific transcript based on timestamps
                val seconds = frame.timestampMs / 1000f
                val relevantSnippet = extractSegmentForTime(transcriptionText, seconds)
                
                var snippetY = textY + 20f
                val lines = relevantSnippet.split("\n")
                lines.forEach { line ->
                    if (snippetY < pageHeight - 50) {
                        val cleanLine = if (!includeTimestamps) {
                            line.replace(Regex("^\\[\\d{2}:\\d{2}\\]\\s*"), "")
                                .replace(Regex("^\\[\\d{2}:\\d{2}:\\d{2}\\]\\s*"), "")
                        } else {
                            line
                        }
                        // Skip printing if it's empty after stripping
                        if (cleanLine.trim().isNotEmpty()) {
                            canvas.drawText(cleanLine, 50f, snippetY, textPaint)
                            snippetY += 15f
                        }
                    }
                }
            }

            document.finishPage(page)
        }

        // Write document to file
        FileOutputStream(pdfFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun extractSegmentForTime(fullText: String, seconds: Float): String {
        val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ""

        // Try to find the closest line that has a timestamp
        var closestLine: String? = null
        var minDiff = Float.MAX_VALUE

        val matchedLines = mutableListOf<String>()

        for (line in lines) {
            if (line.contains("[") && line.contains("]")) {
                val timePart = line.substringAfter("[").substringBefore("]")
                if (timePart.contains(":")) {
                    val parts = timePart.split(":")
                    val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val sec = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val lineSec = min * 60 + sec
                    val diff = Math.abs(lineSec - seconds)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestLine = line
                    }
                    // If we want very close match (3 seconds)
                    if (diff <= 3) {
                        matchedLines.add(line)
                    }
                }
            }
        }

        if (matchedLines.isNotEmpty()) {
            return matchedLines.joinToString("\n")
        }
        if (closestLine != null) {
            return closestLine
        }

        // Fallback for non-timestamp paragraphs
        if (fullText.length > 300) {
            val length = fullText.length
            val ratio = seconds / (fullText.length / 50f)
            val start = Math.max(0, (ratio * 150).toInt())
            val end = Math.min(length, start + 250)
            return "..." + fullText.substring(start, end) + "..."
        }
        return fullText
    }

    private fun createZip(srcFolder: File, destZipFile: File) {
        ZipOutputStream(FileOutputStream(destZipFile)).use { zos ->
            srcFolder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val zipEntryPath = if (file.name.endsWith(".jpg", ignoreCase = true)) {
                        "capturas/${file.name}"
                    } else {
                        file.name
                    }
                    val zipEntry = ZipEntry(zipEntryPath)
                    zos.putNextEntry(zipEntry)
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun decodeAndResampleAudio(videoFile: File, pcmOutputFile: File): Boolean {
        val extractor = android.media.MediaExtractor()
        try {
            extractor.setDataSource(videoFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        var trackIndex = -1
        var format: android.media.MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }

        if (trackIndex == -1 || format == null) {
            extractor.release()
            return false
        }

        extractor.selectTrack(trackIndex)

        val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
        var decoder: android.media.MediaCodec? = null
        var outBuffer: java.io.BufferedOutputStream? = null
        var outStream: java.io.FileOutputStream? = null
        try {
            decoder = android.media.MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            outStream = java.io.FileOutputStream(pcmOutputFile)
            outBuffer = java.io.BufferedOutputStream(outStream)

            val srcSampleRate = if (format.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44100
            }
            val srcChannels = if (format.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }

            val info = android.media.MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                if (!isEOS) {
                    val inIndex = decoder.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)
                        if (buffer != null) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                if (outIndex >= 0) {
                    val buffer = decoder.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.get(chunk)
                        buffer.clear()

                        val shortsCount = chunk.size / 2
                        val shorts = ShortArray(shortsCount)
                        val byteBuf = java.nio.ByteBuffer.wrap(chunk).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                        for (s in 0 until shortsCount) {
                            if (byteBuf.remaining() >= 2) {
                                shorts[s] = byteBuf.getShort()
                            }
                        }

                        val monoShorts = if (srcChannels == 2) {
                            val mCount = shorts.size / 2
                            val mono = ShortArray(mCount)
                            for (m in 0 until mCount) {
                                val left = shorts[m * 2].toInt()
                                val right = shorts[m * 2 + 1].toInt()
                                mono[m] = ((left + right) / 2).toShort()
                            }
                            mono
                        } else {
                            shorts
                        }

                        if (srcSampleRate == 16000) {
                            for (sample in monoShorts) {
                                outBuffer.write(sample.toInt() and 0xFF)
                                outBuffer.write((sample.toInt() shr 8) and 0xFF)
                            }
                        } else {
                            val ratio = srcSampleRate.toDouble() / 16000.0
                            var srcIndex = 0.0
                            while (srcIndex < monoShorts.size) {
                                val idx = srcIndex.toInt()
                                if (idx < monoShorts.size) {
                                    val sample = monoShorts[idx]
                                    outBuffer.write(sample.toInt() and 0xFF)
                                    outBuffer.write((sample.toInt() shr 8) and 0xFF)
                                }
                                srcIndex += ratio
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                outBuffer?.flush()
                outBuffer?.close()
                outStream?.close()
            } catch (e: Exception) {}
            try {
                decoder?.stop()
                decoder?.release()
            } catch (e: Exception) {}
            try {
                extractor.release()
            } catch (e: Exception) {}
        }
    }

    private fun buildTranscriptWithTimestamps(jsonResultList: List<String>): String {
        val sb = java.lang.StringBuilder()
        for (jsonStr in jsonResultList) {
            try {
                val json = JSONObject(jsonStr)
                val text = json.optString("text", "")
                if (text.isEmpty()) continue

                val resultArray = json.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    val firstWord = resultArray.getJSONObject(0)
                    val startTime = firstWord.optDouble("start", 0.0)
                    val minutes = (startTime / 60).toInt()
                    val seconds = (startTime % 60).toInt()
                    val timeStr = String.format("[%02d:%02d]", minutes, seconds)
                    sb.append(timeStr).append(" ").append(text).append("\n")
                } else {
                    sb.append(text).append("\n")
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        return sb.toString()
    }
}
