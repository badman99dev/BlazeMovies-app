package com.movie.app.best.data.telemetry

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One HTTP transaction (request + response) captured for incident context.
 *
 * The app's own backend can also be the culprit (bad manifest url, 5xx, empty
 * body, wrong headers). So when playback/download fails we ship the *server-side*
 * evidence too: the URL called, the headers we sent, and the response itself.
 */
data class NetTrace(
    val url: String,
    val method: String,
    val reqHeaders: Map<String, String>,
    val status: Int,
    val respHeaders: Map<String, String>,
    val body: String,
    val tookMs: Long,
    val error: String
)

/**
 * Always-on ring buffer of the most recent HTTP transactions (NOT gated on the
 * debug flag, unlike [com.movie.app.best.data.debug.NetworkLogger]).
 *
 * Cheap by design: bodies are only peeked for manifests / error responses / text
 * payloads, never for large media segments.
 */
object NetTraceRecorder {

    const val BODY_CAP = 1500
    private const val MAX = 40

    private val order = CopyOnWriteArrayList<String>()
    private val byUrl = ConcurrentHashMap<String, NetTrace>()

    private val SENSITIVE = setOf(
        "authorization", "x-auth-token", "cookie", "set-cookie", "proxy-authorization", "x-api-key"
    )

    fun record(trace: NetTrace) {
        byUrl[trace.url] = trace
        order.remove(trace.url)
        order.add(trace.url)
        while (order.size > MAX) {
            val old = order.removeAt(0)
            byUrl.remove(old)
        }
    }

    fun lastFor(url: String): NetTrace? = if (url.isEmpty()) null else byUrl[url]

    fun lastForPrefix(prefix: String): NetTrace? =
        if (prefix.isEmpty()) null
        else order.asReversed().firstNotNullOfOrNull { byUrl[it]?.takeIf { t -> t.url.startsWith(prefix) } }

    fun recent(limit: Int = 12): List<NetTrace> =
        order.asReversed().take(limit).mapNotNull { byUrl[it] }

    fun sanitize(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (k, v) ->
            if (k.lowercase() in SENSITIVE) mask(v) else v.take(400)
        }

    /** Token/cookie values are masked — the header name + length is enough to debug. */
    private fun mask(v: String): String =
        if (v.length <= 10) "\u2022\u2022\u2022" else "${v.take(4)}\u2026(len=${v.length})"
}

private fun Headers.toPlainMap(): Map<String, String> {
    val m = LinkedHashMap<String, String>()
    for (i in 0 until size) m[name(i)] = value(i)
    return m
}

/**
 * Records every transaction into [NetTraceRecorder]. Never alters the response
 * and never throws into the chain.
 */
class NetTraceInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val start = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            val took = System.currentTimeMillis() - start
            // Hot-path guard: a playing video fires this on EVERY HLS segment. Successful
            // binary media is not recorded at all — no header maps, no body peek — so
            // playback adds ~nothing per segment. Only manifests, API/text payloads and
            // any error response are captured.
            if (shouldRecord(url, response)) {
                NetTraceRecorder.record(
                    NetTrace(
                        url, request.method,
                        NetTraceRecorder.sanitize(request.headers.toPlainMap()),
                        response.code,
                        NetTraceRecorder.sanitize(response.headers.toPlainMap()),
                        peekBody(response), took, ""
                    )
                )
            }
            response
        } catch (e: Exception) {
            NetTraceRecorder.record(
                NetTrace(
                    url, request.method,
                    NetTraceRecorder.sanitize(request.headers.toPlainMap()),
                    0, emptyMap(), "", System.currentTimeMillis() - start,
                    "${e.javaClass.simpleName}: ${e.message}"
                )
            )
            throw e
        }
    }

    private fun shouldRecord(url: String, response: Response): Boolean {
        if (response.code >= 400) return true
        val ct = response.header("Content-Type") ?: ""
        return url.contains(".m3u8") || url.contains(".mpd") || url.contains(".json") ||
            ct.contains("json") || ct.contains("text") || ct.contains("xml", true) ||
            ct.contains("mpegurl", true)
    }

    private fun peekBody(response: Response): String =
        runCatching { response.peekBody(8192).string().take(NetTraceRecorder.BODY_CAP) }
            .getOrDefault("")
}
