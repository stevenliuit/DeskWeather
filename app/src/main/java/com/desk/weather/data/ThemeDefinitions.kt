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
    // 📱 设备专属主题
    // ================================================================

    // 电子屏主题 — LED/数码管风格，黑底亮字，高对比度
    val digitalScreenTheme = ThemeColors(
        name = "电子屏",
        topGradient = Color(0xFF0D0D0D),
        bottomGradient = Color(0xFF1A1A1A),
        surfaceColor = Color(0xFF0A0A0A),
        textPrimary = Color(0xFF00FF88),        // 亮绿数字
        textSecondary = Color(0xFF888888),      // 灰白次要文字
        accentColor = Color(0xFF00FF88),        // 绿色强调
        cardHighlight = Color(0xFF1A2A1A),
        isDigitalScreen = true
    )

    // 水墨屏主题 — 电子墨水屏风格，浅灰背景，深灰文字，无背光感
    val einkScreenTheme = ThemeColors(
        name = "水墨屏",
        topGradient = Color(0xFFE8E8E8),
        bottomGradient = Color(0xFFF5F5F5),
        surfaceColor = Color(0xFFFAFAFA),
        textPrimary = Color(0xFF1A1A1A),        // 深灰/近黑文字
        textSecondary = Color(0xFF666666),      // 中灰次要
        accentColor = Color(0xFF333333),        // 深灰强调
        cardHighlight = Color(0xFFDDDDDD),
        isEinkScreen = true
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
        cardHighlight = Color(0xFFEEEEEE)
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
            AppTheme.DIGITAL_SCREEN -> digitalScreenTheme
            AppTheme.EINK_SCREEN -> einkScreenTheme
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
        AppTheme.DIGITAL_SCREEN to digitalScreenTheme,
        AppTheme.EINK_SCREEN to einkScreenTheme,
    )
}