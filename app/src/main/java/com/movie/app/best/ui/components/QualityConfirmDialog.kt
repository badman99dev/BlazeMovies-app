package com.movie.app.best.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.movie.app.best.data.remote.BypassOption
import com.movie.app.best.data.remote.BypassConfirmEvent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DlRedBright = Color(0xFFFF7B72)
private val DlRedMid = Color(0xFFE5484D)
private val DlRedDeep = Color(0xFFB3121B)
private val DlCardTop = Color(0xFF1C1C20)
private val DlCardBottom = Color(0xFF0C0C0E)
private val DlSurface = Color(0xFF101014)
private val DlSurfaceBorder = Color(0xFF26262F)

@Composable
fun QualityConfirmDialog(
    request: BypassConfirmEvent,
    timeoutMs: Long = 75_000L,
    onChoose: (String?) -> Unit
) {
    Dialog(
        onDismissRequest = { /* handled by card gestures */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            decorFitsSystemWindows = false
        )
    ) {
        QualityConfirmCard(request = request, timeoutMs = timeoutMs, onChoose = onChoose)
    }
}

@Composable
private fun QualityConfirmCard(
    request: BypassConfirmEvent,
    timeoutMs: Long,
    onChoose: (String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dialogScale = remember { Animatable(0.92f) }
    val dialogOffsetY = remember { Animatable(0f) }
    val dialogAlpha = remember { Animatable(0f) }
    val backdropAlpha = remember { Animatable(0f) }

    var isDismissing by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(timeoutMs) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { dialogScale.animateTo(1f, tween(300, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) }
            launch { dialogAlpha.animateTo(1f, tween(200)) }
            launch { backdropAlpha.animateTo(1f, tween(220)) }
        }
    }

    fun dismissIntoPocket(onComplete: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            coroutineScope {
                launch {
                    dialogOffsetY.animateTo(
                        targetValue = screenHeightPx * 0.75f,
                        animationSpec = tween(durationMillis = 240, easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f))
                    )
                }
                launch { dialogScale.animateTo(0.88f, tween(220)) }
                launch { dialogAlpha.animateTo(0f, tween(200)) }
                launch { backdropAlpha.animateTo(0f, tween(220)) }
            }
            onComplete()
        }
    }

    fun finish(choice: String?) {
        if (isDismissing) return
        dismissIntoPocket { onChoose(choice) }
    }

    val startMs = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        while (true) {
            val rem = timeoutMs - (System.currentTimeMillis() - startMs)
            remainingMs = rem.coerceAtLeast(0L)
            if (rem <= 0L) {
                finish(null)
                break
            }
            delay(50)
        }
    }

    PredictiveBackHandler(enabled = !isDismissing) { progress ->
        try {
            progress.collect { backEvent ->
                val p = backEvent.progress
                dialogScale.snapTo(1f - (p * 0.12f))
                dialogOffsetY.snapTo(p * with(density) { 45.dp.toPx() })
                backdropAlpha.snapTo(1f - (p * 0.35f))
            }
            dismissIntoPocket { onChoose(null) }
        } catch (e: CancellationException) {
            scope.launch {
                coroutineScope {
                    launch { dialogScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                    launch { dialogOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                    launch { backdropAlpha.animateTo(1f, tween(150)) }
                }
            }
        }
    }

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    BackHandler(enabled = !isDismissing) { finish(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9999f)
            .background(Color.Black.copy(alpha = 0.86f * backdropAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finish(null) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .graphicsLayer {
                    scaleX = dialogScale.value
                    scaleY = dialogScale.value
                    translationY = dialogOffsetY.value
                    alpha = dialogAlpha.value
                    rotationZ = 0f
                    transformOrigin = TransformOrigin(0.5f, 0.75f)
                }
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(listOf(DlCardTop, DlCardBottom)))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            DlRedBright.copy(alpha = 0.6f),
                            DlRedMid.copy(alpha = 0.35f),
                            DlSurfaceBorder
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { verticalDragAccumulator = 0f },
                        onDragEnd = {
                            if (verticalDragAccumulator > with(density) { 70.dp.toPx() }) {
                                finish(null)
                            } else {
                                scope.launch {
                                    coroutineScope {
                                        launch { dialogOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                        launch { dialogScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                coroutineScope {
                                    launch { dialogOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                                    launch { dialogScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
                                }
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0 || verticalDragAccumulator > 0) {
                                verticalDragAccumulator = (verticalDragAccumulator + dragAmount).coerceAtLeast(0f)
                                val progress = (verticalDragAccumulator / with(density) { 260.dp.toPx() }).coerceIn(0f, 1f)
                                scope.launch {
                                    dialogOffsetY.snapTo(verticalDragAccumulator * 0.7f)
                                    dialogScale.snapTo(1f - (progress * 0.12f))
                                }
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DlRedBright)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SELECT DOWNLOAD QUALITY",
                            color = DlRedBright,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF121217))
                            .border(1.dp, DlSurfaceBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = DlRedBright,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${ceil(remainingMs / 1000.0).toInt()}s",
                                color = DlRedBright,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((remainingMs.toFloat() / timeoutMs).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(listOf(DlRedBright, DlRedMid, DlRedDeep))
                            )
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DlRedMid.copy(alpha = 0.18f))
                            .border(1.dp, DlRedMid.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = DlRedBright,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pick your quality",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = request.prompt ?: "Multiple qualities available for this title.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    request.options.forEach { option ->
                        QualityConfirmOptionRow(option = option, onClick = { finish(option.id) })
                    }
                    if (request.options.isEmpty()) {
                        Text(
                            text = "No options available",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, DlSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Choose within ${ceil(timeoutMs / 1000.0).toInt()}s or the request is dropped",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                TextButton(onClick = { finish(null) }) {
                    Text(
                        text = "Cancel Download",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityConfirmOptionRow(option: BypassOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DlSurface)
            .border(
                width = 1.dp,
                brush = if (option.default)
                    Brush.horizontalGradient(listOf(DlRedMid.copy(alpha = 0.7f), DlRedBright.copy(alpha = 0.35f)))
                else
                    Brush.horizontalGradient(listOf(DlSurfaceBorder, DlSurfaceBorder)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(DlRedMid.copy(alpha = 0.16f))
                .border(2.dp, DlRedMid, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DlRedBright)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.label ?: option.id,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (option.default) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DlRedMid.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEFAULT",
                            color = DlRedBright,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }

        Text(
            text = option.meta ?: "",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
