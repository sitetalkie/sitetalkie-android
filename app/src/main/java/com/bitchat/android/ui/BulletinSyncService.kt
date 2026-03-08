package com.bitchat.android.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL = "https://gwolhiudnwacaqvpgmca.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd3b2xoaXVkbndhY2FxdnBnbWNhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI0ODMwMzUsImV4cCI6MjA4ODA1OTAzNX0.YYWMzTMVOVK_Yn0rVdZkdnPbq32_PzXM3oeq3wwg9SE"

object BulletinSyncService {

    private val gson = Gson()

    suspend fun sync(context: Context) {
        try {
            val json = httpGet("$SUPABASE_URL/rest/v1/bulletins?select=*&status=in.(published,broadcast)&order=created_at.desc")
            if (json != null) {
                val list: List<Bulletin> = gson.fromJson(json, object : TypeToken<List<Bulletin>>() {}.type)
                    ?: emptyList()
                BulletinStore.mergeFromSupabase(list)
                BulletinStore.save(context)

                // Download attachments
                list.forEach { bulletin ->
                    bulletin.attachments?.forEachIndexed { index, attachment ->
                        try {
                            val ext = attachment.name.substringAfterLast('.', "bin")
                            val target = File(context.filesDir, "bulletin_attachment_${bulletin.id}_${index}.$ext")
                            if (!target.exists()) {
                                downloadFile(attachment.url, target)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {

        }
    }

    suspend fun postAcknowledgment(context: Context, bulletinId: Int, senderId: String, displayName: String) {
        try {
            val body = gson.toJson(mapOf(
                "bulletin_id" to bulletinId,
                "sender_id" to senderId,
                "display_name" to displayName
            ))
            httpPost("$SUPABASE_URL/rest/v1/bulletin_acks", body)
        } catch (e: Exception) {

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
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(urlString: String, body: String) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", "return=minimal")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode // trigger the request
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
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
