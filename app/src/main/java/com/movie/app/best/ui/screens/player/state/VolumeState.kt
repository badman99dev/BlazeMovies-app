package com.movie.app.best.ui.screens.player.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.Player

@Composable
fun rememberVolumeState(player: Player?): VolumeState {
    val context = LocalContext.current
    val volumeState = remember(player) { VolumeState(player, context) }
    DisposableEffect(context) { volumeState.handleLifecycle(this) }
    return volumeState
}

@Stable
class VolumeState(
    private val player: Player?,
    private val context: Context,
) {
    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)
        ?: throw IllegalStateException("AudioManager not available")
    private val systemMaxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    val maxVolumePercentage: Int = 100

    var currentVolume: Int by mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        private set

    var volumePercentage: Int by mutableIntStateOf(calculateVolumePercentage())
        private set

    /** Continuous 0f..1f level so the gesture indicator fills smoothly instead of
     *  snapping to the coarse system volume steps (e.g. 0, 6, 13, 20 ...). */
    var volumeFraction: Float by mutableFloatStateOf(calculateVolumePercentage() / 100f)
        private set

    private var lastSetVolume: Int = -1

    fun updateVolumePercentage(percentage: Int) {
        val clampedPercentage = percentage.coerceIn(0, maxVolumePercentage)
        volumeFraction = (clampedPercentage / 100f).coerceIn(0f, 1f)
        val targetVolume = (volumeFraction * systemMaxVolume).toInt().coerceIn(0, systemMaxVolume)
        setVolume(targetVolume, keepFraction = true)
    }

    fun increaseVolume() { setVolume(currentVolume + 1) }
    fun decreaseVolume() { setVolume(currentVolume - 1) }

    fun handleLifecycle(disposableEffectScope: DisposableEffectScope): DisposableEffectResult =
        with(disposableEffectScope) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == VOLUME_CHANGED_ACTION) {
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        volumePercentage = calculateVolumePercentage()
                        if (currentVolume != lastSetVolume) {
                            volumeFraction = currentVolume.toFloat() / systemMaxVolume
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(VOLUME_CHANGED_ACTION), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(VOLUME_CHANGED_ACTION))
            }
            onDispose { context.unregisterReceiver(receiver) }
        }

    private fun setVolume(volume: Int, keepFraction: Boolean = false) {
        val clampedVolume = volume.coerceIn(0, systemMaxVolume)
        currentVolume = clampedVolume
        lastSetVolume = clampedVolume
        volumePercentage = calculateVolumePercentage()
        if (!keepFraction) {
            volumeFraction = clampedVolume.toFloat() / systemMaxVolume
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clampedVolume, 0)
    }

    private fun calculateVolumePercentage(): Int {
        return (currentVolume.toFloat() / systemMaxVolume * 100).toInt()
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
