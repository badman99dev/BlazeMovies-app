package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun ReportDrawer(
    movieId: Int,
    currentModeration: com.movie.app.best.data.model.ContentModeration? = null,
    isModerator: Boolean = false,
    onSubmit: (movieId: Int, reportType: String, reason: String) -> Unit,
    onInstantCheck: (movieId: Int, reportType: String, reason: String) -> Unit = { _, _, _ -> },
    onModeratorVerdict: (movieId: Int, poster: String, screenshots: String, storyline: String, reasoning: String) -> Unit = { _, _, _, _, _ -> },
    onDismiss: () -> Unit,
    isSubmitting: Boolean = false
) {
    var selectedType by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showCustomFlag by remember { mutableStateOf(false) }

    val submitInteraction = remember { MutableInteractionSource() }
    val instantInteraction = remember { MutableInteractionSource() }
    val submitPressed by submitInteraction.collectIsPressedAsState()
    val instantPressed by instantInteraction.collectIsPressedAsState()
    val submitScale by animateFloatAsState(
        targetValue = if (submitPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 600f),
        label = "submitScale"
    )
    val instantScale by animateFloatAsState(
        targetValue = if (instantPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 600f),
        label = "instantScale"
    )

    if (showCustomFlag && isModerator) {
        CustomFlagModal(
            movieId = movieId,
            currentModeration = currentModeration,
            onSubmit = onModeratorVerdict,
            onDismiss = { showCustomFlag = false }
        )
        return
    }

    val isCurrentlyFlagged = currentModeration?.hasAnyFlag == true

    val reportTypes = if (isCurrentlyFlagged) {
        listOf("sexual" to "🚫 Sexual / Inappropriate", "objection" to "✏️ Object to the moderation / Request an edit")
    } else {
        listOf("sexual" to "🚫 Sexual / Inappropriate")
    }

    val isObjection = selectedType == "objection"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Report Content",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Select the type of inappropriate content",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))

            if (isModerator) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { showCustomFlag = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add/Edit Custom Flag (mod)",
                                color = Color(0xFFB388FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Directly set moderation flags without AI",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))
            }

            reportTypes.forEach { (type, label) ->
                val isSelected = selectedType == type
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedType = type },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE50914).copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { 
                    Text(
                        if (isObjection) "Explain your objection (required)" else "Reason (optional)", 
                        color = Color.White.copy(alpha = 0.5f)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        if (selectedType.isNotEmpty() && (!isObjection || reason.isNotBlank())) {
                            onSubmit(movieId, selectedType, reason)
                        }
                    },
                    interactionSource = submitInteraction,
                    enabled = !isSubmitting && selectedType.isNotEmpty() && (!isObjection || reason.isNotBlank()),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .graphicsLayer { scaleX = submitScale; scaleY = submitScale },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE50914),
                        disabledContainerColor = Color(0xFFE50914).copy(alpha = 0.3f)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Submitting...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isObjection) "Submit Objection" else "Submit Report",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                }
            }

            if (isModerator) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (selectedType.isNotEmpty() && (!isObjection || reason.isNotBlank())) {
                            onInstantCheck(movieId, selectedType, reason)
                        }
                    },
                    interactionSource = instantInteraction,
                    enabled = !isSubmitting && selectedType.isNotEmpty() && (!isObjection || reason.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer { scaleX = instantScale; scaleY = instantScale },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C4DFF),
                        disabledContainerColor = Color(0xFF7C4DFF).copy(alpha = 0.25f)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Checking with AI...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("⚡ Instant Check (mod) — AI + instant verdict", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Text(
                    text = "Runs AI analysis immediately — shows verdict, flags & celebration. Regular submit just queues the report for review.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CustomFlagModal(
    movieId: Int,
    currentModeration: com.movie.app.best.data.model.ContentModeration? = null,
    onSubmit: (movieId: Int, poster: String, screenshots: String, storyline: String, reasoning: String) -> Unit,
    onDismiss: () -> Unit
) {
    var flagPoster by remember { mutableStateOf(currentModeration?.poster == "sexual") }
    var flagScreenshots by remember { mutableStateOf(currentModeration?.screenshots == "sexual") }
    var flagStoryline by remember { mutableStateOf(currentModeration?.storyline == "sexual") }
    var reasoning by remember { mutableStateOf("Moderated by moderator") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color(0xFF7C4DFF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Custom Flag (Mod)",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Set moderation flags directly — no AI check",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(20.dp))

            FlagToggle(
                label = "Flag poster as sexual",
                isOn = flagPoster,
                onToggle = { flagPoster = it }
            )

            Spacer(Modifier.height(10.dp))

            FlagToggle(
                label = "Flag screenshots as sexual",
                isOn = flagScreenshots,
                onToggle = { flagScreenshots = it }
            )

            Spacer(Modifier.height(10.dp))

            FlagToggle(
                label = "Flag storyline as sexual",
                isOn = flagStoryline,
                onToggle = { flagStoryline = it }
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = reasoning,
                onValueChange = { reasoning = it },
                label = { Text("Reasoning", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "💡 If you add a reason, AI can modify your decision",
                color = Color(0xFFFF9800).copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        onSubmit(
                            movieId,
                            if (flagPoster) "sexual" else "safe",
                            if (flagScreenshots) "sexual" else "safe",
                            if (flagStoryline) "sexual" else "none",
                            reasoning
                        )
                    },
                    modifier = Modifier.weight(1.5f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Flag", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReportSuccessModal(
    message: String,
    onDismiss: () -> Unit
) {
    val displayMessage = message.ifBlank {
        "Your report successfully submitted. We will review and respond in a few minutes. Thank you for helping keep BlazeMovies safe."
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.97f), RoundedCornerShape(20.dp))
            .padding(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Report Submitted",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = displayMessage,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FlagToggle(
    label: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isOn) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) Color(0xFFFF1744).copy(alpha = 0.15f) else Color(0xFF2A2A2A)
        ),
        border = if (isOn) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = if (isOn) Color(0xFFFF1744) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    color = if (isOn) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOn) Color(0xFFFF1744) else Color(0xFF444444))
                    .clickable { onToggle(!isOn) },
                contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun ReportWaitingPopup(isObjection: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isObjection) "Objection received!" else "Report successfully received!",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isObjection) "Our AI is re-examining the content based on your objection. This may take up to 20 seconds." else "Wait a few seconds, our agent is checking your report. This may take up to 20 seconds to respond.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.CircularProgressIndicator(
                color = Color(0xFFE50914),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ReportResultModal(
    moderation: com.movie.app.best.data.model.ContentModerationResponse,
    previousModeration: com.movie.app.best.data.model.ContentModerationResponse? = null,
    isObjection: Boolean = false,
    onDismiss: () -> Unit
) {
    val prevCm = previousModeration
    val hasPrevious = prevCm != null

    val posterChanged = hasPrevious && moderation.poster != prevCm.poster
    val screenshotsChanged = hasPrevious && moderation.screenshots != prevCm.screenshots
    val storylineChanged = hasPrevious && moderation.storyline != prevCm.storyline
    val anyChange = posterChanged || screenshotsChanged || storylineChanged

    val newFlags = mutableListOf<Pair<String, String>>()
    val removedFlags = mutableListOf<Pair<String, String>>()

    if (hasPrevious) {
        if (moderation.poster == "sexual" && prevCm.poster != "sexual") newFlags.add("Poster" to "Poster now flagged")
        if (moderation.screenshots == "sexual" && prevCm.screenshots != "sexual") newFlags.add("Screenshots" to "Screenshots now flagged")
        if (moderation.storyline == "sexual" && prevCm.storyline != "sexual") newFlags.add("Storyline" to "Storyline now flagged")
        if (moderation.poster != "sexual" && prevCm.poster == "sexual") removedFlags.add("Poster" to "Poster unflagged ✓")
        if (moderation.screenshots != "sexual" && prevCm.screenshots == "sexual") removedFlags.add("Screenshots" to "Screenshots unflagged ✓")
        if (moderation.storyline != "sexual" && prevCm.storyline == "sexual") removedFlags.add("Storyline" to "Storyline unflagged ✓")
    } else {
        if (moderation.poster == "sexual") newFlags.add("Poster" to "Poster flagged successfully")
        if (moderation.storyline == "sexual") newFlags.add("Storyline" to "Storyline flagged successfully")
        if (moderation.screenshots == "sexual") newFlags.add("Screenshots" to "Screenshots flagged successfully")
    }

    val hasAnyFlag = newFlags.isNotEmpty()
    val showCelebration = anyChange

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.97f), RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isObjection && !anyChange && hasPrevious) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF90A4AE),
                    modifier = Modifier.size(40.dp)
                )
            } else if (hasAnyFlag) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    isObjection && anyChange -> "Moderation Updated"
                    isObjection && !anyChange && hasPrevious -> "Verdict Unchanged"
                    hasAnyFlag -> "Content Reviewed"
                    else -> "All Clear"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            if (moderation.reasoning.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isObjection) "AI Re-examination" else "AI Analysis",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = moderation.reasoning,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            newFlags.forEach { (label, message) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = message,
                        color = Color(0xFFFF8A80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            removedFlags.forEach { (label, message) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = message,
                        color = Color(0xFF81C784),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (hasAnyFlag || removedFlags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when {
                        isObjection && anyChange -> "Moderation has been updated based on re-examination."
                        isObjection && !anyChange -> "AI re-examined the content — original verdict stands."
                        hasAnyFlag -> "Thank you for contributing to making our platform safe."
                        else -> "No inappropriate content detected. Content is safe."
                    },
                    color = when {
                        isObjection && anyChange -> Color(0xFF4CAF50)
                        isObjection -> Color.White.copy(alpha = 0.6f)
                        hasAnyFlag -> Color(0xFF4CAF50)
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isObjection) "AI re-examined the content — original verdict stands." else "No inappropriate content detected. Content is safe.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
