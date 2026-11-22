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
        val mime = i.type?.lowercase().orEmpty()
        val fromIntent: Uri? = when (i.action) {
            // "Open with" → only local files. http(s) App Links are handled by
            // the deep-link router; never treat them as videos here.
            Intent.ACTION_VIEW -> {
                if (mime.isNotEmpty() && !mime.startsWith("video/")) return
                val data = i.data ?: return
                val scheme = data.scheme?.lowercase()
                if (scheme != "content" && scheme != "file") return
                data
            }
            // Share sheet → only explicit video shares.
            Intent.ACTION_SEND -> {
                if (!mime.startsWith("video/")) return
                @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
            }
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