package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.data.model.DownloadLink
import com.movie.app.best.data.repository.ResolvedMirror
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AccentPurple
import com.movie.app.best.ui.theme.AppRed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager

@Composable
fun DownloadBottomSheetContent(
    downloadLinks: List<DownloadLink>,
    downloadLoadingLinkId: Int?,
    downloadLogs: Map<Int?, List<String>> = emptyMap(),
    expandedLogsLinkId: Int? = null,
    onToggleLogs: (Int?) -> Unit = {},
    downloadPhase: DownloadPhase,
    downloadProgress: Int,
    downloadError: String?,
    downloadFailureReason: String?,
    resolvedMirrors: Map<Int?, List<ResolvedMirror>>,
    expandedLinkId: Int?,
    onStartDownload: (String, Int?) -> Unit,
    onPickMirror: (ResolvedMirror) -> Unit,
    onToggleExpand: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onGoToDownloads: () -> Unit = {},
    isZip: Boolean = false,
    extractionProgress: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 16.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Download",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val showPopup = downloadPhase != DownloadPhase.NONE

        AnimatedVisibility(
            visible = showPopup,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(400)
            ) + fadeIn(tween(350)),
            exit = slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(250)
            ) + fadeOut(tween(200))
        ) {
            DownloadStatusPopup(
                phase = downloadPhase,
                progress = downloadProgress,
                failureReason = downloadFailureReason,
                onGoToDownloads = onGoToDownloads,
                isZip = isZip,
                extractionProgress = extractionProgress
            )
        }

        AnimatedVisibility(
            visible = !showPopup,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            if (downloadLinks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No download links available",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    downloadLinks.forEach { link ->
                        DownloadLinkBottomSheetItem(
                            link = link,
                            isLoading = downloadLoadingLinkId == link.id,
                            logs = downloadLogs[link.id] ?: emptyList(),
                            logsExpanded = expandedLogsLinkId == link.id,
                            onToggleLogs = { onToggleLogs(link.id) },
                            mirrors = resolvedMirrors[link.id],
                            isExpanded = expandedLinkId == link.id,
                            onDownload = { onStartDownload(link.linkUrl, link.id) },
                            onPickMirror = onPickMirror,
                            onToggleExpand = { onToggleExpand(link.id) }
                        )
                    }
                }
            }
        }

        // Error
        if (downloadError != null) {
            Text(
                text = downloadError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DownloadStatusPopup(
    phase: DownloadPhase,
    progress: Int,
    failureReason: String?,
    onGoToDownloads: () -> Unit,
    isZip: Boolean = false,
    extractionProgress: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "capsule")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val accentColor = when (phase) {
        DownloadPhase.COMPLETE -> SuccessGreen
        DownloadPhase.FAILED -> AppRed
        DownloadPhase.CANCELLED -> Color(0xFFFB923C)
        else -> SuccessGreen
    }

    val title = when (phase) {
        DownloadPhase.INITIALIZING -> "Initializing..."
        DownloadPhase.DOWNLOADING -> if (isZip) "Downloading ZIP... $progress%" else "Downloading... $progress%"
        DownloadPhase.EXTRACTING -> "Unpacking... $extractionProgress%"
        DownloadPhase.COMPLETE -> if (isZip) "Ready to Play!" else "Download Complete!"
        DownloadPhase.CANCELLED -> "Download Cancelled"
        DownloadPhase.FAILED -> "Download Failed"
        else -> ""
    }

    val subtitle = when (phase) {
        DownloadPhase.INITIALIZING -> "Preparing poster and metadata..."
        DownloadPhase.DOWNLOADING -> if (isZip) "ZIP downloading in background" else "File is downloading in background"
        DownloadPhase.EXTRACTING -> "Preparing episodes"
        DownloadPhase.COMPLETE -> if (isZip) "Episodes ready to play" else "File saved to Downloads/BlazeMovies"
        DownloadPhase.CANCELLED -> "Download was cancelled"
        DownloadPhase.FAILED -> failureReason ?: "An error occurred during download"
        else -> ""
    }

    val showGoToDownloads = phase == DownloadPhase.COMPLETE || phase == DownloadPhase.DOWNLOADING || phase == DownloadPhase.EXTRACTING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = pulseAlpha * 0.6f),
                            accentColor.copy(alpha = 0.2f),
                            accentColor.copy(alpha = pulseAlpha * 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (phase) {
                        DownloadPhase.COMPLETE -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(28.dp)
                        )
                        DownloadPhase.FAILED, DownloadPhase.CANCELLED -> Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        else -> CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = accentColor,
                            strokeWidth = 2.5.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = accentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val progressWidth = when (phase) {
            DownloadPhase.DOWNLOADING -> progress / 100f
            DownloadPhase.EXTRACTING -> extractionProgress / 100f
            else -> shimmerOffset
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressWidth)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.3f),
                                accentColor.copy(alpha = 0.8f),
                                accentColor.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }

        if (showGoToDownloads) {
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f),
                                accentColor.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor.copy(alpha = 0.2f),
                                accentColor.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onGoToDownloads() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Go to Downloads",
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadLinkBottomSheetItem(
    link: DownloadLink,
    isLoading: Boolean,
    logs: List<String> = emptyList(),
    logsExpanded: Boolean = false,
    onToggleLogs: () -> Unit = {},
    mirrors: List<ResolvedMirror>?,
    isExpanded: Boolean,
    onDownload: () -> Unit,
    onPickMirror: (ResolvedMirror) -> Unit,
    onToggleExpand: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDownload
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = AppRed,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Download,
                        null,
                        tint = AppRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (logs.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (logsExpanded) AccentPurple.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleLogs
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (logsExpanded) Icons.Default.KeyboardArrowUp else Icons.Filled.Article,
                        contentDescription = "View logs",
                        tint = if (logsExpanded) Color(0xFFB388FF) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.label.ifEmpty { "Download" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (link.fileSize.isNotEmpty() || link.type.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (link.fileSize.isNotEmpty()) {
                            Text(
                                link.fileSize,
                                color = Color.White.copy(0.5f),
                                fontSize = 12.sp
                            )
                        }
                        if (link.type.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    link.type.uppercase(),
                                    color = Color(0xFFB388FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AppRed.copy(alpha = 0.6f),
                    strokeWidth = 2.dp
                )
            } else if (mirrors != null && mirrors.size > 1) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    null,
                    tint = Color.White.copy(0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (logsExpanded && logs.isNotEmpty()) {
            ProcessLogsPanel(logs = logs)
        }

        AnimatedVisibility(
            visible = isExpanded && mirrors != null && mirrors.size > 1,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mirrors?.forEach { mirror ->
                    MirrorItem(
                        mirror = mirror,
                        onPick = { onPickMirror(mirror) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessLogsPanel(logs: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val infiniteTransition = rememberInfiniteTransition(label = "logPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logPulseAlpha"
    )

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF050507))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppRed.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "PROCESS LOGS",
                color = Color(0xFF71717A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF18181B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = logs.size.toString(),
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (copied) {
                    Text(
                        text = "Copied!",
                        color = SuccessGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                        copied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy logs",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF3F3F46),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .background(Color(0xFF050507))
            ) {
                items(logs) { line ->
                    SelectionContainer {
                        Text(
                            text = line,
                            color = logLineColor(line),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun logLineColor(line: String): Color {
    return when {
        Regex("❌|error|fail|Error").containsMatchIn(line) -> Color(0xFFEF4444)
        Regex("💎|✅|BINGO|success|Jackpot").containsMatchIn(line) -> Color(0xFF22C55E)
        Regex("⚠️|warn").containsMatchIn(line) -> Color(0xFFEAB308)
        else -> Color(0xFF71717A)
    }
}

@Composable
private fun MirrorItem(
    mirror: ResolvedMirror,
    onPick: () -> Unit
) {
    val qualityColors = when (mirror.quality) {
        "4K", "2K" -> Color(0xFFC084FC)
        "1080p" -> Color(0xFF60A5FA)
        "720p" -> Color(0xFF4ADE80)
        "480p" -> Color(0xFFFB923C)
        else -> Color(0xFF71717A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(qualityColors.copy(alpha = 0.08f))
            .border(1.dp, qualityColors.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(qualityColors.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (mirror.isZip) Icons.Default.FolderZip else Icons.Default.Download,
                null,
                tint = qualityColors,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mirror.sourceLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = mirror.fileName,
                color = Color.White.copy(0.5f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (mirror.isZip) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentPurple.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "ZIP",
                    color = Color(0xFFB388FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(4.dp))
        }

        if (mirror.size.isNotEmpty()) {
            Text(
                text = mirror.size,
                color = qualityColors,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (mirror.resumable) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "Resume",
                    color = SuccessGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
