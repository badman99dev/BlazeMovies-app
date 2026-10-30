package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.chat.ChatMessage
import com.movie.app.best.data.chat.ChatReaction
import com.movie.app.best.data.chat.LiveChatState
import com.movie.app.best.ui.screens.player.ui.icons.PersonOutlined
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

private const val MAX_INPUT = 500

private val BgBlack = Color(0xFF000000)
private val SurfaceInput = Color(0xFF18181B)
private val Hairline = Color(0xFF27272A)
private val TextWhite = Color(0xFFF4F4F5)

private fun avatarColors(name: String): Pair<Color, Color> {
    val hash = kotlin.math.abs(name.lowercase(Locale.getDefault()).hashCode())
    return when (hash % 6) {
        0 -> Color(0xFF2563EB) to Color(0xFF1D4ED8)
        1 -> Color(0xFF059669) to Color(0xFF047857)
        2 -> Color(0xFFD97706) to Color(0xFFB45309)
        3 -> Color(0xFF7C3AED) to Color(0xFF6D28D9)
        4 -> Color(0xFFDB2777) to Color(0xFFBE185D)
        else -> Color(0xFF4B5563) to Color(0xFF374151)
    }
}

data class FloatingParticle(
    val id: Long,
    val emoji: String,
    val lane: Float,
    val drift: Float,
    val durationMs: Int
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
    var isSending by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    var guidelinesVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isConnected, state.messages.isEmpty()) {
        if (state.isConnected && state.messages.isEmpty()) {
            // small grace window: join history usually lands right after connect
            delay(800)
            guidelinesVisible = state.isConnected && state.messages.isEmpty()
        } else {
            guidelinesVisible = false
        }
    }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val particles = remember { mutableStateListOf<FloatingParticle>() }

    // Smooth Auto-scroll behavior:
    // If user is at the bottom (index <= 1), auto-scroll to the newest message.
    val isNearBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= totalItems - 2
            }
        }
    }

    var unreadCount by remember { mutableIntStateOf(0) }
    val currentMessagesCount = state.messages.size

    LaunchedEffect(currentMessagesCount) {
        if (currentMessagesCount > 0) {
            if (isNearBottom) {
                // Instant (non-animated) follow while the user is at the bottom:
                // animating every incoming message queues animations and jitters
                // in fast rooms. Smooth animation is reserved for explicit jumps.
                listState.scrollToItem(currentMessagesCount - 1)
                unreadCount = 0
            } else {
                unreadCount += 1
            }
        }
    }

    // Floating reaction receiver
    LaunchedEffect(floatingReactions) {
        floatingReactions.collect { reaction ->
            if (particles.size >= 25) particles.removeAt(0)
            particles.add(
                FloatingParticle(
                    id = System.currentTimeMillis() * 1000 + Random.nextLong(999),
                    emoji = reaction.emoji.ifBlank { "❤️" },
                    lane = Random.nextFloat(),
                    drift = 8f + Random.nextFloat() * 14f,
                    durationMs = 2100 + Random.nextInt(600)
                )
            )
        }
    }

    // Error toast
    LaunchedEffect(state.error) {
        val err = state.error
        if (!err.isNullOrBlank()) {
            toast = err
            delay(3500)
            if (toast == err) toast = null
        }
    }

    val sendAction: () -> Unit = {
        val text = inputText.trim()
        if (text.isNotEmpty() && !isSending) {
            scope.launch {
                isSending = true
                val ok = onSendMessage(text)
                isSending = false
                if (ok) {
                    inputText = ""
                    focusManager.clearFocus()
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                    unreadCount = 0
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // YouTube-style Header
            YouTubeChatHeader(
                onlineCount = state.onlineCount,
                isConnected = state.isConnected,
                isConnecting = state.isConnecting,
                onCloseClick = onCloseClick
            )

            // Main Chat Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Flat Continuous Message Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.seq }) { message ->
                        YouTubeChatMessageItem(message = message)
                    }
                }

                // Empty state: community guidelines — only after the server has
                // confirmed the room is connected and has no messages, otherwise it
                // flashes for a moment before the history arrives.
                if (guidelinesVisible) {
                    EmptyChatGuidelines(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }

                // Floating Reactions Animation Layer (Right edge rising up)
                ReactionParticlesLayer(
                    particles = particles,
                    onFinish = { id -> particles.removeAll { it.id == id } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 48.dp, bottom = 8.dp)
                        .size(width = 80.dp, height = 280.dp)
                )

                // YouTube Right-Side Vertical Floating Reaction Dock (Screenshot 3)
                LiveChatReactionsDock(
                    onSendReaction = { id, emoji ->
                        onSendReaction(id, emoji)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp)
                )

                // Jump to Latest (↓) — floating button with unread badge
                JumpToLatestButton(
                    visible = !isNearBottom,
                    unreadCount = unreadCount,
                    onClick = {
                        scope.launch {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }
                        unreadCount = 0
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )

                // Toast Notification
                if (toast != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE627272A))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = toast ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Input Bar
            YouTubeChatComposer(
                inputText = inputText,
                isSending = isSending,
                onInputChange = { inputText = it.take(MAX_INPUT) },
                onSend = sendAction
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty chat → community guidelines
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyChatGuidelines(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Live chat is quiet right now",
            color = TextWhite,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Say hi and get the conversation started.",
            color = TextWhite.copy(alpha = 0.55f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceInput)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Community guidelines",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                GuidelineLine("Be respectful. No abusive language, hate speech or harassment.")
                GuidelineLine("Never share personal information such as phone numbers, addresses, emails or passwords.")
                GuidelineLine("No spam, ads or self-promotion.")
                GuidelineLine("Keep the chat relevant to the stream.")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Messages that break these rules may be removed and can get you banned.",
                    color = TextWhite.copy(alpha = 0.45f),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GuidelineLine(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "\u2022",
            color = TextWhite.copy(alpha = 0.6f),
            fontSize = 11.5.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = TextWhite.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            lineHeight = 15.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Jump to Latest (standalone composable — keeps AnimatedVisibility out of the
// Box/Column scope overlap that resolves to the wrong overload)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JumpToLatestButton(
    visible: Boolean,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { it },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it },
        modifier = modifier
    ) {
        Box {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF27272A))
                    .border(1.dp, Color(0xFF3F3F46), CircleShape)
                    .clickable(onClick = onClick)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Jump to latest",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-5).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .border(1.5.dp, BgBlack, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// YouTube Header (Compact & Streamlined)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YouTubeChatHeader(
    onlineCount: Int,
    isConnected: Boolean,
    isConnecting: Boolean,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live chat",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConnected && onlineCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFFF3B30), CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = PersonOutlined,
                            contentDescription = "Viewers",
                            tint = Color(0xFFFF3B30).copy(alpha = 0.9f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "$onlineCount",
                            color = Color(0xFFFF3B30),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (isConnecting) "Connecting..." else "Live",
                            color = if (isConnected) Color(0xFFFF3B30) else Color(0xFFA1A1AA),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close chat",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E1E24))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// YouTube Flat Chat Row with Inline Custom Emojis Support
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YouTubeChatMessageItem(message: ChatMessage) {
    val (startColor, endColor) = remember(message.name) { avatarColors(message.name) }
    val initial = remember(message.name) {
        message.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Round Avatar (Screenshot 1 & 3)
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Brush.linearGradient(listOf(startColor, endColor)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        // YouTube Inline Text Stream: @name followed immediately by the message!
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.name.startsWith("@")) message.name else "@${message.name}",
                    color = startColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Plain body — real unicode emoji render natively, no shortcodes
            Text(
                text = message.body,
                color = TextWhite,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// YouTube Chat Input Bar (Input field + Send)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YouTubeChatComposer(
    inputText: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = inputText.trim().isNotEmpty() && !isSending

    // Sit EXACTLY above the keyboard. The window is edge-to-edge
    // (decorFitsSystemWindows = false), so the IME never resizes the window —
    // we lift the bar ourselves by max(navigation bar, keyboard height).
    // WindowInsets.ime is snapshot-backed, so this recomposes the moment the
    // keyboard opens or closes.
    val density = LocalDensity.current
    val bottomLift = maxOf(
        WindowInsets.navigationBars.getBottom(density),
        WindowInsets.ime.getBottom(density)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .padding(bottom = with(density) { bottomLift.toDp() })
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Pill Input Box
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceInput)
                .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Chat...",
                    color = Color(0xFF71717A),
                    fontSize = 13.5.sp
                )
            }

            // Real input field
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(Color.White),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(38.dp)
                .background(if (canSend) Color(0xFF2563EB) else Color(0xFF1E1E24), CircleShape)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else Color(0xFF71717A),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating Reaction Particle Physics
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReactionParticlesLayer(
    particles: List<FloatingParticle>,
    onFinish: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        particles.forEach { particle ->
            key(particle.id) {
                FloatingEmojiParticle(
                    emoji = particle.emoji,
                    lane = particle.lane,
                    drift = particle.drift,
                    durationMs = particle.durationMs,
                    onFinish = { onFinish(particle.id) }
                )
            }
        }
    }
}

@Composable
private fun FloatingEmojiParticle(
    emoji: String,
    lane: Float,
    drift: Float,
    durationMs: Int,
    onFinish: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
        onFinish()
    }

    Text(
        text = emoji,
        fontSize = 24.sp,
        modifier = Modifier
            .offset {
                val p = progress.value
                val sway = sin((p * 6f).toDouble()).toFloat() * drift.dp.toPx()
                IntOffset(
                    x = ((lane - 0.5f) * 60.dp.toPx() + sway).toInt(),
                    y = -(p * 260.dp.toPx()).toInt()
                )
            }
            .graphicsLayer {
                val p = progress.value
                val appear = (p / 0.12f).coerceIn(0f, 1f)
                alpha = appear * (1f - p).coerceIn(0f, 1f)
                val s = 0.8f + p * 0.4f
                scaleX = s
                scaleY = s
                rotationZ = sin((p * 5f).toDouble()).toFloat() * 10f
            }
    )
}
