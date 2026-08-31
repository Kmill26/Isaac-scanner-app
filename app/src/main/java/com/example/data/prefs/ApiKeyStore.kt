package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.apiKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_settings")

/**
 * Persists the user-provided Gemini API key across app restarts.
 * Falls back to [BuildConfig.GEMINI_API_KEY] when no custom key is stored.
 */
class ApiKeyStore(private val context: Context) {

    val apiKeyFlow: Flow<String> = context.apiKeyDataStore.data.map { prefs ->
        val userKey = prefs[KEY_API_KEY]?.trim().orEmpty()
        if (userKey.isNotEmpty()) userKey else BuildConfig.GEMINI_API_KEY.trim()
    }

    suspend fun saveKey(key: String) {
        context.apiKeyDataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key.trim()
        }
    }

    suspend fun clearKey() {
        context.apiKeyDataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
        }
    }

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("user_gemini_api_key")

        @Volatile
        var customKey: String? = null

        fun isKeyValid(k: String?): Boolean {
            val key = k?.trim().orEmpty()
            return key.isNotEmpty() &&
                !key.equals("NONE", ignoreCase = true) &&
                key != "MY_GEMINI_API_KEY" &&
                key != "YOUR_API_KEY"
        }

        fun getEffectiveKey(): String {
            val custom = customKey?.trim().orEmpty()
            if (isKeyValid(custom)) return custom
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            if (isKeyValid(buildKey)) return buildKey
            return ""
        }
    }
}
