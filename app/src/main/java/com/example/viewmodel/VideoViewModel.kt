package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Job
import com.example.data.JobRepository
import com.example.data.SettingsRepository
import com.example.processor.VideoProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val jobRepository = JobRepository(database.jobDao())
    val settingsRepository = SettingsRepository(application)
    private val videoProcessor = VideoProcessor(application, jobRepository, settingsRepository)
    private val appCtx = getApplication<Application>()

    // Reactive StateFlow for jobs list
    val allJobs: StateFlow<List<Job>> = jobRepository.allJobs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current screen configuration state
    var selectedVideoUris by mutableStateOf<List<Uri>>(emptyList())
    val selectedVideoUri: Uri?
        get() = selectedVideoUris.firstOrNull()

    var multipleFilesMode by mutableStateOf("SEPARADOS") // "JUNTOS" or "SEPARADOS"

    var additionalNotes by mutableStateOf("")
    var frameInterval by mutableStateOf(5) // default 5 seconds
    
    var whisperMode by mutableStateOf(settingsRepository.getSelectedModelId())
        private set

    data class VoskModelInfo(
        val code: String,
        val displayName: String,
        val size: String,
        val url: String
    )

    val availableVoskModels = listOf(
        VoskModelInfo("es", "Español", "39 MB", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.22.zip"),
        VoskModelInfo("en", "English", "40 MB", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"),
        VoskModelInfo("pt", "Português", "31 MB", "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"),
        VoskModelInfo("fr", "Français", "39 MB", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"),
        VoskModelInfo("de", "Deutsch", "45 MB", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip"),
        VoskModelInfo("it", "Italiano", "45 MB", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip")
    )

    var activeVoskLanguage by mutableStateOf(settingsRepository.getActiveLocalLanguage())
        private set

    var downloadedVoskLanguages by mutableStateOf<Set<String>>(emptySet())
        private set

    var isDarkTheme by mutableStateOf(settingsRepository.getDarkThemeEnabled())
        private set
    var appLanguage by mutableStateOf(settingsRepository.getLanguage())
        private set

    // Local Whisper model state
    var isWhisperLocalDownloaded by mutableStateOf(settingsRepository.isWhisperLocalDownloaded())
        private set
    var isDownloadingLocalWhisper by mutableStateOf(false)
        private set
    var downloadingLocalWhisperLangCode by mutableStateOf("")
        private set
    var localWhisperDownloadProgress by mutableStateOf(0f)
        private set
    var localWhisperDownloadStatus by mutableStateOf("")
        private set

    init {
        try {
            val oldModel = java.io.File(application.filesDir, "vosk-model")
            val esModel = java.io.File(application.filesDir, "vosk-model-es")
            if (oldModel.exists() && oldModel.isDirectory && !esModel.exists()) {
                oldModel.renameTo(esModel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        refreshDownloadedVoskLanguages()

        val stored = settingsRepository.getSelectedModelId()
        if (stored == "GEMINI") {
            val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY
            val isBuildConfigValid = buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.length > 10
            val isConfigured = isBuildConfigValid || settingsRepository.getApiKey().isNotEmpty()
            if (!isConfigured) {
                updateWhisperMode("LOCAL")
            }
        } else if (stored == "REMOTE") {
            if (settingsRepository.getApiKey().isEmpty()) {
                updateWhisperMode("LOCAL")
            }
        }
    }

    fun selectActiveVoskLanguage(code: String) {
        settingsRepository.setActiveLocalLanguage(code)
        activeVoskLanguage = code
        refreshDownloadedVoskLanguages()
    }

    fun refreshDownloadedVoskLanguages() {
        val app = appCtx
        val set = mutableSetOf<String>()
        for (m in availableVoskModels) {
            val targetDir = java.io.File(app.filesDir, "vosk-model-${m.code}")
            val hasModel = targetDir.exists() && targetDir.isDirectory && 
                           (targetDir.list()?.isNotEmpty() ?: false)
            if (hasModel) {
                set.add(m.code)
            }
        }
        if (set.isNotEmpty()) {
            settingsRepository.setWhisperLocalDownloaded(true)
            isWhisperLocalDownloaded = true
        } else {
            val legacyDir = java.io.File(app.filesDir, "vosk-model")
            val hasLegacy = legacyDir.exists() && legacyDir.isDirectory && 
                             (legacyDir.list()?.isNotEmpty() ?: false)
            if (hasLegacy) {
                set.add("es")
                settingsRepository.setWhisperLocalDownloaded(true)
                isWhisperLocalDownloaded = true
            } else {
                settingsRepository.setWhisperLocalDownloaded(false)
                isWhisperLocalDownloaded = false
            }
        }
        downloadedVoskLanguages = set
    }

    fun updateWhisperMode(value: String) {
        settingsRepository.setSelectedModelId(value)
        whisperMode = value
    }

    var customModelsList by mutableStateOf(settingsRepository.getCustomModels())
        private set

    var includeTimestamps by mutableStateOf(settingsRepository.getIncludeTimestamps())
        private set

    fun updateIncludeTimestamps(newValue: Boolean) {
        settingsRepository.setIncludeTimestamps(newValue)
        includeTimestamps = newValue
    }

    fun loadCustomModels() {
        customModelsList = settingsRepository.getCustomModels()
    }

    fun addCustomModel(configName: String, type: String, apiKey: String, endpoint: String, modelName: String) {
        val model = com.example.data.CustomModel(
            id = java.util.UUID.randomUUID().toString(),
            name = configName,
            type = type,
            apiKey = apiKey,
            endpoint = endpoint,
            modelName = modelName
        )
        settingsRepository.addCustomModel(model)
        loadCustomModels()
        updateWhisperMode(model.id)
    }

    fun deleteCustomModel(id: String) {
        settingsRepository.deleteCustomModel(id)
        loadCustomModels()
        if (whisperMode == id) {
            updateWhisperMode("LOCAL")
        }
    }

    fun downloadLocalWhisper(langCode: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (isDownloadingLocalWhisper) return
        val targetModel = availableVoskModels.find { it.code == langCode } ?: return
        isDownloadingLocalWhisper = true
        downloadingLocalWhisperLangCode = langCode
        localWhisperDownloadProgress = 0f
        localWhisperDownloadStatus = "Conectando al servidor..."
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(targetModel.url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw java.io.IOException("Error del servidor: ${response.code}")
                }
                
                val body = response.body ?: throw java.io.IOException("Respuesta vacía del servidor")
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                
                val destFile = java.io.File(appCtx.cacheDir, "vosk_model_${langCode}_temp.zip")
                val outputStream = java.io.FileOutputStream(destFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            localWhisperDownloadProgress = progress * 0.9f // reserve 10% for extraction
                            localWhisperDownloadStatus = "Descargando modelo ${targetModel.displayName} (${(totalBytesRead / (1024 * 1024))} MB / ${(contentLength / (1024 * 1024))} MB)..."
                        }
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                // Now extract zip
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    localWhisperDownloadProgress = 0.92f
                    localWhisperDownloadStatus = "Descomprimiendo archivos..."
                }
                
                val extractDir = java.io.File(appCtx.filesDir, "vosk-model-$langCode")
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()
                
                unzip(destFile, extractDir)
                destFile.delete() // Cleanup temporary zip
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    refreshDownloadedVoskLanguages()
                    isDownloadingLocalWhisper = false
                    downloadingLocalWhisperLangCode = ""
                    localWhisperDownloadProgress = 1.0f
                    localWhisperDownloadStatus = "Listo para usar sin conexión (Offline)"
                    onSuccess("Modelo de transcripción local (${targetModel.displayName}) descargado e instalado con éxito.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isDownloadingLocalWhisper = false
                    downloadingLocalWhisperLangCode = ""
                    localWhisperDownloadProgress = 0f
                    localWhisperDownloadStatus = "Error en descarga: ${e.localizedMessage}"
                    onError("Fallo en descarga: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun unzip(zipFile: java.io.File, targetDir: java.io.File) {
        val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            val newFile = java.io.File(targetDir, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                val fos = java.io.FileOutputStream(newFile)
                val buffer = ByteArray(4096)
                var len: Int
                while (zipInputStream.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
            }
            zipInputStream.closeEntry()
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }

    fun deleteLocalWhisper(langCode: String, onDelete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val extractDir = java.io.File(appCtx.filesDir, "vosk-model-$langCode")
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                if (langCode == "es") {
                    val legacyDir = java.io.File(appCtx.filesDir, "vosk-model")
                    if (legacyDir.exists()) {
                        legacyDir.deleteRecursively()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                refreshDownloadedVoskLanguages()
                localWhisperDownloadProgress = 0f
                localWhisperDownloadStatus = ""
                onDelete()
            }
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        settingsRepository.setDarkThemeEnabled(enabled)
        isDarkTheme = enabled
    }

    fun updateLanguage(lang: String) {
        settingsRepository.setLanguage(lang)
        appLanguage = lang
    }

    // Processing status state
    var isProcessing by mutableStateOf(false)
        private set
    var currentProgress by mutableStateOf(0)
        private set
    var currentStatusMessage by mutableStateOf("")
        private set
    var logs = mutableStateOf<List<String>>(emptyList())
        private set

    init {
        // Init default configurations
        frameInterval = settingsRepository.getDefaultInterval()
    }

    fun selectVideos(uris: List<Uri>) {
        selectedVideoUris = uris
        if (uris.isNotEmpty()) {
            addLog("Videos seleccionados (${uris.size}): ${uris.joinToString { it.lastPathSegment ?: "video.mp4" }}")
        }
    }

    fun selectVideo(uri: Uri?) {
        selectedVideoUris = if (uri != null) listOf(uri) else emptyList()
        if (uri != null) {
            addLog("Video seleccionado: ${uri.lastPathSegment ?: "video.mp4"}")
        }
    }

    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs.value = logs.value + "[$timestamp] $msg"
    }

    private var activeProcessingJob: kotlinx.coroutines.Job? = null

    fun cancelProcessing() {
        activeProcessingJob?.cancel()
        isProcessing = false
        currentProgress = 0
        currentStatusMessage = "Procesamiento cancelado."
        addLog("Cancelado por el usuario.")
    }

    fun startProcessing(onComplete: (String) -> Unit) {
        if (selectedVideoUris.isEmpty()) return
        isProcessing = true
        currentProgress = 0
        currentStatusMessage = "Iniciando..."
        logs.value = emptyList()
        addLog("Iniciando proceso de Vontext...")

        activeProcessingJob = viewModelScope.launch {
            try {
                val model = settingsRepository.getModel()
                if (selectedVideoUris.size == 1) {
                    val uri = selectedVideoUris.first()
                    val jobId = videoProcessor.process(
                        videoUri = uri,
                        notes = additionalNotes,
                        frameInterval = frameInterval,
                        whisperModel = model,
                        whisperMode = whisperMode
                    ) { progress, message ->
                        currentProgress = progress
                        currentStatusMessage = message
                        addLog(message)
                    }
                    selectedVideoUris = emptyList()
                    additionalNotes = ""
                    onComplete(jobId)
                } else if (multipleFilesMode == "JUNTOS") {
                    val jobId = videoProcessor.processMultipleTogether(
                        videoUris = selectedVideoUris,
                        notes = additionalNotes,
                        frameInterval = frameInterval,
                        whisperModel = model,
                        whisperMode = whisperMode
                    ) { progress, message ->
                        currentProgress = progress
                        currentStatusMessage = message
                        addLog(message)
                    }
                    selectedVideoUris = emptyList()
                    additionalNotes = ""
                    onComplete(jobId)
                } else {
                    val totalVideos = selectedVideoUris.size
                    var lastCompletedJobId = ""
                    selectedVideoUris.forEachIndexed { index, uri ->
                        addLog("Iniciando procesamiento de archivo ${index + 1} de $totalVideos...")
                        val jobId = videoProcessor.process(
                            videoUri = uri,
                            notes = additionalNotes,
                            frameInterval = frameInterval,
                            whisperModel = model,
                            whisperMode = whisperMode
                        ) { progress, message ->
                            val baseProgress = (index * 100) / totalVideos
                            val chunkProgress = progress / totalVideos
                            currentProgress = baseProgress + chunkProgress
                            currentStatusMessage = "[Archivo ${index + 1}/$totalVideos] $message"
                            addLog("[Archivo ${index + 1}/$totalVideos] $message")
                        }
                        lastCompletedJobId = jobId
                    }
                    selectedVideoUris = emptyList()
                    additionalNotes = ""
                    onComplete(lastCompletedJobId)
                }
            } catch (e: Exception) {
                addLog("Error crítico: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val job = jobRepository.getJobByIdSync(jobId)
                if (job != null) {
                    // Delete PDF file
                    if (!job.pdfPath.isNullOrEmpty()) {
                        val pdfFile = java.io.File(job.pdfPath)
                        if (pdfFile.exists()) {
                            pdfFile.delete()
                        }
                    }
                    // Delete ZIP file
                    if (!job.zipPath.isNullOrEmpty()) {
                        val zipFile = java.io.File(job.zipPath)
                        if (zipFile.exists()) {
                            zipFile.delete()
                        }
                    }
                    // Delete job directory (e.g. parentDir/job_$jobId)
                    val parentDir = java.io.File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        "Vontext"
                    )
                    val outputDir = java.io.File(parentDir, "job_$jobId")
                    if (outputDir.exists()) {
                        outputDir.deleteRecursively()
                    }

                    // Also delete any other potential ZIP matching the jobId
                    val searchZipName = "Vontext_${jobId.take(6)}.zip"
                    val probableZipFile = java.io.File(parentDir, searchZipName)
                    if (probableZipFile.exists()) {
                        probableZipFile.delete()
                    }

                    // Delete cached files in CacheDir
                    val cacheDir = appCtx.cacheDir
                    val tempFiles = cacheDir.listFiles { _, name -> name.contains(jobId) }
                    tempFiles?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            jobRepository.deleteJob(jobId)
        }
    }

    fun clearAllJobs() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Delete everything in output directory
                val parentDir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "Vontext"
                )
                if (parentDir.exists()) {
                    parentDir.deleteRecursively()
                    // Re-create the folder so it's fresh and ready for next jobs
                    parentDir.mkdirs()
                }

                // Delete entire local cache directories for temp videos
                val cacheDir = appCtx.cacheDir
                val tempFiles = cacheDir.listFiles { _, name -> name.contains("temp_video_") }
                tempFiles?.forEach { it.delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            jobRepository.clearAllJobs()
        }
    }
}
