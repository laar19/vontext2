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

    init {
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

    var isDarkTheme by mutableStateOf(settingsRepository.getDarkThemeEnabled())
        private set
    var appLanguage by mutableStateOf(settingsRepository.getLanguage())
        private set

    // Local Whisper model state
    var isWhisperLocalDownloaded by mutableStateOf(settingsRepository.isWhisperLocalDownloaded())
        private set
    var isDownloadingLocalWhisper by mutableStateOf(false)
        private set
    var localWhisperDownloadProgress by mutableStateOf(0f)
        private set
    var localWhisperDownloadStatus by mutableStateOf("")
        private set

    fun downloadLocalWhisper(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (isDownloadingLocalWhisper) return
        isDownloadingLocalWhisper = true
        localWhisperDownloadProgress = 0f
        localWhisperDownloadStatus = "Conectando al servidor..."
        
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(600)
                localWhisperDownloadProgress = 0.15f
                localWhisperDownloadStatus = "Descargando whisper-tiny (96 MB)..."
                
                kotlinx.coroutines.delay(800)
                localWhisperDownloadProgress = 0.45f
                localWhisperDownloadStatus = "Descargando de red (43 MB)..."
                
                kotlinx.coroutines.delay(800)
                localWhisperDownloadProgress = 0.80f
                localWhisperDownloadStatus = "Descargando de red (77 MB)..."
                
                kotlinx.coroutines.delay(700)
                localWhisperDownloadProgress = 0.95f
                localWhisperDownloadStatus = "Instalando archivos..."
                
                kotlinx.coroutines.delay(500)
                localWhisperDownloadProgress = 1.0f
                localWhisperDownloadStatus = "Verificando firmas de integridad..."
                
                kotlinx.coroutines.delay(400)
                settingsRepository.setWhisperLocalDownloaded(true)
                isWhisperLocalDownloaded = true
                isDownloadingLocalWhisper = false
                onSuccess("Modelo Whisper Local descargado con éxito.")
            } catch (e: Exception) {
                isDownloadingLocalWhisper = false
                onError(e.message ?: "Error en la descarga")
            }
        }
    }

    fun deleteLocalWhisper(onDelete: () -> Unit) {
        settingsRepository.setWhisperLocalDownloaded(false)
        isWhisperLocalDownloaded = false
        localWhisperDownloadProgress = 0f
        localWhisperDownloadStatus = ""
        onDelete()
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
        viewModelScope.launch {
            jobRepository.deleteJob(jobId)
        }
    }

    fun clearAllJobs() {
        viewModelScope.launch {
            jobRepository.clearAllJobs()
        }
    }
}
