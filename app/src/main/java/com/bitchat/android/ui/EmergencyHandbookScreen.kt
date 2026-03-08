package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBg = Color(0xFF0E1012)
private val CardBg = Color(0xFF1A1C20)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)
private val GreenAccent = Color(0xFF34C759)

@Composable
fun EmergencyHandbookScreen(
    onBack: () -> Unit,
    onScenarioSelected: (ScenarioData) -> Unit
) {
    val siteAddress = SiteDataStore.siteConfig?.siteAddress ?: ""
    val siteName = SiteDataStore.siteConfig?.siteName
    val scenarios = remember(siteAddress) { allScenarios(siteAddress) }
    val needsTranslation = !TranslationService.isEnglish
    val targetLang = TranslationService.preferredLanguage

    // Translation cache: scenarioId -> (translatedTitle, translatedCategory)
    var translatedTitles by remember { mutableStateOf<Map<String, Pair<String, String>>>(emptyMap()) }

    LaunchedEffect(targetLang) {
        if (!needsTranslation) return@LaunchedEffect
        val results = mutableMapOf<String, Pair<String, String>>()
        for (s in scenarios) {
            launch {
                val title = TranslationService.translate(s.title, to = targetLang) ?: s.title
                val category = TranslationService.translate(s.category, to = targetLang) ?: s.category
                results[s.id] = title to category
                translatedTitles = results.toMap()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Emergency Handbook",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (siteName != null) {
                    Text(
                        text = siteName,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(GreenAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Offline \u2014 all protocols available",
                        color = GreenAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Scenario rows
        items(scenarios, key = { it.id }) { scenario ->
            val translated = translatedTitles[scenario.id]
            ScenarioRow(
                scenario = scenario,
                translatedTitle = translated?.first,
                translatedCategory = translated?.second,
                onClick = { onScenarioSelected(scenario) }
            )
        }

        // Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Not a substitute for first aid training",
                    color = TertiaryText,
                    fontSize = 10.sp
                )
                Text(
                    text = "Health and Safety (First Aid) Regulations 1981",
                    color = TertiaryText,
                    fontSize = 10.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ScenarioRow(
    scenario: ScenarioData,
    translatedTitle: String? = null,
    translatedCategory: String? = null,
    onClick: () -> Unit
) {
    val categoryColor = Color(scenario.categoryColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left color bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(52.dp)
                .background(categoryColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = translatedTitle ?: scenario.title,
                color = PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = translatedCategory ?: scenario.category,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier
                .size(18.dp)
                .offset(x = (-8).dp)
        )
    }
}
