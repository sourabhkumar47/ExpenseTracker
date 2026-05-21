package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFEFFF),
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF161523),
    surface = LightSurface,
    onSurface = Color(0xFF161523),
    surfaceVariant = Color(0xFFE2E4F2),
    onSurfaceVariant = Color(0xFF3B3A50),
    outline = Color(0xFF757488)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF0E0D16),
    primaryContainer = Color(0xFF1F1E2E),
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF0E0D16),
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF5E1700),
    background = DarkBackground,
    onBackground = Color(0xFFE5E6FC),
    surface = DarkSurface,
    onSurface = Color(0xFFE5E6FC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C5DC),
    outline = Color(0xFF8D8EAA)
)

@Composable
fun MyApplicationTheme(
    darkModeSetting: Int = 0, // 0 = System, 1 = Light, 2 = Dark
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkModeSetting) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
