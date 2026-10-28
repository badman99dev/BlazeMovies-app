package com.movie.app.best.ui.screens.sports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.chat.ChatEmojiRegistry
import com.movie.app.best.data.chat.CustomChatEmoji

@Composable
fun LiveChatCustomEmojiKeyboard(
    onEmojiSelected: (CustomChatEmoji) -> Unit,
    onBackspace: () -> Unit,
    onSwitchToTextKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { listOf("YouTube", "Blaze") }
    var selectedCategory by remember { mutableStateOf("YouTube") }

    val filteredEmojis = remember(selectedCategory) {
        ChatEmojiRegistry.allEmojis.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF0F0F12))
            .border(1.dp, Color(0xFF1E1E24))
    ) {
        // Category Header (like "YouTube" in screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF27272A) else Color.Transparent)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        // Custom Emojis Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredEmojis, key = { it.code }) { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEmojiSelected(emoji) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    emoji.draw(Modifier.size(32.dp))
                }
            }
        }

        // Bottom Controls Bar (Keyboard switch on left, Backspace on right)
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
                    contentDescription = "Switch to Text Keyboard",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Tap sticker to insert",
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
