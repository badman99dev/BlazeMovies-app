package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickReactionItem(
    val id: String,
    val emoji: String
)

@Composable
fun LiveChatReactionsDock(
    onSendReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val primaryReactions = remember {
        listOf(
            QuickReactionItem("heart", "❤️"),
            QuickReactionItem("lol", "😂"),
            QuickReactionItem("party", "🎉"),
            QuickReactionItem("wow", "😮"),
            QuickReactionItem("100", "💯")
        )
    }

    val extraReactions = remember {
        listOf(
            QuickReactionItem("fire", "🔥"),
            QuickReactionItem("clap", "👏"),
            QuickReactionItem("eyes", "👀"),
            QuickReactionItem("pray", "🙏")
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xCC18181B)) // Translucent dark pill
            .border(1.dp, Color(0xFF2E2E35), RoundedCornerShape(26.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Extra reactions shown when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                extraReactions.forEach { item ->
                    ReactionDockButton(
                        emoji = item.emoji,
                        onClick = { onSendReaction(item.id, item.emoji) }
                    )
                }
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }

        // Primary 5 reactions
        primaryReactions.forEach { item ->
            ReactionDockButton(
                emoji = item.emoji,
                onClick = { onSendReaction(item.id, item.emoji) }
            )
        }

        // Expand / Collapse Chevron Toggle
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse reactions" else "More reactions",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReactionDockButton(
    emoji: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 800f),
        label = "reactionDockScale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp
        )
    }
}
