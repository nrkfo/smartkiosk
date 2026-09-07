package com.smartkiosk.tv.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var startUrl: String
        get() = prefs.getString(KEY_START_URL, DEFAULT_START_URL) ?: DEFAULT_START_URL
        set(value) = prefs.edit().putString(KEY_START_URL, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    var isKioskModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_KIOSK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_KIOSK_ENABLED, value).apply()

    var isAutoLaunchEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT)
        set(value) = prefs.edit().putInt(KEY_SERVER_PORT, value).apply()

    var mediaUrl: String
        get() = prefs.getString(KEY_MEDIA_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MEDIA_URL, value).apply()

    var isClearCacheOnReload: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_CACHE, false)
        set(value) = prefs.edit().putBoolean(KEY_CLEAR_CACHE, value).apply()

    var idleTimeoutSeconds: Int
        get() = prefs.getInt(KEY_IDLE_TIMEOUT, 0)
        set(value) = prefs.edit().putInt(KEY_IDLE_TIMEOUT, value).apply()

    companion object {
        private const val PREFS_NAME = "smart_kiosk_prefs"
        private const val KEY_START_URL = "start_url"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_KIOSK_ENABLED = "kiosk_enabled"
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_MEDIA_URL = "media_url"
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_IDLE_TIMEOUT = "idle_timeout"

        const val DEFAULT_START_URL = "https://google.com"
        const val DEFAULT_ADMIN_PIN = "0000"
        const val DEFAULT_SERVER_PORT = 8080
    }
}
