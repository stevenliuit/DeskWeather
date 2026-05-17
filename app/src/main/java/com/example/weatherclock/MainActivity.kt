package com.example.weatherclock

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherclock.ui.main.MainScreenViewModel
import com.example.weatherclock.ui.main.MainScreen
import com.example.weatherclock.ui.theme.WeatherClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape for TV/tablet/smart display
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Keep screen on for smart display / TV use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // enableEdgeToEdge() is API 35+. Guard for older devices.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } else {
            // Fallback for API 23-34: use window flags instead
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        setContent {
            val viewModel: MainScreenViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            WeatherClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = uiState.themeColors.surfaceColor
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onLocationChange = { }
                    )
                }
            }
        }
    }
}