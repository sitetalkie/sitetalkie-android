package com.bitchat.android.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.model.BitchatMessage
import kotlinx.coroutines.delay

private val AmberAccent = Color(0xFFE8960C)
private val CardBackground = Color(0xFF1A1C20)
private val SecondaryText = Color(0xFF8A8E96)
private val GreenNearby = Color(0xFF34C759)

private const val NEARBY_CUTOFF_MS = 120_000L // 120 seconds

private data class PersonEntry(
    val peerID: String,
    val displayName: String,
    val trade: String?,
    val lastSeen: Long,
    val isNearby: Boolean
)

@Composable
fun PeopleScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: () -> Unit = {}
) {
    val meshService = viewModel.meshService
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val privateChats by viewModel.privateChats.collectAsState()
    val peerNicknames by viewModel.peerNicknames.collectAsState()

    // Periodically refresh peer data (every 5 seconds to keep "nearby" status current)
    var refreshTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            refreshTick++
        }
    }

    // Build peer lists from mesh service data
    val (nearbyPeers, recentPeers) = remember(connectedPeers, privateChats, peerNicknames, refreshTick) {
        buildPeerLists(meshService, privateChats)
    }

    // Scanning state for pull-to-refresh substitute
    var isScanning by remember { mutableStateOf(false) }
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(2000)
            isScanning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header with scan button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "People",
                        color = Color(0xFFF0F0F0),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Scan button as pull-to-refresh substitute
                    Text(
                        text = if (isScanning) "Scanning..." else "Scan",
                        color = if (isScanning) SecondaryText else AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = !isScanning) {
                                isScanning = true
                                try { meshService.sendBroadcastAnnounce() } catch (_: Exception) {}
                            }
                            .padding(8.dp)
                    )
                }
            }

            // NEARBY NOW section
            item {
                SectionHeader("NEARBY NOW")
            }

            if (nearbyPeers.isEmpty()) {
                item {
                    NearbyEmptyState()
                }
            } else {
                items(nearbyPeers, key = { "nearby_${it.peerID}" }) { person ->
                    NearbyPeerCard(
                        person = person,
                        onMessageClick = {
                            // Open the DM sheet (same as MeshPeerListSheet) then switch to Chat tab
                            viewModel.showPrivateChatSheet(person.peerID)
                            onNavigateToChat()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // RECENT section
            if (recentPeers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("RECENT")
                }

                items(recentPeers, key = { "recent_${it.peerID}" }) { person ->
                    RecentPeerCard(
                        person = person,
                        lastMessageTime = getLastMessageTime(privateChats, person.peerID),
                        onClick = {
                            // Open the DM sheet (same as MeshPeerListSheet) then switch to Chat tab
                            viewModel.showPrivateChatSheet(person.peerID)
                            onNavigateToChat()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Invite card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                InviteCard()
            }
        }
    }
}

@Composable
private fun InviteCard() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .clickable {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Join me on SiteTalkie \u2014 offline mesh messaging for construction sites. Download: https://sitetalkie.com/download"
                    )
                }
                context.startActivity(Intent.createChooser(intent, "Invite to SiteTalkie"))
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Invite",
            tint = AmberAccent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Invite to SiteTalkie",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "More phones = stronger mesh",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF5A5E66),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = SecondaryText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun NearbyEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Signal-off icon using Material icon
        Text(
            text = "\u26A0", // warning sign as signal-off placeholder
            fontSize = 48.sp,
            color = Color(0xFF5A5E66)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No one nearby",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Share SiteTalkie with your site",
            color = SecondaryText,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun NearbyPeerCard(
    person: PersonEntry,
    onMessageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeerAvatar(displayName = person.displayName)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = person.trade ?: "No trade set",
                color = SecondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Message",
            color = AmberAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onMessageClick)
                .padding(8.dp)
        )
    }
}

@Composable
private fun RecentPeerCard(
    person: PersonEntry,
    lastMessageTime: Long?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeerAvatar(displayName = person.displayName)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = person.trade ?: "No trade set",
                color = SecondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (person.isNearby) {
            Text(
                text = "Nearby",
                color = GreenNearby,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else if (lastMessageTime != null) {
            Text(
                text = formatRelativeTime(lastMessageTime),
                color = SecondaryText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PeerAvatar(displayName: String) {
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AmberAccent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun buildPeerLists(
    meshService: BluetoothMeshService,
    privateChats: Map<String, List<BitchatMessage>>
): Pair<List<PersonEntry>, List<PersonEntry>> {
    val now = System.currentTimeMillis()
    val myPeerID = meshService.myPeerID
    val allPeers = meshService.getAllPeers()

    // Nearby: peers seen within NEARBY_CUTOFF_MS, exclude self
    val nearbyEntries = allPeers.values
        .filter { it.id != myPeerID && (now - it.lastSeen) < NEARBY_CUTOFF_MS && it.isConnected }
        .sortedByDescending { it.lastSeen }
        .map { peer ->
            PersonEntry(
                peerID = peer.id,
                displayName = stripHashSuffix(peer.nickname),
                trade = peer.trade,
                lastSeen = peer.lastSeen,
                isNearby = true
            )
        }

    val nearbyPeerIDs = nearbyEntries.map { it.peerID }.toSet()

    // Recent: peers we've had DM conversations with, sorted by most recent message, excluding nearby
    val recentEntries = privateChats
        .filter { (peerID, messages) ->
            peerID !in nearbyPeerIDs && peerID != myPeerID && messages.isNotEmpty()
        }
        .mapNotNull { (peerID, messages) ->
            val lastMsg = messages.maxByOrNull { it.timestamp.time } ?: return@mapNotNull null
            val peer = allPeers[peerID]
            val displayName = peer?.nickname ?: messages.firstOrNull()?.let { msg ->
                if (msg.sender != "system" && msg.senderPeerID == peerID) msg.sender
                else msg.recipientNickname
            } ?: peerID.take(8)
            val isCurrentlyNearby = peer != null && (now - peer.lastSeen) < NEARBY_CUTOFF_MS && peer.isConnected

            PersonEntry(
                peerID = peerID,
                displayName = stripHashSuffix(displayName),
                trade = peer?.trade,
                lastSeen = lastMsg.timestamp.time,
                isNearby = isCurrentlyNearby
            )
        }
        .sortedByDescending { it.lastSeen }

    return nearbyEntries to recentEntries
}

private fun getLastMessageTime(
    privateChats: Map<String, List<BitchatMessage>>,
    peerID: String
): Long? {
    return privateChats[peerID]
        ?.maxByOrNull { it.timestamp.time }
        ?.timestamp?.time
}

private fun stripHashSuffix(name: String): String {
    if (name.length < 5) return name
    val suffix = name.takeLast(5)
    if (suffix.startsWith("#") && suffix.drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return name.dropLast(5)
    }
    return name
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
