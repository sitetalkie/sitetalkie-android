package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RedAccent = Color(0xFFE5484D)
private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val GreyAccent = Color(0xFF8A8E96)
private val PrimaryText = Color(0xFFF0F0F0)
private val TertiaryText = Color(0xFF5A5E66)

private data class BranchOption(
    val label: String,
    val color: Color,
    val redirect: String
)

@Composable
fun LoneWorkerBranchView() {
    val options = remember {
        listOf(
            BranchOption("Not breathing", RedAccent, "Cardiac Arrest protocol \u2014 CPR immediately"),
            BranchOption("Severe bleeding", AmberAccent, "Severe Bleeding protocol \u2014 direct pressure now"),
            BranchOption("Conscious", GreenAccent, "Assess injuries. Reassure. Do not move."),
            BranchOption("Unconscious, breathing", GreyAccent, "Recovery position. Monitor airway. Await 999.")
        )
    }
    var selected by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "What did you find?",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 2x2 grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        val option = options[index]
                        val isSelected = selected == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) option.color.copy(alpha = 0.15f)
                                    else Color(0xFF242628)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) option.color.copy(alpha = 0.5f)
                                    else Color(0xFF2A2C30),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selected = index }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = option.label,
                                    color = option.color,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = option.redirect,
                                        color = PrimaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
