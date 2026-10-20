package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaServerViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MediaServerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CastBeamTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MediaServerViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    val serverUrl = "http://${uiState.localIp}:${uiState.port}"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBackground)
            .testTag("main_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = CinemaPrimary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    tint = CinemaOnPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "CastBeam",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CinemaTextPrimary
                            )
                            Text(
                                text = if (uiState.isWifiConnected) "Wi-Fi Streaming Hub" else "Offline / Hotspot",
                                fontSize = 11.sp,
                                color = CinemaTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowQrDialog(true) },
                        modifier = Modifier.testTag("top_bar_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR Code",
                            tint = CinemaPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshMedia() },
                        modifier = Modifier.testTag("top_bar_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Library",
                            tint = CinemaTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CinemaSurface,
                    titleContentColor = CinemaTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CinemaSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                NavigationItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = Icons.Default.Home,
                    label = "Server",
                    tag = "nav_tab_server"
                )
                NavigationItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = Icons.Default.VideoLibrary,
                    label = "Library",
                    badge = if (uiState.mediaItems.isNotEmpty()) uiState.mediaItems.size else null,
                    tag = "nav_tab_library"
                )
                NavigationItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = Icons.Default.SettingsRemote,
                    label = "TV Remote",
                    tag = "nav_tab_remote"
                )
                NavigationItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = Icons.Default.Tune,
                    label = "Settings",
                    tag = "nav_tab_settings"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    // Dashboard View: Hero Server Card, Quick Access, Quick TV guide
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ServerHeroCard(
                            isRunning = uiState.isServerRunning,
                            ipAddress = uiState.localIp,
                            port = uiState.port,
                            isWifiConnected = uiState.isWifiConnected,
                            activeClientsCount = uiState.clientSessions.size,
                            bytesSent = uiState.bytesSent,
                            totalFiles = uiState.mediaItems.size,
                            onToggleServer = { viewModel.toggleServer() },
                            onShowQr = { viewModel.setShowQrDialog(true) }
                        )

                        // Quick TV Connection Instructions Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaOutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = CinemaSecondary
                                    )
                                    Text(
                                        text = "How to Watch on Your Smart TV",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinemaTextPrimary
                                    )
                                }

                                TvStepItem(
                                    stepNumber = "1",
                                    title = "Same Wi-Fi Network",
                                    desc = "Ensure your TV and this phone are connected to the same Wi-Fi router."
                                )
                                TvStepItem(
                                    stepNumber = "2",
                                    title = "Open Web Browser on TV",
                                    desc = "Open the browser on LG webOS, Samsung Tizen, Android TV, or PC."
                                )
                                TvStepItem(
                                    stepNumber = "3",
                                    title = "Enter URL or Scan QR",
                                    desc = "Navigate to $serverUrl or scan the QR code."
                                )
                                TvStepItem(
                                    stepNumber = "4",
                                    title = "Stream & Remote Control",
                                    desc = "Click any movie to play in full screen. Use the TV Remote tab to pause or scrub from your phone!"
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Folders & Media Library
                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        FoldersAndMediaView(
                            sharedFolders = uiState.sharedFolders,
                            mediaItems = uiState.mediaItems,
                            selectedCategory = uiState.selectedCategory,
                            searchQuery = uiState.searchQuery,
                            isLoading = uiState.isLoadingFolders,
                            onAddFolder = { uri -> viewModel.addFolder(uri) },
                            onRemoveFolder = { uriString -> viewModel.removeFolder(uriString) },
                            onSelectCategory = { cat -> viewModel.setSelectedCategory(cat) },
                            onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                            onPlayOnTv = { item -> viewModel.remotePlayMedia(item) }
                        )
                    }
                }

                2 -> {
                    // Real-time Synchronized TV Remote Controller
                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        TvRemoteDeck(
                            state = uiState.remotePlaybackState,
                            onPlay = { viewModel.remotePlay() },
                            onPause = { viewModel.remotePause() },
                            onTogglePlay = { viewModel.remoteTogglePlay() },
                            onSeek = { seconds -> viewModel.remoteSeek(seconds) },
                            onSeekRelative = { delta -> viewModel.remoteSeekRelative(delta) },
                            onSetVolume = { vol -> viewModel.remoteSetVolume(vol) },
                            onToggleMute = { viewModel.remoteToggleMute() },
                            onToggleFullscreen = { viewModel.remoteToggleFullscreen() }
                        )
                    }
                }

                3 -> {
                    // Settings & Live Logs View
                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        ServerLogsAndSettingsView(
                            currentPort = uiState.port,
                            clientSessions = uiState.clientSessions,
                            serverLogs = uiState.serverLogs,
                            onPortChange = { port -> viewModel.setPort(port) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showQrDialog) {
        QrCodeDialog(
            url = serverUrl,
            onDismiss = { viewModel.setShowQrDialog(false) }
        )
    }
}

@Composable
private fun RowScope.NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    badge: Int? = null,
    tag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            BadgedBox(
                badge = {
                    if (badge != null && badge > 0) {
                        Badge(
                            containerColor = CinemaPrimary,
                            contentColor = CinemaOnPrimary
                        ) {
                            Text(text = badge.toString())
                        }
                    }
                }
            ) {
                Icon(imageVector = icon, contentDescription = label)
            }
        },
        label = { Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CinemaOnPrimary,
            selectedTextColor = CinemaPrimary,
            indicatorColor = CinemaPrimary,
            unselectedIconColor = CinemaTextMuted,
            unselectedTextColor = CinemaTextMuted
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun TvStepItem(stepNumber: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = CinemaSurfaceContainerHigh,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CinemaPrimary
                )
            }
        }
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CinemaTextPrimary
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = CinemaTextSecondary
            )
        }
    }
}
