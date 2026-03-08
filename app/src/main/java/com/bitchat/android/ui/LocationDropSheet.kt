package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject

private val SheetBg = Color(0xFF1A1C20)
private val CardBg = Color(0xFF242628)
private val BorderColor = Color(0xFF2A2C30)
private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)

private val LANDMARKS = listOf(
    "Main entrance", "Stairwell", "Lift", "Loading bay", "Roof", "Basement"
)

@Composable
fun LocationDropSheet(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE) }

    var floor by remember { mutableIntStateOf(prefs.getInt("currentFloorNumber", 0)) }
    var zone by remember { mutableStateOf("") }
    var selectedLandmark by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var lat by remember { mutableDoubleStateOf(0.0) }
    var lon by remember { mutableDoubleStateOf(0.0) }
    var hasGps by remember { mutableStateOf(false) }

    // Fetch GPS
    LaunchedEffect(Unit) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(30_000)
                    .build()
                client.getCurrentLocation(request, null).addOnSuccessListener { location ->
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                        hasGps = true
                    }
                }
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(SheetBg)
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = GreenAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Location",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Floor stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Floor", color = SecondaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (floor > -3) floor-- },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AmberAccent, contentColor = Color.Black
                        ),
                        modifier = Modifier.size(32.dp),
                        enabled = floor > -3
                    ) {
                        Text("\u2212", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (floor == 0) "Ground" else "Floor $floor",
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledIconButton(
                        onClick = { if (floor < 50) floor++ },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AmberAccent, contentColor = Color.Black
                        ),
                        modifier = Modifier.size(32.dp),
                        enabled = floor < 50
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Zone / Area
            Text("Zone / Area", color = SecondaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (zone.isEmpty()) {
                    Text("e.g. East wing, near lift", color = SecondaryText.copy(alpha = 0.5f), fontSize = 14.sp)
                }
                BasicTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(AmberAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Landmark quick picks
            Text("Landmark", color = SecondaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LANDMARKS.forEach { landmark ->
                    val isSelected = selectedLandmark == landmark
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberAccent.copy(alpha = 0.15f) else CardBg)
                            .border(
                                1.dp,
                                if (isSelected) AmberAccent else BorderColor,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedLandmark = if (isSelected) null else landmark
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = landmark,
                            color = if (isSelected) AmberAccent else PrimaryText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            Text("Note (optional)", color = SecondaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (note.isEmpty()) {
                    Text("Additional details...", color = SecondaryText.copy(alpha = 0.5f), fontSize = 14.sp)
                }
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(AmberAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // GPS indicator
            if (hasGps) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = GreenAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GPS: %.5f, %.5f".format(lat, lon),
                        color = Color(0xFF5A5E66),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Send button
            Button(
                onClick = {
                    val json = JSONObject().apply {
                        put("floor", floor)
                        if (zone.isNotBlank()) put("zone", zone.trim())
                        if (selectedLandmark != null) put("landmark", selectedLandmark)
                        if (note.isNotBlank()) put("note", note.trim())
                        if (hasGps) {
                            put("lat", lat)
                            put("lon", lon)
                        }
                    }
                    onSend("[LOCATION_DROP:${json}]")
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Share Location",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
