package com.smartkiosk.tv.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.smartkiosk.tv.R
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.service.KioskWatchdogService

class AdminSettingsDialog(
    context: Context,
    private val prefs: PreferencesManager,
    private val onSaveListener: () -> Unit,
    private val onExitKioskListener: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_admin_settings)

        val etUrl = findViewById<EditText>(R.id.et_start_url)
        val etPin = findViewById<EditText>(R.id.et_admin_pin)
        val cbKiosk = findViewById<CheckBox>(R.id.cb_kiosk_enabled)
        val cbAutoLaunch = findViewById<CheckBox>(R.id.cb_auto_launch)
        val tvNetworkInfo = findViewById<TextView>(R.id.tv_network_info)
        val btnSave = findViewById<Button>(R.id.btn_save_settings)
        val btnExit = findViewById<Button>(R.id.btn_exit_kiosk)

        // Populate existing preferences
        etUrl.setText(prefs.startUrl)
        etPin.setText(prefs.adminPin)
        cbKiosk.isChecked = prefs.isKioskModeEnabled
        cbAutoLaunch.isChecked = prefs.isAutoLaunchEnabled

        val ip = KioskWatchdogService.getLocalIpAddress(context)
        tvNetworkInfo.text = "Web Remote Admin: http://$ip:${prefs.serverPort}"

        btnSave.setOnClickListener {
            val newUrl = etUrl.text.toString().trim()
            val newPin = etPin.text.toString().trim()

            if (newUrl.isNotEmpty()) prefs.startUrl = newUrl
            if (newPin.isNotEmpty()) prefs.adminPin = newPin

            prefs.isKioskModeEnabled = cbKiosk.isChecked
            prefs.isAutoLaunchEnabled = cbAutoLaunch.isChecked

            Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            onSaveListener()
            dismiss()
        }

        btnExit.setOnClickListener {
            onExitKioskListener()
            dismiss()
        }
    }
}
