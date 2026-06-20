package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

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

    fun getActiveLocalLanguage(): String = prefs.getString("active_local_language", "es") ?: "es"
    fun setActiveLocalLanguage(value: String) = prefs.edit().putString("active_local_language", value).apply()

    fun getSelectedModelId(): String = prefs.getString("selected_model_id", "LOCAL") ?: "LOCAL"
    fun setSelectedModelId(value: String) = prefs.edit().putString("selected_model_id", value).apply()

    fun getIncludeTimestamps(): Boolean = prefs.getBoolean("include_timestamps", true)
    fun setIncludeTimestamps(value: Boolean) = prefs.edit().putBoolean("include_timestamps", value).apply()

    fun getCustomModels(): List<CustomModel> {
        val serialized = prefs.getString("custom_models_serialized", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        val list = mutableListOf<CustomModel>()
        val items = serialized.split("||")
        for (item in items) {
            if (item.isEmpty()) continue
            try {
                val parts = item.split(";;")
                if (parts.size >= 6) {
                    val id = String(Base64.decode(parts[0], Base64.NO_WRAP), Charsets.UTF_8)
                    val name = String(Base64.decode(parts[1], Base64.NO_WRAP), Charsets.UTF_8)
                    val type = String(Base64.decode(parts[2], Base64.NO_WRAP), Charsets.UTF_8)
                    val apiKey = String(Base64.decode(parts[3], Base64.NO_WRAP), Charsets.UTF_8)
                    val endpoint = String(Base64.decode(parts[4], Base64.NO_WRAP), Charsets.UTF_8)
                    val modelName = String(Base64.decode(parts[5], Base64.NO_WRAP), Charsets.UTF_8)
                    list.add(CustomModel(id, name, type, apiKey, endpoint, modelName))
                }
            } catch (e: Exception) {
                // skip malformed
            }
        }
        return list
    }

    private fun saveCustomModels(list: List<CustomModel>) {
        val sb = StringBuilder()
        for (i in list.indices) {
            val m = list[i]
            val idB64 = Base64.encodeToString(m.id.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val nameB64 = Base64.encodeToString(m.name.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val typeB64 = Base64.encodeToString(m.type.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val apiKeyB64 = Base64.encodeToString(m.apiKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val endpointB64 = Base64.encodeToString(m.endpoint.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val modelNameB64 = Base64.encodeToString(m.modelName.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sb.append("$idB64;;$nameB64;;$typeB64;;$apiKeyB64;;$endpointB64;;$modelNameB64")
            if (i < list.size - 1) {
                sb.append("||")
            }
        }
        prefs.edit().putString("custom_models_serialized", sb.toString()).apply()
    }

    fun addCustomModel(model: CustomModel) {
        val current = getCustomModels().toMutableList()
        current.add(model)
        saveCustomModels(current)
    }

    fun deleteCustomModel(id: String) {
        val current = getCustomModels().filter { it.id != id }
        saveCustomModels(current)
    }
}
