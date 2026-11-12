package com.movie.app.best.ui.components.promo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.PromoAction
import com.movie.app.best.data.model.PromoAd
import com.movie.app.best.ui.theme.AppFonts
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.util.AlphaHitTest
import com.movie.app.best.ui.util.AppLinkRouter

/**
 * Server-driven home popup. Renders one of the app templates, dismissed via the cross
 * (anchored to the creative's top-left corner) or the system back button.
 *
 * `image_alpha` mode is alpha-tested: only painted pixels fire the link, so a transparent
 * PNG/WebP behaves like a real cut-out sticker and its empty area stays inert.
 */
@Composable
fun PromoAdOverlay(
    ad: PromoAd,
    onDismiss: () -> Unit,
    onDeeplink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialogTemplate = remember(ad.template.dialog, ad.renderMode) {
        if (ad.renderMode == "image") PromoTemplates.imageOverlayDialog()
        else PromoTemplates.dialog(ad.template.dialog)
    }
    val buttonTemplate = remember(ad.template.button, ad.renderMode) {
        if (ad.renderMode == "image") PromoTemplates.imageOverlayButton()
        else PromoTemplates.button(ad.template.button)
    }

    val fireAction: (PromoAction) -> Unit = { action ->
        AppLinkRouter.handle(context, action, onDeeplink)
    }

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.74f))
    ) {
        when (dialogTemplate) {
            PromoTemplates.DIALOG_BOTTOM_SHEET -> BottomSheetLayout(ad, buttonTemplate, onDismiss, fireAction)
            PromoTemplates.DIALOG_FULLSCREEN_HERO -> FullscreenHeroLayout(ad, buttonTemplate, onDismiss, fireAction)
            PromoTemplates.DIALOG_TOP_BANNER -> TopBannerLayout(ad, buttonTemplate, onDismiss, fireAction)
            PromoTemplates.DIALOG_CENTER_CARD -> CenterCardLayout(ad, buttonTemplate, onDismiss, fireAction)
            else -> ImageOverlayLayout(ad, buttonTemplate, onDismiss, fireAction)
        }
    }
}

/* ─────────────────────────── image_alpha ─────────────────────────── */

@Composable
private fun ImageOverlayLayout(
    ad: PromoAd,
    buttonTemplate: String,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit
) {
    val context = LocalContext.current
    val imageUrl = ad.image?.url
    val mask by produceState<ImageBitmap?>(initialValue = null, imageUrl) {
        value = imageUrl?.let { AlphaHitTest.mask(context, it) }
    }

    val aspect: Float = remember(mask, ad.image?.width, ad.image?.height) {
        val m = mask
        when {
            m != null && m.height > 0 -> m.width.toFloat() / m.height.toFloat()
            (ad.image?.width ?: 0) > 0 && (ad.image?.height ?: 0) > 0 ->
                ad.image!!.width.toFloat() / ad.image!!.height.toFloat()
            else -> 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val maxW = constraints.maxWidth.toFloat()
            val maxH = constraints.maxHeight.toFloat()

            val boxW: Dp
            val boxH: Dp
            if (aspect > 0f) {
                val fitScale = minOf(maxW / aspect, maxH)
                with(density) {
                    boxW = (aspect * fitScale).toDp()
                    boxH = fitScale.toDp()
                }
            } else {
                boxW = maxWidth * 0.92f
                boxH = maxHeight
            }

            var drawn by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .width(boxW)
                    .height(boxH)
                    .onSizeChanged { drawn = it }
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(mask, drawn) {
                            detectTapGestures { offset ->
                                val m = mask ?: return@detectTapGestures
                                if (AlphaHitTest.isOpaque(
                                        m, offset.x, offset.y,
                                        drawn.width.toFloat(), drawn.height.toFloat()
                                    )
                                ) {
                                    fireAction(ad.imageAction)
                                }
                            }
                        }
                )

                // Cross on the creative's top-left corner.
                CloseCross(
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-12).dp, y = (-12).dp)
                        .size(30.dp)
                )

                if (ad.button.enabled) {
                    Box(
                        modifier = Modifier
                            .align(buttonAlignment(ad.button.position.align))
                            .offset(x = ad.button.position.offsetX.dp, y = (-ad.button.position.offsetY).dp)
                    ) {
                        PromoButtonView(ad, buttonTemplate, fireAction)
                    }
                }
            }
        }
    }
}

/* ─────────────────────────── native templates ─────────────────────────── */

private val CardShape = RoundedCornerShape(22.dp)

@Composable
private fun CenterCardLayout(
    ad: PromoAd,
    buttonTemplate: String,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(Color(0xFF0D0D0D))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CardShape)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeroImage(ad, Modifier.fillMaxWidth().height(190.dp), 16.dp, onDismiss, fireAction)
                Spacer(Modifier.height(14.dp))
                PromoTexts(ad)
                if (ad.button.enabled) {
                    Spacer(Modifier.height(16.dp))
                    PromoButtonView(ad, buttonTemplate, fireAction, Modifier.fillMaxWidth())
                }
            }
            CloseCross(onDismiss, Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp))
        }
    }
}

@Composable
private fun BottomSheetLayout(
    ad: PromoAd,
    buttonTemplate: String,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit
) {
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(Color(0xFF0D0D0D))
                .border(1.dp, Color.White.copy(alpha = 0.08f), sheetShape)
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                )
                Spacer(Modifier.height(14.dp))
                HeroImage(ad, Modifier.fillMaxWidth().height(180.dp), 18.dp, onDismiss, fireAction)
                Spacer(Modifier.height(14.dp))
                PromoTexts(ad)
                if (ad.button.enabled) {
                    Spacer(Modifier.height(16.dp))
                    PromoButtonView(ad, buttonTemplate, fireAction, Modifier.fillMaxWidth())
                }
            }
            CloseCross(onDismiss, Modifier.align(Alignment.TopEnd).padding(12.dp).size(30.dp))
        }
    }
}

@Composable
private fun FullscreenHeroLayout(
    ad: PromoAd,
    buttonTemplate: String,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        HeroImage(
            ad = ad,
            size = Modifier.fillMaxSize(),
            corner = 0.dp,
            onDismiss = onDismiss,
            fireAction = fireAction,
            scale = ContentScale.Crop,
            alphaAware = false
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PromoTexts(ad)
            if (ad.button.enabled) {
                Spacer(Modifier.height(16.dp))
                PromoButtonView(ad, buttonTemplate, fireAction, Modifier.fillMaxWidth())
            }
        }
        CloseCross(
            onDismiss,
            Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(34.dp)
        )
    }
}

@Composable
private fun TopBannerLayout(
    ad: PromoAd,
    buttonTemplate: String,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 14.dp, end = 14.dp)
                .clip(CardShape)
                .background(Color(0xFF0D0D0D))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CardShape)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeroImage(
                        ad = ad,
                        size = Modifier.size(64.dp),
                        corner = 12.dp,
                        onDismiss = onDismiss,
                        fireAction = fireAction
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) { PromoTexts(ad, compact = true) }
                    CloseCross(onDismiss, Modifier.size(30.dp))
                }
                if (ad.button.enabled) {
                    Spacer(Modifier.height(12.dp))
                    PromoButtonView(ad, buttonTemplate, fireAction, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/* ─────────────────────────── shared pieces ─────────────────────────── */

@Composable
private fun CloseCross(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun HeroImage(
    ad: PromoAd,
    size: Modifier,
    corner: Dp,
    onDismiss: () -> Unit,
    fireAction: (PromoAction) -> Unit,
    scale: ContentScale = ContentScale.Crop,
    alphaAware: Boolean = true
) {
    val url = ad.image?.url
    if (url.isNullOrBlank()) return
    val context = LocalContext.current
    val mask by produceState<ImageBitmap?>(initialValue = null, url) {
        value = if (alphaAware) AlphaHitTest.mask(context, url) else null
    }
    var measured by remember { mutableStateOf(IntSize.Zero) }
    val shape = RoundedCornerShape(corner)

    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = scale,
        modifier = size
            .clip(shape)
            .onSizeChanged { measured = it }
            .pointerInput(mask, measured, alphaAware) {
                detectTapGestures { offset ->
                    val action = ad.imageAction
                    if (action.isNone) return@detectTapGestures
                    if (!alphaAware) {
                        fireAction(action)
                        return@detectTapGestures
                    }
                    val m = mask ?: return@detectTapGestures
                    if (AlphaHitTest.isOpaque(
                            m, offset.x, offset.y,
                            measured.width.toFloat(), measured.height.toFloat()
                        )
                    ) {
                        fireAction(action)
                    }
                }
            }
    )
}

@Composable
private fun PromoTexts(ad: PromoAd, compact: Boolean = false) {
    val title = ad.content.title
    val subtitle = ad.content.subtitle
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                color = Color.White,
                fontFamily = AppFonts.display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (compact) 16.sp else 21.sp,
                letterSpacing = (-0.4).sp,
                lineHeight = if (compact) 19.sp else 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (compact) TextAlign.Start else TextAlign.Center
            )
        }
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.68f),
                fontFamily = AppFonts.ui,
                fontSize = if (compact) 12.sp else 13.5.sp,
                lineHeight = if (compact) 16.sp else 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (compact) TextAlign.Start else TextAlign.Center
            )
        }
    }
}

@Composable
private fun PromoButtonView(
    ad: PromoAd,
    template: String,
    fireAction: (PromoAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = ad.content.buttonLabel.ifBlank { "Open" }
    val accent = parseAccent(ad.template.accent)
    val fire = { fireAction(ad.button.action) }
    val buttonUrl = ad.button.image?.url

    if (template == PromoTemplates.BUTTON_CUSTOM_PNG && !buttonUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val mask by produceState<ImageBitmap?>(initialValue = null, buttonUrl) {
            value = AlphaHitTest.mask(context, buttonUrl)
        }
        var measured by remember { mutableStateOf(IntSize.Zero) }
        AsyncImage(
            model = buttonUrl,
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .height(52.dp)
                .onSizeChanged { measured = it }
                .pointerInput(mask, measured) {
                    detectTapGestures { offset ->
                        val m = mask ?: return@detectTapGestures
                        if (AlphaHitTest.isOpaque(
                                m, offset.x, offset.y,
                                measured.width.toFloat(), measured.height.toFloat()
                            )
                        ) {
                            fire()
                        }
                    }
                }
        )
        return
    }

    val shape = RoundedCornerShape(999.dp)
    val brush: Brush? = when (template) {
        PromoTemplates.BUTTON_SOLID_LIGHT, PromoTemplates.BUTTON_OUTLINE_GLASS -> null
        PromoTemplates.BUTTON_GOLD_PREMIUM ->
            Brush.linearGradient(listOf(Color(0xFFF5C451), Color(0xFFA8791A)))
        else -> Brush.linearGradient(listOf(accent, Color(0xFF7A0A12)))
    }
    val bgColor = when (template) {
        PromoTemplates.BUTTON_SOLID_LIGHT -> Color(0xFFEFEFEF)
        PromoTemplates.BUTTON_OUTLINE_GLASS -> Color.White.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val textColor = when (template) {
        PromoTemplates.BUTTON_SOLID_LIGHT -> Color(0xFF111111)
        PromoTemplates.BUTTON_GOLD_PREMIUM -> Color(0xFF201700)
        else -> Color.White
    }
    val borderColor = if (template == PromoTemplates.BUTTON_OUTLINE_GLASS) {
        Color.White.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(bgColor))
            .then(
                if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            )
            .clickable { fire() }
            .padding(vertical = 14.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontFamily = AppFonts.ui,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buttonAlignment(align: String): Alignment = when (align) {
    "bottom-left" -> Alignment.BottomStart
    "bottom-right" -> Alignment.BottomEnd
    "top-right" -> Alignment.TopEnd
    "center" -> Alignment.Center
    else -> Alignment.BottomCenter
}

private fun parseAccent(raw: String): Color {
    if (raw.isBlank()) return AppRed
    return try {
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: Exception) {
        AppRed
    }
}
