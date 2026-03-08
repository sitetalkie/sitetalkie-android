package com.bitchat.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class SiteAlertType(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val protocolName: String,
    val scenarioId: String? = null
) {
    // Emergency types with scenario IDs
    CARDIAC(
        label = "Cardiac Arrest",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.HeartBroken,
        protocolName = "CARDIAC",
        scenarioId = "cardiac"
    ),
    FALL(
        label = "Fall from Height",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.PersonOff,
        protocolName = "FALL",
        scenarioId = "fall"
    ),
    BLEEDING(
        label = "Severe Bleeding",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.WaterDrop,
        protocolName = "BLEEDING",
        scenarioId = "bleeding"
    ),
    CHEMICAL(
        label = "Chemical Splash",
        color = Color(0xFFE8960C),
        icon = Icons.Filled.Science,
        protocolName = "CHEMICAL",
        scenarioId = "chemical"
    ),
    ELECTRICAL(
        label = "Electrical Contact",
        color = Color(0xFFE8960C),
        icon = Icons.Filled.Bolt,
        protocolName = "ELECTRICAL",
        scenarioId = "electrical"
    ),
    CRUSH(
        label = "Crush Injury",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.Compress,
        protocolName = "CRUSH",
        scenarioId = "crush"
    ),
    BURNS(
        label = "Burns",
        color = Color(0xFFE8960C),
        icon = Icons.Filled.Whatshot,
        protocolName = "BURNS",
        scenarioId = "burns"
    ),
    CONFINED(
        label = "Confined Space",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.Lock,
        protocolName = "CONFINED",
        scenarioId = "confined"
    ),
    BREATHING(
        label = "Breathing Difficulty",
        color = Color(0xFFE8960C),
        icon = Icons.Filled.Air,
        protocolName = "BREATHING",
        scenarioId = "breathing"
    ),
    HEAT(
        label = "Heat Stroke",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.Thermostat,
        protocolName = "HEAT",
        scenarioId = "heat"
    ),
    LONE_WORKER(
        label = "Lone Worker",
        color = Color(0xFF8A8E96),
        icon = Icons.Filled.PersonSearch,
        protocolName = "LONE_WORKER",
        scenarioId = "lone"
    ),

    // Existing general alert types (no scenario ID)
    FIRE(
        label = "Fire / Evacuation",
        color = Color(0xFFE5484D),
        icon = Icons.Filled.LocalFireDepartment,
        protocolName = "FIRE"
    ),
    WARNING(
        label = "General Warning",
        color = Color(0xFFE8960C),
        icon = Icons.Filled.Warning,
        protocolName = "WARNING"
    ),
    MEDICAL(
        label = "Medical Emergency",
        color = Color(0xFF3B82F6),
        icon = Icons.Filled.LocalHospital,
        protocolName = "MEDICAL"
    ),
    CRANE(
        label = "Crane / Lifting Op",
        color = Color(0xFFEAB308),
        icon = Icons.Filled.SwapVert,
        protocolName = "CRANE"
    ),
    GENERAL(
        label = "General",
        color = Color(0xFFA1A1AA),
        icon = Icons.Filled.Campaign,
        protocolName = "GENERAL"
    ),
    ALL_CLEAR(
        label = "All Clear",
        color = Color(0xFF34C759),
        icon = Icons.Filled.VerifiedUser,
        protocolName = "ALL_CLEAR"
    );

    companion object {
        fun fromProtocolName(name: String): SiteAlertType? =
            entries.find { it.protocolName == name }
    }
}

data class SiteAlert(
    val type: SiteAlertType,
    val floor: String,
    val detail: String,
    val senderName: String = ""
)

private val SITE_ALERT_REGEX = Regex("""\[SITE_ALERT:(\w+):(F-?\d+|B\d+)]\s?(.*)""")

fun parseSiteAlert(messageContent: String): SiteAlert? {
    val match = SITE_ALERT_REGEX.find(messageContent) ?: return null
    val typeName = match.groupValues[1]
    val floor = match.groupValues[2]
    val detail = match.groupValues[3].trim()
    val type = SiteAlertType.fromProtocolName(typeName) ?: return null
    return SiteAlert(type = type, floor = floor, detail = detail)
}

fun formatSiteAlert(type: SiteAlertType, floorNumber: Int, detail: String): String {
    val floorStr = if (floorNumber < 0) "B${-floorNumber}" else "F$floorNumber"
    val detailPart = if (detail.isNotBlank()) " $detail" else ""
    return "[SITE_ALERT:${type.protocolName}:$floorStr]$detailPart"
}

fun formatFloorDisplay(floor: String): String {
    return when {
        floor.startsWith("B") -> {
            val num = floor.removePrefix("B").toIntOrNull() ?: 0
            "Basement $num"
        }
        floor.startsWith("F") -> {
            val num = floor.removePrefix("F").toIntOrNull() ?: 0
            if (num == 0) "Ground Floor" else "Floor $num"
        }
        else -> floor
    }
}
