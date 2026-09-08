package com.movie.app.best.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movie.app.best.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BypassWsClient @Inject constructor(private val gson: Gson) {

    private class SimpleCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, ConcurrentLinkedQueue<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val bucket = store.getOrPut(url.host) { ConcurrentLinkedQueue() }
            cookies.forEach { cookie ->
                bucket.removeAll { it.name == cookie.name }
                bucket.add(cookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val matched = mutableListOf<Cookie>()
            for ((host, bucket) in store) {
                if (url.host == host || url.host.endsWith(".$host")) {
                    bucket.forEach { cookie ->
                        if (cookie.matches(url) && cookie.expiresAt > System.currentTimeMillis()) {
                            matched.add(cookie)
                        }
                    }
                }
            }
            return matched
        }
    }

    private val cookieJar = SimpleCookieJar()

    private val executorBase = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(FETCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val followClient = executorBase.newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val noFollowClient = executorBase.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val wsUrl: String

    init {
        val base = BuildConfig.DL_AGENT_BASE_URL
        val ws = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }
        wsUrl = ws.trimEnd('/')
    }

    suspend fun resolve(
        linkUrl: String,
        fetchInfo: Boolean = true,
        onLog: (String) -> Unit = {}
    ): BypassDoneEvent = withContext(Dispatchers.IO) {
        val session = Session(linkUrl, fetchInfo, onLog)
        try {
            withTimeout(SESSION_TIMEOUT_MS) { session.await() }
        } finally {
            session.shutdown()
        }
    }

    private inner class Session(
        private val linkUrl: String,
        private val fetchInfo: Boolean,
        private val onLog: (String) -> Unit
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val result = CompletableDeferred<BypassDoneEvent>()
        private val finished = AtomicBoolean(false)
        private var webSocket: WebSocket? = null

        suspend fun await(): BypassDoneEvent {
            val request = Request.Builder().url("$wsUrl/ws").build()
            webSocket = wsClient.newWebSocket(request, listener())
            return result.await()
        }

        fun shutdown() {
            scope.cancel()
            runCatching { webSocket?.close(1000, "bye") }
        }

        private fun listener() = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val start = JsonObject().apply {
                    addProperty("type", "start")
                    addProperty("url", linkUrl)
                    addProperty("fetch_info", fetchInfo)
                    addProperty("use_client", true)
                }
                webSocket.send(start.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val msg = JsonParser.parseString(text).asJsonObject
                    when (msg.get("action")?.takeIf { !it.isJsonNull }?.asString) {
                        "log" -> onLog(msg.get("log")?.takeIf { !it.isJsonNull }?.asString ?: "")
                        "fetch" -> scope.launch {
                            val reply = runCatching { executeFetch(msg).toString() }
                                .getOrElse { fetchError(it.message ?: "fetch failed").toString() }
                            runCatching { webSocket.send(reply) }
                        }
                        "done" -> {
                            val event = runCatching { gson.fromJson(text, BypassDoneEvent::class.java) }
                                .getOrDefault(BypassDoneEvent())
                            if (finished.compareAndSet(false, true)) result.complete(event)
                        }
                        "error" -> {
                            val message = msg.get("message")?.takeIf { !it.isJsonNull }?.asString ?: "relay error"
                            if (finished.compareAndSet(false, true)) result.completeExceptionally(Exception(message))
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (finished.compareAndSet(false, true)) {
                    result.completeExceptionally(Exception(t.message ?: "websocket failure"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (finished.compareAndSet(false, true)) {
                    result.completeExceptionally(Exception("connection closed ($code)"))
                }
            }
        }

        private fun executeFetch(msg: JsonObject): JsonObject {
            val url = msg.get("url")?.takeIf { !it.isJsonNull }?.asString
            if (url.isNullOrEmpty()) return fetchError("missing url")
            val method = (msg.get("method")?.takeIf { !it.isJsonNull }?.asString ?: "GET").uppercase()
            val needBody = msg.get("need_body")?.takeIf { !it.isJsonNull }?.asBoolean ?: true
            val followRedirects = msg.get("follow_redirects")?.takeIf { !it.isJsonNull }?.asBoolean ?: true
            val client = if (followRedirects) followClient else noFollowClient

            return try {
                val builder = Request.Builder().url(url)
                val headers = msg.get("headers")?.takeIf { it.isJsonObject }?.asJsonObject
                val hasUserAgent = headers?.entrySet()
                    ?.any { it.key.equals("User-Agent", ignoreCase = true) } == true
                if (!hasUserAgent) builder.header("User-Agent", CLIENT_UA)
                headers?.entrySet()?.forEach { (key, value) ->
                    val headerValue = value.takeIf { !it.isJsonNull }?.asString
                    if (!headerValue.isNullOrEmpty() &&
                        !key.equals("Cookie", ignoreCase = true) &&
                        !key.equals("Host", ignoreCase = true) &&
                        !key.equals("Content-Length", ignoreCase = true)
                    ) {
                        runCatching { builder.header(key, headerValue) }
                    }
                }
                val body = msg.get("body")?.takeIf { !it.isJsonNull }?.asString
                when (method) {
                    "HEAD" -> builder.head()
                    "GET" -> builder.get()
                    "POST" -> {
                        val contentType = headers?.get("Content-Type")?.takeIf { !it.isJsonNull }?.asString
                            ?: "application/x-www-form-urlencoded"
                        builder.post((body ?: "").toRequestBody(contentType.toMediaTypeOrNull()))
                    }
                    else -> builder.method(method, body?.toRequestBody(null))
                }
                client.newCall(builder.build()).execute().use { resp ->
                    val respHeaders = JsonObject()
                    for (name in resp.headers.names()) {
                        respHeaders.addProperty(name.lowercase(), resp.headers.values(name).joinToString(", "))
                    }
                    val bodyText = if (needBody && method != "HEAD") readBodyCapped(resp) else ""
                    JsonObject().apply {
                        addProperty("type", "fetch_result")
                        addProperty("success", true)
                        addProperty("status", resp.code)
                        add("headers", respHeaders)
                        addProperty("body", bodyText)
                    }
                }
            } catch (e: Exception) {
                fetchError(e.message ?: "fetch failed")
            }
        }

        private fun fetchError(error: String): JsonObject = JsonObject().apply {
            addProperty("type", "fetch_error")
            addProperty("error", error)
        }

        private fun readBodyCapped(resp: Response): String {
            val stream = resp.body?.byteStream() ?: return ""
            return try {
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16384)
                var total = 0
                while (true) {
                    val n = stream.read(buf)
                    if (n == -1) break
                    total += n
                    if (total > MAX_BODY_BYTES) break
                    out.write(buf, 0, n)
                }
                out.toString("UTF-8")
            } finally {
                stream.close()
            }
        }
    }

    companion object {
        private const val CLIENT_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        private const val FETCH_TIMEOUT_MS = 9000L
        private const val SESSION_TIMEOUT_MS = 150_000L
        private const val MAX_BODY_BYTES = 5L * 1024 * 1024
    }
}
