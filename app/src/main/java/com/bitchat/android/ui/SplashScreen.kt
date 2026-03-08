package com.bitchat.android.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val GradientTopLeft = Color(0xFF14150F)
private val GradientBottomRight = Color(0xFF09090B)
private val AmberAccent = Color(0xFFE8960C)
private val AmberBright = Color(0xFFF5A623)
private val SubtitleColor = Color(0xFF8A8672)

private const val PREFS_NAME = "sitetalkie_prefs"
private const val KEY_HAS_COMPLETED_SETUP = "sitetalkie.hasCompletedSetup"

fun hasCompletedSetup(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_COMPLETED_SETUP, false)
}

fun setSetupCompleted(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAS_COMPLETED_SETUP, true)
        .apply()
}

enum class SplashDestination {
    SETUP,
    MAIN
}

@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(2000)
        if (hasCompletedSetup(context)) {
            onNavigate(SplashDestination.MAIN)
        } else {
            onNavigate(SplashDestination.SETUP)
        }
    }

    // --- Animations ---

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Radar sweep rotation: 360° every 2.5s, linear
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRotation"
    )

    // Node pulse animations (staggered by 0.6s = 600ms out of 2500ms cycle)
    val nodePulse0 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse0"
    )
    val nodePulse1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse1"
    )
    val nodePulse2 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut, delayMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse2"
    )
    val nodePulse3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut, delayMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse3"
    )
    val nodePulses = listOf(nodePulse0, nodePulse1, nodePulse2, nodePulse3)

    // Subtitle pulsing opacity: 40% to 80%, 1.5s ease
    val subtitleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtitleAlpha"
    )

    // Progress bar indicator sliding: 0 to 1, 1.8s ease
    val progressOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressSlide"
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientTopLeft, GradientBottomRight),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Canvas animation ---
            Canvas(
                modifier = Modifier.size(200.dp)
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val scale = w / 200f // scale from 200×200 viewBox

                // Node positions in viewBox coordinates
                val nodes = listOf(
                    Offset(140f * scale, 40f * scale),
                    Offset(55f * scale, 68f * scale),
                    Offset(145f * scale, 132f * scale),
                    Offset(60f * scale, 160f * scale)
                )

                // Outer ring radii: nodes 0,3 = 18dp, nodes 1,2 = 20dp
                val outerRingRadii = with(density) {
                    listOf(18.dp.toPx(), 20.dp.toPx(), 20.dp.toPx(), 18.dp.toPx())
                }
                // Middle circle radii: nodes 0,3 = 12dp, nodes 1,2 = 14dp
                val middleRadii = with(density) {
                    listOf(12.dp.toPx(), 14.dp.toPx(), 14.dp.toPx(), 12.dp.toPx())
                }
                val innerDotRadius = with(density) { 5.dp.toPx() }

                // --- 1. Static radar rings ---
                val ringStrokeWidth = with(density) { 0.6.dp.toPx() }
                val outerRadius = w * 0.86f / 2f
                val middleRadius = w * 0.58f / 2f
                val innerRadius = w * 0.30f / 2f

                // Inner ring
                drawCircle(
                    color = AmberAccent.copy(alpha = 0.10f),
                    radius = innerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = ringStrokeWidth)
                )
                // Middle ring
                drawCircle(
                    color = AmberAccent.copy(alpha = 0.075f),
                    radius = middleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = ringStrokeWidth)
                )
                // Outer ring
                drawCircle(
                    color = AmberAccent.copy(alpha = 0.055f),
                    radius = outerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = ringStrokeWidth)
                )

                // Diagonal crosshair lines corner-to-corner
                val crosshairStroke = with(density) { 0.35.dp.toPx() }
                val crosshairColor = AmberAccent.copy(alpha = 0.055f)
                drawLine(crosshairColor, Offset(0f, 0f), Offset(w, h), strokeWidth = crosshairStroke)
                drawLine(crosshairColor, Offset(w, 0f), Offset(0f, h), strokeWidth = crosshairStroke)

                // Tiny center dot
                val centerDotRadius = with(density) { 2.5.dp.toPx() }
                drawCircle(
                    color = AmberAccent.copy(alpha = 0.25f),
                    radius = centerDotRadius,
                    center = Offset(cx, cy)
                )

                // --- 2. Spinning radar sweep ---
                rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
                    // 45° pie slice
                    drawArc(
                        color = AmberAccent.copy(alpha = 0.08f),
                        startAngle = -22.5f,
                        sweepAngle = 45f,
                        useCenter = true,
                        topLeft = Offset(cx - outerRadius, cy - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2)
                    )
                    // Leading edge line (at +22.5° from sweep direction)
                    val leadingAngleRad = Math.toRadians(22.5).toFloat()
                    val leadEdgeStroke = with(density) { 1.dp.toPx() }
                    drawLine(
                        color = AmberBright.copy(alpha = 0.15f),
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + outerRadius * cos(leadingAngleRad),
                            cy + outerRadius * sin(leadingAngleRad)
                        ),
                        strokeWidth = leadEdgeStroke
                    )
                }

                // --- 3. Bold "S" curve (matching iOS cubic bezier) ---
                val s = scale
                val sPath = Path().apply {
                    moveTo(140f * s, 40f * s)
                    cubicTo(140f * s, 40f * s, 55f * s, 20f * s, 55f * s, 68f * s)
                    cubicTo(55f * s, 105f * s, 145f * s, 95f * s, 145f * s, 132f * s)
                    cubicTo(145f * s, 180f * s, 60f * s, 160f * s, 60f * s, 160f * s)
                }

                // Glow layer
                val glowWidth = with(density) { 14.dp.toPx() }
                drawPath(
                    path = sPath,
                    color = AmberBright.copy(alpha = 0.06f),
                    style = Stroke(
                        width = glowWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // Main curve
                val mainCurveWidth = with(density) { 8.dp.toPx() }
                drawPath(
                    path = sPath,
                    color = AmberBright.copy(alpha = 0.28f),
                    style = Stroke(
                        width = mainCurveWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // --- 4. Cross mesh lines ---
                val meshThick = with(density) { 2.dp.toPx() }
                val meshThin = with(density) { 1.5.dp.toPx() }
                val meshHighColor = AmberBright.copy(alpha = 0.07f)
                val meshLowColor = AmberBright.copy(alpha = 0.04f)

                // (140,40) to (145,132)
                drawLine(meshHighColor, nodes[0], nodes[2], strokeWidth = meshThick)
                // (55,68) to (60,160)
                drawLine(meshHighColor, nodes[1], nodes[3], strokeWidth = meshThick)
                // (140,40) to (60,160)
                drawLine(meshLowColor, nodes[0], nodes[3], strokeWidth = meshThin)
                // (55,68) to (145,132)
                drawLine(meshLowColor, nodes[1], nodes[2], strokeWidth = meshThin)

                // --- 5. Four node blips ---
                for (i in nodes.indices) {
                    val pos = nodes[i]
                    val pulseAlpha = nodePulses[i]

                    // Outer pulsing ring
                    drawCircle(
                        color = AmberAccent.copy(alpha = pulseAlpha),
                        radius = outerRingRadii[i],
                        center = pos,
                        style = Stroke(width = with(density) { 1.8.dp.toPx() })
                    )
                    // Middle filled circle
                    drawCircle(
                        color = AmberAccent,
                        radius = middleRadii[i],
                        center = pos
                    )
                    // Inner bright dot
                    drawCircle(
                        color = AmberBright,
                        radius = innerDotRadius,
                        center = pos
                    )
                }
            }

            // --- Below the animation ---
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SiteTalkie",
                color = AmberBright,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Scanning for nearby users",
                color = SubtitleColor.copy(alpha = subtitleAlpha),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Canvas(
                modifier = Modifier
                    .width(80.dp)
                    .height(3.dp)
            ) {
                val barW = size.width
                val barH = size.height
                val cornerRadius = barH / 2f

                // Track
                drawRoundRect(
                    color = AmberAccent.copy(alpha = 0.10f),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )

                // Indicator: 40% width, slides left to right
                val indicatorW = barW * 0.4f
                val maxTravel = barW - indicatorW
                val indicatorX = maxTravel * progressOffset

                drawRoundRect(
                    color = AmberAccent,
                    topLeft = Offset(indicatorX, 0f),
                    size = Size(indicatorW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            }
        }
    }
}
