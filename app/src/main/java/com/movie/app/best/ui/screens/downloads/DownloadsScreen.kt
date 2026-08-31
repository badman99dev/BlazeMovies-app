package com.movie.app.best.ui.screens.downloads

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AccentPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.util.CfBypassHost
import java.io.File

data class LocalVideoFile(
    val name: String,
    val path: String,
    val size: Long,
    val extension: String,
    val contentUri: String = ""
)

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun scanBlazeMoviesVideos(context: Context): List<LocalVideoFile> {
    val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")
    val dir = java.io.File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "BlazeMovies"
    )
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    return dir.listFiles()
        ?.filter { it.isFile && !it.name.endsWith(".temp") && it.extension.lowercase() in videoExts }
        ?.sortedByDescending { it.lastModified() }
        ?.map { file ->
            LocalVideoFile(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                extension = file.extension.lowercase(),
                contentUri = ""
            )
        } ?: emptyList()
}

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onLocalVideosClick: () -> Unit = {},
    onPlayFile: (String, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    onOpenExtractedSeries: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<UnifiedDownloadItem?>(null) }
    var hasStoragePermission by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.rescanDownloads()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
    }

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.rescanDownloads()
        isRefreshing = false

        if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!hasStoragePermission) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Storage Permission Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Needed to download videos & scan downloads", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val unified = uiState.unifiedDownloads
    val downloadingItems = unified.filter {
        it.phase == UnifiedDownloadPhase.DOWNLOADING ||
        it.phase == UnifiedDownloadPhase.PAUSED ||
        it.phase == UnifiedDownloadPhase.BYPASSING ||
        it.phase == UnifiedDownloadPhase.FAILED ||
        it.phase == UnifiedDownloadPhase.EXTRACTING
    }
    val readyItems = unified.filter { it.phase == UnifiedDownloadPhase.COMPLETE }
    val bypassingItems = uiState.bypassing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        com.movie.app.best.ui.components.CompactPageHeader(
            title = "Downloads",
            actions = {
                if (isRefreshing || uiState.isRescanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    com.movie.app.best.ui.components.PageHeaderIconButton(onClick = {
                        isRefreshing = true
                        viewModel.rescanDownloads()
                        isRefreshing = false
                    }) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
                com.movie.app.best.ui.components.PageHeaderIconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        )

        if (uiState.isResolving) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Resolving download link...",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (uiState.resolveError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0000))
            ) {
                Text(
                    text = uiState.resolveError ?: "Error",
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (unified.isEmpty() && !uiState.isResolving) {
            EmptyDownloadsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (downloadingItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "DOWNLOADING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(downloadingItems, key = { it.id }) { item ->
                        UnifiedDownloadCard(
                            item = item,
                            onPlay = {
                                if (item.isZip && item.extractPath != null) {
                                    onOpenExtractedSeries(item.extractPath, item.slug, item.posterPath)
                                } else if (item.filePath.isNotEmpty()) {
                                    onPlayFile("file://${item.filePath}", item.fileName)
                                }
                            },
                            onPause = { viewModel.pauseDownload(item.ketchId) },
                            onResume = { viewModel.resumeDownload(item.ketchId) },
                            onCancel = { viewModel.cancelDownload(item.ketchId) },
                            onRetry = { viewModel.retryDownload(item.ketchId) },
                            onDelete = { showDeleteDialog = item }
                        )
                    }
                }

                if (readyItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "READY TO PLAY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(readyItems, key = { it.id }) { item ->
                        UnifiedDownloadCard(
                            item = item,
                            onPlay = {
                                if (item.isZip && item.extractPath != null) {
                                    onOpenExtractedSeries(item.extractPath, item.slug, item.posterPath)
                                } else if (item.filePath.isNotEmpty()) {
                                    onPlayFile("file://${item.filePath}", item.fileName)
                                }
                            },
                            onPause = { viewModel.pauseDownload(item.ketchId) },
                            onResume = { viewModel.resumeDownload(item.ketchId) },
                            onCancel = { viewModel.cancelDownload(item.ketchId) },
                            onRetry = { viewModel.retryDownload(item.ketchId) },
                            onDelete = { showDeleteDialog = item }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        bypassingItems.forEach { (ketchId, bypass) ->
            if (bypass.url.isNotBlank()) {
                CfBypassHost(
                    bypassUrl = bypass.url,
                    onLog = { line -> viewModel.appendBypassLog(ketchId, line) },
                    onSolved = { result -> viewModel.onBypassSolved(ketchId, result) },
                    onFailed = { viewModel.onBypassFailed(ketchId) }
                )
            }
        }
    }

    showDeleteDialog?.let { item ->
        DeleteConfirmationDialog(
            fileName = item.title.ifEmpty { item.fileName },
            onConfirm = {
                viewModel.deleteUnifiedItem(item)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

@Composable
private fun EmptyDownloadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No downloads yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Downloads will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedDownloadCard(
    item: UnifiedDownloadItem,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val accentColor = when (item.phase) {
        UnifiedDownloadPhase.COMPLETE -> SuccessGreen
        UnifiedDownloadPhase.FAILED -> AppRed
        UnifiedDownloadPhase.BYPASSING -> AccentPurple
        UnifiedDownloadPhase.PAUSED -> Color(0xFFFFA000)
        UnifiedDownloadPhase.EXTRACTING -> AccentPurple
        UnifiedDownloadPhase.DOWNLOADING -> if (item.isZip) AccentPurple else MaterialTheme.colorScheme.primary
    }

    val statusText = when (item.phase) {
        UnifiedDownloadPhase.DOWNLOADING -> "Downloading ${item.progress}%"
        UnifiedDownloadPhase.PAUSED -> "Paused ${item.progress}%"
        UnifiedDownloadPhase.BYPASSING -> "Bypassing Cloudflare…"
        UnifiedDownloadPhase.EXTRACTING -> "Unpacking ${item.extractionProgress}%"
        UnifiedDownloadPhase.COMPLETE -> "Ready to Play"
        UnifiedDownloadPhase.FAILED -> "Download Failed"
    }

    val subtitle = when (item.phase) {
        UnifiedDownloadPhase.DOWNLOADING -> {
            val speed = if (item.speedBytesPerSec > 0) "${formatFileSize(item.speedBytesPerSec)}/s" else "Connecting..."
            val sizeText = if (item.totalBytes > 0) "${formatFileSize(item.downloadedBytes)} / ${formatFileSize(item.totalBytes)}" else ""
            if (sizeText.isNotEmpty()) "$speed  •  $sizeText" else speed
        }
        UnifiedDownloadPhase.PAUSED -> if (item.totalBytes > 0) "${formatFileSize(item.downloadedBytes)} / ${formatFileSize(item.totalBytes)}" else "Paused"
        UnifiedDownloadPhase.BYPASSING -> "Solving Cloudflare challenge"
        UnifiedDownloadPhase.EXTRACTING -> "Preparing episodes"
        UnifiedDownloadPhase.COMPLETE -> if (item.isZip) "${item.episodeCount} episodes ready" else formatFileSize(item.totalBytes)
        UnifiedDownloadPhase.FAILED -> item.failureReason ?: "An error occurred"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmer"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = item.phase == UnifiedDownloadPhase.COMPLETE,
                onClick = onPlay
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isZip) Color(0xFF1A1A2E) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (item.posterPath.isNotEmpty() && File(item.posterPath).exists()) {
                    AsyncImage(
                        model = File(item.posterPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp, 84.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp, 84.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.isZip) Icons.Default.FolderZip else Icons.Default.Download,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.phase != UnifiedDownloadPhase.COMPLETE) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor.copy(alpha = pulseAlpha),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (item.phase) {
                        UnifiedDownloadPhase.DOWNLOADING -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Pause, "Pause", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Cancel", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            }
                        }
                        UnifiedDownloadPhase.PAUSED -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, "Resume", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Cancel", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            }
                        }
                        UnifiedDownloadPhase.BYPASSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        }
                        UnifiedDownloadPhase.FAILED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, "Retry", tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Dismiss", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            }
                        }
                        UnifiedDownloadPhase.EXTRACTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        }
                        UnifiedDownloadPhase.COMPLETE -> {
                            val playBg = Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
                            val playBorder = Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(playBg)
                                    .border(1.dp, playBorder, RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onPlay
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Play",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { showSheet = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, "More options", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (item.phase == UnifiedDownloadPhase.DOWNLOADING || item.phase == UnifiedDownloadPhase.PAUSED) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }

            if (item.phase == UnifiedDownloadPhase.EXTRACTING) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.extractionProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }
        }
    }

    if (showSheet) {
        DownloadItemSheet(
            item = item,
            onDismiss = { showSheet = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete") },
        text = { Text("Are you sure you want to delete \"$fileName\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFFF5252))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class MediaStoreInfo(
    val size: Long?,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val mime: String?
)

private data class TechnicalInfo(
    val mime: String?,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val videoCodecName: String?,
    val audioCodecName: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val channels: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadItemSheet(
    item: UnifiedDownloadItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var infoState by remember { mutableStateOf<MediaStoreInfo?>(null) }
    var techState by remember { mutableStateOf<TechnicalInfo?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showTech by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E2E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            SheetOptionRow(
                icon = Icons.Default.OpenInNew,
                title = "Open in External Player",
                onClick = {
                    openInExternalPlayer(context, item)
                    onDismiss()
                }
            )

            if (Build.VERSION.SDK_INT >= 30) {
                SheetOptionRow(
                    icon = Icons.Default.Settings,
                    title = "Technical Details",
                    showArrow = true,
                    expanded = showTech,
                    onClick = {
                        if (!showTech && techState == null) {
                            scope.launch {
                                techState = withContext(Dispatchers.IO) { extractTechnicalInfo(item) }
                            }
                        }
                        showTech = !showTech
                    }
                )
                if (showTech) {
                    val t = techState
                    if (t == null) {
                        SheetLoadingRow()
                    } else {
                        SheetInfoRows(t.toRows())
                    }
                }
            }

            SheetOptionRow(
                icon = Icons.Default.Info,
                title = "Info",
                showArrow = true,
                expanded = showInfo,
                onClick = {
                    if (!showInfo && infoState == null) {
                        scope.launch {
                            infoState = withContext(Dispatchers.IO) { queryMediaStoreInfo(context, item) }
                        }
                    }
                    showInfo = !showInfo
                }
            )
            if (showInfo) {
                val i = infoState
                if (i == null) {
                    SheetLoadingRow()
                } else {
                    SheetInfoRows(i.toRows(item))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            SheetOptionRow(
                icon = Icons.Default.Delete,
                title = "Delete",
                destructive = true,
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: ImageVector,
    title: String,
    destructive: Boolean = false,
    showArrow: Boolean = false,
    expanded: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (destructive) Color(0xFFFF5252) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SheetLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color(0xFFE50914),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(10.dp))
        Text("Loading...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
    }
}

@Composable
private fun SheetInfoRows(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 20.dp, bottom = 14.dp)
    ) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.width(110.dp)
                )
                Text(
                    value,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun openInExternalPlayer(context: Context, item: UnifiedDownloadItem) {
    try {
        val file = File(item.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun queryMediaStoreInfo(context: Context, item: UnifiedDownloadItem): MediaStoreInfo? {
    val storeResult: MediaStoreInfo? = try {
        val projection = arrayOf(
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )
        val selection = "${MediaStore.Video.Media.DATA} = ?"
        val selectionArgs = arrayOf(item.filePath)
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val getLong: (String) -> Long? = { col ->
                    val idx = cursor.getColumnIndex(col)
                    if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
                }
                val getString: (String) -> String? = { col ->
                    val idx = cursor.getColumnIndex(col)
                    if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
                }
                MediaStoreInfo(
                    size = getLong(MediaStore.Video.Media.SIZE),
                    durationMs = getLong(MediaStore.Video.Media.DURATION),
                    width = getLong(MediaStore.Video.Media.WIDTH)?.toInt(),
                    height = getLong(MediaStore.Video.Media.HEIGHT)?.toInt(),
                    mime = getString(MediaStore.Video.Media.MIME_TYPE)
                )
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }

    if (storeResult?.durationMs != null && storeResult.width != null && storeResult.height != null) {
        return storeResult
    }

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(item.filePath)
        val getMeta = { key: Int -> retriever.extractMetadata(key) }
        val fallback = MediaStoreInfo(
            size = storeResult?.size,
            durationMs = getMeta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            width = getMeta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = getMeta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            mime = getMeta(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        )
        MediaStoreInfo(
            size = fallback.size ?: File(item.filePath).length(),
            durationMs = fallback.durationMs,
            width = fallback.width,
            height = fallback.height,
            mime = fallback.mime
        )
    } catch (_: Exception) {
        storeResult
    } finally {
        try { retriever.release() } catch (_: Exception) {
        }
    }
}

private fun extractTechnicalInfo(item: UnifiedDownloadItem): TechnicalInfo? {
    var mime: String? = null
    var durationMs: Long? = null
    var width: Int? = null
    var height: Int? = null
    var bitrate: Int? = null

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(item.filePath)
        val getMeta = { key: Int -> retriever.extractMetadata(key) }
        mime = getMeta(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        durationMs = getMeta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        width = getMeta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        height = getMeta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        bitrate = getMeta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let { it.toLongOrNull()?.toInt() }
    } catch (_: Exception) {
    } finally {
        try { retriever.release() } catch (_: Exception) {
        }
    }

    var videoCodecName: String? = null
    var audioCodecName: String? = null
    var sampleRate: Int? = null
    var channels: Int? = null

    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(item.filePath)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
            when {
                trackMime.startsWith("video/") && videoCodecName == null -> {
                    videoCodecName = codecDisplayName(trackMime)
                    if (width == null && format.containsKey(MediaFormat.KEY_WIDTH)) {
                        width = format.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (height == null && format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                }
                trackMime.startsWith("audio/") && audioCodecName == null -> {
                    audioCodecName = codecDisplayName(trackMime)
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
        }
    } catch (_: Exception) {
    } finally {
        try { extractor.release() } catch (_: Exception) {
        }
    }

    return TechnicalInfo(
        mime = mime,
        durationMs = durationMs,
        width = width,
        height = height,
        videoCodecName = videoCodecName,
        audioCodecName = audioCodecName,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels
    )
}

private fun codecDisplayName(mime: String?): String? {
    if (mime == null) return null
    return when (mime) {
        "video/avc" -> "H.264"
        "video/hevc" -> "H.265"
        "video/vp8" -> "VP8"
        "video/vp9" -> "VP9"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4"
        "video/x-ms-wmv" -> "WMV"
        "video/x-matroska" -> "Matroska"
        "audio/mp4a-latm" -> "AAC"
        "audio/mp3" -> "MP3"
        "audio/mpeg" -> "MP3"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/ac3" -> "AC3"
        "audio/eac3" -> "E-AC3"
        "audio/flac" -> "FLAC"
        "audio/x-flac" -> "FLAC"
        "audio/raw" -> "PCM"
        "audio/x-ms-wma" -> "WMA"
        "audio/amr-wb" -> "AMR-WB"
        "audio/amr" -> "AMR"
        else -> mime.substringAfterLast("/").uppercase()
    }
}

private fun MediaStoreInfo.toRows(item: UnifiedDownloadItem): List<Pair<String, String>> = buildList {
    add("File Name" to item.fileName)
    add("File Path" to item.filePath)
    add("Size" to (this@toRows.size?.let { formatFileSize(it) } ?: formatFileSize(File(item.filePath).length())))
    add("Duration" to formatDuration(durationMs))
    add("Resolution" to (if (width != null && height != null) "${width} x ${height}" else "—"))
    add("Format" to (mime ?: "—"))
}

private fun TechnicalInfo.toRows(): List<Pair<String, String>> = buildList {
    add("Format" to (mime ?: "—"))
    add("Duration" to formatDuration(durationMs))
    add("Resolution" to (if (width != null && height != null) "${width} x ${height}" else "—"))
    add("Video Codec" to (videoCodecName ?: "—"))
    add("Audio Codec" to (audioCodecName ?: "—"))
    if (sampleRate != null) add("Sample Rate" to formatSampleRate(sampleRate!!))
    if (channels != null) add("Channels" to formatChannels(channels!!))
    add("Bitrate" to (bitrate?.let { formatFileSize(it.toLong()) + "/s" } ?: "—"))
}

private fun formatSampleRate(hz: Int): String {
    return if (hz % 1000 == 0) "${hz / 1000} kHz" else "$hz Hz"
}

private fun formatChannels(count: Int): String {
    return when (count) {
        1 -> "Mono (1 ch)"
        2 -> "Stereo (2 ch)"
        else -> "$count ch"
    }
}

private fun formatDuration(ms: Long?): String {
    if (ms == null || ms <= 0) return "—"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}
