package com.spendora.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class TransactionLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null
)

/**
 * TransactionLocationManager
 *
 * Privacy Invariants:
 * - One-shot location fix strictly requested on explicit user command ("Add Location").
 * - Never collects location continuously, never runs in the background.
 * - Coordinates are bounded to 5 decimal places (~1.1m precision) for data minimization.
 * - Failure to acquire a fix never prevents or aborts transaction creation.
 */
class TransactionLocationManager(
    private val context: Context
) {
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getOneShotLocation(timeoutMillis: Long = 5000L): TransactionLocation? {
        if (!hasLocationPermission()) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1. Try last known location first for instantaneous responsiveness
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null && (System.currentTimeMillis() - loc.time) < 120_000L) { // Fresh within 2 mins
                    return formatLocation(loc)
                }
            }
        }

        // 2. Request a single, one-shot update with timeout
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val bestProvider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (bestProvider == null) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(formatLocation(location))
                        }
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(null)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }

                try {
                    locationManager.requestSingleUpdate(bestProvider, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    locationManager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private fun formatLocation(location: Location): TransactionLocation {
        // Data minimization: round to 5 decimal places
        val lat = Math.round(location.latitude * 100000.0) / 100000.0
        val lon = Math.round(location.longitude * 100000.0) / 100000.0
        return TransactionLocation(
            latitude = lat,
            longitude = lon,
            label = null
        )
    }
}
