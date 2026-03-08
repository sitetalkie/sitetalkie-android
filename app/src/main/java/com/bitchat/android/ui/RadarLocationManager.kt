package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides GPS location via FusedLocationProviderClient.
 * Publishes current location as a StateFlow<Location?>.
 * Requests updates when radar is visible, stops when not.
 * Handles permission denied gracefully — location stays null.
 */
class RadarLocationManager(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 5_000L
    ).setMinUpdateIntervalMillis(2_000L).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { _location.value = it }
        }
    }

    private var active = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    fun start() {
        if (active) return
        if (!hasPermission()) return
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            active = true
            // Seed with last known location
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _location.value == null) _location.value = loc
            }
        } catch (_: SecurityException) { /* permission revoked at runtime */ }
    }

    fun stop() {
        if (!active) return
        fusedClient.removeLocationUpdates(locationCallback)
        active = false
    }
}
