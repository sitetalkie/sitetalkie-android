package com.bitchat.android.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val PreviewBackground = Color(0xFF1A1C20)
private val PreviewBorder = Color(0xFF2A2C30)
private val SecondaryText = Color(0xFF8A8E96)
private val AmberAccent = Color(0xFFE8960C)
private val DangerRed = Color(0xFFE53935)

/**
 * Attachment preview bar shown above the text input when the user has staged
 * a photo, voice note, or document for sending.
 */
@Composable
fun AttachmentPreviewBar(
    attachment: PendingAttachment?,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = attachment != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (attachment != null) {
            Column {
                // Top border
                HorizontalDivider(thickness = 1.dp, color = PreviewBorder)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PreviewBackground)
                        .padding(12.dp)
                ) {
                    when (attachment) {
                        is PendingAttachment.Photo -> PhotoPreviewContent(
                            filePath = attachment.filePath,
                            onDiscard = onDiscard
                        )
                        is PendingAttachment.VoiceNote -> VoiceNotePreviewContent(
                            filePath = attachment.filePath,
                            durationMs = attachment.durationMs,
                            onDiscard = onDiscard
                        )
                        is PendingAttachment.Document -> DocumentPreviewContent(
                            fileName = attachment.fileName,
                            fileSize = attachment.fileSize,
                            onDiscard = onDiscard
                        )
                    }
                }
            }
        }
    }
}

// ── Photo Preview ──────────────────────────────────────────────────────────────

@Composable
private fun PhotoPreviewContent(
    filePath: String,
    onDiscard: () -> Unit
) {
    val bitmap = remember(filePath) {
        try {
            BitmapFactory.decodeFile(filePath)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Box {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            // Fallback if bitmap can't be decoded
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2C30)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // X dismiss button (top-right of thumbnail)
        DismissButton(
            onClick = onDiscard,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
        )
    }
}

// ── Voice Note Preview ─────────────────────────────────────────────────────────

@Composable
private fun VoiceNotePreviewContent(
    filePath: String,
    durationMs: Long,
    onDiscard: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var actualDurationMs by remember { mutableStateOf(durationMs) }
    val player = remember { MediaPlayer() }

    LaunchedEffect(filePath) {
        isPrepared = false
        isPlaying = false
        progress = 0f
        try {
            player.reset()
            player.setOnPreparedListener {
                isPrepared = true
                actualDurationMs = player.duration.toLong().coerceAtLeast(durationMs)
            }
            player.setOnCompletionListener {
                isPlaying = false
                progress = 1f
            }
            player.setOnErrorListener { _, _, _ ->
                isPlaying = false
                true
            }
            player.setDataSource(filePath)
            player.prepareAsync()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPlaying, isPrepared) {
        try {
            if (isPlaying && isPrepared) player.start()
            else if (isPrepared && player.isPlaying) player.pause()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            progress = try {
                player.currentPosition.toFloat() / player.duration.toFloat().coerceAtLeast(1f)
            } catch (_: Exception) { 0f }
            kotlinx.coroutines.delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { player.release() } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(AmberAccent, CircleShape)
                .clickable {
                    if (isPrepared) {
                        if (isPlaying) {
                            isPlaying = false
                        } else {
                            // If at end, restart
                            if (progress >= 1f) {
                                try { player.seekTo(0) } catch (_: Exception) {}
                                progress = 0f
                            }
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        // Waveform / progress bar
        com.bitchat.android.ui.media.WaveformPreview(
            modifier = Modifier
                .height(24.dp)
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            path = filePath,
            sendProgress = null,
            playbackProgress = progress,
            onSeek = { pos ->
                if (isPrepared && actualDurationMs > 0) {
                    val seekMs = (pos * actualDurationMs).toInt().coerceIn(0, actualDurationMs.toInt())
                    try {
                        player.seekTo(seekMs)
                        progress = pos
                    } catch (_: Exception) {}
                }
            }
        )

        // Duration text
        val durSecs = (actualDurationMs / 1000).toInt()
        val mm = durSecs / 60
        val ss = durSecs % 60
        Text(
            text = String.format("%d:%02d", mm, ss),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SecondaryText
        )

        // Dismiss button
        DismissButton(onClick = {
            try { if (isPlaying) { isPlaying = false } } catch (_: Exception) {}
            onDiscard()
        })
    }
}

// ── Document Preview ───────────────────────────────────────────────────────────

@Composable
private fun DocumentPreviewContent(
    fileName: String,
    fileSize: Long,
    onDiscard: () -> Unit
) {
    val isTooLarge = fileSize > 1_000_000L // 1 MB

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Document icon
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            tint = AmberAccent,
            modifier = Modifier.size(28.dp)
        )

        // Filename + size
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF0F0F0),
                maxLines = 1
            )
            Text(
                text = formatFileSize(fileSize),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SecondaryText
            )
            if (isTooLarge) {
                Text(
                    text = "File too large (max 1MB)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = DangerRed
                )
            }
        }

        // Dismiss button
        DismissButton(onClick = onDiscard)
    }
}

// ── Shared dismiss "X" button ──────────────────────────────────────────────────

@Composable
private fun DismissButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(Color(0xFF3A3C40), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Discard",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1_048_576 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / 1_048_576.0)
}
