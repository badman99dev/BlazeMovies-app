package com.movie.app.best.data.chat

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.movie.app.best.data.config.RemoteConfig
import com.movie.app.best.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
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
    val onlineCount: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null
)

@Singleton
class LiveChatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "LiveChatManager"
        private const val PREFS_NAME = "bm_live_chat_prefs"
        private const val KEY_SID = "chat_sid_cookie"
        private const val KEY_UID = "chat_uid"
        private const val KEY_NAME = "chat_name"
        private const val KEY_OWNER = "chat_owner_uid"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var sessionCookie: String? = prefs.getString(KEY_SID, null)

    /** The app account this chat session belongs to (Firebase uid). */
    @Volatile
    private var ownerUid: String? = null

    private data class ChatIdentity(val token: String, val handle: String)

    /** Verified Firebase token + server-assigned handle sent on EVERY request. */
    @Volatile
    private var identity: ChatIdentity? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Same pool/settings, but no read timeout so the SSE body can stay open. */
    private val streamClient: OkHttpClient = client.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
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

    init {
        ownerUid = prefs.getString(KEY_OWNER, null)
        FirebaseAuth.getInstance().addAuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            if (uid != ownerUid) {
                // Account switched (or signed out): the stored chat session belongs
                // to the previous account, so nothing from it may be reused — else
                // messages keep going out under the old handle.
                disconnect()
                sessionCookie = null
                identity = null
                ownerUid = uid
                prefs.edit().clear().putString(KEY_OWNER, uid ?: "").apply()
                _chatState.update {
                    it.copy(currentUserId = null, currentUserName = null, roomId = "")
                }
            }
        }
    }

    private var sseCall: Call? = null
    private var streamJob: Job? = null
    @Volatile private var activeRoomId: String? = null
    @Volatile private var lastEventSeq: Long = 0L
    @Volatile private var localReactionId: String = ""
    @Volatile private var localReactionWindowEnd: Long = 0L

    private fun getBaseUrl(): String {
        val raw = RemoteConfig.liveChat().trim()
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    /**
     * Firebase ID token + the account's server-assigned handle. Every chat call
     * carries both, so the server never has to trust a client-typed name.
     */
    private suspend fun resolveIdentity(force: Boolean = false): ChatIdentity? {
        identity?.takeIf { !force }?.let { return it }
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val token = try { user.getIdToken(false).await()?.token } catch (_: Exception) { null }
            ?: return null
        val handle = authRepository.ensureHandle()?.takeIf { it.isNotBlank() } ?: return null
        return ChatIdentity(token, handle).also { identity = it }
    }

    private fun Request.Builder.withIdentity(): Request.Builder {
        identity?.let { id ->
            header("Authorization", "Bearer ${id.token}")
            header("X-Chat-Handle", id.handle)
        }
        return this
    }

    suspend fun ensureLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Firebase token + the account's immutable handle. Without both we do
            // NOT connect — a wrong name would impersonate somebody in chat.
            val id = resolveIdentity()
            if (id == null) {
                setError("Couldn't verify your chat identity. Check your connection and retry.")
                return@withContext false
            }
            val handle = id.handle

            // Reuse an existing session only when it belongs to THIS account and
            // still carries the same handle (server mints a new user per login,
            // so re-logging in would churn the uid on every connect).
            val existing = sessionCookie
            if (existing != null) {
                val me = fetchMe(existing)
                if (me != null && me.name.equals(handle, ignoreCase = true)) {
                    applyUser(me)
                    return@withContext true
                }
                sessionCookie = null
                prefs.edit().remove(KEY_SID).apply()
            }

            val cleanName = sanitizeName(handle)
            val body = gson.toJson(mapOf("name" to cleanName))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${getBaseUrl()}api/login")
                .post(body)
                .header("Content-Type", "application/json")
                .withIdentity()
                .build()

            client.newCall(request).execute().use { response ->
                val setCookie = response.header("Set-Cookie")
                if (!setCookie.isNullOrBlank()) {
                    val cookieVal = setCookie.split(";").firstOrNull { it.trim().startsWith("sid=") }?.trim()
                    if (cookieVal != null) {
                        sessionCookie = cookieVal
                        ownerUid = FirebaseAuth.getInstance().currentUser?.uid
                        prefs.edit()
                            .putString(KEY_SID, cookieVal)
                            .putString(KEY_OWNER, ownerUid ?: "")
                            .apply()
                    }
                }
                if (!response.isSuccessful) {
                    setError(friendlyError(readError(response)))
                    return@withContext false
                }
                val user = gson.fromJson(
                    response.body?.string().orEmpty(),
                    ChatLoginResponse::class.java
                )?.user
                if (user == null) {
                    setError("Couldn't join live chat. Please try again.")
                    return@withContext false
                }
                applyUser(user)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            setError("Can't reach the chat server.")
            return@withContext false
        }
    }

    private fun fetchMe(cookie: String): ChatUser? = try {
        val request = Request.Builder()
            .url("${getBaseUrl()}api/me")
            .header("Cookie", cookie)
            .withIdentity()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else gson.fromJson(response.body?.string().orEmpty(), ChatMeResponse::class.java)?.user
        }
    } catch (e: Exception) {
        null
    }

    private fun applyUser(user: ChatUser) {
        prefs.edit()
            .putString(KEY_UID, user.id)
            .putString(KEY_NAME, user.name)
            .apply()
        _chatState.update { it.copy(currentUserId = user.id, currentUserName = user.name) }
    }

    /** Server accepts 2-24 chars of letters/numbers/space/underscore/dot/dash. */
    private fun sanitizeName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[^\\p{L}\\p{N} ._-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(24)
            .trim()
        return if (cleaned.length >= 2) cleaned else "Fan"
    }

    private fun setError(message: String?) {
        _chatState.update { it.copy(error = message) }
    }

    private fun readError(response: Response): String? = try {
        val parsed = gson.fromJson(response.body?.string().orEmpty(), ChatErrorResponse::class.java)
        parsed?.message?.takeIf { it.isNotBlank() } ?: parsed?.error
    } catch (e: Exception) {
        null
    }

    private fun friendlyError(code: String?): String {
        val c = code?.trim()?.lowercase()
        return when {
            c.isNullOrEmpty() -> "Chat connection failed."
            c == "slow down" -> "Slow down a little and try again."
            c == "login required" -> "Chat login failed. Reopen the chat to retry."
            c == "you are muted" -> "You are muted in this room."
            c == "unknown reaction" -> "That reaction isn't available."
            c == "no_key" -> "Live chat isn't available for this match."
            c == "room_not_exist" -> "This chat room has closed."
            c == "not_yet" -> "This chat room hasn't opened yet."
            c == "bad_signature" || c == "invalid_key" -> "Live chat is misconfigured."
            else -> (code ?: "").trim()
        }
    }

    fun connectToRoom(roomId: String) {
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
                onlineCount = 0,
                messages = emptyList(),
                error = null
            )
        }

        streamJob = scope.launch {
            if (!ensureLogin()) {
                _chatState.update { it.copy(isConnecting = false, isConnected = false) }
                return@launch
            }
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
                .withIdentity()
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
            } else if (activeRoomId == roomId) {
                setError(friendlyError(readError(response)))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load history: ${e.message}")
            if (activeRoomId == roomId) setError("Couldn't load chat history.")
        }
    }

    private suspend fun startSseLoop(roomId: String) {
        var backoffMs = 1000L
        var fatal = false
        while (scope.isActive && activeRoomId == roomId && !fatal) {
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
                    .withIdentity()

                if (lastEventSeq > 0) {
                    reqBuilder.header("Last-Event-ID", lastEventSeq.toString())
                }
                sessionCookie?.let { reqBuilder.header("Cookie", it) }

                val call = streamClient.newCall(reqBuilder.build())
                sseCall = call
                val response = call.execute()

                if (!response.isSuccessful) {
                    val code = response.code
                    val err = friendlyError(readError(response))
                    response.close()
                    _chatState.update { it.copy(isConnected = false) }
                    // A key that is rejected/expired will never start working by
                    // retrying — stop and show the reason instead of looping.
                    if (code in 400..499) {
                        setError(err)
                        fatal = true
                        break
                    }
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(10000L)
                    continue
                }

                backoffMs = 1000L
                _chatState.update { it.copy(isConnected = true, isConnecting = false, error = null) }

                val source = response.body?.byteStream()
                if (source == null) {
                    response.close()
                    _chatState.update { it.copy(isConnected = false) }
                    break
                }
                val reader = BufferedReader(InputStreamReader(source, Charsets.UTF_8))

                var currentEvent = ""
                var currentId: Long? = null
                val dataBuilder = StringBuilder()

                while (scope.isActive && activeRoomId == roomId) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) {
                        if (dataBuilder.isNotEmpty()) {
                            val data = dataBuilder.toString().trim()
                            val keepGoing = dispatchSseEvent(currentEvent, data, currentId)
                            // Only advance the cursor once the event was applied, so a
                            // drop between `id:` and `data:` cannot lose a message.
                            currentId?.let { lastEventSeq = maxOf(lastEventSeq, it) }
                            dataBuilder.setLength(0)
                            if (!keepGoing) {
                                fatal = true
                                break
                            }
                        }
                        currentEvent = ""
                        currentId = null
                    } else if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim()
                    } else if (line.startsWith("id:")) {
                        currentId = line.substring(3).trim().toLongOrNull()
                    } else if (line.startsWith("data:")) {
                        if (dataBuilder.isNotEmpty()) dataBuilder.append("\n")
                        dataBuilder.append(line.substring(5).trim())
                    }
                }
                response.close()
                _chatState.update { it.copy(isConnected = false) }
            } catch (e: Exception) {
                if (scope.isActive && activeRoomId == roomId) {
                    _chatState.update { it.copy(isConnected = false, isConnecting = true) }
                    delay(backoffMs)
                    backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(10000L)
                }
            }
        }
        if (fatal) {
            _chatState.update { it.copy(isConnected = false, isConnecting = false) }
        }
    }

    /** Returns false when the stream should stop (room denied / closed). */
    private fun dispatchSseEvent(eventName: String, data: String, eventId: Long?): Boolean {
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
                        _chatState.update { it.copy(onlineCount = p.online.coerceAtLeast(0)) }
                    }
                }
                "reaction" -> {
                    val r = gson.fromJson(data, ChatReaction::class.java)
                    if (r != null) {
                        val state = _chatState.value
                        val mine = (r.userId.isNotEmpty() && r.userId == state.currentUserId) ||
                            (r.userId.isEmpty() && r.name.isNotEmpty() &&
                                r.name.equals(state.currentUserName, ignoreCase = true))
                        val ownEcho = mine &&
                            r.reaction == localReactionId &&
                            System.currentTimeMillis() < localReactionWindowEnd
                        if (ownEcho) {
                            localReactionId = ""
                            localReactionWindowEnd = 0L
                        } else {
                            _floatingReactions.tryEmit(r)
                        }
                    }
                }
                "hello" -> {
                    runCatching {
                        val obj = gson.fromJson(data, Map::class.java)
                        val online = (obj["online"] as? Number)?.toInt() ?: 0
                        _chatState.update { it.copy(onlineCount = online.coerceAtLeast(0)) }
                    }
                }
                "denied" -> {
                    val obj = runCatching { gson.fromJson(data, Map::class.java) }.getOrNull()
                    val code = (obj?.get("error") as? String) ?: (obj?.get("message") as? String)
                    setError(friendlyError(code))
                    _chatState.update { it.copy(isConnected = false, isConnecting = false) }
                    return false
                }
                "room" -> {
                    val obj = runCatching { gson.fromJson(data, Map::class.java) }.getOrNull()
                    if (obj?.get("closed") == true) {
                        setError("This chat room has closed.")
                        _chatState.update { it.copy(isConnected = false, isConnecting = false) }
                        return false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event $eventName: ${e.message}")
        }
        return true
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
                .withIdentity()

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
                setError(null)
                return@withContext true
            }
            if (response.code == 401) {
                sessionCookie = null
                prefs.edit().remove(KEY_SID).apply()
            }
            setError(friendlyError(readError(response)))
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            return@withContext false
        }
    }

    suspend fun sendReaction(reactionId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        val rid = activeRoomId ?: return@withContext false

        localReactionId = reactionId
        localReactionWindowEnd = System.currentTimeMillis() + 8000L

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
                .withIdentity()

            sessionCookie?.let { reqBuilder.header("Cookie", it) }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    setError(friendlyError(readError(response)))
                    return@withContext false
                }
            }

            _floatingReactions.tryEmit(
                ChatReaction(
                    reactionId = "",
                    reaction = reactionId,
                    emoji = emoji,
                    userId = _chatState.value.currentUserId.orEmpty(),
                    name = _chatState.value.currentUserName.orEmpty(),
                    ts = System.currentTimeMillis()
                )
            )
            setError(null)
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
        _chatState.update { it.copy(isConnected = false, isConnecting = false, error = null, onlineCount = 0) }
    }
}
