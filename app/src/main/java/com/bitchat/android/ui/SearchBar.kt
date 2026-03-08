package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SearchBackground = Color(0xFF1A1C20)
private val PlaceholderColor = Color(0xFF5A5E66)
private val ResultCountColor = Color(0xFF8A8E96)
private val AmberCursor = Color(0xFFE8960C)
private val PrimaryText = Color(0xFFF0F0F0)

@Composable
fun MessageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(SearchBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(AmberCursor),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (query.isEmpty()) {
                    Text(
                        text = "Search messages...",
                        color = PlaceholderColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close search",
                tint = ResultCountColor,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClose() }
            )
        }

        // Result count
        if (query.isNotEmpty()) {
            Text(
                text = if (resultCount > 0) "$resultCount results" else "No messages found",
                color = ResultCountColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
            )
        }
    }
}
