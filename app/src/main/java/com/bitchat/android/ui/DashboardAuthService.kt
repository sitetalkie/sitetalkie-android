package com.bitchat.android.ui

import android.content.Context
import android.util.Log
import com.bitchat.android.nostr.NostrEvent
import com.bitchat.android.nostr.NostrIdentityBridge
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object DashboardAuthService {
    private const val TAG = "DashboardAuthService"

    /** Pending auth from deep link — observed by SettingsScreen */
    private val _pendingAuth = MutableStateFlow<Pair<String, String>?>(null)
    val pendingAuth: StateFlow<Pair<String, String>?> = _pendingAuth

    fun setPendingAuth(sessionId: String, challenge: String) {
        _pendingAuth.value = sessionId to challenge
    }

    fun clearPendingAuth() {
        _pendingAuth.value = null
    }
    private const val SUPABASE_URL = "https://gwolhiudnwacaqvpgmca.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd3b2xoaXVkbndhY2FxdnBnbWNhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI0ODMwMzUsImV4cCI6MjA4ODA1OTAzNX0.YYWMzTMVOVK_Yn0rVdZkdnPbq32_PzXM3oeq3wwg9SE"

    /**
     * Parse a sitetalkie://auth URL and extract sessionId + challenge.
     * Returns null if the URL doesn't match the expected format.
     */
    fun parseAuthUrl(url: String): Pair<String, String>? {
        if (!url.startsWith("sitetalkie://auth")) return null
        val uri = android.net.Uri.parse(url)
        val session = uri.getQueryParameter("session") ?: return null
        val challenge = uri.getQueryParameter("challenge") ?: return null
        if (session.isBlank() || challenge.isBlank()) return null
        return session to challenge
    }

    /**
     * Sign in to the SiteTalkie Dashboard using NIP-46 authentication.
     *
     * 1. Reads the device's existing Nostr keypair
     * 2. Creates and signs a kind-27235 Nostr event with the challenge
     * 3. POSTs the signed event to the Supabase edge function
     */
    suspend fun signIn(context: Context, sessionId: String, challenge: String): Result<String> {
        return try {
            // 1. Get the device's Nostr identity
            val identity = NostrIdentityBridge.getCurrentNostrIdentity(context)
                ?: return Result.failure(Exception("No Nostr identity available"))

            val privateKeyHex = identity.privateKeyHex
            val publicKeyHex = identity.publicKeyHex
            val npub = identity.npub

            // 2. Create a kind-27235 Nostr event
            val createdAt = (System.currentTimeMillis() / 1000).toInt()
            val event = NostrEvent(
                pubkey = publicKeyHex,
                createdAt = createdAt,
                kind = 27235,
                tags = emptyList(),
                content = challenge
            )

            // 3. Sign the event (calculates id + BIP-340 Schnorr sig)
            val signedEvent = event.sign(privateKeyHex)

            // 4. Build the request body with signedEvent as a nested object
            val body = mapOf(
                "sessionId" to sessionId,
                "challenge" to challenge,
                "npub" to npub,
                "signedEvent" to mapOf(
                    "id" to signedEvent.id,
                    "pubkey" to signedEvent.pubkey,
                    "created_at" to signedEvent.createdAt,
                    "kind" to signedEvent.kind,
                    "tags" to signedEvent.tags,
                    "content" to signedEvent.content,
                    "sig" to (signedEvent.sig ?: "")
                )
            )

            val gson = GsonBuilder().disableHtmlEscaping().create()
            val jsonBody = gson.toJson(body)

            Log.d(TAG, "Posting NIP-46 auth for npub=${npub.take(16)}...")

            // 5. POST to edge function
            val url = URL("$SUPABASE_URL/functions/v1/nip46-verify")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseBody = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            conn.disconnect()

            if (responseCode in 200..299) {
                Log.i(TAG, "Dashboard sign-in successful")
                Result.success(sessionId)
            } else {
                Log.e(TAG, "Dashboard sign-in failed: HTTP $responseCode — $responseBody")
                Result.failure(Exception("HTTP $responseCode: $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dashboard sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
