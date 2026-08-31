package com.movie.app.best.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginRequiredDialog(
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit,
    anchorCenter: Offset? = null
) {
    var visible by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    LaunchedEffect(Unit) { visible = true }

    fun dismiss(callback: () -> Unit) {
        visible = false
        scope.launch {
            delay(230)
            callback()
        }
    }

    val transformOrigin = remember(anchorCenter, boxSize) {
        if (anchorCenter == null || boxSize == IntSize.Zero) {
            TransformOrigin.Center
        } else {
            val screenW = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenH = with(density) { configuration.screenHeightDp.dp.toPx() }
            val dialogLeft = (screenW - boxSize.width) / 2f
            val dialogTop = (screenH - boxSize.height) / 2f
            val originX = ((anchorCenter.x - dialogLeft) / boxSize.width).coerceIn(0f, 1f)
            val originY = ((anchorCenter.y - dialogTop) / boxSize.height).coerceIn(0f, 1f)
            TransformOrigin(originX, originY)
        }
    }

    Dialog(
        onDismissRequest = { dismiss(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(300),
                transformOrigin = transformOrigin
            ),
            exit = fadeOut(tween(200)) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(200),
                transformOrigin = transformOrigin
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .onSizeChanged { boxSize = it }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D))))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252))),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFFB71C1C))))
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF5252))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Login Required",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Unlock these features:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    LoginFeatureRow(Icons.Default.SmartDisplay, "Request Stream")
                    LoginFeatureRow(Icons.Default.Flag, "Report Content")
                    LoginFeatureRow(Icons.Default.ChatBubble, "Comments")
                    LoginFeatureRow(Icons.Default.Favorite, "Like")
                    LoginFeatureRow(Icons.Default.BookmarkAdd, "My List")

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFFB71C1C))))
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252))),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { dismiss(onLoginClick) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login & Sign Up",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { dismiss(onDismiss) }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginFeatureRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE50914).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
