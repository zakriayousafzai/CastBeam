package com.example.model

enum class MediaCategory(val label: String) {
    VIDEO("Videos & Movies"),
    AUDIO("Music & Audio"),
    IMAGE("Photos & Images"),
    DOCUMENT("Documents"),
    OTHER("Other Files")
}

data class MediaItem(
    val id: String,
    val name: String,
    val uriString: String,
    val mimeType: String,
    val size: Long,
    val category: MediaCategory,
    val folderName: String,
    val durationMs: Long? = null,
    val subtitleUri: String? = null,
    val lastModified: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val formatted = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.1f %s", formatted, units[digitGroups])
        }

    val formattedDuration: String?
        get() {
            if (durationMs == null || durationMs <= 0) return null
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
            }
        }
}

data class SharedFolder(
    val uriString: String,
    val name: String,
    val itemCount: Int,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class RemotePlaybackState(
    val mediaId: String? = null,
    val title: String = "No media playing",
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val isPlaying: Boolean = false,
    val volume: Double = 1.0,
    val isMuted: Boolean = false,
    val isFullscreen: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val commandQueue: MutableList<RemoteCommand> = mutableListOf()
)

data class RemoteCommand(
    val id: String = java.util.UUID.randomUUID().toString(),
    val action: String, // "play", "pause", "toggle_play", "seek", "volume", "mute", "load", "fullscreen"
    val value: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ServerClientSession(
    val id: String,
    val ipAddress: String,
    val userAgent: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val isTv: Boolean = false
)

data class ServerLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val status: Int,
    val clientIp: String
)
