package com.movie.app.best.data.telemetry

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.movie.app.best.data.config.RemoteConfig

/**
 * Everything a player/watch screen knows at the moment a source is declared dead.
 * The screen only owns the Media3 error + the live [Player]; the ViewModel fills
 * the identity/server fields from its own state.
 */
data class PlayerFailure(
    val imdbId: String = "",
    val slug: String = "",
    val contentTitle: String = "",
    val serverName: String = "",
    val serverKind: String = "",
    val serverService: String = "",
    val url: String = "",
    val serverIndex: Int = -1,
    val totalServers: Int = 0,
    val hasMoreServers: Boolean = false,
    val error: PlaybackException? = null,
    val player: Player? = null
)

/**
 * Builds the generic [Incident] for a failed/unhealthy playback source.
 *
 * Covers both cases the app cares about:
 *   • [hasMoreServers] true  → the server failed and we silently switched to the next
 *     (reason `server_failover`).
 *   • [hasMoreServers] false → single source or last server, nothing left to try
 *     (reason `all_servers_failed`).
 *
 * Ships the actual Media3 error, the server name/url, our backend + stream HTTP
 * traces (url + headers + response), the runtime decoder, and the device profile
 * (the last two are auto-attached by [IncidentReporter]).
 */
object PlayerIncidents {

    fun build(f: PlayerFailure): Incident {
        val hasMore = f.hasMoreServers
        val reason = if (hasMore) "server_failover" else "all_servers_failed"
        val title = if (hasMore) "Player server failed - switched" else "Player failed - no server left"

        val extra = LinkedHashMap<String, Any?>()
        if (f.imdbId.isNotEmpty()) extra["imdb_id"] = f.imdbId
        if (f.contentTitle.isNotEmpty()) extra["content_title"] = f.contentTitle
        extra["server_index"] = f.serverIndex
        extra["total_servers"] = f.totalServers
        extra["has_more_servers"] = hasMore
        extra["server_name"] = f.serverName
        extra["server_kind"] = f.serverKind
        extra["server_service"] = f.serverService
        extra["media_url"] = f.url
        extra["codec"] = CodecTracker.snapshot()
        extra["client_time"] = clientTime()

        f.error?.let { e ->
            extra["error_code"] = e.errorCode
            extra["error_code_name"] = e.errorCodeName
            extra["error_message"] = e.message
            extra["error_cause"] = causeChain(e).take(1000)
        }

        f.player?.let { p ->
            runCatching {
                extra["playback_state"] = p.playbackState
                extra["position_ms"] = p.currentPosition
                extra["duration_ms"] = p.duration
                extra["buffered_ms"] = p.bufferedPosition
                extra["video_w"] = p.videoSize.width
                extra["video_h"] = p.videoSize.height
                extra["is_live"] = p.isCurrentMediaItemLive
                extra["media_item"] = p.currentMediaItem?.localConfiguration?.uri?.toString()
            }
        }

        // Server-side evidence: the failing stream, the last call to our own backend,
        // and the recent request sequence (so a server bug is visible, not just the client).
        NetTraceRecorder.lastFor(f.url)?.let { extra["media_trace"] = traceToMap(it) }
        NetTraceRecorder.lastForPrefix(RemoteConfig.apiOrigin())?.let { extra["backend_trace"] = traceToMap(it) }
        extra["recent_requests"] = NetTraceRecorder.recent(12).map { t ->
            linkedMapOf(
                "method" to t.method,
                "url" to t.url,
                "status" to t.status,
                "took_ms" to t.tookMs,
                "error" to t.error
            )
        }

        val log = buildString {
            append("cat=player reason=").append(reason).append('\n')
            append("server=").append(f.serverName)
                .append(" kind=").append(f.serverKind)
                .append(" service=").append(f.serverService)
                .append(" idx=").append(f.serverIndex).append('/').append(f.totalServers).append('\n')
            append("url=").append(f.url).append('\n')
            f.error?.let {
                append("media3=").append(it.errorCodeName).append(" (").append(it.errorCode).append(") ")
                    .append(it.message ?: "").append('\n')
                append("cause=").append(causeChain(it)).append('\n')
            }
            NetTraceRecorder.lastFor(f.url)?.let {
                append("media_http=").append(it.status).append(" ").append(it.url)
                    .append(if (it.error.isNotEmpty()) " err=${it.error}" else "").append('\n')
            }
        }

        return Incident(
            category = "player",
            severity = if (hasMore) "medium" else "high",
            title = title,
            linkUrl = f.url,
            slug = f.slug,
            server = f.serverName,
            source = "player_watch",
            reason = reason,
            log = log,
            extra = extra
        )
    }

    private fun traceToMap(t: NetTrace): Map<String, Any?> = linkedMapOf(
        "url" to t.url,
        "method" to t.method,
        "status" to t.status,
        "took_ms" to t.tookMs,
        "req_headers" to t.reqHeaders,
        "resp_headers" to t.respHeaders,
        "body" to t.body,
        "error" to t.error
    )

    private fun causeChain(t: Throwable?): String {
        val sb = StringBuilder()
        var c = t
        var depth = 0
        while (c != null && depth < 6) {
            if (depth > 0) sb.append(" <- ")
            sb.append(c.javaClass.simpleName).append(": ").append(c.message ?: "")
            c = c.cause
            depth++
        }
        return sb.toString()
    }

    private fun clientTime(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        return fmt.format(java.util.Date())
    }
}
