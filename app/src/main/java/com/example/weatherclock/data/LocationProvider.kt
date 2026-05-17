package com.example.weatherclock.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationProvider {

    private const val TAG = "LocationProvider"

    // 标记 Play Services 是否可用（Android 8.1 无 Play Services 时会报 ClassNotFoundException）
    private var playServicesAvailable: Boolean? = null

    private fun isPlayServicesAvailable(): Boolean {
        playServicesAvailable?.let { return it }
        return try {
            // 尝试调用一次 LocationServices 关键方法，触发 ClassNotFoundException
            Class.forName("com.google.android.gms.location.LocationServices")
            playServicesAvailable = true
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Google Play Services not available on this device", e)
            playServicesAvailable = false
            false
        }
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val source: String
    )

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): LocationData? {
        if (!isPlayServicesAvailable()) {
            Log.w(TAG, "Play Services unavailable, skipping location")
            return null
        }

        val fusedClient: FusedLocationProviderClient? = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get fused location client", e)
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                    val result = location?.let {
                        LocationData(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy,
                            source = getAccuracySource(it)
                        )
                    }
                    continuation.resume(result)
                }?.addOnFailureListener {
                    continuation.resume(null)
                } ?: continuation.resume(null)
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting last known location", e)
                continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData? {
        if (!isPlayServicesAvailable()) {
            Log.w(TAG, "Play Services unavailable, skipping location")
            return null
        }

        val fusedClient: FusedLocationProviderClient? = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get fused location client", e)
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            // API 31+ 使用 LocationRequest.Builder，API 23-30 使用旧版 LocationRequest
            val locationRequest: LocationRequest = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        1000L
                    ).apply {
                        setMinUpdateIntervalMillis(500L)
                        setMaxUpdates(1)
                        setWaitForAccurateLocation(false)
                    }.build()
                } else {
                    // API 23-30: 使用旧版 LocationRequest 构造函数
                    @Suppress("DEPRECATION")
                    LocationRequest().apply {
                        priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        interval = 1000L
                        fastestInterval = 500L
                        numUpdates = 1
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build location request", e)
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val callback = object : LocationCallback() {
                override fun onLocationResult(res: GmsLocationResult) {
                    try {
                        fusedClient?.removeLocationUpdates(this)
                    } catch (_: Exception) {}
                    res.lastLocation?.let { loc ->
                        continuation.resume(
                            LocationData(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                source = getAccuracySource(loc)
                            )
                        )
                    } ?: continuation.resume(null)
                }
            }

            try {
                fusedClient?.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                // Hard timeout: if no location within 20 seconds, give up
                val timeoutJob = GlobalScope.launch {
                    kotlinx.coroutines.delay(20_000L)
                    if (continuation.isActive) {
                        try {
                            fusedClient?.removeLocationUpdates(callback)
                        } catch (_: Exception) {}
                        continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation {
                    timeoutJob.cancel()
                    try {
                        fusedClient?.removeLocationUpdates(callback)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting location updates", e)
                continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocation(context: Context): Flow<LocationData> = callbackFlow {
        if (!isPlayServicesAvailable()) {
            close()
            return@callbackFlow
        }

        val fusedClient: FusedLocationProviderClient? = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val locationRequest: LocationRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                10000L
            ).apply {
                setMinUpdateIntervalMillis(5000L)
                setWaitForAccurateLocation(true)
            }.build()
        } else {
            @Suppress("DEPRECATION")
            LocationRequest().apply {
                priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                interval = 10000L
                fastestInterval = 5000L
            }
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(res: GmsLocationResult) {
                res.lastLocation?.let { loc ->
                    trySend(LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        source = getAccuracySource(loc)
                    ))
                }
            }
        }

        try {
            fusedClient?.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                fusedClient?.removeLocationUpdates(callback)
            } catch (_: Exception) {}
        }
    }

    private fun getAccuracySource(location: Location): String {
        return when {
            location.accuracy <= 10 -> "📡 GPS"
            location.accuracy <= 50 -> "📶 WiFi定位"
            location.accuracy <= 200 -> "📱 基站定位"
            else -> "🌍 网络定位"
        }
    }

    fun formatAccuracy(accuracyMeters: Float): String {
        return when {
            accuracyMeters <= 10 -> "高精度"
            accuracyMeters <= 50 -> "中等精度"
            accuracyMeters <= 200 -> "低精度"
            else -> "粗略位置"
        }
    }
}