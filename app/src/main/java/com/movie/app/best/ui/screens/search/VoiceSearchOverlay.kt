package com.movie.app.best.ui.screens.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun VoiceSearchOverlay(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var liveText by remember { mutableStateOf("") }
    var rmsDb by remember { mutableFloatStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Suggested examples matching YouTube's "Try saying" list
    val suggestionList = remember {
        listOf(
            "\"Spider-Man: No Way Home\"",
            "\"Avengers: Endgame\"",
            "\"Interstellar\"",
            "\"Mirzapur Season 3\"",
            "\"The Dark Knight\"",
            "\"Inception\""
        ).shuffled().take(3)
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun stopListeningSession() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        isListening = false
        rmsDb = 0f
    }

    fun startListeningSession() {
        if (speechRecognizer == null) {
            isListening = false
            return
        }
        try {
            liveText = ""
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    liveText = ""
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    rmsDb = rmsdB.coerceAtLeast(0f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isListening = false
                    rmsDb = 0f
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    rmsDb = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawText = matches?.firstOrNull()?.trim() ?: ""

                    // Filter common filler words
                    val cleaned = rawText
                        .replace(Regex("(?i)\\b(movie|film|series|show|season|episode|dikhao|chalao|play|lagao)\\b"), "")
                        .trim()
                    val finalText = if (cleaned.isNotBlank()) cleaned else rawText

                    if (finalText.isNotBlank()) {
                        liveText = finalText
                        onResult(finalText)
                    } else {
                        isListening = false
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = partialList?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        liveText = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                }
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer.startListening(intent)
            isListening = true
        } catch (_: Exception) {
            isListening = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListeningSession()
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Infinite animation for circular wavefronts
    val infiniteTransition = rememberInfiniteTransition(label = "wavefrontTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Animated mic button background color: Red when active, Dark Grey when inactive
    val micButtonColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFFFF0033) else Color(0xFF272727),
        animationSpec = tween(durationMillis = 300),
        label = "micButtonColor"
    )

    // AMOLED pure black root container using Column so Top Bar & Content NEVER overlap
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
    ) {
        // Top Bar: Cross / Close button to go back to Search page (matches AppHeader geometry)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    stopListeningSession()
                    onDismiss()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Voice Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Main content column placed completely below the Top Bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isListening) {
                // ACTIVE STATE: Shows "Listening..." or live spoken words
                val displayText = if (liveText.isNotBlank()) liveText else "Listening..."
                Text(
                    text = displayText,
                    color = if (liveText.isNotBlank()) Color.White else Color(0xFFE2E2E2),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Single hint text when actively listening
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Try saying",
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = suggestionList.firstOrNull() ?: "\"Avengers: Endgame\"",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // INACTIVE / STOPPED STATE: Shows "Try saying" title + 3 suggestions
                Text(
                    text = "Try saying",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                suggestionList.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = Color(0xFFD0D0D0),
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Mic Button Area (with Circular Wavefronts in active state)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated Circular Wavefront Ripples (Only active when listening!)
                if (isListening) {
                    val audioBoost = (rmsDb / 12f).coerceIn(0f, 0.40f)

                    // Wave 1
                    WaveRing(
                        phase = wavePhase,
                        audioBoost = audioBoost,
                        baseSize = 88.dp,
                        maxScale = 2.2f
                    )

                    // Wave 2 (offset by 0.33)
                    WaveRing(
                        phase = (wavePhase + 0.33f) % 1f,
                        audioBoost = audioBoost,
                        baseSize = 88.dp,
                        maxScale = 2.2f
                    )

                    // Wave 3 (offset by 0.66)
                    WaveRing(
                        phase = (wavePhase + 0.66f) % 1f,
                        audioBoost = audioBoost,
                        baseSize = 88.dp,
                        maxScale = 2.2f
                    )

                    // Dark circular background halo (YouTube baseplate)
                    Box(
                        modifier = Modifier
                            .size(114.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                    )
                }

                // Circular Mic Button: RED when listening, DARK GREY when stopped
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(micButtonColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isListening) {
                                stopListeningSession()
                            } else {
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    startListeningSession()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search Mic",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Inactive state hint: "Tap microphone to try again"
            if (!isListening) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap microphone to try again",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WaveRing(
    phase: Float,
    audioBoost: Float,
    baseSize: androidx.compose.ui.unit.Dp,
    maxScale: Float
) {
    val scale = 1.0f + (maxScale - 1.0f) * phase + audioBoost
    val alpha = ((1.0f - phase) * 0.40f * (1.0f + audioBoost)).coerceIn(0f, 0.6f)

    Box(
        modifier = Modifier
            .size(baseSize)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2C))
    )
}
