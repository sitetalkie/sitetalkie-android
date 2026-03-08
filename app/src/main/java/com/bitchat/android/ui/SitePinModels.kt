package com.bitchat.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

// --- Enums ---

enum class SitePinType(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val protocolName: String,
    val sortOrder: Int
) {
    HAZARD("Hazard", Color(0xFFE5484D), Icons.Filled.Warning, "HAZARD", 0),
    NOTE("Note", Color(0xFF3B82F6), Icons.AutoMirrored.Filled.StickyNote2, "NOTE", 1);

    companion object {
        fun fromProtocolName(name: String): SitePinType? =
            entries.find { it.protocolName == name }
    }
}

enum class PinPrecision(
    val label: String,
    val radiusMetres: Double,
    val protocolName: String
) {
    PRECISE("Precise (20m)", 20.0, "PRECISE"),
    ROOM_WIDE("Room (50m)", 50.0, "ROOM_WIDE"),
    BUILDING_WIDE("Building (200m)", 200.0, "BUILDING_WIDE");

    companion object {
        fun fromProtocolName(name: String): PinPrecision =
            entries.find { it.protocolName == name } ?: PRECISE
    }
}

// --- Data class ---

data class SitePin(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: SitePinType,
    val title: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val floor: Int,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isResolved: Boolean = false,
    val photoUri: String? = null,
    val radius: Double = 20.0,
    val precision: PinPrecision = PinPrecision.PRECISE
)

// --- Gson-compatible intermediary ---

data class SitePinJson(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val floor: Int,
    val createdBy: String,
    val createdAt: Long,
    val expiresAt: Long?,
    val isResolved: Boolean,
    val photoUri: String?,
    val radius: Double,
    val precision: String
) {
    fun toSitePin(): SitePin? {
        val pinType = SitePinType.fromProtocolName(type) ?: return null
        return SitePin(
            id = id,
            type = pinType,
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            floor = floor,
            createdBy = createdBy,
            createdAt = createdAt,
            expiresAt = expiresAt,
            isResolved = isResolved,
            photoUri = photoUri,
            radius = radius,
            precision = PinPrecision.fromProtocolName(precision)
        )
    }

    companion object {
        fun from(pin: SitePin) = SitePinJson(
            id = pin.id,
            type = pin.type.protocolName,
            title = pin.title,
            description = pin.description,
            latitude = pin.latitude,
            longitude = pin.longitude,
            floor = pin.floor,
            createdBy = pin.createdBy,
            createdAt = pin.createdAt,
            expiresAt = pin.expiresAt,
            isResolved = pin.isResolved,
            photoUri = pin.photoUri,
            radius = pin.radius,
            precision = pin.precision.protocolName
        )
    }
}

// --- Wire format: [SITE_PIN:{json}] ---

private val gson = Gson()
private const val SITE_PIN_PREFIX = "[SITE_PIN:"
private const val SITE_PIN_SUFFIX = "]"

fun formatSitePin(pin: SitePin): String {
    val json = gson.toJson(SitePinJson.from(pin))
    return "$SITE_PIN_PREFIX$json$SITE_PIN_SUFFIX"
}

fun parseSitePin(messageContent: String): SitePin? {
    if (!messageContent.startsWith(SITE_PIN_PREFIX)) return null
    val endIndex = messageContent.lastIndexOf(SITE_PIN_SUFFIX)
    if (endIndex <= SITE_PIN_PREFIX.length) return null
    val json = messageContent.substring(SITE_PIN_PREFIX.length, endIndex)
    return try {
        val pinJson = gson.fromJson(json, SitePinJson::class.java)
        pinJson?.toSitePin()
    } catch (e: JsonSyntaxException) {
        null
    }
}
