package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val AmberWarning = Color(0xFFE8960C)
private val GreenComplete = Color(0xFF34C759)
private val SecondaryText = Color(0xFF8A8E96)
private val PrimaryText = Color(0xFFF0F0F0)

@Composable
fun CountdownTimerView(
    totalSeconds: Int,
    label: String,
    accentColor: Color,
    note: String
) {
    var remaining by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    val elapsed = totalSeconds - remaining
    val progress = if (totalSeconds > 0) elapsed.toFloat() / totalSeconds else 0f
    val isComplete = remaining <= 0

    val arcColor = when {
        isComplete -> GreenComplete
        remaining < 60 -> AmberWarning
        else -> accentColor
    }

    LaunchedEffect(isRunning) {
        while (isRunning && remaining > 0) {
            delay(1000)
            if (isRunning) remaining--
        }
        if (remaining <= 0) isRunning = false
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF5A5E66),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Timer circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .drawBehind {
                    val strokeWidth = 4.dp.toPx()
                    val padding = strokeWidth / 2
                    drawArc(
                        color = arcColor.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(padding, padding),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(padding, padding),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
        ) {
            if (isComplete) {
                Text(
                    text = "Complete",
                    color = GreenComplete,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                val mins = remaining / 60
                val secs = remaining % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    color = PrimaryText,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isComplete) {
                IconButton(
                    onClick = { isRunning = !isRunning },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = accentColor.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = accentColor
                    )
                }
            }
            IconButton(
                onClick = {
                    isRunning = false
                    remaining = totalSeconds
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF242628)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = SecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = note,
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}
