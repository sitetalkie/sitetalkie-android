package com.bitchat.android.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL = "https://gwolhiudnwacaqvpgmca.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd3b2xoaXVkbndhY2FxdnBnbWNhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI0ODMwMzUsImV4cCI6MjA4ODA1OTAzNX0.YYWMzTMVOVK_Yn0rVdZkdnPbq32_PzXM3oeq3wwg9SE"

data class SiteConfig(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("site_address") val siteAddress: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class EquipmentLocation(
    val id: Int,
    @SerializedName("equipment_type") val equipmentType: String,
    val label: String,
    val description: String?,
    val floor: String?,
    @SerializedName("nearest_node_id") val nearestNodeId: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("updated_at") val updatedAt: String
)

object SiteDataSyncService {

    private val gson = Gson()

    suspend fun sync(context: Context) {
        try {
            // 1. Fetch site_config
            val configJson = httpGet("$SUPABASE_URL/rest/v1/site_config?select=*&limit=1")
            if (configJson != null) {
                writeFile(context, "site_config.json", configJson)
            }
        } catch (e: Exception) {
        }

        try {
            // 2. Fetch equipment_locations
            val equipJson = httpGet("$SUPABASE_URL/rest/v1/equipment_locations?select=*")
            if (equipJson != null) {
                writeFile(context, "equipment_locations.json", equipJson)
            }
        } catch (e: Exception) {
        }

        // 3. Download equipment photos
        try {
            val equipment = loadEquipmentLocations(context)
            equipment.forEach { equip ->
                val photoUrl = equip.photoUrl
                if (!photoUrl.isNullOrBlank()) {
                    try {
                        downloadFile(photoUrl, File(context.filesDir, "equipment_photo_${equip.id}.jpg"))
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

    }

    fun loadSiteConfig(context: Context): SiteConfig? {
        return try {
            val json = readFile(context, "site_config.json") ?: return null
            val list: List<SiteConfig> = gson.fromJson(json, object : TypeToken<List<SiteConfig>>() {}.type)
            list.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun loadEquipmentLocations(context: Context): List<EquipmentLocation> {
        return try {
            val json = readFile(context, "equipment_locations.json") ?: return emptyList()
            gson.fromJson(json, object : TypeToken<List<EquipmentLocation>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun httpGet(urlString: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        return try {
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(urlString: String, target: File) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        try {
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun writeFile(context: Context, filename: String, content: String) {
        File(context.filesDir, filename).writeText(content)
    }

    private fun readFile(context: Context, filename: String): String? {
        val file = File(context.filesDir, filename)
        return if (file.exists()) file.readText() else null
    }
}
