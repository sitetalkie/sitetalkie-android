package com.bitchat.android.ui

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet

private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val DarkText = Color(0xFF5A5E66)
private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val RedAccent = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnagDetailSheet(
    snag: Snag,
    myName: String,
    onDismiss: () -> Unit,
    onStartWork: () -> Unit,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (snag.status) {
        SnagStatus.OPEN -> RedAccent
        SnagStatus.IN_PROGRESS -> AmberAccent
        SnagStatus.RESOLVED -> GreenAccent
    }

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Priority + Status badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = snag.priority.color.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = snag.priority.color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${snag.priority.label} Priority",
                            color = snag.priority.color,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = snag.status.label,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = snag.title,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            // Description
            if (snag.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = snag.description,
                    color = SecondaryText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detail rows
            SnagDetailRow(icon = Icons.Filled.Layers, label = "Floor", value = formatSnagFloor(snag.floor))

            snag.trade?.let {
                SnagDetailRow(icon = Icons.Filled.Engineering, label = "Trade", value = it)
            }

            SnagDetailRow(
                icon = Icons.Filled.Person,
                label = "Created by",
                value = snag.createdBy
            )

            SnagDetailRow(
                icon = Icons.Filled.Schedule,
                label = "Created",
                value = formatTimeAgo(snag.createdAt)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons based on status
            if (snag.status == SnagStatus.OPEN) {
                Button(
                    onClick = onStartWork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Work", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (snag.status != SnagStatus.RESOLVED) {
                Button(
                    onClick = onResolve,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Resolved", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Delete button (only for own snags)
            if (snag.createdBy == myName) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Snag", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", fontFamily = FontFamily.Monospace, color = SecondaryText)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SnagDetailRow(
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

private fun formatSnagFloor(floor: Int): String = when {
    floor < 0 -> "Basement ${-floor}"
    floor == 0 -> "Ground Floor"
    else -> "Floor $floor"
}
