package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.Comment
import com.movie.app.best.ui.theme.AppRed
import kotlinx.coroutines.delay

@Composable
fun CommentsSection(
    comments: List<Comment>,
    isPosting: Boolean,
    posted: Boolean,
    error: String?,
    onPost: (name: String, msg: String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name    by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Auto-reset after post success
    LaunchedEffect(posted) {
        if (posted) { delay(2500); onReset() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle(title = "Comments", icon = Icons.Default.ChatBubbleOutline)

        Column(
            modifier              = Modifier.padding(horizontal = 18.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            // Comment count
            Text(
                "${comments.size} comment${if (comments.size != 1) "s" else ""}",
                color    = Color.White.copy(0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Comment list
            if (comments.isEmpty()) {
                Text(
                    text     = "No comments yet. Be the first!",
                    color    = Color.White.copy(0.35f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                comments.forEach { CommentCard(it) }
            }

            // Success banner
            AnimatedVisibility(visible = posted, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B5E20).copy(0.35f))
                        .padding(10.dp)
                ) {
                    Text("Comment posted! ✓", color = Color(0xFF69F0AE), fontSize = 13.sp)
                }
            }

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // Input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        label         = { Text("Your Name") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(8.dp),
                        colors        = commentFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = message,
                        onValueChange = { message = it },
                        label         = { Text("Write a comment…") },
                        minLines      = 2,
                        maxLines      = 4,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(8.dp),
                        colors        = commentFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick  = {
                            if (name.isNotBlank() && message.isNotBlank()) {
                                onPost(name, message)
                                message = ""
                            }
                        },
                        enabled  = !isPosting && name.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AppRed)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(Modifier.size(18.dp), Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Post Comment", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar initial circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppRed.copy(0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = comment.userName.take(1).uppercase(),
                        color      = AppRed,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(comment.userName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(timeAgo(comment.createdAt), color = Color.White.copy(0.35f), fontSize = 11.sp)
                }
            }
            if (comment.comment.isNotEmpty()) {
                Text(
                    text     = comment.comment,
                    color    = Color.White.copy(0.70f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 42.dp),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun commentFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = Color.DarkGray.copy(0.2f),
    focusedContainerColor   = Color.DarkGray.copy(0.2f),
    unfocusedIndicatorColor = Color.White.copy(0.1f),
    focusedIndicatorColor   = AppRed
)

private fun timeAgo(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val formats = listOf(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            java.time.format.DateTimeFormatter.ISO_DATE_TIME
        )
        var parsed: java.time.LocalDateTime? = null
        for (fmt in formats) {
            try { parsed = java.time.LocalDateTime.parse(dateStr, fmt); break } catch (_: Exception) {}
        }
        val dur = java.time.Duration.between(parsed ?: return dateStr, java.time.LocalDateTime.now())
        when {
            dur.toMinutes() < 1  -> "just now"
            dur.toMinutes() < 60 -> "${dur.toMinutes()}m ago"
            dur.toHours() < 24   -> "${dur.toHours()}h ago"
            dur.toDays() < 30    -> "${dur.toDays()}d ago"
            dur.toDays() < 365   -> "${dur.toDays() / 30}mo ago"
            else                 -> "${dur.toDays() / 365}y ago"
        }
    } catch (_: Exception) { dateStr }
}
