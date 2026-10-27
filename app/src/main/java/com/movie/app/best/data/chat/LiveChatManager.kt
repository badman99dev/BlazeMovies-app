package com.movie.app.best.data.chat

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.movie.app.best.data.config.RemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class LiveChatState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val roomId: String = "",
    val onlineCount: Int = 1,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null
)

@Singleton
class LiveChatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "LiveChatManager"
        private const val PREFS_NAME = "bm_live_chat_prefs"
        private const val KEY_SID = "chat_sid_cookie"
        private const val KEY_UID = "chat_uid"
        private const val KEY_NAME = "chat_name"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var sessionCookie: String? = prefs.getString(KEY_SID, null)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for SSE streaming
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _chatState = MutableStateFlow(
        LiveChatState(
            currentUserId = prefs.getString(KEY_UID, null),
            currentUserName = prefs.getString(KEY_NAME, null)
        )
    )
    val chatState: StateFlow<LiveChatState> = _chatState.asStateFlow()

    private val _floatingReactions = MutableSharedFlow<ChatReaction>(extraBufferCapacity = 50)
    val floatingReactions: SharedFlow<ChatReaction> = _floatingReactions.asSharedFlow()

    private var sseCall: Call? = null
    private var streamJob: Job? = null
    private var activeRoomId: String? = null
    private var lastEventSeq: Long = 0L

    private fun getBaseUrl(): String {
        val raw = RemoteConfig.liveChat().trim()
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    suspend fun ensureLogin(displayName: String): Boolean = withContext(Dispatchers.IO) {
        val cleanName = displayName.trim().ifBlank { "Fan" }.take(24)
        try {
            val url = "${getBaseUrl()}api/login"
            val body = gson.toJson(mapOf("name" to cleanName))
                .toRequestBody("application/json".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")

            sessionCookie?.let { reqBuilder.header("Cookie", it) }

            val response = client.newCall(reqBuilder.build()).execute()
            val setCookie = response.header("Set-Cookie")
            if (!setCookie.isNullOrBlank()) {
                val cookieVal = setCookie.split(";").firstOrNull { it.trim().startsWith("sid=") }?.trim()
                if (cookieVal != null) {
                    sessionCookie = cookieVal
                    prefs.edit().putString(KEY_SID, cookieVal).apply()
                }
            }

            val respBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val loginResp = gson.fromJson(respBody, ChatLoginResponse::class.java)
                val user = loginResp.user
                if (user != null) {
                    prefs.edit()
                        .putString(KEY_UID, user.id)
                        .putString(KEY_NAME, user.name)
                        .apply()
                    _chatState.update {
                        it.copy(
                            currentUserId = user.id,
                            currentUserName = user.name
                        )
                    }
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            return@withContext false
        }
    }

    fun connectToRoom(roomId: String, userName: String) {
        if (activeRoomId == roomId && _chatState.value.isConnected) {
            return
        }

        disconnect()
        activeRoomId = roomId
        lastEventSeq = 0L

        _chatState.update {
            it.copy(
                isConnecting = true,
                isConnected = false,
                roomId = roomId,
                error = null
            )
        }

        streamJob = scope.launch {
            ensureLogin(userName)
            loadHistory(roomId)
            startSseLoop(roomId)
        }
    }

    private suspend fun loadHistory(roomId: String) = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}api/chat/history?key=$roomId&limit=50"
            val reqBuilder = Request.Builder()
                .url(url)
                .get()
            sessionCookie?.let { reqBuilder.header("Cookie", it) }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val history = gson.fromJson(body, ChatHistoryResponse::class.java)
                val list = history.messages.sortedBy { it.seq }
                list.lastOrNull()?.let { lastEventSeq = maxOf(lastEventSeq, it.seq) }
                _chatState.update { current ->
                    current.copy(messages = list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load history: ${e.message}")
        }
    }

    private suspend fun startSseLoop(roomId: String) {
        var backoffMs = 1000L
        while (scope.isActive && activeRoomId == roomId) {
            try {
                _chatState.update { it.copy(isConnecting = true) }
                val urlBuilder = StringBuilder("${getBaseUrl()}events/chat?key=$roomId")
                if (lastEventSeq > 0) {
                    urlBuilder.append("&since=").append(lastEventSeq)
                }

                val reqBuilder = Request.Builder()
                    .url(urlBuilder.toString())
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")

                if (lastEventSeq > 0) {
                    reqBuilder.header("Last-Event-ID", lastEventSeq.toString())
                }
                sessionCookie?.let { reqBuilder.header("Cookie", it) }

                val call = client.newCall(reqBuilder.build())
                sseCall = call
                val response = call.execute()

                if (!response.isSuccessful) {
                    response.close()
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(10000L)
                    continue
                }

                backoffMs = 1000L
                _chatState.update { it.copy(isConnected = true, isConnecting = false, error = null) }

                val source = response.body?.byteStream() ?: break
                val reader = BufferedReader(InputStreamReader(source, Charsets.UTF_8))

                var currentEvent = ""
                var currentId: Long? = null
                val dataBuilder = StringBuilder()

                while (scope.isActive && activeRoomId == roomId) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) {
                        if (dataBuilder.isNotEmpty()) {
                            val data = dataBuilder.toString().trim()
                            dispatchSseEvent(currentEvent, data, currentId)
                            dataBuilder.setLength(0)
                        }
                        currentEvent = ""
                        currentId = null
                    } else if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim()
                    } else if (line.startsWith("id:")) {
                        currentId = line.substring(3).trim().toLongOrNull()
                        if (currentId != null) {
                            lastEventSeq = maxOf(lastEventSeq, currentId)
                        }
                    } else if (line.startsWith("data:")) {
                        if (dataBuilder.isNotEmpty()) dataBuilder.append("\n")
                        dataBuilder.append(line.substring(5).trim())
                    }
                }
                response.close()
            } catch (e: Exception) {
                if (scope.isActive && activeRoomId == roomId) {
                    _chatState.update { it.copy(isConnected = false, isConnecting = true) }
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(10000L)
                }
            }
        }
    }

    private fun dispatchSseEvent(eventName: String, data: String, eventId: Long?) {
        try {
            when (eventName) {
                "chat" -> {
                    val msg = gson.fromJson(data, ChatMessage::class.java)
                    if (msg != null) {
                        _chatState.update { state ->
                            if (state.messages.any { it.seq == msg.seq }) {
                                state
                            } else {
                                state.copy(messages = (state.messages + msg).takeLast(200))
                            }
                        }
                    }
                }
                "presence" -> {
                    val p = gson.fromJson(data, ChatPresence::class.java)
                    if (p != null) {
                        _chatState.update { it.copy(onlineCount = p.online.coerceAtLeast(1)) }
                    }
                }
                "reaction" -> {
                    val r = gson.fromJson(data, ChatReaction::class.java)
                    if (r != null) {
                        _floatingReactions.tryEmit(r)
                    }
                }
                "hello" -> {
                    runCatching {
                        val obj = gson.fromJson(data, Map::class.java)
                        val online = (obj["online"] as? Number)?.toInt() ?: 1
                        _chatState.update { it.copy(onlineCount = online.coerceAtLeast(1)) }
                    }
                }
                "room" -> {
                    Log.d(TAG, "Room closed notification received: $data")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event $eventName: ${e.message}")
        }
    }

    suspend fun sendMessage(text: String, kind: String = "text"): Boolean = withContext(Dispatchers.IO) {
        val rid = activeRoomId ?: return@withContext false
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return@withContext false

        try {
            val url = "${getBaseUrl()}api/chat/send"
            val payload = mapOf(
                "key" to rid,
                "body" to cleanText,
                "kind" to kind
            )
            val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")

            sessionCookie?.let { reqBuilder.header("Cookie", it) }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string().orEmpty()
                val sendResp = gson.fromJson(respBody, ChatSendResponse::class.java)
                val msg = sendResp.message
                if (msg != null) {
                    _chatState.update { state ->
                        if (state.messages.any { it.seq == msg.seq }) state
                        else state.copy(messages = (state.messages + msg).takeLast(200))
                    }
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            return@withContext false
        }
    }

    suspend fun sendReaction(reactionId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        val rid = activeRoomId ?: return@withContext false

        _floatingReactions.tryEmit(
            ChatReaction(
                rid = rid,
                reaction = reactionId,
                emoji = emoji,
                userId = _chatState.value.currentUserId.orEmpty(),
                name = _chatState.value.currentUserName.orEmpty(),
                ts = System.currentTimeMillis()
            )
        )

        try {
            val url = "${getBaseUrl()}api/chat/react"
            val payload = mapOf(
                "key" to rid,
                "reaction" to reactionId
            )
            val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())

            val reqBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")

            sessionCookie?.let { reqBuilder.header("Cookie", it) }

            client.newCall(reqBuilder.build()).execute().close()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reaction: ${e.message}")
            return@withContext false
        }
    }

    fun disconnect() {
        sseCall?.cancel()
        sseCall = null
        streamJob?.cancel()
        streamJob = null
        activeRoomId = null
        _chatState.update { it.copy(isConnected = false, isConnecting = false) }
    }
}
