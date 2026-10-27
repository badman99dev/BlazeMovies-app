package com.movie.app.best.data.chat

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    @SerializedName("seq") val seq: Long = 0L,
    @SerializedName("room") val room: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("kind") val kind: String = "text",
    @SerializedName("body") val body: String = "",
    @SerializedName("ts") val ts: Long = 0L
)

data class ChatReaction(
    /** Server-side reaction id ("r12"), NOT the room id. */
    @SerializedName("rid") val reactionId: String = "",
    @SerializedName("reaction") val reaction: String = "",
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("ts") val ts: Long = 0L
)

data class ChatPresence(
    @SerializedName("online") val online: Int = 1,
    @SerializedName("room") val room: String = ""
)

data class ChatRoomInfo(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String? = null
)

data class ChatHistoryResponse(
    @SerializedName("room") val room: ChatRoomInfo? = null,
    @SerializedName("messages") val messages: List<ChatMessage> = emptyList()
)

data class ChatLoginResponse(
    @SerializedName("user") val user: ChatUser? = null,
    @SerializedName("error") val error: String? = null
)

data class ChatUser(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = ""
)

data class ChatSendResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("message") val message: ChatMessage? = null,
    @SerializedName("error") val error: String? = null
)

data class ChatReactResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("reaction") val reaction: String? = null,
    @SerializedName("dropped") val dropped: Boolean = false,
    @SerializedName("error") val error: String? = null
)

data class ChatMeResponse(
    @SerializedName("user") val user: ChatUser? = null
)

data class ChatErrorResponse(
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null
)
