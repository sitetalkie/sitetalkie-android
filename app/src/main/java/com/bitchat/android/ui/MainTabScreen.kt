package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    CHAT("Chat", Icons.Default.ChatBubble),
    PEOPLE("People", Icons.Default.People),
    SITE("Site", Icons.Default.LocationOn),
    SETTINGS("Settings", Icons.Default.Settings)
}

private val TabBarBackground = Color(0xFF1A1C20)
private val TabBarBorder = Color(0xFF2A2C30)
private val ActiveTabColor = Color(0xFFE8960C)
private val InactiveTabColor = Color(0xFF5A5E66)
private val ActivePillColor = Color(0x1AE8960C)
private val SOSRed = Color(0xFFE5484D)

@Composable
fun MainTabScreen(viewModel: ChatViewModel, onSwitchToBitChat: (() -> Unit)? = null) {
    var selectedTab by remember { mutableStateOf(MainTab.CHAT) }
    val activeSiteAlert by viewModel.activeSiteAlert.collectAsStateWithLifecycle()
    val handbookScenarioId by viewModel.handbookScenarioId.collectAsStateWithLifecycle()
    var showSOSComposer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Content area
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    MainTab.CHAT -> ChatScreen(viewModel = viewModel)
                    MainTab.PEOPLE -> PeopleScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { selectedTab = MainTab.CHAT }
                    )
                    MainTab.SITE -> SiteScreen(viewModel = viewModel)
                    MainTab.SETTINGS -> SettingsScreen(onSwitchToBitChat = onSwitchToBitChat)
                }
            }

            // Bottom tab bar
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onSOSTapped = { showSOSComposer = true }
            )
        }

        // SOS Composer Sheet
        if (showSOSComposer) {
            SiteAlertComposerSheet(
                isPresented = true,
                onDismiss = { showSOSComposer = false },
                onSendAlert = { formattedMessage ->
                    viewModel.sendMessage(formattedMessage)
                    showSOSComposer = false
                },
                onOpenProtocol = { scenarioId ->
                    viewModel.openHandbookScenario(scenarioId)
                }
            )
        }

        // Site Alert Overlay — shown on top of ALL tabs regardless of which is active
        val currentAlert = activeSiteAlert
        if (currentAlert != null) {
            SiteAlertOverlay(
                alert = currentAlert,
                onDismiss = { viewModel.dismissSiteAlert() },
                onOpenProtocol = { scenarioId ->
                    viewModel.dismissSiteAlert()
                    viewModel.openHandbookScenario(scenarioId)
                }
            )
        }

        // Emergency Handbook deep link — full-screen overlay
        val currentScenarioId = handbookScenarioId
        if (currentScenarioId != null) {
            val siteAddress = SiteDataStore.siteConfig?.siteAddress ?: ""
            val scenario = remember(currentScenarioId, siteAddress) {
                allScenarios(siteAddress).find { it.id == currentScenarioId }
            }
            if (scenario != null) {
                EmergencyScenarioScreen(
                    scenario = scenario,
                    onBack = { viewModel.closeHandbookScenario() }
                )
            }
        }
    }
}

@Composable
private fun BottomTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSOSTapped: () -> Unit
) {
    val borderColor = TabBarBorder
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(TabBarBackground)
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat
        TabItem(
            tab = MainTab.CHAT,
            isSelected = selectedTab == MainTab.CHAT,
            onClick = { onTabSelected(MainTab.CHAT) },
            modifier = Modifier.weight(1f)
        )

        // People
        TabItem(
            tab = MainTab.PEOPLE,
            isSelected = selectedTab == MainTab.PEOPLE,
            onClick = { onTabSelected(MainTab.PEOPLE) },
            modifier = Modifier.weight(1f)
        )

        // SOS center button
        SOSTabButton(
            onClick = onSOSTapped,
            modifier = Modifier.weight(1f)
        )

        // Site
        TabItem(
            tab = MainTab.SITE,
            isSelected = selectedTab == MainTab.SITE,
            onClick = { onTabSelected(MainTab.SITE) },
            modifier = Modifier.weight(1f)
        )

        // Settings
        TabItem(
            tab = MainTab.SETTINGS,
            isSelected = selectedTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SOSTabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SOSRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) ActiveTabColor else InactiveTabColor

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) ActivePillColor else Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
