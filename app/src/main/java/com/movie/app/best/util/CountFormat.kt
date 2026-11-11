package com.movie.app.best.util

import java.util.Locale

/**
 * "8672" -> "🔥 8.6K+ Titles", "957" -> "957 Titles", "1200000" -> "1.2M+ Titles".
 * Values are truncated (not rounded) so a count never looks inflated.
 */
fun formatTitleCount(count: Int): String {
    val compact = when {
        count >= 1_000_000 -> trim(count / 1_000_000.0) + "M+"
        count >= 1_000 -> trim(count / 1_000.0) + "K+"
        else -> count.toString()
    }
    val hot = count >= HOT_THRESHOLD
    return (if (hot) "🔥 " else "") + compact + " Titles"
}

/** Compact form without the "Titles" suffix / fire emoji. */
fun formatCountCompact(count: Int): String = when {
    count >= 1_000_000 -> trim(count / 1_000_000.0) + "M+"
    count >= 1_000 -> trim(count / 1_000.0) + "K+"
    else -> count.toString()
}

private const val HOT_THRESHOLD = 5000

private fun trim(value: Double): String {
    val floored = (value * 10).toInt() / 10.0
    val text = String.format(Locale.US, "%.1f", floored)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}
