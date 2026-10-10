package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.R
import com.movie.app.best.data.model.CrexBowlerStats
import com.movie.app.best.data.model.CrexPlayerStats
import com.movie.app.best.data.repository.CricketScoreUiData
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.AppSurface
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen

@Composable
fun CricketScoreCard(
    scoreData: CricketScoreUiData?,
    isLoading: Boolean,
    syncWithStream: Boolean,
    onToggleStreamSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Top Header: Match Status & Stream-Sync Toggle ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(AppRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (scoreData?.status?.equals("finished", ignoreCase = true) == true) "COMPLETED" else "LIVE SCORE",
                            color = if (scoreData?.status?.equals("finished", ignoreCase = true) == true) Color.White.copy(alpha = 0.7f) else AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (scoreData?.matchDesc?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = scoreData.matchDesc,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Stream Sync Switch (30s delay toggle) ─────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (syncWithStream) "Sync (30s)" else "Instant",
                        color = if (syncWithStream) Color(0xFFFFB74D) else InfoBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = syncWithStream,
                        onCheckedChange = onToggleStreamSync,
                        modifier = Modifier.size(width = 34.dp, height = 20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFFB74D),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading && scoreData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = AppRed,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syncing live cricket score...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                return@Column
            }

            if (scoreData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scorecard will be updated once play commences",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                return@Column
            }

            // ── Primary Score Display ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = scoreData.battingTeamName.ifBlank { "Batting Team" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Baseline) {
                        Text(
                            text = scoreData.scoreRaw.ifBlank { "0/0" },
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (scoreData.overs != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${scoreData.overs} ov)",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Run Rates & Target
                Column(horizontalAlignment = Alignment.End) {
                    if (scoreData.crr != null && scoreData.crr > 0.0) {
                        Text(
                            text = "CRR: ${String.format("%.2f", scoreData.crr)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (scoreData.target != null && scoreData.target > 0) {
                        Text(
                            text = "Target: ${scoreData.target}" + if (scoreData.rrr != null) " (RRR: ${String.format("%.2f", scoreData.rrr)})" else "",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Equation / Result Banner
            if (scoreData.equation?.isNotBlank() == true || scoreData.statusText.isNotBlank()) {
                val bannerText = scoreData.equation?.takeIf { it.isNotBlank() } ?: scoreData.statusText
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bannerText,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // ── Recent Balls Strip ────────────────────────────────────────────
            if (scoreData.recentBalls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent: ",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(scoreData.recentBalls) { ball ->
                            RecentBallChip(ball = ball)
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f)
            )

            // ── Stationary Batsmen Rows with Animated Sliding Bat Indicator ───
            Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Batter",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 28.dp)
                    )
                    Text(
                        text = "R (B)",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "4s",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "6s",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "SR",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Slot 1 Batsman (Stationary position)
                BatsmanRow(batsman = scoreData.slot1Batsman)

                Spacer(modifier = Modifier.height(4.dp))

                // Slot 2 Batsman (Stationary position)
                BatsmanRow(batsman = scoreData.slot2Batsman)
            }

            // ── Active Bowler Row ─────────────────────────────────────────────
            if (scoreData.bowler != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bowler",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 28.dp)
                    )
                    Text(
                        text = "O",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "M",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(26.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "R",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "W",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "ECO",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                BowlerRow(bowler = scoreData.bowler)
            }
        }
    }
}

@Composable
private fun BatsmanRow(batsman: CrexPlayerStats?) {
    if (batsman == null) return

    val isStriker = batsman.strike

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isStriker) Color.White.copy(alpha = 0.04f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated Bat Indicator Slot (Bat slides/appears smoothly for striker)
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isStriker,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cricket_bat),
                    contentDescription = "Striker",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Circular Player Avatar
        CircularAvatarBadge(imageUrl = batsman.head)

        Spacer(modifier = Modifier.width(8.dp))

        // Batter Name
        Text(
            text = batsman.shortName?.ifBlank { batsman.name } ?: batsman.name ?: "-",
            color = if (isStriker) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 12.5.sp,
            fontWeight = if (isStriker) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // R (B)
        Text(
            text = "${batsman.runs} (${batsman.balls})",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )

        // 4s
        Text(
            text = "${batsman.fours}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        // 6s
        Text(
            text = "${batsman.sixes}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        // SR
        Text(
            text = String.format("%.1f", batsman.strikeRate),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BowlerRow(bowler: CrexBowlerStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(24.dp))

        // Circular Bowler Avatar
        CircularAvatarBadge(imageUrl = bowler.head)

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = bowler.shortName?.ifBlank { bowler.name } ?: bowler.name ?: "-",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = bowler.overs ?: "-",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = "${bowler.maidens}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = "${bowler.runs}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = "${bowler.wickets ?: 0}",
            color = if ((bowler.wickets ?: 0) > 0) AppRed else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if ((bowler.wickets ?: 0) > 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = String.format("%.1f", bowler.economy),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CircularAvatarBadge(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(AppSurface)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun RecentBallChip(ball: String) {
    val b = ball.trim().lowercase()
    val isWicket = b.contains("w") && !b.contains("wd")
    val isBoundary4 = b == "4"
    val isBoundary6 = b == "6"
    val isExtra = b.contains("wd") || b.contains("nb")

    val bgColor = when {
        isWicket -> AppRed
        isBoundary6 -> Color(0xFFFFD54F)
        isBoundary4 -> SuccessGreen
        isExtra -> Color(0xFFFF9800)
        else -> Color.White.copy(alpha = 0.12f)
    }

    val textColor = when {
        isBoundary6 -> Color.Black
        isWicket || isBoundary4 || isExtra -> Color.White
        else -> Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ball,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
