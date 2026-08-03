package com.chani.reptap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = AccentBlue,
    onPrimaryContainer = SurfaceWhite,
    secondary = PrimaryDark,
    onSecondary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = DividerColor,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = ErrorRed,
    onError = SurfaceWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = SurfaceWhite,
    secondary = PrimaryBlue,
    onSecondary = SurfaceWhite,
    background = Color(0xFF12151C),
    onBackground = SurfaceWhite,
    surface = Color(0xFF1B2430),
    onSurface = SurfaceWhite,
    surfaceVariant = Color(0xFF2A3441),
    onSurfaceVariant = Color(0xFFB0B8C4),
    outline = Color(0xFF3A4552),
    error = ErrorRed,
    onError = SurfaceWhite
)

@Composable
fun RepTapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}