package com.ritsu.ai_assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Pink80,
    secondary = LightBlue80,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Black80,
    onSecondary = Black80,
    onBackground = White80,
    onSurface = White80,
    primaryContainer = Pink40,
    secondaryContainer = LightBlue40
)

private val LightColorScheme = lightColorScheme(
    primary = Pink40,
    secondary = LightBlue40,
    background = Color(0xFFF0F0F0),
    surface = White40,
    onPrimary = White40,
    onSecondary = White40,
    onBackground = Black40,
    onSurface = Black40,
    primaryContainer = Pink80,
    secondaryContainer = LightBlue80
)

@Composable
fun RitsuAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // We will create this file next
        content = content
    )
}
