package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ServerHeroCard(
    isRunning: Boolean,
    ipAddress: String,
    port: Int,
    isWifiConnected: Boolean,
    activeClientsCount: Int,
    bytesSent: Long,
    totalFiles: Int,
    onToggleServer: () -> Unit,
    onShowQr: () -> Unit
) {
    val context = LocalContext.current
    val serverUrl = "http://$ipAddress:$port"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isRunning) CinemaSuccess else CinemaError,
        label = "statusColor"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        if (isRunning) CinemaPrimary.copy(alpha = 0.5f) else CinemaOutline,
                        CinemaOutlineVariant
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("server_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Status Bar & Power Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(if (isRunning) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Column {
                        Text(
                            text = if (isRunning) "SERVER ACTIVE" else "SERVER STOPPED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = statusColor
                        )
                        Text(
                            text = if (isWifiConnected) "Local Wi-Fi Network" else "No Wi-Fi (Offline)",
                            fontSize = 11.sp,
                            color = CinemaTextMuted
                        )
                    }
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { onToggleServer() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CinemaOnPrimary,
                        checkedTrackColor = CinemaPrimary,
                        uncheckedThumbColor = CinemaTextMuted,
                        uncheckedTrackColor = CinemaSurfaceVariant
                    ),
                    modifier = Modifier.testTag("server_power_switch")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Prominent IP Address Display
            Surface(
                color = CinemaSurfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CinemaOutlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TV & Browser Stream URL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serverUrl,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRunning) CinemaPrimary else CinemaTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: QR Code, Copy, Browser
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShowQr,
                    enabled = isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaPrimaryContainer,
                        contentColor = CinemaOnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("hero_qr_button")
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TV QR", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CastBeam URL", serverUrl))
                        Toast.makeText(context, "URL Copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaTextPrimary),
                    border = BorderStroke(1.dp, CinemaOutline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("hero_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                        context.startActivity(browserIntent)
                    },
                    enabled = isRunning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaTextPrimary),
                    border = BorderStroke(1.dp, CinemaOutline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("hero_open_browser_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Metrics Pill Row
            HorizontalDivider(color = CinemaOutlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = "TVs & Clients",
                    value = "$activeClientsCount active",
                    icon = Icons.Default.Tv
                )
                MetricItem(
                    label = "Data Streamed",
                    value = formatBytes(bytesSent),
                    icon = Icons.Default.Speed
                )
                MetricItem(
                    label = "Media Files",
                    value = "$totalFiles files",
                    icon = Icons.Default.VideoLibrary
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CinemaSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CinemaTextPrimary
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = CinemaTextMuted
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb > 1024) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
