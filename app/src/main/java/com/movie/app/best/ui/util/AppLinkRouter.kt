package com.movie.app.best.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.movie.app.best.data.model.PromoAction

/**
 * Shared promo/notification link handling: `url` opens the browser, `deeplink` is handed
 * to the in-app navigator (the app:// registry in AppNavigation).
 */
object AppLinkRouter {

    fun handle(context: Context, action: PromoAction, navigate: (String) -> Unit): Boolean {
        if (action.isNone) return false
        return when {
            action.isUrl -> {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.value)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.isSuccess
            }
            action.isDeeplink -> runCatching { navigate(action.value) }.isSuccess
            else -> false
        }
    }
}
