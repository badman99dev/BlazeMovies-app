package com.movie.app.best.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Deterministic tint per category key (slug) — decorative wash behind every tile so cards
 * without a usable banner still look designed instead of empty.
 */
object CategoryPalette {

    private val pairs = listOf(
        Color(0xFFE50914) to Color(0xFF780A14),
        Color(0xFF6C4BE0) to Color(0xFF1E145A),
        Color(0xFF00A0AA) to Color(0xFF06282D),
        Color(0xFFE69628) to Color(0xFF462806),
        Color(0xFFA03CC8) to Color(0xFF3C1050),
        Color(0xFF2878E6) to Color(0xFF081A3C),
        Color(0xFFC86E3C) to Color(0xFF461E0C),
        Color(0xFF78AA28) to Color(0xFF1E2C08)
    )

    fun of(key: String): Pair<Color, Color> {
        var h = 0
        for (c in key) h = (h * 31 + c.code) and 0x7FFFFFFF
        return pairs[h % pairs.size]
    }
}

/** Two soft radial washes (top-left + bottom-right) painted over the card backdrop. */
fun Modifier.categoryWash(
    topLeft: Color,
    bottomRight: Color,
    topAlpha: Float = 0.22f,
    bottomAlpha: Float = 0.16f
): Modifier = this.drawWithCache {
    val a = Brush.radialGradient(
        colors = listOf(topLeft.copy(alpha = topAlpha), Color.Transparent),
        center = Offset(size.width * 0.08f, 0f),
        radius = size.width * 0.95f
    )
    val b = Brush.radialGradient(
        colors = listOf(bottomRight.copy(alpha = bottomAlpha), Color.Transparent),
        center = Offset(size.width, size.height * 1.1f),
        radius = size.width * 1.2f
    )
    onDrawWithContent {
        drawContent()
        drawRect(a)
        drawRect(b)
    }
}
