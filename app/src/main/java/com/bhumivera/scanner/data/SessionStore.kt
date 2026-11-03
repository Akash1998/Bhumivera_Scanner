package com.bhumivera.scanner.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "bhumivera_scanner_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun token(): String? = preferences.getString(TOKEN_KEY, null)

    fun saveToken(token: String) {
        preferences.edit(commit = true) { putString(TOKEN_KEY, token) }
    }

    fun scannerApiKey(): String {
        val stored = preferences.getString(SCANNER_KEY, null)
        return if (!stored.isNullOrBlank()) stored else DEFAULT_SCANNER_KEY
    }

    fun saveScannerApiKey(key: String) {
        val clean = key.trim()
        preferences.edit(commit = true) { putString(SCANNER_KEY, clean) }
    }

    fun backendUrl(): String {
        val stored = preferences.getString(BACKEND_URL_KEY, null)
        return if (!stored.isNullOrBlank()) stored else "https://service.bhumivera.com"
    }

    fun saveBackendUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        preferences.edit(commit = true) { putString(BACKEND_URL_KEY, clean) }
    }

    fun sessionCode(): String {
        val stored = preferences.getString(SESSION_CODE_KEY, null)
        return if (!stored.isNullOrBlank()) stored else "DEFAULT-POS-TERMINAL"
    }

    fun saveSessionCode(code: String) {
        preferences.edit(commit = true) { putString(SESSION_CODE_KEY, code.trim()) }
    }

    fun isAutoAddMode(): Boolean = preferences.getBoolean(AUTO_ADD_KEY, false)

    fun setAutoAddMode(enabled: Boolean) {
        preferences.edit(commit = true) { putBoolean(AUTO_ADD_KEY, enabled) }
    }

    fun clear() {
        preferences.edit(commit = true) { clear() }
    }

    companion object {
        const val DEFAULT_SCANNER_KEY = "BV-SCAN-34F204D93552CF7D3A398568"
        private const val TOKEN_KEY = "customer_token"
        private const val SCANNER_KEY = "scanner_api_key"
        private const val BACKEND_URL_KEY = "scanner_backend_url"
        private const val SESSION_CODE_KEY = "scanner_session_code"
        private const val AUTO_ADD_KEY = "scanner_auto_add_mode"
    }
}
