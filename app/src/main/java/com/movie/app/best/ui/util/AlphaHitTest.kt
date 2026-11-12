package com.movie.app.best.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Pixel-accurate tap testing for transparent creatives (PNG / WebP).
 *
 * A tap only counts when the touched pixel is actually painted, so the empty area of a
 * transparent ad never triggers its link. The opaque mask is the first frame of the file,
 * decoded once per URL/path and reused.
 */
object AlphaHitTest {

    private const val DEFAULT_THRESHOLD = 24
    private const val MAX_MASK_DIM = 1440

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun mask(context: Context, source: String): ImageBitmap? {
        cache[source]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { decode(source) } ?: return null
        cache[source] = bitmap
        return bitmap
    }

    private fun decode(source: String): ImageBitmap? {
        val file = File(source)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source, bounds)
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > MAX_MASK_DIM) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sample
        }
        var bitmap: Bitmap? = try {
            BitmapFactory.decodeFile(source, opts)
        } catch (_: Exception) {
            null
        }

        // Animated WebP: BitmapFactory may refuse it — decode the first frame instead.
        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bitmap = try {
                val src = android.graphics.ImageDecoder.createSource(file)
                android.graphics.ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                    decoder.setTargetSize(
                        (info.size.width / sample).coerceAtLeast(1),
                        (info.size.height / sample).coerceAtLeast(1)
                    )
                }
            } catch (_: Exception) {
                null
            }
        }

        return bitmap?.asImageBitmap()
    }

    /**
     * Maps a tap inside [displayWidth]x[displayHeight] (ContentScale.Fit) back onto the mask
     * and reports whether that pixel is painted.
     */
    fun isOpaque(
        mask: ImageBitmap,
        tapX: Float,
        tapY: Float,
        displayWidth: Float,
        displayHeight: Float,
        threshold: Int = DEFAULT_THRESHOLD
    ): Boolean {
        if (displayWidth <= 0f || displayHeight <= 0f) return false
        val scale = minOf(displayWidth / mask.width, displayHeight / mask.height)
        if (scale <= 0f) return false
        val drawnW = mask.width * scale
        val drawnH = mask.height * scale
        val left = (displayWidth - drawnW) / 2f
        val top = (displayHeight - drawnH) / 2f

        val px = ((tapX - left) / scale).toInt()
        val py = ((tapY - top) / scale).toInt()
        if (px < 0 || py < 0 || px >= mask.width || py >= mask.height) return false

        val pixel = IntArray(1)
        return try {
            mask.readPixels(pixel, px, py, 1, 1)
            val alpha = (pixel[0] ushr 24) and 0xFF
            alpha > threshold
        } catch (_: Exception) {
            false
        }
    }
}
