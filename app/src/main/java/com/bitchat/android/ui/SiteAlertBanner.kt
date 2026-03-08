package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SiteAlertBanner(
    alert: SiteAlert,
    senderName: String,
    timestamp: Date,
    modifier: Modifier = Modifier,
    onOpenProtocol: ((String) -> Unit)? = null
) {
    val alertColor = alert.type.color
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = alertColor,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            },
        shape = RoundedCornerShape(4.dp),
        color = alertColor.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = alert.type.icon,
                    contentDescription = alert.type.label,
                    modifier = Modifier.size(16.dp),
                    tint = alertColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = alert.type.label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    color = alertColor
                )
            }

            if (alert.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.detail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFFF0F0F0)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFloorDisplay(alert.floor),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF8A8E96)
                )
                Text(
                    text = "$senderName ${timeFormatter.format(timestamp)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF8A8E96)
                )
            }

            // Deep link to Emergency Handbook protocol
            if (alert.type.scenarioId != null && onOpenProtocol != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "See First Aid Protocol",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = Color(0xFFE8960C),
                    modifier = Modifier.clickable { onOpenProtocol(alert.type.scenarioId!!) }
                )
            }
        }
    }
}
