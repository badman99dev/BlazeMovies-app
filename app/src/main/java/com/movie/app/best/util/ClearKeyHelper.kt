package com.movie.app.best.util

import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.MediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import java.util.UUID

@OptIn(UnstableApi::class)
object ClearKeyHelper {

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "").trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun buildClearKeyJson(keyPairString: String): String? {
        return try {
            val parts = keyPairString.split(":")
            if (parts.size != 2) return null
            val kid = parts[0].trim()
            val k = parts[1].trim()

            val kidB64 = base64UrlEncode(hexToBytes(kid))
            val kB64 = base64UrlEncode(hexToBytes(k))

            """{"keys":[{"kty":"oct","k":"$kB64","kid":"$kidB64"}],"type":"temporary"}"""
        } catch (_: Exception) {
            null
        }
    }

    class StaticClearKeyCallback(private val responseJson: String) : MediaDrmCallback {
        private val responseBytes = responseJson.toByteArray(Charsets.UTF_8)
        override fun executeProvisionRequest(uuid: UUID, request: androidx.media3.exoplayer.drm.ExoMediaDrm.ProvisionRequest): ByteArray = ByteArray(0)
        override fun executeKeyRequest(uuid: UUID, request: androidx.media3.exoplayer.drm.ExoMediaDrm.KeyRequest): ByteArray = responseBytes
    }

    fun buildDrmSessionManager(keyPairString: String): DrmSessionManager? {
        val json = buildClearKeyJson(keyPairString) ?: return null
        val callback = StaticClearKeyCallback(json)
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .setMultiSession(false)
            .build(callback)
    }

    fun buildHttpDataSourceFactory(headers: Map<String, String>?): DataSource.Factory {
        val builder = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        headers?.let { map ->
            val cleanHeaders = mutableMapOf<String, String>()
            map.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) {
                    if (k.equals("User-Agent", ignoreCase = true)) {
                        builder.setUserAgent(v)
                    } else {
                        cleanHeaders[k] = v
                    }
                }
            }
            if (cleanHeaders.isNotEmpty()) {
                builder.setDefaultRequestProperties(cleanHeaders)
            }
        }
        return builder
    }

    fun createMediaSource(
        url: String,
        headers: Map<String, String>? = null,
        drmKey: String? = null,
        isDash: Boolean = false,
        isHls: Boolean = false
    ): MediaSource {
        val dataSourceFactory = buildHttpDataSourceFactory(headers)
        val mediaItem = MediaItem.fromUri(url)
        val drmManager = if (!drmKey.isNullOrBlank()) buildDrmSessionManager(drmKey) else null

        val useDash = isDash || url.contains(".mpd", ignoreCase = true)
        val useHls = isHls || url.contains(".m3u8", ignoreCase = true)

        return when {
            useDash -> {
                val factory = DashMediaSource.Factory(dataSourceFactory)
                if (drmManager != null) factory.setDrmSessionManagerProvider { drmManager }
                factory.createMediaSource(mediaItem)
            }
            useHls -> {
                val factory = HlsMediaSource.Factory(dataSourceFactory)
                if (drmManager != null) factory.setDrmSessionManagerProvider { drmManager }
                factory.createMediaSource(mediaItem)
            }
            else -> {
                val factory = DefaultMediaSourceFactory(dataSourceFactory)
                if (drmManager != null) factory.setDrmSessionManagerProvider { drmManager }
                factory.createMediaSource(mediaItem)
            }
        }
    }
}
