package com.movie.app.best.data.update

import android.content.Context
import com.google.gson.Gson
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Update check ka single source of truth.
 *
 * ABI aur device details OkHttp interceptor (X-App-Abi*) se jaate hain — backend
 * best APK ka link khud resolve karke `download_url` me deta hai, client kuch
 * decide nahi karta.
 *
 * - boot / settings / resume sab yahi se guzarte hain
 * - ek hi network call (dedupe) + 15 min TTL
 * - offline/error pe last known response se kaam chalta hai
 * - 24h prompt throttle manager me hai, UI me nahi
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MovieRepository
) {

    private companion object {
        const val PREFS = "update_cache"
        const val KEY_JSON = "last_response"
        const val KEY_CHECKED_AT = "last_check_at"
        const val KEY_PROMPT_AT = "last_prompt_time"
        const val KEY_PROMPT_VERSION = "last_prompt_version"

        const val TTL_MS = 15 * 60 * 1000L
        const val STALE_MS = 6 * 60 * 60 * 1000L
        const val PROMPT_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var lastData: UpdateResponse? = null
    private var lastCheckAt: Long = 0L

    init {
        restore()
    }

    fun current(): UpdateResponse? = lastData

    /** Boot pe — call karne wala block nahi hota. */
    fun checkInBackground(reason: String, force: Boolean = false) {
        scope.launch { check(reason, force) }
    }

    /** Sirf tab call karo jab last check [STALE_MS] se purana ho (resume pe). */
    fun checkIfStale(reason: String) {
        if (System.currentTimeMillis() - lastCheckAt < STALE_MS) return
        checkInBackground(reason)
    }

    /**
     * Result: latest [UpdateResponse] (cache/error pe last known).
     * Null tab hi jab kabhi kuch nahi mila.
     */
    suspend fun check(reason: String, force: Boolean = false): UpdateResponse? {
        if (!force && System.currentTimeMillis() - lastCheckAt < TTL_MS && lastData != null) {
            publish(lastData!!)
            return lastData
        }
        if (!mutex.tryLock()) {
            // Koi aur check chal raha hai — usi ka result use kar lo.
            return lastData
        }
        return try {
            _state.value = UpdateState.Checking
            when (val result = repository.checkForUpdate(BuildConfig.VERSION_CODE)) {
                is Resource.Success -> {
                    val data = result.data
                    if (data != null) {
                        save(data)
                        publish(data)
                        data
                    } else {
                        fallback("Empty update response", reason)
                    }
                }
                is Resource.Error -> fallback(result.error ?: "Network error", reason)
                is Resource.Loading -> lastData
            }
        } catch (e: Exception) {
            fallback(e.message ?: "Update check failed", reason)
        } finally {
            mutex.unlock()
        }
    }

    fun promptAllowed(versionCode: Int): Boolean {
        if (versionCode <= 0) return false
        val lastVersion = prefs.getInt(KEY_PROMPT_VERSION, 0)
        if (lastVersion != versionCode) return true
        return System.currentTimeMillis() - prefs.getLong(KEY_PROMPT_AT, 0L) >= PROMPT_INTERVAL_MS
    }

    fun markPrompted(versionCode: Int) {
        prefs.edit()
            .putInt(KEY_PROMPT_VERSION, versionCode)
            .putLong(KEY_PROMPT_AT, System.currentTimeMillis())
            .apply()
    }

    /** Force-update / maintenance screen ke retry ke liye. */
    fun resetAvailability() {
        _state.value = UpdateState.Idle
        checkInBackground("retry", force = true)
    }

    private fun fallback(message: String, reason: String): UpdateResponse? {
        val cached = lastData
        if (cached != null) {
            publish(cached)
        } else {
            _state.value = UpdateState.Failed(message)
        }
        android.util.Log.w("UpdateManager", "check($reason) failed: $message")
        return cached
    }

    private fun publish(data: UpdateResponse) {
        _state.value = UpdateState.Ready(
            data = data,
            promptNow = promptAllowed(data.versionCode)
        )
    }

    private fun save(data: UpdateResponse) {
        lastData = data
        lastCheckAt = System.currentTimeMillis()
        try {
            prefs.edit()
                .putString(KEY_JSON, gson.toJson(data))
                .putLong(KEY_CHECKED_AT, lastCheckAt)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun restore() {
        lastCheckAt = prefs.getLong(KEY_CHECKED_AT, 0L)
        val raw = prefs.getString(KEY_JSON, null) ?: return
        try {
            val data = gson.fromJson(raw, UpdateResponse::class.java) ?: return
            lastData = data
            _state.value = UpdateState.Ready(data, promptNow = false)
        } catch (_: Exception) {
        }
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()

    /** Backend ka fresh (ya cached) jawab. UI isi ke flags pe branch karta hai. */
    data class Ready(val data: UpdateResponse, val promptNow: Boolean) : UpdateState()

    data class Failed(val message: String) : UpdateState()
}