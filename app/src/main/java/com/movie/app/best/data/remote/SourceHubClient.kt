package com.movie.app.best.data.remote

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.SourceHubRequest
import com.movie.app.best.data.model.SourceHubResolveResult
import com.movie.app.best.data.model.ServerScanRow
import com.movie.app.best.data.model.SourceHubServiceResult
import com.movie.app.best.data.model.SourceHubStartEvent
import com.movie.app.best.data.model.SourceHubSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHubClient @Inject constructor(private val gson: Gson) {

    private val executorBase = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val wsUrl: String = run {
        val base = BuildConfig.SOURCE_HUB_BASE_URL
        val ws = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }
        ws.trimEnd('/') + "/ws"
    }

    suspend fun resolve(
        request: SourceHubRequest,
        onService: (SourceHubServiceResult) -> Unit = {},
        onScan: (List<ServerScanRow>) -> Unit = {}
    ): SourceHubResolveResult = withContext(Dispatchers.IO) {
        val session = Session(request, onService, onScan)
        try {
            withTimeout(SESSION_TIMEOUT_MS) { session.await() }
        } catch (e: Exception) {
            SourceHubResolveResult()
        } finally {
            session.shutdown()
        }
    }

    private inner class Session(
        private val request: SourceHubRequest,
        private val onService: (SourceHubServiceResult) -> Unit,
        private val onScan: (List<ServerScanRow>) -> Unit
    ) {
        private val scan = LinkedHashMap<String, ServerScanRow>()

        private fun pushScan(name: String, status: String, elapsedMs: Long = 0, error: String? = null) {
            if (name.isBlank()) return
            val prev = scan[name]
            scan[name] = ServerScanRow(name = name, status = status, elapsedMs = if (elapsedMs > 0) elapsedMs else prev?.elapsedMs ?: 0, error = error)
            runCatching { onScan(scan.values.toList()) }
        }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val result = CompletableDeferred<SourceHubResolveResult>()
        private val finished = AtomicBoolean(false)
        private val reqId = "r" + System.currentTimeMillis().toString(36)
        private val sources = mutableListOf<SourceHubSource>()

        suspend fun await(): SourceHubResolveResult {
            val req = Request.Builder().url(wsUrl).build()
            wsClient.newWebSocket(req, listener())
            return result.await()
        }

        fun shutdown() {
            scope.cancel()
        }

        private fun listener() = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(gson.toJson(mapOf("action" to "hello", "relay" to true)))
                val start = mutableMapOf<String, Any?>(
                    "action" to "resolve",
                    "id" to request.id,
                    "type" to request.type,
                    "reqId" to reqId
                )
                request.season?.let { start["season"] = it }
                request.episode?.let { start["episode"] = it }
                webSocket.send(gson.toJson(start))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val msg = JsonParser.parseString(text).asJsonObject
                    when (msg.get("event")?.takeIf { !it.isJsonNull }?.asString) {
                        "http_request" -> scope.launch {
                            val reply = runCatching { executeFetch(msg).toString() }
                                .getOrElse { fetchError(msg, it.message ?: "fetch failed").toString() }
                            runCatching { webSocket.send(reply) }
                        }
                        "start" -> {
                            val start = runCatching { gson.fromJson(text, SourceHubStartEvent::class.java) }.getOrNull()
                            start?.services?.forEach { pushScan(it.name, it.status) }
                        }
                        "service" -> {
                            val svc = runCatching { gson.fromJson(text, SourceHubServiceResult::class.java) }
                                .getOrNull() ?: SourceHubServiceResult()
                            val st = svc.status ?: if (svc.skipped != null) "na" else if (svc.ok) "found" else "fail"
                            pushScan(svc.service, st, elapsedMs = msg.get("elapsedMs")?.takeIf { !it.isJsonNull }?.asLong ?: 0, error = svc.error)
                            if (svc.sources.isNotEmpty()) {
                                sources.addAll(svc.sources)
                                runCatching { onService(svc) }
                            }
                        }
                        "done" -> {
                            if (finished.compareAndSet(false, true)) {
                                msg.getAsJsonArray("pending")?.forEach { el ->
                                    val n = el.takeIf { !it.isJsonNull }?.asString ?: return@forEach
                                    pushScan(n, "fail", error = "timeout")
                                }
                                val out = SourceHubResolveResult(
                                    ok = sources.isNotEmpty(),
                                    count = sources.size,
                                    sources = sources.toList()
                                )
                                result.complete(out)
                            }
                        }
                        "error" -> {
                            if (finished.compareAndSet(false, true)) {
                                result.complete(SourceHubResolveResult())
                            }
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                NetworkLogger.logAction("SOURCEHUB_WS_ERR", t.message ?: "unknown")
                if (finished.compareAndSet(false, true)) {
                    result.complete(SourceHubResolveResult())
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (finished.compareAndSet(false, true)) {
                    result.complete(SourceHubResolveResult(sources = sources.toList(), count = sources.size, ok = sources.isNotEmpty()))
                }
            }
        }

        private fun executeFetch(msg: JsonObject): JsonObject {
            val hopId = msg.get("hopId")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val reqIdOut = msg.get("reqId")?.takeIf { !it.isJsonNull }?.asString ?: reqId
            val spec = msg.getAsJsonObject("request")
                ?: return fetchError(msg, "missing request")

            val url = spec.get("url")?.takeIf { !it.isJsonNull }?.asString
            if (url.isNullOrEmpty()) return fetchError(msg, "missing url")
            val method = (spec.get("method")?.takeIf { !it.isJsonNull }?.asString ?: "GET").uppercase()
            val responseType = spec.get("responseType")?.takeIf { !it.isJsonNull }?.asString ?: "text"
            val maxBytes = (spec.get("maxBytes")?.takeIf { !it.isJsonNull }?.asLong ?: MAX_BODY_BYTES)
                .coerceIn(1024L, HARD_MAX_BODY_BYTES)
            val timeoutMs = (spec.get("timeoutMs")?.takeIf { !it.isJsonNull }?.asLong ?: 15000L)
                .coerceIn(1000L, 25000L)

            val builder = Request.Builder().url(url)
            val headers = spec.getAsJsonObject("headers")
            val hasUserAgent = headers?.entrySet()
                ?.any { it.key.equals("User-Agent", ignoreCase = true) } == true
            if (!hasUserAgent) builder.header("User-Agent", CLIENT_UA)
            headers?.entrySet()?.forEach { (key, value) ->
                val headerValue = value.takeIf { !it.isJsonNull }?.asString
                if (!headerValue.isNullOrEmpty() &&
                    !key.equals("Host", ignoreCase = true) &&
                    !key.equals("Content-Length", ignoreCase = true)
                ) {
                    runCatching { builder.header(key, headerValue) }
                }
            }

            val body = spec.get("body")?.takeIf { !it.isJsonNull }?.asString
            when (method) {
                "HEAD" -> builder.head()
                "GET" -> builder.get()
                "POST" -> {
                    val contentType = headers?.get("Content-Type")?.takeIf { !it.isJsonNull }?.asString
                        ?: "application/x-www-form-urlencoded"
                    builder.post((body ?: "").toRequestBody(contentType.toMediaTypeOrNull()))
                }
                else -> builder.method(method, (body ?: "").toRequestBody(null))
            }

            val callClient = if (timeoutMs != 15000L)
                executorBase.newBuilder().callTimeout(timeoutMs, TimeUnit.MILLISECONDS).build()
            else executorBase

            return try {
                callClient.newCall(builder.build()).execute().use { resp ->
                    val respHeaders = JsonObject()
                    for (name in resp.headers.names()) {
                        respHeaders.addProperty(name.lowercase(), resp.headers.values(name).joinToString(", "))
                    }
                    val bytes = if (method != "HEAD") readBodyCapped(resp, maxBytes) else ByteArray(0)
                    val payload = JsonObject().apply {
                        addProperty("status", resp.code)
                        add("headers", respHeaders)
                        if (responseType == "base64") {
                            addProperty("body", Base64.encodeToString(bytes, Base64.NO_WRAP))
                            addProperty("bodyEncoding", "base64")
                        } else {
                            addProperty("body", String(bytes, Charsets.UTF_8))
                        }
                    }
                    JsonObject().apply {
                        addProperty("action", "http_response")
                        addProperty("reqId", reqIdOut)
                        addProperty("hopId", hopId)
                        add("response", payload)
                    }
                }
            } catch (e: Exception) {
                fetchError(msg, e.message ?: "fetch failed")
            }
        }

        private fun fetchError(msg: JsonObject, error: String): JsonObject {
            val hopId = msg.get("hopId")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val reqIdOut = msg.get("reqId")?.takeIf { !it.isJsonNull }?.asString ?: reqId
            return JsonObject().apply {
                addProperty("action", "http_response")
                addProperty("reqId", reqIdOut)
                addProperty("hopId", hopId)
                add("response", JsonObject().apply {
                    addProperty("status", 0)
                    addProperty("error", error)
                })
            }
        }

        private fun readBodyCapped(resp: Response, maxBytes: Long): ByteArray {
            val stream = resp.body?.byteStream() ?: return ByteArray(0)
            return try {
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16384)
                var total = 0L
                while (true) {
                    val n = stream.read(buf)
                    if (n == -1) break
                    total += n
                    if (total > maxBytes) {
                        val keep = (n - (total - maxBytes)).toInt().coerceAtLeast(0)
                        if (keep > 0) out.write(buf, 0, keep)
                        break
                    }
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } finally {
                stream.close()
            }
        }
    }

    companion object {
        private const val CLIENT_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36"
        private const val SESSION_TIMEOUT_MS = 60_000L
        private const val MAX_BODY_BYTES = 2L * 1024 * 1024
        private const val HARD_MAX_BODY_BYTES = 8L * 1024 * 1024
    }
}
