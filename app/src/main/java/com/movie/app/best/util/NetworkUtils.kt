package com.movie.app.best.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.PlaybackException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Checks if the playback failure was caused by the device losing its internet connection
     * (e.g. Wi-Fi turned off, mobile data lost, airplane mode).
     *
     * When offline, the server is NOT at fault, so we must never trigger a server failover.
     */
    fun isDeviceOffline(context: Context, error: PlaybackException? = null): Boolean {
        if (!isOnline(context)) {
            return true
        }

        if (error != null) {
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            ) {
                return true
            }

            var cause: Throwable? = error.cause
            while (cause != null) {
                if (cause is UnknownHostException ||
                    cause is ConnectException ||
                    cause is NoRouteToHostException ||
                    cause is SocketTimeoutException ||
                    cause is SocketException
                ) {
                    return true
                }
                cause = cause.cause
            }
        }

        return false
    }
}
