package com.desk.weather.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

// ============================================================
// 城市数据模型（带拼音）
// ============================================================
@Serializable
data class WeatherLocation(
    val name: String,
    val pinyin: String,          // 拼音全称：bei jing
    val pinyinInitial: String,   // 首字母：B
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val country: String = "",
    val countryCode: String = ""
) {
    // 按拼音字母分组用的key
    val sortKey: String get() = pinyinInitial.uppercase()[0].toString()
}

// ============================================================
// 天气数据模型
// ============================================================
@Serializable
data class CurrentWeather(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val humidity: Int,
    val isDay: Boolean,
    val feelsLike: Double = temperature
)

@Serializable
data class DailyForecast(
    val date: String,
    val tempMax: Double,
    val tempMin: Double,
    val weatherCode: Int,
    val precipitationProbability: Int,
    val sunrise: String = "",
    val sunset: String = ""
)

@Serializable
data class WeatherData(
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val location: WeatherLocation,
    val fetchedAt: Long = System.currentTimeMillis()
)

// 空气质量数据
data class AirQualityData(
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double,
    val no2: Double,
    val so2: Double,
    val co: Double,
    val aqiLevel: String,
    val aqiColor: Long,
)

// ============================================================
// 主题枚举 + 主题色定义
// ============================================================
enum class AppTheme(val displayName: String, val description: String) {
    // 基础主题
    AUTO("自动", "跟随当地时间自动切换"),
    DAY("晴昼", "蓝天白云明媚清新"),
    DUSK("黄昏", "暖橙晚霞浪漫氛围"),
    NIGHT("星空", "深邃夜空静谧安宁"),
    FOREST("森林", "翠绿自然清新淡雅"),
    OCEAN("海洋", "蓝白渐变开阔宁静"),
    // 设备专属主题
    DIGITAL_SCREEN("电子屏", "LED数码管风格·黑底亮绿"),
    EINK_SCREEN("水墨屏", "电子墨水屏风格·浅灰深字"),
    // 特殊
    NONE("无", "");

    companion object {
        fun fromOrdinal(ord: Int): AppTheme = entries.getOrElse(ord) { AUTO }
        fun baseThemeCount() = 6  // 前6个为基础主题
    }
}

// ============================================================
// 主题色数据结构
// ============================================================
data class ThemeColors(
    val name: String,
    val topGradient: Color,
    val bottomGradient: Color,
    val surfaceColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentColor: Color,
    val cardHighlight: Color,
    val isDigitalScreen: Boolean = false,  // 电子屏：黑底亮绿数字
    val isEinkScreen: Boolean = false,     // 水墨屏：浅灰背景深灰文字
    val isDynamicWeather: Boolean = false,   // 动态天气主题
)

// ============================================================
// ============================================================
// 视觉比重布局样式（只有3种，无左右分区概念）
// 布局类型：决定主天气区和七日预报的整体结构
enum class LayoutType {
    TODAY_DETAIL,  // 今日详情：当天大卡片 + 七日预报(从明天开始)
    WEEK_OVERVIEW,  // 七日总览：当前天气中等 + 全部7天预报(含今天)
    MINIMAL_CLOCK   // 极简时钟：时钟超大 + 天气小 + 七日预报(从明天开始)
}

// 视觉比重布局样式（只有3种，对应3种结构化布局）
enum class VisualStyle(
    val displayName: String,
    val description: String,
    val layoutType: LayoutType
) {
    TODAY_DETAIL("今日详情", "当天大卡片 · 七日预报从明天开始", LayoutType.TODAY_DETAIL),
    WEEK_OVERVIEW("七日总览", "当前天气中等 · 完整7天预报", LayoutType.WEEK_OVERVIEW),
    MINIMAL_CLOCK("极简时钟", "时钟超大 · 天气小信息少", LayoutType.MINIMAL_CLOCK)
}

// 自定义区块类型
// ============================================================
enum class WidgetType(val displayName: String, val description: String) {
    CLOCK("时钟", "当前时间"),
    DATE("日期", "当前日期"),
    TEMPERATURE("温度", "当前温度和天气图标"),
    FORECAST_7D("7日预报", "未来七天天气预报"),
    DETAILS("详情", "湿度/风速/体感温度等"),
    AIR_QUALITY("空气质量", "AQI/PM2.5等"),
    SUN_SUNRISE("日出日落", "日出日落时间"),
    LOCATION("位置", "城市名和时区"),
    THEME_SWITCH("主题切换", "切换主题"),
    SPACING("间距", "留白间隔"),
}

@Serializable
data class WidgetConfig(
    val type: String,
    val visible: Boolean = true,
    val order: Int = 0,
    val span: Int = 1,  // 占据的列数
)

@Serializable
data class LayoutConfig(
    val version: Int = 1,
    val visualStyle: VisualStyle = VisualStyle.TODAY_DETAIL,
    // zones字段保留但忽略，仅用于向后兼容
    val zones: List<WidgetZone> = emptyList()
)

@Serializable
data class WidgetZone(
    val id: String,
    val widgets: List<WidgetConfig> = emptyList()
)