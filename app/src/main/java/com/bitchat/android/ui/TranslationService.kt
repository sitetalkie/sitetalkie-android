package com.bitchat.android.ui

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object TranslationService {

    private const val PREFS_NAME = "sitetalkie_prefs"
    private const val KEY_LANGUAGE = "sitetalkie.preferredLanguage"
    private const val KEY_AUTO_TRANSLATE = "sitetalkie.autoTranslateChats"

    private var appContext: Context? = null
    private val translatorCache = mutableMapOf<String, Translator>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val preferredLanguage: String
        get() {
            val ctx = appContext ?: return Locale.getDefault().language.takeIf {
                TranslateLanguage.fromLanguageTag(it) != null
            } ?: "en"
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_LANGUAGE, null)
            if (stored != null) return stored
            val deviceLang = Locale.getDefault().language
            return if (TranslateLanguage.fromLanguageTag(deviceLang) != null) deviceLang else "en"
        }

    val isEnglish: Boolean
        get() = preferredLanguage == "en"

    val autoTranslateChats: Boolean
        get() {
            val ctx = appContext ?: return false
            return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_TRANSLATE, false)
        }

    fun setPreferredLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun setAutoTranslateChats(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_TRANSLATE, enabled).apply()
    }

    fun getSupportedLanguages(): List<Pair<String, String>> {
        val priorityCodes = listOf(
            "pl", "pt", "hi", "ar", "es", "it", "fr", "de", "ru", "uk", "tr", "zh", "ro", "bg"
        )

        val allLangs = TranslateLanguage.getAllLanguages()
        val result = mutableListOf<Pair<String, String>>()
        val added = mutableSetOf<String>()

        // Priority languages first
        for (code in priorityCodes) {
            if (code in allLangs) {
                result.add(code to getDisplayName(code))
                added.add(code)
            }
        }

        // Remaining languages sorted by display name
        val rest = allLangs
            .filter { it !in added && it != "en" }
            .map { it to getDisplayName(it) }
            .sortedBy { it.second }
        result.addAll(rest)

        // English at top
        result.add(0, "en" to "English")

        return result
    }

    private fun getDisplayName(code: String): String {
        val locale = Locale(code)
        val native = locale.getDisplayLanguage(locale)
            .replaceFirstChar { it.titlecase(locale) }
        return native
    }

    fun getLanguageDisplayName(code: String): String {
        if (code == "en") return "English"
        return getDisplayName(code)
    }

    private fun getOrCreateTranslator(from: String, to: String): Translator {
        val key = "$from->$to"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(from) ?: TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(to) ?: TranslateLanguage.ENGLISH)
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun translate(text: String, from: String = "en", to: String = preferredLanguage): String? {
        if (from == to || text.isBlank()) return text
        val translator = getOrCreateTranslator(from, to)
        return try {
            // Ensure model is downloaded
            suspendCancellableCoroutine { cont ->
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resume(Unit) }
            }
            // Translate
            suspendCancellableCoroutine { cont ->
                translator.translate(text)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun isModelDownloaded(languageCode: String): Boolean {
        val modelManager = RemoteModelManager.getInstance()
        val model = TranslateRemoteModel.Builder(
            TranslateLanguage.fromLanguageTag(languageCode) ?: return false
        ).build()
        return try {
            suspendCancellableCoroutine { cont ->
                modelManager.isModelDownloaded(model)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(false) }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadModel(languageCode: String): Boolean {
        val mlkitCode = TranslateLanguage.fromLanguageTag(languageCode) ?: return false
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(mlkitCode)
            .build()
        val translator = Translation.getClient(options)
        return try {
            suspendCancellableCoroutine { cont ->
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { cont.resume(false) }
            }
        } catch (_: Exception) {
            false
        }
    }
}
