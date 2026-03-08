package com.bitchat.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.mesh.PeerInfo
import kotlinx.coroutines.delay
import kotlin.math.*

// ─── Design tokens ───────────────────────────────────────────────────────────
private val RadarBackground = Color(0xFF0E1012)
private val AmberAccent = Color(0xFFE8960C)
private val CardBg = Color(0xFF1A1C20)
private val CardBorder = Color(0xFF2A2C30)
private val SecondaryGrey = Color(0xFF8A8E96)
private val CompassGrey = Color(0xFF5A5E66)
private val GreenDot = Color(0xFF34C759)
private val ScreenBackground = Color(0xFF0E1012)

// ─── Public entry point ──────────────────────────────────────────────────────
@Composable
fun NearbyRadarScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val meshService = viewModel.meshService

    // ── Compass ──
    val compassManager = remember { RadarCompassManager(context) }
    val heading by compassManager.heading.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        compassManager.start()
        onDispose { compassManager.stop() }
    }

    // ── Location ──
    val locationManager = remember { RadarLocationManager(context) }
    val myLocation by locationManager.location.collectAsStateWithLifecycle()

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locationManager.start()
    }

    // Start / stop location with screen visibility
    DisposableEffect(Unit) {
        if (locationManager.hasPermission()) {
            locationManager.start()
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        onDispose { locationManager.stop() }
    }

    // ── Peer data (refreshed every 2s) ──
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val peerRSSI by viewModel.peerRSSI.collectAsStateWithLifecycle()
    var refreshTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            refreshTick++
        }
    }

    val radarPeers = remember(connectedPeers, peerNicknames, peerRSSI, refreshTick) {
        buildRadarPeers(meshService, myLocation)
    }

    // ── Floor selector ──
    val prefs = remember { context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE) }
    var currentFloor by remember { mutableIntStateOf(prefs.getInt("currentFloorNumber", 0)) }

    // ── Scanning badge pulse ──
    val scanPulse = rememberInfiniteTransition(label = "scanPulse")
    val scanAlpha by scanPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = LinearEasing), RepeatMode.Reverse
        ), label = "scanAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        // ── HEADER ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nearby", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            // Scanning badge
            Row(
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AmberAccent.copy(alpha = scanAlpha), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text("Scanning...", color = AmberAccent, fontSize = 12.sp)
            }
        }

        // ── INFO BAR ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${radarPeers.size} ${if (radarPeers.size == 1) "person" else "people"} nearby",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            // Floor selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Floor down",
                    tint = if (currentFloor > -3) AmberAccent else CompassGrey,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(enabled = currentFloor > -3) {
                            currentFloor--
                            prefs.edit().putInt("currentFloorNumber", currentFloor).apply()
                        }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (currentFloor == 0) "Ground" else "Floor $currentFloor",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Floor up",
                    tint = if (currentFloor < 50) AmberAccent else CompassGrey,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(enabled = currentFloor < 50) {
                            currentFloor++
                            prefs.edit().putInt("currentFloorNumber", currentFloor).apply()
                        }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── RADAR CANVAS ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarCanvas(
                heading = heading,
                radarPeers = radarPeers,
                currentFloor = currentFloor,
                myLocation = myLocation
            )
        }

        // ── INVITE CARD ──
        InviteCard(context)

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Radar Peer data ─────────────────────────────────────────────────────────
private data class RadarPeer(
    val peerID: String,
    val name: String,
    val trade: String?,
    val rssi: Int?,
    val distance: Float?,       // metres, null if no GPS
    val bearing: Float?,        // degrees from north, null if no GPS
    val hasGps: Boolean,
    val floorDelta: Int?        // positive = above, negative = below
)

private fun buildRadarPeers(
    meshService: BluetoothMeshService,
    myLocation: Location?
): List<RadarPeer> {
    val now = System.currentTimeMillis()
    val myPeerID = meshService.myPeerID
    val allPeers = meshService.getAllPeers()
    val rssiMap = meshService.getPeerRSSI()

    return allPeers.values
        .filter { it.id != myPeerID && it.isConnected && (now - it.lastSeen) < 120_000L }
        .map { peer ->
            val rssi = rssiMap[peer.id]
            RadarPeer(
                peerID = peer.id,
                name = stripRadarHashSuffix(peer.nickname),
                trade = peer.trade,
                rssi = rssi,
                distance = null,    // GPS peer-to-peer not available yet (future TLV 0x06/0x07/0x08)
                bearing = null,
                hasGps = false,
                floorDelta = null
            )
        }
}

private fun stripRadarHashSuffix(name: String): String {
    if (name.length < 5) return name
    val suffix = name.takeLast(5)
    if (suffix.startsWith("#") && suffix.drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return name.dropLast(5)
    }
    return name
}

// ─── Radar Canvas composable ─────────────────────────────────────────────────
@Composable
private fun RadarCanvas(
    heading: Float,
    radarPeers: List<RadarPeer>,
    currentFloor: Int,
    myLocation: Location?
) {
    // Sweep animation — 360° every 3 seconds
    val sweepTransition = rememberInfiniteTransition(label = "sweep")
    val sweepAngle by sweepTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing), RepeatMode.Restart
        ), label = "sweepAngle"
    )

    // Peer glow pulse
    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by glowTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glowScale"
    )
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) * 0.88f // leave room for compass labels

        val innerR = maxR * 0.33f
        val midR = maxR * 0.66f
        val outerR = maxR * 1.0f

        // ── Crosshairs ──
        val crossColor = AmberAccent.copy(alpha = 0.20f)
        val crossStroke = with(density) { 0.5.dp.toPx() }
        drawLine(crossColor, Offset(cx - outerR, cy), Offset(cx + outerR, cy), crossStroke)
        drawLine(crossColor, Offset(cx, cy - outerR), Offset(cx, cy + outerR), crossStroke)

        // ── Concentric rings ──
        val ringStroke = with(density) { 2.dp.toPx() }
        drawCircle(AmberAccent.copy(alpha = 0.35f), innerR, Offset(cx, cy), style = Stroke(ringStroke))
        drawCircle(AmberAccent.copy(alpha = 0.30f), midR, Offset(cx, cy), style = Stroke(ringStroke))
        drawCircle(AmberAccent.copy(alpha = 0.25f), outerR, Offset(cx, cy), style = Stroke(ringStroke))

        // ── Distance labels ──
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#8A8E96")
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("10m", cx + midR + with(density) { 4.dp.toPx() }, cy - with(density) { 4.dp.toPx() }, labelPaint)
        drawContext.canvas.nativeCanvas.drawText("30m", cx + outerR + with(density) { 4.dp.toPx() }, cy - with(density) { 4.dp.toPx() }, labelPaint)

        // ── Sweep wedge (45°) ──
        val sweepStart = sweepAngle - 90f // canvas arc starts at 3 o'clock; -90 to start from top
        drawArc(
            color = AmberAccent.copy(alpha = 0.12f),
            startAngle = sweepStart,
            sweepAngle = 45f,
            useCenter = true,
            topLeft = Offset(cx - outerR, cy - outerR),
            size = Size(outerR * 2, outerR * 2),
            style = Fill
        )

        // ── Compass labels (rotate with heading so "up" = user facing direction) ──
        val headingRad = Math.toRadians(heading.toDouble())
        val compassPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#5A5E66")
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val compassBoldPaint = android.graphics.Paint(compassPaint).apply {
            isFakeBoldText = true
        }
        val compassOffset = outerR + with(density) { 14.dp.toPx() }
        data class CompassLabel(val label: String, val angleDeg: Float, val bold: Boolean)
        val labels = listOf(
            CompassLabel("N", 0f, true),
            CompassLabel("E", 90f, false),
            CompassLabel("S", 180f, false),
            CompassLabel("W", 270f, false)
        )
        for (cl in labels) {
            val angle = Math.toRadians((cl.angleDeg - heading).toDouble())
            val lx = cx + compassOffset * sin(angle).toFloat()
            val ly = cy - compassOffset * cos(angle).toFloat()
            val paint = if (cl.bold) compassBoldPaint else compassPaint
            drawContext.canvas.nativeCanvas.drawText(cl.label, lx, ly + with(density) { 4.dp.toPx() }, paint)
        }

        // ── Center dot (user) ──
        val centerDotR = with(density) { 5.dp.toPx() }
        drawCircle(Color.White.copy(alpha = 0.80f), centerDotR, Offset(cx, cy))

        // ── Peer dots ──
        val peerDotR = with(density) { 5.dp.toPx() }
        val peerBrightR = with(density) { 2.5.dp.toPx() }
        val peerNamePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        val peerTradePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#8A8E96")
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val peerCount = radarPeers.size
        radarPeers.forEachIndexed { index, peer ->
            val dotColor: Color
            val distLabel: String
            val ringRadius: Float

            if (peer.hasGps && peer.bearing != null && peer.distance != null) {
                // GPS-based placement
                dotColor = GreenDot
                ringRadius = when {
                    peer.distance < 10f -> innerR * (peer.distance / 10f).coerceIn(0.2f, 1f)
                    peer.distance < 30f -> innerR + (midR - innerR) * ((peer.distance - 10f) / 20f)
                    else -> midR + (outerR - midR) * ((peer.distance - 30f) / 30f).coerceAtMost(1f)
                }
                distLabel = "${peer.distance.toInt()}m"
                val placementAngle = Math.toRadians((peer.bearing - heading).toDouble())
                val px = cx + ringRadius * sin(placementAngle).toFloat()
                val py = cy - ringRadius * cos(placementAngle).toFloat()
                drawPeerDot(px, py, dotColor, peerDotR, peerBrightR, glowScale, glowAlpha, peer, distLabel, currentFloor, peerNamePaint, peerTradePaint, density)
            } else {
                // RSSI-based fallback — distribute evenly
                dotColor = AmberAccent
                val rssi = peer.rssi ?: -70
                when {
                    rssi > -50 -> { ringRadius = innerR; distLabel = "~5m" }
                    rssi > -70 -> { ringRadius = midR; distLabel = "~15m" }
                    else -> { ringRadius = outerR; distLabel = "~30m+" }
                }
                // Even angular distribution offset by heading
                val sliceAngle = 360.0 / peerCount.coerceAtLeast(1)
                val angleDeg = sliceAngle * index
                val angleRad = Math.toRadians(angleDeg - heading.toDouble())
                val px = cx + ringRadius * sin(angleRad).toFloat()
                val py = cy - ringRadius * cos(angleRad).toFloat()
                drawPeerDot(px, py, dotColor, peerDotR, peerBrightR, glowScale, glowAlpha, peer, distLabel, currentFloor, peerNamePaint, peerTradePaint, density)
            }
        }
    }
}

private fun DrawScope.drawPeerDot(
    px: Float, py: Float,
    color: Color,
    dotR: Float, brightR: Float,
    glowScale: Float, glowAlpha: Float,
    peer: RadarPeer,
    distLabel: String,
    currentFloor: Int,
    namePaint: android.graphics.Paint,
    tradePaint: android.graphics.Paint,
    density: androidx.compose.ui.unit.Density
) {
    // Outer glow ring
    drawCircle(color.copy(alpha = glowAlpha), dotR * glowScale, Offset(px, py), style = Stroke(with(density) { 1.5.dp.toPx() }))
    // Filled dot
    drawCircle(color, dotR, Offset(px, py))
    // Bright center
    drawCircle(Color.White, brightR, Offset(px, py))

    // Label below dot
    val labelY = py + dotR + with(density) { 12.dp.toPx() }
    drawContext.canvas.nativeCanvas.drawText(peer.name, px, labelY, namePaint)

    // Trade abbreviation
    val tradeText = peer.trade?.take(12) ?: ""
    if (tradeText.isNotEmpty()) {
        drawContext.canvas.nativeCanvas.drawText(tradeText, px, labelY + with(density) { 12.dp.toPx() }, tradePaint)
    }

    // Distance
    val distY = labelY + (if (tradeText.isNotEmpty()) with(density) { 24.dp.toPx() } else with(density) { 12.dp.toPx() })
    drawContext.canvas.nativeCanvas.drawText(distLabel, px, distY, tradePaint)

    // Floor delta
    if (peer.floorDelta != null && peer.floorDelta != 0) {
        val floorLabel = if (peer.floorDelta > 0) "↑F${peer.floorDelta}" else "↓B${-peer.floorDelta}"
        drawContext.canvas.nativeCanvas.drawText(floorLabel, px, distY + with(density) { 12.dp.toPx() }, tradePaint)
    }
}

// ─── Invite Card ─────────────────────────────────────────────────────────────
@Composable
private fun InviteCard(context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Download SiteTalkie — free offline messaging for construction sites. No signal needed. https://sitetalkie.com"
                    )
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share SiteTalkie"))
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Share, contentDescription = "Share", tint = AmberAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Invite to SiteTalkie", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("More phones = stronger mesh", color = SecondaryGrey, fontSize = 12.sp)
        }
        Text("›", color = CompassGrey, fontSize = 20.sp)
    }
}
