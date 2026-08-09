package com.movie.app.best.ui.screens.downloads

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AccentPurple
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Play",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
