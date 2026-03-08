package com.bitchat.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SetupBackground = Color(0xFF0E1012)
private val CardBackground = Color(0xFF1A1C20)
private val CardBorder = Color(0xFF2A2C30)
private val AmberAccent = Color(0xFFE8960C)
private val SubtitleColor = Color(0xFF8A8E96)
private val PrimaryText = Color(0xFFF0F0F0)

private val SETUP_TRADES = listOf(
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

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var selectedTrade by remember { mutableStateOf<String?>(null) }
    var customTrade by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(TranslationService.preferredLanguage) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val isValid = displayName.isNotBlank() && displayName.length <= 15

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SetupBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Welcome to SiteTalkie",
            color = PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set your display name so your site knows who you are",
            color = SubtitleColor,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display name input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (displayName.isEmpty()) {
                Text(
                    text = "Your name",
                    color = SubtitleColor,
                    fontSize = 16.sp
                )
            }
            BasicTextField(
                value = displayName,
                onValueChange = { if (it.length <= 15) displayName = it },
                textStyle = TextStyle(
                    color = PrimaryText,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(AmberAccent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trade section
        Text(
            text = "Your trade (optional)",
            color = SubtitleColor,
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
        ) {
            SETUP_TRADES.forEachIndexed { index, trade ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTrade = if (selectedTrade == trade) null else trade
                            customTrade = ""
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trade,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedTrade == trade) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (index < SETUP_TRADES.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = CardBorder,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom trade input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
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
                if (customTrade.isEmpty()) {
                    Text(
                        text = "Enter custom trade",
                        color = SubtitleColor,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = customTrade,
                    onValueChange = {
                        customTrade = it
                        if (it.isNotBlank()) selectedTrade = null
                    },
                    textStyle = TextStyle(
                        color = PrimaryText,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(AmberAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language section
        Text(
            text = "Your language (optional)",
            color = SubtitleColor,
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .clickable { showLanguagePicker = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TranslationService.getLanguageDisplayName(selectedLanguage),
                color = PrimaryText,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedLanguage.uppercase(),
                color = SubtitleColor,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started button
        Button(
            onClick = {
                if (isValid) {
                    // Save name
                    context.getSharedPreferences("bitchat_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("nickname", displayName.trim())
                        .apply()
                    // Save trade if selected
                    val trade = selectedTrade ?: customTrade.trim().takeIf { it.isNotEmpty() }
                    val stPrefs = context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE)
                    if (trade != null) {
                        stPrefs.edit().putString("com.sitetalkie.user.trade", trade).apply()
                    }
                    // Save language
                    TranslationService.setPreferredLanguage(context, selectedLanguage)
                    setSetupCompleted(context)
                    onSetupComplete()
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberAccent,
                contentColor = Color.Black,
                disabledContainerColor = AmberAccent.copy(alpha = 0.3f),
                disabledContentColor = Color.Black.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = "Get Started",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            currentLanguage = selectedLanguage,
            onLanguageSelected = { code ->
                selectedLanguage = code
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }
    }
}
