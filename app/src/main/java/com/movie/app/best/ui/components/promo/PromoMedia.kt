package com.movie.app.best.ui.components.promo

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.movie.app.best.ui.util.AlphaHitTest
import java.io.File

/**
 * Renders a promo creative.
 *
 * - `type = "lottie"` → Lottie animation (loops forever). Lottie is vector, so there is no
 *   alpha mask: the whole rendered area is tappable.
 * - otherwise → PNG / WebP / GIF via Coil, and taps are alpha-tested so transparent pixels
 *   stay inert (unless [alphaAware] is off, e.g. a cropped hero).
 *
 * The source is either an already-downloaded local file (offline) or a remote URL.
 */
@Composable
fun PromoMedia(
    source: String,
    type: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alphaAware: Boolean = true,
    tappable: Boolean = true,
    onClick: () -> Unit = {}
) {
    if (type.equals("lottie", ignoreCase = true)) {
        val spec = remember(source) {
            val file = File(source)
            if (file.exists()) LottieCompositionSpec.File(file) else LottieCompositionSpec.Url(source)
        }
        val composition by rememberLottieComposition(spec)
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            contentScale = contentScale,
            modifier = modifier.then(
                if (tappable) {
                    Modifier.pointerInput(source) { detectTapGestures { onClick() } }
                } else {
                    Modifier
                }
            )
        )
        return
    }

    val context = LocalContext.current
    val mask by produceState<ImageBitmap?>(initialValue = null, source, alphaAware) {
        value = if (alphaAware) AlphaHitTest.mask(context, source) else null
    }
    var measured by remember { mutableStateOf(IntSize.Zero) }

    AsyncImage(
        model = source,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .onSizeChanged { measured = it }
            .then(
                if (!tappable) {
                    Modifier
                } else if (!alphaAware) {
                    Modifier.pointerInput(source) { detectTapGestures { onClick() } }
                } else {
                    Modifier.pointerInput(mask, measured) {
                        detectTapGestures { offset ->
                            val m = mask ?: return@detectTapGestures
                            if (AlphaHitTest.isOpaque(
                                    m, offset.x, offset.y,
                                    measured.width.toFloat(), measured.height.toFloat()
                                )
                            ) {
                                onClick()
                            }
                        }
                    }
                }
            )
    )
}
