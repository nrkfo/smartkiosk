package com.smartkiosk.tv.dpc

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class KioskAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Kiosk Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Kiosk Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, KioskAdminReceiver::class.java)
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isDeviceOwnerApp(context.packageName)
        }

        fun configureKioskLockTask(context: Context, enable: Boolean) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val compName = getComponentName(context)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                try {
                    if (enable) {
                        dpm.setLockTaskPackages(compName, arrayOf(context.packageName))
                    } else {
                        dpm.setLockTaskPackages(compName, arrayOf())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
