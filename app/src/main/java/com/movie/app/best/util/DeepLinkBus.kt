package com.movie.app.best.util

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bridges intents received in [com.movie.app.best.MainActivity.onNewIntent] to
 * Compose. When the activity is already RESUMED (e.g. split screen), the
 * lifecycle observer never sees a new ON_RESUME, so the App Link would be
 * dropped — this bus delivers it straight to MainScreen.
 */
object DeepLinkBus {

    var pending: String? by mutableStateOf(null)
        private set

    private val ACCEPTED_SCHEMES = setOf("http", "https", "app", "notif", "firestore")

    fun offer(intent: Intent?) {
        val i = intent ?: return
        if (i.action != Intent.ACTION_VIEW) return
        val data = i.data ?: return
        val scheme = data.scheme?.lowercase() ?: return
        if (scheme !in ACCEPTED_SCHEMES) return
        pending = data.toString()
    }

    fun consume() {
        pending = null
    }
}