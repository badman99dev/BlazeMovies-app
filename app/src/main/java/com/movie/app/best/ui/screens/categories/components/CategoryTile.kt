package com.movie.app.best.ui.screens.categories.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Category
import com.movie.app.best.ui.theme.AppFonts
import com.movie.app.best.ui.theme.CategoryPalette
import com.movie.app.best.ui.theme.categoryWash
import com.movie.app.best.util.formatTitleCount

private val TileShape = RoundedCornerShape(16.dp)
private val Hairline = Color.White.copy(alpha = 0.06f)
private val TileSurface = Color(0xFF0B0B0B)

/**
 * Text-first AMOLED category tile.
 * Banner (when present) is only a soft blurred backdrop — never the focus, and never a
 * broken-image placeholder. Name + count do the talking.
 */
@Composable
fun CategoryTile(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "tileScale"
    )

    val (tintA, tintB) = remember(category.slug) { CategoryPalette.of(category.slug) }
    val countText = remember(category.count) { formatTitleCount(category.count) }

    Box(
        modifier = modifier
            .aspectRatio(16f / 10f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(TileShape)
            .background(TileSurface)
            .border(1.dp, if (pressed) Color.White.copy(alpha = 0.12f) else Hairline, TileShape)
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        if (category.bannerUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.14f
                        scaleY = 1.14f
                    }
                    .blur(20.dp)
            ) {
                AsyncImage(
                    model = category.bannerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.84f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .categoryWash(tintA, tintB)
        )

        Text(
            text = category.categoryName.take(1).uppercase(),
            color = Color.White.copy(alpha = 0.06f),
            fontFamily = AppFonts.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 104.sp,
            letterSpacing = (-6).sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 22.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = category.categoryName,
                color = Color.White,
                fontFamily = AppFonts.ui,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                letterSpacing = (-0.25).sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            CountPill(text = countText, adult = category.isAdult)
        }
    }
}

@Composable
private fun CountPill(text: String, adult: Boolean) {
    val bg = if (adult) Color(0xFFE50914).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
    val border = if (adult) Color(0xFFE50914).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.07f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            fontFamily = AppFonts.ui,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}
