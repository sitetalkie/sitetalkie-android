package com.bitchat.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

private val ScreenBg = Color(0xFF0E1012)
private val CardBg = Color(0xFF1A1C20)
private val RedAccent = Color(0xFFE5484D)
private val RedLight = Color(0xFFF08080)
private val AmberAccent = Color(0xFFE8960C)
private val GreenAccent = Color(0xFF34C759)
private val PrimaryText = Color(0xFFF0F0F0)
private val SecondaryText = Color(0xFF8A8E96)
private val TertiaryText = Color(0xFF5A5E66)

@Composable
fun EmergencyScenarioScreen(
    scenario: ScenarioData,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val equipment = remember { SiteDataStore.equipment }
    val needsTranslation = !TranslationService.isEnglish
    val targetLang = TranslationService.preferredLanguage

    // Translation cache
    var translatedCall999 by remember { mutableStateOf<String?>(null) }
    var translatedDoNots by remember { mutableStateOf<List<String>?>(null) }
    var translatedSteps by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var translatedEvidence by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    LaunchedEffect(scenario.id, targetLang) {
        if (!needsTranslation) return@LaunchedEffect
        isTranslating = true
        launch {
            translatedCall999 = TranslationService.translate(scenario.call999Script, to = targetLang)
        }
        launch {
            translatedDoNots = scenario.doNots.map { doNot ->
                TranslationService.translate(doNot, to = targetLang) ?: doNot
            }
        }
        launch {
            translatedSteps = scenario.steps.map { step ->
                val title = TranslationService.translate(step.title, to = targetLang) ?: step.title
                val detail = TranslationService.translate(step.detail, to = targetLang) ?: step.detail
                title to detail
            }
        }
        launch {
            translatedEvidence = TranslationService.translate(scenario.evidenceNote, to = targetLang)
        }
        isTranslating = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Back button + title
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
                Text(
                    text = scenario.title,
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Translation indicator
        if (needsTranslation) {
            item {
                Text(
                    text = if (isTranslating) "Translating..." else "Translated to ${TranslationService.getLanguageDisplayName(targetLang)}",
                    color = TertiaryText,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }
        }

        // 1. CALL 999 BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedAccent.copy(alpha = 0.12f))
                    .border(1.dp, RedAccent.copy(alpha = 0.33f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = RedAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Call 999 first",
                            color = RedAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = translatedCall999 ?: scenario.call999Script,
                            color = RedLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. DO NOT SECTION
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedAccent.copy(alpha = 0.12f))
                    .border(1.dp, RedAccent.copy(alpha = 0.33f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "DO NOT",
                        color = RedAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val doNotsList = translatedDoNots ?: scenario.doNots
                    doNotsList.forEach { doNot ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = null,
                                tint = RedAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = doNot,
                                color = RedLight,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. INTERACTIVE ELEMENT
        if (scenario.interactiveType != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (scenario.interactiveType) {
                        InteractiveType.CPR_METRONOME -> CPRMetronomeView()
                        InteractiveType.FLUSH_TIMER -> CountdownTimerView(
                            totalSeconds = 1200,
                            label = "Flush Timer",
                            accentColor = GreenAccent,
                            note = "Do not stop when pain subsides \u2014 full 20 minutes"
                        )
                        InteractiveType.COOLING_TIMER -> CountdownTimerView(
                            totalSeconds = 1200,
                            label = "Cooling Timer",
                            accentColor = AmberAccent,
                            note = "Cool water only \u2014 not ice or iced water"
                        )
                        InteractiveType.ELECTRICAL_BRANCH -> ElectricalBranchView()
                        InteractiveType.COOLING_CHECKLIST -> CoolingChecklistView()
                        InteractiveType.LONE_WORKER_BRANCH -> LoneWorkerBranchView()
                        InteractiveType.SPINAL_GATE -> SpinalGateView()
                    }
                }
            }
        }

        // 4. STEPS
        item {
            Text(
                text = "STEP-BY-STEP RESPONSE",
                color = TertiaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        itemsIndexed(scenario.steps) { index, step ->
            val stepTitle = translatedSteps?.getOrNull(index)?.first ?: step.title
            val stepDetail = translatedSteps?.getOrNull(index)?.second ?: step.detail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBg)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "%02d".format(index + 1),
                        color = AmberAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(24.dp)
                    )
                    Column {
                        Text(
                            text = stepTitle,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stepDetail,
                            color = SecondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // 5. EQUIPMENT SECTION
        if (scenario.equipmentTypes.isNotEmpty()) {
            item {
                Text(
                    text = "EQUIPMENT TRIGGERED",
                    color = TertiaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            scenario.equipmentTypes.forEach { eqType ->
                item(key = "equip_$eqType") {
                    val matched = equipment.filter {
                        it.equipmentType.equals(eqType, ignoreCase = true)
                    }

                    if (matched.isNotEmpty()) {
                        matched.forEach { eq ->
                            EquipmentRow(eq, context)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = eqType.replace("_", " "),
                                color = TertiaryText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. EVIDENCE NOTE
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent.copy(alpha = 0.06f))
                    .border(1.dp, GreenAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = translatedEvidence ?: scenario.evidenceNote,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }

        // 7. SOURCES
        item {
            Text(
                text = "VERIFICATION SOURCES",
                color = TertiaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        scenario.sources.forEachIndexed { index, source ->
            item(key = "source_${scenario.id}_$index") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = source.name,
                            color = AmberAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = source.note,
                            color = SecondaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (index < scenario.sources.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color(0xFF2A2C30)
                        )
                    }
                }
            }
        }

        // 8. DISCLAIMER
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
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Health and Safety (First Aid) Regulations 1981",
                    color = TertiaryText,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EquipmentRow(eq: EquipmentLocation, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Photo thumbnail
        val photoFile = File(context.filesDir, "equipment_photo_${eq.id}.jpg")
        if (photoFile.exists()) {
            val bitmap = remember(eq.id) {
                android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = eq.label,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eq.label,
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!eq.description.isNullOrBlank()) {
                Text(
                    text = eq.description,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!eq.floor.isNullOrBlank()) {
                Text(
                    text = eq.floor,
                    color = TertiaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
