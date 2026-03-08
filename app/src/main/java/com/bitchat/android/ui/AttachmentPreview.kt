package com.bitchat.android.ui

/**
 * Represents a pending attachment staged for preview before sending.
 * Only one attachment can be pending at a time.
 */
sealed class PendingAttachment {
    /** Photo ready to send — filePath points to downscaled image in app files */
    data class Photo(val filePath: String) : PendingAttachment()

    /** Voice note recording ready to send */
    data class VoiceNote(val filePath: String, val durationMs: Long) : PendingAttachment()

    /** Document/file ready to send */
    data class Document(
        val filePath: String,
        val fileName: String,
        val fileSize: Long
    ) : PendingAttachment()
}
