package com.movie.app.best.data.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * YouTube-style custom in-app emoji definition.
 * Transported across wire as `:shortcode:`.
 */
data class CustomChatEmoji(
    val code: String,
    val name: String,
    val category: String = "YouTube",
    val draw: @Composable (Modifier) -> Unit
)

object ChatEmojiRegistry {

    private val EMOJI_REGEX = Regex(""":([a-zA-Z0-9_-]+):""")

    val allEmojis: List<CustomChatEmoji> = listOf(
        // 1. Signature Green Smiling Face (from user screenshot 4)
        CustomChatEmoji(":face-green-smiling:", "Green Laughing", "YouTube") { modifier ->
            GreenSmilingFace(modifier)
        },
        // 2. Purple Laughing Face
        CustomChatEmoji(":face-purple-wide-eyes:", "Purple Laugh", "YouTube") { modifier ->
            PurpleWideFace(modifier)
        },
        // 3. Orange Angry Face
        CustomChatEmoji(":face-orange-angry:", "Orange Angry", "YouTube") { modifier ->
            OrangeAngryFace(modifier)
        },
        // 4. Blue Cold / Frozen Face
        CustomChatEmoji(":face-blue-freeze:", "Blue Freeze", "YouTube") { modifier ->
            BlueFreezeFace(modifier)
        },
        // 5. Pink Party Monster
        CustomChatEmoji(":face-pink-party:", "Pink Party", "YouTube") { modifier ->
            PinkPartyFace(modifier)
        },
        // 6. Yellow Wink / Wow
        CustomChatEmoji(":face-yellow-wow:", "Yellow Wow", "YouTube") { modifier ->
            YellowWowFace(modifier)
        },
        // 7. Fire Flame
        CustomChatEmoji(":blaze-fire:", "Blaze Fire", "Blaze") { modifier ->
            BlazeFireEmoji(modifier)
        },
        // 8. 100 Score
        CustomChatEmoji(":score-100:", "100 Score", "Blaze") { modifier ->
            Score100Emoji(modifier)
        },
        // 9. Red Heart
        CustomChatEmoji(":heart-flame:", "Heart Flame", "YouTube") { modifier ->
            HeartFlameEmoji(modifier)
        },
        // 10. Trophy
        CustomChatEmoji(":trophy-gold:", "Gold Trophy", "Blaze") { modifier ->
            GoldTrophyEmoji(modifier)
        },
        // 11. Clapping Hands
        CustomChatEmoji(":hands-clap:", "Clapping Hands", "Blaze") { modifier ->
            HandsClapEmoji(modifier)
        },
        // 12. Party Popper
        CustomChatEmoji(":party-popper:", "Party Popper", "YouTube") { modifier ->
            PartyPopperEmoji(modifier)
        },
        // 13. Cool Sunglasses
        CustomChatEmoji(":face-cool-sunglasses:", "Cool Shades", "YouTube") { modifier ->
            CoolSunglassesFace(modifier)
        },
        // 14. GG
        CustomChatEmoji(":gg:", "GG", "Blaze") { modifier ->
            GGBadgeEmoji(modifier)
        }
    )

    private val emojiMap: Map<String, CustomChatEmoji> = allEmojis.associateBy { it.code }

    fun findEmoji(code: String): CustomChatEmoji? = emojiMap[code]

    /**
     * Converts a raw chat body (like "hello :face-green-smiling::face-green-smiling:")
     * into an AnnotatedString with Compose InlineTextContent placeholders.
     */
    fun parseToAnnotatedString(
        text: String,
        emojiSize: Dp = 19.dp
    ): Pair<AnnotatedString, Map<String, InlineTextContent>> {
        val inlineContentMap = mutableMapOf<String, InlineTextContent>()
        var matchIndex = 0

        val annotatedString = buildAnnotatedString {
            var lastIndex = 0
            val matches = EMOJI_REGEX.findAll(text)

            for (match in matches) {
                val fullMatch = match.value
                val emoji = emojiMap[fullMatch]

                // Append preceding plain text
                if (match.range.first > lastIndex) {
                    append(text.substring(lastIndex, match.range.first))
                }

                if (emoji != null) {
                    val inlineId = "emoji_${matchIndex++}_${emoji.code}"
                    appendInlineContent(inlineId, fullMatch)

                    inlineContentMap[inlineId] = InlineTextContent(
                        Placeholder(
                            width = emojiSize.value.sp,
                            height = emojiSize.value.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Box(modifier = Modifier.size(emojiSize)) {
                            emoji.draw(Modifier.size(emojiSize))
                        }
                    }
                } else {
                    // Unknown shortcode, leave as text
                    append(fullMatch)
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }

        return annotatedString to inlineContentMap
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom In-App Stickers & Emojis Drawers (Crisp Canvas Rendering)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GreenSmilingFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Vibrant lime-green rounded head
        drawRoundRect(
            color = Color(0xFF10B981),
            size = Size(w, h * 0.92f),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )
        // Two black eyes
        drawCircle(
            color = Color.Black,
            radius = w * 0.11f,
            center = Offset(w * 0.32f, h * 0.36f)
        )
        drawCircle(
            color = Color.Black,
            radius = w * 0.11f,
            center = Offset(w * 0.68f, h * 0.36f)
        )
        // Eye shine (white dots)
        drawCircle(
            color = Color.White,
            radius = w * 0.04f,
            center = Offset(w * 0.30f, h * 0.34f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.04f,
            center = Offset(w * 0.66f, h * 0.34f)
        )
        // Big open grin mouth
        val mouthPath = Path().apply {
            moveTo(w * 0.22f, h * 0.54f)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.78f, h * 0.54f)
            close()
        }
        drawPath(mouthPath, color = Color(0xFF18181B), style = Fill)
        // Red tongue
        val tonguePath = Path().apply {
            moveTo(w * 0.38f, h * 0.72f)
            quadraticTo(w * 0.5f, h * 0.86f, w * 0.62f, h * 0.72f)
            close()
        }
        drawPath(tonguePath, color = Color(0xFFEF4444), style = Fill)
    }
}

@Composable
fun PurpleWideFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Purple head
        drawRoundRect(
            color = Color(0xFF8B5CF6),
            size = Size(w, h * 0.92f),
            cornerRadius = CornerRadius(w * 0.3f, h * 0.3f)
        )
        // Big wide open eyes
        drawCircle(color = Color.White, radius = w * 0.16f, center = Offset(w * 0.33f, h * 0.38f))
        drawCircle(color = Color.White, radius = w * 0.16f, center = Offset(w * 0.67f, h * 0.38f))
        drawCircle(color = Color.Black, radius = w * 0.09f, center = Offset(w * 0.33f, h * 0.38f))
        drawCircle(color = Color.Black, radius = w * 0.09f, center = Offset(w * 0.67f, h * 0.38f))
        // Huge teeth grin
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.62f),
            size = Size(w * 0.56f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
        )
        // Grid lines for teeth
        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = Offset(w * 0.5f, h * 0.62f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = 1.5f
        )
    }
}

@Composable
fun OrangeAngryFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = Color(0xFFF97316), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
        // Angry slanted eyebrows
        drawLine(color = Color.Black, start = Offset(w * 0.20f, h * 0.28f), end = Offset(w * 0.42f, h * 0.38f), strokeWidth = 3f)
        drawLine(color = Color.Black, start = Offset(w * 0.80f, h * 0.28f), end = Offset(w * 0.58f, h * 0.38f), strokeWidth = 3f)
        // Eyes
        drawCircle(color = Color.Black, radius = w * 0.08f, center = Offset(w * 0.33f, h * 0.44f))
        drawCircle(color = Color.Black, radius = w * 0.08f, center = Offset(w * 0.67f, h * 0.44f))
        // Frown mouth
        val mouth = Path().apply {
            moveTo(w * 0.28f, h * 0.76f)
            quadraticTo(w * 0.5f, h * 0.62f, w * 0.72f, h * 0.76f)
        }
        drawPath(mouth, color = Color.Black, style = Stroke(width = 3.5f))
    }
}

@Composable
fun BlueFreezeFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = Color(0xFF0EA5E9), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
        // Shivering eyes
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.33f, h * 0.4f))
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.67f, h * 0.4f))
        drawCircle(color = Color.Black, radius = w * 0.06f, center = Offset(w * 0.33f, h * 0.4f))
        drawCircle(color = Color.Black, radius = w * 0.06f, center = Offset(w * 0.67f, h * 0.4f))
        // Ice icicles
        drawLine(color = Color(0xFFBAE6FD), start = Offset(w * 0.3f, h * 0.68f), end = Offset(w * 0.3f, h * 0.86f), strokeWidth = 3f)
        drawLine(color = Color(0xFFBAE6FD), start = Offset(w * 0.5f, h * 0.68f), end = Offset(w * 0.5f, h * 0.90f), strokeWidth = 3f)
        drawLine(color = Color(0xFFBAE6FD), start = Offset(w * 0.7f, h * 0.68f), end = Offset(w * 0.7f, h * 0.86f), strokeWidth = 3f)
    }
}

@Composable
fun PinkPartyFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = Color(0xFFEC4899), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
        // Happy closed eyes ^ ^
        val eyeLeft = Path().apply {
            moveTo(w * 0.24f, h * 0.42f)
            quadraticTo(w * 0.34f, h * 0.30f, w * 0.44f, h * 0.42f)
        }
        val eyeRight = Path().apply {
            moveTo(w * 0.56f, h * 0.42f)
            quadraticTo(w * 0.66f, h * 0.30f, w * 0.76f, h * 0.42f)
        }
        drawPath(eyeLeft, color = Color.White, style = Stroke(width = 3f))
        drawPath(eyeRight, color = Color.White, style = Stroke(width = 3f))
        // Open singing mouth
        drawCircle(color = Color.White, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.68f))
    }
}

@Composable
fun YellowWowFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = Color(0xFFEAB308), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
        drawCircle(color = Color.Black, radius = w * 0.08f, center = Offset(w * 0.34f, h * 0.38f))
        drawCircle(color = Color.Black, radius = w * 0.08f, center = Offset(w * 0.66f, h * 0.38f))
        // Big O mouth
        drawCircle(color = Color.Black, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.68f))
    }
}

@Composable
fun BlazeFireEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val outerFire = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            quadraticTo(w * 0.78f, h * 0.36f, w * 0.82f, h * 0.62f)
            quadraticTo(w * 0.86f, h * 0.92f, w * 0.5f, h * 0.94f)
            quadraticTo(w * 0.14f, h * 0.92f, w * 0.18f, h * 0.62f)
            quadraticTo(w * 0.22f, h * 0.36f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(outerFire, color = Color(0xFFEF4444))
        val innerFire = Path().apply {
            moveTo(w * 0.5f, h * 0.36f)
            quadraticTo(w * 0.68f, h * 0.56f, w * 0.68f, h * 0.76f)
            quadraticTo(w * 0.68f, h * 0.92f, w * 0.5f, h * 0.92f)
            quadraticTo(w * 0.32f, h * 0.92f, w * 0.32f, h * 0.76f)
            quadraticTo(w * 0.32f, h * 0.56f, w * 0.5f, h * 0.36f)
            close()
        }
        drawPath(innerFire, color = Color(0xFFFBBF24))
    }
}

@Composable
fun Score100Emoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Red 1 0 0 style
        drawLine(color = Color(0xFFDC2626), start = Offset(w * 0.22f, h * 0.28f), end = Offset(w * 0.22f, h * 0.72f), strokeWidth = 4f)
        drawCircle(color = Color(0xFFDC2626), radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = 3.5f))
        drawCircle(color = Color(0xFFDC2626), radius = w * 0.14f, center = Offset(w * 0.78f, h * 0.50f), style = Stroke(width = 3.5f))
        // Underline bars
        drawLine(color = Color(0xFFDC2626), start = Offset(w * 0.15f, h * 0.84f), end = Offset(w * 0.85f, h * 0.84f), strokeWidth = 2.5f)
        drawLine(color = Color(0xFFDC2626), start = Offset(w * 0.15f, h * 0.92f), end = Offset(w * 0.85f, h * 0.92f), strokeWidth = 2.5f)
    }
}

@Composable
fun HeartFlameEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val heart = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.58f, w * 0.05f, h * 0.30f, w * 0.28f, h * 0.20f)
            cubicTo(w * 0.42f, h * 0.14f, w * 0.5f, h * 0.30f, w * 0.5f, h * 0.30f)
            cubicTo(w * 0.5f, h * 0.30f, w * 0.58f, h * 0.14f, w * 0.72f, h * 0.20f)
            cubicTo(w * 0.95f, h * 0.30f, w * 0.85f, h * 0.58f, w * 0.5f, h * 0.82f)
            close()
        }
        drawPath(heart, color = Color(0xFFE11D48))
    }
}

@Composable
fun GoldTrophyEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Trophy cup
        val cup = Path().apply {
            moveTo(w * 0.26f, h * 0.20f)
            lineTo(w * 0.74f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.56f)
            quadraticTo(w * 0.5f, h * 0.70f, w * 0.32f, h * 0.56f)
            close()
        }
        drawPath(cup, color = Color(0xFFFACC15))
        // Base
        drawLine(color = Color(0xFFFACC15), start = Offset(w * 0.5f, h * 0.65f), end = Offset(w * 0.5f, h * 0.82f), strokeWidth = 4f)
        drawRoundRect(color = Color(0xFFEAB308), topLeft = Offset(w * 0.3f, h * 0.82f), size = Size(w * 0.4f, h * 0.12f), cornerRadius = CornerRadius(2f, 2f))
    }
}

@Composable
fun HandsClapEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = Color(0xFFF59E0B), radius = w * 0.24f, center = Offset(w * 0.38f, h * 0.5f))
        drawCircle(color = Color(0xFFFBBF24), radius = w * 0.24f, center = Offset(w * 0.62f, h * 0.44f))
        // Clap lines
        drawLine(color = Color(0xFFD97706), start = Offset(w * 0.5f, h * 0.18f), end = Offset(w * 0.5f, h * 0.08f), strokeWidth = 2.5f)
        drawLine(color = Color(0xFFD97706), start = Offset(w * 0.34f, h * 0.22f), end = Offset(w * 0.26f, h * 0.12f), strokeWidth = 2.5f)
        drawLine(color = Color(0xFFD97706), start = Offset(w * 0.66f, h * 0.22f), end = Offset(w * 0.74f, h * 0.12f), strokeWidth = 2.5f)
    }
}

@Composable
fun PartyPopperEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Cone
        val cone = Path().apply {
            moveTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.55f, h * 0.45f)
            lineTo(w * 0.35f, h * 0.25f)
            close()
        }
        drawPath(cone, color = Color(0xFFF59E0B))
        // Confetti dots
        drawCircle(color = Color(0xFFEF4444), radius = w * 0.06f, center = Offset(w * 0.65f, h * 0.25f))
        drawCircle(color = Color(0xFF3B82F6), radius = w * 0.06f, center = Offset(w * 0.82f, h * 0.40f))
        drawCircle(color = Color(0xFF10B981), radius = w * 0.05f, center = Offset(w * 0.72f, h * 0.55f))
        drawCircle(color = Color(0xFFA855F7), radius = w * 0.05f, center = Offset(w * 0.52f, h * 0.15f))
    }
}

@Composable
fun CoolSunglassesFace(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Yellow head
        drawCircle(color = Color(0xFFFACC15), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f))
        // Bridge of the shades
        drawRoundRect(
            color = Color(0xFF18181B),
            topLeft = Offset(w * 0.30f, h * 0.40f),
            size = Size(w * 0.40f, h * 0.07f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Left / right lenses
        drawRoundRect(
            color = Color(0xFF111113),
            topLeft = Offset(w * 0.13f, h * 0.34f),
            size = Size(w * 0.31f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )
        drawRoundRect(
            color = Color(0xFF111113),
            topLeft = Offset(w * 0.56f, h * 0.34f),
            size = Size(w * 0.31f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )
        // Lens shine
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = w * 0.045f, center = Offset(w * 0.22f, h * 0.40f))
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = w * 0.045f, center = Offset(w * 0.65f, h * 0.40f))
        // Smile
        val mouth = Path().apply {
            moveTo(w * 0.32f, h * 0.68f)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.68f, h * 0.68f)
        }
        drawPath(mouth, color = Color(0xFF18181B), style = Stroke(width = 3f))
    }
}

@Composable
fun GGBadgeEmoji(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Purple badge with a white plate and bold "GG"
        drawRoundRect(
            color = Color(0xFF7C3AED),
            size = Size(w, h * 0.86f),
            topLeft = Offset(0f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.10f, h * 0.20f),
            size = Size(w * 0.80f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        val paint = android.graphics.Paint().apply {
            color = 0xFF7C3AED.toInt()
            textSize = h * 0.46f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("GG", w * 0.5f, h * 0.68f, paint)
    }
}
