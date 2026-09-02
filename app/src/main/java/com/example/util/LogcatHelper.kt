package com.example.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogcatHelper {
    private const val TAG = "LogcatHelper"

    /**
     * Captures the recent device and application logcat lines (up to maxLines).
     * Focuses on errors, warnings, exceptions, and app-level logs.
     */
    fun captureRecentLogs(maxLines: Int = 250): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("=== VONTEXT LOGCAT DUMP [Capturado: $timestamp] ===\n")
        sb.append("Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, SDK ${android.os.Build.VERSION.SDK_INT})\n\n")

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
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
                count++
            }
            reader.close()
            process.waitFor()

            if (count == 0) {
                sb.append("[No se encontraron líneas de logcat recientes en el buffer o permiso restringido]\n")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al capturar logcat: ${e.message}")
            sb.append("[Fallo al ejecutar logcat: ${e.localizedMessage}]\n")
        } finally {
            process?.destroy()
        }

        return sb.toString()
    }

    /**
     * Writes the captured logcat into a file (e.g. logcat.txt in the output directory).
     */
    fun saveLogcatToFile(outputFile: File, maxLines: Int = 300): Boolean {
        return try {
            val content = captureRecentLogs(maxLines)
            outputFile.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving logcat to file: ${e.message}")
            false
        }
    }
}
