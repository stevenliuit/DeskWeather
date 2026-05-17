package com.example.weatherclock.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationProvider {

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
        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val result = location?.let {
                        LocationData(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy,
                            source = getAccuracySource(it)
                        )
                    }
                    continuation.resume(result)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData? {
        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                1000L
            ).apply {
                setMinUpdateIntervalMillis(500L)
                setMaxUpdates(1)
                setWaitForAccurateLocation(false)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(res: GmsLocationResult) {
                    fusedClient.removeLocationUpdates(this)
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

            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

            continuation.invokeOnCancellation {
                fusedClient.removeLocationUpdates(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocation(context: Context): Flow<LocationData> = callbackFlow {
        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setWaitForAccurateLocation(true)
        }.build()

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

        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
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