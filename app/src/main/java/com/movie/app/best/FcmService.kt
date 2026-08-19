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
        const val CHANNEL_ID = "blazemovies_alerts"
        const val TOPIC_BROADCASTS = "blazemovies_alerts"
        const val DEEP_LINK_URI = "app://notifications"
        const val MESSAGE_DEEP_LINK = "firestore://message"
        const val LEGACY_INBOX_URI = "blazemovies://notifications"
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

        showNotification(title, body, deepLink, refer, markdown)

        // Logged-out users ke liye device-local cache (3-day TTL repository handles)
        if (!firebaseRepository.isLoggedIn()) {
            firebaseRepository.saveLocalNotification(
                title = title,
                body = body,
                refer = if (deepLink == MESSAGE_DEEP_LINK) deepLink else null,
                message = markdown
            )
        }
    }

    private fun showNotification(title: String, body: String, deepLink: String, ref: String?, markdown: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BlazeMovies Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "App broadcasts and alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (ref != null) putExtra("ref", ref)
            if (markdown != null) putExtra("message", markdown)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notifId, notification)
    }
}