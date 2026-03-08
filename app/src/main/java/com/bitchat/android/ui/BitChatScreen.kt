package com.bitchat.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.model.BitchatMessage
import java.text.SimpleDateFormat
import java.util.*

private val TerminalGreen = Color(0xFF00FF00)
private val TerminalBg = Color(0xFF000000)
private val AmberAccent = Color(0xFFE8960C)
private val CardBg = Color(0xFF1A1C20)

private const val BITCHAT_PREFS = "sitetalkie_prefs"
private const val KEY_BITCHAT_MODE = "sitetalkie.bitchatMode"

fun isBitChatMode(context: Context): Boolean {
    return context.getSharedPreferences(BITCHAT_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_BITCHAT_MODE, false)
}

fun setBitChatMode(context: Context, enabled: Boolean) {
    context.getSharedPreferences(BITCHAT_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_BITCHAT_MODE, enabled).apply()
}

@Composable
fun BitChatScreen(
    viewModel: ChatViewModel,
    onReturnToSiteTalkie: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "bitchat",
                color = TerminalGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${connectedPeers.size} peers",
                color = TerminalGreen.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Return to SiteTalkie button
        Text(
            text = "Return to SiteTalkie",
            color = AmberAccent,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable { showReturnDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Message list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            val reversed = messages.asReversed()
            items(reversed, key = { it.id }) { message ->
                BitChatMessageRow(message, timeFormatter)
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                textStyle = TextStyle(
                    color = TerminalGreen,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(TerminalGreen),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, TerminalGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(TerminalBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "message...",
                                color = TerminalGreen.copy(alpha = 0.3f),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ">",
                color = TerminalGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Footer
        Text(
            text = "end-to-end encrypted",
            color = TerminalGreen.copy(alpha = 0.3f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .navigationBarsPadding()
        )
    }

    // Return confirmation dialog
    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            containerColor = CardBg,
            title = {
                Text(
                    text = "Return to SiteTalkie?",
                    color = Color(0xFFF0F0F0),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReturnDialog = false
                    onReturnToSiteTalkie()
                }) {
                    Text("Return", color = AmberAccent, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text(
                        "Stay on BitChat",
                        color = Color(0xFF8A8E96),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }
}

@Composable
private fun BitChatMessageRow(message: BitchatMessage, timeFormatter: SimpleDateFormat) {
    // Strip [CH:tag] prefix if present, and [SITE_ALERT: messages
    val displayContent = remember(message.content) {
        var text = message.content
        if (text.startsWith("[CH:")) {
            val closeBracket = text.indexOf(']')
            if (closeBracket > 0) {
                text = text.substring(closeBracket + 1).trimStart()
            }
        }
        if (text.startsWith("[SITE_ALERT:")) return@remember null
        text
    } ?: return

    val timeStr = timeFormatter.format(message.timestamp)
    Text(
        text = "[$timeStr] <${message.sender}> $displayContent",
        color = TerminalGreen,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace
    )
}
