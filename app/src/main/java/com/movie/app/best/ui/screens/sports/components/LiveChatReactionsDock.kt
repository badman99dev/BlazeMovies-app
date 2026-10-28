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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * Floating reaction dock (YouTube style).
 *
 * Collapsed: only a single heart. Tapping the heart EXPANDS the dock — it does
 * not send anything. Once expanded all five reactions are shown and tapping one
 * sends it and collapses the dock again.
 *
 * Ids must exist in the server catalogue (LiveChat-server/lib/reactions.js).
 */
private val REACTIONS = listOf(
    QuickReactionItem("heart", "❤️"),
    QuickReactionItem("lol", "😂"),
    QuickReactionItem("party", "🎉"),
    QuickReactionItem("wow", "😮"),
    QuickReactionItem("100", "💯")
)

@Composable
fun LiveChatReactionsDock(
    onSendReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xCC18181B))
            .border(1.dp, Color(0xFF2E2E35), RoundedCornerShape(26.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // The four extra reactions appear only when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                REACTIONS.drop(1).forEach { item ->
                    ReactionDockButton(
                        emoji = item.emoji,
                        onClick = {
                            onSendReaction(item.id, item.emoji)
                            isExpanded = false
                        }
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

        // Heart — collapsed it expands, expanded it sends
        ReactionDockButton(
            emoji = REACTIONS.first().emoji,
            onClick = {
                if (isExpanded) {
                    onSendReaction(REACTIONS.first().id, REACTIONS.first().emoji)
                    isExpanded = false
                } else {
                    isExpanded = true
                }
            }
        )
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
