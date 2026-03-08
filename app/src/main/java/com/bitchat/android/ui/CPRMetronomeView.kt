package com.bitchat.android.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val ElevatedBg = Color(0xFF242628)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)

private enum class CprPhase { COMPRESS, BREATHE }

@Composable
fun CPRMetronomeView() {
    var isRunning by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(CprPhase.COMPRESS) }
    var count by remember { mutableIntStateOf(0) }
    var cyclesComplete by remember { mutableIntStateOf(0) }

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { toneGenerator?.release() } catch (_: Exception) {}
        }
    }

    // Metronome timer
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        // Reset on start
        phase = CprPhase.COMPRESS
        count = 0

        while (isRunning) {
            delay(545) // 110 BPM
            if (!isRunning) break

            if (phase == CprPhase.COMPRESS) {
                count++
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50) } catch (_: Exception) {}
                if (count >= 30) {
                    phase = CprPhase.BREATHE
                    count = 0
                }
            } else {
                count++
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 100) } catch (_: Exception) {}
                if (count >= 2) {
                    cyclesComplete++
                    phase = CprPhase.COMPRESS
                    count = 0
                }
            }
        }
    }

    val maxCount = if (phase == CprPhase.COMPRESS) 30 else 2
    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val arcColor = if (phase == CprPhase.COMPRESS) AmberAccent else GreenAccent
    val orbColor = when {
        !isRunning -> ElevatedBg
        phase == CprPhase.COMPRESS -> AmberAccent
        else -> GreenAccent
    }

    // Pulse animation for compressions
    val pulseScale by animateFloatAsState(
        targetValue = if (isRunning && phase == CprPhase.COMPRESS && count > 0) 1.05f else 1f,
        animationSpec = tween(150),
        label = "pulse"
    )
    // Breathing animation
    val breathScale by animateFloatAsState(
        targetValue = if (isRunning && phase == CprPhase.BREATHE) 1.08f else 1f,
        animationSpec = tween(400, easing = EaseInOutSine),
        label = "breath"
    )
    val currentScale = if (phase == CprPhase.COMPRESS) pulseScale else breathScale

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .scale(currentScale)
                .drawBehind {
                    // Progress arc
                    val strokeWidth = 4.dp.toPx()
                    val padding = strokeWidth / 2
                    drawArc(
                        color = arcColor.copy(alpha = 0.3f),
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
                .clip(CircleShape)
                .background(orbColor.copy(alpha = if (isRunning) 0.2f else 1f))
                .clickable { isRunning = !isRunning }
        ) {
            if (!isRunning) {
                Text(
                    text = "Tap to start",
                    color = TertiaryText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$count",
                        color = Color.Black,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "of $maxCount",
                        color = Color.Black.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase card
        val phaseCardColor = if (phase == CprPhase.COMPRESS) AmberAccent else GreenAccent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(phaseCardColor.copy(alpha = 0.12f))
                .border(1.dp, phaseCardColor.copy(alpha = 0.33f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (phase == CprPhase.COMPRESS || !isRunning) {
                Column {
                    Text(
                        text = "Push hard. Push fast.",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "5\u20136 cm depth \u00b7 full chest recoil",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                Column {
                    Text(
                        text = "Tilt head. Pinch nose. 2 breaths.",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Watch chest rise \u00b7 1 second each",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$cyclesComplete cycles",
                color = SecondaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "110 BPM",
                color = AmberAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            TextButton(onClick = {
                isRunning = false
                phase = CprPhase.COMPRESS
                count = 0
                cyclesComplete = 0
            }) {
                Text("Reset", color = SecondaryText, fontSize = 12.sp)
            }
        }
    }
}
