package com.movie.app.best.ui.screens.player.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current

    if (onLongClick != null) {
        LaunchedEffect(interactionSource) {
            var isLongPressClicked = false
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongPressClicked = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        onLongClick.let {
                            isLongPressClicked = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            it.invoke()
                        }
                    }
                    is PressInteraction.Release -> {
                        if (!isLongPressClicked) onClick()
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        IconButton(
            onClick = if (onLongClick == null) onClick else {{}},
            enabled = isEnabled,
            modifier = modifier.size(48.dp),
            interactionSource = interactionSource,
            colors = IconButtonDefaults.iconButtonColors().copy(containerColor = containerColor),
            content = content,
        )
    }
}
