package dev.laar19.vontext.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.laar19.vontext.MainActivity
import dev.laar19.vontext.processor.VideoProcessor
import dev.laar19.vontext.util.LogcatHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Floating Overlay Service that records screen + microphone audio via MediaProjection,
 * displays a floating red bubble with a timer during recording, saves the video,
 * automatically processes it with VideoProcessor, and sends a notification when the PDF is ready.
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var bubbleContainer: LinearLayout? = null
    private var iconView: ImageView? = null
    private var timerTextView: TextView? = null
    private var actionMenu: LinearLayout? = null

    private var isExpanded = false
    private var isRecording = false
    private var recordingStartTimeMs = 0L

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Screen recording fields
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var currentRecordingFile: File? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsedSec = (System.currentTimeMillis() - recordingStartTimeMs) / 1000
                val min = elapsedSec / 60
                val sec = elapsedSec % 60
                timerTextView?.text = String.format(Locale.US, "%02d:%02d", min, sec)
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    companion object {
        private const val TAG = "FloatingOverlayService"
        const val CHANNEL_ID = "vontext_overlay_channel"
        const val REPORT_CHANNEL_ID = "vontext_report_channel"
        const val NOTIFICATION_ID = 2001
        const val REPORT_NOTIFICATION_ID = 2002

        const val ACTION_START = "dev.laar19.vontext.service.ACTION_START"
        const val ACTION_STOP = "dev.laar19.vontext.service.ACTION_STOP"
        const val ACTION_START_RECORDING = "dev.laar19.vontext.service.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "dev.laar19.vontext.service.ACTION_STOP_RECORDING"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var isRunning = false
            private set

        fun isOverlayPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannels()
        startNormalForeground()
        showFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (isRecording) {
                    stopScreenRecording()
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode != 0 && resultData != null) {
                    startScreenRecording(resultCode, resultData)
                }
            }
            ACTION_STOP_RECORDING -> {
                if (isRecording) {
                    stopScreenRecording()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val overlayChannel = NotificationChannel(
                CHANNEL_ID,
                "Vontext Botón Flotante y Grabación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la grabación y la burbuja flotante para depuración de apps"
            }
            manager.createNotificationChannel(overlayChannel)

            val reportChannel = NotificationChannel(
                REPORT_CHANNEL_ID,
                "Reportes de Vontext",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un reporte PDF de prueba está listo"
            }
            manager.createNotificationChannel(reportChannel)
        }
    }

    private fun startNormalForeground() {
        val notification = buildNormalNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (e: Exception) {
                // If projection is not yet granted at service start, start basic foreground
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNormalNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vontext Depuración Activa")
            .setContentText("Toca la burbuja flotante para iniciar la grabación del bug.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cerrar", pendingStop)
            .setOngoing(true)
            .build()
    }

    private fun buildRecordingNotification(): Notification {
        val stopRecIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val pendingStopRec = PendingIntent.getService(
            this, 2, stopRecIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 Grabando pantalla y micrófono...")
            .setContentText("Toca la burbuja roja para detener y procesar el reporte.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_pause, "Detener Grabación", pendingStopRec)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permiso de superposición no concedido", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 300
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            elevation = 16f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            background = createBubbleBackground(android.graphics.Color.parseColor("#1E52B3"))
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(54, 54)
        }

        val timerText = TextView(this).apply {
            text = " Grabar"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            visibility = View.VISIBLE
        }

        container.addView(icon)
        container.addView(timerText)
        rootLayout.addView(container)

        // Submenu for extra quick actions (Dump Logcat, Open Vontext)
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 10, 0, 0)
        }

        val dumpBtn = TextView(this).apply {
            text = "📋 Volcar Logcat"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 11f
            setPadding(14, 8, 14, 8)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#2C3E50"))
                cornerRadius = 14f
            }
            setOnClickListener {
                val vontextDir = LogcatHelper.getVontextBaseDir(this@FloatingOverlayService)
                val logFile = File(vontextDir, "logcat_manual_${System.currentTimeMillis()}.txt")
                val success = LogcatHelper.saveLogcatToFile(
                    outputFile = logFile,
                    context = this@FloatingOverlayService,
                    maxLines = 300
                )
                if (success) {
                    Toast.makeText(this@FloatingOverlayService, "✅ Logcat guardado en: ${logFile.name}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@FloatingOverlayService, "⚠️ No se pudo guardar el logcat", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val openVontextBtn = TextView(this).apply {
            text = "🎬 Abrir Vontext"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 11f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
            }
            layoutParams = lp
            setPadding(14, 8, 14, 8)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#27AE60"))
                cornerRadius = 14f
            }
            setOnClickListener {
                val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }

        menu.addView(dumpBtn)
        menu.addView(openVontextBtn)
        rootLayout.addView(menu)

        bubbleContainer = container
        iconView = icon
        timerTextView = timerText
        actionMenu = menu

        // Drag and click handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        onBubbleClicked()
                    }
                    true
                }
                else -> false
            }
        }

        container.setOnLongClickListener {
            isExpanded = !isExpanded
            actionMenu?.visibility = if (isExpanded) View.VISIBLE else View.GONE
            windowManager?.updateViewLayout(rootLayout, params)
            true
        }

        overlayView = rootLayout
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al crear burbuja: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBubbleBackground(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = 32f
            setStroke(3, android.graphics.Color.WHITE)
        }
    }

    private fun onBubbleClicked() {
        if (isRecording) {
            // Stop recording
            stopScreenRecording()
        } else {
            // Request permission transparently to start recording
            val intent = Intent(this, ScreenCapturePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun startScreenRecording(resultCode: Int, resultData: Intent) {
        if (isRecording) return

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager == null) {
                Toast.makeText(this, "No se pudo obtener MediaProjectionManager", Toast.LENGTH_SHORT).show()
                return
            }

            // CRITICAL FOR ANDROID 14+: startForeground with mediaProjection|microphone BEFORE getMediaProjection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildRecordingNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildRecordingNotification())
            }

            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                Toast.makeText(this, "Error: Permiso de captura denegado", Toast.LENGTH_SHORT).show()
                startNormalForeground()
                return
            }

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealMetrics(metrics)
            val density = metrics.densityDpi

            // Determine safe resolution (even dimensions)
            val width = if (metrics.widthPixels > 1080) 1080 else (metrics.widthPixels / 2) * 2
            val height = if (metrics.heightPixels > 1920) 1920 else (metrics.heightPixels / 2) * 2

            val vontextDir = LogcatHelper.getVontextBaseDir(this)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val videoFile = File(vontextDir, "vontext_capture_$timestamp.mp4")
            currentRecordingFile = videoFile

            val hasMicPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                if (hasMicPermission) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (hasMicPermission) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(96000)
                }
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(3_000_000)
                setOutputFile(videoFile.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "VontextScreenRecorder",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            isRecording = true
            recordingStartTimeMs = System.currentTimeMillis()

            // Update UI to red bubble with timer
            bubbleContainer?.background = createBubbleBackground(android.graphics.Color.parseColor("#E74C3C"))
            iconView?.setImageResource(android.R.drawable.ic_media_pause)
            timerTextView?.text = " 00:00"
            timerTextView?.visibility = View.VISIBLE
            actionMenu?.visibility = View.GONE
            isExpanded = false

            mainHandler.post(timerRunnable)
            Toast.makeText(this, "🔴 Grabando pantalla y audio...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording", e)
            Toast.makeText(this, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_LONG).show()
            cleanupRecordingResources()
            startNormalForeground()
        }
    }

    private fun stopScreenRecording() {
        if (!isRecording) return
        isRecording = false
        mainHandler.removeCallbacks(timerRunnable)

        val recordedVideo = currentRecordingFile
        val recordStart = recordingStartTimeMs

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop failed: ${e.message}")
        }

        cleanupRecordingResources()

        // Reset bubble UI
        bubbleContainer?.background = createBubbleBackground(android.graphics.Color.parseColor("#1E52B3"))
        iconView?.setImageResource(android.R.drawable.ic_menu_camera)
        timerTextView?.text = " Grabar"
        startNormalForeground()

        if (recordedVideo != null && recordedVideo.exists() && recordedVideo.length() > 0) {
            Toast.makeText(this, "✅ Grabación guardada. Procesando reporte...", Toast.LENGTH_LONG).show()
            enqueueProcessing(recordedVideo, recordStart)
        } else {
            Toast.makeText(this, "⚠️ La grabación no se guardó correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanupRecordingResources() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {}

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {}

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {}
    }

    private fun enqueueProcessing(videoFile: File, startTimeMs: Long) {
        serviceScope.launch {
            try {
                val db = dev.laar19.vontext.data.AppDatabase.getDatabase(applicationContext)
                val jobRepo = dev.laar19.vontext.data.JobRepository(db.jobDao())
                val settingsRepo = dev.laar19.vontext.data.SettingsRepository(applicationContext)
                val processor = VideoProcessor(applicationContext, jobRepo, settingsRepo)
                val videoUri = Uri.fromFile(videoFile)
                val notes = "Grabación de depuración iniciada desde burbuja flotante."

                processor.process(
                    videoUri = videoUri,
                    notes = notes,
                    frameInterval = 0,
                    whisperModel = "base",
                    whisperMode = "LOCAL",
                    onProgressUpdate = { progress, msg ->
                        Log.d(TAG, "Procesando grabación automática: $progress% - $msg")
                    }
                )

                // Locate generated PDF
                val vontextDir = LogcatHelper.getVontextBaseDir(applicationContext)
                val jobDirs = vontextDir.listFiles { f -> f.isDirectory && f.name.startsWith("job_") }
                    ?.sortedByDescending { it.lastModified() }
                val latestJobDir = jobDirs?.firstOrNull()
                val pdfFile = latestJobDir?.listFiles { f -> f.name.endsWith(".pdf") }?.firstOrNull()

                if (pdfFile != null && pdfFile.exists()) {
                    showReportReadyNotification(pdfFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background processing", e)
            }
        }
    }

    private fun showReportReadyNotification(pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            pdfFile
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            105,
            viewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, REPORT_CHANNEL_ID)
            .setContentTitle("Reporte listo")
            .setContentText("Toca para abrir el reporte PDF generado: ${pdfFile.name}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(REPORT_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (isRecording) {
            stopScreenRecording()
        }
        cleanupRecordingResources()
        mainHandler.removeCallbacks(timerRunnable)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {}

        if (overlayView != null) {
            try {
                if (overlayView?.isAttachedToWindow == true) {
                    windowManager?.removeView(overlayView)
                }
            } catch (e: Exception) {}
            overlayView = null
        }
    }
}
