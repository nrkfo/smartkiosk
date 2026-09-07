package com.smartkiosk.tv.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.smartkiosk.tv.R
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.dpc.KioskAdminReceiver
import com.smartkiosk.tv.service.KioskWatchdogService

class AdminSettingsDialog(
    context: Context,
    private val prefs: PreferencesManager,
    private val onSaveListener: () -> Unit,
    private val onExitKioskListener: () -> Unit,
    private val onQuickReloadListener: (() -> Unit)? = null,
    private val onQuickClearCacheListener: (() -> Unit)? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_admin_settings)

        // Make window background transparent so rounded dialog shape shows properly
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etUrl = findViewById<EditText>(R.id.et_start_url)
        val etPin = findViewById<EditText>(R.id.et_admin_pin)
        val cbKiosk = findViewById<CheckBox>(R.id.cb_kiosk_enabled)
        val cbAutoLaunch = findViewById<CheckBox>(R.id.cb_auto_launch)
        val cbBlockDownloads = findViewById<CheckBox>(R.id.cb_block_downloads)
        val cbDisableSelection = findViewById<CheckBox>(R.id.cb_disable_selection)
        val tvNetworkInfo = findViewById<TextView>(R.id.tv_network_info)
        val tvDeviceOwnerBadge = findViewById<TextView>(R.id.tv_device_owner_badge)
        val btnQuickReload = findViewById<Button>(R.id.btn_quick_reload)
        val btnQuickClearCache = findViewById<Button>(R.id.btn_quick_clear_cache)
        val btnSave = findViewById<Button>(R.id.btn_save_settings)
        val btnExit = findViewById<Button>(R.id.btn_exit_kiosk)

        // Populate existing preferences
        etUrl.setText(prefs.startUrl)
        etPin.setText(prefs.adminPin)
        cbKiosk.isChecked = prefs.isKioskModeEnabled
        cbAutoLaunch.isChecked = prefs.isAutoLaunchEnabled
        cbBlockDownloads.isChecked = prefs.isBlockDownloads
        cbDisableSelection.isChecked = prefs.isDisableTextSelection

        val isOwner = KioskAdminReceiver.isDeviceOwner(context)
        if (isOwner) {
            tvDeviceOwnerBadge.text = "🟢 LockTask Active"
            tvDeviceOwnerBadge.setTextColor(0xFF00E676.toInt())
        } else {
            tvDeviceOwnerBadge.text = "⚠️ Device Owner Inactive"
            tvDeviceOwnerBadge.setTextColor(0xFFFFD54F.toInt())
        }

        val ip = KioskWatchdogService.getLocalIpAddress(context)
        tvNetworkInfo.text = "🌐 Web Remote Admin: http://$ip:${prefs.serverPort}"

        // Quick Actions
        btnQuickReload.setOnClickListener {
            onQuickReloadListener?.invoke()
            Toast.makeText(context, "Перезагрузка страницы...", Toast.LENGTH_SHORT).show()
        }

        btnQuickClearCache.setOnClickListener {
            onQuickClearCacheListener?.invoke()
            Toast.makeText(context, "Кэш очищен", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val newUrl = etUrl.text.toString().trim()
            val newPin = etPin.text.toString().trim()

            if (newUrl.isNotEmpty()) prefs.startUrl = newUrl
            if (newPin.isNotEmpty()) prefs.adminPin = newPin

            prefs.isKioskModeEnabled = cbKiosk.isChecked
            prefs.isAutoLaunchEnabled = cbAutoLaunch.isChecked
            prefs.isBlockDownloads = cbBlockDownloads.isChecked
            prefs.isDisableTextSelection = cbDisableSelection.isChecked

            Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            onSaveListener()
            dismiss()
        }

        btnExit.setOnClickListener {
            onExitKioskListener()
            dismiss()
        }

        // Apply TV Focus scale animation
        val focusableViews = listOf(
            btnQuickReload, btnQuickClearCache, btnSave, btnExit,
            etUrl, etPin, cbKiosk, cbAutoLaunch, cbBlockDownloads, cbDisableSelection
        )
        for (view in focusableViews) {
            applyTvFocusAnimation(view)
        }
    }

    private fun applyTvFocusAnimation(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }
        }
    }
}
