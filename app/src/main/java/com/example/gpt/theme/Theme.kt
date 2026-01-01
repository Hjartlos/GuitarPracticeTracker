package com.example.gpt.theme

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
    primary = MetalRed,
    secondary = MetalRedDark,
    tertiary = MetalAccentGreen,
    background = MetalBackground,
    surface = MetalSurface,
    surfaceVariant = MetalSurfaceVariant,
    onBackground = MetalTextPrimary,
    onSurface = MetalTextPrimary,
    onSurfaceVariant = MetalTextSecondary,
    onPrimary = MetalTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = MetalRed,
    secondary = MetalRedDark,
    tertiary = MetalAccentGreen,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    onPrimary = MetalTextPrimary
)

@Composable
fun GPTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}