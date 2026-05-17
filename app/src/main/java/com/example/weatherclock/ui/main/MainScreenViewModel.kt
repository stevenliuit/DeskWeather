package com.example.weatherclock.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherclock.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import kotlin.math.pow

data class DayForecastItem(
    val dayLabel: String,
    val icon: String,
    val tempMax: String,
    val tempMin: String,
    val precipProb: String
)

// 空气质量展示项
data class AirQualityItem(
    val label: String,
    val value: String,
    val unit: String,
    val level: String,
    val color: Long
)

data class MainUiState(
    val currentTime: String = "--:--",
    val currentDate: String = "----年--月--日",
    val timezoneDisplay: String = "",
    val locationName: String = "",
    val locationSource: String = "",
    val temperature: String = "--°",
    val weatherIcon: String = "",
    val weatherDescription: String = "",
    val feelsLike: String = "",
    val humidity: String = "",
    val windSpeed: String = "",
    val forecast: List<DayForecastItem> = emptyList(),
    val airQuality: AirQualityItem? = null,  // 空气质量卡片
    val availableLocations: List<WeatherLocation> = emptyList(),
    val selectedLocationIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val locationDenied: Boolean = false,
    val isLocating: Boolean = false,
    val useMyLocationEnabled: Boolean = false,
    val themeColors: ThemeColors = ThemeDefinitions.autoTheme,

    // Search
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val searchResults: List<WeatherLocation> = emptyList(),
    val pinyinInitials: List<String> = emptyList(),
    val locationsByPinyin: Map<String, List<WeatherLocation>> = emptyMap(),

    // City management
    val favoriteLocations: List<WeatherLocation> = emptyList(),    // 收藏
    val recentLocations: List<WeatherLocation> = emptyList(),      // 最近访问
    val pinnedLocations: List<WeatherLocation> = emptyList(),      // 置顶

    // Layout editor
    val layoutConfig: LayoutConfig = LayoutConfig(),
    val showLayoutEditor: Boolean = false,
    val editingZone: String = "left",  // 当前编辑的区域
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var timeJob: Job? = null
    private var weatherJob: Job? = null
    private var currentLocation: WeatherLocation? = null
    private var currentWeatherCode: Int = 0
    private var isCurrentlyDay: Boolean = true

    init {
        initializeData()
        startClock()
    }

    private fun initializeData() {
        val allLocs = WeatherRepository.getAllLocations()
        val ctx = getApplication<Application>()
        val favorites = loadCities(ctx, AppSettings.loadFavorites(ctx))
        val recent = loadCities(ctx, AppSettings.loadRecent(ctx))
        val pinned = loadCities(ctx, AppSettings.loadPinned(ctx))

        _uiState.value = _uiState.value.copy(
            availableLocations = allLocs,
            pinyinInitials = WeatherRepository.getPinyinInitials(),
            locationsByPinyin = WeatherRepository.getLocationsByPinyinInitial(),
            favoriteLocations = favorites,
            recentLocations = recent,
            pinnedLocations = pinned,
            layoutConfig = AppSettings.loadLayoutConfig(ctx),
        )

        // 优先选中置顶城市，否则默认
        val defaultIndex = if (pinned.isNotEmpty()) {
            allLocs.indexOfFirst { it.name == pinned.first().name }.takeIf { it >= 0 } ?: 0
        } else {
            AppSettings.loadSelectedCityIndex(ctx).coerceIn(0, allLocs.size - 1)
        }
        selectCity(defaultIndex)
    }

    private fun loadCities(ctx: android.content.Context, names: List<String>): List<WeatherLocation> {
        return names.mapNotNull { name -> WeatherRepository.getLocationByName(name) }
    }

    private fun startClock() {
        timeJob?.cancel()
        timeJob = viewModelScope.launch {
            while (true) {
                updateDateTime()
                delay(1000)
            }
        }
    }

    private fun updateDateTime() {
        val loc = currentLocation ?: WeatherRepository.getAllLocations().firstOrNull() ?: return
        val tz = java.time.ZoneId.of(loc.timezone)
        val now = ZonedDateTime.now(tz)
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日  E", java.util.Locale.CHINESE))

        val tzDisplay = when {
            loc.timezone == "Asia/Shanghai" -> "🕐 北京时间 (UTC+8)"
            loc.timezone == "Asia/Tokyo" -> "🕐 东京时间 (UTC+9)"
            loc.timezone == "America/New_York" -> "🕐 纽约时间 (UTC-5)"
            loc.timezone == "Europe/London" -> "🕐 伦敦时间 (UTC+0)"
            loc.timezone == "Europe/Paris" -> "🕐 巴黎时间 (UTC+1)"
            loc.timezone == "America/Los_Angeles" -> "🕐 洛杉矶时间 (UTC-8)"
            else -> "🕐 ${loc.timezone}"
        }

        val theme = AppSettings.loadTheme(getApplication()).let {
            if (it == AppTheme.AUTO) if (isCurrentlyDay) AppTheme.DAY else AppTheme.NIGHT
            else it
        }
        val colors = ThemeDefinitions.getTheme(theme, isCurrentlyDay)

        _uiState.value = _uiState.value.copy(
            currentTime = timeStr,
            currentDate = dateStr,
            timezoneDisplay = tzDisplay,
            themeColors = colors,
        )
    }

    fun selectCity(index: Int) {
        val locations = _uiState.value.availableLocations
        if (index < 0 || index >= locations.size) return

        currentLocation = locations[index]
        _uiState.value = _uiState.value.copy(
            selectedLocationIndex = index,
            locationName = locations[index].name,
            useMyLocationEnabled = false,
            isLoading = true,
            error = null,
        )

        AppSettings.saveSelectedCityIndex(getApplication(), index)
        fetchWeather(locations[index])
        recordRecent(locations[index])
    }

    fun selectCityByLocation(location: WeatherLocation) {
        val allLocs = WeatherRepository.getAllLocations()
        val idx = allLocs.indexOfFirst { it.name == location.name && it.latitude == location.latitude }
        if (idx >= 0) selectCity(idx) else {
            currentLocation = location
            _uiState.value = _uiState.value.copy(
                locationName = location.name,
                isLoading = true,
            )
            fetchWeather(location)
            recordRecent(location)
        }
    }

    private fun fetchWeather(location: WeatherLocation) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val weatherResult = WeatherRepository.getWeather(location)
            weatherResult.onSuccess { data ->
                currentWeatherCode = data.current.weatherCode
                isCurrentlyDay = data.current.isDay

                val forecast = data.daily.mapIndexed { idx, day ->
                    val dayLabel = when (idx) {
                        0 -> "今天"
                        1 -> "明天"
                        else -> {
                            val date = try {
                                java.time.LocalDate.parse(day.date)
                            } catch (e: Exception) {
                                java.time.LocalDate.now()
                            }
                            date.format(DateTimeFormatter.ofPattern("E", java.util.Locale.CHINESE))
                        }
                    }
                    DayForecastItem(
                        dayLabel = dayLabel,
                        icon = WeatherRepository.getWeatherIcon(day.weatherCode, true),
                        tempMax = "${day.tempMax.toInt()}°",
                        tempMin = "${day.tempMin.toInt()}°",
                        precipProb = if (day.precipitationProbability > 0) "${day.precipitationProbability}%" else ""
                    )
                }

                _uiState.value = _uiState.value.copy(
                    temperature = "${data.current.temperature.toInt()}°",
                    weatherIcon = WeatherRepository.getWeatherIcon(data.current.weatherCode, data.current.isDay),
                    weatherDescription = WeatherRepository.getWeatherDescription(data.current.weatherCode),
                    feelsLike = "体感 ${data.current.feelsLike.toInt()}°",
                    humidity = "${data.current.humidity}%",
                    windSpeed = "${data.current.windSpeed.toInt()} km/h",
                    forecast = forecast,
                    isLoading = false,
                )
                updateDateTime()

            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "获取天气失败")
            }

            // 同时拉取空气质量
            val aqResult = WeatherRepository.getAirQuality(location)
            aqResult.onSuccess { aq ->
                val items = listOf(
                    AirQualityItem("AQI", "${aq.usAqi}", "", aq.aqiLevel, aq.aqiColor),
                    AirQualityItem("PM2.5", "%.1f".format(aq.pm25), "μg/m³", levelText(aq.pm25, 35.0, 75.0, 150.0, 250.0, 350.0, 500.0), 0xFF64B5F6),
                    AirQualityItem("PM10", "%.1f".format(aq.pm10), "μg/m³", levelText(aq.pm10, 50.0, 150.0, 250.0, 350.0, 420.0, 600.0), 0xFF4FC3F7),
                    AirQualityItem("O₃", "%.1f".format(aq.ozone), "μg/m³", levelText(aq.ozone, 100.0, 160.0, 215.0, 265.0, 800.0, 800.0), 0xFFFFD54F),
                    AirQualityItem("NO₂", "%.1f".format(aq.no2), "μg/m³", levelText(aq.no2, 100.0, 200.0, 700.0, 1200.0, 2340.0, 3090.0), 0xFFCE93D8),
                    AirQualityItem("CO", "%.1f".format(aq.co), "mg/m³", if (aq.co <= 2.0) "低" else if (aq.co <= 4.0) "中" else "高", 0xFF90CAF9),
                )
                _uiState.value = _uiState.value.copy(airQuality = items.firstOrNull())
                // 存储完整aq数据用于详情
                _airQualityData = aq
            }
        }
    }

    private var _airQualityData: AirQualityData? = null
    fun getAirQualityData(): AirQualityData? = _airQualityData

    private fun levelText(value: Double, vararg thresholds: Double): String {
        var level = "低"
        for ((i, t) in thresholds.withIndex()) {
            if (value <= t) { level = when(i) { 0->"低";1->"中";2->"高";3->"超高";else->"极高" }; break }
        }
        return level
    }

    fun cycleTheme() {
        val current = AppSettings.loadTheme(getApplication())
        val themes = AppTheme.entries.filter { it != AppTheme.NONE }
        val nextIdx = (themes.indexOf(current) + 1) % themes.size
        AppSettings.saveTheme(getApplication(), themes[nextIdx])
        updateDateTime()
    }

    fun setTheme(theme: AppTheme) {
        AppSettings.saveTheme(getApplication(), theme)
        // Force immediate recomposition by updating state synchronously
        val isDay = currentWeatherCode.let { code ->
            // Use current isDay from weather, or default to day
            isCurrentlyDay
        }
        val colors = ThemeDefinitions.getTheme(theme, isDay)
        _uiState.value = _uiState.value.copy(themeColors = colors)
    }

    // ── 城市管理 ──
    fun toggleFavorite(city: WeatherLocation) {
        val ctx = getApplication<Application>()
        val current = AppSettings.loadFavorites(ctx).toMutableList()
        if (current.contains(city.name)) {
            current.remove(city.name)
        } else {
            current.add(city.name)
        }
        AppSettings.saveFavorites(ctx, current)
        _uiState.value = _uiState.value.copy(favoriteLocations = loadCities(ctx, current))
    }

    fun togglePin(city: WeatherLocation) {
        val ctx = getApplication<Application>()
        val current = AppSettings.loadPinned(ctx).toMutableList()
        if (current.contains(city.name)) {
            current.remove(city.name)
        } else {
            current.add(0, city.name)  // 置顶放在最前面
        }
        AppSettings.savePinned(ctx, current)
        _uiState.value = _uiState.value.copy(pinnedLocations = loadCities(ctx, current))
    }

    fun isFavorite(city: WeatherLocation): Boolean {
        return AppSettings.loadFavorites(getApplication()).contains(city.name)
    }

    fun isPinned(city: WeatherLocation): Boolean {
        return AppSettings.loadPinned(getApplication()).contains(city.name)
    }

    private fun recordRecent(city: WeatherLocation) {
        val ctx = getApplication<Application>()
        val current = AppSettings.loadRecent(ctx).toMutableList()
        current.remove(city.name)
        current.add(0, city.name)
        if (current.size > 10) current.removeAt(current.size - 1)  // 最多保留10条
        AppSettings.saveRecent(ctx, current)
        _uiState.value = _uiState.value.copy(recentLocations = loadCities(ctx, current))
    }

    // ── 搜索 ──
    private var searchDebounceJob: Job? = null
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchMode = true)
        searchDebounceJob?.cancel()
        if (query.isNotBlank()) {
            searchDebounceJob = viewModelScope.launch {
                delay(150) // debounce
                _uiState.value = _uiState.value.copy(
                    searchResults = WeatherRepository.searchLocations(query),
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun onSearchFocus(focused: Boolean) {
        if (!focused && _uiState.value.searchQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(isSearchMode = false, searchResults = emptyList())
        }
    }

    fun onSearchOpen() {
        _uiState.value = _uiState.value.copy(isSearchMode = true, searchQuery = "", searchResults = emptyList())
    }

    fun onCitySelectedFromSearch(location: WeatherLocation) {
        val allLocs = WeatherRepository.getAllLocations()
        val idx = allLocs.indexOfFirst { it.name == location.name && it.latitude == location.latitude }
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearchMode = false, searchResults = emptyList())
        if (idx >= 0) selectCity(idx) else selectCityByLocation(location)
    }

    fun scrollToPinyinInitial(initial: String) {
        val available = _uiState.value.availableLocations
        val match = available.indexOfFirst { it.pinyinInitial == initial }
        if (match >= 0) selectCity(match)
    }

    // ── 布局编辑器 ──
    fun toggleLayoutEditor() {
        _uiState.value = _uiState.value.copy(showLayoutEditor = !_uiState.value.showLayoutEditor)
    }

    fun saveLayoutConfig(config: LayoutConfig) {
        AppSettings.saveLayoutConfig(getApplication(), config)
        _uiState.value = _uiState.value.copy(layoutConfig = config)
    }

    // 拖拽排序：移动区块
    fun moveWidget(zoneId: String, fromIndex: Int, toIndex: Int) {
        val zones = _uiState.value.layoutConfig.zones.toMutableList()
        val zoneIndex = zones.indexOfFirst { it.id == zoneId }
        if (zoneIndex < 0) return

        val zone = zones[zoneIndex]
        val widgets = zone.widgets.toMutableList()
        if (fromIndex < 0 || fromIndex >= widgets.size || toIndex < 0 || toIndex >= widgets.size) return

        Collections.swap(widgets, fromIndex, toIndex)
        zones[zoneIndex] = WidgetZone(zoneId, widgets)
        val newConfig = LayoutConfig(zones = zones)
        saveLayoutConfig(newConfig)
    }

    // 切换区块可见性
    fun toggleWidgetVisibility(zoneId: String, widgetType: String) {
        val zones = _uiState.value.layoutConfig.zones.toMutableList()
        val zoneIndex = zones.indexOfFirst { it.id == zoneId }
        if (zoneIndex < 0) return

        val zone = zones[zoneIndex]
        val widgets = zone.widgets.map { w ->
            if (w.type == widgetType) w.copy(visible = !w.visible) else w
        }
        zones[zoneIndex] = WidgetZone(zoneId, widgets)
        saveLayoutConfig(LayoutConfig(zones = zones))
    }

    fun setEditingZone(zone: String) {
        _uiState.value = _uiState.value.copy(editingZone = zone)
    }

    // 应用布局模板
    fun applyTemplate(templateName: String) {
        val ctx = getApplication<Application>()
        val config = when (templateName) {
            "默认" -> LayoutConfig(zones = listOf(
                WidgetZone("left", listOf(
                    WidgetConfig("CLOCK", true, 0), WidgetConfig("DATE", true, 1),
                    WidgetConfig("LOCATION", true, 2), WidgetConfig("TEMPERATURE", true, 3), WidgetConfig("DETAILS", true, 4)
                )),
                WidgetZone("right", listOf(
                    WidgetConfig("FORECAST_7D", true, 0), WidgetConfig("AIR_QUALITY", true, 1),
                    WidgetConfig("SUN_SUNRISE", true, 2), WidgetConfig("THEME_SWITCH", true, 3)
                ))
            ))
            "极简" -> LayoutConfig(zones = listOf(
                WidgetZone("left", listOf(
                    WidgetConfig("CLOCK", true, 0), WidgetConfig("DATE", true, 1), WidgetConfig("TEMPERATURE", true, 2)
                )),
                WidgetZone("right", listOf(
                    WidgetConfig("FORECAST_7D", true, 0)
                ))
            ))
            "信息全开" -> LayoutConfig(zones = listOf(
                WidgetZone("left", listOf(
                    WidgetConfig("CLOCK", true, 0), WidgetConfig("DATE", true, 1),
                    WidgetConfig("LOCATION", true, 2), WidgetConfig("TEMPERATURE", true, 3), WidgetConfig("DETAILS", true, 4)
                )),
                WidgetZone("right", listOf(
                    WidgetConfig("FORECAST_7D", true, 0), WidgetConfig("AIR_QUALITY", true, 1),
                    WidgetConfig("SUN_SUNRISE", true, 2), WidgetConfig("THEME_SWITCH", true, 3), WidgetConfig("SPACING", true, 4)
                ))
            ))
            else -> return
        }
        saveLayoutConfig(config)
    }

    // ── 位置权限 ──
    private var locatingJob: Job? = null

    fun onLocationPermissionGranted() {
        // Cancel any in-progress location request
        locatingJob?.cancel()
        _uiState.value = _uiState.value.copy(isLocating = true, locationDenied = false, useMyLocationEnabled = true)
        locatingJob = viewModelScope.launch {
            val ctx = getApplication<Application>()

            // Helper to process location result
            fun processLocationResult(lat: Double, lon: Double, source: String) {
                onLocationReceived(lat, lon, source)
            }

            try {
                // Step 1: Try current location with 15-second timeout
                val location = withTimeoutOrNull(15_000L) {
                    LocationProvider.getCurrentLocation(ctx)
                }

                if (location != null) {
                    processLocationResult(location.latitude, location.longitude, location.source)
                    return@launch
                }

                // Step 2: No GPS fix yet — try last known location
                val lastLocation = LocationProvider.getLastKnownLocation(ctx)
                if (lastLocation != null) {
                    // Use last known but note it's stale
                    processLocationResult(lastLocation.latitude, lastLocation.longitude, lastLocation.source + " (缓存)")
                    return@launch
                }

                // Step 3: No location available at all
                _uiState.value = _uiState.value.copy(
                    isLocating = false,
                    error = "无法获取位置，请检查定位服务是否开启"
                )

            } catch (e: Exception) {
                // Step 4: Exception — last resort fallback
                try {
                    val last = withTimeoutOrNull(3_000L) {
                        LocationProvider.getLastKnownLocation(getApplication())
                    }
                    if (last != null) {
                        processLocationResult(last.latitude, last.longitude, last.source + " (缓存)")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLocating = false,
                            error = "定位失败，请检查GPS或稍后重试"
                        )
                    }
                } catch (e2: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        error = "定位服务异常，请手动选择城市"
                    )
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(locationDenied = true, isLocating = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null, locationDenied = false)
    }

    fun onLocationReceived(lat: Double, lon: Double, source: String) {
        viewModelScope.launch {
            // Reverse geocode to get real city name
            val locationName = WeatherRepository.reverseGeocode(lat, lon)
                ?: WeatherRepository.findNearestCity(lat, lon).name

            _uiState.value = _uiState.value.copy(isLocating = false, locationSource = source)

            // Try to find a matching hardcoded city first
            val nearest = WeatherRepository.findNearestCity(lat, lon)
            val distance = kotlin.math.sqrt(
                (nearest.latitude - lat) * (nearest.latitude - lat) +
                (nearest.longitude - lon) * (nearest.longitude - lon)
            )

            // If within ~50km, use the hardcoded city (has full weather data)
            if (distance < 1.0) {  // ~1 degree ≈ 111km, so 1.0 ≈ 111km
                selectCityByLocation(nearest.copy(name = locationName))
            } else {
                // Far from any hardcoded city — search by name and use dynamic result
                val results = WeatherRepository.searchCityByName(
                    locationName.substringBefore(",").trim(), 5
                )
                val matched = results.minByOrNull { r ->
                    kotlin.math.sqrt((r.latitude - lat).pow(2) + (r.longitude - lon).pow(2))
                } ?: WeatherRepository.findNearestCity(lat, lon)
                selectCityByLocation(matched.copy(name = locationName))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeJob?.cancel()
        weatherJob?.cancel()
    }
}