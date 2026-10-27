package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.chat.ChatMessage
import com.movie.app.best.data.chat.ChatReaction
import com.movie.app.best.data.chat.LiveChatState
import com.movie.app.best.ui.theme.AppRed
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

private val USER_COLORS = listOf(
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFEAB308),
    Color(0xFF22C55E),
    Color(0xFF06B6D4),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899)
)

private fun getUserColor(name: String): Color {
    val hash = kotlin.math.abs(name.hashCode())
    return USER_COLORS[hash % USER_COLORS.size]
}

data class FloatingParticle(
    val id: Long,
    val emoji: String,
    val startX: Float
)

@Composable
fun LiveChatSheet(
    state: LiveChatState,
    floatingReactions: SharedFlow<ChatReaction>,
    currentUserName: String?,
    onCloseClick: () -> Unit,
    onSendMessage: suspend (String) -> Boolean,
    onSendReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var particles by remember { mutableStateOf<List<FloatingParticle>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    // Only clear the composer once the server actually accepted the message.
    val sendCurrent: () -> Unit = {
        val text = inputText.trim()
        if (text.isNotEmpty() && !isSending) {
            scope.launch {
                isSending = true
                val ok = onSendMessage(text)
                isSending = false
                if (ok) {
                    inputText = ""
                    focusManager.clearFocus()
                }
            }
        }
    }

    LaunchedEffect(floatingReactions) {
        floatingReactions.collect { reaction ->
            val newP = FloatingParticle(
                id = System.currentTimeMillis() + Random.nextLong(1000),
                emoji = reaction.emoji.ifBlank { "❤️" },
                startX = Random.nextFloat() * 60f
            )
            particles = (particles + newP).takeLast(25)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val reactionOptions = remember {
        listOf(
            "heart" to "❤️",
            "fire" to "🔥",
            "lol" to "😂",
            "clap" to "👏",
            "party" to "🎉"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10141D))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live chat",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${state.onlineCount} Online",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Live Chat",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color.White.copy(alpha = 0.07f)
                )

                if (state.error != null && state.messages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.35f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            // Message Feed & Floating reactions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.error != null) {
                            Text(
                                text = state.error ?: "",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        } else if (state.isConnecting) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = AppRed,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Connecting to live chat...",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Welcome to Live Chat! Say hi 👋",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.messages, key = { it.seq }) { message ->
                            ChatMessageRow(
                                message = message,
                                isMe = state.currentUserId?.let { it == message.userId }
                                    ?: message.name.equals(currentUserName, ignoreCase = true)
                            )
                        }
                    }
                }

                // Floating Particles Layer
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(90.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 60.dp, end = 12.dp)
                ) {
                    particles.forEach { p ->
                        FloatingEmojiBubble(
                            key = p.id,
                            emoji = p.emoji,
                            offsetX = p.startX,
                            onFinish = {
                                particles = particles.filterNot { it.id == p.id }
                            }
                        )
                    }
                }

                // Quick Reaction Rail (Right side)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .background(Color(0xFF171B24).copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reactionOptions.forEach { (id, emoji) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSendReaction(id, emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Bottom Composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10141D))
            ) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color.White.copy(alpha = 0.08f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = currentUserName?.firstOrNull()?.uppercaseChar()?.toString() ?: "F"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1B202C))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Chat as ${currentUserName ?: "you"}...",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it.take(300) },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(AppRed),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { sendCurrent() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = inputText.trim().isNotEmpty() && !isSending && !state.isConnecting
                    IconButton(
                        onClick = sendCurrent,
                        enabled = canSend,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (canSend) AppRed else Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    isMe: Boolean
) {
    val initial = message.name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val avatarBg = getUserColor(message.name)
    val timeStr = remember(message.ts) {
        if (message.ts > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.ts))
        } else ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${message.name}",
                    color = avatarBg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(YOU)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (timeStr.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = message.body,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun FloatingEmojiBubble(
    key: Long,
    emoji: String,
    offsetX: Float,
    onFinish: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(key) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
        )
        onFinish()
    }

    val progress = animProgress.value
    val yOffset = -progress * 240f
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val scale = 0.6f + progress * 0.7f

    Text(
        text = emoji,
        fontSize = 24.sp,
        modifier = Modifier
            .offset(x = offsetX.dp, y = yOffset.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
    )
}
