package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.bitchat.android.features.media.ImageUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import java.io.File

private const val PREFS_NAME = "bitchat_prefs"
private const val KEY_FLOOR = "currentFloorNumber"
private const val FLOOR_MIN = -3
private const val FLOOR_MAX = 50

private val AmberAccent = Color(0xFFE8960C)
private val CardBg = Color(0xFF1A1C20)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePinSheet(
    onDismiss: () -> Unit,
    onCreatePin: (SitePin) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val siteTalkiePrefs = remember { context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE) }

    var selectedType by remember { mutableStateOf<SitePinType?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currentFloor by remember { mutableIntStateOf(prefs.getInt(KEY_FLOOR, 1)) }
    var selectedPrecision by remember { mutableStateOf(PinPrecision.PRECISE) }
    var selectedExpiry by remember { mutableStateOf<String>("none") } // "none", "24h", "1w", "resolved"
    var photoPath by remember { mutableStateOf<String?>(null) }

    // Location state
    var currentLat by remember { mutableDoubleStateOf(0.0) }
    var currentLng by remember { mutableDoubleStateOf(0.0) }
    var hasLocation by remember { mutableStateOf(false) }

    // Fetch location on sheet open
    LaunchedEffect(Unit) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(15000)
                    .build()
                fusedClient.getCurrentLocation(request, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLat = location.latitude
                            currentLng = location.longitude
                            hasLocation = true
                        }
                    }
                // Also try last known as fallback
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (!hasLocation && location != null) {
                        currentLat = location.latitude
                        currentLng = location.longitude
                        hasLocation = true
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Photo picker
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val outPath = ImageUtils.downscaleAndSaveToAppFiles(context, uri)
            if (!outPath.isNullOrBlank()) photoPath = outPath
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = capturedImagePath
        if (success && !path.isNullOrBlank()) {
            val outPath = ImageUtils.downscalePathAndSaveToAppFiles(context, path)
            if (!outPath.isNullOrBlank()) photoPath = outPath
            runCatching { File(path).delete() }
        } else {
            path?.let { runCatching { File(it).delete() } }
        }
        capturedImagePath = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraCapture(context) { path, uri ->
                capturedImagePath = path
                takePictureLauncher.launch(uri)
            }
        }
    }

    val canCreate = selectedType != null && title.isNotBlank() && hasLocation

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = AmberAccent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Create Pin",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp
                    ),
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pin type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PinTypeCard(SitePinType.HAZARD, selectedType == SitePinType.HAZARD, { selectedType = SitePinType.HAZARD }, Modifier.weight(1f))
                PinTypeCard(SitePinType.NOTE, selectedType == SitePinType.NOTE, { selectedType = SitePinType.NOTE }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("What's here?", fontFamily = FontFamily.Monospace, color = SecondaryText)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = SecondaryText.copy(alpha = 0.5f),
                    cursorColor = AmberAccent,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text("Add details...", fontFamily = FontFamily.Monospace, color = SecondaryText)
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
                    unfocusedBorderColor = SecondaryText.copy(alpha = 0.5f),
                    cursorColor = AmberAccent,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Floor selector
            Text(
                text = "Floor",
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

            // Precision selector
            Text(
                text = "Precision",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PinPrecision.entries.forEach { precision ->
                    PillSelector(
                        text = precision.label,
                        isSelected = selectedPrecision == precision,
                        onClick = { selectedPrecision = precision },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo button
            if (photoPath != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = CardBg
                    ) {
                        // Show a photo indicator
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Photo,
                                contentDescription = "Photo",
                                tint = AmberAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Photo attached", color = SecondaryText, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { photoPath = null }) {
                        Icon(Icons.Filled.Close, "Remove photo", tint = Color(0xFFE5484D))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showPhotoPickerSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, SecondaryText.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.PhotoCamera, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Photo", color = SecondaryText, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry selector
            Text(
                text = "Expires",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val expiryOptions = listOf("none" to "No expiry", "24h" to "24 hours", "1w" to "1 week", "resolved" to "Until resolved")
                expiryOptions.forEach { (key, label) ->
                    PillSelector(
                        text = label,
                        isSelected = selectedExpiry == key,
                        onClick = { selectedExpiry = key },
                        modifier = Modifier.weight(1f),
                        fontSize = 10
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasLocation) {
                Text(
                    text = "Acquiring GPS location...",
                    color = AmberAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create button
            val buttonColor = selectedType?.color ?: SecondaryText
            Button(
                onClick = {
                    val type = selectedType ?: return@Button
                    val nickname = siteTalkiePrefs.getString("com.sitetalkie.user.trade", null)
                    val displayName = context.getSharedPreferences("bitchat_prefs", Context.MODE_PRIVATE)
                        .getString("nickname", "Unknown") ?: "Unknown"

                    val expiresAt = when (selectedExpiry) {
                        "24h" -> System.currentTimeMillis() + 24 * 3600_000L
                        "1w" -> System.currentTimeMillis() + 7 * 24 * 3600_000L
                        else -> null
                    }

                    val pin = SitePin(
                        type = type,
                        title = title.trim(),
                        description = description.trim(),
                        latitude = currentLat,
                        longitude = currentLng,
                        floor = currentFloor,
                        createdBy = displayName,
                        expiresAt = expiresAt,
                        isResolved = false,
                        photoUri = photoPath,
                        radius = selectedPrecision.radiusMetres,
                        precision = selectedPrecision
                    )
                    onCreatePin(pin)
                    Toast.makeText(context, "Pin created \u2713", Toast.LENGTH_SHORT).show()
                },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = SecondaryText.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CREATE PIN",
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

    // Photo source picker sheet
    if (showPhotoPickerSheet) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        ModalBottomSheet(
            onDismissRequest = { showPhotoPickerSheet = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoPickerSheet = false
                            if (hasCameraPermission) {
                                startCameraCapture(context) { path, uri ->
                                    capturedImagePath = path
                                    takePictureLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoCamera, null, tint = AmberAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Take Photo", color = PrimaryText, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
                HorizontalDivider(color = Color(0xFF2A2C30), modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoPickerSheet = false
                            imagePicker.launch("image/*")
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = AmberAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Choose from Library", color = PrimaryText, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun startCameraCapture(context: Context, onReady: (String, Uri) -> Unit) {
    try {
        val dir = File(context.filesDir, "images/outgoing").apply { mkdirs() }
        val file = File(dir, "pin_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        onReady(file.absolutePath, uri)
    } catch (e: Exception) {
        android.util.Log.e("CreatePinSheet", "Camera capture failed", e)
    }
}

@Composable
private fun PinTypeCard(
    type: SitePinType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) type.color else Color.Transparent
    val bgColor = if (isSelected) type.color.copy(alpha = 0.10f) else CardBg

    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                modifier = Modifier.size(24.dp),
                tint = type.color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PillSelector(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 12
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) AmberAccent.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) AmberAccent else SecondaryText.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = text,
                color = if (isSelected) Color.White else SecondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
