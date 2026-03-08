package com.bitchat.android.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SitePinStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "sitetalkie_prefs"
        private const val KEY_PINS = "sitetalkie.pins"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _pins = MutableStateFlow<List<SitePin>>(emptyList())
    val pins: StateFlow<List<SitePin>> = _pins.asStateFlow()

    init {
        loadPins()
        pruneExpired()
    }

    fun addPin(pin: SitePin) {
        val current = _pins.value.toMutableList()
        // Avoid duplicates by ID
        if (current.any { it.id == pin.id }) return
        current.add(pin)
        _pins.value = current
        savePins()
    }

    fun removePin(id: String) {
        val current = _pins.value.toMutableList()
        current.removeAll { it.id == id }
        _pins.value = current
        savePins()
    }

    fun resolvePin(id: String) {
        val current = _pins.value.map { pin ->
            if (pin.id == id) pin.copy(isResolved = true) else pin
        }
        _pins.value = current
        savePins()
    }

    fun extendPin(id: String, hours: Long) {
        val current = _pins.value.map { pin ->
            if (pin.id == id) {
                val baseTime = pin.expiresAt ?: System.currentTimeMillis()
                val newExpiry = baseTime + hours * 3600_000L
                pin.copy(expiresAt = newExpiry)
            } else pin
        }
        _pins.value = current
        savePins()
    }

    fun pruneExpired() {
        val now = System.currentTimeMillis()
        val current = _pins.value.filter { pin ->
            pin.expiresAt == null || pin.expiresAt > now
        }
        if (current.size != _pins.value.size) {
            _pins.value = current
            savePins()
        }
    }

    private fun loadPins() {
        val json = prefs.getString(KEY_PINS, null) ?: return
        try {
            val type = object : TypeToken<List<SitePinJson>>() {}.type
            val list: List<SitePinJson> = gson.fromJson(json, type)
            _pins.value = list.mapNotNull { it.toSitePin() }
        } catch (e: Exception) {
            android.util.Log.e("SitePinStore", "Failed to load pins: ${e.message}")
        }
    }

    private fun savePins() {
        val jsonList = _pins.value.map { SitePinJson.from(it) }
        val json = gson.toJson(jsonList)
        prefs.edit().putString(KEY_PINS, json).apply()
    }
}
