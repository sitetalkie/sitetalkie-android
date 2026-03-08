package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.google.android.gms.location.LocationServices

private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val DarkText = Color(0xFF5A5E66)
private val CardBg = Color(0xFF1A1C20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDetailSheet(
    pin: SitePin,
    myName: String,
    onDismiss: () -> Unit,
    onResolve: () -> Unit,
    onDelete: () -> Unit,
    onExtend: (Long) -> Unit
) {
    val context = LocalContext.current
    var distanceText by remember { mutableStateOf<String?>(null) }

    // Get distance to pin
    LaunchedEffect(pin) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val pinLocation = Location("pin").apply {
                        latitude = pin.latitude
                        longitude = pin.longitude
                    }
                    val distance = location.distanceTo(pinLocation)
                    distanceText = if (distance < 1000) {
                        "~${distance.toInt()}m away"
                    } else {
                        "~${"%.1f".format(distance / 1000)}km away"
                    }
                }
            }
        }
    }

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Pin type badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = pin.type.color.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = pin.type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = pin.type.color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pin.type.label,
                        color = pin.type.color,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    if (pin.isResolved) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resolved",
                            color = Color(0xFF34C759),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = pin.title,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            // Description
            if (pin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pin.description,
                    color = SecondaryText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo
            if (pin.photoUri != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = CardBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Photo,
                            contentDescription = "Photo",
                            tint = SecondaryText,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Detail rows
            DetailRow(icon = Icons.Filled.Layers, label = "Floor", value = formatFloorInt(pin.floor))

            distanceText?.let {
                DetailRow(icon = Icons.Filled.NearMe, label = "Distance", value = it)
            }

            DetailRow(
                icon = Icons.Filled.Person,
                label = "Created by",
                value = pin.createdBy
            )

            DetailRow(
                icon = Icons.Filled.Schedule,
                label = "Created",
                value = formatTimeAgo(pin.createdAt)
            )

            if (pin.expiresAt != null) {
                val expiryText = if (pin.expiresAt > System.currentTimeMillis()) {
                    "In ${formatTimeRemaining(pin.expiresAt)}"
                } else {
                    "Expired"
                }
                DetailRow(icon = Icons.Filled.Timer, label = "Expires", value = expiryText)
            } else {
                DetailRow(icon = Icons.Filled.Timer, label = "Expires", value = "No expiry")
            }

            DetailRow(
                icon = Icons.Filled.MyLocation,
                label = "Precision",
                value = "${pin.radius.toInt()}m radius"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Extend button
            if (pin.expiresAt != null && pin.expiresAt > System.currentTimeMillis()) {
                OutlinedButton(
                    onClick = { onExtend(24) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Update, null, tint = Color(0xFFE8960C), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extend 24 hours", color = Color(0xFFE8960C), fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Delete button (only for own pins)
            if (pin.createdBy == myName) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Pin", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", fontFamily = FontFamily.Monospace, color = SecondaryText)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = DarkText
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = DarkText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = SecondaryText,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

private fun formatFloorInt(floor: Int): String = when {
    floor < 0 -> "Basement ${-floor}"
    floor == 0 -> "Ground Floor"
    else -> "Floor $floor"
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

private fun formatTimeRemaining(expiresAt: Long): String {
    val diff = expiresAt - System.currentTimeMillis()
    if (diff <= 0) return "expired"
    val hours = diff / 3600_000
    val days = hours / 24
    return when {
        hours < 1 -> "${diff / 60_000}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d ${hours % 24}h"
    }
}
