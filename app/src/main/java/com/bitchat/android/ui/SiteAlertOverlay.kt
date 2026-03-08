package com.bitchat.android.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import com.bitchat.android.MainActivity
import com.bitchat.android.R
import kotlinx.coroutines.delay

private const val ALERT_CHANNEL_ID = "site_alert_notifications"

@Composable
fun SiteAlertOverlay(
    alert: SiteAlert,
    onDismiss: () -> Unit,
    onOpenProtocol: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    // Play sound and vibrate on first composition
    DisposableEffect(alert) {
        val isGeneral = alert.type == SiteAlertType.GENERAL
        val mediaPlayer = try {
            val soundUri = if (isGeneral) {
                // General: standard notification sound, not critical alarm
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            MediaPlayer.create(context, soundUri)?.apply { start() }
        } catch (_: Exception) {
            null
        }

        if (isGeneral) {
            triggerLightVibration(context)
        } else {
            triggerAlertVibration(context)
        }

        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) { }
        }
    }

    // Auto-dismiss ALL_CLEAR after 3 seconds, GENERAL after 5 seconds
    if (alert.type == SiteAlertType.ALL_CLEAR) {
        LaunchedEffect(Unit) {
            delay(3000)
            onDismiss()
        }
    } else if (alert.type == SiteAlertType.GENERAL) {
        LaunchedEffect(Unit) {
            delay(5000)
            onDismiss()
        }
    }

    // Pulse animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Parse pipe-delimited detail fields: zone | GPS:lat,lon | detail text
    val parsedDetail = remember(alert.detail) {
        parseAlertDetail(alert.detail)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(alert.type.color.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Pulsing icon
                Icon(
                    imageVector = alert.type.icon,
                    contentDescription = alert.type.label,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Alert type name
                Text(
                    text = when (alert.type) {
                        SiteAlertType.FIRE -> "FIRE EVACUATION"
                        SiteAlertType.ALL_CLEAR -> "ALL CLEAR"
                        else -> alert.type.label.uppercase()
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 32.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Floor + zone location
                val locationLine = buildString {
                    append(formatFloorDisplay(alert.floor))
                    if (parsedDetail.zone.isNotBlank()) {
                        append(" \u2014 ")
                        append(parsedDetail.zone)
                    }
                }
                Text(
                    text = locationLine,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // GPS coordinates
                if (parsedDetail.gps.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parsedDetail.gps,
                        color = Color(0xFF5A5E66),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Detail text
                if (parsedDetail.detailText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = parsedDetail.detailText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Sender
                if (alert.senderName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sent by ${alert.senderName}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // First Aid Protocol button (only for scenario-linked types)
                if (alert.type.scenarioId != null) {
                    Button(
                        onClick = {
                            onOpenProtocol?.invoke(alert.type.scenarioId!!)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.20f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Open First Aid Protocol",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Dismiss button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = BorderStroke(2.dp, Color.White),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "DISMISS",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private data class ParsedAlertDetail(
    val zone: String = "",
    val gps: String = "",
    val detailText: String = ""
)

private fun parseAlertDetail(detail: String): ParsedAlertDetail {
    if (detail.isBlank()) return ParsedAlertDetail()

    val parts = detail.split("|").map { it.trim() }
    var zone = ""
    var gps = ""
    val otherParts = mutableListOf<String>()

    for (part in parts) {
        when {
            part.startsWith("GPS:") -> gps = part
            zone.isEmpty() && !part.startsWith("GPS:") -> zone = part
            else -> otherParts.add(part)
        }
    }

    return ParsedAlertDetail(
        zone = zone,
        gps = gps,
        detailText = otherParts.joinToString(" | ")
    )
}

private fun triggerLightVibration(context: Context) {
    val pattern = longArrayOf(0, 200, 100, 200)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    } catch (_: Exception) { }
}

private fun triggerAlertVibration(context: Context) {
    val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    } catch (_: Exception) { }
}

private const val GENERAL_ALERT_CHANNEL_ID = "general_alert_notifications"

fun showSiteAlertNotification(context: Context, alert: SiteAlert) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val isGeneral = alert.type == SiteAlertType.GENERAL

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (isGeneral) {
            val channel = NotificationChannel(
                GENERAL_ALERT_CHANNEL_ID,
                "General Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General site announcements"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }
            notificationManager.createNotificationChannel(channel)
        } else {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Site Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency site alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = when (alert.type) {
        SiteAlertType.FIRE -> "FIRE EVACUATION"
        SiteAlertType.ALL_CLEAR -> "ALL CLEAR"
        SiteAlertType.GENERAL -> "GENERAL ANNOUNCEMENT"
        else -> alert.type.label.uppercase()
    }

    val body = buildString {
        append(formatFloorDisplay(alert.floor))
        if (alert.detail.isNotBlank()) {
            append(" — ")
            append(alert.detail)
        }
        if (alert.senderName.isNotBlank()) {
            append(" (from ")
            append(alert.senderName)
            append(")")
        }
    }

    val channelId = if (isGeneral) GENERAL_ALERT_CHANNEL_ID else ALERT_CHANNEL_ID
    val soundUri = if (isGeneral) {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
    val vibrationPattern = if (isGeneral) {
        longArrayOf(0, 200, 100, 200)
    } else {
        longArrayOf(0, 500, 300, 500, 300, 500)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setSound(soundUri)
        .setVibrate(vibrationPattern)

    if (isGeneral) {
        // General: standard priority, no DND bypass, no full-screen intent
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
    } else {
        // Emergency: high priority, full-screen intent, alarm category
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
    }

    try {
        notificationManager.notify(900, builder.build())
    } catch (_: SecurityException) { }
}
