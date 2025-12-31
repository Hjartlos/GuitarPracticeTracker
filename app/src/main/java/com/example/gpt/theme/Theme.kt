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
    background = MetalBlack,
    surface = MetalSurface,
    onBackground = MetalTextPrimary,
    onSurface = MetalTextPrimary,
    onPrimary = MetalTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = MetalRed,       // Czerwony zostaje jako akcent marki
    secondary = MetalRedDark,
    tertiary = MetalAccentGreen,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onPrimary = MetalTextPrimary // Tekst na czerwonym przycisku nadal biały
)

@Composable
fun GPTTheme(
    // Domyślnie bierzemy ustawienie systemowe, ale SettingsScreen może to nadpisać
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Ikony paska stanu: jasne dla ciemnego motywu, ciemne dla jasnego
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}