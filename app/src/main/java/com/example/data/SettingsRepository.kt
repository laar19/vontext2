package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vontext_settings", Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString("api_key", "") ?: ""
    fun setApiKey(value: String) = prefs.edit().putString("api_key", value).apply()

    fun getEndpoint(): String = prefs.getString("endpoint", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
    fun setEndpoint(value: String) = prefs.edit().putString("endpoint", value).apply()

    fun getModel(): String = prefs.getString("model", "whisper-1") ?: "whisper-1"
    fun setModel(value: String) = prefs.edit().putString("model", value).apply()

    fun getDefaultInterval(): Int = prefs.getInt("default_interval", 5)
    fun setDefaultInterval(value: Int) = prefs.edit().putInt("default_interval", value).apply()

    fun getLanguage(): String = prefs.getString("language", "es") ?: "es"
    fun setLanguage(value: String) = prefs.edit().putString("language", value).apply()

    fun getDarkThemeEnabled(): Boolean = prefs.getBoolean("dark_theme", false)
    fun setDarkThemeEnabled(value: Boolean) = prefs.edit().putBoolean("dark_theme", value).apply()

    fun isWhisperLocalDownloaded(): Boolean = prefs.getBoolean("whisper_local_downloaded", false)
    fun setWhisperLocalDownloaded(value: Boolean) = prefs.edit().putBoolean("whisper_local_downloaded", value).apply()
}
