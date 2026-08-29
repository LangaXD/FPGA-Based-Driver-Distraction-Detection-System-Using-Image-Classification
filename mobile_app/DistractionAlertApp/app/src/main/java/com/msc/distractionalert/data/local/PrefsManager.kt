package com.msc.distractionalert.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Plain SharedPreferences, not EncryptedSharedPreferences.
 *
 * This is a single-user research prototype talking to a token issued by a FastAPI
 * backend under the same developer's control; it is not a production auth surface,
 * so the extra key-management complexity of encrypted storage isn't warranted here.
 */
class PrefsManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit { putString(KEY_SERVER_URL, value.trimEnd('/')) }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, value) }

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrBlank()

    fun clearSession() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    companion object {
        private const val PREFS_NAME = "distraction_alert_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

        // 10.0.2.2 is the Android emulator's alias for the host machine's localhost,
        // so this works out of the box against a backend running on the same dev
        // machine. Change it in Settings once the backend has a real deployed URL -
        // plain HTTP on a non-standard port can get blocked by campus/corporate
        // proxies, so HTTPS behind a real domain is worth setting up for a phone
        // on a real network rather than an emulator.
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8001"
    }
}
