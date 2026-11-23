package com.movie.app.best.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.settings.VisualSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualCustomizationScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var navGlassDisabled by remember { mutableStateOf(VisualSettings.isNavGlassDisabled(context)) }
    var sliderBlurDisabled by remember { mutableStateOf(VisualSettings.isSliderBlurDisabled(context)) }
    var showRestartDialog by remember { mutableStateOf(false) }

    if (showRestartDialog) {
        RestartRequiredDialog(
            onCancel = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                restartApp(context)
            }
        )
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
                        text = "Visual Customization",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Appearance & performance",
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            windowInsets = WindowInsets.statusBars
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp)
        ) {
            SettingsSectionHeader(title = "Optimizations")

            SettingsGroupCard {
                OptimizationRow(
                    icon = Icons.Default.BlurOn,
                    gradient = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.30f),
                        Color(0xFF4338CA).copy(alpha = 0.15f)
                    ),
                    tint = Color(0xFF818CF8),
                    title = "Disable Nav Bar Glassmorphism",
                    subtitle = "Turn off the frosted blur to save battery & CPU",
                    checked = navGlassDisabled,
                    onCheckedChange = { checked ->
                        navGlassDisabled = checked
                        VisualSettings.setNavGlassDisabled(context, checked)
                        showRestartDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                OptimizationRow(
                    icon = Icons.Default.Wallpaper,
                    gradient = listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.30f),
                        Color(0xFF0E7490).copy(alpha = 0.15f)
                    ),
                    tint = Color(0xFF22D3EE),
                    title = "Disable Slider Background Blur",
                    subtitle = "Turn off the home hero backdrop blur effect",
                    checked = sliderBlurDisabled,
                    onCheckedChange = { checked ->
                        sliderBlurDisabled = checked
                        VisualSettings.setSliderBlurDisabled(context, checked)
                        showRestartDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tip: on low-end phones, disabling both effects reduces GPU / CPU load and saves battery. Your choice is saved immediately and applies after the next app restart.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun OptimizationRow(
    icon: ImageVector,
    gradient: List<Color>,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            gradientColors = gradient,
            contentColor = tint
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        MakkhanSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RestartRequiredDialog(
    onCancel: () -> Unit,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF141414),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f),
        title = {
            Text("Restart Required", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Your change is saved. It will apply only after the app restarts.\n\n" +
                    "Restart now, or tap Cancel — the change will apply next time the app starts."
            )
        },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("Restart", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

private fun restartApp(context: Context) {
    val appContext = context.applicationContext
    val intent = appContext.packageManager
        .getLaunchIntentForPackage(appContext.packageName)
        ?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
    if (intent != null) appContext.startActivity(intent)
    Handler(Looper.getMainLooper()).postDelayed({
        Process.killProcess(Process.myPid())
        Runtime.getRuntime().exit(0)
    }, 350)
}