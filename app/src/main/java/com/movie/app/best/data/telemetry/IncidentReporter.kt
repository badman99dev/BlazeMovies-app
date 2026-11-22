package com.movie.app.best.data.telemetry

import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import com.movie.app.best.data.config.RemoteConfig
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.remote.AppInfoInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single client-side incident to ship to the backend (`POST /analytics/incident`).
 *
 * Generic on purpose — a failed download-link resolve is just one [category].
 * Non-content incidents can leave [linkUrl]/[slug]/[movieId] empty.
 */
data class Incident(
    val category: String,
    val severity: String = "medium",
    val title: String = "",
    val linkUrl: String = "",
    val slug: String = "",
    val movieId: Int = 0,
    val linkId: Int = 0,
    val source: String = "",
    val reason: String = "",
    val httpCode: Int = 0,
    val server: String = "",
    val log: String = "",
    val extra: Map<String, Any?> = emptyMap()
)

/**
 * Fire-and-forget incident reporter.
 *
 * Never blocks or throws into the caller: every report runs on a private IO scope
 * and failures are swallowed (logged only). Device identity (fid / uid / app) is
 * attached automatically so the backend can correlate a failure to a device.
 */
@Singleton
class IncidentReporter @Inject constructor(
    okHttpClient: OkHttpClient
) {

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(AppInfoInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedFid: String? = null

    /** Queue a report. Returns immediately; the network call happens in the background. */
    fun report(incident: Incident) {
        scope.launch {
            runCatching { send(incident) }
                .onFailure { NetworkLogger.logAction("INCIDENT_SEND_ERR", it.message ?: "unknown") }
        }
    }

    /** Firebase Installation id — stable per install. Used as X-Device-Id. */
    private fun fid(): String {
        cachedFid?.let { return it }
        val id = try {
            Tasks.await(FirebaseInstallations.getInstance().id, 10, TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        }
        if (!id.isNullOrBlank()) cachedFid = id
        return id ?: ""
    }

    private fun firebaseToken(): String? =
        FirebaseAuth.getInstance().currentUser?.let {
            try {
                Tasks.await(it.getIdToken(false), 10, TimeUnit.SECONDS)?.token
            } catch (_: Exception) {
                null
            }
        }

    private fun send(incident: Incident) {
        val fid = fid()
        val token = firebaseToken()

        val body = JSONObject().apply {
            put("category", incident.category)
            put("severity", incident.severity)
            if (incident.title.isNotEmpty()) put("title", incident.title)
            if (incident.linkUrl.isNotEmpty()) put("link_url", incident.linkUrl)
            if (incident.slug.isNotEmpty()) put("slug", incident.slug)
            if (incident.movieId > 0) put("movie_id", incident.movieId)
            if (incident.linkId > 0) put("link_id", incident.linkId)
            if (incident.source.isNotEmpty()) put("source", incident.source)
            if (incident.reason.isNotEmpty()) put("reason", incident.reason)
            if (incident.httpCode > 0) put("http_code", incident.httpCode)
            if (incident.server.isNotEmpty()) put("server", incident.server)
            if (incident.log.isNotEmpty()) put("log", incident.log.take(60_000))
            if (fid.isNotEmpty()) put("fid", fid)
            put("device_name", Build.MODEL)
            if (incident.extra.isNotEmpty()) put("extra", JSONObject(incident.extra))
            put("ts", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url(RemoteConfig.apiOrigin() + "/analytics/incident")
            .header("Content-Type", "application/json")
            .apply { if (fid.isNotEmpty()) header("X-Device-Id", fid) }
            .apply { if (!token.isNullOrEmpty()) header("X-Auth-Token", "Bearer $token") }
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            NetworkLogger.logAction(
                "INCIDENT_SENT",
                "${incident.category}/${incident.reason.ifEmpty { "-" }} -> ${resp.code}"
            )
        }
    }
}
