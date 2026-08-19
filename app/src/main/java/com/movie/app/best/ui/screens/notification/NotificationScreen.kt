package com.movie.app.best.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.movie.app.best.data.model.FirebaseNotification
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.theme.AppRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (FirebaseNotification) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = "Notifications",
            onBackClick = onBackClick,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.notifications.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            color = AppRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showClearDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        )

        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppRed)
                }
            }
            else -> {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = uiState.isLoading),
                    onRefresh = { viewModel.loadNotifications() },
                    indicator = { state, trigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = 80.dp,
                            backgroundColor = Color(0xFF1A1A1A),
                            contentColor = AppRed
                        )
                    }
                ) {
                    if (uiState.notifications.isNotEmpty()) {
                        NotificationList(
                            notifications = uiState.notifications,
                            onNotificationClick = onNotificationClick,
                            onDelete = { viewModel.deleteNotification(it) }
                        )
                    } else {
                        EmptyNotificationState()
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.75f),
            title = { Text("Clear all notifications?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently delete your entire notification history. " +
                        "This action cannot be undone. Are you sure you want to continue?",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllNotifications()
                    }
                ) {
                    Text("Clear All", color = AppRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun NotificationList(
    notifications: List<FirebaseNotification>,
    onNotificationClick: (FirebaseNotification) -> Unit,
    onDelete: (FirebaseNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications, key = { "${it.title}-${it.sentAt}" }) { notification ->
            NotificationRow(
                notification = notification,
                onClick = { onNotificationClick(notification) },
                onDelete = { onDelete(notification) }
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: FirebaseNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161616))
            .clickable { onClick() }
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        NotificationIconBadge(notification)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(19.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

// ── Icon badge: icon field se control (keyword / emoji / https image / auto) ──
@Composable
private fun NotificationIconBadge(notification: FirebaseNotification) {
    val icon = notification.icon?.trim().orEmpty()
    val boxModifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(10.dp))
    when {
        icon.isEmpty() -> Box(
            modifier = boxModifier.background(
                if (notification.isMessageType) AppRed.copy(alpha = 0.25f) else AppRed.copy(alpha = 0.15f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (notification.isMessageType) Icons.Default.Mail else Icons.Default.Notifications,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(19.dp)
            )
        }
        icon.startsWith("http", ignoreCase = true) -> {
            val bg = iconBackgroundColor(notification.title, notification.isMessageType)
            Box(
                modifier = boxModifier.background(bg.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        else -> iconKeywordIcon(icon)?.let { keyword ->
            Box(
                modifier = boxModifier.background(if (notification.isMessageType) AppRed.copy(alpha = 0.25f) else AppRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = keyword,
                    contentDescription = null,
                    tint = AppRed,
                    modifier = Modifier.size(19.dp)
                )
            }
        } ?: Box(
            modifier = boxModifier.background(if (notification.isMessageType) AppRed.copy(alpha = 0.25f) else AppRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = AppRed,
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}

private fun iconKeywordIcon(keyword: String) = when (keyword.lowercase(Locale.ROOT)) {
    "mail" -> Icons.Default.Mail
    "bell" -> Icons.Default.Notifications
    "gear" -> Icons.Default.Settings
    "update" -> Icons.Default.Autorenew
    "android" -> Icons.Default.Android
    "bot" -> Icons.Default.SmartToy
    "party" -> Icons.Default.Celebration
    else -> null
}

// Deterministic color per title → low-opacity background behind square-fit images (YouTube style)
private fun iconBackgroundColor(title: String, isMessage: Boolean): Color {
    val palette = listOf(
        Color(0xFFE50914), Color(0xFFFF6B35), Color(0xFFFFC107),
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0),
        Color(0xFF00BCD4), Color(0xFFF06292)
    )
    val h = if (title.isEmpty()) 0 else title.hashCode().toPositive()
    return palette[h % palette.size]
}

private fun Int.toPositive(): Int = if (this == Int.MIN_VALUE) 0 else abs(this)

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