package com.movie.app.best.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Kisi aur app se bheja gaya video (Open with / Share). */
data class ExternalVideo(val uri: String, val title: String)

/**
 * External video intents ko Compose tak le jane ka bridge.
 * MainActivity ise call karta hai; navigation pending item ko player screen pe bhejta hai.
 */
object ExternalVideoOpen {

    var pending: ExternalVideo? by mutableStateOf(null)
        private set

    fun handle(context: Context, intent: Intent?) {
        val i = intent ?: return
        val fromIntent: Uri? = when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        val uri: Uri = fromIntent ?: return

        // Same intent dobara handle na ho (onNewIntent/recreate).
        i.data = null
        i.removeExtra(Intent.EXTRA_STREAM)

        // Kuch apps persistable grant dete hain — mil jaye to le lo.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        pending = ExternalVideo(uri.toString(), displayName(context, uri))
    }

    fun consume() {
        pending = null
    }

    private fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) return cursor.getString(0) ?: ""
                    }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Video"
    }
}