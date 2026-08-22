package com.movie.app.best

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.movie.app.best.data.settings.VideoQualitySettings
import com.ketch.Ketch
import com.ketch.NotificationConfig
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.toast
import org.acra.config.dialog
import org.acra.ktx.initAcra
import org.acra.data.StringFormat

@HiltAndroidApp
class MovieApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    lateinit var ketch: Ketch
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(300)
            .build()
    }

    private fun isAcraProcess(): Boolean {
        val processName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.app.Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            (getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                .runningAppProcesses?.find { it.pid == pid }?.processName ?: packageName
        }
        return processName.endsWith(":acra")
    }

    override fun onCreate() {
        super.onCreate()

        VideoQualitySettings.initCache(this)

        createNotificationChannel()

        if (!isAcraProcess()) {
            ketch = Ketch.builder()
                .setNotificationConfig(
                    NotificationConfig(
                        enabled = true,
                        smallIcon = android.R.drawable.stat_sys_download,
                        showSpeed = true,
                        showSize = true,
                        showTime = true
                    )
                ).build(this)

            Thread { CrashPasteManager.ensurePasteExists(this) }.start()

            // FCM topic subscribe (safety — onNewToken ke liye wait nahi karna)
            Thread {
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        .subscribeToTopic(FcmService.TOPIC_BROADCASTS)
                } catch (_: Exception) {}
            }.start()
        }

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST

            toast {
                text = "Crash report Tempserv pe bhej raha hoon... 📤"
                length = android.widget.Toast.LENGTH_LONG
            }

            dialog {
                title = "Crash Report"
                text = "App crash ho gaya! Report Tempserv pe upload ho raha hai.\n\nLink copy karke developer ko bhej do."
                commentPrompt = "Kya kar rahe the jab crash hua? (optional)"
                positiveButtonText = "OK"
                negativeButtonText = "Close"
                resTheme = android.R.style.Theme_DeviceDefault_Light_Dialog
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            val defaultChannel = android.app.NotificationChannel(
                FcmService.CHANNEL_ID,
                "App Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "App broadcasts and alerts (sound + vibration)"
                enableVibration(true)
            }
            nm.createNotificationChannel(defaultChannel)

            val soundOnlyChannel = android.app.NotificationChannel(
                FcmService.CHANNEL_ID_SOUND,
                "App Alerts — Sound Only",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sound plays, no vibration"
                enableVibration(false)
            }
            nm.createNotificationChannel(soundOnlyChannel)

            val vibrateOnlyChannel = android.app.NotificationChannel(
                FcmService.CHANNEL_ID_VIBRATE,
                "App Alerts — Vibration Only",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vibrates, no sound"
                setSound(null, null)
                enableVibration(true)
            }
            nm.createNotificationChannel(vibrateOnlyChannel)

            val silentChannel = android.app.NotificationChannel(
                FcmService.CHANNEL_ID_SILENT,
                "App Alerts — Silent",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "No sound, no vibration"
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(silentChannel)
        }
    }
}
