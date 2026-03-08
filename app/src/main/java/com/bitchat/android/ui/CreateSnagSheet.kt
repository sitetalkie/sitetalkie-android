package com.bitchat.android.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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

private const val PREFS_NAME = "bitchat_prefs"
private const val KEY_FLOOR = "currentFloorNumber"
private const val FLOOR_MIN = -3
private const val FLOOR_MAX = 50

private val AmberAccent = Color(0xFFE8960C)
private val CardBg = Color(0xFF1A1C20)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSnagSheet(
    onDismiss: () -> Unit,
    onCreateSnag: (Snag, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val siteTalkiePrefs = remember { context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(SnagPriority.MEDIUM) }
    var currentFloor by remember { mutableIntStateOf(prefs.getInt(KEY_FLOOR, 1)) }
    var showTradePicker by remember { mutableStateOf(false) }

    val userTrade = remember {
        siteTalkiePrefs.getString("com.sitetalkie.user.trade", null)
    }
    var selectedTrade by remember { mutableStateOf(userTrade) }

    val displayName = remember {
        context.getSharedPreferences("bitchat_prefs", Context.MODE_PRIVATE)
            .getString("nickname", "Unknown") ?: "Unknown"
    }

    val canCreate = title.isNotBlank()

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = AmberAccent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Report Snag",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp
                    ),
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("What's the issue?", fontFamily = FontFamily.Monospace, color = SecondaryText)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = SecondaryText.copy(alpha = 0.5f),
                    cursorColor = AmberAccent,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text("Add details...", fontFamily = FontFamily.Monospace, color = SecondaryText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = SecondaryText.copy(alpha = 0.5f),
                    cursorColor = AmberAccent,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Priority selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnagPriority.entries.forEach { priority ->
                    PriorityCard(
                        priority = priority,
                        isSelected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trade selector
            Text(
                text = "Trade",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showTradePicker = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, SecondaryText.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Engineering, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    selectedTrade ?: "Select trade (optional)",
                    color = if (selectedTrade != null) PrimaryText else SecondaryText,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Floor selector
            Text(
                text = "Floor",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (currentFloor > FLOOR_MIN) {
                            currentFloor--
                            prefs.edit().putInt(KEY_FLOOR, currentFloor).apply()
                        }
                    },
                    enabled = currentFloor > FLOOR_MIN
                ) {
                    Icon(Icons.Filled.Remove, "Decrease floor", tint = AmberAccent)
                }
                val floorDisplay = when {
                    currentFloor < 0 -> "B${-currentFloor}"
                    currentFloor == 0 -> "G"
                    else -> "F$currentFloor"
                }
                Text(
                    text = floorDisplay,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 60.dp)
                )
                IconButton(
                    onClick = {
                        if (currentFloor < FLOOR_MAX) {
                            currentFloor++
                            prefs.edit().putInt(KEY_FLOOR, currentFloor).apply()
                        }
                    },
                    enabled = currentFloor < FLOOR_MAX
                ) {
                    Icon(Icons.Filled.Add, "Increase floor", tint = AmberAccent)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create button
            Button(
                onClick = {
                    val snag = Snag(
                        title = title.trim(),
                        description = description.trim(),
                        priority = selectedPriority,
                        trade = selectedTrade,
                        floor = currentFloor,
                        createdBy = displayName
                    )
                    val wireMessage = formatSnagMessage(snag)
                    onCreateSnag(snag, wireMessage)
                    Toast.makeText(context, "Snag reported", Toast.LENGTH_SHORT).show()
                },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    disabledContainerColor = SecondaryText.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "REPORT SNAG",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", fontFamily = FontFamily.Monospace, color = SecondaryText)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTradePicker) {
        TradePickerSheet(
            currentTrade = selectedTrade,
            onTradeSelected = { trade ->
                selectedTrade = trade
                showTradePicker = false
            },
            onDismiss = { showTradePicker = false }
        )
    }
}

@Composable
private fun PriorityCard(
    priority: SnagPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) priority.color else Color.Transparent
    val bgColor = if (isSelected) priority.color.copy(alpha = 0.10f) else CardBg

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = priority.label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (isSelected) priority.color else PrimaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
