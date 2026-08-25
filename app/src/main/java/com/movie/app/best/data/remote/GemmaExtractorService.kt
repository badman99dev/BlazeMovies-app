package com.movie.app.best.data.remote

import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.GemmaEpisodeInfo
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.GemmaSeasonInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaExtractorService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36"
    private val referer = BuildConfig.GEMMA_BASE_URL
    private val playlistBase = BuildConfig.GEMMA_PLAYLIST_HOST + "/playlist/"

    private val abc = buildAbc()
    private val keyStr = abc + "0123456789+/="

    private fun buildAbc(): String {
        val codes = intArrayOf(65,66,67,68,69,70,71,72,73,74,75,76,77,97,98,99,100,101,102,103,104,105,106,107,108,109,78,79,80,81,82,83,84,85,86,87,88,89,90,110,111,112,113,114,115,116,117,118,119,120,121,122)
        return String(codes.map { it.toChar() }.toCharArray())
    }

    suspend fun extract(imdbId: String): GemmaExtractionResult = withContext(Dispatchers.IO) {
        try {
            val url = BuildConfig.GEMMA_BASE_URL + "play/$imdbId"
            val html = httpGet(url, referer)
                ?: return@withContext GemmaExtractionResult(emptyMap(), "", "Page fetch failed")

            val p3 = extractConfig(html)
                ?: return@withContext GemmaExtractionResult(emptyMap(), "", "No config found")

            val file = p3.optString("file", "")
            val csrfKey = p3.optString("key", "")

            if (file.isEmpty()) return@withContext GemmaExtractionResult(emptyMap(), csrfKey, "No file in config")

            val resolvedFile = if (file.startsWith("/playlist/")) BuildConfig.GEMMA_PLAYLIST_HOST + file else file
            val resp = httpPost(resolvedFile, referer, csrfKey)
                ?: return@withContext GemmaExtractionResult(emptyMap(), csrfKey, "Playlist POST failed")

            val parsed = parseResponse(resp)
                ?: return@withContext GemmaExtractionResult(emptyMap(), csrfKey, "Playlist parse failed")

            val arr = when (parsed) {
                is JSONArray -> parsed
                is String -> if (parsed.contains(".m3u8")) {
                    return@withContext GemmaExtractionResult(
                        mapOf(1 to GemmaSeasonInfo("Season 1", mapOf(1 to GemmaEpisodeInfo("Episode 1", mapOf("Hindi" to parsed.trim()))))),
                        csrfKey
                    )
                } else return@withContext GemmaExtractionResult(emptyMap(), csrfKey, "Unexpected response")
                else -> return@withContext GemmaExtractionResult(emptyMap(), csrfKey, "Unexpected type")
            }

            val seasons = analyzeStructure(arr, csrfKey)
            GemmaExtractionResult(seasons, csrfKey)
        } catch (e: Exception) {
            GemmaExtractionResult(emptyMap(), "", e.message)
        }
    }

    private fun extractConfig(html: String): JSONObject? {
        val patterns = listOf(
            "let\\s+p3\\s*=\\s*(\\{.+?\\})\\s*;".toRegex(RegexOption.DOT_MATCHES_ALL),
            "new\\s+HDVBPlayer\\s*\\(\\s*(\\{.+?\\})\\s*\\)".toRegex(RegexOption.DOT_MATCHES_ALL),
            "var\\s+o_params\\s*=\\s*(\\{.+?\\})\\s*;".toRegex(RegexOption.DOT_MATCHES_ALL),
            "(\\{\"file\"\\s*:.+?\"kp\"\\s*:.+?\\})".toRegex(RegexOption.DOT_MATCHES_ALL)
        )

        for (pat in patterns) {
            val m = pat.find(html)
            if (m != null) {
                try { return JSONObject(m.groupValues[1].replace("\\/", "/")) } catch (_: Exception) {}
            }
        }

        val idx = html.indexOf("{\"file\"")
        if (idx >= 0) {
            val sub = html.substring(idx)
            var depth = 0
            for (i in sub.indices) {
                when (sub[i]) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) {
                        try { return JSONObject(sub.substring(0, i + 1).replace("\\/", "/")) } catch (_: Exception) {}
                        break
                    }}
                }
            }
        }
        return null
    }

    @Suppress("SuspiciousCallableReferenceInLambda")
    private fun analyzeStructure(arr: JSONArray, csrfKey: String): Map<Int, GemmaSeasonInfo> {
        val seasons = mutableMapOf<Int, GemmaSeasonInfo>()
        try {
            if (arr.length() == 0) return seasons
            val first = arr.optJSONObject(0) ?: return seasons

            val hasSeasons = first.has("folder") && !first.has("episode") && first.optString("file", "").isEmpty()
            val hasEpisodes = first.has("episode") || (first.has("folder") && first.optString("file", "").isEmpty())

            if (hasSeasons) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "Season ${i + 1}")
                    val folder = obj.optJSONArray("folder") ?: continue
                    val episodes = parseEpisodeFolder(folder, csrfKey, i + 1)
                    val seasonNo = extractSeasonNo(title, i + 1)
                    seasons[seasonNo] = GemmaSeasonInfo(title, episodes)
                }
            } else if (hasEpisodes) {
                val episodes = parseEpisodeFolder(arr, csrfKey, 1)
                seasons[1] = GemmaSeasonInfo("Season 1", episodes)
            } else {
                val langs = mutableMapOf<String, String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val t = obj.optString("title", "Lang ${i + 1}")
                    val f = obj.optString("file", "")
                    if (f.isNotEmpty()) langs[t] = f
                }
                if (langs.isNotEmpty()) {
                    seasons[1] = GemmaSeasonInfo("Season 1", mapOf(1 to GemmaEpisodeInfo("Episode 1", langs)))
                }
            }
        } catch (_: Exception) {}
        return seasons
    }

    private fun parseEpisodeFolder(folder: JSONArray, csrfKey: String, defaultSeason: Int): Map<Int, GemmaEpisodeInfo> {
        val episodes = mutableMapOf<Int, GemmaEpisodeInfo>()
        try {
            for (i in 0 until folder.length()) {
                val item = folder.opt(i)
                if (item is JSONObject) {
                    val ep = item
                    val epTitle = ep.optString("title", "")
                    val epNo = ep.optString("episode", "").toIntOrNull() ?: (i + 1)
                    val epFolder = ep.optJSONArray("folder")

                    if (epFolder != null && epFolder.length() > 0) {
                        val langs = mutableMapOf<String, String>()
                        for (j in 0 until epFolder.length()) {
                            val langItem = epFolder.opt(j)
                            if (langItem is JSONObject) {
                                val langTitle = langItem.optString("title", "Lang ${j + 1}")
                                val langFolder = langItem.optJSONArray("folder")
                                if (langFolder != null && langFolder.length() > 0) {
                                    for (k in 0 until langFolder.length()) {
                                        val subItem = langFolder.opt(k)
                                        if (subItem is JSONObject) {
                                            val subFile = subItem.optString("file", "")
                                            if (subFile.isNotEmpty()) langs[langTitle] = subFile
                                        }
                                    }
                                } else {
                                    val langFile = langItem.optString("file", "")
                                    if (langFile.isNotEmpty()) langs[langTitle] = langFile
                                }
                            }
                        }
                        episodes[epNo] = GemmaEpisodeInfo(epTitle.ifBlank { "Episode $epNo" }, langs)
                    } else {
                        val epFile = ep.optString("file", "")
                        if (epFile.isNotEmpty()) {
                            episodes[epNo] = GemmaEpisodeInfo(epTitle.ifBlank { "Episode $epNo" }, mapOf("Hindi" to epFile))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return episodes
    }

    suspend fun resolveFile(file: String, csrfKey: String): String? = withContext(Dispatchers.IO) {
        try {
            if (file.contains(".m3u8")) return@withContext file.trim()
            if (file.startsWith("~")) {
                val subUrl = playlistBase + file.substring(1) + ".txt"
                val resp = httpPost(subUrl, referer, csrfKey) ?: return@withContext null
                val parsed = parseResponse(resp) ?: return@withContext null
                when (parsed) {
                    is String -> return@withContext parsed.trim()
                    is JSONArray -> {
                        for (i in 0 until parsed.length()) {
                            val item = parsed.opt(i)
                            when (item) {
                                is JSONObject -> {
                                    val f = item.optString("file", "")
                                    if (f.isNotEmpty()) {
                                        val r = resolveFile(f, csrfKey)
                                        if (r != null) return@withContext r
                                    }
                                }
                                is String -> return@withContext item.trim()
                            }
                        }
                    }
                }
                return@withContext null
            }
            if (file.startsWith("http") && file.contains(".txt")) {
                val resp = httpPost(file, referer, csrfKey) ?: return@withContext null
                val parsed = parseResponse(resp) ?: return@withContext null
                if (parsed is String) return@withContext parsed.trim()
            }
            null
        } catch (_: Exception) { null }
    }

    private fun parseResponse(resp: String): Any? {
        val trimmed = resp.trim()
        return try {
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("#1") -> {
                    val d = decryptSaltD(decryptPepper(trimmed.substring(2), -1)).trim()
                    when {
                        d.startsWith("[") -> JSONArray(d)
                        d.startsWith("{") -> JSONObject(d)
                        else -> d
                    }
                }
                trimmed.startsWith("#0") -> {
                    val d = decryptSaltD(trimmed.substring(2)).trim()
                    when {
                        d.startsWith("[") -> JSONArray(d)
                        d.startsWith("{") -> JSONObject(d)
                        else -> d
                    }
                }
                trimmed.startsWith("http") && trimmed.contains(".m3u8") -> trimmed
                trimmed.startsWith("http") -> trimmed
                trimmed.startsWith("#EXTM3U") -> trimmed
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun httpGet(url: String, ref: String): String? {
        return try {
            val req = Request.Builder().url(url).get()
                .header("User-Agent", ua).header("Referer", ref)
                .header("Accept", "text/html,application/json,*/*").build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) resp.body!!.string().also { resp.close() }
            else { resp.close(); null }
        } catch (_: Exception) { null }
    }

    private fun httpPost(url: String, ref: String, csrf: String): String? {
        return try {
            val body = "".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val req = Request.Builder().url(url).post(body)
                .header("User-Agent", ua).header("Referer", ref)
                .header("Content-type", "application/x-www-form-urlencoded")
                .header("X-CSRF-TOKEN", csrf).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) resp.body!!.string().also { resp.close() }
            else { resp.close(); null }
        } catch (_: Exception) { null }
    }

    private fun extractSeasonNo(title: String, fallback: Int): Int {
        val m = Pattern.compile("(\\d+)").matcher(title)
        return if (m.find()) m.group(1)?.toIntOrNull() ?: fallback else fallback
    }

    private fun decryptPepper(t: String, eVal: Int): String {
        val plusFixed = t.replace("+", "#").replace("#", "+")
        var shift = 1 * eVal
        if (eVal < 0) shift += abc.length / 2
        val s = abc.substring(2 * shift) + abc.substring(0, 2 * shift)
        return plusFixed.map { c -> val idx = abc.indexOf(c); if (idx >= 0) s[idx] else c }.joinToString("")
    }

    private fun decryptSaltD(t: String): String {
        val clean = t.replace(Regex("[^A-Za-z0-9+/=]"), "")
        val sb = StringBuilder()
        var l = 0
        while (l < clean.length) {
            val i = keyStr.indexOf(clean[l++])
            val s2 = if (l < clean.length) keyStr.indexOf(clean[l++]) else 64
            val n = if (l < clean.length) keyStr.indexOf(clean[l++]) else 64
            val a = if (l < clean.length) keyStr.indexOf(clean[l++]) else 64
            sb.append(((i shl 2) or (s2 shr 4)).toChar())
            if (n != 64) sb.append((((15 and s2) shl 4) or (n shr 2)).toChar())
            if (a != 64) sb.append((((3 and n) shl 6) or a).toChar())
        }
        return saltUd(sb.toString())
    }

    private fun saltUd(t: String): String {
        val e = StringBuilder()
        var o = 0
        while (o < t.length) {
            val i = t[o].code
            when {
                i < 128 -> { e.append(t[o]); o++ }
                i > 191 && i < 224 -> {
                    val s = t[o + 1].code
                    e.append((((31 and i) shl 6) or (63 and s)).toChar()); o += 2
                }
                else -> {
                    val s = t[o + 1].code; val c3 = t[o + 2].code
                    e.append((((15 and i) shl 12) or ((63 and s) shl 6) or (63 and c3)).toChar()); o += 3
                }
            }
        }
        return e.toString()
    }
}
