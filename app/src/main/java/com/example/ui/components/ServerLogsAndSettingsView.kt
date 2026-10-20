package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerClientSession
import com.example.model.ServerLogEntry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ServerLogsAndSettingsView(
    currentPort: Int,
    clientSessions: List<ServerClientSession>,
    serverLogs: List<ServerLogEntry>,
    onPortChange: (Int) -> Unit
) {
    var portInput by remember(currentPort) { mutableStateOf(currentPort.toString()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("server_logs_and_settings_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Port & Network Configuration
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                border = BorderStroke(1.dp, CinemaOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Server Configuration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("HTTP Port", color = CinemaTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaPrimary,
                                unfocusedBorderColor = CinemaOutline,
                                focusedTextColor = CinemaTextPrimary,
                                unfocusedTextColor = CinemaTextPrimary,
                                focusedContainerColor = CinemaSurfaceContainer,
                                unfocusedContainerColor = CinemaSurfaceContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("port_input_field")
                        )

                        Button(
                            onClick = {
                                val p = portInput.toIntOrNull()
                                if (p != null && p in 1024..65535) {
                                    onPortChange(p)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaPrimary,
                                contentColor = CinemaOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("apply_port_button")
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8080, 8000, 8888, 9090).forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    portInput = preset.toString()
                                    onPortChange(preset)
                                },
                                label = { Text("Port $preset") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = CinemaSurfaceContainer,
                                    labelColor = CinemaTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Active Devices & Smart TVs
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                border = BorderStroke(1.dp, CinemaOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Clients",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CinemaTextPrimary
                        )
                        Text(
                            text = "${clientSessions.size} detected",
                            fontSize = 12.sp,
                            color = CinemaTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (clientSessions.isEmpty()) {
                        Text(
                            text = "No TV or browser connected yet. Open the server URL on your TV.",
                            fontSize = 13.sp,
                            color = CinemaTextMuted
                        )
                    } else {
                        clientSessions.forEach { session ->
                            Surface(
                                color = CinemaSurfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (session.isTv) Icons.Default.Tv else Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = if (session.isTv) CinemaPrimary else CinemaSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.ipAddress,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CinemaTextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = session.userAgent.take(45) + if (session.userAgent.length > 45) "..." else "",
                                            fontSize = 11.sp,
                                            color = CinemaTextMuted
                                        )
                                    }
                                    if (session.isTv) {
                                        Surface(
                                            color = CinemaPrimaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "SMART TV",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CinemaOnPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Server Activity & Streaming Requests
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                border = BorderStroke(1.dp, CinemaOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Live HTTP Traffic Log",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (serverLogs.isEmpty()) {
                        Text(
                            text = "Awaiting incoming streaming requests...",
                            fontSize = 13.sp,
                            color = CinemaTextMuted
                        )
                    } else {
                        serverLogs.take(15).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (log.status in 200..299) CinemaSuccess.copy(alpha = 0.2f) else CinemaError.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${log.status}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.status in 200..299) CinemaSuccess else CinemaError,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = log.method,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CinemaSecondary
                                    )

                                    Text(
                                        text = log.path.take(24) + if (log.path.length > 24) "..." else "",
                                        fontSize = 12.sp,
                                        color = CinemaTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = timeFormat.format(Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = CinemaTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
