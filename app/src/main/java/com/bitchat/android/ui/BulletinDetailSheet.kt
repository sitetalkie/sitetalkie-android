package com.bitchat.android.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val DarkText = Color(0xFF5A5E66)
private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val RedAccent = Color(0xFFE5484D)
private val BlueAccent = Color(0xFF3B82F6)
private val CardBg = Color(0xFF1A1C20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletinDetailSheet(
    bulletin: Bulletin,
    onDismiss: () -> Unit,
    onAcknowledge: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val priorityColor = when (bulletin.priority) {
        "urgent" -> RedAccent
        "high" -> AmberAccent
        "normal" -> BlueAccent
        else -> DarkText
    }

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Priority badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = priorityColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = bulletin.priority.replaceFirstChar { it.uppercase() },
                    color = priorityColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = bulletin.title,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            // Content
            if (bulletin.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = bulletin.content,
                    color = SecondaryText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            // Metadata
            Spacer(modifier = Modifier.height(16.dp))

            bulletin.createdBy?.let { author ->
                DetailRow(icon = Icons.Filled.Person, label = "Posted by", value = author)
            }

            if (bulletin.createdAt.isNotBlank()) {
                DetailRow(icon = Icons.Filled.Schedule, label = "Posted", value = formatDetailTime(bulletin.createdAt))
            }

            // Attachments
            val attachments = bulletin.attachments
            if (!attachments.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ATTACHMENTS",
                    color = AmberAccent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                attachments.forEach { attachment ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = CardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    attachment.type.startsWith("image") -> Icons.Filled.Image
                                    attachment.type.contains("pdf") -> Icons.Filled.Description
                                    else -> Icons.Filled.AttachFile
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = DarkText
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = attachment.name,
                                color = SecondaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Acknowledge button
            if (bulletin.requiresAck) {
                if (bulletin.isAcknowledged) {
                    // Already acknowledged
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = GreenAccent.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = GreenAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acknowledged",
                                color = GreenAccent,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            onAcknowledge(bulletin.id)
                            // Post ack to Supabase in background
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val prefs = context.getSharedPreferences("bitchat_prefs", Context.MODE_PRIVATE)
                                    val senderId = prefs.getString("nickname", "") ?: ""
                                    BulletinSyncService.postAcknowledgment(
                                        context = context,
                                        bulletinId = bulletin.id,
                                        senderId = senderId,
                                        displayName = senderId
                                    )
                                    BulletinStore.save(context)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Acknowledge",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Close button
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", fontFamily = FontFamily.Monospace, color = SecondaryText)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = DarkText
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = DarkText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = SecondaryText,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

private fun formatDetailTime(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        val date = sdf.parse(dateString) ?: return dateString.take(16)
        java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.US).format(date)
    } catch (_: Exception) {
        dateString.take(16)
    }
}
