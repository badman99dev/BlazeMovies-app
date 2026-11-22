package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.CommentApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SheetBg = Color(0xFF0B0B0F)
private val SheetSurface = Color(0xFF18181B)
private val SheetHairline = Color(0xFF27272A)
private val SheetText = Color(0xFFF4F4F5)
private val SheetMuted = Color(0xFF71717A)
private val Accent = Color(0xFFE50914)
private const val MAX_COMMENT_INPUT = 500

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var input by remember { mutableStateOf("") }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        contentColor = SheetText,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
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
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                ) {
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp)
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

            CommentsComposer(
                value = input,
                onValueChange = {
                    input = it.take(MAX_COMMENT_INPUT)
                    if (state.postError != null) viewModel.clearPostError()
                },
                isSending = state.isPosting,
                error = state.postError,
                onSend = { submit() }
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
    onSend: () -> Unit
) {
    val canSend = value.trim().isNotEmpty() && !isSending

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetBg)
    ) {
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFFF6B6B),
                fontSize = 11.5.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp)
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