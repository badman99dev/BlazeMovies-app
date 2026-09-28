package com.movie.app.best.ui.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.data.settings.VideoQualitySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
            onBackClick = { showLogs = false },
            onRefresh = { logs = NetworkLogger.getLogs() },
            onCopy = { clipboardManager.setText(AnnotatedString(logs)) },
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
                Column {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Preferences & System",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFE50914).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 1: STREAMING & FILTER
            SettingsSectionHeader(title = "Streaming & Content")

            SettingsGroupCard {
                // Content Moderation Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Default.Security,
                        gradientColors = listOf(Color(0xFFE50914).copy(alpha = 0.3f), Color(0xFF8B0000).copy(alpha = 0.15f)),
                        contentColor = Color(0xFFFF4D5E)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Content Moderation Filter",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (!moderationEnabled) {
                                "Off — Show all content uncensored"
                            } else if (moderationMode == ModerationSettings.MODE_BLUR) {
                                "Blur inappropriate scenes & posters"
                            } else {
                                "Hide sexual & inappropriate titles"
                            },
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    MakkhanSwitch(
                        checked = moderationEnabled,
                        onCheckedChange = {
                            moderationEnabled = it
                            ModerationSettings.setEnabled(context, it)
                        }
                    )
                }

                // Moderation Sliding Segment
                AnimatedVisibility(
                    visible = moderationEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        SlidingSegmentedControl(
                            items = listOf(
                                "Blur (Recommended)" to ModerationSettings.MODE_BLUR,
                                "Hide Completely" to ModerationSettings.MODE_HIDE
                            ),
                            selectedItem = moderationMode,
                            onItemSelected = { mode ->
                                moderationMode = mode
                                ModerationSettings.setMode(context, mode)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(14.dp))

                // Video Quality Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Default.Videocam,
                        gradientColors = listOf(Color(0xFFA855F7).copy(alpha = 0.3f), Color(0xFF7E22CE).copy(alpha = 0.15f)),
                        contentColor = Color(0xFFC084FC)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Playback Quality",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = when (videoQualityMode) {
                                VideoQualitySettings.MODE_HIGH -> "Up to 1080p for crystal clear quality"
                                VideoQualitySettings.MODE_DATA_SAVING -> "Lowest quality to save mobile data"
                                else -> "Adaptive quality based on network speed"
                            },
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Sliding Segment
                SlidingSegmentedControl(
                    items = listOf(
                        "Auto" to VideoQualitySettings.MODE_AUTO,
                        "High (1080p)" to VideoQualitySettings.MODE_HIGH,
                        "Data Saver" to VideoQualitySettings.MODE_DATA_SAVING
                    ),
                    selectedItem = videoQualityMode,
                    onItemSelected = { q ->
                        videoQualityMode = q
                        VideoQualitySettings.setMode(context, q)
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 2: DEVELOPER & DIAGNOSTICS
            SettingsSectionHeader(title = "Developer & Diagnostics")

            SettingsGroupCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Default.BugReport,
                        gradientColors = listOf(Color(0xFFF59E0B).copy(alpha = 0.3f), Color(0xFFB45309).copy(alpha = 0.15f)),
                        contentColor = Color(0xFFFBBF24)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Debug Mode",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (debugEnabled) {
                                "Capturing ${NetworkLogger.getLogCount()} network requests & errors"
                            } else {
                                "Record network requests & diagnose issues"
                            },
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    MakkhanSwitch(
                        checked = debugEnabled,
                        onCheckedChange = { debugEnabled = it }
                    )
                }

                AnimatedVisibility(
                    visible = debugEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Logs", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { NetworkLogger.clear() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF5252)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                SettingsClickableRow(
                    icon = Icons.Default.Security,
                    iconGradient = listOf(Color(0xFFE50914).copy(alpha = 0.3f), Color(0xFFFF2E93).copy(alpha = 0.15f)),
                    iconTint = Color(0xFFFF4D8D),
                    title = "Test Firebase Diagnostics",
                    subtitle = "Send a safe test signal to Crashlytics (no app crash)",
                    onClick = {
                        try {
                            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                                log("Diagnostic Safe Ping triggered manually by user from Settings")
                                setCustomKey("manual_test_time", System.currentTimeMillis().toString())
                                recordException(Exception("BlazeMovies Crashlytics Safe Test - Everything is working!"))
                            }
                            android.widget.Toast.makeText(context, "Safe test report sent to Firebase! Check Crashlytics console.", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Firebase ping failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 3: SYSTEM & UPDATES
            SettingsSectionHeader(title = "App & System")

            SettingsGroupCard {
                SettingsClickableRow(
                    icon = Icons.Default.SystemUpdate,
                    iconGradient = listOf(Color(0xFF06B6D4).copy(alpha = 0.3f), Color(0xFF0891B2).copy(alpha = 0.15f)),
                    iconTint = Color(0xFF22D3EE),
                    title = "Check for Updates",
                    subtitle = "Verify if a newer OTA build is released",
                    onClick = { updateViewModel.checkForUpdate() }
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(10.dp))

                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    iconGradient = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)),
                    iconTint = Color.White,
                    title = "About BlazeMovies",
                    subtitle = "Fastest stream aggregation & failover player",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(10.dp))

                SettingsClickableRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconGradient = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.03f)),
                    iconTint = Color.White.copy(alpha = 0.4f),
                    title = "Help & Feedback",
                    subtitle = "Coming soon in upcoming release",
                    onClick = {},
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // FOOTER BRANDING
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BlazeMovies Android • Built with Jetpack Compose",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // ──────────────────────────────────────────────
    // PRESERVED 100% UPDATE LOGIC & STATE HANDLING
    // ──────────────────────────────────────────────
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
                title = "Update Check Failed",
                message = s.message,
                onDismiss = { updateViewModel.resetState() }
            )
        }
        else -> {}
    }
}

// ──────────────────────────────────────────────
// CUSTOM "MAKKHAN" SMOOTH SPRING TOGGLE
// ──────────────────────────────────────────────
@Composable
fun MakkhanSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val trackBg by animateColorAsState(
        targetValue = if (checked) Color(0xFFE50914) else Color(0xFF1E1E1E),
        animationSpec = tween(durationMillis = 240),
        label = "switchTrackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(trackBg)
            .border(
                width = 1.dp,
                color = if (checked) Color(0xFFFF5252).copy(alpha = 0.45f) else Color(0xFF2C2C2E),
                shape = RoundedCornerShape(99.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// CUSTOM SLIDING PILL SEGMENTED CONTROL
// ──────────────────────────────────────────────
@Composable
fun <T> SlidingSegmentedControl(
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (label, value) ->
            val isSelected = selectedItem == value
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFE50914) else Color.Transparent,
                animationSpec = tween(220),
                label = "segmentBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                animationSpec = tween(180),
                label = "segmentText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bgColor)
                    .clickable { onItemSelected(value) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// SQUIRCLE ICON BADGE WITH GRADIENT ACCENTS
// ──────────────────────────────────────────────
@Composable
fun IconBadge(
    icon: ImageVector,
    gradientColors: List<Color>,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, contentColor.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ──────────────────────────────────────────────
// ISLAND CARD CONTAINER
// ──────────────────────────────────────────────
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

// ──────────────────────────────────────────────
// SECTION HEADER
// ──────────────────────────────────────────────
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF9CA3AF),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp)
    )
}

// ──────────────────────────────────────────────
// INTERACTIVE CLICKABLE ROW
// ──────────────────────────────────────────────
@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailingBadge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            gradientColors = iconGradient,
            contentColor = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = if (enabled) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.25f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE50914).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFE50914).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = trailingBadge,
                    color = Color(0xFFFF5252),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (enabled) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ──────────────────────────────────────────────
// UPGRADED LOG VIEWER SCREEN
// ──────────────────────────────────────────────
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
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Debug Logs (${NetworkLogger.getLogCount()} entries)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Refresh",
                        tint = Color(0xFFFF9800)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        if (uploadedLink.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        uploadedLink,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "COPY",
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onCopyLink() }
                    )
                }
            }
        }

        if (uploadError.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E0000))
            ) {
                Text(
                    uploadError,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF080808))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = logs.ifEmpty { "No logs captured yet. Enable debug mode and use the app." },
                color = Color(0xFF00FF66),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onCopy()
                    copied = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (copied) "Copied!" else "Copy", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onUpload,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Get Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onClear,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
            ) {
                Text("Clear", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ──────────────────────────────────────────────
// UPGRADED CINEMATIC UPDATE & ALERT DIALOGS
// ──────────────────────────────────────────────
@Composable
private fun LoadingDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Row(
                modifier = Modifier.padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFE50914),
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF121212),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.75f)
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFF8B0000)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "New Update Available!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${data.version} • ${data.downloadSizeMb} MB",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (data.whatsNew.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF080808))
                            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                "What's New in this Build:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val lines = data.whatsNew.lines()
                                .map { it.removePrefix("## ").removePrefix("# ").trim() }
                                .filter { it.isNotBlank() && !it.startsWith("What's New") }
                            lines.forEach { line ->
                                val cleanLine = line.removePrefix("- ").trim()
                                if (cleanLine.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("▸ ", color = Color(0xFFE50914), fontSize = 13.sp)
                                        Text(cleanLine, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpdate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFFE50914),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Downloading Update...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Downloading package",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                    Text(
                        "$progress%",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Ready to Install", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "Update package downloaded successfully. Tap Install to upgrade BlazeMovies.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Install Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        },
        containerColor = Color(0xFF121212),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.75f)
    )
}
