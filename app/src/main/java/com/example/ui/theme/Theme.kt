package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color(0xFF0B0C10),
    secondary = NeonBlue,
    onSecondary = Color(0xFF0B0C10),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    error = SunsetOrange,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default (students love it!)
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium obsidian theme cohesive
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
