package com.movie.app.best.data.repository

import android.content.Context
import com.google.gson.Gson
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.PromoAd
import com.movie.app.best.data.remote.MovieApiService
import com.movie.app.best.util.DeviceFacts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-scheduled home promo/popup.
 *
 * The server owns scheduling: [PromoAd.displayAfter] is the earliest moment the ad may
 * appear and [PromoAd.displayBefore] is its expiry. The app never counts repeats — it
 * simply downloads the payload + images, keeps them locally, and checks on every app
 * open whether the window is open yet. A `null` response (or an expired window) wipes
 * the cache. A different dialog id replaces the cache; the same id keeps the images.
 *
 * Nothing is written to the gallery — files live in app-internal storage.
 */
@Singleton
class PromoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: MovieApiService
) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val http = OkHttpClient()

    private val _current = MutableStateFlow<PromoAd?>(null)

    /** Ad that should be on screen right now (null = nothing to show). */
    val current: StateFlow<PromoAd?> = _current.asStateFlow()

    private var shownDialogId: String? = null

    // ── Called on every app open (and home refresh) ─────────────────────
    suspend fun sync() {
        val cached = readCache()

        // 1. Offline-first: if the cached ad's window is already open, show it now.
        if (cached != null && isEligible(cached)) publish(cached)

        // 2. Ask the server (server is the single source of truth for schedule/targeting).
        val fetched = try {
            val resp = apiService.getHomePromo(
                ver = BuildConfig.VERSION_NAME,
                vcode = BuildConfig.VERSION_CODE,
                soc = DeviceFacts.cpu(context),
                cores = DeviceFacts.cores(),
                ram = DeviceFacts.ramMb(context),
                abi = DeviceFacts.abi(),
                sdk = DeviceFacts.sdk(),
                device = DeviceFacts.device()
            )
            resp.data
        } catch (_: Exception) {
            null
        }

        when {
            fetched == null -> {
                // No ad for this device / cancelled → clear everything.
                clearAll()
            }
            cached != null && cached.dialogId == fetched.dialogId -> {
                // Same ad → keep downloaded images, refresh window + actions.
                val merged = fetched.copy(
                    image = fetched.image?.let { fresh ->
                        cached.image?.takeIf { it.url == fresh.url } ?: fresh
                    },
                    button = fetched.button.copy(
                        image = fetched.button.image?.let { fresh ->
                            cached.button.image?.takeIf { it.url == fresh.url } ?: fresh
                        }
                    )
                )
                writeCache(merged)
                if (shownDialogId != merged.dialogId && isEligible(merged) && isDownloaded(merged)) {
                    // display_after moved (server re-scheduled) → allow it to show again.
                    publish(merged)
                }
            }
            else -> {
                // New ad → download assets, then cache.
                val ready = downloadAssets(fetched)
                writeCache(ready)
                shownDialogId = null
                if (isEligible(ready)) publish(ready)
            }
        }
    }

    fun dismiss(ad: PromoAd) {
        shownDialogId = ad.dialogId
        prefs.edit().putString(KEY_SHOWN_ID, ad.dialogId).apply()
        _current.value = null
    }

    // ── internals ───────────────────────────────────────────────────────

    private fun publish(ad: PromoAd) {
        val alreadyShown = prefs.getString(KEY_SHOWN_ID, null) == ad.dialogId &&
            shownDialogId == ad.dialogId
        if (!alreadyShown) {
            shownDialogId = ad.dialogId
            prefs.edit().putString(KEY_SHOWN_ID, ad.dialogId).apply()
        } else {
            return
        }
        _current.value = ad
    }

    private fun isEligible(ad: PromoAd): Boolean {
        val now = System.currentTimeMillis() / 1000L
        if (ad.displayAfter > 0 && now < ad.displayAfter) return false
        if (ad.displayBefore > 0 && now >= ad.displayBefore) return false
        if (prefs.getString(KEY_SHOWN_ID, null) == ad.dialogId) return false
        return ad.image != null || ad.template.dialog != "image_alpha" ||
            ad.content.title.isNotBlank() || ad.button.enabled
    }

    private fun isDownloaded(ad: PromoAd): Boolean {
        val needed = listOfNotNull(ad.image?.url, ad.button.image?.url)
        return needed.isEmpty() || needed.all { localFile(it).exists() }
    }

    private fun readCache(): PromoAd? {
        val raw = prefs.getString(KEY_AD, null) ?: return null
        return try {
            gson.fromJson(raw, PromoAd::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(ad: PromoAd) {
        prefs.edit().putString(KEY_AD, gson.toJson(ad)).apply()
    }

    /** Downloads the creative into app-internal storage (never the gallery). */
    private suspend fun downloadAssets(ad: PromoAd): PromoAd = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "promo/${ad.dialogId.ifBlank { "ad" }}").apply {
            if (!exists()) mkdirs()
        }
        // Drop files of any previous dialog id — only one slot is kept.
        File(context.filesDir, "promo").listFiles()
            ?.filter { it.isDirectory && it.name != dir.name }
            ?.forEach { it.deleteRecursively() }

        fun fetch(url: String?, isLottie: Boolean = false): String? {
            if (url.isNullOrBlank()) return null
            val ext = when {
                !isLottie -> ".img"
                url.trimEnd('/').endsWith(".lottie", ignoreCase = true) -> ".lottie"
                else -> ".json"
            }
            val file = File(dir, url.hashCode().toString() + ext)
            if (file.exists() && file.length() > 0) return file.absolutePath
            return try {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    resp.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    if (file.length() > 0) file.absolutePath else null
                }
            } catch (_: Exception) {
                null
            }
        }

        ad.copy(
            image = ad.image?.copy(
                url = fetch(ad.image.url, ad.image.isLottie) ?: ad.image.url
            ),
            button = ad.button.copy(
                image = ad.button.image?.copy(
                    url = fetch(ad.button.image.url, ad.button.image.isLottie) ?: ""
                )
            )
        )
    }

    private fun localFile(pathOrUrl: String): File = File(pathOrUrl)

    private fun clearAll() {
        prefs.edit().remove(KEY_AD).remove(KEY_SHOWN_ID).apply()
        shownDialogId = null
        _current.value = null
        File(context.filesDir, "promo").deleteRecursively()
    }

    private companion object {
        const val PREFS = "promo_cache"
        const val KEY_AD = "ad_json"
        const val KEY_SHOWN_ID = "shown_dialog_id"
    }
}
