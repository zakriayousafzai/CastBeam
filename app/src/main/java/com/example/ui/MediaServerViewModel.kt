package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.MediaRepository
import com.example.server.HttpServerEngine
import com.example.service.MediaServerService
import com.example.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MediaServerUiState(
    val isServerRunning: Boolean = false,
    val localIp: String = "127.0.0.1",
    val port: Int = 8080,
    val isWifiConnected: Boolean = true,
    val sharedFolders: List<SharedFolder> = emptyList(),
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedCategory: MediaCategory? = null,
    val searchQuery: String = "",
    val remotePlaybackState: RemotePlaybackState = RemotePlaybackState(),
    val clientSessions: List<ServerClientSession> = emptyList(),
    val serverLogs: List<ServerLogEntry> = emptyList(),
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val selectedTab: Int = 0,
    val showQrDialog: Boolean = false,
    val isLoadingFolders: Boolean = false,
    val toastMessage: String? = null
)

class MediaServerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(MediaServerUiState())
    val uiState: StateFlow<MediaServerUiState> = _uiState.asStateFlow()

    private val serverEngine: HttpServerEngine = HttpServerEngine(
        context = application,
        getMediaList = { repository.getCachedItems() },
        getFoldersList = { repository.getSharedFolders() },
        onLogEntry = { logEntry ->
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { state ->
                    val logs = (listOf(logEntry) + state.serverLogs).take(50)
                    state.copy(serverLogs = logs)
                }
            }
        },
        onClientConnected = { session ->
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { state ->
                    val sessions = (listOf(session) + state.clientSessions.filter { it.id != session.id }).take(20)
                    state.copy(clientSessions = sessions)
                }
            }
        }
    )

    init {
        val ip = NetworkUtils.getLocalIpAddress(application)
        val isWifi = NetworkUtils.isWifiConnected(application)
        _uiState.update { it.copy(localIp = ip, isWifiConnected = isWifi) }

        refreshMedia()

        // Auto-start server for immediate plug-and-play convenience
        startServer()

        // Periodic state synchronization loop
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(800)
                val currentIp = NetworkUtils.getLocalIpAddress(getApplication())
                val isWifiNow = NetworkUtils.isWifiConnected(getApplication())
                val remoteState = serverEngine.getRemotePlaybackState()
                val sent = serverEngine.bytesSent.get()
                val recv = serverEngine.bytesReceived.get()

                _uiState.update { state ->
                    state.copy(
                        localIp = currentIp,
                        isWifiConnected = isWifiNow,
                        remotePlaybackState = remoteState,
                        bytesSent = sent,
                        bytesReceived = recv
                    )
                }
            }
        }
    }

    fun startServer() {
        val port = _uiState.value.port
        val success = serverEngine.start(port)
        if (success) {
            MediaServerService.startService(getApplication(), _uiState.value.localIp, port)
            _uiState.update { it.copy(isServerRunning = true) }
        }
    }

    fun stopServer() {
        serverEngine.stop()
        MediaServerService.stopService(getApplication())
        _uiState.update { it.copy(isServerRunning = false) }
    }

    fun toggleServer() {
        if (_uiState.value.isServerRunning) {
            stopServer()
        } else {
            startServer()
        }
    }

    fun setPort(newPort: Int) {
        if (newPort in 1024..65535) {
            val wasRunning = _uiState.value.isServerRunning
            if (wasRunning) stopServer()
            _uiState.update { it.copy(port = newPort) }
            if (wasRunning) startServer()
        }
    }

    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFolders = true) }
            repository.addFolder(uri)
            val updatedFolders = repository.getSharedFolders()
            val updatedMedia = repository.scanAllFolders()
            _uiState.update {
                it.copy(
                    sharedFolders = updatedFolders,
                    mediaItems = updatedMedia,
                    isLoadingFolders = false,
                    toastMessage = "Folder added and indexed!"
                )
            }
        }
    }

    fun removeFolder(uriString: String) {
        viewModelScope.launch {
            repository.removeFolder(uriString)
            val updatedFolders = repository.getSharedFolders()
            val updatedMedia = repository.scanAllFolders()
            _uiState.update {
                it.copy(
                    sharedFolders = updatedFolders,
                    mediaItems = updatedMedia,
                    toastMessage = "Folder unshared"
                )
            }
        }
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFolders = true) }
            val folders = repository.getSharedFolders()
            val items = repository.scanAllFolders()
            _uiState.update {
                it.copy(
                    sharedFolders = folders,
                    mediaItems = items,
                    isLoadingFolders = false
                )
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSelectedCategory(cat: MediaCategory?) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowQrDialog(show: Boolean) {
        _uiState.update { it.copy(showQrDialog = show) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // Remote playback controls
    fun remotePlay() {
        serverEngine.sendRemoteCommand("play")
    }

    fun remotePause() {
        serverEngine.sendRemoteCommand("pause")
    }

    fun remoteTogglePlay() {
        serverEngine.sendRemoteCommand("toggle_play")
    }

    fun remoteSeek(seconds: Double) {
        serverEngine.sendRemoteCommand("seek", seconds.toString())
    }

    fun remoteSeekRelative(deltaSeconds: Double) {
        serverEngine.sendRemoteCommand("seek_relative", deltaSeconds.toString())
    }

    fun remoteSetVolume(volume: Double) {
        serverEngine.sendRemoteCommand("volume", volume.toString())
    }

    fun remoteToggleMute() {
        serverEngine.sendRemoteCommand("mute")
    }

    fun remotePlayMedia(item: MediaItem) {
        serverEngine.sendRemoteCommand("load", item.id)
        _uiState.update {
            it.copy(
                selectedTab = 2, // Switch straight to TV Remote tab
                toastMessage = "Now streaming ${item.name} to TV!"
            )
        }
    }

    fun remoteToggleFullscreen() {
        serverEngine.sendRemoteCommand("fullscreen")
    }

    override fun onCleared() {
        super.onCleared()
        // Note: we let the Foreground Service keep the server socket alive if desired,
        // or shut it down if user explicitly stops it.
    }
}
