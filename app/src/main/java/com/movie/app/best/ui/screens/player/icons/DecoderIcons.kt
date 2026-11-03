package com.movie.app.best.ui.screens.player.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val BOX_PATH =
    "M5,20q-0.83,0-1.41-0.59T3,18V6Q3,5.18 3.59,4.59T5,4H19q0.83,0 1.41,0.59Q21,5.18 21,6V18" +
        "q0,0.82-0.59,1.41T19,20H5Zm0-2H19V6H5V18Z"

/**
 * Box (fill) + letters (stroke). Icon ka color Icon(tint = ...) se aayega,
 * isliye yahan Color.Black use kiya hai (tint usse override kar deta hai).
 */
private fun decoderIcon(name: String, lettersPath: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(BOX_PATH).toNodes(),
            fill = SolidColor(Color.Black)
        )
        addPath(
            pathData = PathParser().parsePathString(lettersPath).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.25f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter
        )
    }.build()

object DecoderIcons {

    val HwPlus: ImageVector by lazy {
        decoderIcon(
            "HwPlus",
            "M6.25 9V15M8.5 9V15M6.25 12H8.5" +
                "M10 9L11.05 15L12.1 10.5L13.15 15L14.2 9" +
                "M17 8.75V12.25M15.25 10.5H18.75"
        )
    }

    val Hw: ImageVector by lazy {
        decoderIcon(
            "Hw",
            "M7.75 9V15M10.25 9V15M7.75 12H10.25" +
                "M12.25 9L13.4 15L14.5 10.5L15.6 15L16.75 9"
        )
    }

    val Sw: ImageVector by lazy {
        decoderIcon(
            "Sw",
            "M10.5 9.75H8.25Q7.5 9.75 7.5 10.5V11.25Q7.5 12 8.25 12H9.75Q10.5 12 10.5 12.75" +
                "V13.5Q10.5 14.25 9.75 14.25H7.5" +
                "M12.5 9L13.6 15L14.75 10.5L15.9 15L17 9"
        )
    }
}