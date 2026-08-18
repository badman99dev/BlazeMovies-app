package com.movie.app.best

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movie.app.best.ui.screens.main.MainScreen
import com.movie.app.best.ui.theme.MovieAppTheme
import com.movie.app.best.util.FullscreenPlayerState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Nonce: har notification tap pe increment hota hai (cold + warm dono handle).
    // AppNavigation mein LaunchedEffect(nonce) har change pe fire karta hai.
    private val openNotificationNonce = mutableStateOf(0)

    private fun consumeNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(FcmService.EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            openNotificationNonce.value++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeNotificationIntent(intent)
        setContent {
            MovieAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Global punch-hole/notch-safe strip, app-wide — exactly like
                        // YouTube keeps a permanent solid black status-bar zone on every
                        // screen, and only drops it once a video goes truly full screen.
                        if (!FullscreenPlayerState.isActive) {
                            val cutoutSafeHeight = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                .asPaddingValues().calculateTopPadding()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxOf(cutoutSafeHeight, 24.dp))
                                    .background(Color.Black)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            MainScreen(openNotifications = openNotificationNonce.value)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationIntent(intent)
    }
}
