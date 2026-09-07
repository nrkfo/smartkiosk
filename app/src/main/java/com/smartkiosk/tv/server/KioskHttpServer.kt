package com.smartkiosk.tv.server

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.StatFs
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
        fun onSettingsChanged()
    }

    private var server = embeddedServer(CIO, host = "0.0.0.0", port = prefs.serverPort) {
        routing {
            // Dashboard HTML Web UI
            get("/") {
                val html = getDashboardHtml(this@KioskHttpServer.context, prefs)
                call.respondText(html, ContentType.Text.Html)
            }

            // API: Info & All Settings
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
                        "adminPin": "${prefs.adminPin}",
                        "mediaUrl": "${prefs.mediaUrl}",
                        "serverPort": ${prefs.serverPort},
                        "pageZoom": ${prefs.pageZoomPercent},
                        "scheduledReloadHour": ${prefs.scheduledReloadHour},
                        "scheduledReloadEnabled": ${prefs.isScheduledReloadEnabled},
                        "clearCacheOnReload": ${prefs.isClearCacheOnReload},
                        "kioskEnabled": ${prefs.isKioskModeEnabled},
                        "autoLaunchEnabled": ${prefs.isAutoLaunchEnabled},
                        "blockDownloads": ${prefs.isBlockDownloads},
                        "disableTextSelection": ${prefs.isDisableTextSelection},
                        "ram": "${getRamInfo(this@KioskHttpServer.context)}",
                        "storage": "${getStorageInfo(this@KioskHttpServer.context)}",
                        "wifiSignal": "${getWifiSignalInfo(this@KioskHttpServer.context)}",
                        "isDeviceOwner": $isOwner,
                        "uptimeSeconds": ${SystemClock.elapsedRealtime() / 1000}
                    }
                """.trimIndent()
                call.respondText(json, ContentType.Application.Json)
            }

            // API: Update All Settings
            post("/api/settings") {
                val params = call.receiveParameters()
                val newUrl = params["startUrl"]
                val newPin = params["adminPin"]
                val newMediaUrl = params["mediaUrl"]
                val newPort = params["serverPort"]?.toIntOrNull()
                val newZoom = params["pageZoomPercent"]?.toIntOrNull()
                val newReloadHour = params["scheduledReloadHour"]?.toIntOrNull()
                val kioskEnabled = params["kioskEnabled"]?.toBoolean()
                val autoLaunchEnabled = params["autoLaunchEnabled"]?.toBoolean()
                val clearCacheOnReload = params["clearCacheOnReload"]?.toBoolean()
                val scheduledReloadEnabled = params["scheduledReloadEnabled"]?.toBoolean()
                val blockDownloads = params["blockDownloads"]?.toBoolean()
                val disableTextSelection = params["disableTextSelection"]?.toBoolean()

                if (!newUrl.isNullOrEmpty()) prefs.startUrl = newUrl
                if (!newPin.isNullOrEmpty()) prefs.adminPin = newPin
                if (newMediaUrl != null) prefs.mediaUrl = newMediaUrl
                if (newPort != null && newPort in 1024..65535) prefs.serverPort = newPort
                if (newZoom != null && newZoom in 50..300) prefs.pageZoomPercent = newZoom
                if (newReloadHour != null && newReloadHour in 0..23) prefs.scheduledReloadHour = newReloadHour
                if (kioskEnabled != null) prefs.isKioskModeEnabled = kioskEnabled
                if (autoLaunchEnabled != null) prefs.isAutoLaunchEnabled = autoLaunchEnabled
                if (clearCacheOnReload != null) prefs.isClearCacheOnReload = clearCacheOnReload
                if (scheduledReloadEnabled != null) prefs.isScheduledReloadEnabled = scheduledReloadEnabled
                if (blockDownloads != null) prefs.isBlockDownloads = blockDownloads
                if (disableTextSelection != null) prefs.isDisableTextSelection = disableTextSelection

                CoroutineScope(Dispatchers.Main).launch {
                    listener?.onSettingsChanged()
                    sendBroadcast("com.smartkiosk.tv.ACTION_URL_CHANGED")
                }
                call.respondText("""{"status": "ok", "message": "Settings updated"}""", ContentType.Application.Json)
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
        fun getRamInfo(context: Context): String {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                am.getMemoryInfo(memInfo)
                val availGb = String.format("%.1f", memInfo.availMem / 1073741824.0)
                val totalGb = String.format("%.1f", memInfo.totalMem / 1073741824.0)
                return "$availGb GB / $totalGb GB свободно"
            } catch (e: Exception) {
                return "Н/Д"
            }
        }

        fun getStorageInfo(context: Context): String {
            try {
                val stat = StatFs(context.filesDir.absolutePath)
                val availGb = String.format("%.1f", stat.availableBytes / 1073741824.0)
                val totalGb = String.format("%.1f", stat.totalBytes / 1073741824.0)
                return "$availGb GB / $totalGb GB свободно"
            } catch (e: Exception) {
                return "Н/Д"
            }
        }

        fun getWifiSignalInfo(context: Context): String {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val rssi = wifiManager.connectionInfo.rssi
                if (rssi == 0 || rssi < -120) return "LAN (Ethernet)"
                return when {
                    rssi >= -55 -> "$rssi dBm (🟢 Отличный)"
                    rssi >= -70 -> "$rssi dBm (🟡 Хороший)"
                    else -> "$rssi dBm (🔴 Слабый)"
                }
            } catch (e: Exception) {
                return "Н/Д"
            }
        }

        fun getDashboardHtml(context: Context, prefs: PreferencesManager): String {
            val isOwner = KioskAdminReceiver.isDeviceOwner(context)
            val rawIp = KioskWatchdogService.getLocalIpAddress(context)
            val isEmulator = rawIp.startsWith("10.0.2.") || Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk")

            val displayIp = if (isEmulator) "$rawIp (Внутренний IP эмулятора)" else rawIp
            val webAdminUrl = if (isEmulator) "http://127.0.0.1:${prefs.serverPort}" else "http://$rawIp:${prefs.serverPort}"
            val noteText = if (isEmulator) "💡 Вы запущены в эмуляторе. Для доступа с компьютера Mac переходите по ссылке <b>http://127.0.0.1:${prefs.serverPort}</b>. На реальном Smart TV здесь будет реальный IP вашей Wi-Fi сети." else "Подключайтесь с любого устройства в той же Wi-Fi/Ethernet сети."

            val isFirstLaunch = prefs.startUrl == PreferencesManager.DEFAULT_START_URL

            val ramStr = getRamInfo(context)
            val storageStr = getStorageInfo(context)
            val wifiStr = getWifiSignalInfo(context)

            val welcomeBannerHtml = if (isFirstLaunch) """
                <div class="card welcome-card">
                    <h1>👋 Добро пожаловать в Smart TV Kiosk!</h1>
                    <p>Для первой настройки открытого сайта введите адрес в поле ниже или откройте этот веб-интерфейс с ПК/смартфона по ссылке:</p>
                    <div class="ip-box">$webAdminUrl</div>
                    <p class="sub-note">Или нажмите на пульте ТВ <b>Menu (☰)</b> или <b>3x Назад (←)</b> для вызова скрытых настроек.</p>
                </div>
            """.trimIndent() else ""

            return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Smart TV Kiosk Admin Panel</title>
                    <style>
                        body { font-family: system-ui, -apple-system, sans-serif; margin: 0; padding: 24px; background: #0D0D10; color: #fff; }
                        .card { background: #16161D; border: 1px solid #272732; padding: 24px; border-radius: 16px; max-width: 680px; margin: 0 auto 24px auto; box-shadow: 0 8px 24px rgba(0,0,0,0.6); }
                        .welcome-card { background: #1E1B4B; border: 1px solid #6366F1; text-align: center; }
                        .welcome-card h1 { color: #818CF8; }
                        .ip-box { background: #0F172A; border: 1px solid #38BDF8; color: #38BDF8; font-size: 24px; font-weight: bold; padding: 16px; border-radius: 12px; margin: 16px 0; word-break: break-all; }
                        .sub-note { color: #C7D2FE; font-size: 14px; }
                        h1, h2 { color: #00E676; margin-top: 0; }
                        label { display: block; margin-top: 16px; font-weight: bold; color: #A1A1AA; }
                        input[type="text"], input[type="password"], select { width: 100%; padding: 14px; margin-top: 8px; box-sizing: border-box; background: #22222E; border: 1px solid #3F3F4E; color: #fff; border-radius: 8px; font-size: 16px; }
                        .checkbox-label { display: flex; align-items: center; gap: 10px; margin-top: 14px; cursor: pointer; color: #FFF; font-size: 15px; }
                        .checkbox-label input[type="checkbox"] { width: 20px; height: 20px; accent-color: #00E676; }
                        button { background: #00E676; color: #000; border: none; padding: 14px 24px; margin-top: 20px; cursor: pointer; border-radius: 8px; font-size: 16px; font-weight: bold; transition: all 0.2s; width: 100%; }
                        button:hover { opacity: 0.9; transform: translateY(-1px); }
                        .btn-secondary { background: #3D5AFE; color: #fff; width: auto; margin-top: 0; }
                        .btn-danger { background: #FF5252; color: #fff; width: auto; margin-top: 0; }
                        .info-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                        .info-table td { padding: 10px; border-bottom: 1px solid #272732; font-size: 15px; }
                        .ip-highlight { color: #00E5FF; font-weight: bold; font-size: 16px; }
                        .url-highlight { color: #00E676; font-weight: bold; font-size: 16px; word-break: break-all; }
                        .note-banner { background: #1E1B4B; border: 1px solid #4338CA; padding: 14px; border-radius: 10px; margin-top: 16px; font-size: 14px; color: #C7D2FE; line-height: 1.5; }
                        .button-group { margin-top: 24px; display: flex; flex-wrap: wrap; gap: 10px; }
                        
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

                    $welcomeBannerHtml

                    <div class="card">
                        <h1>⚙️ Панель Управления Smart TV Kiosk</h1>
                        
                        <form id="settingsForm" onsubmit="submitAllSettings(event)">
                            <label for="startUrl">Адрес стартовой веб-страницы (Start URL):</label>
                            <input type="text" id="startUrl" name="startUrl" value="${prefs.startUrl}">

                            <label for="adminPin">PIN-код администратора:</label>
                            <input type="password" id="adminPin" name="adminPin" value="${prefs.adminPin}">

                            <label for="mediaUrl">Digital Signage URL (видео-реклама, опционально):</label>
                            <input type="text" id="mediaUrl" name="mediaUrl" value="${prefs.mediaUrl}">

                            <label for="serverPort">Порт веб-сервера управления (по умолчанию 8080):</label>
                            <input type="text" id="serverPort" name="serverPort" value="${prefs.serverPort}">

                            <label for="pageZoomPercent">Масштабирование страниц (Page Zoom):</label>
                            <select id="pageZoomPercent" name="pageZoomPercent">
                                <option value="80" ${if (prefs.pageZoomPercent == 80) "selected" else ""}>80%</option>
                                <option value="100" ${if (prefs.pageZoomPercent == 100) "selected" else ""}>100% (По умолчанию)</option>
                                <option value="125" ${if (prefs.pageZoomPercent == 125) "selected" else ""}>125%</option>
                                <option value="150" ${if (prefs.pageZoomPercent == 150) "selected" else ""}>150%</option>
                                <option value="200" ${if (prefs.pageZoomPercent == 200) "selected" else ""}>200%</option>
                            </select>

                            <label for="scheduledReloadHour">Час ночной автоперезагрузки страницы (0-23 ч):</label>
                            <select id="scheduledReloadHour" name="scheduledReloadHour">
                                <option value="1" ${if (prefs.scheduledReloadHour == 1) "selected" else ""}>01:00 AM</option>
                                <option value="2" ${if (prefs.scheduledReloadHour == 2) "selected" else ""}>02:00 AM</option>
                                <option value="3" ${if (prefs.scheduledReloadHour == 3) "selected" else ""}>03:00 AM (По умолчанию)</option>
                                <option value="4" ${if (prefs.scheduledReloadHour == 4) "selected" else ""}>04:00 AM</option>
                                <option value="5" ${if (prefs.scheduledReloadHour == 5) "selected" else ""}>05:00 AM</option>
                            </select>

                            <label class="checkbox-label">
                                <input type="checkbox" id="kioskEnabled" ${if (prefs.isKioskModeEnabled) "checked" else ""}>
                                🔒 Режим Киоска (LockTask Mode)
                            </label>

                            <label class="checkbox-label">
                                <input type="checkbox" id="autoLaunchEnabled" ${if (prefs.isAutoLaunchEnabled) "checked" else ""}>
                                🚀 Автозапуск при включении Smart TV
                            </label>

                            <label class="checkbox-label">
                                <input type="checkbox" id="clearCacheOnReload" ${if (prefs.isClearCacheOnReload) "checked" else ""}>
                                🧹 Авто-очистка кэша при перезагрузке
                            </label>

                            <label class="checkbox-label">
                                <input type="checkbox" id="scheduledReloadEnabled" ${if (prefs.isScheduledReloadEnabled) "checked" else ""}>
                                🌙 Ночная плановая автоперезагрузка
                            </label>

                            <label class="checkbox-label">
                                <input type="checkbox" id="blockDownloads" ${if (prefs.isBlockDownloads) "checked" else ""}>
                                🛡️ Блокировать скачивание файлов (.apk, .pdf)
                            </label>

                            <label class="checkbox-label">
                                <input type="checkbox" id="disableTextSelection" ${if (prefs.isDisableTextSelection) "checked" else ""}>
                                🚫 Запретить выделение текста на страницах
                            </label>

                            <button type="submit">💾 Сохранить и применить все настройки</button>
                        </form>
                        
                        <div class="button-group">
                            <button type="button" class="btn-secondary" onclick="sendAction('/api/reload', 'Страница перезагружается...')">🔄 Перезагрузить</button>
                            <button type="button" class="btn-secondary" onclick="sendAction('/api/clearcache', 'Кэш успешно очищен!')">🧹 Очистить кэш</button>
                            <button type="button" class="btn-danger" onclick="sendAction('/api/exit', 'Выход в меню Android TV...')">🚪 Выйти в меню Android TV</button>
                        </div>
                    </div>

                    <div class="card">
                        <h2>📊 Телеметрия и мониторинг системы</h2>
                        <table class="info-table">
                            <tr><td><b>IP-адрес TV в сети:</b></td><td><span class="ip-highlight">$displayIp</span></td></tr>
                            <tr><td><b>Ссылка веб-админки:</b></td><td><span class="url-highlight">$webAdminUrl</span></td></tr>
                            <tr><td><b>Сигнал Wi-Fi / Сеть:</b></td><td><span style="color:#00E5FF; font-weight:bold;">$wifiStr</span></td></tr>
                            <tr><td><b>Оперативная память (RAM):</b></td><td>$ramStr</td></tr>
                            <tr><td><b>Накопитель (Flash Storage):</b></td><td>$storageStr</td></tr>
                            <tr><td><b>Ночная автоперезагрузка:</b></td><td>${if (prefs.isScheduledReloadEnabled) "Включена (${prefs.scheduledReloadHour}:00 AM)" else "Отключена"}</td></tr>
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

                        function submitAllSettings(event) {
                            event.preventDefault();
                            const body = new URLSearchParams();
                            body.append('startUrl', document.getElementById('startUrl').value);
                            body.append('adminPin', document.getElementById('adminPin').value);
                            body.append('mediaUrl', document.getElementById('mediaUrl').value);
                            body.append('serverPort', document.getElementById('serverPort').value);
                            body.append('pageZoomPercent', document.getElementById('pageZoomPercent').value);
                            body.append('scheduledReloadHour', document.getElementById('scheduledReloadHour').value);
                            body.append('kioskEnabled', document.getElementById('kioskEnabled').checked);
                            body.append('autoLaunchEnabled', document.getElementById('autoLaunchEnabled').checked);
                            body.append('clearCacheOnReload', document.getElementById('clearCacheOnReload').checked);
                            body.append('scheduledReloadEnabled', document.getElementById('scheduledReloadEnabled').checked);
                            body.append('blockDownloads', document.getElementById('blockDownloads').checked);
                            body.append('disableTextSelection', document.getElementById('disableTextSelection').checked);

                            fetch('/api/settings', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: body.toString()
                            })
                            .then(function(res) { return res.json(); })
                            .then(function(data) {
                                showToast('✅ Все настройки успешно сохранены и применены на TV!');
                            })
                            .catch(function(err) {
                                showToast('❌ Ошибка сохранения!', true);
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
