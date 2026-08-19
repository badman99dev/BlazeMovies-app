package com.movie.app.best.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.model.FirebaseNotification
import com.movie.app.best.ui.theme.AppRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (FirebaseNotification) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        com.movie.app.best.ui.components.CompactPageHeader(
            title = "Notifications",
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { viewModel.loadNotifications() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppRed)
                }
            }
            uiState.notifications.isNotEmpty() -> {
                NotificationList(
                    notifications = uiState.notifications,
                    onNotificationClick = onNotificationClick
                )
            }
            else -> {
                EmptyNotificationState()
            }
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<FirebaseNotification>,
    onNotificationClick: (FirebaseNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications, key = { "${it.title}-${it.sentAt}" }) { notification ->
            NotificationRow(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: FirebaseNotification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161616))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (notification.isMessageType) AppRed.copy(alpha = 0.25f) else AppRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (notification.isMessageType) Icons.Default.Mail else Icons.Default.Notifications,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (notification.isMessageType) {
                    Text(
                        text = "✉️ READ",
                        color = AppRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = formatRelativeTime(notification.sentAt),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp
                )
            }
            if (notification.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hrs ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun EmptyNotificationState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No notifications yet",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "When there's a broadcast, it will show up here",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
