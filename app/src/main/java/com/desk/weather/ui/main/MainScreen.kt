@file:OptIn(ExperimentalMaterial3Api::class)
package com.desk.weather.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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
            else -> 50
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
                angle = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 2f,
                pulse = Random.nextFloat() * 360f,
                pulseSpeed = 1f + Random.nextFloat() * 2f,
            ))
        }
    }

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

    Box(
        modifier = boxModifier
            .graphicsLayer { rotationZ = gravityRotation.toFloat() }
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
                        else -> 0.3f + 0.2f * sin(p.pulse * 0.1f)
                    }
                    when {
                        p.isRain -> {
                            drawLine(
                                color = Color.White.copy(alpha = alpha),
                                start = Offset(p.x * size.width, p.y * size.height),
                                end = Offset(p.x * size.width - 4, p.y * size.height + p.size * 8),
                                strokeWidth = p.size
                            )
                        }
                        p.isSnow -> {
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = p.size,
                                center = Offset(p.x * size.width, p.y * size.height)
                            )
                        }
                        p.isLightning -> {
                            if (Random.nextFloat() < 0.1f) {
                                drawCircle(
                                    color = Color.Yellow.copy(alpha = 0.8f),
                                    radius = p.size * 2,
                                    center = Offset(p.x * size.width, p.y * size.height * 0.5f)
                                )
                            }
                        }
                        p.isFog -> {
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = p.size * 20,
                                center = Offset(p.x * size.width, p.y * size.height)
                            )
                        }
                        else -> {
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
                onSetVisualStyle = { viewModel.setVisualStyle(it) },
                airQualityData = viewModel.getAirQualityData(),
            )
        }
    }
}

// ============================================================
// 主内容区（无左右分区，单列自适应布局）
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
    onSetVisualStyle: (VisualStyle) -> Unit,
    airQualityData: AirQualityData?,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Adaptive sizing based on screen dimensions
    val isCompact = screenWidth < 400.dp
    val isMedium = screenWidth in 400.dp..599.dp

    when {
        uiState.showLayoutEditor -> LayoutEditorScreen(
            uiState, colors, onToggleLayoutEditor, onSetVisualStyle
        )
        uiState.isSearchMode -> SearchScreen(
            uiState, colors, onSearchQueryChange, onSearchFocus,
            onCitySelectedFromSearch, onPinyinNavClick,
            onToggleFavorite, onTogglePin, onIsFavorite, onIsPinned,
            onBack = { viewModel.onSearchBack() },
        )
        else -> SingleColumnLayout(
            viewModel = viewModel,
            uiState = uiState, colors = colors,
            screenWidth = screenWidth, screenHeight = screenHeight,
            onThemeCycle = onThemeCycle, onThemeSelected = onThemeSelected,
            onLocationRequest = onLocationRequest,
            onSearchOpen = { viewModel.onSearchOpen() },
            onPinyinNavClick = onPinyinNavClick,
            onLayoutEditorClick = onToggleLayoutEditor,
            airQualityData = airQualityData,
        )
    }
}

// ============================================================
// 单列自适应布局（替代原有左右分区）
// ============================================================
@Composable
private fun SingleColumnLayout(
    viewModel: MainScreenViewModel,
    uiState: MainUiState, colors: ThemeColors,
    screenWidth: androidx.compose.ui.unit.Dp, screenHeight: androidx.compose.ui.unit.Dp,
    onThemeCycle: () -> Unit, onThemeSelected: (AppTheme) -> Unit,
    onLocationRequest: () -> Unit, onSearchOpen: () -> Unit,
    onPinyinNavClick: (String) -> Unit, onLayoutEditorClick: () -> Unit,
    airQualityData: AirQualityData?,
) {
    val style = uiState.layoutConfig.visualStyle
    val layoutType = style.layoutType
    val isCompact = screenWidth < 400.dp
    val isMedium = screenWidth in 400.dp..599.dp

    // 布局类型决定字号大小
    val clockSize = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 48.sp else if (isMedium) 58.sp else 72.sp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 40.sp else if (isMedium) 50.sp else 64.sp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 72.sp else if (isMedium) 88.sp else 112.sp
    }
    val dateSize = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 18.sp else if (isMedium) 22.sp else 28.sp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 16.sp else if (isMedium) 18.sp else 24.sp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 14.sp else if (isMedium) 16.sp else 20.sp
    }
    val temperatureSize = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 56.sp else if (isMedium) 72.sp else 96.sp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 40.sp else if (isMedium) 52.sp else 68.sp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 32.sp else if (isMedium) 40.sp else 52.sp
    }
    val weatherIconSize = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 64.sp else if (isMedium) 82.sp else 108.sp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 44.sp else if (isMedium) 56.sp else 72.sp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 32.sp else if (isMedium) 40.sp else 52.sp
    }
    val locationSize = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 16.sp else if (isMedium) 20.sp else 24.sp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 14.sp else if (isMedium) 16.sp else 20.sp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 14.sp else if (isMedium) 16.sp else 18.sp
    }
    val spacing = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 10.dp else 16.dp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 8.dp else 12.dp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 6.dp else 10.dp
    }
    val detailsSpacing = when (layoutType) {
        LayoutType.TODAY_DETAIL -> if (isCompact) 32.dp else 48.dp
        LayoutType.WEEK_OVERVIEW -> if (isCompact) 20.dp else 32.dp
        LayoutType.MINIMAL_CLOCK -> if (isCompact) 16.dp else 24.dp
    }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showAirQualitySheet by remember { mutableStateOf(false) }

    // 水平内边距
    val hPadding = when {
        isCompact -> 16.dp
        isMedium -> 24.dp
        else -> 32.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPadding, vertical = 16.dp)
    ) {
        // ── 顶部工具栏 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 定位按钮
            LocationButton(
                isLocating = uiState.isLocating,
                useMyLocation = uiState.useMyLocationEnabled,
                onClick = onLocationRequest,
                colors = colors
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 搜索按钮
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onSearchOpen),
                    color = colors.cardHighlight,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔍", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("搜索", fontSize = 13.sp, color = colors.textPrimary)
                    }
                }
                // 布局编辑器按钮
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onLayoutEditorClick),
                    color = colors.cardHighlight,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "📐 布局",
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                // 主题按钮
                ThemeButton(colors = colors, onClick = { showThemeSheet = true })
            }
        }

        // ── 错误提示 ──
        if (uiState.error != null && uiState.locationDenied) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color.Red.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("定位权限被拒绝", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("请在系统设置中开启定位权限", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 布局类型渲染 ──
        if (layoutType == LayoutType.TODAY_DETAIL) {
            TodayDetailContent(
                uiState = uiState, colors = colors,
                clockSize = clockSize, dateSize = dateSize,
                temperatureSize = temperatureSize, weatherIconSize = weatherIconSize,
                locationSize = locationSize, spacing = spacing,
                screenWidth = screenWidth,
                showAirQualitySheet = showAirQualitySheet,
                onAirQualityClick = { showAirQualitySheet = true },
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        } else if (layoutType == LayoutType.WEEK_OVERVIEW) {
            WeekOverviewContent(
                uiState = uiState, colors = colors,
                clockSize = clockSize, dateSize = dateSize,
                temperatureSize = temperatureSize, weatherIconSize = weatherIconSize,
                locationSize = locationSize, spacing = spacing,
                screenWidth = screenWidth,
                showAirQualitySheet = showAirQualitySheet,
                onAirQualityClick = { showAirQualitySheet = true },
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        } else {
            MinimalClockContent(
                uiState = uiState, colors = colors,
                clockSize = clockSize, dateSize = dateSize,
                temperatureSize = temperatureSize, weatherIconSize = weatherIconSize,
                locationSize = locationSize, spacing = spacing,
                screenWidth = screenWidth,
                showAirQualitySheet = showAirQualitySheet,
                onAirQualityClick = { showAirQualitySheet = true },
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // ── 主题选择弹窗 ──
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
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(60.dp, 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(tc.topGradient, tc.bottomGradient)))
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(theme.displayName, fontSize = 16.sp, color = tc.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(theme.description, fontSize = 11.sp, color = tc.textSecondary)
                                }
                                if (isSelected) {
                                    Surface(color = tc.accentColor, shape = CircleShape) {
                                        Text("✓", fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
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

    // ── 空气质量详情 ──
    if (showAirQualitySheet && airQualityData != null) {
        BottomSheetScaffold(
            sheetContent = { AirQualitySheet(airQualityData, colors) },
            containerColor = colors.surfaceColor,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            scaffoldState = rememberBottomSheetScaffoldState(),
        ) {}
    }
}

// ============================================================
// 搜索界面
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
    val hPadding = 28.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPadding, vertical = 16.dp)
    ) {
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
        SearchBar(query = uiState.searchQuery, colors = colors, onQueryChange = onSearchQueryChange, onFocusChange = onSearchFocus)
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Surface(
                    modifier = Modifier.size(24.dp).clickable { onQueryChange("") },
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
// 布局编辑器（简化为只有3种视觉样式）
// ============================================================
@Composable
private fun LayoutEditorScreen(
    uiState: MainUiState, colors: ThemeColors,
    onClose: () -> Unit,
    onSetVisualStyle: (VisualStyle) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📐 布局编辑器", fontSize = 28.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClose),
                color = colors.cardHighlight
            ) {
                Text("✕ 关闭", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("选择视觉布局样式，内容自动适配屏幕", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.height(20.dp))

        // 视觉布局样式选择
        Text("🎨 视觉布局（三种）", fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))

        // 三列或自适应展示
        val isCompact = screenWidth < 500.dp
        if (isCompact) {
            // 窄屏：垂直堆叠
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VisualStyle.entries.forEach { style ->
                    VisualStyleCard(style = style, isSelected = uiState.layoutConfig.visualStyle == style, colors = colors, onSelect = {
                        onSetVisualStyle(style)
                        onClose()
                    })
                }
            }
        } else {
            // 宽屏：水平三列
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VisualStyle.entries.forEach { style ->
                    VisualStyleCard(
                        style = style,
                        isSelected = uiState.layoutConfig.visualStyle == style,
                        colors = colors,
                        onSelect = {
                            onSetVisualStyle(style)
                            onClose()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // 当前样式预览说明
        val currentStyle = uiState.layoutConfig.visualStyle
        Surface(
            color = colors.cardHighlight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("📋 当前布局", fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(currentStyle.displayName, fontSize = 20.sp, color = colors.accentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(currentStyle.description, fontSize = 14.sp, color = colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                val preview = when (currentStyle) {
                    VisualStyle.TODAY_DETAIL -> "今日大卡片 ${if (isCompact) "↑" else "→"} 七日预报（明天起）"
                    VisualStyle.WEEK_OVERVIEW -> "当前天气中等 ${if (isCompact) "↑" else "→"} 七日预报（含今天）"
                    VisualStyle.MINIMAL_CLOCK -> "时钟超大 ${if (isCompact) "↑" else "→"} 天气信息少"
                }
                Text("布局结构：$preview", fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun VisualStyleCard(
    style: VisualStyle,
    isSelected: Boolean,
    colors: ThemeColors,
    onSelect: (VisualStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onSelect(style) },
        color = if (isSelected) colors.accentColor.copy(alpha = 0.2f) else colors.cardHighlight,
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, colors.accentColor) else null,
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 简单图示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (style) {
                            VisualStyle.TODAY_DETAIL -> Brush.verticalGradient(listOf(colors.accentColor.copy(alpha = 0.5f), colors.accentColor.copy(alpha = 0.2f)))
                            VisualStyle.WEEK_OVERVIEW -> Brush.verticalGradient(listOf(colors.textSecondary.copy(alpha = 0.4f), colors.textPrimary.copy(alpha = 0.2f)))
                            VisualStyle.MINIMAL_CLOCK -> Brush.verticalGradient(listOf(colors.textPrimary.copy(alpha = 0.2f), colors.textSecondary.copy(alpha = 0.4f)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (style) {
                        VisualStyle.TODAY_DETAIL -> "今日详情"
                        VisualStyle.WEEK_OVERVIEW -> "七日总览"
                        VisualStyle.MINIMAL_CLOCK -> "极简时钟"
                    },
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                style.displayName,
                fontSize = 15.sp,
                color = if (isSelected) colors.accentColor else colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                style.description,
                fontSize = 11.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================
// 共用组件
// ============================================================
@Composable private fun LocationButton(isLocating: Boolean, useMyLocation: Boolean, onClick: () -> Unit, colors: ThemeColors) {
    val bgColor by animateColorAsState(targetValue = if (useMyLocation) colors.accentColor.copy(alpha = 0.3f) else colors.cardHighlight, label = "locBtnBg")
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLocating) CircularProgressIndicator(Modifier.size(18.dp), color = colors.accentColor, strokeWidth = 2.dp)
            else Text(if (useMyLocation) "📍" else "🎯", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                if (isLocating) "定位中..." else if (useMyLocation) "我的位置" else "使用位置",
                fontSize = 13.sp,
                color = if (useMyLocation) colors.accentColor else colors.textPrimary,
                fontWeight = if (useMyLocation) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable private fun ThemeButton(colors: ThemeColors, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = colors.cardHighlight,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎨", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(colors.name, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable private fun WeatherDetailItem(emoji: String, label: String?, value: String, colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 26.sp)
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ForecastCard(item: DayForecastItem, isToday: Boolean, colors: ThemeColors, modifier: Modifier = Modifier) {
    val cardBg = if (isToday) colors.accentColor.copy(alpha = 0.18f) else colors.cardHighlight
    Card(
        modifier = modifier.wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                item.dayLabel,
                fontSize = if (isToday) 13.sp else 11.sp,
                color = if (isToday) colors.accentColor else colors.textSecondary,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Text(item.icon, fontSize = 24.sp, maxLines = 1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.tempMax, fontSize = 15.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.tempMin, fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.65f), maxLines = 1)
            }
            // 固定高度占位，确保有/无降水概率时高度一致
            Spacer(Modifier.height(16.dp))
            if (item.precipProb.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(16.dp)
                ) {
                    Text("💧", fontSize = 10.sp)
                    Text(item.precipProb, fontSize = 10.sp, color = Color(0xFF64B5F6))
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
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

// ============================================================
// 布局 A：今日详情
// 左侧：位置 + 日期 + 超大时钟
// 右侧：今日天气卡片 + 详情（湿度/风速/空气质量）
// 底部：七日预报（明天起，2行x3列网格）
// ============================================================
@Composable
private fun TodayDetailContent(
    uiState: MainUiState, colors: ThemeColors,
    clockSize: TextUnit, dateSize: TextUnit,
    temperatureSize: TextUnit, weatherIconSize: TextUnit,
    locationSize: TextUnit, spacing: Dp,
    screenWidth: Dp,
    showAirQualitySheet: Boolean,
    onAirQualityClick: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // ── 左：位置 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📍", fontSize = locationSize)
            Spacer(Modifier.width(6.dp))
            Text(uiState.locationName, fontSize = locationSize, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            if (uiState.locationSource.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Surface(color = colors.cardHighlight, shape = RoundedCornerShape(8.dp)) {
                    Text(uiState.locationSource, fontSize = 10.sp, color = colors.textSecondary.copy(alpha = 0.8f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 左侧：时钟日期  右侧：今日天气 ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 左侧：日期 + 时钟（大号）
            Column(modifier = Modifier.weight(1f)) {
                if (uiState.currentDate.isNotEmpty()) {
                    Text(uiState.currentDate, fontSize = dateSize, color = colors.textSecondary, lineHeight = dateSize * 1.1f)
                }
                Spacer(Modifier.height(spacing / 4))
                Text(uiState.currentTime, fontSize = clockSize, color = colors.textPrimary, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                if (uiState.timezoneDisplay.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.timezoneDisplay, fontSize = 12.sp, color = colors.textSecondary.copy(alpha = 0.6f))
                }
            }

            // 右侧：今日天气大卡片
            Surface(
                modifier = Modifier.weight(1f),
                color = colors.accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.weatherIcon, fontSize = weatherIconSize)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(uiState.temperature, fontSize = temperatureSize, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            Text(uiState.weatherDescription, fontSize = (temperatureSize.value * 0.28f).sp, color = colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeatherDetailItem(emoji = "💧", label = "湿度", value = uiState.humidity, colors = colors)
                        WeatherDetailItem(emoji = "💨", label = "风速", value = uiState.windSpeed, colors = colors)
                        if (uiState.airQuality != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAirQualityClick() }) {
                                Text("🫁", fontSize = 22.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("空气", fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.8f))
                                Text(uiState.airQuality!!.value, fontSize = 14.sp, color = Color(uiState.airQuality!!.color), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 七日预报（明天起，2行x3列网格）──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("七日预报", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("（从明天起）", fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(8.dp))

        val forecastItems = uiState.forecast.drop(1)
        // 动态列数：窄屏2列，中屏3列，宽屏3列（最多6天=2行）
        val gridCols = if (isCompact) 2 else 3
        // 不要take(6)，而是显示所有可用天数（最多gridCols*2个）
        val rows = forecastItems.chunked(gridCols)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEachIndexed { rowIdx, rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEachIndexed { colIdx, forecast ->
                        ForecastCard(
                            item = forecast, isToday = false, colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 填充空白格子以保持对齐
                    repeat(gridCols - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ============================================================
// 布局 B：七日总览
// 位置 + 日期 + 时钟 + 天气（横向排列，紧凑）
// 完整7天预报网格（含今天，7列）
// 详情行（湿度/风速/空气质量）
// ============================================================
@Composable
private fun WeekOverviewContent(
    uiState: MainUiState, colors: ThemeColors,
    clockSize: TextUnit, dateSize: TextUnit,
    temperatureSize: TextUnit, weatherIconSize: TextUnit,
    locationSize: TextUnit, spacing: Dp,
    screenWidth: Dp,
    showAirQualitySheet: Boolean,
    onAirQualityClick: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // ── 左：位置 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📍", fontSize = locationSize)
            Spacer(Modifier.width(6.dp))
            Text(uiState.locationName, fontSize = locationSize, color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(spacing))

        // ── 左侧：日期+时钟  中间：天气图标+温度  右侧：七天预报 ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 左：日期 + 时钟
            Column(modifier = Modifier.weight(1f)) {
                if (uiState.currentDate.isNotEmpty()) {
                    Text(uiState.currentDate, fontSize = dateSize, color = colors.textSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Text(uiState.currentTime, fontSize = clockSize, color = colors.textPrimary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }

            // 中：天气图标 + 温度（紧凑）
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(uiState.weatherIcon, fontSize = weatherIconSize)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(uiState.temperature, fontSize = temperatureSize, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text(uiState.weatherDescription, fontSize = (temperatureSize.value * 0.28f).sp, color = colors.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 详情行（湿度/风速/AQ）──
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            WeatherDetailItem(emoji = "💧", label = "湿度", value = uiState.humidity, colors = colors)
            WeatherDetailItem(emoji = "💨", label = "风速", value = uiState.windSpeed, colors = colors)
            if (uiState.airQuality != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAirQualityClick() }) {
                    Text("🫁", fontSize = 22.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("空气", fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.8f))
                    Text(uiState.airQuality!!.value, fontSize = 14.sp, color = Color(uiState.airQuality!!.color), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 七日预报（含今天，动态列数）──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("七日预报", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("（含今天）", fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(8.dp))

        val forecastItems = uiState.forecast
        // 动态列数：窄屏2-3列，宽屏3-4列，显示所有7天
        val numDays = forecastItems.size.coerceIn(1, 7)
        val gridCols = if (isCompact) minOf(numDays, 3) else minOf(numDays, 4)
        val rows = forecastItems.chunked(gridCols)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEachIndexed { idx, forecast ->
                        ForecastCard(
                            item = forecast,
                            isToday = forecast == forecastItems.first(),
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(gridCols - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ============================================================
// 布局 C：极简时钟
// 左侧：超大时钟占据左侧 2/3（日期+时间，视觉焦点）
// 右侧：天气信息列（温度+图标+体感+湿度/风速/AQ）
// 底部：七日预报（明天起，2行x3列）
// ============================================================
@Composable
private fun MinimalClockContent(
    uiState: MainUiState, colors: ThemeColors,
    clockSize: TextUnit, dateSize: TextUnit,
    temperatureSize: TextUnit, weatherIconSize: TextUnit,
    locationSize: TextUnit, spacing: Dp,
    screenWidth: Dp,
    showAirQualitySheet: Boolean,
    onAirQualityClick: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // ── 左：位置 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📍", fontSize = locationSize)
            Spacer(Modifier.width(6.dp))
            Text(uiState.locationName, fontSize = locationSize, color = colors.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            if (uiState.timezoneDisplay.isNotEmpty()) {
                Text(uiState.timezoneDisplay, fontSize = (locationSize.value * 0.75f).sp, color = colors.textSecondary.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 左侧：超大时钟（占 2/3）  右侧：天气信息 ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 左侧：超大时钟
            Column(modifier = Modifier.weight(2f)) {
                if (uiState.currentDate.isNotEmpty()) {
                    Text(uiState.currentDate, fontSize = dateSize, color = colors.textSecondary, lineHeight = dateSize * 1.1f)
                }
                Spacer(Modifier.height(spacing / 4))
                Text(uiState.currentTime, fontSize = clockSize, color = colors.textPrimary, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            }

            // 右侧：天气信息（温度+图标+体感+湿度/风速/AQ）
            Surface(
                modifier = Modifier.weight(1f),
                color = colors.cardHighlight,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.weatherIcon, fontSize = weatherIconSize)
                    Text(uiState.temperature, fontSize = temperatureSize, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    Text(uiState.weatherDescription, fontSize = (temperatureSize.value * 0.25f).sp, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(uiState.feelsLike, fontSize = 12.sp, color = colors.textSecondary.copy(alpha = 0.75f))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherDetailItem(emoji = "💧", label = null, value = uiState.humidity, colors = colors)
                        WeatherDetailItem(emoji = "💨", label = null, value = uiState.windSpeed, colors = colors)
                    }
                    if (uiState.airQuality != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onAirQualityClick() }) {
                            Text("🫁", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(uiState.airQuality!!.value, fontSize = 14.sp, color = Color(uiState.airQuality!!.color), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing))

        // ── 七日预报（明天起，动态列数）──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("七日预报", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("（从明天起）", fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(8.dp))

        val forecastItems = uiState.forecast.drop(1)
        // 动态列数：窄屏2列，中屏3列，宽屏3列（最多6天=2行）
        val gridCols = if (isCompact) 2 else 3
        val rows = forecastItems.chunked(gridCols)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { forecast ->
                        ForecastCard(item = forecast, isToday = false, colors = colors, modifier = Modifier.weight(1f))
                    }
                    repeat(gridCols - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ============================================================
private data class RainParticle(
    val x: Float, val y: Float, val speed: Float, val size: Float,
    val drift: Float = 0f, val opacity: Float = 1f,
    val isRain: Boolean = false, val isSnow: Boolean = false,
    val isLightning: Boolean = false, val isFog: Boolean = false,
    val angle: Float = 0f, val rotSpeed: Float = 0f,
    val pulse: Float = 0f, val pulseSpeed: Float = 0f,
)
