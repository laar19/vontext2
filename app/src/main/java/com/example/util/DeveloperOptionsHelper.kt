package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object DeveloperOptionsHelper {
    private const val TAG = "DevOptionsHelper"

    /**
     * Checks if "Show taps" (show_touches) is enabled in Android System Settings.
     */
    fun isShowTouchesEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "show_touches", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if "Pointer location" (pointer_location) is enabled.
     */
    fun isPointerLocationEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "pointer_location", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the system Developer Options screen so the user can easily toggle "Show taps".
     */
    fun openDeveloperOptions(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open development settings directly: ${e.message}")
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
