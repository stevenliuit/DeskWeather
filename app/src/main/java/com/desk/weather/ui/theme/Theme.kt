package com.desk.weather.ui.theme

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
import com.desk.weather.data.ThemeColors

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
            // 水墨屏(E-ink)是浅色背景，用深色状态栏
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = themeColors.isEinkScreen
        }
    }

    // Build a dynamic color scheme based on theme type:
    // isEinkScreen = light theme (light gray bg, dark text)
    // isDigitalScreen = dark theme (near-black bg, bright green text)
    val colorScheme = when {
        themeColors.isEinkScreen -> {
            // 水墨屏：浅灰背景、深灰文字，lightColorScheme
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
        themeColors.isDigitalScreen -> {
            // 电子屏：深黑背景、亮绿文字，darkColorScheme
            darkColorScheme(
                primary = themeColors.accentColor,
                onPrimary = themeColors.textPrimary,
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
            // 默认深色主题
            darkColorScheme(
                primary = themeColors.accentColor,
                onPrimary = themeColors.textPrimary,
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
