package com.bhumivera.scanner.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        return preferences.getString(SCANNER_KEY, null).orEmpty()
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


    fun staffToken(): String? = preferences.getString(STAFF_TOKEN_KEY, null)

    fun saveStaffToken(token: String) {
        preferences.edit(commit = true) { putString(STAFF_TOKEN_KEY, token) }
    }

    fun staffUsername(): String = preferences.getString(STAFF_USERNAME_KEY, null).orEmpty()

    fun saveStaffUsername(u: String) {
        preferences.edit(commit = true) { putString(STAFF_USERNAME_KEY, u.trim()) }
    }

    fun isOnWork(): Boolean = preferences.getBoolean(IS_ON_WORK_KEY, true)

    fun setIsOnWork(onWork: Boolean) {
        preferences.edit(commit = true) { putBoolean(IS_ON_WORK_KEY, onWork) }
    }

    fun loadStackedOrders(): List<StackedOrderRecord> {
        val json = preferences.getString(STACKED_ORDERS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<StackedOrderRecord>>() {}.type
        return Gson().fromJson<List<StackedOrderRecord>>(json, type) ?: emptyList()
    }

    fun saveStackedOrders(orders: List<StackedOrderRecord>) {
        val json = Gson().toJson(orders)
        preferences.edit(commit = true) { putString(STACKED_ORDERS_KEY, json) }
    }

    fun isAutoAddMode(): Boolean = preferences.getBoolean(AUTO_ADD_KEY, true)

    fun operatorName(): String {
        val stored = preferences.getString(OPERATOR_NAME_KEY, null)
        return if (!stored.isNullOrBlank()) stored else "Akash"
    }

    fun saveOperatorName(name: String) {
        preferences.edit(commit = true) { putString(OPERATOR_NAME_KEY, name.trim()) }
    }

    fun lastActiveAt(): Long = preferences.getLong(LAST_ACTIVE_AT_KEY, 0L)

    fun recordActivity(now: Long = System.currentTimeMillis()) {
        preferences.edit(commit = true) {
            putLong(LAST_ACTIVE_AT_KEY, now)
            putLong(IDLE_EXPIRES_AT_KEY, now + TEN_MINUTES_MILLIS)
        }
    }

    fun idleExpiresAt(): Long {
        val stored = preferences.getLong(IDLE_EXPIRES_AT_KEY, 0L)
        if (stored > 0L) return stored
        val last = lastActiveAt()
        return if (last > 0L) last + TEN_MINUTES_MILLIS else 0L
    }

    fun isIdleExpired(now: Long = System.currentTimeMillis()): Boolean {
        val exp = idleExpiresAt()
        return exp > 0L && now >= exp
    }

    fun idleSecondsRemaining(now: Long = System.currentTimeMillis()): Int {
        val exp = idleExpiresAt()
        if (exp <= now) return 0
        return ((exp - now) / 1000L).toInt()
    }

    fun setAutoAddMode(enabled: Boolean) {
        preferences.edit(commit = true) { putBoolean(AUTO_ADD_KEY, enabled) }
    }

    fun pairedAt(): Long = preferences.getLong(PAIRED_AT_KEY, 0L)

    fun savePairedAt(time: Long) {
        preferences.edit(commit = true) { putLong(PAIRED_AT_KEY, time) }
    }

    fun isPaired(): Boolean = scannerApiKey().isNotBlank() && pairedAt() > 0L && !isSessionExpired()

    fun loadPosDrafts(): List<PosDraftOrder> {
        val json = preferences.getString(POS_DRAFTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<PosDraftOrder>>() {}.type
        return Gson().fromJson<List<PosDraftOrder>>(json, type) ?: emptyList()
    }

    fun savePosDrafts(drafts: List<PosDraftOrder>) {
        val json = Gson().toJson(drafts)
        preferences.edit(commit = true) { putString(POS_DRAFTS_KEY, json) }
    }

    fun expiresAt(): Long {
        val paired = pairedAt()
        if (paired <= 0L) return 0L
        val twelveHourExpiry = paired + TWELVE_HOURS_MILLIS
        val stored = preferences.getLong(EXPIRES_AT_KEY, 0L)
        return if (stored > 0L) minOf(stored, twelveHourExpiry) else twelveHourExpiry
    }

    fun saveExpiresAt(time: Long) {
        preferences.edit(commit = true) { putLong(EXPIRES_AT_KEY, time) }
    }

    fun hoursRemaining(): Int {
        if (!isPaired()) return 0
        val remaining = expiresAt() - System.currentTimeMillis()
        if (remaining <= 0L) return 0
        return ((remaining + 3_599_999L) / 3_600_000L).toInt()
    }

    fun isSessionExpired(): Boolean = pairedAt() > 0L && System.currentTimeMillis() >= expiresAt()

    fun clear() {
        preferences.edit(commit = true) { clear() }
    }

    companion object {
        const val TWELVE_HOURS_MILLIS = 12L * 60 * 60 * 1000L
        const val TEN_MINUTES_MILLIS = 10L * 60 * 1000L
        private const val TOKEN_KEY = "customer_token"
        private const val SCANNER_KEY = "scanner_api_key"
        private const val BACKEND_URL_KEY = "scanner_backend_url"
        private const val SESSION_CODE_KEY = "scanner_session_code"
        private const val OPERATOR_NAME_KEY = "scanner_operator_name"
        private const val AUTO_ADD_KEY = "scanner_auto_add_mode"
        private const val PAIRED_AT_KEY = "scanner_paired_at"
        private const val EXPIRES_AT_KEY = "scanner_expires_at"
        private const val LAST_ACTIVE_AT_KEY = "scanner_last_active_at"
        private const val IDLE_EXPIRES_AT_KEY = "scanner_idle_expires_at"
        private const val POS_DRAFTS_KEY = "pos_saved_drafts"
        private const val STAFF_TOKEN_KEY = "staff_auth_token"
        private const val STAFF_USERNAME_KEY = "staff_username"
        private const val IS_ON_WORK_KEY = "staff_is_on_work"
        private const val STACKED_ORDERS_KEY = "stacked_orders_queue"
    }
}
