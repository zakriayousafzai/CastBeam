package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemotePlaybackState
import com.example.ui.theme.*

@Composable
fun TvRemoteDeck(
    state: RemotePlaybackState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Double) -> Unit,
    onSeekRelative: (Double) -> Unit,
    onSetVolume: (Double) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    var userScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0.0) }

    val displayCurrentTime = if (userScrubbing) scrubPosition else state.currentTime
    val duration = if (state.duration > 0) state.duration else 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_remote_deck"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Now Playing Movie Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
            border = BorderStroke(1.dp, CinemaOutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CinemaPrimaryContainer, CinemaSecondaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Tv else Icons.Default.TvOff,
                        contentDescription = null,
                        tint = if (state.isPlaying) CinemaPrimary else CinemaTextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isPlaying) CinemaSuccess else CinemaSecondary)
                        )
                        Text(
                            text = if (state.isPlaying) "NOW STREAMING TO TV" else "TV PLAYER IDLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (state.isPlaying) CinemaSuccess else CinemaSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = state.title.ifEmpty { "No Media Selected" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CinemaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("remote_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = CinemaTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Progress & Timeline Scrubber
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlaybackTime(displayCurrentTime),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CinemaPrimary
                )
                Text(
                    text = formatPlaybackTime(state.duration),
                    fontSize = 13.sp,
                    color = CinemaTextMuted
                )
            }

            Slider(
                value = (displayCurrentTime / duration).toFloat().coerceIn(0f, 1f),
                onValueChange = { frac ->
                    userScrubbing = true
                    scrubPosition = frac.toDouble() * duration
                },
                onValueChangeFinished = {
                    userScrubbing = false
                    onSeek(scrubPosition)
                },
                colors = SliderDefaults.colors(
                    thumbColor = CinemaPrimary,
                    activeTrackColor = CinemaPrimary,
                    inactiveTrackColor = CinemaSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remote_timeline_slider")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Big Tactile Playback Remote Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip -10s
            IconButton(
                onClick = { onSeekRelative(-10.0) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CinemaSurfaceContainer)
                    .testTag("remote_skip_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Skip -10s",
                    tint = CinemaTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Big Center Play/Pause button
            Button(
                onClick = onTogglePlay,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CinemaPrimary,
                    contentColor = CinemaOnPrimary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(80.dp)
                    .border(3.dp, CinemaPrimary.copy(alpha = 0.3f), CircleShape)
                    .testTag("remote_play_pause_button")
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = CinemaOnPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            // Skip +10s
            IconButton(
                onClick = { onSeekRelative(10.0) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CinemaSurfaceContainer)
                    .testTag("remote_skip_forward_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Skip +10s",
                    tint = CinemaTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // TV Volume Deck
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
            border = BorderStroke(1.dp, CinemaOutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("remote_mute_button")
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (state.isMuted) CinemaError else CinemaSecondary
                    )
                }

                Slider(
                    value = if (state.isMuted) 0f else state.volume.toFloat(),
                    onValueChange = { vol -> onSetVolume(vol.toDouble()) },
                    colors = SliderDefaults.colors(
                        thumbColor = CinemaSecondary,
                        activeTrackColor = CinemaSecondary,
                        inactiveTrackColor = CinemaSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("remote_volume_slider")
                )

                Text(
                    text = if (state.isMuted) "Muted" else "${(state.volume * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CinemaTextSecondary,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatPlaybackTime(seconds: Double): String {
    if (seconds <= 0 || seconds.isNaN()) return "0:00"
    val totalSecs = seconds.toLong()
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", minutes, secs)
    }
}
