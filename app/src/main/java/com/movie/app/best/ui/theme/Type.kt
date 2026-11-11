package com.movie.app.best.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.movie.app.best.R

/**
 * App typefaces.
 *  - [ui]      = Outfit (body, labels, buttons)
 *  - [display] = Cabinet Grotesk (headings, titles, wordmark)
 *
 * Both are bundled in res/font so they work fully offline (no downloadable-font downloads).
 */
object AppFonts {

    val ui: FontFamily = FontFamily(
        Font(R.font.outfit_regular, FontWeight.Normal),
        Font(R.font.outfit_medium, FontWeight.Medium),
        Font(R.font.outfit_semibold, FontWeight.SemiBold),
        Font(R.font.outfit_bold, FontWeight.Bold)
    )

    val display: FontFamily = FontFamily(
        Font(R.font.cabinet_grotesk_bold, FontWeight.Bold),
        Font(R.font.cabinet_grotesk_extrabold, FontWeight.ExtraBold),
        Font(R.font.cabinet_grotesk_extrabold, FontWeight.Black)
    )
}

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val Typography = Typography(
    displayLarge = style(AppFonts.display, FontWeight.ExtraBold, 44, 50, -1.2),
    displayMedium = style(AppFonts.display, FontWeight.ExtraBold, 36, 42, -1.0),
    displaySmall = style(AppFonts.display, FontWeight.Bold, 30, 36, -0.8),

    headlineLarge = style(AppFonts.display, FontWeight.Bold, 28, 34, -0.6),
    headlineMedium = style(AppFonts.display, FontWeight.Bold, 24, 30, -0.5),
    headlineSmall = style(AppFonts.display, FontWeight.Bold, 21, 27, -0.4),

    titleLarge = style(AppFonts.ui, FontWeight.Bold, 20, 26, -0.3),
    titleMedium = style(AppFonts.ui, FontWeight.SemiBold, 16, 22, -0.2),
    titleSmall = style(AppFonts.ui, FontWeight.SemiBold, 14, 20, -0.1),

    bodyLarge = style(AppFonts.ui, FontWeight.Normal, 16, 24, 0.1),
    bodyMedium = style(AppFonts.ui, FontWeight.Normal, 14, 20, 0.1),
    bodySmall = style(AppFonts.ui, FontWeight.Normal, 12, 17, 0.15),

    labelLarge = style(AppFonts.ui, FontWeight.SemiBold, 14, 19, 0.1),
    labelMedium = style(AppFonts.ui, FontWeight.Medium, 12, 16, 0.3),
    labelSmall = style(AppFonts.ui, FontWeight.Medium, 11, 15, 0.4)
)
