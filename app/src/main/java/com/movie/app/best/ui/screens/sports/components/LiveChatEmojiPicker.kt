package com.movie.app.best.ui.screens.sports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Emoji drawer for the live-chat composer.
 *
 * Plain unicode emoji only: they travel as normal text (no shortcodes), render
 * natively inside BasicTextField, and the chat server needs no changes.
 */
private val POPULAR_EMOJI = listOf(
    "😂", "❤️", "🔥", "👍", "😍", "🙏", "👏", "🎉",
    "💯", "😮", "😢", "😡", "🤣", "😭", "😎", "🤔",
    "👀", "🙌", "💪", "🤝", "✨", "⚡", "💔", "😴",
    "🤯", "😱", "👌", "🥳", "😇", "🤩", "🥺", "😜"
)

private val SPORTS_EMOJI = listOf(
    "🏏", "⚽", "🏆", "🥇", "🏅", "🎯", "🏀", "🎾",
    "🏐", "🏓", "🥊", "🏸", "🏃", "🚴", "📣", "🎽"
)

private val ALL_EMOJI = POPULAR_EMOJI + SPORTS_EMOJI

@Composable
fun LiveChatEmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onSwitchToTextKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = remember { ALL_EMOJI }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(0xFF0F0F12))
            .border(1.dp, Color(0xFF1E1E24))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 25.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom controls: text keyboard on the left, backspace on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF09090B))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSwitchToTextKeyboard,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF1E1E24), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Switch to text keyboard",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Tap an emoji to insert",
                color = Color(0xFF71717A),
                fontSize = 11.sp
            )

            IconButton(
                onClick = onBackspace,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF1E1E24), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
