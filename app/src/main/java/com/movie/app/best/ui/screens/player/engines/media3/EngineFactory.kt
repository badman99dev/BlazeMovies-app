package com.movie.app.best.ui.screens.player.engines.media3

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.movie.app.best.data.config.RemoteConfig
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.Zee5False404Interceptor
import com.movie.app.best.ui.screens.player.PlayerFactory
import okhttp3.OkHttpClient

/**
 * Builds the Media3 (ExoPlayer) engine for a source.
 *
 * Brother of [com.movie.app.best.ui.screens.player.engines.vlc.VlcEngineFactory].
 */
object Media3EngineFactory {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36"

    fun create(
        context: Context,
        isHls: Boolean,
        trackSelector: DefaultTrackSelector,
        backBufferMs: Int = PlayerFactory.BACK_BUFFER_MS,
        playWhenReady: Boolean = true,
    ): Media3Engine {
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(Zee5False404Interceptor())
            .addInterceptor(DebugInterceptor())
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to RemoteConfig.playerReferer(),
                    "Accept" to "*/*",
                ),
            )

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)
        val mediaSourceFactory = if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory)
        }

        val exo = PlayerFactory.build(
            context = context,
            trackSelector = trackSelector,
            mediaSourceFactory = mediaSourceFactory,
            backBufferMs = backBufferMs,
            playWhenReady = playWhenReady,
        )
        return Media3Engine(exo)
    }
}
