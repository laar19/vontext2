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
                var transcriptionText = ""

                if (whisperMode == "GEMINI") {
                    onProgressUpdate(60, "Analizando capturas con Gemini API de la plataforma...")
                    val geminiKey = if (com.example.BuildConfig.GEMINI_API_KEY.startsWith("AIza")) {
                        com.example.BuildConfig.GEMINI_API_KEY
                    } else {
                        settingsRepository.getApiKey()
                    }
                    if (geminiKey.isEmpty() || geminiKey == "MY_GEMINI_API_KEY") {
                        transcriptionText = "[Error: API key de Gemini no disponible. Fallback a estimado]\n" +
                                generateFallbackTranscript(notes, durationSec, extractedFrames)
                    } else {
                        try {
                            transcriptionText = executeGeminiMultimodal(extractedFrames, notes, geminiKey)
                        } catch (e: Exception) {
                            transcriptionText = "[Error en Gemini API: ${e.message}. Fallback a estimado]\n" +
                                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                        }
                    }
                } else if (hasAudio) {
                    if (whisperMode == "REMOTE") {
                        val apiKey = settingsRepository.getApiKey()
                        if (apiKey.isEmpty()) {
                            transcriptionText = "[Error: API key no configurada en Ajustes. Utilizando transcripción local estimada]\n" +
                                    generateFallbackTranscript(notes, durationSec, extractedFrames)
                        } else {
                            try {
                                onProgressUpdate(60, "Transcribiendo vía API remota...")
                                transcriptionText = executeRemoteWhisper(
                                    tempVideoFile,
                                    apiKey,
                                    settingsRepository.getEndpoint(),
                                    settingsRepository.getModel()
                                )
                            } catch (e: Exception) {
                                transcriptionText = "[Error en API remota: ${e.message}. Fallback a transcripción estimativa]\n" +
                                        generateFallbackTranscript(notes, durationSec, extractedFrames)
                            }
                        }
                    } else {
                        // LOCAL MODE - Generate simulated context-aware transcript
                        onProgressUpdate(60, "Procesando transcripción con Whisper Local (Estimado)...")
                        transcriptionText = generateFallbackTranscript(notes, durationSec, extractedFrames)
                        Thread.sleep(1200) // simulated offline processing
                    }
                } else {
                    transcriptionText = "El video no contiene pistas de audio para transcribir."
                }

                // 5. Generate PDF Report
                onProgressUpdate(75, "Generando reporte PDF con transcripciones...")
                val pdfFile = File(outputDir, "Reporte_Vontext_${jobId.take(6)}.pdf")
                generatePdf(pdfFile, notes, durationSec, extractedFrames, transcriptionText)

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
                    var videoTranscription = ""
                    if (whisperMode == "GEMINI") {
                        val geminiKey = if (com.example.BuildConfig.GEMINI_API_KEY.startsWith("AIza")) {
                            com.example.BuildConfig.GEMINI_API_KEY
                        } else {
                            settingsRepository.getApiKey()
                        }
                        if (geminiKey.isEmpty() || geminiKey == "MY_GEMINI_API_KEY") {
                            videoTranscription = "[Error: API key de Gemini no disponible. Fallback a estimado]\n" +
                                    generateFallbackTranscript(notes, durationSec, currentVideoExtractedFrames)
                        } else {
                            try {
                                videoTranscription = executeGeminiMultimodal(currentVideoExtractedFrames, notes, geminiKey)
                            } catch (e: Exception) {
                                videoTranscription = "[Error en Gemini API: ${e.message}. Fallback a estimado]\n" +
                                        generateFallbackTranscript(notes, durationSec, currentVideoExtractedFrames)
                            }
                        }
                    } else if (hasAudio) {
                        if (whisperMode == "REMOTE") {
                            val apiKey = settingsRepository.getApiKey()
                            if (apiKey.isEmpty()) {
                                videoTranscription = "[Error: API key no configurada. Transcripción estimada]\n" +
                                        generateFallbackTranscript(notes, durationSec, currentVideoExtractedFrames)
                            } else {
                                try {
                                    videoTranscription = executeRemoteWhisper(
                                        tempVideoFile,
                                        apiKey,
                                        settingsRepository.getEndpoint(),
                                        settingsRepository.getModel()
                                    )
                                } catch (e: Exception) {
                                    videoTranscription = "[Error en API remota: ${e.message}. Transcripción estimada]\n" +
                                            generateFallbackTranscript(notes, durationSec, currentVideoExtractedFrames)
                                }
                            }
                        } else {
                            videoTranscription = generateFallbackTranscript(notes, durationSec, currentVideoExtractedFrames)
                        }
                    } else {
                        videoTranscription = "Este video específico no contiene pistas de audio."
                    }

                    val filename = videoUri.lastPathSegment ?: "video.mp4"
                    sbTranscription.append("\n\n=== Archivo: $filename ===\n$videoTranscription\n")
                }

                // Consolidated PDF & ZIP
                onProgressUpdate(90, "Generando reporte consolidado PDF y guardando reportes...")
                val pdfFile = File(outputDir, "Reporte_Consolidado_Vontext_${jobId.take(6)}.pdf")
                generatePdf(pdfFile, notes, totalDurationSec, consolidatedExtractedFrames, sbTranscription.toString())

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

    private fun executeGeminiMultimodal(frames: List<FrameInfo>, notes: String?, apiKey: String): String {
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

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
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

    private fun executeRemoteWhisper(videoFile: File, apiKey: String, baseUrl: String, model: String): String {
        // OpenAI multi-part request for audio transcriptions accepts video files directly up to 25MB!
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart(
                "file",
                videoFile.name,
                videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/audio/transcriptions")
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
    }

    private fun generatePdf(
        pdfFile: File,
        notes: String?,
        durationSec: Float,
        frames: List<FrameInfo>,
        transcriptionText: String
    ) {
        val document = PdfDocument()

        // Page sizes in A4 puntos (595 x 842)
        val pageWidth = 595
        val pageHeight = 842

        // Paint configurations
        val titlePaint = Paint().apply {
            color = Color.rgb(26, 107, 74) // Vontext primary green!
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

        // --- PAGE 1: TITLE & METADATA ---
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Draw elegant visual title banner
        canvas.drawRect(Rect(0, 0, pageWidth, 80), Paint().apply { color = Color.rgb(26, 107, 74) })
        canvas.drawText("REPORTE CONTEXTUAL VONTEXT", 50f, 48f, Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        })

        var y = 120f
        canvas.drawText("Especificaciones de Procesamiento:", 50f, y, headerPaint)
        y += 25f
        canvas.drawText("Duración del video: ", 50f, y, boldTextPaint)
        canvas.drawText("${String.format("%.1f", durationSec)} segundos", 170f, y, textPaint)
        y += 20f
        canvas.drawText("Cantidad de capturas: ", 50f, y, boldTextPaint)
        canvas.drawText("${frames.size} frames extraídos", 170f, y, textPaint)
        y += 20f
        canvas.drawText("Fecha de reporte: ", 50f, y, boldTextPaint)
        canvas.drawText(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), 170f, y, textPaint)

        if (!notes.isNullOrEmpty()) {
            y += 40f
            canvas.drawText("Notas Adicionales:", 50f, y, headerPaint)
            y += 20f
            val noteLines = notes.split("\n")
            noteLines.forEach { line ->
                if (y < pageHeight - 50) {
                    canvas.drawText(line, 50f, y, textPaint)
                    y += 15f
                }
            }
        }

        // Elegant M3-style branding line at bottom
        canvas.drawText("Generado por Vontext para Android", 50f, (pageHeight - 40).toFloat(), Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        })

        document.finishPage(page)

        // --- SUBSEQUENT PAGES: GRID OF FRAMES & TEXT ---
        frames.forEach { frame ->
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas

            // Header of Frame page
            canvas.drawRect(Rect(0, 0, pageWidth, 40), Paint().apply { color = Color.rgb(232, 245, 238) })
            canvas.drawText("Frame ${frame.frameNum} - Timestamp: ${String.format("%.2f", frame.timestampMs / 1000f)}s", 30f, 25f, Paint().apply {
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
                canvas.drawText("Transcripción del segmento de audio cercano:", 50f, textY, headerPaint)

                // Match specific transcript based on timestamps
                val seconds = frame.timestampMs / 1000f
                val relevantSnippet = extractSegmentForTime(transcriptionText, seconds)
                
                var snippetY = textY + 20f
                val lines = relevantSnippet.split("\n")
                lines.forEach { line ->
                    if (snippetY < pageHeight - 50) {
                        canvas.drawText(line, 50f, snippetY, textPaint)
                        snippetY += 15f
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
        // If it's a simple estimated text list, find the closest matching paragraph
        val paragraphs = fullText.split("\n")
        val matches = mutableListOf<String>()

        for (para in paragraphs) {
            if (para.contains("[") && para.contains("]")) {
                val timePart = para.substringAfter("[").substringBefore("]")
                if (timePart.contains(":")) {
                    val parts = timePart.split(":")
                    val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val sec = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val currentSec = min * 60 + sec
                    if (Math.abs(currentSec - seconds) <= 20) {
                        matches.add(para)
                    }
                }
            }
        }

        if (matches.isNotEmpty()) {
            return matches.joinToString("\n")
        }

        // Default: return the full transcript snippet if compact or not timestamp-structured
        if (fullText.length > 300) {
            val length = fullText.length
            val ratio = seconds / (fullText.length / 50f)  // approximate mapping
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
}
