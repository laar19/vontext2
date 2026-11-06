package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LogcatHelper {
    private const val TAG = "LogcatHelper"

    /**
     * Resolves a reliable output directory for Vontext:
     * Tries the public Downloads/Vontext folder first, and falls back to
     * app-specific external storage if scoped storage forbids direct public access.
     */
    fun getVontextBaseDir(context: Context): File {
        return try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val vontextDir = File(publicDir, "Vontext")
            if (vontextDir.exists() || vontextDir.mkdirs()) {
                if (vontextDir.canWrite()) {
                    vontextDir
                } else {
                    fallbackDir(context)
                }
            } else {
                fallbackDir(context)
            }
        } catch (e: Exception) {
            fallbackDir(context)
        }
    }

    private fun fallbackDir(context: Context): File {
        val appExtDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "Vontext")
        if (!appExtDir.exists()) {
            appExtDir.mkdirs()
        }
        return appExtDir
    }

    fun hasReadLogsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_LOGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Redacts sensitive secrets, API keys, tokens, emails, and credentials.
     * Replaces with [REDACTED].
     */
    fun redactSensitiveInfo(input: String): String {
        var text = input

        // 1. Bearer tokens: Bearer <token>
        text = text.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9_\\-\\.~+/]+=*")) { "Bearer [REDACTED]" }

        // 2. OpenAI / standard api keys: sk-...
        text = text.replace(Regex("\\bsk-[A-Za-z0-9_\\-]{16,}\\b")) { "[REDACTED]" }

        // 3. Google API keys: AIza...
        text = text.replace(Regex("\\bAIza[0-9A-Za-z_\\-]{35}\\b")) { "[REDACTED]" }

        // 4. JWT tokens: eyJ...
        text = text.replace(Regex("\\beyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\b")) { "[REDACTED]" }

        // 5. Query or json parameters like api_key=..., apiKey=..., token=..., secret=...
        text = text.replace(Regex("(?i)(api[_-]?key|secret|token|password|auth|access[_-]?token)\\s*[:=]\\s*['\"]?([A-Za-z0-9_\\-\\.~+/]{6,})['\"]?")) { match ->
            val key = match.groupValues[1]
            "$key=[REDACTED]"
        }

        // 6. Emails
        text = text.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")) { "[REDACTED]" }

        return text
    }

    /**
     * Finds the PID of a package if it is currently running.
     */
    fun getPidOfPackage(packageName: String): Int? {
        if (packageName.isBlank()) return null
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("pidof", packageName.trim()))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.destroy()
            line?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Captures system and app logcat lines with exact time window, PID/package filtering, and secret redaction.
     *
     * @param context Application context
     * @param targetPackage Optional package to focus on (e.g. com.myapp.debug)
     * @param startTimeMs Optional start time timestamp in milliseconds (e.g. recording start time or video METADATA_KEY_DATE)
     * @param maxLines Maximum lines to keep
     */
    fun captureRecentLogs(
        context: Context? = null,
        targetPackage: String? = null,
        startTimeMs: Long? = null,
        maxLines: Int = 300
    ): String {
        val hasPermission = context != null && hasReadLogsPermission(context)
        val sb = StringBuilder()
        val dumpTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("=== VONTEXT LOGCAT DUMP [Capturado: $dumpTime] ===\n")
        sb.append("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})\n")

        if (!hasPermission) {
            val appId = context?.packageName ?: "com.example"
            sb.append("\n⚠️ AVISO DE PERMISOS:\n")
            sb.append("El permiso READ_LOGS no está concedido. Por seguridad del sistema Android, este archivo solo contiene los logs propios de la app Vontext y no capturará excepciones ni eventos de apps de terceros.\n")
            sb.append("Para capturar logs de cualquier app bajo prueba, concede el permiso vía ADB ejecutando en tu PC:\n")
            sb.append("adb shell pm grant $appId android.permission.READ_LOGS\n\n")
        }

        val pkg = targetPackage?.trim().orEmpty()
        val pid = if (pkg.isNotEmpty()) getPidOfPackage(pkg) else null
        if (pkg.isNotEmpty()) {
            sb.append("App objetivo a depurar: $pkg (PID activo: ${pid ?: "no activo / cerrado"})\n\n")
        }

        // Determine -T parameter for temporal window
        // Format for logcat -T: "MM-dd HH:mm:ss.mmm"
        val windowStartMs = when {
            startTimeMs != null && startTimeMs > 0L -> startTimeMs
            else -> System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)
        }
        val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        val timeFilterStr = timeFormat.format(Date(windowStartMs))

        val cmdList = mutableListOf("logcat", "-d", "-v", "threadtime")

        if (pid != null) {
            cmdList.add("--pid=$pid")
        }

        cmdList.add("-T")
        cmdList.add(timeFilterStr)

        var process: Process? = null
        val capturedLines = mutableListOf<String>()

        try {
            process = Runtime.getRuntime().exec(cmdList.toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                capturedLines.add(line ?: "")
            }
            reader.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(3, TimeUnit.SECONDS)
            } else {
                var waitCycles = 0
                while (waitCycles < 15) {
                    try {
                        process.exitValue()
                        break
                    } catch (e: IllegalThreadStateException) {
                        Thread.sleep(100)
                        waitCycles++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error executing logcat with time filter: ${e.message}")
            // Fallback to plain logcat -d -v threadtime -t maxLines
            try {
                process?.destroy()
                val fallbackCmd = arrayOf("logcat", "-d", "-v", "threadtime", "-t", maxLines.toString())
                process = Runtime.getRuntime().exec(fallbackCmd)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    capturedLines.add(line ?: "")
                }
                reader.close()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    process.waitFor(3, TimeUnit.SECONDS)
                }
            } catch (ex: Exception) {
                sb.append("[Error al ejecutar logcat: ${ex.localizedMessage}]\n")
            }
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {}
        }

        // If package specified and PID was null (process died or multiple processes),
        // filter lines that contain package name OR AndroidRuntime OR FATAL EXCEPTION
        val filteredLines = if (pkg.isNotEmpty() && pid == null) {
            val matched = capturedLines.filter { l ->
                l.contains(pkg, ignoreCase = true) ||
                l.contains("AndroidRuntime", ignoreCase = true) ||
                l.contains("FATAL EXCEPTION", ignoreCase = true)
            }
            if (matched.isNotEmpty()) matched else capturedLines
        } else {
            capturedLines
        }

        val tailLines = if (filteredLines.size > maxLines) {
            filteredLines.takeLast(maxLines)
        } else {
            filteredLines
        }

        if (tailLines.isEmpty()) {
            sb.append("[No se encontraron líneas de logcat en la ventana temporal seleccionada]\n")
        } else {
            for (rawLine in tailLines) {
                val clean = redactSensitiveInfo(rawLine)
                sb.append(clean).append("\n")
            }
        }

        return sb.toString()
    }

    /**
     * Writes the captured logcat into a file (e.g. logcat.txt in the output directory).
     */
    fun saveLogcatToFile(
        outputFile: File,
        context: Context? = null,
        targetPackage: String? = null,
        startTimeMs: Long? = null,
        maxLines: Int = 300
    ): Boolean {
        return try {
            val content = captureRecentLogs(
                context = context,
                targetPackage = targetPackage,
                startTimeMs = startTimeMs,
                maxLines = maxLines
            )
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            outputFile.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving logcat to file: ${e.message}")
            false
        }
    }
}
