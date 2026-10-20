# CastBeam 🎬📡

**CastBeam** transforms your Android smartphone into a high-performance, standalone local Wi-Fi media server. It allows anyone on the same network to stream movies, videos, music, and files directly to Smart TVs (Samsung Tizen, LG webOS, Android TV, Fire TV, Apple TV) and web browsers without third-party cloud services or an active internet connection.

---

## 🌟 Key Features

- **🚀 Embedded HTTP 1.1 Server**:
  - Multi-threaded, lightweight server running directly on Android.
  - Native RFC 7233 **HTTP Byte-Range (`206 Partial Content`)** streaming engine, enabling instant fast-forwarding, rewinding, and timeline seeking on multi-gigabyte 1080p and 4K movie files.
- **📺 Smart TV Cinema View (`http://<phone-ip>:8080`)**:
  - 10-foot leanback interface designed specifically for TV screens.
  - Navigable with standard TV physical remotes (D-pad arrow keys, Enter/OK, Space, F for fullscreen).
  - High-contrast visual cards with movie duration and file size badges.
- **💬 Subtitle Integration**:
  - Automatically indexes matching sidecar subtitle files (`.srt` and `.vtt`).
  - Converts SubRip (`.srt`) to WebVTT on the fly for native HTML5 video player subtitle rendering.
- **📱 Dual-Mode TV Remote Controller**:
  - Built-in phone remote deck with a large tactile Play/Pause button, timeline scrub slider, ±10-second quick skips, volume slider with mute toggle, and fullscreen triggers.
  - Bidirectional real-time state synchronization between the TV screen and the phone.
- **📁 Storage Access Framework (SAF)**:
  - Expose any selected phone folder (e.g., `Downloads`, `Movies`, custom folders) with persisted URI permissions.
  - Built-in demo media samples for immediate first-launch testing.
- **⚡ Foreground Service with WakeLock**:
  - Uses `PARTIAL_WAKE_LOCK` and `WifiLock` via an Android Foreground Service so long movies stream smoothly without interruption when the phone screen turns off.
- **📶 Instant QR Code Connect**:
  - On-device QR code generator rendered on Compose Canvas. Point your Smart TV camera or laptop to connect instantly without typing IP addresses.
- **📤 Web File Manager & Uploader**:
  - Category filters (`Movies`, `Audio`, `Photos`, `Documents`).
  - Live search bar and direct downloads.
  - Upload files directly from your computer browser back to your phone storage.

---

## 🎮 Smart TV Remote Keymap

When browsing or streaming on a Smart TV or PC browser:

| Key | Action |
|---|---|
| <kbd>▲</kbd> <kbd>▼</kbd> <kbd>◄</kbd> <kbd>►</kbd> | Navigate movie cards / Adjust volume (<kbd>▲</kbd>/<kbd>▼</kbd>) |
| <kbd>Enter</kbd> / <kbd>OK</kbd> | Select movie / Open player / Play & Pause |
| <kbd>Space</kbd> | Toggle Play / Pause |
| <kbd>◄</kbd> / <kbd>►</kbd> *(in player)* | Seek backward / forward 10 seconds |
| <kbd>F</kbd> | Toggle Fullscreen |
| <kbd>Esc</kbd> / <kbd>Back</kbd> | Close player and return to catalog |

---

## 📡 HTTP Endpoints & API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/` | `GET` | Serves the Cinema Web Dashboard & TV Player |
| `/stream?id=<id>` | `GET` | Seekable media stream with `206 Partial Content` support |
| `/download?id=<id>` | `GET` | Direct download attachment for the file |
| `/subtitles?id=<id>` | `GET` | Serves `.vtt` subtitles (auto-converted from `.srt`) |
| `/api/media` | `GET` | Returns JSON catalog of all indexed media items |
| `/api/folders` | `GET` | Returns list of exposed storage folders |
| `/api/status` | `GET` | Server telemetry (active clients, bytes sent/received) |
| `/api/remote/state` | `GET` | Polled by TV player for phone commands |
| `/api/remote/command` | `POST` | Dispatches remote actions (`play`, `pause`, `seek`, `volume`, etc.) |
| `/api/remote/status` | `POST` | TV player heartbeat updating live playback position |
| `/api/upload` | `POST` | Accepts file uploads from browser to phone |

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3.
- **Theme**: Cinema Dark (`#090D16` Midnight Navy, `#38BDF8` Cyan, `#F59E0B` Amber).
- **Concurrency**: Kotlin Coroutines (`Dispatchers.IO`) & StateFlow.
- **Local Persistence**: Android `SharedPreferences` + `DocumentsContract`.
- **Background Persistence**: Android Foreground Service with ongoing notification and CPU/Wi-Fi locks.
- **Embedded Web Engine**: Zero-dependency Kotlin/Java socket server with zero external framework overhead.
- **Testing**: Robolectric local JVM testing suite.

---

## 🚀 Getting Started

### 1. Build & Run
Run the unit tests and assemble the application using Gradle:
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

### 2. How to Use
1. **Connect to Wi-Fi**: Ensure your Android phone and your Smart TV (or laptop) are connected to the same Wi-Fi router or mobile hotspot.
2. **Launch CastBeam**: The server starts automatically and displays your local IP (e.g., `http://192.168.1.15:8080`).
3. **Select Folders**: Tap **Select Folder** in the *Library* tab to add your movie download directory.
4. **Open on TV**: Open the web browser on your TV (or scan the QR code).
5. **Enjoy the Show**: Click any movie to stream in full screen, and use your phone as a remote control!
