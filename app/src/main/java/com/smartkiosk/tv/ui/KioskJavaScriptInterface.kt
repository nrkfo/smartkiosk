package com.smartkiosk.tv.ui

import android.os.Build
import android.webkit.JavascriptInterface

class KioskJavaScriptInterface(private val activity: KioskActivity) {

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "model": "${Build.MANUFACTURER} ${Build.MODEL}",
                "sdk": ${Build.VERSION.SDK_INT},
                "appVersion": "1.0"
            }
        """.trimIndent()
    }

    @JavascriptInterface
    fun reload() {
        activity.runOnUiThread {
            activity.reloadWebView()
        }
    }

    @JavascriptInterface
    fun clearCache() {
        activity.runOnUiThread {
            activity.clearWebViewCache()
        }
    }

    @JavascriptInterface
    fun openAdmin(pin: String) {
        activity.runOnUiThread {
            activity.verifyAndOpenAdminDialog(pin)
        }
    }
}
