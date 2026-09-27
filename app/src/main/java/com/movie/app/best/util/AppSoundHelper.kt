package com.movie.app.best.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.movie.app.best.R

object AppSoundHelper {
    private var soundPool: SoundPool? = null
    private var likeSoundId: Int = 0
    private var unlikeSoundId: Int = 0
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    likeSoundId = load(context.applicationContext, R.raw.pop_like, 1)
                    unlikeSoundId = load(context.applicationContext, R.raw.pop_unlike, 1)
                }
            isInitialized = true
        } catch (_: Exception) {
            // graceful fallback
        }
    }

    fun playPopSound(context: Context, isPositive: Boolean = true) {
        if (!isInitialized) {
            init(context)
        }
        try {
            val soundId = if (isPositive) likeSoundId else unlikeSoundId
            if (soundId != 0) {
                soundPool?.play(soundId, 0.85f, 0.85f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun triggerHapticFeedback(context: Context, isPositive: Boolean = true) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = if (isPositive) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isPositive) 18L else 10L
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isPositive) 18L else 10L)
            }
        } catch (_: Exception) {}
    }
}
