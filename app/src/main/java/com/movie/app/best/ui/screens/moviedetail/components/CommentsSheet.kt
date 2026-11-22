package com.movie.app.best.ui.screens.moviedetail.components

import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.movie.app.best.data.model.CommentApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SheetBg = Color.Black
private val SheetSurface = Color(0xFF141416)
private val SheetHairline = Color(0xFF27272A)
private val SheetText = Color(0xFFF4F4F5)
private val SheetMuted = Color(0xFF71717A)
private val Accent = Color(0xFFE50914)
private const val MAX_COMMENT_INPUT = 500

@Composable
fun CommentsSheet(
    movieId: Int,
    slug: String,
    isLoggedIn: Boolean,
    onLoginRequired: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: CommentsViewModel
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var input by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(movieId, slug) { viewModel.open(movieId, slug) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                val s = viewModel.state.value
                if (last >= 0 &&
                    s.hasMore &&
                    !s.isLoading &&
                    !s.isLoadingMore &&
                    !s.isPosting &&
                    last >= s.comments.size - 3
                ) {
                    viewModel.loadMore()
                }
            }
    }

    LaunchedEffect(state.comments.firstOrNull()?.id) {
        if (state.comments.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || state.isPosting) return
        if (!isLoggedIn) {
            onLoginRequired()
            return
        }
        viewModel.post(text)
        input = ""
        focusManager.clearFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Keep the window full-size when the IME shows up: we lift the composer
        // ourselves (Android insets), so the sheet itself must not be resized.
        val view = LocalView.current
        LaunchedEffect(Unit) {
            runCatching {
                (view.parent as? DialogWindowProvider)?.window
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            }
        }

        val density = LocalDensity.current
        val bottomLiftPx = maxOf(
            WindowInsets.navigationBars.getBottom(density),
            WindowInsets.ime.getBottom(density)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val fullHeight = maxHeight * 0.88f
            val partialHeight = maxHeight * 0.5f
            val sheetHeight by animateDpAsState(
                targetValue = if (expanded) fullHeight else partialHeight,
                animationSpec = tween(220),
                label = "commentsSheetHeight"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(SheetBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -6f) expanded = true
                                else if (dragAmount > 6f) expanded = false
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 9.dp, bottom = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SheetHairline)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Comments",
                                color = SheetText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    state.total > 0 -> if (state.total == 1) "1 comment" else "${state.total} comments"
                                    state.isLoading -> "Loading…"
                                    else -> "Be the first to comment"
                                },
                                color = SheetMuted,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SheetMuted)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SheetHairline)
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 64.dp)) {
                    when {
                        state.isLoading && state.comments.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(34.dp),
                                color = Accent,
                                strokeWidth = 3.dp
                            )
                        }

                        state.error != null && state.comments.isEmpty() -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.error ?: "Could not load comments",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 13.sp
                                )
                                TextButton(onClick = viewModel::retry) {
                                    Text("Retry", color = Accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        state.comments.isEmpty() -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = SheetHairline,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "No comments yet",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Be the first to share your thoughts",
                                    color = SheetMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp)
                            ) {
                                items(state.comments, key = { it.id }) { comment ->
                                    CommentRow(comment)
                                }
                                if (state.isLoadingMore) {
                                    item(key = "loading_more") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Accent,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Independent composer layer: follows the keyboard, not the sheet.
            CommentsComposer(
                value = input,
                onValueChange = {
                    input = it.take(MAX_COMMENT_INPUT)
                    if (state.postError != null) viewModel.clearPostError()
                },
                isSending = state.isPosting,
                error = state.postError,
                onSend = { submit() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = with(density) { bottomLiftPx.toDp() })
            )
        }
    }
}

@Composable
private fun CommentRow(comment: CommentApi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(handleColor(comment.handle)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.handle.trim().take(1).uppercase().ifBlank { "?" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.handle}",
                    color = SheetText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(comment),
                    color = SheetMuted,
                    fontSize = 10.5.sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = comment.comment,
                color = Color(0xFFD4D4D8),
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun CommentsComposer(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    error: String?,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.trim().isNotEmpty() && !isSending

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SheetBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SheetHairline)
        )
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFFF6B6B),
                fontSize = 11.5.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SheetSurface)
                    .border(1.dp, SheetHairline, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Add a comment…",
                        color = SheetMuted,
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = SheetText,
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

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(38.dp)
                    .background(if (canSend) Accent else Color(0xFF1E1E24), CircleShape)
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
                        tint = if (canSend) Color.White else SheetMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private fun handleColor(handle: String): Color {
    val palette = listOf(
        Color(0xFFE50914),
        Color(0xFF2563EB),
        Color(0xFF059669),
        Color(0xFF7C3AED),
        Color(0xFFDB2777),
        Color(0xFFEA580C),
        Color(0xFF0891B2),
        Color(0xFF65A30D)
    )
    var h = 0
    for (ch in handle.lowercase()) h = h * 31 + ch.code
    val idx = ((h % palette.size) + palette.size) % palette.size
    return palette[idx]
}

private fun relativeTime(comment: CommentApi): String {
    val createdAtMs: Long = if (comment.createdTs > 0) {
        comment.createdTs * 1000L
    } else {
        val raw = comment.createdAt
        if (raw.isBlank()) return ""
        val parsed = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw)
        } catch (_: Exception) {
            null
        } ?: return raw.take(10)
        parsed.time
    }

    val diff = System.currentTimeMillis() - createdAtMs
    val mins = diff / 60_000L
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1_440 -> "${mins / 60}h"
        mins < 10_080 -> "${mins / 1_440}d"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(createdAtMs))
    }
}