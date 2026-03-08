package com.bitchat.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AmberAccent = Color(0xFFE8960C)
private val RedAccent = Color(0xFFE5484D)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)

private enum class VoltageSelection { NONE, LOW, HIGH }

@Composable
fun ElectricalBranchView() {
    var selection by remember { mutableStateOf(VoltageSelection.NONE) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Voltage buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Low Voltage
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selection == VoltageSelection.LOW) AmberAccent.copy(alpha = 0.2f)
                        else Color(0xFF242628)
                    )
                    .border(
                        1.dp,
                        if (selection == VoltageSelection.LOW) AmberAccent else Color(0xFF2A2C30),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selection = VoltageSelection.LOW }
            ) {
                Text(
                    text = "Low Voltage",
                    color = AmberAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // High Voltage
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selection == VoltageSelection.HIGH) RedAccent.copy(alpha = 0.2f)
                        else Color(0xFF242628)
                    )
                    .border(
                        1.dp,
                        if (selection == VoltageSelection.HIGH) RedAccent else Color(0xFF2A2C30),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selection = VoltageSelection.HIGH }
            ) {
                Text(
                    text = "High Voltage",
                    color = RedAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selection) {
            VoltageSelection.HIGH -> HighVoltageContent()
            VoltageSelection.LOW -> LowVoltageContent()
            VoltageSelection.NONE -> {}
        }
    }
}

@Composable
private fun HighVoltageContent() {
    // Pulsing warning
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(8.dp))
            .background(RedAccent.copy(alpha = 0.12f))
            .border(1.dp, RedAccent.copy(alpha = 0.33f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DO NOT APPROACH",
                color = RedAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Keep 25m back \u00b7 Call 999 and DNO \u00b7 Wait for engineers",
                color = Color(0xFFF08080),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val hvSteps = listOf(
        "Keep everyone back 25m",
        "Call 999 and grid operator/DNO",
        "No entry to zone",
        "Once isolated: treat as low voltage"
    )

    hvSteps.forEachIndexed { index, step ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300, delayMillis = index * 100)) +
                    slideInVertically(tween(300, delayMillis = index * 100)) { it / 4 }
        ) {
            BranchStepRow(index + 1, step, RedAccent)
        }
        if (index < hvSteps.lastIndex) Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun LowVoltageContent() {
    val lvSteps = listOf(
        "Isolate at source",
        "Cannot isolate? Dry non-conductive item",
        "Assess once isolated \u2014 CPR if needed",
        "Burns \u2014 dry sterile dressing only"
    )

    lvSteps.forEachIndexed { index, step ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300, delayMillis = index * 100)) +
                    slideInVertically(tween(300, delayMillis = index * 100)) { it / 4 }
        ) {
            BranchStepRow(index + 1, step, AmberAccent)
        }
        if (index < lvSteps.lastIndex) Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun BranchStepRow(number: Int, text: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1C20))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "%02d".format(number),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = text,
            color = PrimaryText,
            fontSize = 13.sp
        )
    }
}
