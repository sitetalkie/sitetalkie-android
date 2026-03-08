package com.bitchat.android.ui


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.R
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.withStyle
import com.bitchat.android.ui.theme.BASE_FONT_SIZE
import com.bitchat.android.features.voice.normalizeAmplitudeSample
import com.bitchat.android.features.voice.AudioWaveformExtractor
import com.bitchat.android.ui.media.RealtimeScrollingWaveform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.bitchat.android.ui.media.ImagePickerButton
import com.bitchat.android.ui.media.FilePickerButton

/**
 * Input components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

/**
 * VisualTransformation that styles slash commands with background and color
 * while preserving cursor positioning and click handling
 */
class SlashCommandVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val slashCommandRegex = Regex("(/\\w+)(?=\\s|$)")
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0

            slashCommandRegex.findAll(text.text).forEach { match ->
                // Add text before the match
                if (match.range.first > lastIndex) {
                    append(text.text.substring(lastIndex, match.range.first))
                }

                // Add the styled slash command
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF00FF7F), // Bright green
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        background = Color(0xFF2D2D2D) // Dark gray background
                    )
                ) {
                    append(match.value)
                }

                lastIndex = match.range.last + 1
            }

            // Add remaining text
            if (lastIndex < text.text.length) {
                append(text.text.substring(lastIndex))
            }
        }

        return TransformedText(
            text = annotatedString,
            offsetMapping = OffsetMapping.Identity
        )
    }
}

/**
 * VisualTransformation that styles mentions with background and color
 * while preserving cursor positioning and click handling
 */
class MentionVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mentionRegex = Regex("@([a-zA-Z0-9_]+)")
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0
            
            mentionRegex.findAll(text.text).forEach { match ->
                // Add text before the match
                if (match.range.first > lastIndex) {
                    append(text.text.substring(lastIndex, match.range.first))
                }
                
                // Add the styled mention
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFFFF9500), // Orange
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(match.value)
                }
                
                lastIndex = match.range.last + 1
            }
            
            // Add remaining text
            if (lastIndex < text.text.length) {
                append(text.text.substring(lastIndex))
            }
        }
        
        return TransformedText(
            text = annotatedString,
            offsetMapping = OffsetMapping.Identity
        )
    }
}

/**
 * VisualTransformation that combines multiple visual transformations
 */
class CombinedVisualTransformation(private val transformations: List<VisualTransformation>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var resultText = text
        
        // Apply each transformation in order
        transformations.forEach { transformation ->
            resultText = transformation.filter(resultText).text
        }
        
        return TransformedText(
            text = resultText,
            offsetMapping = OffsetMapping.Identity
        )
    }
}





@Composable
fun MessageInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onSendVoiceNote: (String?, String?, String) -> Unit,
    onSendImageNote: (String?, String?, String) -> Unit,
    onSendFileNote: (String?, String?, String) -> Unit,
    pendingAttachment: PendingAttachment?,
    onPendingAttachmentChanged: (PendingAttachment?) -> Unit,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    nickname: String,
    showMediaButtons: Boolean,
    onLocationDrop: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isFocused = remember { mutableStateOf(false) }
    val hasText = value.text.isNotBlank()
    val hasPendingAttachment = pendingAttachment != null
    val isSendEnabled = hasText || (hasPendingAttachment && !(pendingAttachment is PendingAttachment.Document && pendingAttachment.fileSize > 1_000_000L))
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var amplitude by remember { mutableStateOf(0) }
    var showPlusMenu by remember { mutableStateOf(false) }

    val latestSelectedPeer = rememberUpdatedState(selectedPrivatePeer)
    val latestChannel = rememberUpdatedState(currentChannel)

    // Animated rotation for + icon
    val plusRotation by animateFloatAsState(
        targetValue = if (showPlusMenu) 45f else 0f,
        label = "plusRotation"
    )

    Column(modifier = modifier) {
        // Expandable + menu row
        AnimatedVisibility(
            visible = showPlusMenu && showMediaButtons && !isRecording,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // File button
                PlusMenuButton(
                    icon = Icons.Default.Description,
                    label = "File",
                    iconTint = Color(0xFF8A8E96),
                    modifier = Modifier.weight(1f),
                    content = {
                        FilePickerButton(
                            onFileReady = { path ->
                                val file = java.io.File(path)
                                onPendingAttachmentChanged(
                                    PendingAttachment.Document(path, file.name, file.length())
                                )
                                showPlusMenu = false
                            }
                        )
                    }
                )

                // Photo button
                PlusMenuButton(
                    icon = Icons.Default.PhotoCamera,
                    label = "Photo",
                    iconTint = Color(0xFF8A8E96),
                    modifier = Modifier.weight(1f),
                    content = {
                        ImagePickerButton(
                            onImageReady = { outPath ->
                                onPendingAttachmentChanged(PendingAttachment.Photo(outPath))
                                showPlusMenu = false
                            }
                        )
                    }
                )

                // Location button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .border(1.dp, Color(0xFF2A2C30), RoundedCornerShape(12.dp))
                        .background(Color(0xFF242628), RoundedCornerShape(12.dp))
                        .clickable {
                            showPlusMenu = false
                            onLocationDrop?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Location",
                            color = Color(0xFF8A8E96),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Main input row: [+] [text field] [mic/send]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // + button (toggle)
            if (showMediaButtons && !isRecording) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1A1C20), CircleShape)
                        .clickable { showPlusMenu = !showPlusMenu },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (showPlusMenu) "Close menu" else "Attachments",
                        tint = Color(0xFFE8960C),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(plusRotation)
                    )
                }
            }

            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1C20), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF2A2C30), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        if (it.text.isNotEmpty()) showPlusMenu = false
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(if (isRecording) Color.Transparent else colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (isSendEnabled) onSend()
                    }),
                    visualTransformation = CombinedVisualTransformation(
                        listOf(SlashCommandVisualTransformation(), MentionVisualTransformation())
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused.value = focusState.isFocused
                        }
                )

                if (value.text.isEmpty() && !isRecording) {
                    Text(
                        text = stringResource(R.string.type_a_message_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RealtimeScrollingWaveform(
                            modifier = Modifier.weight(1f).height(32.dp),
                            amplitudeNorm = normalizeAmplitudeSample(amplitude)
                        )
                        Spacer(Modifier.width(20.dp))
                        val secs = (elapsedMs / 1000).toInt()
                        val mm = secs / 60
                        val ss = secs % 60
                        val maxSecs = 10
                        val maxMm = maxSecs / 60
                        val maxSs = maxSecs % 60
                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", mm, ss, maxMm, maxSs),
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.primary,
                            fontSize = (BASE_FONT_SIZE - 4).sp
                        )
                    }
                }
            }

            // Right side: mic when idle, send when text/attachment
            if (value.text.isEmpty() && !hasPendingAttachment && showMediaButtons) {
                VoiceRecordButton(
                    size = 48.dp,
                    backgroundColor = Color(0xFFE8960C).copy(alpha = 0.75f),
                    onStart = {
                        isRecording = true
                        showPlusMenu = false
                        elapsedMs = 0L
                        if (isFocused.value) {
                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    },
                    onAmplitude = { amp, ms ->
                        amplitude = amp
                        elapsedMs = ms
                    },
                    onFinish = { path ->
                        isRecording = false
                        val recordedDuration = elapsedMs
                        AudioWaveformExtractor.extractAsync(path, sampleCount = 120) { arr ->
                            if (arr != null) {
                                try { com.bitchat.android.features.voice.VoiceWaveformCache.put(path, arr) } catch (_: Exception) {}
                            }
                        }
                        onPendingAttachmentChanged(PendingAttachment.VoiceNote(path, recordedDuration))
                    }
                )
            } else {
                // Send button
                IconButton(
                    onClick = {
                        if (isSendEnabled) {
                            if (hasPendingAttachment) {
                                when (pendingAttachment) {
                                    is PendingAttachment.Photo -> onSendImageNote(
                                        latestSelectedPeer.value, latestChannel.value, pendingAttachment.filePath
                                    )
                                    is PendingAttachment.VoiceNote -> onSendVoiceNote(
                                        latestSelectedPeer.value, latestChannel.value, pendingAttachment.filePath
                                    )
                                    is PendingAttachment.Document -> onSendFileNote(
                                        latestSelectedPeer.value, latestChannel.value, pendingAttachment.filePath
                                    )
                                }
                                onPendingAttachmentChanged(null)
                            }
                            if (hasText) onSend()
                        }
                    },
                    enabled = isSendEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (!isSendEnabled) colorScheme.onSurface.copy(alpha = 0.3f)
                                else Color(0xFFE8960C).copy(alpha = 0.75f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(id = R.string.send_message),
                            modifier = Modifier.size(20.dp),
                            tint = if (!isSendEnabled) colorScheme.onSurface.copy(alpha = 0.5f) else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusMenuButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // The content (FilePickerButton / ImagePickerButton) provides its own clickable
    // We wrap it in a styled card
    Box(
        modifier = modifier
            .height(60.dp)
            .border(1.dp, Color(0xFF2A2C30), RoundedCornerShape(12.dp))
            .background(Color(0xFF242628), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Invisible button overlay that handles the actual picker (hidden visually, still clickable)
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0f }) { content() }
        // Visual label (non-interactive, drawn on top but ignores touches)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color(0xFF8A8E96),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CommandSuggestionsBox(
    suggestions: List<CommandSuggestion>,
    onSuggestionClick: (CommandSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(vertical = 8.dp)
    ) {
        suggestions.forEach { suggestion: CommandSuggestion ->
            CommandSuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
fun CommandSuggestionItem(
    suggestion: CommandSuggestion,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .background(Color.Gray.copy(alpha = 0.1f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show all aliases together
        val allCommands = if (suggestion.aliases.isNotEmpty()) {
            listOf(suggestion.command) + suggestion.aliases
        } else {
            listOf(suggestion.command)
        }

        Text(
            text = allCommands.joinToString(", "),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = colorScheme.primary,
            fontSize = (BASE_FONT_SIZE - 4).sp
        )

        // Show syntax if any
        suggestion.syntax?.let { syntax ->
            Text(
                text = syntax,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = (BASE_FONT_SIZE - 5).sp
            )
        }

        // Show description
        Text(
            text = suggestion.description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = (BASE_FONT_SIZE - 5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MentionSuggestionsBox(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(vertical = 8.dp)
    ) {
        suggestions.forEach { suggestion: String ->
            MentionSuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
fun MentionSuggestionItem(
    suggestion: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .background(Color.Gray.copy(alpha = 0.1f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.mention_suggestion_at, suggestion),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFFFF9500), // Orange like mentions
            fontSize = (BASE_FONT_SIZE - 4).sp
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = stringResource(R.string.mention),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = (BASE_FONT_SIZE - 5).sp
        )
    }
}
