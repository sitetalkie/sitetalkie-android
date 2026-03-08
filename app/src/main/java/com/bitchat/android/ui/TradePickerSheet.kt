package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet

private val CardBackground = Color(0xFF1A1C20)
private val CardBorder = Color(0xFF2A2C30)
private val AmberAccent = Color(0xFFE8960C)
private val RedAccent = Color(0xFFE5484D)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)

private val TRADES = listOf(
    "Electrician",
    "Plumber",
    "Mechanical",
    "General Contractor",
    "Site Manager",
    "Architect",
    "Quantity Surveyor",
    "Structural Engineer",
    "MEP Engineer",
    "Health & Safety",
    "Labourer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradePickerSheet(
    currentTrade: String?,
    onTradeSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var customTrade by remember { mutableStateOf("") }
    val isCustom = currentTrade != null && currentTrade !in TRADES

    BitchatBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Select your trade",
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trade list card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
            ) {
                TRADES.forEachIndexed { index, trade ->
                    TradeRow(
                        trade = trade,
                        isSelected = trade == currentTrade,
                        onClick = { onTradeSelected(trade) }
                    )
                    if (index < TRADES.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = CardBorder,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom trade card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF12141A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (customTrade.isEmpty() && !isCustom) {
                            Text(
                                text = "Enter custom trade",
                                color = SecondaryText,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = if (isCustom && customTrade.isEmpty()) currentTrade ?: "" else customTrade,
                            onValueChange = { customTrade = it },
                            textStyle = TextStyle(
                                color = PrimaryText,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(AmberAccent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (customTrade.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { onTradeSelected(customTrade.trim()) }
                        ) {
                            Text("Save", color = AmberAccent, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clear trade option
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .clickable { onTradeSelected(null) }
            ) {
                Text(
                    text = "Clear Trade",
                    color = RedAccent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TradeRow(
    trade: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = trade,
            color = PrimaryText,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AmberAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
