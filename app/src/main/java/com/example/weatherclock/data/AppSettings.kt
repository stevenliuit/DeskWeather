package com.example.weatherclock.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppSettings {
    private const val PREFS_NAME = "weather_clock_settings"
    private const val KEY_THEME = "app_theme"
    private const val KEY_SELECTED_CITY = "selected_city"
    private const val KEY_CITIES = "saved_cities"
    private const val KEY_LAYOUT_CONFIG = "layout_config"
    private const val KEY_FAVORITES = "favorite_cities"
    private const val KEY_RECENT = "recent_cities"
    private const val KEY_PINNED = "pinned_cities"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        getPrefs(context).edit().putInt(KEY_THEME, theme.ordinal).apply()
    }

    fun loadTheme(context: Context): AppTheme {
        val ord = getPrefs(context).getInt(KEY_THEME, 0)
        return AppTheme.fromOrdinal(ord)
    }

    fun saveSelectedCityIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_SELECTED_CITY, index).apply()
    }

    fun loadSelectedCityIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_SELECTED_CITY, 0)
    }

    fun saveCities(context: Context, cities: List<String>) {
        getPrefs(context).edit().putString(KEY_CITIES, cities.joinToString(",")).apply()
    }

    fun loadCities(context: Context): List<String> {
        val str = getPrefs(context).getString(KEY_CITIES, null) ?: return emptyList()
        return str.split(",").filter { it.isNotBlank() }
    }

    // ── 收藏城市 ──
    fun saveFavorites(context: Context, cities: List<String>) {
        getPrefs(context).edit().putString(KEY_FAVORITES, json.encodeToString(cities)).apply()
    }

    fun loadFavorites(context: Context): List<String> {
        val str = getPrefs(context).getString(KEY_FAVORITES, null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(str) } catch (e: Exception) { emptyList() }
    }

    // ── 最近访问 ──
    fun saveRecent(context: Context, cities: List<String>) {
        getPrefs(context).edit().putString(KEY_RECENT, json.encodeToString(cities)).apply()
    }

    fun loadRecent(context: Context): List<String> {
        val str = getPrefs(context).getString(KEY_RECENT, null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(str) } catch (e: Exception) { emptyList() }
    }

    // ── 置顶城市 ──
    fun savePinned(context: Context, cities: List<String>) {
        getPrefs(context).edit().putString(KEY_PINNED, json.encodeToString(cities)).apply()
    }

    fun loadPinned(context: Context): List<String> {
        val str = getPrefs(context).getString(KEY_PINNED, null) ?: return emptyList()
        return try { json.decodeFromString<List<String>>(str) } catch (e: Exception) { emptyList() }
    }

    // ── 布局配置 ──
    fun saveLayoutConfig(context: Context, config: LayoutConfig) {
        getPrefs(context).edit().putString(KEY_LAYOUT_CONFIG, json.encodeToString(config)).apply()
    }

    fun loadLayoutConfig(context: Context): LayoutConfig {
        val str = getPrefs(context).getString(KEY_LAYOUT_CONFIG, null) ?: return defaultLayoutConfig()
        return try { json.decodeFromString<LayoutConfig>(str) } catch (e: Exception) { defaultLayoutConfig() }
    }

    private fun defaultLayoutConfig(): LayoutConfig = LayoutConfig(
        version = 1,
        zones = listOf(
            WidgetZone("left", listOf(
                WidgetConfig("CLOCK", true, 0),
                WidgetConfig("DATE", true, 1),
                WidgetConfig("LOCATION", true, 2),
                WidgetConfig("TEMPERATURE", true, 3),
                WidgetConfig("DETAILS", true, 4),
            )),
            WidgetZone("right", listOf(
                WidgetConfig("FORECAST_7D", true, 0),
                WidgetConfig("AIR_QUALITY", true, 1),
                WidgetConfig("SUN_SUNRISE", true, 2),
                WidgetConfig("THEME_SWITCH", true, 3),
            ))
        )
    )
}