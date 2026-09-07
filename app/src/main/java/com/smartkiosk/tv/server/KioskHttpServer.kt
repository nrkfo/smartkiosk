package com.smartkiosk.tv.server

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.dpc.KioskAdminReceiver
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KioskHttpServer(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val listener: KioskServerListener? = null
) {

    interface KioskServerListener {
        fun onUrlChanged(newUrl: String)
        fun onReloadRequested()
        fun onClearCacheRequested()
    }

    private var server = embeddedServer(CIO, host = "0.0.0.0", port = prefs.serverPort) {
        routing {
            // Dashboard HTML Web UI
            get("/") {
                val html = getDashboardHtml(this@KioskHttpServer.context, prefs)
                call.respondText(html, ContentType.Text.Html)
            }

            // API: Info
            get("/api/info") {
                val isOwner = KioskAdminReceiver.isDeviceOwner(this@KioskHttpServer.context)
                val json = """
                    {
                        "model": "${Build.MANUFACTURER} ${Build.MODEL}",
                        "sdk": ${Build.VERSION.SDK_INT},
                        "startUrl": "${prefs.startUrl}",
                        "kioskEnabled": ${prefs.isKioskModeEnabled},
                        "isDeviceOwner": $isOwner,
                        "uptimeSeconds": ${SystemClock.elapsedRealtime() / 1000}
                    }
                """.trimIndent()
                call.respondText(json, ContentType.Application.Json)
            }

            // API: Change URL
            post("/api/url") {
                val params = call.receiveParameters()
                val newUrl = params["url"]
                if (!newUrl.isNullOrEmpty()) {
                    prefs.startUrl = newUrl
                    CoroutineScope(Dispatchers.Main).launch {
                        listener?.onUrlChanged(newUrl)
                    }
                    call.respondText("""{"status": "ok", "newUrl": "$newUrl"}""", ContentType.Application.Json)
                } else {
                    call.respondText("""{"status": "error", "message": "Missing 'url' parameter"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            // API: Reload
            post("/api/reload") {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onReloadRequested()
                }
                call.respondText("""{"status": "ok", "action": "reload"}""", ContentType.Application.Json)
            }

            // API: Clear Cache
            post("/api/clearcache") {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onClearCacheRequested()
                }
                call.respondText("""{"status": "ok", "action": "clearcache"}""", ContentType.Application.Json)
            }
        }
    }

    fun start() {
        try {
            server.start(wait = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            server.stop(1000, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun getDashboardHtml(context: Context, prefs: PreferencesManager): String {
            val isOwner = KioskAdminReceiver.isDeviceOwner(context)
            return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Smart TV Kiosk Admin Panel</title>
                    <style>
                        body { font-family: system-ui, -apple-system, sans-serif; margin: 0; padding: 24px; background: #0D0D10; color: #fff; }
                        .card { background: #16161D; border: 1px solid #272732; padding: 24px; border-radius: 16px; max-width: 640px; margin: 0 auto 24px auto; box-shadow: 0 8px 24px rgba(0,0,0,0.6); }
                        h1, h2 { color: #00E676; margin-top: 0; }
                        label { display: block; margin-top: 16px; font-weight: bold; color: #A1A1AA; }
                        input[type="text"] { width: 100%; padding: 14px; margin-top: 8px; box-sizing: border-box; background: #22222E; border: 1px solid #3F3F4E; color: #fff; border-radius: 8px; font-size: 16px; }
                        button { background: #00E676; color: #000; border: none; padding: 12px 20px; margin-top: 16px; cursor: pointer; border-radius: 8px; font-size: 15px; font-weight: bold; }
                        button:hover { background: #00C853; }
                        .btn-secondary { background: #3D5AFE; color: #fff; }
                        .btn-danger { background: #FF5252; color: #fff; }
                        .info-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                        .info-table td { padding: 10px; border-bottom: 1px solid #272732; font-size: 15px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>📺 Smart TV Kiosk Admin</h1>
                        <p>Управление киоском Smart TV в режиме реального времени</p>
                        
                        <form action="/api/url" method="post">
                            <label for="url">Текущий URL веб-киоска:</label>
                            <input type="text" id="url" name="url" value="${prefs.startUrl}">
                            <button type="submit">Изменить URL на TV</button>
                        </form>
                        
                        <div style="margin-top: 20px;">
                            <form action="/api/reload" method="post" style="display:inline;">
                                <button type="submit" class="btn-secondary">🔄 Перезагрузить страницу</button>
                            </form>
                            <form action="/api/clearcache" method="post" style="display:inline;">
                                <button type="submit" class="btn-secondary">🧹 Очистить кэш</button>
                            </form>
                        </div>
                    </div>

                    <div class="card">
                        <h2>📊 Телеметрия устройства</h2>
                        <table class="info-table">
                            <tr><td><b>Модель TV:</b></td><td>${Build.MANUFACTURER} ${Build.MODEL}</td></tr>
                            <tr><td><b>Android Version:</b></td><td>Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})</td></tr>
                            <tr><td><b>Device Owner Status:</b></td><td>${if (isOwner) "🟢 Активен (LockTask Mode)" else "⚠️ Нет прав Device Owner"}</td></tr>
                            <tr><td><b>Uptime (Время работы):</b></td><td>${SystemClock.elapsedRealtime() / 1000 / 60} мин</td></tr>
                            <tr><td><b>Порт управления:</b></td><td>${prefs.serverPort}</td></tr>
                        </table>
                    </div>
                </body>
                </html>
            """.trimIndent()
        }
    }
}
