package com.example.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
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

    /**
     * Captures the recent device and application logcat lines (up to maxLines).
     * Focuses on errors, warnings, exceptions, and app-level logs.
     * Guaranteed never to hang or block indefinitely.
     */
    fun captureRecentLogs(maxLines: Int = 250): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("=== VONTEXT LOGCAT DUMP [Capturado: $timestamp] ===\n")
        sb.append("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})\n\n")

        var process: Process? = null
        try {
            // logcat -d dumps the buffer without blocking
            // -v time formats lines with timestamps
            // -t fetches the last N lines
            val command = arrayOf("logcat", "-d", "-v", "time", "-t", maxLines.toString())
            process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var count = 0
            while (reader.readLine().also { line = it } != null && count < maxLines) {
                sb.append(line).append("\n")
                count++
            }
            reader.close()

            // Wait with strict 3-second timeout to prevent freezes
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

            if (count == 0) {
                sb.append("[No se encontraron líneas de logcat recientes en el buffer o permiso restringido]\n")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al capturar logcat: ${e.message}")
            sb.append("[Fallo al ejecutar logcat: ${e.localizedMessage}]\n")
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup issues
            }
        }

        return sb.toString()
    }

    /**
     * Writes the captured logcat into a file (e.g. logcat.txt in the output directory).
     */
    fun saveLogcatToFile(outputFile: File, maxLines: Int = 300): Boolean {
        return try {
            val content = captureRecentLogs(maxLines)
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            outputFile.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving logcat to file: ${e.message}")
            false
        }
    }
}

