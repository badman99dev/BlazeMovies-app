package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class FirebaseUserProfile(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String = "user",
    val tier: String = "free",
    val isVerified: Boolean = false,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val requestCount: Int = 0,
    val requestQuota: Int = 3
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "username" to username,
        "firstName" to firstName,
        "lastName" to lastName,
        "role" to role,
        "tier" to tier,
        "isVerified" to isVerified,
        "avatarUrl" to avatarUrl,
        "createdAt" to createdAt,
        "requestCount" to requestCount,
        "requestQuota" to requestQuota
    )
}

data class BookmarkItem(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
    val contentModeration: Map<String, String>? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "slug" to slug,
        "title" to title,
        "posterUrl" to posterUrl,
        "isSeries" to isSeries,
        "addedAt" to addedAt,
        "contentModeration" to contentModeration
    )
}

data class FirebaseHistoryItem(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean,
    val imdbId: String = "",
    val watchedAt: Long = System.currentTimeMillis(),
    val progressMs: Long = 0,
    val durationMs: Long = 0,
    val progressPercent: Float = 0f,
    val contentModeration: Map<String, String>? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "slug" to slug,
        "title" to title,
        "posterUrl" to posterUrl,
        "isSeries" to isSeries,
        "imdbId" to imdbId,
        "watchedAt" to watchedAt,
        "progressMs" to progressMs,
        "durationMs" to durationMs,
        "progressPercent" to progressPercent,
        "contentModeration" to contentModeration
    )
}

data class LikeItem(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val isSeries: Boolean,
    val likedAt: Long = System.currentTimeMillis(),
    val contentModeration: Map<String, String>? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "slug" to slug,
        "title" to title,
        "posterUrl" to posterUrl,
        "isSeries" to isSeries,
        "likedAt" to likedAt,
        "contentModeration" to contentModeration
    )
}

data class FirebaseNotification(
    val title: String,
    val body: String,
    val sentAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "body" to body,
        "sentAt" to sentAt
    )
}
