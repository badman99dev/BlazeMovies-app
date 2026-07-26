package com.movie.app.best.ui.screens.notification

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.model.WasmerNotification
import com.movie.app.best.ui.theme.WasmerRed

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onBtnLinkClick: (String) -> Unit = {},
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
            onBackClick = onBackClick
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WasmerRed)
                }
            }
            uiState.notification != null -> {
                val notif = uiState.notification!!
                ActiveNotificationCard(
                    notification = notif,
                    onBtnLinkClick = onBtnLinkClick
                )
            }
            else -> {
                EmptyNotificationState()
            }
        }
    }
}

@Composable
private fun ActiveNotificationCard(
    notification: WasmerNotification,
    onBtnLinkClick: (String) -> Unit
) {
    val accentColor = when (notification.type) {
        "critical" -> Color(0xFFFF5252)
        "alert" -> Color(0xFFFFD740)
        "success" -> Color(0xFF69F0AE)
        else -> Color(0xFF448AFF)
    }
    val accentBg = accentColor.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accentBg, Color(0xFF1A1A1A))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = notification.type.replaceFirstChar { it.uppercase() },
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "LIVE",
                        color = WasmerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = notification.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )

                if (notification.btnText.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            if (notification.btnLink.isNotBlank()) {
                                onBtnLinkClick(notification.btnLink)
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = notification.btnText,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
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
                text = "When there's an alert, it will show up here",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
