package com.desk.weather.data

import android.content.Context
import android.content.SharedPreferences

object LocationStorage {
    private const val PREFS_NAME = "weather_clock_prefs"
    private const val KEY_SELECTED_INDEX = "selected_location_index"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_SELECTED_INDEX, index).apply()
    }

    fun loadSelectedIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_SELECTED_INDEX, 0)
    }
}