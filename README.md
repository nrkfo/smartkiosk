# 📺 Smart TV Kiosk (Android TV)

<p align="center">
  <img src="https://img.shields.io/badge/Version-v1.0.0--beta.1-FF9800?style=for-the-badge&logo=github&logoColor=white" alt="Version" />
  <img src="https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android TV" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Server-Ktor%20CIO-087CFA?style=for-the-badge&logo=ktor&logoColor=white" alt="Ktor Server" />
  <img src="https://img.shields.io/badge/Media-AndroidX%20Media3%20ExoPlayer-FF4081?style=for-the-badge&logo=youtube&logoColor=white" alt="Media3 ExoPlayer" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

**Smart TV Kiosk** — это профессиональное Android TV приложение для трансформации любых телевизоров, дисплеев и интерактивных панелей под управлением **Android TV / Google TV / Fire TV** в надежные терминалы самообслуживания, рекламные медиа-стойки (Digital Signage) и веб-киоски.

---

## ✨ Ключевые возможности (Features)

### 🔒 1. Полная системная блокировка (Lock Task Mode / Device Owner)
- **Device Policy Controller (DPC)**: Интеграция с Android Device Policy Manager.
- **Блокировка системных клавиш**: Перехват и блокировка кнопок `Home`, `Back`, `Recents` и `Settings` на пультах Smart TV.
- **Блокировка шторки уведомлений**: Запрет вызова системного меню и настроек Android TV.

### 🌐 2. Полноэкранный Web Kiosk (HTML5 Engine)
- **Современный WebView**: Поддержка HTML5, JavaScript, DOM Storage, WebGL, IndexedDB.
- **Обход SSL-ошибок**: Автоматическое пропускирование предупреждений об SSL-сертификатах для локальных HTTPS серверов.
- **JavaScript Bridge (`window.SmartKiosk`)**: Управление функциями киоска прямо из веб-приложения (перезагрузка, очистка кэша, телеметрия).
- **Оффлайн-режим (Offline Fallback)**: Автоматическое обнаружение потери Wi-Fi/Ethernet и мягкое восстановление при появлении сети.

### 🎥 3. Digital Signage Media Player
- **Зацикленный проигрыватель**: Встроенный **AndroidX Media3 ExoPlayer** для зацикленного воспроизведения промо-видео роликов в 4K/1080p.

### 🌐 4. Удаленное управление и REST API (Embedded Web Admin)
- **Встроенный Ktor HTTP-сервер**: Приложение запускает собственный локальный веб-сервер (по умолчанию на порту `8080`).
- **Удаленная веб-панель**: Доступ с любого смартфона/ПК в той же сети для управления киоском.
- **REST API**:
  - `GET /` — Веб-панель администратора.
  - `GET /api/info` — Телеметрия TV (модель, версия Android, uptime, статус LockTask).
  - `POST /api/url` — Динамическая смена стартовой веб-страницы на ТВ.
  - `POST /api/reload` — Перезагрузка текущей страницы.
  - `POST /api/clearcache` — Очистка кэша браузера.

### 🕹️ 5. Smart TV Пульт & Защита PIN-кодом
- **Навигация DPAD**: Полный контроль фокуса для работы без сенсорного экрана.
- **4 Удобных способа вызова админки**: Кнопка `Menu (☰)`, 3x быстрое нажатие `Back (←)`, удерживание `OK`, плавающая иконка `⚙️`.
- **Автозапуск при включении (Boot Completed)**: Приложение и служба запускаются автоматически при подаче питания на телевизор.

---

## 💻 Системные требования (System Requirements)

| Параметр | Минимальные требования | Рекомендуемые требования |
| :--- | :--- | :--- |
| **Операционная система** | Android TV / Google TV / Fire OS 6.0+ (API level 23+) | Android TV 9.0+ / Google TV (API level 28+) |
| **ОЗУ (RAM)** | 1 ГБ | 2 ГБ и более (для 4K рендеринга и тяжелых HTML5 приложений) |
| **Память (Storage)** | 50 МБ свободного места | 200 МБ+ (для кэширования медиа) |
| **Архитектура ЦП** | ARM (arm64-v8a, armeabi-v7a), x86 / x86_64 | ARM64 Quad-Core / Octa-Core |
| **Сеть (Network)** | Wi-Fi (2.4 ГГц) или Ethernet (LAN) | Ethernet (LAN) 100/1000 Мбит или Wi-Fi 5 ГГц |
| **Управление** | Пульт ДУ (DPAD), мышь или клавиатура | Стандартный пульт ДУ с DPAD |
| **ADB отладка** | Требуется для активации Device Owner | Требуется для активации Device Owner |

---

## 🔖 История версий (Release History)

### `v1.0.0-beta.1` (Первый бета-релиз) — *07.09.2026*
- 🚀 Первый официальный бета-релиз системы **Smart TV Kiosk**.
- 🔒 Реализация DPC Device Owner и LockTask режима.
- 🌐 Встроенный Ktor HTTP-сервер на порту 8080 с веб-панелью управления и REST API.
- 🎨 Обновленный TV UI в стиле Neon SK с анимацией масштабирования фокуса с пульта.
- 🛡️ Автоматический обход SSL-предупреждений для локальных HTTPS серверов.
- 📺 Поддержка зацикленного видео-плеера Digital Signage (AndroidX Media3).

---

## 🚀 Быстрый старт и установка

### 1. Сборка проекта
Клонируйте репозиторий и соберите APK с помощью Gradle:

```bash
git clone https://github.com/nrkfo/smartkiosk.git
cd smartkiosk
./gradlew assembleDebug
```

Итоговый APK файл будет находиться по адресу:  
`app/build/outputs/apk/debug/app-debug.apk`

---

### 2. Установка на Android TV через ADB

Подключитесь к вашему Smart TV по сети:

```bash
adb connect <IP_АДРЕС_ТЕЛЕВИЗОРА>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### 3. Назначение прав Device Owner (Активация режима блокировки)

Для полной блокировки системных клавиш и обеспечения максимального уровня безопасности киоска назначьте приложение владельцем устройства:

```bash
adb shell dpm set-device-owner com.smartkiosk.tv/.dpc.KioskAdminReceiver
```

> [!IMPORTANT]
> Если на устройстве уже заведен профиль пользователя, перед выполнением команды сбросьте устройство до заводских настроек или удалите существующие аккаунты.

---

## 🛠️ Удаленное управление (Web Remote Admin)

После запуска приложения на телевизоре откройте браузер на компьютере или телефоне в той же сети:

```text
http://<IP_АДРЕС_ТЕЛЕВИЗОРА>:8080
```

---

## 📄 Лицензия

Проект распространяется под лицензией **MIT**. Вы можете свободно использовать и модифицировать код в коммерческих и личных целях.
