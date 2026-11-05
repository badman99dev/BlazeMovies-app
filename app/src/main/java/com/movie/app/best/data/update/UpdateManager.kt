package com.movie.app.best.data.update

import android.content.Context
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
 * CACHE: isme koi cache nahi hai (jaan-boojh kar). Har boot / resume / manual
 * check pe hamesha fresh network call jaati hai, taaki admin ke maintenance /
 * force-update / version rules turant asar karein. Sirf in-flight calls dedupe
 * hote hain (mutex).
 *
 * - boot / settings / resume sab yahi se guzarte hain
 * - ek hi network call (in-flight dedupe), koi TTL nahi
 * - offline/error pe session ka last known response use hota hai
 * - 24h prompt throttle prefs me hai (wo sirf normal-update dialog ka nagging
 *   control karta hai — maintenance/force ko nahi rokta)
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MovieRepository
) {

    private companion object {
        const val PREFS = "update_cache"
        const val KEY_PROMPT_AT = "last_prompt_time"
        const val KEY_PROMPT_VERSION = "last_prompt_version"

        const val PROMPT_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Sirf session ke liye, disk pe kuch save nahi hota. */
    private var lastData: UpdateResponse? = null

    init {
        // Pehle wale cached response ko ek baar saaf kar do — cache hat gaya hai.
        prefs.edit().remove("last_response").remove("last_check_at").apply()
    }

    fun current(): UpdateResponse? = lastData

    /** Boot pe — call karne wala block nahi hota, hamesha fresh fetch. */
    fun checkInBackground(reason: String, force: Boolean = false) {
        scope.launch { check(reason, force) }
    }

    /** Resume pe bhi hamesha fresh check (koi stale window nahi). */
    fun checkIfStale(reason: String) = checkInBackground(reason)

    /**
     * Result: latest [UpdateResponse] (error pe session ka last known).
     * Null tab hi jab kabhi kuch nahi mila.
     */
    suspend fun check(reason: String, force: Boolean = false): UpdateResponse? {
        // In-flight dedupe — ek waqt me ek hi call.
        if (!mutex.tryLock()) {
            return lastData
        }
        return try {
            // Purana response ho to UI ko wahi dikhate raho (flicker nahi),
            // warna loading state.
            if (lastData == null) _state.value = UpdateState.Checking
            when (val result = repository.checkForUpdate(BuildConfig.VERSION_CODE)) {
                is Resource.Success -> {
                    val data = result.data
                    if (data != null) {
                        lastData = data
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
        // Purana response ho to UI wahi dikhata rahe (screen flash na ho),
        // fresh response aate hi replace ho jayega.
        if (lastData == null) _state.value = UpdateState.Idle
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
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()

    /** Backend ka fresh jawab. UI isi ke flags pe branch karta hai. */
    data class Ready(val data: UpdateResponse, val promptNow: Boolean) : UpdateState()

    data class Failed(val message: String) : UpdateState()
}
