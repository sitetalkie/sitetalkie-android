package com.bitchat.android.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object BulletinStore {
    private val gson = Gson()
    private val _bulletins = MutableStateFlow<List<Bulletin>>(emptyList())
    val bulletins: StateFlow<List<Bulletin>> = _bulletins.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var acknowledgedIds = mutableSetOf<Int>()
    private var readIds = mutableSetOf<Int>()

    fun addFromMesh(parsed: ParsedBulletin) {
        val current = _bulletins.value.toMutableList()
        if (current.any { it.id == parsed.id }) return
        val bulletin = Bulletin(
            id = parsed.id,
            title = parsed.title,
            content = parsed.content,
            requiresAck = parsed.requiresAck,
            priority = "normal",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            isRead = false,
            isAcknowledged = parsed.id in acknowledgedIds
        )
        current.add(0, bulletin)
        _bulletins.value = current
        updateUnreadCount()
    }

    fun mergeFromSupabase(fetched: List<Bulletin>) {
        val current = _bulletins.value.toMutableList()
        for (b in fetched) {
            val idx = current.indexOfFirst { it.id == b.id }
            if (idx >= 0) {
                // Preserve local read/ack state
                current[idx] = b.copy(
                    isRead = current[idx].isRead || b.id in readIds,
                    isAcknowledged = current[idx].isAcknowledged || b.id in acknowledgedIds,
                    acknowledgedAt = current[idx].acknowledgedAt
                )
            } else {
                current.add(b.copy(
                    isRead = b.id in readIds,
                    isAcknowledged = b.id in acknowledgedIds
                ))
            }
        }
        current.sortByDescending { it.createdAt }
        _bulletins.value = current
        updateUnreadCount()
    }

    fun markAsRead(id: Int) {
        readIds.add(id)
        _bulletins.value = _bulletins.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        updateUnreadCount()
    }

    fun markAsAcknowledged(id: Int) {
        acknowledgedIds.add(id)
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _bulletins.value = _bulletins.value.map {
            if (it.id == id) it.copy(isAcknowledged = true, acknowledgedAt = now) else it
        }
    }

    fun save(context: Context) {
        try {
            val json = gson.toJson(_bulletins.value)
            File(context.filesDir, "bulletins.json").writeText(json)
            context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("sitetalkie.acknowledgedBulletins", acknowledgedIds.map { it.toString() }.toSet())
                .putStringSet("sitetalkie.readBulletins", readIds.map { it.toString() }.toSet())
                .apply()
        } catch (_: Exception) {}
    }

    fun load(context: Context) {
        try {
            val prefs = context.getSharedPreferences("sitetalkie_prefs", Context.MODE_PRIVATE)
            acknowledgedIds = (prefs.getStringSet("sitetalkie.acknowledgedBulletins", emptySet()) ?: emptySet())
                .mapNotNull { it.toIntOrNull() }.toMutableSet()
            readIds = (prefs.getStringSet("sitetalkie.readBulletins", emptySet()) ?: emptySet())
                .mapNotNull { it.toIntOrNull() }.toMutableSet()

            val file = File(context.filesDir, "bulletins.json")
            if (file.exists()) {
                val json = file.readText()
                val list: List<Bulletin> = gson.fromJson(json, object : TypeToken<List<Bulletin>>() {}.type)
                    ?: emptyList()
                _bulletins.value = list.map {
                    it.copy(
                        isRead = it.isRead || it.id in readIds,
                        isAcknowledged = it.isAcknowledged || it.id in acknowledgedIds
                    )
                }
            }
        } catch (_: Exception) {}
        updateUnreadCount()
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _bulletins.value.count { !it.isRead }
    }
}
