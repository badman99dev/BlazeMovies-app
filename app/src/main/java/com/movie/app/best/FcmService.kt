package com.movie.app.best

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.movie.app.best.data.repository.FirebaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    companion object {
        const val CHANNEL_ID = "app_alerts"
        const val CHANNEL_ID_SOUND = "app_alerts_sound"
        const val CHANNEL_ID_VIBRATE = "app_alerts_vibrate"
        const val CHANNEL_ID_SILENT = "app_alerts_silent"
        const val TOPIC_BROADCASTS = "app_alerts"
        const val DEEP_LINK_URI = "app://notifications"
        const val MESSAGE_DEEP_LINK = "firestore://message"
        const val LEGACY_INBOX_URI = "notif://notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic(TOPIC_BROADCASTS)
            .addOnCompleteListener {
                android.util.Log.d("FcmService", "Topic subscribe: success=${it.isSuccessful}")
            }
        // Token rotate hua -> handshake: logged-in = uid binding, warna anon (token save rehta hai)
        val loggedIn = firebaseRepository.isLoggedIn()
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.registerFcmToken(anon = !loggedIn)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""

        // Routing: app:// deep links OR firestore://message (Mail-style markdown page)
        val deepLink = message.data["deep_link"] ?: DEEP_LINK_URI
        val refer = message.data["ref"]          // Firestore doc id: "notif/{docId}"
        val markdown = message.data["message"]   // full markdown (message-type pushes)
        val icon = message.data["icon"]          // badge icon: keyword | emoji | https image
        val sound = message.data["sound"] != "0" // "0" = silent, default sound on
        val vibrate = message.data["vibrate"] != "0"

        showNotification(title, body, deepLink, refer, markdown, icon, sound, vibrate)

        // Logged-out users ke liye device-local cache (3-day TTL repository handles)
        if (!firebaseRepository.isLoggedIn()) {
            firebaseRepository.saveLocalNotification(
                title = title,
                body = body,
                refer = if (deepLink == MESSAGE_DEEP_LINK) deepLink else null,
                message = markdown,
                icon = icon
            )
        }
    }

    private fun channelFor(sound: Boolean, vibrate: Boolean): String = when {
        sound && vibrate -> CHANNEL_ID
        !sound && !vibrate -> CHANNEL_ID_SILENT
        !sound -> CHANNEL_ID_VIBRATE
        else -> CHANNEL_ID_SOUND
    }

    private fun showNotification(title: String, body: String, deepLink: String, ref: String?, markdown: String?, icon: String? = null, sound: Boolean = true, vibrate: Boolean = true) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = channelFor(sound, vibrate)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Alerts",
                if (channelId == CHANNEL_ID_SILENT) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "App broadcasts and alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (ref != null) putExtra("ref", ref)
            if (markdown != null) putExtra("message", markdown)
            if (icon != null) putExtra("icon", icon)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (channelId == CHANNEL_ID_SILENT) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notifId, notification)
    }
}