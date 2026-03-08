package com.bitchat.android.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.bitchat.android.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class PinGeofenceManager(private val context: Context) {

    companion object {
        private const val TAG = "PinGeofenceManager"
        const val ACTION_GEOFENCE_EVENT = "com.sitetalkie.GEOFENCE_EVENT"
        const val EXTRA_PIN_ID = "pin_id"
        const val EXTRA_PIN_TITLE = "pin_title"
        const val EXTRA_PIN_TYPE = "pin_type"
        const val GEOFENCE_CHANNEL_ID = "sitetalkie_pin_geofence"
    }

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    init {
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    fun registerGeofence(pin: SitePin) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Missing location permission, cannot register geofence")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(pin.id)
            .setCircularRegion(pin.latitude, pin.longitude, pin.radius.toFloat())
            .setExpirationDuration(
                if (pin.expiresAt != null) {
                    (pin.expiresAt - System.currentTimeMillis()).coerceAtLeast(1000L)
                } else {
                    Geofence.NEVER_EXPIRE
                }
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
            putExtra(EXTRA_PIN_ID, pin.id)
            putExtra(EXTRA_PIN_TITLE, pin.title)
            putExtra(EXTRA_PIN_TYPE, pin.type.protocolName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pin.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Geofence registered: ${pin.title}") }
            .addOnFailureListener { Log.e(TAG, "Geofence failed for ${pin.title}: ${it.message}") }
    }

    fun removeGeofence(pinId: String) {
        geofencingClient.removeGeofences(listOf(pinId))
            .addOnSuccessListener { Log.d(TAG, "Geofence removed: $pinId") }
            .addOnFailureListener { Log.e(TAG, "Geofence remove failed: ${it.message}") }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                "Site Pins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when you enter a pinned area"
                enableVibration(true)
                setShowBadge(true)
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.createNotificationChannel(channel)
        }
    }
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofencing event error: ${geofencingEvent?.errorCode}")
            return
        }

        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val pinTitle = intent.getStringExtra(PinGeofenceManager.EXTRA_PIN_TITLE) ?: "Pin"
        val pinTypeName = intent.getStringExtra(PinGeofenceManager.EXTRA_PIN_TYPE) ?: ""
        val pinType = SitePinType.fromProtocolName(pinTypeName)

        // Show notification
        showPinNotification(context, pinTitle, pinType)

        // Trigger haptic
        triggerHaptic(context, pinType)
    }

    private fun showPinNotification(context: Context, title: String, pinType: SitePinType?) {
        val notifTitle = when (pinType) {
            SitePinType.HAZARD -> "\u26a0\ufe0f Hazard Nearby"
            SitePinType.NOTE -> "Note Nearby"
            null -> "Pin Nearby"
        }

        val importance = when (pinType) {
            SitePinType.HAZARD -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, PinGeofenceManager.GEOFENCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notifTitle)
            .setContentText("$title \u2014 Tap to view")
            .setPriority(importance)
            .setAutoCancel(true)
            .build()

        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.notify(title.hashCode(), notification)
    }

    private fun triggerHaptic(context: Context, pinType: SitePinType?) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        val effect = when (pinType) {
            SitePinType.HAZARD -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            else -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
