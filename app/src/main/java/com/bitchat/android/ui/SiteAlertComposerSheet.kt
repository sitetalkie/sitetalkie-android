package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

private const val PREFS_NAME = "bitchat_prefs"
private const val KEY_FLOOR = "currentFloorNumber"
private const val FLOOR_MIN = -3
private const val FLOOR_MAX = 50

private val AmberAccent = Color(0xFFE8960C)
private val RedAccent = Color(0xFFE5484D)
private val CardBg = Color(0xFF1A1C20)
private val ElevatedBg = Color(0xFF242628)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)

// Card order as specified — most likely emergencies first
private val ALERT_CARD_ORDER = listOf(
    SiteAlertType.CARDIAC,
    SiteAlertType.FALL,
    SiteAlertType.BLEEDING,
    SiteAlertType.BURNS,
    SiteAlertType.CHEMICAL,
    SiteAlertType.ELECTRICAL,
    SiteAlertType.CRUSH,
    SiteAlertType.BREATHING,
    SiteAlertType.CONFINED,
    SiteAlertType.HEAT,
    SiteAlertType.LONE_WORKER,
    SiteAlertType.FIRE,
    SiteAlertType.CRANE,
    SiteAlertType.WARNING
)

private val LANDMARK_PICKS = listOf(
    "Main entrance", "Stairwell", "Lift", "Loading bay",
    "Roof", "Basement", "Car park", "Canopy"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteAlertComposerSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    onSendAlert: (String) -> Unit,
    onOpenProtocol: ((String) -> Unit)? = null
) {
    if (!isPresented) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var selectedType by remember { mutableStateOf<SiteAlertType?>(null) }
    var currentFloor by remember { mutableIntStateOf(prefs.getInt(KEY_FLOOR, 1)) }
    var zoneText by remember { mutableStateOf("") }
    var detailText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // GPS state
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }
    var gpsLocating by remember { mutableStateOf(true) }

    // Fetch GPS on sheet open
    LaunchedEffect(Unit) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(15000)
                    .build()
                fusedClient.getCurrentLocation(request, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            gpsLat = location.latitude
                            gpsLng = location.longitude
                            gpsLocating = false
                        }
                    }
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (gpsLat == null && location != null) {
                        gpsLat = location.latitude
                        gpsLng = location.longitude
                        gpsLocating = false
                    }
                }
                // Timeout after 10s
                delay(10000)
                if (gpsLat == null) gpsLocating = false
            } else {
                gpsLocating = false
            }
        } catch (_: Exception) {
            gpsLocating = false
        }
    }

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Header: X button + title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = ElevatedBg,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "EMERGENCY TYPE",
                        color = RedAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2-column grid of alert type cards
            val rows = ALERT_CARD_ORDER.chunked(2)
            items(rows.size) { rowIndex ->
                val row = rows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { type ->
                        SOSAlertCard(
                            type = type,
                            isSelected = selectedType == type,
                            onClick = { selectedType = type },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Pad if odd number in last row
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Floor selector
                Text(
                    text = "Your floor",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (currentFloor > FLOOR_MIN) {
                                currentFloor--
                                prefs.edit().putInt(KEY_FLOOR, currentFloor).apply()
                            }
                        },
                        enabled = currentFloor > FLOOR_MIN
                    ) {
                        Icon(Icons.Filled.Remove, "Decrease floor", tint = AmberAccent)
                    }

                    val floorDisplay = when {
                        currentFloor < 0 -> "B${-currentFloor}"
                        currentFloor == 0 -> "G"
                        else -> "F$currentFloor"
                    }
                    Text(
                        text = floorDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = PrimaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 60.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentFloor < FLOOR_MAX) {
                                currentFloor++
                                prefs.edit().putInt(KEY_FLOOR, currentFloor).apply()
                            }
                        },
                        enabled = currentFloor < FLOOR_MAX
                    ) {
                        Icon(Icons.Filled.Add, "Increase floor", tint = AmberAccent)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // YOUR LOCATION section
                Text(
                    text = "YOUR LOCATION",
                    color = TertiaryText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Zone text field
                OutlinedTextField(
                    value = zoneText,
                    onValueChange = { zoneText = it },
                    placeholder = {
                        Text(
                            "e.g. East wing, near lift",
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryText
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        unfocusedBorderColor = SecondaryText.copy(alpha = 0.3f),
                        cursorColor = AmberAccent,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Landmark quick-picks
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LANDMARK_PICKS.forEach { landmark ->
                        Surface(
                            onClick = { zoneText = landmark },
                            shape = RoundedCornerShape(18.dp),
                            color = ElevatedBg,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = landmark,
                                    color = SecondaryText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS display
                val gpsDisplay = if (gpsLat != null && gpsLng != null) {
                    "GPS: ${"%.4f".format(gpsLat)}, ${"%.4f".format(gpsLng)}"
                } else if (gpsLocating) {
                    "GPS: Locating..."
                } else {
                    "GPS: Unavailable"
                }
                Text(
                    text = gpsDisplay,
                    color = TertiaryText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail text field
                OutlinedTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    placeholder = {
                        Text(
                            "Add details (optional)...",
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryText
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        unfocusedBorderColor = SecondaryText.copy(alpha = 0.3f),
                        cursorColor = AmberAccent,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                // SEND SOS button
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedType != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedAccent,
                        disabledContainerColor = SecondaryText.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SEND SOS",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", fontFamily = FontFamily.Monospace, color = SecondaryText)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedType != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Send ${selectedType!!.label}?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = "This cannot be undone. All nearby devices will be alerted.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        val type = selectedType!!

                        // Build detail string with pipe-delimited fields
                        val detailParts = mutableListOf<String>()
                        if (zoneText.isNotBlank()) detailParts.add(zoneText.trim())
                        if (gpsLat != null && gpsLng != null) {
                            detailParts.add("GPS:${"%.4f".format(gpsLat)},${"%.4f".format(gpsLng)}")
                        }
                        if (detailText.isNotBlank()) detailParts.add(detailText.trim())
                        val combinedDetail = detailParts.joinToString(" | ")

                        val message = formatSiteAlert(type, currentFloor, combinedDetail)
                        onSendAlert(message)

                        onDismiss()

                        // Sender auto-open: navigate to matching protocol
                        if (type.scenarioId != null) {
                            onOpenProtocol?.invoke(type.scenarioId!!)
                        }
                    }
                ) {
                    Text(
                        text = "Send",
                        color = RedAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = SecondaryText, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = CardBg,
            tonalElevation = 8.dp
        )
    }
}

@Composable
private fun SOSAlertCard(
    type: SiteAlertType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AmberAccent else Color.Transparent
    val bgColor = CardBg

    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() }
            .drawBehind {
                // Coloured left border 3dp
                drawRect(
                    color = type.color,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                modifier = Modifier.size(24.dp),
                tint = type.color
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = type.label,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2
            )
        }
    }
}
