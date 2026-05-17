package com.desk.weather.util

import android.app.Activity
import android.content.Context
import android.graphics.Insets
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.DisplayCutout
import android.view.WindowManager
import androidx.core.content.getSystemService

/**
 * 圆形屏幕检测与适配工具
 * 支持 Android 6.0 (API 23) 至最新版本
 */
object RoundDisplayHelper {

    private fun min(a: Int, b: Int) = if (a < b) a else b
    private fun max(a: Int, b: Int) = if (a > b) a else b
    private fun min(a: Float, b: Float) = if (a < b) a else b
    private fun max(a: Float, b: Float) = if (a > b) a else b

    /**
     * 检测当前设备屏幕是否为圆形
     */
    fun isRoundDisplay(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val display = getDisplay(context) ?: return false

        return try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                // API 24-32: 使用 Display.isRound()（已废弃但仍可用）
                val method = Display::class.java.getMethod("isRound")
                @Suppress("DEPRECATION")
                method.invoke(display) as Boolean
            } else {
                // API 33+: isRound 已废弃，通过屏幕宽高比近似判断
                val size = getRealDisplaySize(display)
                if (size.x > 0 && size.y > 0) {
                    val ratio = size.x.toFloat().coerceAtLeast(size.y.toFloat()) / size.x.toFloat().coerceAtMost(size.y.toFloat())
                    // 宽高比接近 1:1 (误差 15% 以内) 认为是圆形/方形屏幕
                    ratio < 1.18f
                } else false
            }
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getDisplay(context: Context): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val windowManager = context.getSystemService<WindowManager>()
            windowManager?.defaultDisplay
        }
    }

    @Suppress("DEPRECATION")
    private fun getRealDisplaySize(display: Display): Point {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val size = Point()
                display.getRealSize(size)
                size
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val size = Point()
                display.getSize(size)
                size
            } else {
                Point(display.width, display.height)
            }
        } catch (_: Throwable) {
            Point(0, 0)
        }
    }

    /**
     * 获取安全内容区域边距（排除刘海/挖孔/系统栏）
     * 返回以 px 为单位的 Rect (left, top, right, bottom)
     */
    fun getSafeInsets(activity: Activity): Rect {
        val decorView = activity.window.decorView

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: 使用 WindowInsets API
                val windowInsets = decorView.rootWindowInsets ?: return Rect(0, 0, 0, 0)
                val displayCutout = windowInsets.displayCutout ?: return Rect(0, 0, 0, 0)
                getSafeInsetsFromCutout(displayCutout)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28-29: 使用 DisplayCutout.getSafeInsetInsets()
                val windowInsets = decorView.rootWindowInsets
                if (windowInsets != null) {
                    val displayCutout = windowInsets.displayCutout
                    if (displayCutout != null) {
                        return getSafeInsetsFromCutout(displayCutout)
                    }
                }
                Rect(0, 0, 0, 0)
            } else {
                // API 23-27: 无刘海支持
                Rect(0, 0, 0, 0)
            }
        } catch (_: Throwable) {
            Rect(0, 0, 0, 0)
        }
    }

    /**
     * 通过反射调用 DisplayCutout.getSafeInsetInsets() 获取安全边距
     * 解决 Kotlin 编译器在 API 28-30 对 safeInsetInsets 的引用问题
     */
    private fun getSafeInsetsFromCutout(cutout: DisplayCutout): Rect {
        return try {
            val method = DisplayCutout::class.java.getMethod("getSafeInsetInsets")
            val insets = method.invoke(cutout) as Insets
            Rect(insets.left, insets.top, insets.right, insets.bottom)
        } catch (_: Throwable) {
            Rect(0, 0, 0, 0)
        }
    }

    /**
     * 计算适合圆形屏幕的内容最大直径（px）
     * 取屏幕宽高中的较小值作为直径
     */
    @Suppress("DEPRECATION")
    fun getRoundDiameterPx(context: Context): Int {
        val display = getDisplay(context) ?: return 0
        val size = getRealDisplaySize(display)
        return min(size.x, size.y)
    }
}