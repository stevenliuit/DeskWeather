package com.example.weatherclock

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherclock.ui.main.MainScreenViewModel
import com.example.weatherclock.ui.main.MainScreen
import com.example.weatherclock.ui.theme.WeatherClockTheme
import com.example.weatherclock.util.RoundDisplayHelper
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private val configUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.weatherclock.CONFIG_UPDATE") {
                recreate()
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("com.example.weatherclock.CONFIG_UPDATE")
        registerReceiver(configUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Force landscape for TV/tablet/smart display
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        setContent {
            val viewModel: MainScreenViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            var isRound by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isRound = RoundDisplayHelper.isRoundDisplay(context)
            }

            WeatherClockTheme(themeColors = uiState.themeColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = uiState.themeColors.surfaceColor
                ) {
                    if (isRound) {
                        // 圆形屏幕：将内容限制在最大圆形区域内
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val density = LocalDensity.current
                            val safeInsets = remember { RoundDisplayHelper.getSafeInsets(this@MainActivity) }
                            val safePaddingH = with(density) { (safeInsets.left + safeInsets.right).toDp() }
                            val safePaddingV = with(density) { (safeInsets.top + safeInsets.bottom).toDp() }

                            // 最大可用正方形尺寸（考虑安全区域）
                            val availableW = maxWidth - safePaddingH
                            val availableH = maxHeight - safePaddingV
                            val roundDiameter = min(availableW.value, availableH.value).dp

                            // 用 clip(CircleShape) 裁剪为圆形
                            Box(
                                modifier = Modifier
                                    .size(roundDiameter)
                                    .clip(CircleShape)
                                    .background(uiState.themeColors.surfaceColor),
                                contentAlignment = Alignment.Center
                            ) {
                                MainScreen(
                                    viewModel = viewModel,
                                    onLocationChange = { },
                                    isRoundDisplay = true,
                                    roundDiameter = roundDiameter
                                )
                            }
                        }
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onLocationChange = { },
                            isRoundDisplay = false,
                            roundDiameter = null
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(configUpdateReceiver)
    }
}