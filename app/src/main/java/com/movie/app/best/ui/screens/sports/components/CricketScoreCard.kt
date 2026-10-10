package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.movie.app.best.data.model.CrexLastOver
import com.movie.app.best.data.model.CrexPlayerStats
import com.movie.app.best.data.repository.CricketScoreUiData
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.AppSurface
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen

@Composable
fun CricketScoreSection(
    scoreData: CricketScoreUiData?,
    isLoading: Boolean,
    syncWithStream: Boolean,
    onToggleStreamSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        // Divider separating Team Flags from Score Section
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // ── Top Header: Status & Stream-Sync Toggle ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                val isFinished = scoreData?.status?.equals("finished", ignoreCase = true) == true
                Box(
                    modifier = Modifier
                        .background(
                            if (isFinished) Color.White.copy(alpha = 0.12f) else AppRed.copy(alpha = 0.18f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = if (isFinished) "FINISHED" else "LIVE SCORE",
                        color = if (isFinished) Color.White.copy(alpha = 0.7f) else AppRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (!scoreData?.matchDesc.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scoreData!!.matchDesc,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Stream Sync Switch (Proper sizing, non-clipped) ───────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(start = 10.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = if (syncWithStream) "Sync (30s)" else "Instant",
                    color = if (syncWithStream) Color(0xFFFFB74D) else InfoBlue,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = syncWithStream,
                    onCheckedChange = onToggleStreamSync,
                    modifier = Modifier.height(24.dp),
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
                    .height(70.dp),
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
                        text = "Loading live score...",
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
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live scorecard connecting...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            return@Column
        }

        // ── Primary Score & Clean Overs Display ───────────────────────────────
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = scoreData.scoreMain.ifBlank { "0/0" },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (!scoreData.oversFormatted.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = scoreData.oversFormatted,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Run Rates & Target
            Column(horizontalAlignment = Alignment.End) {
                if (scoreData.crr != null && scoreData.crr > 0.0) {
                    Text(
                        text = "CRR: ${String.format("%.2f", scoreData.crr)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (scoreData.target != null && scoreData.target > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target: ${scoreData.target}" + if (scoreData.rrr != null) " (RRR: ${String.format("%.2f", scoreData.rrr)})" else "",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Equation / Result Note (Only if meaningful text)
        if (!scoreData.equation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scoreData.equation,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else if (scoreData.statusText.isNotBlank() && scoreData.statusText.lowercase().contains("won")) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scoreData.statusText,
                color = SuccessGreen,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ── Over-by-Over Breakdown Cards Strip ────────────────────────────────
        if (scoreData.lastOvers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recent Overs",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(scoreData.lastOvers) { overItem ->
                    OverBlockCard(overItem = overItem)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f)
        )

        // ── Stationary Batsmen Rows with Authentic Bat Indicator ──────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batter",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 28.dp)
                )
                Text(
                    text = "R (B)",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(52.dp),
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

        // ── Active Bowler Row ─────────────────────────────────────────────────
        if (scoreData.bowler != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bowler",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 28.dp)
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

@Composable
private fun OverBlockCard(overItem: CrexLastOver) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = overItem.over ?: "Over",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "= ${overItem.total}",
                    color = Color(0xFF00B4D8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                overItem.balls.forEach { ball ->
                    BallCircle(ball = ball)
                }
            }
        }
    }
}

@Composable
private fun BallCircle(ball: String) {
    val b = ball.trim().lowercase()
    val isWicket = b.contains("w") && !b.contains("wd")
    val isFour = b == "4"
    val isSix = b == "6"
    val isExtra = b.contains("wd") || b.contains("nb") || b.contains("lb") || b.contains("b")

    val bgColor = when {
        isWicket -> AppRed
        isFour || isSix -> Color(0xFF0096C7) // Vibrant Cyan / Teal like user screenshot
        isExtra -> Color(0xFFFF9800)
        else -> Color(0xFF1E293B) // Dark pill for 0, 1, 2, 3
    }

    val textColor = Color.White

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ball,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
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
        // Authentic Cricket Bat Icon Slot
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isStriker,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cricket_bat),
                    contentDescription = "Striker",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(17.dp)
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

        // Runs (Balls)
        Text(
            text = "${batsman.runs} (${batsman.balls})",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp),
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
