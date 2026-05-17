package com.example.weatherclock.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.weatherclock.data.ThemeColors

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF42A5F5),
    secondary = Color(0xFF90CAF9),
    tertiary = Color(0xFFFFD54F),
    background = Color(0xFF0D47A1),
    surface = Color(0xFF1565C0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WeatherClockTheme(
    themeColors: ThemeColors,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = themeColors.topGradient.toArgb()
            window.navigationBarColor = themeColors.bottomGradient.toArgb()
            // Use a light status bar if the background is light (minimal theme)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeColors.isMinimalStyle
        }
    }

    // Build a dynamic color scheme based on whether this is a light or dark theme
    // isMinimalStyle = light theme (white/gray bg, dark text)
    // isInkStyle = dark theme but with monochrome gray palette (no colored accents)
    val colorScheme = when {
        themeColors.isMinimalStyle -> {
            // Light theme for 简约 (minimal) — light backgrounds, dark text
            lightColorScheme(
                primary = themeColors.accentColor,
                onPrimary = Color.White,
                secondary = themeColors.cardHighlight,
                onSecondary = themeColors.textPrimary,
                tertiary = themeColors.accentColor,
                onTertiary = Color.White,
                background = themeColors.topGradient,
                onBackground = themeColors.textPrimary,
                surface = themeColors.surfaceColor,
                onSurface = themeColors.textPrimary,
                surfaceVariant = themeColors.cardHighlight,
                onSurfaceVariant = themeColors.textSecondary,
            )
        }
        themeColors.isInkStyle -> {
            // 水墨 Ink style — warm dark monochrome with subtle off-white text
            // Since isInkStyle is dark but the text is light gray, use darkColorScheme
            darkColorScheme(
                primary = themeColors.accentColor,   // gray accent
                onPrimary = themeColors.textPrimary, // off-white text on dark bg
                secondary = themeColors.cardHighlight,
                onSecondary = themeColors.textPrimary,
                tertiary = themeColors.accentColor,
                onTertiary = themeColors.textPrimary,
                background = themeColors.topGradient,
                onBackground = themeColors.textPrimary,
                surface = themeColors.surfaceColor,
                onSurface = themeColors.textPrimary,
                surfaceVariant = themeColors.cardHighlight,
                onSurfaceVariant = themeColors.textSecondary,
            )
        }
        else -> {
            // Dark theme for all other themes (DAY, NIGHT, FOREST, OCEAN, DUSK, DYNAMIC_WEATHER)
            darkColorScheme(
                primary = themeColors.accentColor,
                onPrimary = if (themeColors.isMinimalStyle) Color.White else themeColors.textPrimary,
                secondary = themeColors.cardHighlight,
                onSecondary = themeColors.textPrimary,
                tertiary = themeColors.accentColor,
                onTertiary = if (themeColors.isMinimalStyle) Color.White else themeColors.textPrimary,
                background = themeColors.topGradient,
                onBackground = themeColors.textPrimary,
                surface = themeColors.surfaceColor,
                onSurface = themeColors.textPrimary,
                surfaceVariant = themeColors.cardHighlight,
                onSurfaceVariant = themeColors.textSecondary,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}