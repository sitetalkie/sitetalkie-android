package com.bitchat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val SeparatorLineColor = Color(0xFF2A2C30)
private val SeparatorTextColor = Color(0xFF5A5E66)

@Composable
fun DateSeparatorItem(dateText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = SeparatorLineColor
        )
        Text(
            text = dateText,
            color = SeparatorTextColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = SeparatorLineColor
        )
    }
}

fun formatDateSeparatorText(date: Date): String {
    val messageCalendar = Calendar.getInstance().apply { time = date }
    val todayCalendar = Calendar.getInstance()

    return when {
        messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR) -> "Today"

        run {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            messageCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    messageCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
        } -> "Yesterday"

        else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(date)
    }
}
