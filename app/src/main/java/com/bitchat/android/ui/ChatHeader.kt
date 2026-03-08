package com.bitchat.android.ui


import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import com.bitchat.android.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.core.ui.utils.singleOrTripleClickable
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Header components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */




@Composable
fun TorStatusDot(
    modifier: Modifier = Modifier
) {
    val torProvider = remember { com.bitchat.android.net.ArtiTorManager.getInstance() }
    val torStatus by torProvider.statusFlow.collectAsState()
    
    if (torStatus.mode != com.bitchat.android.net.TorMode.OFF) {
        val dotColor = when {
            torStatus.running && torStatus.bootstrapPercent < 100 -> Color(0xFFFF9500) // Orange - bootstrapping
            torStatus.running && torStatus.bootstrapPercent >= 100 -> Color(0xFF00C851) // Green - connected
            else -> Color.Red // Red - error/disconnected
        }
        Canvas(
            modifier = modifier
        ) {
            val radius = size.minDimension / 2
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}

@Composable
fun NoiseSessionIcon(
    sessionState: String?,
    modifier: Modifier = Modifier
) {
    val (icon, color, contentDescription) = when (sessionState) {
        "uninitialized" -> Triple(
            Icons.Outlined.NoEncryption,
            Color(0x87878700), // Grey - ready to establish
            stringResource(R.string.cd_ready_for_handshake)
        )
        "handshaking" -> Triple(
            Icons.Outlined.Sync,
            Color(0x87878700), // Grey - in progress
            stringResource(R.string.cd_handshake_in_progress)
        )
        "established" -> Triple(
            Icons.Filled.Lock,
            Color(0xFFFF9500), // Orange - secure
            stringResource(R.string.cd_encrypted)
        )
        else -> { // "failed" or any other state
            Triple(
                Icons.Outlined.Warning,
                Color(0xFFFF4444), // Red - error
                stringResource(R.string.cd_handshake_failed)
            )
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun NicknameEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    // Auto-scroll to end when text changes (simulates cursor following)
    LaunchedEffect(value) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.at_symbol),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary.copy(alpha = 0.8f)
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colorScheme.primary,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { 
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .widthIn(max = 120.dp)
                .horizontalScroll(scrollState)
        )
    }
}

@Composable
fun PeerCounter(
    connectedPeers: List<String>,
    joinedChannels: Set<String>,
    hasUnreadChannels: Map<String, Int>,
    isConnected: Boolean,
    selectedLocationChannel: com.bitchat.android.geohash.ChannelID?,
    geohashPeople: List<GeoPerson>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Compute channel-aware people count and color (matches iOS logic exactly)
    val (peopleCount, countColor) = when (selectedLocationChannel) {
        is com.bitchat.android.geohash.ChannelID.Location -> {
            // Geohash channel: show geohash participants
            val count = geohashPeople.size
            val green = Color(0xFFE8960C) // Amber for active
            Pair(count, if (count > 0) green else Color.Gray)
        }
        is com.bitchat.android.geohash.ChannelID.Mesh,
        null -> {
            // Mesh channel: show Bluetooth-connected peers (excluding self)
            val count = connectedPeers.size
            val meshBlue = Color(0xFFE8960C) // Amber for site channel
            Pair(count, if (isConnected && count > 0) meshBlue else Color.Gray)
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onClick() }.padding(end = 8.dp) // Added right margin to match "bitchat" logo spacing
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = when (selectedLocationChannel) {
                is com.bitchat.android.geohash.ChannelID.Location -> stringResource(R.string.cd_geohash_participants)
                else -> stringResource(R.string.cd_connected_peers)
            },
            modifier = Modifier.size(16.dp),
            tint = countColor
        )
        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$peopleCount",
            style = MaterialTheme.typography.bodyMedium,
            color = countColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        if (joinedChannels.isNotEmpty()) {
            Text(
                text = stringResource(R.string.channel_count_prefix) + "${joinedChannels.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) Color(0xFFE8960C) else Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChatHeaderContent(
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onTripleClick: () -> Unit,
    onShowAppInfo: () -> Unit,
    onLocationChannelsClick: () -> Unit,
    onLocationNotesClick: () -> Unit,
    onSiteAlertClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onChannelSidebarClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    when {
        currentChannel != null -> {
            // Channel header
            ChannelHeader(
                channel = currentChannel,
                onBackClick = onBackClick,
                onLeaveChannel = { viewModel.leaveChannel(currentChannel) },
                onSidebarClick = onSidebarClick,
                onSearchClick = onSearchClick
            )
        }
        else -> {
            // Main header
            MainHeader(
                nickname = nickname,
                onNicknameChange = viewModel::setNickname,
                onTitleClick = onShowAppInfo,
                onTripleTitleClick = onTripleClick,
                onSidebarClick = onSidebarClick,
                onLocationChannelsClick = onLocationChannelsClick,
                onLocationNotesClick = onLocationNotesClick,
                onSiteAlertClick = onSiteAlertClick,
                onSearchClick = onSearchClick,
                onChannelSidebarClick = onChannelSidebarClick,
                viewModel = viewModel
            )
        }
    }
}



@Composable
private fun ChannelHeader(
    channel: String,
    onBackClick: () -> Unit,
    onLeaveChannel: () -> Unit,
    onSidebarClick: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open channels",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSidebarClick() },
                tint = colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Channel name + subtitle
            Column(
                modifier = Modifier.clickable { onSidebarClick() }
            ) {
                Text(
                    text = "#$channel",
                    color = Color(0xFFE8960C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Channel",
                    color = Color(0xFF8A8E96),
                    fontSize = 12.sp
                )
            }
        }

        // Right section: search icon only
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search messages",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp)
                .clickable { onSearchClick() },
            tint = colorScheme.primary
        )
    }
}

@Composable
private fun MainHeader(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onTitleClick: () -> Unit,
    onTripleTitleClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onLocationChannelsClick: () -> Unit,
    onLocationNotesClick: () -> Unit,
    onSiteAlertClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onChannelSidebarClick: () -> Unit = {},
    viewModel: ChatViewModel
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open channels",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onChannelSidebarClick() },
                tint = colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Channel name + subtitle
            Column(
                modifier = Modifier.clickable { onChannelSidebarClick() }
            ) {
                Text(
                    text = "#general",
                    color = Color(0xFFE8960C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "General discussion",
                    color = Color(0xFF8A8E96),
                    fontSize = 12.sp
                )
            }
        }

        // Right section: search icon only
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search messages",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp)
                .clickable { onSearchClick() },
            tint = colorScheme.primary
        )
    }
}

@Composable
private fun LocationChannelsButton(
    viewModel: ChatViewModel,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Get current channel selection from location manager
    val selectedChannel by viewModel.selectedLocationChannel.collectAsStateWithLifecycle()
    val teleported by viewModel.isTeleported.collectAsStateWithLifecycle()
    
    val (badgeText, badgeColor) = when (selectedChannel) {
        is com.bitchat.android.geohash.ChannelID.Mesh -> {
            "#site" to Color(0xFFE8960C) // Amber for site channel
        }
        is com.bitchat.android.geohash.ChannelID.Location -> {
            val geohash = (selectedChannel as com.bitchat.android.geohash.ChannelID.Location).channel.geohash
            "#$geohash" to Color(0xFFE8960C) // Amber for location
        }
        null -> "#site" to Color(0xFFE8960C) // Default to site
    }
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = badgeColor
        ),
        contentPadding = PaddingValues(start = 4.dp, end = 0.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = badgeColor,
                maxLines = 1
            )
            
            // Teleportation indicator (like iOS)
            if (teleported) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = stringResource(R.string.cd_teleported),
                    modifier = Modifier.size(12.dp),
                    tint = badgeColor
                )
            }
        }
    }
}
