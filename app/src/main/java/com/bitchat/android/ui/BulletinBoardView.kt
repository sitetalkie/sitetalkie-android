package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val ScreenBg = Color(0xFF0E1012)
private val CardBg = Color(0xFF1A1C20)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val DarkText = Color(0xFF5A5E66)
private val AmberAccent = Color(0xFFE8960C)
private val RedAccent = Color(0xFFE5484D)
private val GreenAccent = Color(0xFF34C759)
private val BlueAccent = Color(0xFF3B82F6)

private enum class BulletinFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    ACTION("Action Required")
}

@Composable
fun BulletinBoardView(
    onBulletinClick: (Bulletin) -> Unit
) {
    val bulletins by BulletinStore.bulletins.collectAsStateWithLifecycle()
    val unreadCount by BulletinStore.unreadCount.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(BulletinFilter.ALL) }

    val filtered = remember(bulletins, selectedFilter) {
        when (selectedFilter) {
            BulletinFilter.ALL -> bulletins
            BulletinFilter.UNREAD -> bulletins.filter { !it.isRead }
            BulletinFilter.ACTION -> bulletins.filter { it.requiresAck && !it.isAcknowledged }
        }
    }

    if (bulletins.isEmpty()) {
        // Empty state
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = DarkText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No bulletins yet",
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Site bulletins from your team will appear here",
                color = SecondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header with unread badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BULLETINS",
                        color = AmberAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = RedAccent
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Filter pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BulletinFilter.entries.forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            modifier = Modifier.clickable { selectedFilter = filter },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) AmberAccent else CardBg
                        ) {
                            Text(
                                text = filter.label,
                                color = if (isSelected) Color.White else SecondaryText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = when (selectedFilter) {
                            BulletinFilter.UNREAD -> "All caught up"
                            BulletinFilter.ACTION -> "No action required"
                            else -> "No bulletins"
                        },
                        color = DarkText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                }
            } else {
                items(filtered, key = { it.id }) { bulletin ->
                    BulletinCard(
                        bulletin = bulletin,
                        onClick = {
                            BulletinStore.markAsRead(bulletin.id)
                            onBulletinClick(bulletin)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletinCard(
    bulletin: Bulletin,
    onClick: () -> Unit
) {
    val priorityColor = when (bulletin.priority) {
        "urgent" -> RedAccent
        "high" -> AmberAccent
        "normal" -> BlueAccent
        else -> DarkText
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable { onClick() }
            .drawBehind {
                drawRect(
                    color = priorityColor,
                    topLeft = Offset.Zero,
                    size = Size(4.dp.toPx(), size.height)
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = CardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Unread dot
            if (!bulletin.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .size(8.dp)
                        .background(AmberAccent, CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = bulletin.title,
                    color = PrimaryText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Content preview
                if (bulletin.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bulletin.content,
                        color = SecondaryText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: time + badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatBulletinTime(bulletin.createdAt),
                        color = DarkText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    // Attachment count
                    val attachmentCount = bulletin.attachments?.size ?: 0
                    if (attachmentCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = DarkText
                        )
                        Text(
                            text = attachmentCount.toString(),
                            color = DarkText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    // Ack badge
                    if (bulletin.requiresAck) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (bulletin.isAcknowledged) GreenAccent.copy(alpha = 0.15f) else RedAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (bulletin.isAcknowledged) "ACK'D" else "ACK REQ",
                                color = if (bulletin.isAcknowledged) GreenAccent else RedAccent,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBulletinTime(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        val date = sdf.parse(dateString) ?: return dateString.take(10)
        val now = System.currentTimeMillis()
        val diff = now - date.time
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> java.text.SimpleDateFormat("dd MMM", java.util.Locale.US).format(date)
        }
    } catch (_: Exception) {
        dateString.take(10)
    }
}
