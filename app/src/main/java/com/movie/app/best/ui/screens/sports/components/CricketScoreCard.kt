package com.movie.app.best.ui.screens.sports.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.movie.app.best.data.model.*
import com.movie.app.best.data.repository.CricketScoreUiData
import com.movie.app.best.ui.components.TeamFlagBadge
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.AppSurface
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun CricketScoreSection(
    scoreData: CricketScoreUiData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Divider separating Team Flags from Interactive Tabs
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // ── Swipeable Tabs Header: Scoreboard | Playing XI | Commentary ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(2.5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Scoreboard", "Playing XI", "Commentary")
            tabs.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) AppRed.copy(alpha = 0.85f) else Color.Transparent)
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading && scoreData == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
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
                        text = "Loading cricket data...",
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
                    text = "Live scorecard connecting...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            return@Column
        }

        // ── Swipeable Multi-Page Container ────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> LiveScoreboardPage(scoreData = scoreData)
                1 -> PlayingXiPage(scoreData = scoreData)
                2 -> CommentaryPage(scoreData = scoreData)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: LIVE SCOREBOARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LiveScoreboardPage(scoreData: CricketScoreUiData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Status row: e.g. "Only-TEST" or "1st-TEST"
        if (scoreData.matchDesc.isNotBlank() || scoreData.status.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFinished = scoreData.status.equals("finished", ignoreCase = true)
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

                if (scoreData.matchDesc.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scoreData.matchDesc,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Primary Score with Arcade Rolling Counter ────────────────────────
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
                    // Arcade Rolling Digits
                    ArcadeScoreTicker(scoreText = scoreData.scoreMain.ifBlank { "0/0" })

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

        // Equation / Result Note
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

        // ── Recent Overs Strip (Locked 64dp height, no jitter) ────────────────
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

        // ── Stationary Batters (Authentic Bat Icon) ───────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
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
            BatsmanRow(batsman = scoreData.slot1Batsman)
            Spacer(modifier = Modifier.height(4.dp))
            BatsmanRow(batsman = scoreData.slot2Batsman)
        }

        // ── Active Bowler ─────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: PLAYING XI & SQUADS (Split with Vertical Divider)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlayingXiPage(scoreData: CricketScoreUiData) {
    val team1 = scoreData.team1Squad
    val team2 = scoreData.team2Squad

    if (team1 == null && team2 == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Playing XI will be updated once confirmed by toss.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Team 1
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        ) {
            // Team Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                TeamFlagBadge(flagUrl = team1?.flag, size = 26.dp, borderWidth = 1.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = team1?.name ?: team1?.shortName ?: "Team 1",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            team1?.players?.forEach { player ->
                SquadPlayerRow(player = player)
                Spacer(modifier = Modifier.height(5.dp))
            }
        }

        // Center Vertical Divider
        Box(
            modifier = Modifier
                .width(0.8.dp)
                .height(IntrinsicSize.Max)
                .background(Color.White.copy(alpha = 0.12f))
        )

        // Right Column: Team 2
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
        ) {
            // Team Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                TeamFlagBadge(flagUrl = team2?.flag, size = 26.dp, borderWidth = 1.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = team2?.name ?: team2?.shortName ?: "Team 2",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            team2?.players?.forEach { player ->
                SquadPlayerRow(player = player)
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun SquadPlayerRow(player: CrexSquadPlayer) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularAvatarBadge(imageUrl = player.head, size = 22.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.name ?: "-",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (player.captain) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "(C)",
                        color = Color(0xFFFBBF24),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (player.keeper) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "(WK)",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!player.role.isNullOrBlank()) {
                Text(
                    text = player.role,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: BALL FEED & COMMENTARY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CommentaryPage(scoreData: CricketScoreUiData) {
    val balls = scoreData.commentaryBalls

    if (balls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ball-by-ball commentary will stream here as the match progresses.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        balls.forEach { ball ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Over tag: e.g. "14.2"
                Text(
                    text = ball.over ?: "-",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp)
                )

                // Ball chip
                BallCircle(ball = ball.ball ?: "0")

                Spacer(modifier = Modifier.width(8.dp))

                // Text: "Bowler to Batter"
                Text(
                    text = ball.text ?: "-",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Score tag: "52/6"
                if (!ball.score.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ball.score,
                        color = Color(0xFF00B4D8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ARCADE ROLLING SCORE TICKER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ArcadeScoreTicker(scoreText: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
    ) {
        scoreText.forEachIndexed { index, char ->
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(350)) { -it } + fadeIn())
                            .togetherWith(slideOutVertically(animationSpec = tween(350)) { it } + fadeOut())
                    },
                    label = "digit_$index"
                ) { targetChar ->
                    Text(
                        text = targetChar.toString(),
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 0.5.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OVER BLOCK CARD (Fixed 64dp Height & 160dp Min-Width)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverBlockCard(overItem: CrexLastOver) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "= ${overItem.total}",
                    color = Color(0xFF00B4D8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

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
        isFour || isSix -> Color(0xFF0096C7) // Vibrant Cyan / Teal
        isExtra -> Color(0xFFFF9800)
        else -> Color(0xFF1E293B) // Dark pill
    }

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
            color = Color.White,
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
        // Authentic Vertical Cricket Bat Icon Slot
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isStriker) {
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
private fun CircularAvatarBadge(imageUrl: String?, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
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
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}
