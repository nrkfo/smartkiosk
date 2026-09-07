package com.smartkiosk.tv.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var startUrl: String
        get() {
            val savedUrl = prefs.getString(KEY_START_URL, DEFAULT_START_URL) ?: DEFAULT_START_URL
            if (savedUrl == "https://google.com") {
                return DEFAULT_START_URL
            }
            return savedUrl
        }
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

    var pageZoomPercent: Int
        get() = prefs.getInt(KEY_PAGE_ZOOM, 100)
        set(value) = prefs.edit().putInt(KEY_PAGE_ZOOM, value).apply()

    var isScheduledReloadEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULED_RELOAD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SCHEDULED_RELOAD_ENABLED, value).apply()

    var scheduledReloadHour: Int
        get() = prefs.getInt(KEY_SCHEDULED_RELOAD_HOUR, 3)
        set(value) = prefs.edit().putInt(KEY_SCHEDULED_RELOAD_HOUR, value).apply()

    var isBlockDownloads: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_DOWNLOADS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_DOWNLOADS, value).apply()

    var isDisableTextSelection: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_TEXT_SELECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_TEXT_SELECTION, value).apply()

    companion object {
        private const val PREFS_NAME = "smart_kiosk_prefs"
        private const val KEY_START_URL = "start_url"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_KIOSK_ENABLED = "kiosk_enabled"
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_MEDIA_URL = "media_url"
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_PAGE_ZOOM = "page_zoom"
        private const val KEY_SCHEDULED_RELOAD_ENABLED = "scheduled_reload_enabled"
        private const val KEY_SCHEDULED_RELOAD_HOUR = "scheduled_reload_hour"
        private const val KEY_BLOCK_DOWNLOADS = "block_downloads"
        private const val KEY_DISABLE_TEXT_SELECTION = "disable_text_selection"

        const val DEFAULT_START_URL = "http://127.0.0.1:8080"
        const val DEFAULT_ADMIN_PIN = "0000"
        const val DEFAULT_SERVER_PORT = 8080
    }
}
