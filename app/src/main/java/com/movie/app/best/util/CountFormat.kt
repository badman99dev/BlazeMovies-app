package com.movie.app.best.util

import java.util.Locale

/**
 * Formatted title count for category cards.
 *
 * 8672 -> "8.6K+ Titles" (hot), 957 -> "957 Titles" (not hot), 1200000 -> "1.2M+ Titles" (hot).
 * Values are truncated (never rounded up) so a count never looks inflated.
 */
data class TitleCount(
    val text: String,
    val hot: Boolean
)

fun titleCount(count: Int): TitleCount {
    val compact = when {
        count >= 1_000_000 -> trim(count / 1_000_000.0) + "M+"
        count >= 1_000 -> trim(count / 1_000.0) + "K+"
        else -> count.toString()
    }
    return TitleCount(text = "$compact Titles", hot = count >= HOT_THRESHOLD)
}

private const val HOT_THRESHOLD = 5000

private fun trim(value: Double): String {
    val floored = (value * 10).toInt() / 10.0
    val text = String.format(Locale.US, "%.1f", floored)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}
