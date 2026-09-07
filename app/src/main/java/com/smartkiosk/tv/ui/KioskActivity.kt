package com.smartkiosk.tv.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.smartkiosk.tv.R
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.dpc.KioskAdminReceiver
import com.smartkiosk.tv.service.KioskWatchdogService

class KioskActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var playerView: PlayerView
    private lateinit var offlineContainer: LinearLayout
    private lateinit var btnRetryNetwork: Button
    private lateinit var prefs: PreferencesManager

    private var exoPlayer: ExoPlayer? = null
    private var dpadUpCounter = 0
    private var lastDpadTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        setContentView(R.layout.activity_kiosk)
        prefs = PreferencesManager(this)

        webView = findViewById(R.id.web_view)
        playerView = findViewById(R.id.player_view)
        offlineContainer = findViewById(R.id.offline_container)
        btnRetryNetwork = findViewById(R.id.btn_retry_network)

        btnRetryNetwork.setOnClickListener {
            reloadWebView()
        }

        btnRetryNetwork.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }
        }

        setupWebView()
        setupLockTaskMode()
        startWatchdogService()

        loadContent()
        observeNetwork()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (prefs.isKioskModeEnabled) {
            setupLockTaskMode()
        }
    }

    private fun startWatchdogService() {
        val serviceIntent = Intent(this, KioskWatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setupLockTaskMode() {
        if (prefs.isKioskModeEnabled && KioskAdminReceiver.isDeviceOwner(this)) {
            KioskAdminReceiver.configureKioskLockTask(this, true)
            try {
                startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopLockTaskMode() {
        try {
            stopLockTask()
            KioskAdminReceiver.configureKioskLockTask(this, false)
            Toast.makeText(this, "Lock Task Mode отключен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mediaPlaybackRequiresUserGesture = false

        webView.addJavascriptInterface(KioskJavaScriptInterface(this), "SmartKiosk")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                offlineContainer.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    offlineContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadContent() {
        if (prefs.mediaUrl.isNotEmpty()) {
            // Digital Signage Video Mode
            webView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            startExoPlayer(prefs.mediaUrl)
        } else {
            // Web Kiosk Mode
            playerView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.loadUrl(prefs.startUrl)
        }
    }

    private fun startExoPlayer(videoUrl: String) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
        }
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true
    }

    fun reloadWebView() {
        if (prefs.isClearCacheOnReload) {
            webView.clearCache(true)
        }
        webView.loadUrl(prefs.startUrl)
    }

    fun clearWebViewCache() {
        webView.clearCache(true)
        Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show()
    }

    private fun observeNetwork() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
        cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    if (offlineContainer.visibility == View.VISIBLE) {
                        reloadWebView()
                    }
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    offlineContainer.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV Remote Secret Combo to open Admin Panel (Press DPAD_UP 5 times within 3 seconds)
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            val now = System.currentTimeMillis()
            if (now - lastDpadTime < 1000) {
                dpadUpCounter++
            } else {
                dpadUpCounter = 1
            }
            lastDpadTime = now

            if (dpadUpCounter >= 5) {
                dpadUpCounter = 0
                promptAdminPin()
                return true
            }
        }

        // Intercept BACK button in Kiosk Mode
        if (keyCode == KeyEvent.KEYCODE_BACK && prefs.isKioskModeEnabled) {
            if (webView.canGoBack()) {
                webView.goBack()
            }
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun promptAdminPin() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("🔒 Код Администратора")
            .setMessage("Введите PIN-код для входа в настройки:")
            .setView(input)
            .setPositiveButton("Войти") { _, _ ->
                val pin = input.text.toString().trim()
                verifyAndOpenAdminDialog(pin)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun verifyAndOpenAdminDialog(pin: String) {
        if (pin == prefs.adminPin) {
            val dialog = AdminSettingsDialog(
                this,
                prefs,
                onSaveListener = {
                    loadContent()
                    if (prefs.isKioskModeEnabled) {
                        setupLockTaskMode()
                    } else {
                        stopLockTaskMode()
                    }
                },
                onExitKioskListener = {
                    prefs.isKioskModeEnabled = false
                    stopLockTaskMode()
                    finish()
                },
                onQuickReloadListener = {
                    reloadWebView()
                },
                onQuickClearCacheListener = {
                    clearWebViewCache()
                }
            )
            dialog.show()
        } else {
            Toast.makeText(this, "Неверный PIN-код!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideSystemUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
