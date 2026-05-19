@file:OptIn(ExperimentalMaterial3Api::class)
package com.desk.weather.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desk.weather.data.*
import com.desk.weather.data.ThemeDefinitions
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ============================================================
// 入口
// ============================================================
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel(),
    onLocationChange: (Int) -> Unit = {},
    isRoundDisplay: Boolean = false,
    roundDiameter: androidx.compose.ui.unit.Dp? = null,
    gravityRotation: Int = 0,
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = uiState.themeColors

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) viewModel.onLocationPermissionGranted()
        else viewModel.onLocationPermissionDenied()
    }

    // ── 动态天气粒子系统 ──
    val particles = remember { mutableStateListOf<RainParticle>() }
    LaunchedEffect(colors.isDynamicWeather, uiState.weatherIcon) {
        if (!colors.isDynamicWeather) return@LaunchedEffect
        particles.clear()

        val isRainy = uiState.weatherIcon in listOf("🌧️","🌦️","⛈️")
        val isSnowy = uiState.weatherIcon in listOf("❄️","🌨️")
        val isLightning = uiState.weatherIcon == "⛈️"
        val isFoggy = uiState.weatherIcon == "🌫️"

        val count = when {
            isRainy -> 120
            isSnowy -> 80
            isLightning -> 60
            isFoggy -> 40
            else -> 50  // 晴天的星星/萤火虫
        }

        repeat(count) {
            particles.add(RainParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 1.3f - 0.15f,
                speed = when {
                    isRainy -> 0.008f + Random.nextFloat() * 0.012f
                    isSnowy -> 0.001f + Random.nextFloat() * 0.003f
                    else -> 0.0005f + Random.nextFloat() * 0.001f
                },
                size = when {
                    isRainy -> 1f + Random.nextFloat() * 2f
                    isSnowy -> 3f + Random.nextFloat() * 5f
                    else -> 1f + Random.nextFloat() * 2f
                },
                drift = (Random.nextFloat() - 0.5f) * 0.003f,
                opacity = 0.3f + Random.nextFloat() * 0.5f,
                isRain = isRainy,
                isSnow = isSnowy,
                isLightning = isLightning,
                isFog = isFoggy,
                // 雪花特有
                angle = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 2f,
                // 萤火虫
                pulse = Random.nextFloat() * 360f,
                pulseSpeed = 1f + Random.nextFloat() * 2f,
            ))
        }
    }

    // 粒子动画循环
    var lightningFlash by remember { mutableStateOf(0f) }
    LaunchedEffect(colors.isDynamicWeather, uiState.weatherIcon) {
        if (!colors.isDynamicWeather) return@LaunchedEffect
        val isLightning = uiState.weatherIcon == "⛈️"
        while (true) {
            delay(16)
            particles.forEachIndexed { i, p ->
                val updated = p.copy(
                    y = p.y + p.speed,
                    x = p.x + p.drift,
                    angle = if (p.isSnow) p.angle + p.rotSpeed else p.angle,
                    pulse = p.pulse + p.pulseSpeed,
                )
                if (updated.y > 1.2f) {
                    val newX = if (p.isRain) Random.nextFloat() else (p.x + p.drift * 50).coerceIn(0f, 1f)
                    particles[i] = updated.copy(y = -0.1f, x = newX)
                } else {
                    particles[i] = updated
                }
            }
            // 闪电效果
            if (isLightning && Random.nextFloat() < 0.003f) {
                lightningFlash = 1f
                delay(80)
                lightningFlash = 0f
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "bgOffset"
    )

    val bgColors = if (colors.isDynamicWeather && lightningFlash > 0) {
        listOf(
            colors.topGradient.copy(alpha = 0.5f + lightningFlash * 0.5f),
            Color.White.copy(alpha = lightningFlash * 0.3f),
            colors.bottomGradient,
        )
    } else listOf(colors.topGradient, colors.bottomGradient)

    // 圆形屏幕：内容限制在正方形区域内
    val boxModifier = if (isRoundDisplay && roundDiameter != null) {
        Modifier
            .size(roundDiameter)
            .clip(CircleShape)
    } else {
        Modifier.fillMaxSize()
    }

    // 主内容应用重力翻转
            Box(
                modifier = boxModifier
                    .graphicsLayer {
                        rotationZ = gravityRotation.toFloat()
                    }
                    .background(brush = Brush.verticalGradient(
                        colors = bgColors, startY = 0f, endY = 2000f + animatedOffset * 300f
                    ))
            ) {
        // ── 动态天气粒子 ──
        if (colors.isDynamicWeather) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    val alpha = when {
                        p.isFog -> 0.05f + 0.03f * sin(p.pulse * 0.05f)
                        p.isRain -> 0.4f * p.opacity
                        p.isSnow -> 0.6f * p.opacity
                        else -> 0.3f + 0.2f * sin(p.pulse * 0.1f)  // 萤火虫闪烁
                    }
                    when {
                        p.isRain -> {
                            // 雨滴：斜线
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(p.x * size.width, p.y * size.height),
                                end = Offset(p.x * size.width - 4, p.y * size.height + p.size * 8),
                                strokeWidth = p.size
                            )
                        }
                        p.isSnow -> {
                            // 雪花：旋转的六边形点
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = p.size,
                                center = Offset(p.x * size.width, p.y * size.height)
                            )
                        }
                        p.isLightning -> {
                            // 闪电：随机闪烁点
                            if (Random.nextFloat() < 0.1f) {
                                drawCircle(
                                    color = Color.Yellow.copy(alpha = 0.8f),
                                    radius = p.size * 2,
                                    center = Offset(p.x * size.width, p.y * size.height * 0.5f)
                                )
                            }
                        }
                        p.isFog -> {
                            // 雾气：大面积模糊圆
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = p.size * 20,
                                center = Offset(p.x * size.width, p.y * size.height)
                            )
                        }
                        else -> {
                            // 萤火虫
                            drawCircle(
                                color = Color(0xFFFFFF99).copy(alpha = alpha),
                                radius = p.size,
                                center = Offset(p.x * size.width, p.y * size.height * 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // ── 装饰光晕 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopEnd)
                .offset(x = (-150).dp, y = (-100).dp)
                .size(400.dp)
                .clip(CircleShape)
                .background(brush = Brush.radialGradient(
                    colors = listOf(colors.accentColor.copy(alpha = 0.12f), Color.Transparent)
                ))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = (-200).dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(brush = Brush.radialGradient(
                    colors = listOf(colors.accentColor.copy(alpha = 0.08f), Color.Transparent)
                ))
        )

        if (uiState.isLoading && uiState.forecast.isEmpty()) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = colors.textPrimary.copy(alpha = 0.8f), strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text("正在获取天气数据...", color = colors.textSecondary, fontSize = 16.sp)
            }
        } else {
            MainContent(
                viewModel = viewModel,
                uiState = uiState, colors = colors,
                onCitySelect = { idx -> viewModel.selectCity(idx); onLocationChange(idx) },
                onThemeCycle = { viewModel.cycleTheme() },
                onLocationRequest = {
                    locationPermissionLauncher.launch(arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                },
                onLocationDeniedDismiss = { viewModel.dismissError() },
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onSearchFocus = { viewModel.onSearchFocus(it) },
                onCitySelectedFromSearch = { viewModel.onCitySelectedFromSearch(it) },
                onPinyinNavClick = { viewModel.scrollToPinyinInitial(it) },
                onThemeSelected = { viewModel.setTheme(it) },
                onToggleLayoutEditor = { viewModel.toggleLayoutEditor() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTogglePin = { viewModel.togglePin(it) },
                onIsFavorite = { viewModel.isFavorite(it) },
                onIsPinned = { viewModel.isPinned(it) },
                onMoveWidget = { z, f, t -> viewModel.moveWidget(z, f, t) },
                onToggleWidgetVisibility = { z, w -> viewModel.toggleWidgetVisibility(z, w) },
                onSetEditingZone = { viewModel.setEditingZone(it) },
                onApplyTemplate = { viewModel.applyTemplate(it) },
                airQualityData = viewModel.getAirQualityData(),
            )
        }
    }
}

// ============================================================
// 主内容区
// ============================================================
@Composable
private fun MainContent(
    viewModel: MainScreenViewModel,
    uiState: MainUiState, colors: ThemeColors,
    onCitySelect: (Int) -> Unit, onThemeCycle: () -> Unit,
    onLocationRequest: () -> Unit, onLocationDeniedDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit, onSearchFocus: (Boolean) -> Unit,
    onCitySelectedFromSearch: (WeatherLocation) -> Unit, onPinyinNavClick: (String) -> Unit,
    onThemeSelected: (AppTheme) -> Unit, onToggleLayoutEditor: () -> Unit,
    onToggleFavorite: (WeatherLocation) -> Unit, onTogglePin: (WeatherLocation) -> Unit,
    onIsFavorite: (WeatherLocation) -> Boolean, onIsPinned: (WeatherLocation) -> Boolean,
    onMoveWidget: (String, Int, Int) -> Unit, onToggleWidgetVisibility: (String, String) -> Unit,
    onSetEditingZone: (String) -> Unit, onApplyTemplate: (String) -> Unit,
    airQualityData: AirQualityData?,
) {
    // Adaptive screen sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Adaptive weight: on narrow screens use equal weights
    val leftWeight = if (screenWidth < 400.dp) 1f else 1f
    val rightWeight = if (screenWidth < 400.dp) 1f else 1.35f

    // Adaptive padding
    val horizontalPadding = if (screenWidth < 400.dp) 16.dp else if (screenWidth < 600.dp) 20.dp else 28.dp
    val itemSpacing = if (screenWidth < 400.dp) 16.dp else if (screenWidth < 600.dp) 20.dp else 28.dp

    when {
        uiState.showLayoutEditor -> LayoutEditorScreen(uiState, colors, onToggleLayoutEditor, onMoveWidget, onToggleWidgetVisibility, onSetEditingZone, onApplyTemplate)
        uiState.isSearchMode -> SearchScreen(
            uiState, colors, onSearchQueryChange, onSearchFocus,
            onCitySelectedFromSearch, onPinyinNavClick,
            onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned,
            onBack = { viewModel.onSearchBack() },
        )
        else -> Row(
            modifier = Modifier.fillMaxSize().padding(horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            LeftPanel(Modifier.weight(leftWeight), uiState, colors, onThemeCycle, onThemeSelected, airQualityData)
            val handleSearchOpen = { viewModel.onSearchOpen() }
            RightPanel(
                Modifier.weight(rightWeight), uiState, colors,
                onCitySelect, onLocationRequest,
                onSearchOpen = handleSearchOpen,
                onPinyinNavClick = onPinyinNavClick,
                onLayoutEditorClick = onToggleLayoutEditor,
            )
        }
    }
}

// ============================================================
// 搜索界面（含城市管理）
// ============================================================
@Composable
private fun SearchScreen(
    uiState: MainUiState, colors: ThemeColors,
    onSearchQueryChange: (String) -> Unit, onSearchFocus: (Boolean) -> Unit,
    onCitySelected: (WeatherLocation) -> Unit, onPinyinNavClick: (String) -> Unit,
    onToggleFavorite: (WeatherLocation) -> Unit, onTogglePin: (WeatherLocation) -> Unit,
    onIsFavorite: (WeatherLocation) -> Boolean, onIsPinned: (WeatherLocation) -> Boolean,
    onBack: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // 返回按钮
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack),
                color = colors.cardHighlight
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("← 返回", fontSize = 14.sp, color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            // 搜索框
            SearchBar(query = uiState.searchQuery, colors = colors, onQueryChange = onSearchQueryChange, onFocusChange = onSearchFocus)
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // ── 有搜索词时显示结果 ──
                if (uiState.searchQuery.isNotBlank()) {
                    if (uiState.searchResults.isEmpty()) {
                        item { Text("未找到城市：「${uiState.searchQuery}」", color = colors.textSecondary, fontSize = 16.sp, modifier = Modifier.padding(16.dp)) }
                    } else {
                        item { Text("找到 ${uiState.searchResults.size} 个城市", fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(bottom = 4.dp)) }
                        items(uiState.searchResults) { city ->
                            CityListItem(city, uiState, colors, onCitySelected, onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned)
                        }
                    }
                } else {
                    // ── 无搜索词：置顶 + 收藏 + 最近 ──
                    val pinned = uiState.pinnedLocations
                    val favorites = uiState.favoriteLocations.filter { !pinned.contains(it) }
                    val recent = uiState.recentLocations.filter { !pinned.contains(it) && !favorites.contains(it) }

                    if (pinned.isNotEmpty()) {
                        item { SectionHeader("📌 置顶城市", colors) }
                        items(pinned) { city ->
                            CityListItem(city, uiState, colors, onCitySelected, onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned, showPinButton = false)
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        item { SectionHeader("⭐ 收藏城市", colors) }
                        items(favorites) { city ->
                            CityListItem(city, uiState, colors, onCitySelected, onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned)
                        }
                    }
                    if (recent.isNotEmpty()) {
                        item { SectionHeader("🕐 最近访问", colors) }
                        items(recent.take(5)) { city ->
                            CityListItem(city, uiState, colors, onCitySelected, onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned, showFavoriteButton = false)
                        }
                    }

                    // ── 全量城市（按拼音）──
                    item { Spacer(Modifier.height(8.dp)); SectionHeader("🌍 所有城市（A-Z 快速导航）", colors) }
                    uiState.pinyinInitials.forEach { initial ->
                        val citiesForLetter = uiState.locationsByPinyin[initial] ?: emptyList()
                        if (citiesForLetter.isNotEmpty()) {
                            item { Text("  $initial", fontSize = 13.sp, color = colors.accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                            items(citiesForLetter) { city ->
                                CityListItem(city, uiState, colors, onCitySelected, onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned)
                            }
                        }
                    }
                }
            }
        }

        // ── 拼音首字母侧边栏 ──
        PinyinSidebar(initials = uiState.pinyinInitials, onLetterClick = onPinyinNavClick, colors = colors)
    }
}

@Composable
private fun SectionHeader(text: String, colors: ThemeColors) {
    Text(text, fontSize = 15.sp, color = colors.accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SearchBar(query: String, colors: ThemeColors, onQueryChange: (String) -> Unit, onFocusChange: (Boolean) -> Unit) {
    Surface(color = colors.cardHighlight, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Text("🔍", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = query, onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 17.sp),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) Text("搜索城市（中文/拼音/首字母）...", color = colors.textSecondary.copy(alpha = 0.7f), fontSize = 17.sp)
                            innerTextField()
                        }
                    },
                )
            }
            if (query.isNotEmpty()) {
                Surface(modifier = Modifier
                    .size(24.dp)
                    .clickable { onQueryChange("") },
                    color = colors.textSecondary.copy(alpha = 0.3f), shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("✕", fontSize = 12.sp, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinyinSidebar(initials: List<String>, onLetterClick: (String) -> Unit, colors: ThemeColors) {
    Column(
        modifier = Modifier.width(36.dp).fillMaxHeight()
            .background(colors.cardHighlight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        initials.forEach { initial ->
            Text(initial, fontSize = 14.sp, color = colors.accentColor, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLetterClick(initial) }.padding(vertical = 2.dp))
        }
    }
}

@Composable
private fun CityListItem(
    city: WeatherLocation, uiState: MainUiState, colors: ThemeColors,
    onCitySelected: (WeatherLocation) -> Unit,
    onToggleFavorite: (WeatherLocation) -> Unit, onTogglePin: (WeatherLocation) -> Unit,
    onIsFavorite: (WeatherLocation) -> Boolean, onIsPinned: (WeatherLocation) -> Boolean,
    showFavoriteButton: Boolean = true, showPinButton: Boolean = true,
) {
    val isSelected = uiState.availableLocations.getOrNull(uiState.selectedLocationIndex)?.name == city.name
    Surface(
        color = if (isSelected) colors.accentColor.copy(alpha = 0.2f) else colors.cardHighlight,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onCitySelected(city) }
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌍", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(city.name, fontSize = 17.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                Text("${city.country} · ${city.pinyinInitial}", fontSize = 12.sp, color = colors.textSecondary)
            }
            if (showPinButton) {
                Text(if (onIsPinned(city)) "📌" else "📍", fontSize = 16.sp, modifier = Modifier.clickable { onTogglePin(city) })
                Spacer(Modifier.width(6.dp))
            }
            if (showFavoriteButton) {
                Text(if (onIsFavorite(city)) "⭐" else "☆", fontSize = 16.sp, modifier = Modifier.clickable { onToggleFavorite(city) })
            }
            Spacer(Modifier.width(8.dp))
            Text(city.pinyinInitial, fontSize = 14.sp, color = colors.accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================
// 左面板
// ============================================================
@Composable
private fun LeftPanel(
    modifier: Modifier, uiState: MainUiState, colors: ThemeColors,
    onThemeCycle: () -> Unit, onThemeSelected: (AppTheme) -> Unit,
    airQualityData: AirQualityData?,
) {
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAirQualitySheet by remember { mutableStateOf(false) }

    // Read widget visibility from layoutConfig and sort by order
    val leftZoneWidgets = uiState.layoutConfig.zones.find { it.id == "left" }?.widgets?.sortedBy { it.order } ?: emptyList()
    val clockVisible = leftZoneWidgets.find { it.type == "CLOCK" }?.visible ?: true
    val dateVisible = leftZoneWidgets.find { it.type == "DATE" }?.visible ?: true
    val locationVisible = leftZoneWidgets.find { it.type == "LOCATION" }?.visible ?: true
    val temperatureVisible = leftZoneWidgets.find { it.type == "TEMPERATURE" }?.visible ?: true
    val detailsVisible = leftZoneWidgets.find { it.type == "DETAILS" }?.visible ?: true
    val airQualityVisible = leftZoneWidgets.find { it.type == "AIR_QUALITY" }?.visible ?: true

    // Adaptive screen sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Adaptive font sizes
    val clockFontSize = if (screenWidth < 400.dp) 48.sp else if (screenWidth < 600.dp) 58.sp else 68.sp
    val temperatureFontSize = if (screenWidth < 400.dp) 48.sp else if (screenWidth < 600.dp) 58.sp else 68.sp
    val weatherIconSize = if (screenWidth < 400.dp) 56.sp else if (screenWidth < 600.dp) 68.sp else 80.sp
    val locationNameFontSize = if (screenWidth < 400.dp) 24.sp else if (screenWidth < 600.dp) 27.sp else 30.sp

    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        // 顶部：日期/时间
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    if (dateVisible) {
                        Text(uiState.currentDate, fontSize = 18.sp, color = colors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (clockVisible) {
                        Text(uiState.currentTime, fontSize = clockFontSize, color = colors.textPrimary, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    ThemeButton(colors = colors, onClick = { showThemeSheet = true })
                    Spacer(Modifier.height(8.dp))
                    if (uiState.locationSource.isNotEmpty()) {
                        Surface(color = colors.cardHighlight, shape = RoundedCornerShape(10.dp)) {
                            Text(uiState.locationSource, fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.8f), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (locationVisible) {
                Text(uiState.timezoneDisplay, fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.6f))
            }
        }

        // 底部：天气主信息
        Column {
            if (locationVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 22.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(uiState.locationName, fontSize = locationNameFontSize, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp))
            }
            if (temperatureVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uiState.weatherIcon, fontSize = weatherIconSize)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(uiState.temperature, fontSize = temperatureFontSize, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text("${uiState.weatherDescription} ${uiState.feelsLike}", fontSize = 16.sp, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
            if (detailsVisible) {
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    WeatherDetailItem(emoji = "💧", label = "湿度", value = uiState.humidity, colors = colors)
                    WeatherDetailItem(emoji = "💨", label = "风速", value = uiState.windSpeed, colors = colors)
                }
            }
            // 空气质量快捷入口
            if (airQualityVisible && uiState.airQuality != null) {
                if (detailsVisible) Spacer(Modifier.height(4.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAirQualitySheet = true }) {
                    Text("🫁", fontSize = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("空气", fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.8f))
                    Spacer(Modifier.height(2.dp))
                    Text(uiState.airQuality!!.value, fontSize = 18.sp, color = Color(uiState.airQuality!!.color), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // 主题选择居中弹窗（用 Dialog 代替 BottomSheetScaffold，无难看的拖动手柄）
    if (showThemeSheet) {
        AlertDialog(
            onDismissRequest = { showThemeSheet = false },
            containerColor = colors.surfaceColor,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("选择主题", fontSize = 20.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text("当前：${colors.name}", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.clip(CircleShape).clickable { showThemeSheet = false },
                        color = colors.cardHighlight
                    ) {
                        Text("✕", fontSize = 16.sp, color = colors.textSecondary, modifier = Modifier.padding(8.dp))
                    }
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ThemeDefinitions.themeList().size) { idx ->
                        val (theme, tc) = ThemeDefinitions.themeList()[idx]
                        val isSelected = colors.name == tc.name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeSelected(theme)
                                    showThemeSheet = false
                                },
                            color = if (isSelected) tc.accentColor.copy(alpha = 0.2f) else tc.cardHighlight,
                            shape = RoundedCornerShape(14.dp),
                            border = if (isSelected) BorderStroke(2.dp, tc.accentColor) else null,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 大尺寸渐变预览（60×36dp）
                                Box(
                                    Modifier
                                        .size(60.dp, 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(tc.topGradient, tc.bottomGradient)))
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        theme.displayName,
                                        fontSize = 16.sp,
                                        color = tc.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        theme.description,
                                        fontSize = 11.sp,
                                        color = tc.textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Surface(color = tc.accentColor, shape = CircleShape) {
                                        Text("✓", fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = colors.cardHighlight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 16.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("自动切换", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                    Text("根据白天/黑夜自动切换日间/夜间主题", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // 空气质量详情底部弹出
    if (showAirQualitySheet && airQualityData != null) {
        BottomSheetScaffold(
            sheetContent = {
                AirQualitySheet(airQualityData, colors)
            },
            containerColor = colors.surfaceColor,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            scaffoldState = rememberBottomSheetScaffoldState(),
        ) { }
    }
}

@Composable
private fun AirQualitySheet(aq: AirQualityData, colors: ThemeColors) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🫁 空气质量详情", fontSize = 20.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val (label, col) = when (aq.aqiLevel) {
                "优" -> "✅ 优" to Color(0xFF4CAF50)
                "良" -> "🟡 良" to Color(0xFFFFEB3B)
                "轻度" -> "🟠 轻度" to Color(0xFFFF9800)
                "中度" -> "🔴 中度" to Color(0xFFFF5722)
                "重度" -> "💔 重度" to Color(0xFFE91E63)
                else -> "💀 严重" to Color(0xFF9C27B0)
            }
            Surface(color = col.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                Text(label, fontSize = 16.sp, color = col, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(20.dp))

        val items = listOf(
            Triple("AQI (美标)", "${aq.usAqi}", aq.aqiLevel to aq.aqiColor),
            Triple("PM2.5", "%.1f μg/m³".format(aq.pm25), levelColor(aq.pm25, 35.0, 75.0, 150.0, 250.0, 350.0)),
            Triple("PM10", "%.1f μg/m³".format(aq.pm10), levelColor(aq.pm10, 50.0, 150.0, 250.0, 350.0, 420.0)),
            Triple("O₃ (臭氧)", "%.1f μg/m³".format(aq.ozone), levelColor(aq.ozone, 100.0, 160.0, 215.0, 265.0, 800.0)),
            Triple("NO₂ (二氧化氮)", "%.1f μg/m³".format(aq.no2), levelColor(aq.no2, 100.0, 200.0, 700.0, 1200.0, 2340.0)),
            Triple("CO (一氧化碳)", "%.2f mg/m³".format(aq.co), if (aq.co <= 2.0) "低" to 0xFF4CAF50 else if (aq.co <= 4.0) "中" to 0xFFFFEB3B else "高" to 0xFFFF5722),
        )

        items.forEach { (label, value, pair) ->
            val (level, col) = pair
            Surface(color = colors.cardHighlight, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(value, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(12.dp))
                    Surface(color = Color(col).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(level, fontSize = 12.sp, color = Color(col), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("数据来源：Open-Meteo Air Quality API（每小时更新）", fontSize = 11.sp, color = colors.textSecondary)
    }
}

private fun levelColor(v: Double, vararg thresholds: Double): Pair<String, Long> {
    var level = "极高" to 0xFF9C27B0
    val names = listOf("低", "中", "高", "超高", "极高")
    for ((i, t) in thresholds.withIndex()) {
        if (v <= t) { level = names.getOrElse(i) { "极高" } to when(i) {
            0 -> 0xFF4CAF50; 1 -> 0xFFFFEB3B; 2 -> 0xFFFF9800; 3 -> 0xFFFF5722; else -> 0xFF9C27B0
        }; break }
    }
    return level
}

@Composable private fun ThemeButton(colors: ThemeColors, onClick: () -> Unit) {
    Surface(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), color = colors.cardHighlight, shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎨", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(colors.name, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable private fun WeatherDetailItem(emoji: String, label: String, value: String, colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.8f))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================
// 右面板
// ============================================================
@Composable
private fun RightPanel(
    modifier: Modifier, uiState: MainUiState, colors: ThemeColors,
    onCitySelect: (Int) -> Unit, onLocationRequest: () -> Unit,
    onSearchOpen: () -> Unit, onPinyinNavClick: (String) -> Unit, onLayoutEditorClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        // 工具栏
        val rightZoneWidgets = uiState.layoutConfig.zones.find { it.id == "right" }?.widgets?.sortedBy { it.order } ?: emptyList()
        val forecastVisible = rightZoneWidgets.find { it.type == "FORECAST_7D" }?.visible ?: true
        val locationBtnVisible = rightZoneWidgets.find { it.type == "LOCATION" }?.visible ?: true

        // Adaptive screen sizing
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        // Adaptive forecast card width
        val cardWidth = when {
            screenWidth < 400.dp -> 64.dp
            screenWidth < 600.dp -> 72.dp
            else -> 80.dp
        }

        // Adaptive font sizes
        val toolbarFontSize = if (screenWidth < 400.dp) 12.sp else 14.sp

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (locationBtnVisible) {
                LocationButton(isLocating = uiState.isLocating, useMyLocation = uiState.useMyLocationEnabled, onClick = onLocationRequest, colors = colors)
                Spacer(Modifier.width(10.dp))
            }
            Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onSearchOpen), color = colors.cardHighlight, shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text("搜索城市", fontSize = toolbarFontSize, color = colors.textPrimary)
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onLayoutEditorClick), color = colors.cardHighlight, shape = RoundedCornerShape(16.dp)) {
                Text("📐 布局", fontSize = toolbarFontSize, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("🌍 ${uiState.locationName}", fontSize = 13.sp, color = colors.textSecondary)
        }

        if (uiState.error != null && uiState.locationDenied) {
            Spacer(Modifier.height(10.dp))
            Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
                    Column {
                        Text("定位权限被拒绝", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("请在系统设置中开启定位权限", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        if (forecastVisible) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                Text("📅", fontSize = 16.sp); Spacer(Modifier.width(6.dp))
                Text("七日天气预报", fontSize = 16.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
            }

            // 七天预报：自动换行布局，不再横向滚动
            val forecastItems = uiState.forecast
            val rows = forecastItems.chunked(4) // 每行最多4个
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { forecast ->
                            ForecastCard(
                                item = forecast,
                                isToday = forecast == forecastItems.first(),
                                colors = colors,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                        // 填充空白让最后一行对齐
                        repeat(4 - rowItems.size) {
                            Spacer(Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 布局编辑器（支持拖拽排序）
// ============================================================
@Composable
private fun LayoutEditorScreen(
    uiState: MainUiState, colors: ThemeColors, onClose: () -> Unit,
    onMoveWidget: (String, Int, Int) -> Unit, onToggleWidgetVisibility: (String, String) -> Unit,
    onSetEditingZone: (String) -> Unit, onApplyTemplate: (String) -> Unit,
) {
    var draggingFrom by remember { mutableStateOf<Int?>(null) }
    var draggingZone by remember { mutableStateOf("") }
    var dragOverIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📐 布局编辑器", fontSize = 28.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Surface(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClose), color = colors.cardHighlight) {
                Text("✕ 关闭", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("长按区块后拖动调整顺序，点击 👁 切换显示/隐藏", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 左侧：区域选择 + 区块列表
            Column(modifier = Modifier.weight(1f)) {
                // 区域选择标签
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("left" to "左侧区域", "right" to "右侧区域").forEach { (id, label) ->
                        val isSelected = uiState.editingZone == id
                        Surface(
                            modifier = Modifier.clickable { onSetEditingZone(id) },
                            color = if (isSelected) colors.accentColor else colors.cardHighlight,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(2.dp, colors.accentColor) else null
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // 区块拖拽列表
                val zoneWidgets = uiState.layoutConfig.zones.find { it.id == uiState.editingZone }?.widgets ?: emptyList()

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(zoneWidgets) { index, widget ->
                        val isDragging = draggingFrom == index && draggingZone == uiState.editingZone
                        val isDropTarget = dragOverIndex == index

                        Surface(
                            color = when {
                                isDragging -> colors.accentColor.copy(alpha = 0.4f)
                                isDropTarget -> colors.accentColor.copy(alpha = 0.15f)
                                else -> colors.cardHighlight
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (isDragging) { alpha = 0.7f; scaleX = 1.05f; scaleY = 1.05f }
                                }
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingFrom = index; draggingZone = uiState.editingZone
                                        },
                                        onDragEnd = {
                                            val from = draggingFrom; val to = dragOverIndex
                                            if (from != null && to != null && from != to) {
                                                onMoveWidget(uiState.editingZone, from, to)
                                            }
                                            draggingFrom = null; draggingZone = ""; dragOverIndex = null
                                        },
                                        onDragCancel = {
                                            draggingFrom = null; draggingZone = ""; dragOverIndex = null
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            dragOverIndex = index
                                        }
                                    )
                                }
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("☰", fontSize = 18.sp, color = colors.textSecondary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(widget.type, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                    Text(WidgetType.entries.find { it.name == widget.type }?.description ?: "", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Surface(
                                    color = if (widget.visible) colors.accentColor.copy(alpha = 0.3f) else colors.cardHighlight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (widget.visible) "👁 显示" else "🚫 隐藏", fontSize = 12.sp, color = colors.textPrimary,
                                        modifier = Modifier.clickable { onToggleWidgetVisibility(uiState.editingZone, widget.type) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 右侧：快速模板（竖向排列展示）
            Column(modifier = Modifier.weight(0.8f)) {
                Text("📋 快速模板", fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))

                val templates = listOf(
                    "默认" to "均衡展示所有信息",
                    "极简" to "仅保留核心数据",
                    "信息全开" to "最大化信息显示"
                )
                templates.forEach { (name, desc) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onApplyTemplate(name) },
                        color = colors.cardHighlight, shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                Text(desc, fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Text("→", fontSize = 18.sp, color = colors.accentColor)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 共用组件
// ============================================================
@Composable private fun LocationButton(isLocating: Boolean, useMyLocation: Boolean, onClick: () -> Unit, colors: ThemeColors) {
    val bgColor by animateColorAsState(targetValue = if (useMyLocation) colors.accentColor.copy(alpha = 0.3f) else colors.cardHighlight, label = "locBtnBg")
    Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), color = bgColor, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isLocating) CircularProgressIndicator(Modifier.size(18.dp), color = colors.accentColor, strokeWidth = 2.dp)
            else Text(if (useMyLocation) "📍" else "🎯", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(if (isLocating) "定位中..." else if (useMyLocation) "我的位置" else "使用位置",
                fontSize = 14.sp, color = if (useMyLocation) colors.accentColor else colors.textPrimary,
                fontWeight = if (useMyLocation) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable private fun ForecastCard(item: DayForecastItem, isToday: Boolean, colors: ThemeColors, modifier: Modifier = Modifier) {
    val cardBg = if (isToday) colors.accentColor.copy(alpha = 0.18f) else colors.cardHighlight
    Card(
        modifier = modifier.width(80.dp).heightIn(min = 110.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                item.dayLabel,
                fontSize = if (isToday) 13.sp else 11.sp,
                color = if (isToday) colors.accentColor else colors.textSecondary,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
            )
            Text(item.icon, fontSize = 24.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.tempMax, fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(item.tempMin, fontSize = 12.sp, color = colors.textSecondary.copy(alpha = 0.65f))
            }
            if (item.precipProb.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💧", fontSize = 10.sp)
                    Text(item.precipProb, fontSize = 10.sp, color = Color(0xFF64B5F6))
                }
            } else {
                Spacer(Modifier.height(0.dp))
            }
        }
    }
}

// ============================================================
// 动态粒子数据类
// ============================================================
data class RainParticle(
    val x: Float, val y: Float, val speed: Float, val size: Float,
    val drift: Float = 0f, val opacity: Float = 1f,
    val isRain: Boolean = false, val isSnow: Boolean = false,
    val isLightning: Boolean = false, val isFog: Boolean = false,
    val angle: Float = 0f, val rotSpeed: Float = 0f,
    val pulse: Float = 0f, val pulseSpeed: Float = 0f,
)