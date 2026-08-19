package com.movie.app.best.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.FirebaseNotification
import com.movie.app.best.data.model.FirebaseUserProfile
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.remote.AuthApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiService: AuthApiService
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_library", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val db: FirebaseFirestore

    init {
        db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(Long.MAX_VALUE)
                    .build()
            )
            .build()
        db.firestoreSettings = settings
    }

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun userDoc() = uid()?.let { db.collection("users").document(it) }

    // ── User Profile ──────────────────────────────────────────

    // FCM token binding handshake via /v1/register.php (never writes Firestore directly).
    // anon=true -> device marked "anon" (logged-out/guest), token kept for later use.
    suspend fun registerFcmToken(anon: Boolean = false) {
        try {
            val fid = FirebaseInstallations.getInstance().id.await()
            val token = FirebaseMessaging.getInstance().token.await()
            if (anon) {
                apiService.registerDevice(
                    uid = null,
                    fid = fid,
                    device = Build.MODEL,
                    fcmToken = token,
                    anon = 1
                )
                return
            }
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val jwt = try { user.getIdToken(false).await()?.token } catch (_: Exception) { null } ?: return
            apiService.registerDevice(
                uid = user.uid,
                fid = fid,
                device = Build.MODEL,
                fcmToken = token,
                authHeader = "Bearer $jwt"
            )
        } catch (_: Exception) {
        }
    }

    suspend fun getOrCreateUserProfile(): FirebaseUserProfile? = withContext(Dispatchers.IO) {
        val uid = uid() ?: return@withContext null
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val d = doc.data ?: return@withContext null
                return@withContext FirebaseUserProfile(
                    uid = uid,
                    email = d["email"] as? String ?: "",
                    username = d["username"] as? String ?: "",
                    firstName = d["firstName"] as? String,
                    lastName = d["lastName"] as? String,
                    role = d["role"] as? String ?: "user",
                    tier = d["tier"] as? String ?: "free",
                    isVerified = d["isVerified"] as? Boolean ?: false,
                    avatarUrl = d["avatarUrl"] as? String,
                    createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    requestCount = (d["requestCount"] as? Number)?.toInt() ?: 0,
                    requestQuota = (d["requestQuota"] as? Number)?.toInt() ?: 3
                )
            } else {
                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val displayName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                val nameParts = displayName.split(" ", limit = 2)
                val profile = FirebaseUserProfile(
                    uid = uid,
                    email = email,
                    username = email.substringBefore("@"),
                    firstName = nameParts.getOrNull(0),
                    lastName = nameParts.getOrNull(1)
                )
                db.collection("users").document(uid).set(profile.toMap(), SetOptions.merge()).await()
                return@withContext profile
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun canMakeRequest(): Boolean = withContext(Dispatchers.IO) {
        val profile = getOrCreateUserProfile() ?: return@withContext false
        profile.requestCount < profile.requestQuota
    }

    suspend fun incrementRequestCount() = withContext(Dispatchers.IO) {
        val uid = uid() ?: return@withContext
        try {
            db.collection("users").document(uid).update("requestCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        } catch (_: Exception) {}
    }

    // ── Bookmarks ─────────────────────────────────────────────

    suspend fun addBookmark(item: BookmarkItem) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("bookmarks").document(item.slug).set(item.toMap()).await() } catch (_: Exception) {}
        saveLocalBookmark(item)
    }

    suspend fun removeBookmark(slug: String) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("bookmarks").document(slug).delete().await() } catch (_: Exception) {}
        removeLocalBookmark(slug)
    }

    suspend fun getBookmarks(): List<BookmarkItem> = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val snap = doc.collection("bookmarks").orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
                return@withContext snap.documents.mapNotNull { d ->
                    val m = d.data ?: return@mapNotNull null
                    BookmarkItem(
                        slug = m["slug"] as? String ?: d.id,
                        title = m["title"] as? String ?: "",
                        posterUrl = m["posterUrl"] as? String ?: "",
                        isSeries = m["isSeries"] as? Boolean ?: false,
                        addedAt = (m["addedAt"] as? Number)?.toLong() ?: 0,
                        contentModeration = m["contentModeration"] as? Map<String, String>
                    )
                }
            } catch (_: Exception) {}
        }
        getLocalBookmarks()
    }

    suspend fun isBookmarked(slug: String): Boolean = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val d = doc.collection("bookmarks").document(slug).get().await()
                return@withContext d.exists()
            } catch (_: Exception) {}
        }
        isLocalBookmarked(slug)
    }

    // ── History ───────────────────────────────────────────────

    suspend fun addToHistory(item: FirebaseHistoryItem) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("history").document(item.slug).set(item.toMap()).await() } catch (_: Exception) {}
        saveLocalHistory(item)
    }

    suspend fun getHistory(): List<FirebaseHistoryItem> = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val snap = doc.collection("history").orderBy("watchedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get().await()
                return@withContext snap.documents.mapNotNull { d ->
                    val m = d.data ?: return@mapNotNull null
                    FirebaseHistoryItem(
                        slug = m["slug"] as? String ?: d.id,
                        title = m["title"] as? String ?: "",
                        posterUrl = m["posterUrl"] as? String ?: "",
                        isSeries = m["isSeries"] as? Boolean ?: false,
                        imdbId = m["imdbId"] as? String ?: "",
                        watchedAt = (m["watchedAt"] as? Number)?.toLong() ?: 0,
                        progressMs = (m["progressMs"] as? Number)?.toLong() ?: 0,
                        durationMs = (m["durationMs"] as? Number)?.toLong() ?: 0,
                        progressPercent = (m["progressPercent"] as? Number)?.toFloat() ?: 0f,
                        contentModeration = m["contentModeration"] as? Map<String, String>
                    )
                }
            } catch (_: Exception) {}
        }
        getLocalHistory()
    }

    suspend fun removeFromHistory(slug: String) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("history").document(slug).delete().await() } catch (_: Exception) {}
        removeLocalHistory(slug)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try {
            val snap = doc.collection("history").get().await()
            for (d in snap.documents) {
                d.reference.delete()
            }
        } catch (_: Exception) {}
        prefs.edit().remove("history").apply()
    }

    fun updateProgressLocal(slug: String, progressMs: Long, durationMs: Long) {
        val percent = if (durationMs > 0) progressMs.toFloat() / durationMs.toFloat() else 0f
        saveLocalProgress(slug, progressMs, durationMs, percent)
    }

    suspend fun updateProgress(slug: String, progressMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        val percent = if (durationMs > 0) progressMs.toFloat() / durationMs.toFloat() else 0f
        try {
            doc.collection("history").document(slug).update(
                mapOf(
                    "progressMs" to progressMs,
                    "durationMs" to durationMs,
                    "progressPercent" to percent,
                    "watchedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {}
        saveLocalProgress(slug, progressMs, durationMs, percent)
    }

    suspend fun getProgress(slug: String): FirebaseHistoryItem? = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val d = doc.collection("history").document(slug).get().await()
                if (d.exists()) {
                    val m = d.data ?: return@withContext null
                    return@withContext FirebaseHistoryItem(
                        slug = m["slug"] as? String ?: d.id,
                        title = m["title"] as? String ?: "",
                        posterUrl = m["posterUrl"] as? String ?: "",
                        isSeries = m["isSeries"] as? Boolean ?: false,
                        imdbId = m["imdbId"] as? String ?: "",
                        watchedAt = (m["watchedAt"] as? Number)?.toLong() ?: 0,
                        progressMs = (m["progressMs"] as? Number)?.toLong() ?: 0,
                        durationMs = (m["durationMs"] as? Number)?.toLong() ?: 0,
                        progressPercent = (m["progressPercent"] as? Number)?.toFloat() ?: 0f,
                        contentModeration = m["contentModeration"] as? Map<String, String>
                    )
                }
            } catch (_: Exception) {}
        }
        getLocalProgress(slug)
    }

    suspend fun syncRemoteToLocal() = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try {
            val snap = doc.collection("history").orderBy("watchedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get().await()
            val items = snap.documents.mapNotNull { d ->
                val m = d.data ?: return@mapNotNull null
                FirebaseHistoryItem(
                    slug = m["slug"] as? String ?: d.id,
                    title = m["title"] as? String ?: "",
                    posterUrl = m["posterUrl"] as? String ?: "",
                    isSeries = m["isSeries"] as? Boolean ?: false,
                    imdbId = m["imdbId"] as? String ?: "",
                    watchedAt = (m["watchedAt"] as? Number)?.toLong() ?: 0,
                    progressMs = (m["progressMs"] as? Number)?.toLong() ?: 0,
                    durationMs = (m["durationMs"] as? Number)?.toLong() ?: 0,
                    progressPercent = (m["progressPercent"] as? Number)?.toFloat() ?: 0f,
                    contentModeration = m["contentModeration"] as? Map<String, String>
                )
            }
            saveAllLocalHistory(items)
        } catch (_: Exception) {}
    }

    // ── Likes ────────────────────────────────────────────────

    suspend fun addLike(item: LikeItem) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("likes").document(item.slug).set(item.toMap()).await() } catch (_: Exception) {}
        saveLocalLike(item)
    }

    suspend fun removeLike(slug: String) = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try { doc.collection("likes").document(slug).delete().await() } catch (_: Exception) {}
        removeLocalLike(slug)
    }

    suspend fun getLikes(): List<LikeItem> = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val snap = doc.collection("likes").orderBy("likedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
                return@withContext snap.documents.mapNotNull { d ->
                    val m = d.data ?: return@mapNotNull null
                    LikeItem(
                        slug = m["slug"] as? String ?: d.id,
                        title = m["title"] as? String ?: "",
                        posterUrl = m["posterUrl"] as? String ?: "",
                        isSeries = m["isSeries"] as? Boolean ?: false,
                        likedAt = (m["likedAt"] as? Number)?.toLong() ?: 0,
                        contentModeration = m["contentModeration"] as? Map<String, String>
                    )
                }
            } catch (_: Exception) {}
        }
        getLocalLikes()
    }

    suspend fun isLiked(slug: String): Boolean = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val d = doc.collection("likes").document(slug).get().await()
                return@withContext d.exists()
            } catch (_: Exception) {}
        }
        isLocalLiked(slug)
    }

    // ── Merge local → Firestore on login ─────────────────────

    suspend fun mergeLocalToFirestore() = withContext(Dispatchers.IO) {
        val doc = userDoc() ?: return@withContext
        try {
            val localBookmarks = getLocalBookmarks()
            for (item in localBookmarks) {
                val exists = doc.collection("bookmarks").document(item.slug).get().await().exists()
                if (!exists) {
                    doc.collection("bookmarks").document(item.slug).set(item.toMap()).await()
                }
            }
            val localHistory = getLocalHistory()
            for (item in localHistory) {
                val exists = doc.collection("history").document(item.slug).get().await().exists()
                if (!exists) {
                    doc.collection("history").document(item.slug).set(item.toMap()).await()
                }
            }
            val localLikes = getLocalLikes()
            for (item in localLikes) {
                val exists = doc.collection("likes").document(item.slug).get().await().exists()
                if (!exists) {
                    doc.collection("likes").document(item.slug).set(item.toMap()).await()
                }
            }
        } catch (_: Exception) {}
    }

    // ── Local SharedPreferences fallback (logged out) ─────────

    private fun saveLocalBookmark(item: BookmarkItem) {
        val list = getLocalBookmarks().toMutableList()
        list.removeAll { it.slug == item.slug }
        list.add(0, item)
        prefs.edit().putString("bookmarks", gson.toJson(list)).apply()
    }

    private fun removeLocalBookmark(slug: String) {
        val list = getLocalBookmarks().toMutableList()
        list.removeAll { it.slug == slug }
        prefs.edit().putString("bookmarks", gson.toJson(list)).apply()
    }

    private fun getLocalBookmarks(): List<BookmarkItem> {
        val json = prefs.getString("bookmarks", null) ?: return emptyList()
        return try { gson.fromJson(json, Array<BookmarkItem>::class.java).toList() } catch (_: Exception) { emptyList() }
    }

    private fun isLocalBookmarked(slug: String): Boolean = getLocalBookmarks().any { it.slug == slug }

    private fun saveLocalHistory(item: FirebaseHistoryItem) {
        val list = getLocalHistory().toMutableList()
        list.removeAll { it.slug == item.slug }
        list.add(0, item)
        if (list.size > 50) list.subList(50, list.size).clear()
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }

    private fun saveLocalProgress(slug: String, progressMs: Long, durationMs: Long, progressPercent: Float) {
        val list = getLocalHistory().toMutableList()
        val existing = list.find { it.slug == slug }
        if (existing != null) {
            list.removeAll { it.slug == slug }
            list.add(0, existing.copy(progressMs = progressMs, durationMs = durationMs, progressPercent = progressPercent, watchedAt = System.currentTimeMillis()))
        }
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }

    private fun saveAllLocalHistory(items: List<FirebaseHistoryItem>) {
        prefs.edit().putString("history", gson.toJson(items)).apply()
    }

    fun getLocalProgress(slug: String): FirebaseHistoryItem? = getLocalHistory().find { it.slug == slug }

    private fun removeLocalHistory(slug: String) {
        val list = getLocalHistory().toMutableList()
        list.removeAll { it.slug == slug }
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }

    private fun getLocalHistory(): List<FirebaseHistoryItem> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try { gson.fromJson(json, Array<FirebaseHistoryItem>::class.java).toList() } catch (_: Exception) { emptyList() }
    }

    fun getLocalHistoryForContinueWatching(): List<FirebaseHistoryItem> {
        return getLocalHistory()
            .filter { it.progressPercent > 0f && it.progressPercent < 0.96f }
            .sortedByDescending { it.watchedAt }
            .take(20)
    }

    private fun saveLocalLike(item: LikeItem) {
        val list = getLocalLikes().toMutableList()
        list.removeAll { it.slug == item.slug }
        list.add(0, item)
        prefs.edit().putString("likes", gson.toJson(list)).apply()
    }

    private fun removeLocalLike(slug: String) {
        val list = getLocalLikes().toMutableList()
        list.removeAll { it.slug == slug }
        prefs.edit().putString("likes", gson.toJson(list)).apply()
    }

    private fun getLocalLikes(): List<LikeItem> {
        val json = prefs.getString("likes", null) ?: return emptyList()
        return try { gson.fromJson(json, Array<LikeItem>::class.java).toList() } catch (_: Exception) { emptyList() }
    }

    private fun isLocalLiked(slug: String): Boolean = getLocalLikes().any { it.slug == slug }

    // ── Local File Progress Cache ────────────────────────────
    // Separate from history — tracks offline/downloaded file progress by filePath+fileName
    // Never touches Firebase

    private val filePrefs = context.getSharedPreferences("local_file_progress", android.content.Context.MODE_PRIVATE)

    data class FileProgressItem(
        val filePath: String,
        val fileName: String,
        val progressMs: Long,
        val durationMs: Long
    )

    fun saveLocalFileProgress(filePath: String, fileName: String, progressMs: Long, durationMs: Long) {
        val list = getLocalFileProgressList().toMutableList()
        val key = "$filePath::$fileName"
        list.removeAll { "${it.filePath}::${it.fileName}" == key }
        list.add(0, FileProgressItem(filePath, fileName, progressMs, durationMs))
        filePrefs.edit().putString("progress", gson.toJson(list)).apply()
    }

    fun getLocalFileProgress(filePath: String, fileName: String): FileProgressItem? {
        val key = "$filePath::$fileName"
        return getLocalFileProgressList().find { "${it.filePath}::${it.fileName}" == key }
    }

    fun removeLocalFileProgress(filePath: String, fileName: String) {
        val key = "$filePath::$fileName"
        val list = getLocalFileProgressList().toMutableList()
        list.removeAll { "${it.filePath}::${it.fileName}" == key }
        filePrefs.edit().putString("progress", gson.toJson(list)).apply()
    }

    private fun getLocalFileProgressList(): List<FileProgressItem> {
        val json = filePrefs.getString("progress", null) ?: return emptyList()
        return try { gson.fromJson(json, Array<FileProgressItem>::class.java).toList() } catch (_: Exception) { emptyList() }
    }

    // ── Notifications ─────────────────────────────────────────

    suspend fun getNotifications(): List<FirebaseNotification> = withContext(Dispatchers.IO) {
        val doc = userDoc()
        if (doc != null) {
            try {
                val cutoff = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
                val snap = doc.collection("notifications")
                    .whereGreaterThanOrEqualTo("sentAt", cutoff)
                    .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                val items = snap.documents.mapNotNull { d ->
                    val m = d.data ?: return@mapNotNull null
                    FirebaseNotification(
                        id = d.id,
                        title = m["title"] as? String ?: "",
                        body = m["body"] as? String ?: "",
                        sentAt = (m["sentAt"] as? Number)?.toLong() ?: 0,
                        refer = m["refer"] as? String,
                        message = m["message"] as? String,
                        icon = m["icon"] as? String
                    )
                }
                if (items.isNotEmpty()) {
                    return@withContext items
                }
            } catch (_: Exception) {}
        }
        getLocalNotifications()
    }

    fun saveLocalNotification(
        title: String,
        body: String,
        sentAt: Long = System.currentTimeMillis(),
        refer: String? = null,
        message: String? = null,
        icon: String? = null
    ) {
        val list = getLocalNotifications().toMutableList()
        list.removeAll { it.title == title && it.sentAt == sentAt }
        list.add(0, FirebaseNotification(title = title, body = body, sentAt = sentAt, refer = refer, message = message, icon = icon))
        if (list.size > 50) list.subList(50, list.size).clear()
        prefs.edit().putString("notifications", gson.toJson(list)).apply()
    }

    fun getLocalNotifications(): List<FirebaseNotification> {
        val json = prefs.getString("notifications", null) ?: return emptyList()
        val all = try { gson.fromJson(json, Array<FirebaseNotification>::class.java).toList() } catch (_: Exception) { emptyList() }
        val cutoff = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
        val fresh = all.filter { it.sentAt >= cutoff }.sortedByDescending { it.sentAt }.take(50)
        if (fresh.size != all.size) {
            prefs.edit().putString("notifications", gson.toJson(fresh)).apply()
        }
        return fresh
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    // Delete one notification: Firestore doc for logged-in users, local prefs otherwise
    suspend fun deleteNotification(id: String?, title: String, sentAt: Long) = withContext(Dispatchers.IO) {
        val uid = uid()
        if (id != null && uid != null) {
            try {
                db.collection("users").document(uid)
                    .collection("notifications").document(id).delete().await()
                return@withContext
            } catch (_: Exception) {}
        }
        val list = getLocalNotifications().toMutableList()
        list.removeAll { (it.id != null && it.id == id) || (id == null && it.title == title && it.sentAt == sentAt) }
        prefs.edit().putString("notifications", gson.toJson(list)).apply()
    }

    // Delete the user's ENTIRE notification history (Firestore + local)
    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        val uid = uid()
        if (uid != null) {
            try {
                val snap = db.collection("users").document(uid)
                    .collection("notifications").get().await()
                if (snap.documents.isNotEmpty()) {
                    snap.documents.chunked(450).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                }
            } catch (_: Exception) {}
        }
        prefs.edit().remove("notifications").apply()
    }

    // Message-type notification: docId = "notif/{docId}" -> users/{uid}/notifications/{docId}
    suspend fun getNotificationMessage(docId: String): String? = withContext(Dispatchers.IO) {
        val uid = uid() ?: return@withContext null
        val id = docId.removePrefix("notif/")
        try {
            val doc = db.collection("users").document(uid)
                .collection("notifications").document(id).get().await()
            doc.getString("message")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}

// Composables ke liye — same singleton instance access (manual instantiation avoid)
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirebaseRepositoryEntryPoint {
    fun firebaseRepository(): FirebaseRepository
}
