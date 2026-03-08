package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// SiteTalkie construction amber theme — dark only
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8960C),        // Construction amber accent
    onPrimary = Color.Black,
    secondary = Color(0xFFD4870B),      // Slightly darker amber
    onSecondary = Color.Black,
    background = Color(0xFF0E1012),     // Near-black background
    onBackground = Color(0xFFF0F0F0),   // Primary text
    surface = Color(0xFF1A1C20),        // Card/surface
    onSurface = Color(0xFFF0F0F0),      // Primary text on surface
    surfaceVariant = Color(0xFF1A1C20), // Card/surface variant
    onSurfaceVariant = Color(0xFF8A8E96), // Secondary text
    error = Color(0xFFFF5555),          // Red for errors
    onError = Color.Black,
    outline = Color(0xFF8A8E96)         // Secondary text for outlines
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // SiteTalkie: always dark theme
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
