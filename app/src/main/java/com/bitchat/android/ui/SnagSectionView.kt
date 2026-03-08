package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AmberAccent = Color(0xFFE8960C)
private val ScreenBg = Color(0xFF0E1012)
private val CardBg = Color(0xFF1A1C20)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val DarkText = Color(0xFF5A5E66)
private val GreenAccent = Color(0xFF34C759)

private enum class SnagFilter(val label: String) {
    ALL("All"),
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved")
}

@Composable
fun SnagSectionView(
    snags: List<Snag>,
    myName: String,
    myTrade: String? = null,
    snagStore: SnagStore,
    onReportSnag: () -> Unit,
    onSnagClick: (Snag) -> Unit
) {
    var showMySnags by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(SnagFilter.ALL) }

    val filteredSnags = remember(snags, showMySnags, selectedFilter, myName, myTrade) {
        snags.filter { snag ->
            val ownerMatch = if (showMySnags) {
                snag.createdBy == myName
            } else {
                // Assigned to me: snag trade matches my trade, AND I didn't create it
                !myTrade.isNullOrBlank() &&
                    snag.trade.equals(myTrade, ignoreCase = true) &&
                    snag.createdBy != myName
            }
            val statusMatch = when (selectedFilter) {
                SnagFilter.ALL -> true
                SnagFilter.OPEN -> snag.status == SnagStatus.OPEN
                SnagFilter.IN_PROGRESS -> snag.status == SnagStatus.IN_PROGRESS
                SnagFilter.RESOLVED -> snag.status == SnagStatus.RESOLVED
            }
            ownerMatch && statusMatch
        }.sortedByDescending { it.createdAt }
    }

    if (snags.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReportSnagButton(onClick = onReportSnag)
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = DarkText
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No snags yet",
                    color = PrimaryText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Report a snag to track issues on site",
                    color = SecondaryText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ReportSnagButton(onClick = onReportSnag)
            }

            // Toggle: My Snags / Assigned to Me
            item {
                SnagToggle(
                    showMySnags = showMySnags,
                    onToggle = { showMySnags = it }
                )
            }

            // Status filter pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SnagFilter.entries.forEach { filter ->
                        val count = snags.count { snag ->
                            val ownerMatch = if (showMySnags) {
                                snag.createdBy == myName
                            } else {
                                !myTrade.isNullOrBlank() &&
                                    snag.trade.equals(myTrade, ignoreCase = true) &&
                                    snag.createdBy != myName
                            }
                            val statusMatch = when (filter) {
                                SnagFilter.ALL -> true
                                SnagFilter.OPEN -> snag.status == SnagStatus.OPEN
                                SnagFilter.IN_PROGRESS -> snag.status == SnagStatus.IN_PROGRESS
                                SnagFilter.RESOLVED -> snag.status == SnagStatus.RESOLVED
                            }
                            ownerMatch && statusMatch
                        }
                        SnagFilterPill(
                            label = filter.label,
                            count = count,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }

            if (filteredSnags.isEmpty()) {
                item {
                    Text(
                        "No snags match this filter",
                        color = DarkText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            items(filteredSnags, key = { it.id }) { snag ->
                LaunchedEffect(snag.id) {
                    snagStore.markViewed(snag.id)
                }
                SnagCard(
                    snag = snag,
                    onClick = { onSnagClick(snag) }
                )
            }
        }
    }
}

@Composable
private fun ReportSnagButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = AmberAccent.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Build, null, tint = AmberAccent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Report Snag",
                color = AmberAccent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.Add, null, tint = AmberAccent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SnagToggle(
    showMySnags: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(true to "My Snags", false to "Assigned to Me").forEach { (isMySnags, label) ->
            val isSelected = showMySnags == isMySnags
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clickable { onToggle(isMySnags) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) AmberAccent else CardBg
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else SecondaryText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SnagFilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AmberAccent.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AmberAccent else SecondaryText.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else SecondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count",
                    color = if (isSelected) AmberAccent else DarkText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SnagCard(
    snag: Snag,
    onClick: () -> Unit
) {
    val priorityColor = snag.priority.color
    val statusColor = when (snag.status) {
        SnagStatus.OPEN -> Color(0xFFE5484D)
        SnagStatus.IN_PROGRESS -> AmberAccent
        SnagStatus.RESOLVED -> GreenAccent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable { onClick() }
            .drawBehind {
                drawRect(
                    color = priorityColor,
                    topLeft = Offset.Zero,
                    size = Size(4.dp.toPx(), size.height)
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = CardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = priorityColor
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snag.title,
                        color = PrimaryText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = snag.status.label,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = snag.priority.label,
                        color = priorityColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    snag.trade?.let { trade ->
                        Text(
                            " \u00b7 $trade",
                            color = SecondaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        " \u00b7 ${snag.createdBy} \u00b7 ${formatTimeAgo(snag.createdAt)}",
                        color = DarkText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
