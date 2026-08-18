package com.movie.app.best.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.repository.PrefetchCache
import com.movie.app.best.ui.components.CategoryDrawerContent
import com.movie.app.best.ui.navigation.AppNavigation
import com.movie.app.best.ui.navigation.BottomNavigationBar
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.screens.splash.SplashScreen
import com.movie.app.best.ui.screens.settings.UpdateViewModel
import com.movie.app.best.ui.screens.settings.UpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    var splashScreenFinished by remember { mutableStateOf(false) }
    var storagePermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = hiltViewModel()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storagePermissionGranted = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    if (!splashScreenFinished) {
        SplashScreen(onSplashScreenFinish = { splashScreenFinished = true })
    } else {
        if (!storagePermissionGranted && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            storagePermissionGranted = true
        }

        if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LaunchedEffect(Unit) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            notificationPermissionGranted = true
        }

        val updateResp = PrefetchCache.updateResponse
        var showUpdateDialog by remember { mutableStateOf(false) }

        if (updateResp != null && updateResp.maintenance) {
            MaintenanceScreen(
                message = updateResp.maintenanceMessage.ifBlank { "Server under maintenance. Please try again later." },
                onRetry = {
                    splashScreenFinished = false
                    PrefetchCache.clear()
                }
            )
        } else if (updateResp != null && updateResp.forceUpdate) {
            ForceUpdateScreen(
                data = updateResp,
                updateViewModel = updateViewModel,
                context = context
            )
        } else if (updateResp != null && updateResp.updateAvailable) {
            val prefs = remember { context.getSharedPreferences("update_cache", android.content.Context.MODE_PRIVATE) }
            val lastShown = remember { prefs.getLong("last_prompt_time", 0L) }
            val shouldShow = remember { (System.currentTimeMillis() - lastShown) >= 24 * 60 * 60 * 1000 }

            // Auto-prompt once per 24h on app open (existing throttle behaviour).
            LaunchedEffect(Unit) {
                if (shouldShow) {
                    prefs.edit().putLong("last_prompt_time", System.currentTimeMillis()).apply()
                    showUpdateDialog = true
                }
            }

            if (showUpdateDialog) {
                NormalUpdateDialog(
                    data = updateResp,
                    onDismiss = { showUpdateDialog = false },
                    onUpdate = {
                        showUpdateDialog = false
                        updateViewModel.startDownload(context, updateResp.downloadUrl)
                    }
                )
            }

            val normalUpdateState by updateViewModel.state.collectAsState()
            when (val s = normalUpdateState) {
                is UpdateUiState.Downloading -> {
                    DownloadProgressDialog(
                        progress = s.progress,
                        onDismiss = { updateViewModel.resetState() }
                    )
                }
                is UpdateUiState.DownloadComplete -> {
                    InstallDialog(
                        onInstall = { updateViewModel.installApk(context, s.file) },
                        onDismiss = { updateViewModel.resetState() }
                    )
                }
                is UpdateUiState.Error -> {
                    InfoDialog(
                        title = "Update Error",
                        message = s.message,
                        onDismiss = { updateViewModel.resetState() }
                    )
                }
                else -> {}
            }

            MainContent(onUpdateClick = { showUpdateDialog = true })
        } else {
            if (updateResp != null && !updateResp.updateAvailable) {
                val prefs = remember { context.getSharedPreferences("update_cache", android.content.Context.MODE_PRIVATE) }
                LaunchedEffect(Unit) {
                    prefs.edit().remove("last_prompt_time").apply()
                }
            }
            MainContent(onUpdateClick = { showUpdateDialog = true })
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainContent(
    onUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var prevLoggedIn by remember { mutableStateOf(authState.isLoggedIn) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Deep-link handling (FCM notification tap -> notifications panel).
    // Cold start: launching intent is processed on first ON_RESUME.
    // Warm start: onNewIntent() -> setIntent() -> ON_RESUME fires again with the new intent.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? ComponentActivity
                val intent = activity?.intent
                if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
                    if (navController.currentDestination?.route != Screen.Notifications.route) {
                        navController.handleDeepLink(intent)
                    }
                    activity.setIntent(Intent())
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var isConnected by remember { mutableStateOf(isCurrentlyConnected(connectivityManager)) }
    var wasOffline by remember { mutableStateOf(!isConnected) }
    var showBackOnlineBanner by remember { mutableStateOf(false) }
    var showConnectionLostBanner by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected && wasOffline) {
            showBackOnlineBanner = true
            wasOffline = false
            NetworkMonitor.onBackOnline()
            delay(1500)
            showBackOnlineBanner = false
        } else if (!isConnected) {
            wasOffline = true
            showConnectionLostBanner = true
            delay(3000)
            showConnectionLostBanner = false
        }
    }

    LaunchedEffect(authState.isLoggedIn) {
        if (prevLoggedIn != authState.isLoggedIn) {
            prevLoggedIn = authState.isLoggedIn
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Movies.route,
        Screen.TVShows.route,
        Screen.Categories.route,
        Screen.Library.route,
        Screen.Zee5.route,
        Screen.Downloads.route,
        Screen.Profile.route
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in listOf(
            Screen.Home.route,
            Screen.Zee5.route,
            Screen.Library.route,
            Screen.Downloads.route,
            Screen.Profile.route,
            Screen.Categories.route
        ) && isConnected,
        drawerContent = {
            CategoryDrawerContent(
                onCategoryClick = { slug, name ->
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.CategoryPage.createRoute(slug, name))
                },
                onAllCategoriesClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Categories.route)
                },
                onUpdateClick = {
                    scope.launch { drawerState.close() }
                    onUpdateClick()
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column {
                    if (shouldShowBottomBar) {
                        BottomNavigationBar(
                            navController = navController
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavigation(
                    navController = navController,
                    isOnline = isConnected,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                AnimatedVisibility(
                    visible = showConnectionLostBanner,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
                ) {
                    NetworkErrorBanner()
                }

                AnimatedVisibility(
                    visible = showBackOnlineBanner,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
                ) {
                    BackOnlineBanner()
                }
            }
        }
    }
}

private fun isCurrentlyConnected(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
private fun NetworkErrorBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD32F2F))
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚠ Connection lost — some features may be unavailable",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BackOnlineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4CAF50))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓ Back Online",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaintenanceScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Under Maintenance",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onRetry,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Text("Try Again", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ForceUpdateScreen(
    data: UpdateResponse,
    updateViewModel: UpdateViewModel,
    context: android.content.Context
) {
    val updateState by updateViewModel.state.collectAsState()
    val apkRepo = remember { com.movie.app.best.data.repository.ApkUpdateRepository() }

    androidx.activity.compose.BackHandler(enabled = true) {}

    when (val s = updateState) {
        is UpdateUiState.Downloading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFE50914),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Downloading Update", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { s.progress / 100f },
                        color = Color(0xFFE50914),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${s.progress}%", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                }
            }
        }
        is UpdateUiState.DownloadComplete -> {
            LaunchedEffect(Unit) {
                apkRepo.installApk(context, s.file)
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF0D2818), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Download Complete", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Installing update...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF1A0A0A), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFFE50914),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Update Required",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        data.forceUpdateMessage.ifBlank { "This version is no longer supported. Please update to continue." },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "v${data.version}  ${data.downloadSizeMb} MB",
                        color = Color(0xFFE50914),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (data.whatsNew.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("What's New", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                val lines = data.whatsNew.lines()
                                    .map { it.removePrefix("## ").removePrefix("# ").trim() }
                                    .filter { it.isNotBlank() && !it.startsWith("What's New") }
                                lines.forEach { line ->
                                    val clean = line.removePrefix("- ").trim()
                                    if (clean.isNotBlank()) {
                                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text("  ", color = Color(0xFFE50914), fontSize = 12.sp)
                                            Text(clean, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Button(
                        onClick = { updateViewModel.startDownload(context, data.downloadUrl) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NormalUpdateDialog(
    data: UpdateResponse,
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
                        val clean = line.removePrefix("- ").trim()
                        if (clean.isNotBlank()) {
                            Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                Text("▸ ", color = Color(0xFFE50914), fontSize = 13.sp)
                                Text(clean, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
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
private fun DownloadProgressDialog(progress: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Downloading Update", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("$progress%", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
private fun InstallDialog(onInstall: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Complete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text("APK downloaded. Tap Install to update.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            androidx.compose.material3.Button(
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
