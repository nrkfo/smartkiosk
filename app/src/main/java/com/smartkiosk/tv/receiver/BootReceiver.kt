package com.smartkiosk.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.service.KioskWatchdogService
import com.smartkiosk.tv.ui.KioskActivity

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action ||
            "android.intent.action.QUICKBOOT_POWERON" == action ||
            "com.htc.intent.action.QUICKBOOT_POWERON" == action
        ) {
            val prefs = PreferencesManager(context)
            if (prefs.isAutoLaunchEnabled) {
                // Start Kiosk Watchdog Service
                val serviceIntent = Intent(context, KioskWatchdogService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Launch Kiosk Main Activity
                val activityIntent = Intent(context, KioskActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(activityIntent)
            }
        }
    }
}
