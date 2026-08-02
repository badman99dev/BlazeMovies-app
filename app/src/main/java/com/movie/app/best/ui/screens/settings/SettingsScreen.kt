package com.movie.app.best.ui.screens.settings

import com.movie.app.best.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.data.settings.VideoQualitySettings
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    var debugEnabled by remember { mutableStateOf(NetworkLogger.isEnabled()) }
    var showLogs by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedLink by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var moderationEnabled by remember { mutableStateOf(ModerationSettings.isEnabled(context)) }
    var moderationMode by remember { mutableStateOf(ModerationSettings.getMode(context)) }
    var videoQualityMode by remember { mutableStateOf(VideoQualitySettings.getMode(context)) }

    LaunchedEffect(debugEnabled) {
        NetworkLogger.setEnabled(debugEnabled)
    }

    if (showLogs) {
        LogViewerScreen(
            logs = logs,
            onBackClick = {
                showLogs = false
            },
            onRefresh = {
                logs = NetworkLogger.getLogs()
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(logs))
            },
            onClear = {
                NetworkLogger.clear()
                logs = ""
            },
            onUpload = {
                if (logs.isEmpty()) return@LogViewerScreen
                isUploading = true
                uploadedLink = ""
                uploadError = ""
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val file = File(context.cacheDir, "app_debug_${System.currentTimeMillis()}.log")
                        file.writeText(logs)
                        val fileName = URLEncoder.encode(file.name, "UTF-8")
                        val connection = java.net.URL("https://tempserv.cmdnode.xyz/api/upload")
                            .openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        val boundary = "----FetchBoundary${System.currentTimeMillis()}"
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                        val output = connection.outputStream
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"mode\"\r\n\r\nfile\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"expiry\"\r\n\r\n6hr\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"paths\"\r\n\r\n[\"$fileName\"]\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"files\"; filename=\"$fileName\"\r\n".toByteArray())
                        output.write("Content-Type: text/plain\r\n\r\n".toByteArray())
                        output.write(file.readBytes())
                        output.write("\r\n--$boundary--\r\n".toByteArray())
                        output.flush()
                        val responseCode = connection.responseCode
                        val responseBody = if (responseCode == 201) {
                            connection.inputStream.bufferedReader().readText()
                        } else {
                            connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                        }
                        withContext(Dispatchers.Main) {
                            if (responseCode == 201) {
                                val slug = try {
                                    val json = org.json.JSONObject(responseBody)
                                    json.optString("slug", "")
                                } catch (_: Exception) { "" }
                                if (slug.isNotEmpty()) {
                                    uploadedLink = "https://tempserv.cmdnode.xyz/file/$slug/dl"
                                } else {
                                    uploadError = "Upload succeeded but no link returned"
                                }
                            } else {
                                uploadError = "Upload failed: $responseBody"
                            }
                            isUploading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            uploadError = "Upload error: ${e.message}"
                            isUploading = false
                        }
                    }
                }
            },
            isUploading = isUploading,
            uploadedLink = uploadedLink,
            uploadError = uploadError,
            onCopyLink = {
                clipboardManager.setText(AnnotatedString(uploadedLink))
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionTitle("General")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "Content Moderation Filter",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (!moderationEnabled) "Off — show all content" else if (moderationMode == ModerationSettings.MODE_BLUR) "Blur sexual/inappropriate content" else "Hide sexual/inappropriate content",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = moderationEnabled,
                        onCheckedChange = {
                            moderationEnabled = it
                            ModerationSettings.setEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFFE50914),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            if (moderationEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerOptionChip("Blur", moderationMode == ModerationSettings.MODE_BLUR) {
                        moderationMode = ModerationSettings.MODE_BLUR
                        ModerationSettings.setMode(context, ModerationSettings.MODE_BLUR)
                    }
                    PlayerOptionChip("Hide", moderationMode == ModerationSettings.MODE_HIDE) {
                        moderationMode = ModerationSettings.MODE_HIDE
                        ModerationSettings.setMode(context, ModerationSettings.MODE_HIDE)
                    }
                }
            }

            SettingsSectionTitle("Video Quality")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "Video Quality",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = when (videoQualityMode) {
                                VideoQualitySettings.MODE_HIGH -> "Up to 1080p for best quality"
                                VideoQualitySettings.MODE_DATA_SAVING -> "Lowest quality to save data"
                                else -> "Adaptive quality based on network"
                            },
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerOptionChip("Auto", videoQualityMode == VideoQualitySettings.MODE_AUTO) {
                    videoQualityMode = VideoQualitySettings.MODE_AUTO
                    VideoQualitySettings.setMode(context, VideoQualitySettings.MODE_AUTO)
                }
                PlayerOptionChip("High", videoQualityMode == VideoQualitySettings.MODE_HIGH) {
                    videoQualityMode = VideoQualitySettings.MODE_HIGH
                    VideoQualitySettings.setMode(context, VideoQualitySettings.MODE_HIGH)
                }
                PlayerOptionChip("Data Saving", videoQualityMode == VideoQualitySettings.MODE_DATA_SAVING) {
                    videoQualityMode = VideoQualitySettings.MODE_DATA_SAVING
                    VideoQualitySettings.setMode(context, VideoQualitySettings.MODE_DATA_SAVING)
                }
            }

            SettingsSectionTitle("Developer")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "Debug Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (debugEnabled) "Capturing ${NetworkLogger.getLogCount()} log entries" else "Record network requests & errors",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = debugEnabled,
                        onCheckedChange = { debugEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFFE50914),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            if (debugEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            logs = NetworkLogger.getLogs()
                            showLogs = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Logs", color = Color.White, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            NetworkLogger.clear()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            SettingsSectionTitle("About")

            SettingsItem(
                icon = Icons.Default.SystemUpdate,
                title = "Check for Update",
                subtitle = "Check if a new version is available",
                onClick = { updateViewModel.checkForUpdate() }
            )

            SettingsItem(
                icon = Icons.Default.Info,
                title = "About Us",
                subtitle = "Learn more about BlazeMovies",
                onClick = {}
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help & Feedback",
                subtitle = "Coming soon",
                onClick = {},
                enabled = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "BlazeMovies v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val updateState by updateViewModel.state.collectAsState()

    when (val s = updateState) {
        is UpdateUiState.Checking -> {
            LoadingDialog("Checking for updates...")
        }
        is UpdateUiState.UpToDate -> {
            InfoDialog(
                title = "Up to Date!",
                message = "You're running the latest version v${BuildConfig.VERSION_NAME}.",
                onDismiss = { updateViewModel.resetState() }
            )
        }
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                data = s.data,
                onDismiss = { updateViewModel.resetState() },
                onUpdate = {
                    updateViewModel.startDownload(context, s.data.downloadUrl)
                }
            )
        }
        is UpdateUiState.Downloading -> {
            DownloadProgressDialog(
                progress = s.progress,
                onDismiss = { updateViewModel.resetState() }
            )
        }
        is UpdateUiState.DownloadComplete -> {
            InstallDialog(
                onInstall = {
                    updateViewModel.installApk(context, s.file)
                },
                onDismiss = { updateViewModel.resetState() }
            )
        }
        is UpdateUiState.Error -> {
            InfoDialog(
                title = "Error",
                message = s.message,
                onDismiss = { updateViewModel.resetState() }
            )
        }
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerScreen(
    logs: String,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onUpload: () -> Unit,
    isUploading: Boolean,
    uploadedLink: String,
    uploadError: String,
    onCopyLink: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Debug Logs (${NetworkLogger.getLogCount()} entries)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.7f))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111111)),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        if (uploadedLink.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uploadedLink, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("COPY", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { onCopyLink() })
                }
            }
        }

        if (uploadError.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E0000))
            ) {
                Text(uploadError, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = logs.ifEmpty { "No logs captured yet. Enable debug mode and use the app." },
                color = Color(0xFF00FF41),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onCopy()
                    copied = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Icon(imageVector = Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (copied) "Copied!" else "Copy", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onUpload,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Link", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onClear,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text("Clear", color = Color(0xFFFF5252), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (enabled) 0.04f else 0.02f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = title,
                    color = tint,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = tint.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
        )
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFE50914) else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LoadingDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFFE50914))
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
private fun UpdateAvailableDialog(
    data: com.movie.app.best.data.model.UpdateResponse,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Update Available", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "v${data.version} · ${data.downloadSizeMb} MB",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (data.whatsNew.isNotBlank()) {
                    Text(
                        "What's New",
                        color = Color(0xFFE50914),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val lines = data.whatsNew.lines()
                        .map { it.removePrefix("## ").removePrefix("# ").trim() }
                        .filter { it.isNotBlank() && !it.startsWith("What's New") }
                    lines.forEach { line ->
                        val cleanLine = line.removePrefix("- ").trim()
                        if (cleanLine.isNotBlank()) {
                            Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                Text("▸ ", color = Color(0xFFE50914), fontSize = 13.sp)
                                Text(cleanLine, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = Color.White.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressDialog(
    progress: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Downloading Update", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("$progress%", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Complete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text("APK downloaded successfully. Tap Install to update BlazeMovies.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Install", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
}
