package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)

@Composable
fun CoolingChecklistView() {
    val items = remember {
        listOf(
            "Remove excess clothing",
            "Move to shade or cool area",
            "Apply cool water to skin",
            "Ice packs \u2014 neck, armpits, groin",
            "Fan continuously",
            "Wet cloth on forehead"
        )
    }
    val checked = remember { mutableStateListOf(false, false, false, false, false, false) }
    val checkedCount = checked.count { it }
    val allChecked = checkedCount == items.size
    val progress = checkedCount.toFloat() / items.size

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COOLING METHODS",
                color = TertiaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "$checkedCount/${items.size}",
                color = if (allChecked) GreenAccent else AmberAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (allChecked) GreenAccent else AmberAccent,
            trackColor = Color(0xFF2A2C30),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Checklist items
        items.forEachIndexed { index, item ->
            val isChecked = checked[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { checked[index] = !checked[index] }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular checkbox
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .then(
                            if (isChecked) Modifier
                                .background(GreenAccent)
                                .border(1.dp, GreenAccent, CircleShape)
                            else Modifier
                                .background(Color.Transparent)
                                .border(1.5.dp, Color(0xFF2A2C30), CircleShape)
                        )
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item,
                    color = if (isChecked) GreenAccent else PrimaryText,
                    fontSize = 13.sp,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }

        // All checked message
        if (allChecked) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent.copy(alpha = 0.1f))
                    .border(1.dp, GreenAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "All cooling methods active \u2014 do not leave casualty unattended",
                    color = GreenAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Do not use ice-cold water immersion \u2014 cool water only",
            color = TertiaryText,
            fontSize = 11.sp
        )
    }
}
