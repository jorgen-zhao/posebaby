package io.jorgen.posebaby

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages API keys, falling back to BuildConfig if not set locally.
 */
class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ZHIPU = "zhipu_api_key"
        private const val KEY_DOUBAO = "doubao_api_key"
    }

    fun getZhipuKey(): String {
        val stored = prefs.getString(KEY_ZHIPU, null)
        if (!stored.isNullOrEmpty()) return stored
        
        val fallback = BuildConfig.ZHIPU_API_KEY
        // Handle case where property is missing and defaults to "null" string
        return if (fallback != "null" && fallback.isNotBlank()) fallback else ""
    }

    fun setZhipuKey(key: String) {
        prefs.edit().putString(KEY_ZHIPU, key).apply()
    }

    fun getDoubaoKey(): String {
        val stored = prefs.getString(KEY_DOUBAO, null)
        if (!stored.isNullOrEmpty()) return stored
        
        val fallback = BuildConfig.DOUBAO_API_KEY
        return if (fallback != "null" && fallback.isNotBlank()) fallback else ""
    }

    fun setDoubaoKey(key: String) {
        prefs.edit().putString(KEY_DOUBAO, key).apply()
    }
    
    fun hasValidKeys(): Boolean {
        // Simple check: keys must not be empty
        // In a real app, might want regex validation for key format
        return getZhipuKey().isNotEmpty() && getDoubaoKey().isNotEmpty()
    }
}
