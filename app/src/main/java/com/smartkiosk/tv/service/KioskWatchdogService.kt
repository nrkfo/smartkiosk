package com.smartkiosk.tv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smartkiosk.tv.R
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.server.KioskHttpServer
import java.net.Inet4Address
import java.net.NetworkInterface

class KioskWatchdogService : Service() {

    private lateinit var prefs: PreferencesManager
    private var httpServer: KioskHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startHttpServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        httpServer?.stop()
        super.onDestroy()
    }

    private fun startHttpServer() {
        if (httpServer == null) {
            httpServer = KioskHttpServer(this, prefs)
            httpServer?.start()
        }
    }

    private fun buildNotification(): Notification {
        val ipAddress = getLocalIpAddress(this)
        val contentText = "Smart TV Kiosk Running | Web Admin: http://$ipAddress:${prefs.serverPort}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart TV Kiosk Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_kiosk_logo_neon)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Kiosk Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "smart_kiosk_channel"
        private const val NOTIFICATION_ID = 1001

        fun getLocalIpAddress(context: Context): String {
            try {
                val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ipInt = wifiInfo.ipAddress
                if (ipInt != 0) {
                    return String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }

                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val inetAddress = addresses.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            return inetAddress.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "127.0.0.1"
        }
    }
}
