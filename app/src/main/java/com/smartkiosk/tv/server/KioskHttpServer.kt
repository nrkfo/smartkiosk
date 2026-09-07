package com.smartkiosk.tv.server

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.smartkiosk.tv.data.PreferencesManager
import com.smartkiosk.tv.dpc.KioskAdminReceiver
import com.smartkiosk.tv.service.KioskWatchdogService
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
        fun onExitRequested()
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
                val rawIp = KioskWatchdogService.getLocalIpAddress(this@KioskHttpServer.context)
                val isEmulator = rawIp.startsWith("10.0.2.") || Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk")
                val webAdminUrl = if (isEmulator) "http://127.0.0.1:${prefs.serverPort}" else "http://$rawIp:${prefs.serverPort}"

                val json = """
                    {
                        "ipAddress": "$rawIp",
                        "isEmulator": $isEmulator,
                        "webAdminUrl": "$webAdminUrl",
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
                        sendBroadcast("com.smartkiosk.tv.ACTION_URL_CHANGED", "url", newUrl)
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
                    sendBroadcast("com.smartkiosk.tv.ACTION_RELOAD")
                }
                call.respondText("""{"status": "ok", "action": "reload"}""", ContentType.Application.Json)
            }

            // API: Clear Cache
            post("/api/clearcache") {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onClearCacheRequested()
                    sendBroadcast("com.smartkiosk.tv.ACTION_CLEAR_CACHE")
                }
                call.respondText("""{"status": "ok", "action": "clearcache"}""", ContentType.Application.Json)
            }

            // API: Exit App to Android TV Home Screen
            post("/api/exit") {
                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onExitRequested()
                    sendBroadcast("com.smartkiosk.tv.ACTION_EXIT_APP")
                }
                call.respondText("""{"status": "ok", "action": "exit"}""", ContentType.Application.Json)
            }
        }
    }

    private fun sendBroadcast(action: String, extraKey: String? = null, extraValue: String? = null) {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            if (extraKey != null && extraValue != null) {
                putExtra(extraKey, extraValue)
            }
        }
        context.sendBroadcast(intent)
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
            val rawIp = KioskWatchdogService.getLocalIpAddress(context)
            val isEmulator = rawIp.startsWith("10.0.2.") || Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk")

            val displayIp = if (isEmulator) "$rawIp (Внутренний IP эмулятора)" else rawIp
            val webAdminUrl = if (isEmulator) "http://127.0.0.1:${prefs.serverPort}" else "http://$rawIp:${prefs.serverPort}"
            val noteText = if (isEmulator) "💡 Вы запущены в эмуляторе. Для доступа с компьютера Mac переходите по ссылке <b>http://127.0.0.1:${prefs.serverPort}</b>. На реальном Smart TV здесь будет реальный IP вашей Wi-Fi сети." else "Подключайтесь с любого устройства в той же Wi-Fi/Ethernet сети."

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
                        button { background: #00E676; color: #000; border: none; padding: 12px 20px; margin-top: 16px; cursor: pointer; border-radius: 8px; font-size: 15px; font-weight: bold; transition: all 0.2s; }
                        button:hover { opacity: 0.9; transform: translateY(-1px); }
                        .btn-secondary { background: #3D5AFE; color: #fff; }
                        .btn-danger { background: #FF5252; color: #fff; }
                        .info-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                        .info-table td { padding: 10px; border-bottom: 1px solid #272732; font-size: 15px; }
                        .ip-highlight { color: #00E5FF; font-weight: bold; font-size: 16px; }
                        .url-highlight { color: #00E676; font-weight: bold; font-size: 16px; word-break: break-all; }
                        .note-banner { background: #1E1B4B; border: 1px solid #4338CA; padding: 14px; border-radius: 10px; margin-top: 16px; font-size: 14px; color: #C7D2FE; line-height: 1.5; }
                        .button-group { margin-top: 20px; display: flex; flex-wrap: wrap; gap: 10px; }
                        
                        /* Toast Banner */
                        #toast {
                            position: fixed;
                            top: 24px;
                            right: 24px;
                            background: #00E676;
                            color: #000;
                            padding: 16px 24px;
                            border-radius: 12px;
                            font-weight: bold;
                            font-size: 16px;
                            box-shadow: 0 10px 30px rgba(0,230,118,0.4);
                            display: none;
                            z-index: 9999;
                        }
                    </style>
                </head>
                <body>
                    <div id="toast">✅ Настройки успешно применены!</div>

                    <div class="card">
                        <h1>📺 Smart TV Kiosk Admin</h1>
                        <p>Управление киоском Smart TV в режиме реального времени</p>
                        
                        <form id="urlForm" onsubmit="submitUrlForm(event)">
                            <label for="url">Текущий URL веб-киоска:</label>
                            <input type="text" id="url" name="url" value="${prefs.startUrl}">
                            <button type="submit">Изменить URL на TV</button>
                        </form>
                        
                        <div class="button-group">
                            <button type="button" class="btn-secondary" onclick="sendAction('/api/reload', 'Страница перезагружается...')">🔄 Перезагрузить</button>
                            <button type="button" class="btn-secondary" onclick="sendAction('/api/clearcache', 'Кэш успешно очищен!')">🧹 Очистить кэш</button>
                            <button type="button" class="btn-danger" onclick="sendAction('/api/exit', 'Выход в меню Android TV...')">🚪 Выйти в меню Android TV</button>
                        </div>
                    </div>

                    <div class="card">
                        <h2>📊 Телеметрия устройства</h2>
                        <table class="info-table">
                            <tr><td><b>IP-адрес TV в сети:</b></td><td><span class="ip-highlight">$displayIp</span></td></tr>
                            <tr><td><b>Ссылка веб-админки:</b></td><td><span class="url-highlight">$webAdminUrl</span></td></tr>
                            <tr><td><b>Модель TV:</b></td><td>${Build.MANUFACTURER} ${Build.MODEL}</td></tr>
                            <tr><td><b>Android Version:</b></td><td>Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})</td></tr>
                            <tr><td><b>Device Owner Status:</b></td><td>${if (isOwner) "🟢 Активен (LockTask Mode)" else "⚠️ Нет прав Device Owner"}</td></tr>
                            <tr><td><b>Uptime (Время работы):</b></td><td>${SystemClock.elapsedRealtime() / 1000 / 60} мин</td></tr>
                        </table>
                        <div class="note-banner">$noteText</div>
                    </div>

                    <script>
                        function showToast(message, isError) {
                            const toast = document.getElementById('toast');
                            toast.innerText = message;
                            toast.style.background = isError ? '#FF5252' : '#00E676';
                            toast.style.color = isError ? '#FFF' : '#000';
                            toast.style.display = 'block';
                            setTimeout(function() {
                                toast.style.display = 'none';
                            }, 3500);
                        }

                        function submitUrlForm(event) {
                            event.preventDefault();
                            const newUrl = document.getElementById('url').value;
                            fetch('/api/url', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: 'url=' + encodeURIComponent(newUrl)
                            })
                            .then(function(res) { return res.json(); })
                            .then(function(data) {
                                showToast('✅ Новый URL успешно применен на TV!');
                            })
                            .catch(function(err) {
                                showToast('❌ Ошибка отправки!', true);
                            });
                        }

                        function sendAction(endpoint, successMessage) {
                            fetch(endpoint, { method: 'POST' })
                            .then(function(res) { return res.json(); })
                            .then(function(data) {
                                showToast('✅ ' + successMessage);
                            })
                            .catch(function(err) {
                                showToast('❌ Ошибка выполнения!', true);
                            });
                        }
                    </script>
                </body>
                </html>
            """.trimIndent()
        }
    }
}
