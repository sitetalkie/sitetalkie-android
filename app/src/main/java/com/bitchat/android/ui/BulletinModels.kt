package com.bitchat.android.ui

import com.google.gson.annotations.SerializedName

data class Bulletin(
    val id: Int,
    val title: String,
    val content: String,
    val priority: String = "normal",
    @SerializedName("requires_ack") val requiresAck: Boolean = false,
    val attachments: List<BulletinAttachment>? = null,
    @SerializedName("scheduled_at") val scheduledAt: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    val status: String = "published",
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    var isRead: Boolean = false,
    var isAcknowledged: Boolean = false,
    var acknowledgedAt: String? = null
)

data class BulletinAttachment(
    val url: String,
    val name: String,
    val type: String
)

data class ParsedBulletin(
    val id: Int,
    val requiresAck: Boolean,
    val title: String,
    val content: String
)

object BulletinMessageParser {
    private val BULLETIN_REGEX = Regex("""\[BULLETIN:(\d+):(ACK|INFO)]\s?(.*)""", RegexOption.DOT_MATCHES_ALL)

    fun parse(text: String): ParsedBulletin? {
        val match = BULLETIN_REGEX.find(text) ?: return null
        val id = match.groupValues[1].toIntOrNull() ?: return null
        val requiresAck = match.groupValues[2] == "ACK"
        val body = match.groupValues[3].trim()

        val lines = body.split("\n", limit = 2)
        val title = lines[0].trim()
        val content = if (lines.size > 1) lines[1].trim() else ""

        if (title.isBlank()) return null
        return ParsedBulletin(id = id, requiresAck = requiresAck, title = title, content = content)
    }
}
