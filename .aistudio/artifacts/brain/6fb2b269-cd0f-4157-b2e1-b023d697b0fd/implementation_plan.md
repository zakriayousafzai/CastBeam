# CastBeam - Local Wi-Fi Media Server & TV Streaming

CastBeam transforms an Android phone into a high-performance local Wi-Fi media server, allowing anyone on the same network to stream movies, videos, music, and files directly to Smart TVs, laptops, or tablets without third-party cloud services or internet usage. It features an embedded HTTP server with seekable byte-range streaming, a TV Cinema web dashboard, and bidirectional remote playback controls.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been aligned based on your preferences:

- **Confirmed Decision 1 (Web Interface)**: **TV Cinema View with Remote Controls & File Manager** — The browser interface served on the local network features a 10-foot Smart TV interface navigable via remote/D-pad/arrow keys, paired with a web file manager for desktop/mobile browsers.
- **Confirmed Decision 2 (Network Access)**: **Completely Open Access** — No login or PIN required. Any device on the same local Wi-Fi network (TV, laptop, friend's phone) can immediately access and stream media by opening the URL or scanning the QR code.
- **Confirmed Decision 3 (Remote Playback)**: **Dual Mode** — The phone features a dedicated TV Remote Control deck that synchronizes in real time with the TV player, while the TV browser maintains full independent on-screen playback controls.

---

## 1. Overview & Core Concept

### What It Does
1. **Folder Selection & Media Indexing**: The user selects one or more device storage folders (e.g., Downloads, Movies, Videos) using Android's Storage Access Framework. The app indexes movies, videos, audio, images, and documents.
2. **Local Wi-Fi Server**: An embedded HTTP server runs directly on the Android device, broadcasting over the local subnet (e.g., `http://192.168.1.45:8080`).
3. **Smart TV & Web Streaming**: TV web browsers (LG webOS, Samsung Tizen, Android TV, Fire TV, Apple TV browsers) or PC/Mac browsers open the web dashboard.
4. **Seekable Video Streaming**: Full support for HTTP 206 Partial Content (Byte-Range requests), enabling instant fast-forwarding, rewinding, and chapter jumping on multi-gigabyte 1080p/4K movie files.
5. **Bidirectional Remote Control**: A real-time command channel lets the phone pause, resume, seek, change volume, toggle subtitles, and switch media on the TV screen.
6. **Continuous Playback Support**: Foreground service with wake lock and Wi-Fi lock keeps the server running seamlessly even when the phone screen is turned off.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Phone User Journey                              │
│                                                                        │
│ 1. Launch App ──► 2. Pick Folder(s) ──► 3. Start Server ──► 4. Connect │
│    (Auto-detects     (Persisted URIs,      (Foreground Svc,   (Show QR │
│     Wi-Fi IP)         auto-indexes)         WakeLock on)       & URL)  │
│                                                                   │    │
│ 5. Remote Deck ◄──────────────────────────────────────────────────┘    │
│    (Controls TV playback: play/pause, scrub bar, volume, skip 10s)     │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                        TV / Client Journey                             │
│                                                                        │
│ 1. Scan QR / Type URL ──► 2. TV Cinema Dashboard ──► 3. Stream Movie   │
│    (Instant load, no         (Grid view of movies,      (HTML5 Video,  │
│     login required)           folders, thumbnails)       Byte-Range)   │
│                                     │                         ▲        │
│                                     └─► Bidirectional Sync ───┘        │
│                                         (Reacts to Phone Remote)       │
└────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Theme**: *Cinema Dark* — Deep midnight navy (`#0B0F19`), rich surface layers (`#131B2E`), subtle cyan/electric blue accents (`#38BDF8`), and warm amber highlights (`#F59E0B`).
- **Mobile App Layout**:
  - **Top Bar**: Wi-Fi connection badge (SSID & local IP), quick server toggle switch.
  - **Hero Server Card**: Prominent IP address (`http://192.168.x.x:8080`), server status pulse indicator, Quick QR Code modal dialog, and Share/Copy buttons.
  - **Tab Navigation**:
    1. *Dashboard*: Server status, network speeds, active streaming clients, quick statistics.
    2. *Folders & Library*: Picked storage folders, indexed video/audio/file browser, quick-play options.
    3. *TV Remote*: Real-time remote controller showing currently playing media on TV, time scrub bar, play/pause, volume slider, jump ±10s/±30s, and aspect ratio/fullscreen triggers.
    4. *Server Logs & Settings*: Port configuration (default 8080), wake-lock toggle, active connection monitor, live request log.
- **TV Web Dashboard Interface**:
  - **TV Mode**: 10-foot UI with large movie cards, clear focus states for TV remotes, high-contrast typography, and full-screen video player with on-screen TV controls.
  - **Desktop / Mobile Web Mode**: Responsive grid, category filters (All, Movies, Music, Pictures, Documents), file search bar, sorting, download button, and file uploader.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Embedded HTTP Server Engine
- **Chosen Approach**: Custom multi-threaded HTTP server utilizing Android's standard `java.net.ServerSocket` and coroutine-backed worker threads, directly reading from Android `ContentResolver` streams.
- **Why**: Zero external bloated server dependencies, highly resilient on Android, completely self-contained, and allows precise implementation of RFC 7233 HTTP Byte-Range requests (`bytes=start-end`), which is mandatory for HTML5 video players on Smart TVs to seek and buffer smoothly.
- **Alternatives Considered**: NanoHTTPD (often lacks clean async byte-range chunking for huge files), external web frameworks (add unnecessary APK bloat and potential binary incompatibilities).

### 2. Remote Control Synchronization Protocol
- **Chosen Approach**: Lightweight HTTP command API + Server-Sent Events (SSE) / fast polling state bridge.
- **Why**: Works reliably across every smart TV browser without requiring complex WebSocket proxy configurations or browser extensions.
- **Alternatives Considered**: WebRTC (overly complex for simple remote playback signals), Bluetooth HID (requires proximity and pairing pairing friction).

### 3. Background Persistence
- **Chosen Approach**: Android Foreground Service with `FOREGROUND_SERVICE_DATA_SYNC` / `MEDIA_PLAYBACK`, accompanied by `PARTIAL_WAKE_LOCK` and `WifiLock`.
- **Why**: Android aggressively puts background apps to sleep. A foreground service with wake lock ensures a 2.5-hour 4K movie does not pause when the phone screen turns off.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Android Application                             │
│                                                                        │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │                 Jetpack Compose UI Layer                     │     │
│   │  [ServerCard]  [LibraryScreen]  [RemoteControl]  [LogsView]  │     │
│   └──────────────────────────────┬───────────────────────────────┘     │
│                                  │ StateFlow                           │
│   ┌──────────────────────────────▼───────────────────────────────┐     │
│   │                   MediaServerViewModel                       │     │
│   └──────┬───────────────────────┬───────────────────────────────┘     │
│          │                       │                                     │
│   ┌──────▼──────────────┐ ┌──────▼───────────────────────────────┐     │
│   │ Storage Access /    │ │ Foreground Service + ServerEngine    │     │
│   │ DocumentFile Indexer│ │   • ServerSocket (Port 8080)         │     │
│   │ • Scans picked URIs │ │   • Static Assets (HTML/CSS/JS)      │     │
│   │ • Extracts metadata │ │   • Media Streamer (Byte-Ranges)     │     │
│   │ • Subtitle matcher  │ │   • Remote State Coordinator         │     │
│   └─────────────────────┘ └───────────────────┬──────────────────┘     │
└───────────────────────────────────────────────┼────────────────────────┘
                                                │ HTTP / Local Wi-Fi
                                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│              Client Devices (Smart TV / Laptop / Tablet)               │
│                                                                        │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │ TV Web Dashboard (HTML5 / Modern JS / Cinema CSS)            │     │
│   │  • Video Player with Byte-Range Seeking                      │     │
│   │  • Subtitle track rendering (VTT)                            │     │
│   │  • TV Remote Key Listeners (Arrow keys, Enter, Back)         │     │
│   │  • Remote Command Listener (Receives Play/Pause/Seek)        │     │
│   │  • Web File Manager & Uploader                               │     │
│   └──────────────────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Key Entities
- **SharedFolder**: `uriString: String`, `displayName: String`, `fileCount: Int`, `addedTimestamp: Long`.
- **MediaItem**: `id: String`, `name: String`, `uriString: String`, `mimeType: String`, `size: Long`, `durationMs: Long?`, `category: MediaCategory` (`VIDEO`, `AUDIO`, `IMAGE`, `OTHER`), `subtitleUriString: String?`.
- **RemotePlaybackState**: `mediaId: String?`, `title: String`, `currentTime: Double`, `duration: Double`, `isPlaying: Boolean`, `volume: Double`, `isMuted: Boolean`, `lastUpdated: Long`.
- **ServerStatus**: `isRunning: Boolean`, `ipAddress: String`, `port: Int`, `activeClientsCount: Int`, `bytesSent: Long`, `bytesReceived: Long`.

---

## 5. Verification Plan

1. **Compilation Check**: Run `compile_applet` to ensure Kotlin and Compose files compile with zero warnings or errors.
2. **Network & Server Validation**:
   - Verify local Wi-Fi IP address resolution.
   - Verify HTTP request routing for static assets, API endpoints, and media streaming.
   - Test byte-range parser (`bytes=0-1024`, `bytes=1024-`, etc.) to guarantee 206 Partial Content compliance.
3. **UI Polish**:
   - Ensure edge-to-edge layout, dark cinema aesthetics, responsive cards, and 48dp touch targets.
   - Test QR code generation and copy link actions.
   - Test folder picker integration with `OpenDocumentTree`.
