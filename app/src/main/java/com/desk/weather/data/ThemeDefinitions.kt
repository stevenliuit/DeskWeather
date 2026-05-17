package com.desk.weather.data

import androidx.compose.ui.graphics.Color

object ThemeDefinitions {

    val autoTheme = ThemeColors(
        name = "自动",
        topGradient = Color(0xFF1A237E),
        bottomGradient = Color(0xFF1565C0),
        surfaceColor = Color(0xFF0D47A1),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.7f),
        accentColor = Color(0xFFFFD700),
        cardHighlight = Color.White.copy(alpha = 0.15f)
    )

    val dayTheme = ThemeColors(
        name = "晴昼",
        topGradient = Color(0xFF1976D2),
        bottomGradient = Color(0xFF42A5F5),
        surfaceColor = Color(0xFF0D47A1),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.7f),
        accentColor = Color(0xFFFFD54F),
        cardHighlight = Color.White.copy(alpha = 0.18f)
    )

    val duskTheme = ThemeColors(
        name = "黄昏",
        topGradient = Color(0xFF4A148C),
        bottomGradient = Color(0xFFFF6F00),
        surfaceColor = Color(0xFF7B1FA2),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.75f),
        accentColor = Color(0xFFFFAB40),
        cardHighlight = Color.White.copy(alpha = 0.12f)
    )

    val nightTheme = ThemeColors(
        name = "星空",
        topGradient = Color(0xFF0D1B2A),
        bottomGradient = Color(0xFF1B263B),
        surfaceColor = Color(0xFF0A1628),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.65f),
        accentColor = Color(0xFF64B5F6),
        cardHighlight = Color.White.copy(alpha = 0.08f)
    )

    val forestTheme = ThemeColors(
        name = "森林",
        topGradient = Color(0xFF1B5E20),
        bottomGradient = Color(0xFF4CAF50),
        surfaceColor = Color(0xFF2E7D32),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.75f),
        accentColor = Color(0xFFA5D6A7),
        cardHighlight = Color.White.copy(alpha = 0.15f)
    )

    val oceanTheme = ThemeColors(
        name = "海洋",
        topGradient = Color(0xFF01579B),
        bottomGradient = Color(0xFF00ACC1),
        surfaceColor = Color(0xFF0277BD),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.7f),
        accentColor = Color(0xFF80DEEA),
        cardHighlight = Color.White.copy(alpha = 0.18f)
    )

    // ================================================================
    // 🖋️ 水墨主题 — 中国水墨画风格，纯黑白灰层次
    // ================================================================
    val inkTheme = ThemeColors(
        name = "水墨",
        topGradient = Color(0xFF1A1A1A),
        bottomGradient = Color(0xFF2D2D2D),
        surfaceColor = Color(0xFF212121),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFFBDBDBD),
        accentColor = Color(0xFF9E9E9E),
        cardHighlight = Color(0xFF424242),
        isInkStyle = true
    )

    // ================================================================
    // 📱 简约主题 — 极简留白，信息清晰，灰白基调
    // ================================================================
    val minimalTheme = ThemeColors(
        name = "简约",
        topGradient = Color(0xFFF5F5F5),
        bottomGradient = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFFAFAFA),
        textPrimary = Color(0xFF212121),
        textSecondary = Color(0xFF757575),
        accentColor = Color(0xFF212121),
        cardHighlight = Color(0xFFEEEEEE),
        isMinimalStyle = true
    )

    // ================================================================
    // 🌦️ 动态天气主题 — 天气驱动的沉浸式动态效果
    // 实际动态效果在 MainScreen.kt 中通过 weatherCode 驱动动画
    // ================================================================
    val dynamicWeatherTheme = ThemeColors(
        name = "动态天气",
        topGradient = Color(0xFF1976D2),
        bottomGradient = Color(0xFF64B5F6),
        surfaceColor = Color(0xFF0D47A1),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.75f),
        accentColor = Color(0xFFFFFFFF),
        cardHighlight = Color.White.copy(alpha = 0.15f),
        isDynamicWeather = true
    )

    fun getTheme(theme: AppTheme, isDay: Boolean): ThemeColors {
        return when (theme) {
            AppTheme.AUTO -> if (isDay) dayTheme else nightTheme
            AppTheme.DAY -> dayTheme
            AppTheme.DUSK -> duskTheme
            AppTheme.NIGHT -> nightTheme
            AppTheme.FOREST -> forestTheme
            AppTheme.OCEAN -> oceanTheme
            AppTheme.INK -> inkTheme
            AppTheme.MINIMAL -> minimalTheme
            AppTheme.DYNAMIC_WEATHER -> dynamicWeatherTheme
            AppTheme.NONE -> nightTheme
        }
    }

    fun themeList(): List<Pair<AppTheme, ThemeColors>> = listOf(
        AppTheme.AUTO to autoTheme,
        AppTheme.DAY to dayTheme,
        AppTheme.DUSK to duskTheme,
        AppTheme.NIGHT to nightTheme,
        AppTheme.FOREST to forestTheme,
        AppTheme.OCEAN to oceanTheme,
        AppTheme.INK to inkTheme,
        AppTheme.MINIMAL to minimalTheme,
        AppTheme.DYNAMIC_WEATHER to dynamicWeatherTheme,
    )
}