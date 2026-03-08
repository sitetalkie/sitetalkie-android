package com.bitchat.android.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bitchat.android.features.media.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerButton(
    modifier: Modifier = Modifier,
    onImageReady: (String) -> Unit
) {
    val context = LocalContext.current
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var showPickerSheet by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val outPath = ImageUtils.downscaleAndSaveToAppFiles(context, uri)
            if (!outPath.isNullOrBlank()) onImageReady(outPath)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = capturedImagePath
        if (success && !path.isNullOrBlank()) {
            val outPath = com.bitchat.android.features.media.ImageUtils.downscalePathAndSaveToAppFiles(context, path)
            if (!outPath.isNullOrBlank()) {
                onImageReady(outPath)
            }
            runCatching { File(path).delete() }
        } else {
            path?.let { runCatching { File(it).delete() } }
        }
        capturedImagePath = null
    }

    fun startCameraCapture() {
        try {
            val dir = File(context.filesDir, "images/outgoing").apply { mkdirs() }
            val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            capturedImagePath = file.absolutePath
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("ImagePickerButton", "Camera capture failed", e)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraCapture()
        }
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clickable { showPickerSheet = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = stringResource(com.bitchat.android.R.string.pick_image),
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }

    // Photo source picker bottom sheet
    if (showPickerSheet) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            containerColor = Color(0xFF1A1C20),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Take Photo option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPickerSheet = false
                            if (hasCameraPermission) {
                                startCameraCapture()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFFE8960C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Take Photo",
                        color = Color(0xFFF0F0F0),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF2A2C30),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // Choose from Library option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPickerSheet = false
                            imagePicker.launch("image/*")
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFFE8960C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Choose from Library",
                        color = Color(0xFFF0F0F0),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
