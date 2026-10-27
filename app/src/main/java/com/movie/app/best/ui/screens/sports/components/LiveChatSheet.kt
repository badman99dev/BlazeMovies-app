package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.chat.ChatMessage
import com.movie.app.best.data.chat.ChatReaction
import com.movie.app.best.data.chat.LiveChatState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

private const val MAX_INPUT = 500
private const val GROUP_WINDOW_MS = 5 * 60 * 1000L

private val BgBlack = Color(0xFF000000)
private val Surface1 = Color(0xFF0D0D0F)
private val Surface2 = Color(0xFF141416)
private val Surface3 = Color(0xFF1C1C1F)
private val Hairline = Color(0xFF1F1F23)
private val HandleGrey = Color(0xFF9A9AA0)
private val TimeGrey = Color(0xFF5C5C63)
private val TextWhite = Color(0xFFF5F5F7)

private fun avatarBrush(name: String): Brush {
    val hash = kotlin.math.abs(name.lowercase(Locale.getDefault()).hashCode())
    val start = when (hash % 3) {
        0 -> Color(0xFF3F3F46)
        1 -> Color(0xFF4A4A52)
        else -> Color(0xFF35353B)
    }
    return Brush.linearGradient(listOf(start, Color(0xFF1A1A1D)))
}

data class FloatingParticle(
    val id: Long,
    val emoji: String,
    val lane: Float,
    val drift: Float,
    val durationMs: Int
)

private data class ChatRow(
    val message: ChatMessage,
    val isMe: Boolean,
    val showHeader: Boolean
)

private fun buildChatRows(
    messages: List<ChatMessage>,
    currentUserId: String?,
    currentUserName: String?
): List<ChatRow> {
    if (messages.isEmpty()) return emptyList()
    val unique = messages.distinctBy { it.seq }
    val rows = ArrayList<ChatRow>(unique.size)
    var prevUserId = ""
    var prevTs = 0L
    for (m in unique) {
        val isMe = if (!currentUserId.isNullOrBlank() && m.userId.isNotBlank()) {
            m.userId == currentUserId
        } else {
            m.name.equals(currentUserName ?: "", ignoreCase = true)
        }
        val newGroup = m.userId != prevUserId || (m.ts - prevTs) > GROUP_WINDOW_MS
        rows += ChatRow(m, isMe, newGroup)
        prevUserId = m.userId
        prevTs = m.ts
    }
    return rows.asReversed()
}

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
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val particles = remember { mutableStateListOf<FloatingParticle>() }

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
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(floatingReactions) {
        floatingReactions.collect { reaction ->
            if (particles.size >= 18) particles.removeAt(0)
            particles.add(
                FloatingParticle(
                    id = System.currentTimeMillis() * 1000 + Random.nextLong(999),
                    emoji = reaction.emoji.ifBlank { "❤️" },
                    lane = Random.nextFloat(),
                    drift = 6f + Random.nextFloat() * 12f,
                    durationMs = 2000 + Random.nextInt(700)
                )
            )
        }
    }

    LaunchedEffect(state.error) {
        val err = state.error
        if (!err.isNullOrBlank()) {
            toast = err
            delay(3600)
            if (toast == err) toast = null
        }
    }

    val rows = remember(state.messages, state.currentUserId, currentUserName) {
        buildChatRows(state.messages, state.currentUserId, currentUserName)
    }

    val showJump by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
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
            .background(BgBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(state = state, onCloseClick = onCloseClick)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 14.dp)
                ) {
                    items(rows, key = { it.message.seq }) { row ->
                        ChatMessageRow(row)
                    }
                }

                ReactionLayer(
                    particles = particles,
                    onFinish = { id -> particles.removeAll { it.id == id } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 8.dp)
                        .size(width = 96.dp, height = 240.dp)
                )

                ChatEmptyOverlay(
                    visible = rows.isEmpty() && !state.isConnecting,
                    error = state.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                ChatSkeletonOverlay(visible = rows.isEmpty() && state.isConnecting)

                JumpToLatestPill(
                    visible = showJump,
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )

                ChatToast(
                    text = toast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp, start = 16.dp, end = 16.dp)
                )
            }

            ChatComposer(
                state = state,
                currentUserName = currentUserName,
                inputText = inputText,
                isSending = isSending,
                reactionOptions = reactionOptions,
                onInputChange = { inputText = it.take(MAX_INPUT) },
                onSend = sendCurrent,
                onSendReaction = onSendReaction
            )
        }
    }
}

@Composable
private fun ChatHeader(
    state: LiveChatState,
    onCloseClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(BgBlack)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Live Chat",
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(state)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Keep it friendly • no spam",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(Surface2, CircleShape)
                    .border(1.dp, Hairline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Live Chat",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline)
        )
    }
}

@Composable
private fun StatusPill(state: LiveChatState) {
    val online = state.onlineCount > 0
    val dotColor = when {
        online -> Color(0xFF22C55E)
        state.isConnecting -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }
    val label = when {
        online -> "${state.onlineCount} online"
        state.isConnecting -> "Connecting"
        else -> "Offline"
    }

    Row(
        modifier = Modifier
            .background(Surface1, RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveDot(color = dotColor, pulsing = online || state.isConnecting)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LiveDot(color: Color, pulsing: Boolean) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )

    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
        if (pulsing) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .graphicsLayer {
                        alpha = (1f - halo) * 0.4f
                        scaleX = 0.45f + halo
                        scaleY = 0.45f + halo
                    }
                    .background(color, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
private fun ChatSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) { index ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { alpha = shimmer }
                        .background(Surface2, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = shimmer }
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width((58 + index * 9).dp)
                            .height(8.dp)
                            .background(Surface2, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.55f + (index % 3) * 0.12f)
                            .height(11.dp)
                            .background(Surface1, RoundedCornerShape(5.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(row: ChatRow) {
    val message = row.message
    val timeStr = remember(message.ts) {
        if (message.ts > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.ts))
        } else ""
    }
    val topPadding = if (row.showHeader) 12.dp else 2.dp

    if (row.isMe) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding),
            horizontalAlignment = Alignment.End
        ) {
            if (row.showHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
                ) {
                    if (timeStr.isNotEmpty()) {
                        Text(
                            text = timeStr,
                            color = TimeGrey,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "You",
                        color = HandleGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = if (row.showHeader) 5.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )

            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(Surface3)
                    .border(1.dp, Hairline, shape)
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.body,
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding),
            verticalAlignment = Alignment.Top
        ) {
            if (row.showHeader) {
                Avatar(name = message.name, size = 28.dp)
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (row.showHeader) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = message.name,
                            color = HandleGrey,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (timeStr.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeStr,
                                color = TimeGrey,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                val shape = RoundedCornerShape(
                    topStart = if (row.showHeader) 5.dp else 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )

                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(shape)
                        .background(Surface2)
                        .border(1.dp, Hairline, shape)
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.body,
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String?, size: Dp) {
    val brush = remember(name) { avatarBrush(name?.ifBlank { null } ?: "fan") }
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    Box(
        modifier = Modifier
            .size(size)
            .background(brush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChatComposer(
    state: LiveChatState,
    currentUserName: String?,
    inputText: String,
    isSending: Boolean,
    reactionOptions: List<Pair<String, String>>,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendReaction: (String, String) -> Unit
) {
    val canSend = inputText.trim().isNotEmpty() && !isSending && !state.isConnecting
    var reactionsOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline)
        )

        AnimatedVisibility(
            visible = reactionsOpen,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 53.dp, end = 12.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reactionOptions.forEach { (id, emoji) ->
                    ReactionChip(
                        emoji = emoji,
                        enabled = !state.isConnecting,
                        onClick = { onSendReaction(id, emoji) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Avatar(name = currentUserName, size = 32.dp)

            Spacer(modifier = Modifier.width(9.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = currentUserName?.let { "Chat as @$it" } ?: "Connecting…",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        color = TextWhite,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(TextWhite),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            ReactionToggle(
                open = reactionsOpen,
                enabled = !state.isConnecting,
                onClick = { reactionsOpen = !reactionsOpen }
            )

            Spacer(modifier = Modifier.width(6.dp))

            SendButton(canSend = canSend, isSending = isSending, onClick = onSend)
        }
    }
}

@Composable
private fun ReactionToggle(
    open: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "toggleScale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(if (open) Surface3 else Surface1)
            .border(1.dp, Hairline, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddReaction,
            contentDescription = "Reactions",
            tint = if (open) TextWhite else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "chipScale"
    )

    Box(
        modifier = Modifier
            .size(33.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(Surface1)
            .border(1.dp, Hairline, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 16.sp)
    }
}

@Composable
private fun SendButton(
    canSend: Boolean,
    isSending: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "sendScale"
    )
    val background by animateColorAsState(
        targetValue = if (canSend) TextWhite else Surface1,
        animationSpec = tween(180),
        label = "sendBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (canSend) TextWhite else Hairline,
        animationSpec = tween(180),
        label = "sendBorder"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = canSend || isSending,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                color = Color.White.copy(alpha = 0.85f),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (canSend) Color.Black else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FloatingEmoji(
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
        fontSize = 22.sp,
        modifier = Modifier
            .offset {
                val p = progress.value
                val sway = sin((p * 6.5f).toDouble()).toFloat() * drift.dp.toPx()
                IntOffset(
                    x = ((lane - 0.5f) * 70.dp.toPx() + sway).toInt(),
                    y = -(p * 228.dp.toPx()).toInt()
                )
            }
            .graphicsLayer {
                val p = progress.value
                val appear = (p / 0.1f).coerceIn(0f, 1f)
                alpha = appear * (1f - p).coerceIn(0f, 1f)
                val s = 0.75f + p * 0.45f
                scaleX = s
                scaleY = s
                rotationZ = sin((p * 5f).toDouble()).toFloat() * 8f
            }
    )
}
@Composable
private fun ReactionLayer(
    particles: List<FloatingParticle>,
    onFinish: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        particles.forEach { particle ->
            key(particle.id) {
                FloatingEmoji(
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
private fun ChatEmptyOverlay(
    visible: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(120)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Surface2, CircleShape)
                    .border(1.dp, Hairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Be the first to say hi 👋",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error ?: "Messages show up here the moment the room goes live.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatSkeletonOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(120))
    ) {
        ChatSkeleton()
    }
}

@Composable
private fun JumpToLatestPill(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 2 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Surface3)
                .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Jump to latest",
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChatToast(
    text: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 2 },
        modifier = modifier
    ) {
        Text(
            text = text ?: "",
            color = TextWhite,
            fontSize = 11.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Surface3.copy(alpha = 0.97f))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
