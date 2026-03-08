package com.bitchat.android.ui

import androidx.compose.ui.graphics.Color

enum class SnagPriority(val label: String, val color: Color, val protocolName: String) {
    HIGH("High", Color(0xFFE5484D), "HIGH"),
    MEDIUM("Medium", Color(0xFFE8960C), "MEDIUM"),
    LOW("Low", Color(0xFF3B82F6), "LOW");

    companion object {
        fun fromProtocolName(name: String): SnagPriority =
            entries.find { it.protocolName == name } ?: MEDIUM
    }
}

enum class SnagStatus(val label: String, val protocolName: String) {
    OPEN("Open", "OPEN"),
    IN_PROGRESS("In Progress", "IN_PROGRESS"),
    RESOLVED("Resolved", "RESOLVED");

    companion object {
        fun fromProtocolName(name: String): SnagStatus =
            entries.find { it.protocolName == name } ?: OPEN
    }
}

data class Snag(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: SnagPriority = SnagPriority.MEDIUM,
    val trade: String? = null,
    val floor: Int = 1,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: SnagStatus = SnagStatus.OPEN,
    val hasPhoto: Boolean = false,
    val photoData: String? = null
)

// --- Wire format: [SNAG:uuid:PRIORITY:FLOOR:TRADE:PHOTO] title\ndescription ---

fun formatSnagMessage(snag: Snag): String {
    val trade = snag.trade ?: ""
    val photo = if (snag.hasPhoto) "1" else "0"
    val header = "[SNAG:${snag.id}:${snag.priority.protocolName}:${snag.floor}:$trade:$photo]"
    return if (snag.description.isNotBlank()) {
        "$header ${snag.title}\n${snag.description}"
    } else {
        "$header ${snag.title}"
    }
}

fun parseSnagMessage(content: String, senderName: String): Snag? {
    if (!content.startsWith("[SNAG:")) return null
    val closeBracket = content.indexOf(']')
    if (closeBracket < 0) return null

    val headerContent = content.substring(6, closeBracket) // after "[SNAG:"
    val parts = headerContent.split(":")
    if (parts.size < 5) return null

    val id = parts[0]
    val priority = SnagPriority.fromProtocolName(parts[1])
    val floor = parts[2].toIntOrNull() ?: 1
    val trade = parts[3].ifBlank { null }
    val hasPhoto = parts[4] == "1"

    val body = content.substring(closeBracket + 1).trimStart()
    val lines = body.split("\n", limit = 2)
    val title = lines[0]
    val description = if (lines.size > 1) lines[1] else ""

    if (title.isBlank()) return null

    return Snag(
        id = id,
        title = title,
        description = description,
        priority = priority,
        trade = trade,
        floor = floor,
        createdBy = senderName,
        status = SnagStatus.OPEN,
        hasPhoto = hasPhoto
    )
}
