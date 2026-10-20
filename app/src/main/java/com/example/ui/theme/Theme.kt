package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CinemaDarkColorScheme = darkColorScheme(
    primary = CinemaPrimary,
    onPrimary = CinemaOnPrimary,
    primaryContainer = CinemaPrimaryContainer,
    onPrimaryContainer = CinemaOnPrimaryContainer,
    secondary = CinemaSecondary,
    onSecondary = CinemaOnSecondary,
    secondaryContainer = CinemaSecondaryContainer,
    onSecondaryContainer = CinemaOnSecondaryContainer,
    tertiary = CinemaTertiary,
    onTertiary = CinemaOnTertiary,
    background = CinemaBackground,
    onBackground = CinemaTextPrimary,
    surface = CinemaSurface,
    onSurface = CinemaTextPrimary,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = CinemaTextSecondary,
    surfaceContainer = CinemaSurfaceContainer,
    surfaceContainerHigh = CinemaSurfaceContainerHigh,
    outline = CinemaOutline,
    outlineVariant = CinemaOutlineVariant,
    error = CinemaError
)

// We maintain cinema dark aesthetic by default for a movie/media streaming application
private val CinemaLightColorScheme = CinemaDarkColorScheme.copy(
    background = CinemaBackground,
    surface = CinemaSurface
)

@Composable
fun CastBeamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = CinemaDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
