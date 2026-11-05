package com.movie.app.best.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.navOptions

object DrawerLinkRouter {

    fun open(context: Context, navController: NavController, link: String): Boolean {
        val url = link.trim()
        if (url.isEmpty()) return false

        return when {
            url.startsWith("https://", ignoreCase = true) -> openExternal(context, url)
            url.startsWith("app://", ignoreCase = true) -> openInternal(navController, url)
            else -> false
        }
    }

    private fun openExternal(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openInternal(navController: NavController, url: String): Boolean {
        return try {
            navController.navigate(
                Uri.parse(url),
                navOptions { launchSingleTop = true }
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}