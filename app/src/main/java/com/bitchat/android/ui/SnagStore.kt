package com.bitchat.android.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SnagStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "sitetalkie_prefs"
        private const val KEY_SNAGS = "sitetalkie.snags"
        private const val KEY_VIEWED = "sitetalkie.snags.viewed"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _snags = MutableStateFlow<List<Snag>>(emptyList())
    val snags: StateFlow<List<Snag>> = _snags.asStateFlow()

    private val viewedSnagIds = mutableSetOf<String>()

    init {
        loadSnags()
        loadViewed()
    }

    fun addSnag(snag: Snag) {
        val current = _snags.value.toMutableList()
        if (current.any { it.id == snag.id }) return
        current.add(snag)
        _snags.value = current
        saveSnags()
    }

    fun addFromMesh(snag: Snag) {
        addSnag(snag)
    }

    fun updateStatus(id: String, status: SnagStatus) {
        val current = _snags.value.map { snag ->
            if (snag.id == id) snag.copy(status = status) else snag
        }
        _snags.value = current
        saveSnags()
    }

    fun deleteSnag(id: String) {
        val current = _snags.value.toMutableList()
        current.removeAll { it.id == id }
        _snags.value = current
        saveSnags()
    }

    fun markViewed(id: String) {
        viewedSnagIds.add(id)
        saveViewed()
    }

    fun isViewed(id: String): Boolean = id in viewedSnagIds

    fun unviewedCount(): Int = _snags.value.count { it.id !in viewedSnagIds && it.status != SnagStatus.RESOLVED }

    private fun loadSnags() {
        val json = prefs.getString(KEY_SNAGS, null) ?: return
        try {
            val type = object : TypeToken<List<SnagJson>>() {}.type
            val list: List<SnagJson> = gson.fromJson(json, type)
            _snags.value = list.map { it.toSnag() }
        } catch (e: Exception) {
            android.util.Log.e("SnagStore", "Failed to load snags: ${e.message}")
        }
    }

    private fun saveSnags() {
        val jsonList = _snags.value.map { SnagJson.from(it) }
        val json = gson.toJson(jsonList)
        prefs.edit().putString(KEY_SNAGS, json).apply()
    }

    private fun loadViewed() {
        val set = prefs.getStringSet(KEY_VIEWED, emptySet()) ?: emptySet()
        viewedSnagIds.addAll(set)
    }

    private fun saveViewed() {
        prefs.edit().putStringSet(KEY_VIEWED, viewedSnagIds.toSet()).apply()
    }
}

/** Gson-compatible intermediary for Snag */
private data class SnagJson(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val trade: String?,
    val floor: Int,
    val createdBy: String,
    val createdAt: Long,
    val status: String,
    val hasPhoto: Boolean,
    val photoData: String?
) {
    fun toSnag() = Snag(
        id = id,
        title = title,
        description = description,
        priority = SnagPriority.fromProtocolName(priority),
        trade = trade,
        floor = floor,
        createdBy = createdBy,
        createdAt = createdAt,
        status = SnagStatus.fromProtocolName(status),
        hasPhoto = hasPhoto,
        photoData = photoData
    )

    companion object {
        fun from(snag: Snag) = SnagJson(
            id = snag.id,
            title = snag.title,
            description = snag.description,
            priority = snag.priority.protocolName,
            trade = snag.trade,
            floor = snag.floor,
            createdBy = snag.createdBy,
            createdAt = snag.createdAt,
            status = snag.status.protocolName,
            hasPhoto = snag.hasPhoto,
            photoData = snag.photoData
        )
    }
}
