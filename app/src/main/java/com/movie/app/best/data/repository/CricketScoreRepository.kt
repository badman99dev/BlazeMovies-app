package com.movie.app.best.data.repository

import com.google.gson.GsonBuilder
import com.movie.app.best.data.model.*
import com.movie.app.best.data.remote.CricketApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CricketScoreUiData(
    val matchId: String = "",
    val seriesName: String = "",
    val matchDesc: String = "",
    val status: String = "", // "live", "upcoming", "finished"
    val statusText: String = "",
    val battingTeamName: String = "",
    val battingTeamShort: String = "",
    val scoreMain: String = "", // Clean "305/8" or "210 & 140/3"
    val oversFormatted: String? = null, // Clean "107.0 ov"
    val runs: Int? = null,
    val wickets: Int? = null,
    val overs: Float? = null,
    val crr: Double? = null,
    val rrr: Double? = null,
    val target: Int? = null,
    val equation: String? = null,
    val slot1Batsman: CrexPlayerStats? = null,
    val slot2Batsman: CrexPlayerStats? = null,
    val bowler: CrexBowlerStats? = null,
    val recentBalls: List<String> = emptyList(),
    val lastOvers: List<CrexLastOver> = emptyList(),
    val team1Squad: CrexTeamSquad? = null,
    val team2Squad: CrexTeamSquad? = null,
    val commentaryBalls: List<CrexCommentaryBall> = emptyList(),
    val isDelayedByStreamSync: Boolean = true
)

@Singleton
class CricketScoreRepository @Inject constructor() {

    private val api: CricketApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("https://lscorexapi.onrender.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CricketApiService::class.java)
    }

    // Circular snapshot buffer for 45s stream sync
    private val snapshotBuffer = Collections.synchronizedList(mutableListOf<Pair<Long, CrexMatchDetail>>())

    // Stationary batsman tracking
    private var anchoredSlot1: CrexPlayerStats? = null
    private var anchoredSlot2: CrexPlayerStats? = null

    // Cached squad data
    private var cachedSquadTeams: CrexSquadTeams? = null
    private var cachedSquadMatchId: String = ""

    fun resetBuffer() {
        snapshotBuffer.clear()
        anchoredSlot1 = null
        anchoredSlot2 = null
        cachedSquadTeams = null
        cachedSquadMatchId = ""
    }

    suspend fun resolveMatchId(
        teamA: String,
        teamB: String,
        seriesName: String? = null,
        startTime: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val tA = cleanTeamName(teamA)
        val tB = cleanTeamName(teamB)
        val sName = seriesName?.trim().orEmpty()

        // Case 1: If teamA and teamB are the same (or one is blank), treat as Series mode
        val areTeamsIdentical = tA.isNotBlank() && tA.equals(tB, ignoreCase = true)
        if (areTeamsIdentical || (tA.isNotBlank() && tB.isBlank()) || (tB.isNotBlank() && tA.isBlank())) {
            val seriesCandidate = when {
                tA.isNotBlank() -> tA
                tB.isNotBlank() -> tB
                else -> sName
            }
            if (seriesCandidate.isNotBlank()) {
                try {
                    val res = api.findMatchBySeries(series = seriesCandidate, startTime = startTime)
                    if (res.matched && !res.match?.id.isNullOrBlank()) {
                        return@withContext res.match?.id
                    }
                } catch (_: Exception) {}
            }
        }

        if (tA.isBlank() && tB.isBlank() && sName.isNotBlank()) {
            try {
                val res = api.findMatchBySeries(series = sName, startTime = startTime)
                if (res.matched && !res.match?.id.isNullOrBlank()) {
                    return@withContext res.match?.id
                }
            } catch (_: Exception) {}
            return@withContext null
        }

        if (tA.isBlank() || tB.isBlank()) return@withContext null

        // 1. Try find-match with startTime if provided
        if (!startTime.isNullOrBlank()) {
            try {
                val res = api.findMatch(team1 = tA, team2 = tB, startTime = startTime)
                if (res.matched && !res.match?.id.isNullOrBlank()) {
                    return@withContext res.match?.id
                }
            } catch (_: Exception) {}
        }

        // 2. Try find-match by team names only
        try {
            val res2 = api.findMatchByTeams(team1 = tA, team2 = tB)
            if (res2.matched && !res2.match?.id.isNullOrBlank()) {
                return@withContext res2.match?.id
            }
        } catch (_: Exception) {}

        // 3. Try reversing teams (team2 = teamA, team1 = teamB)
        try {
            val res3 = api.findMatchByTeams(team1 = tB, team2 = tA)
            if (res3.matched && !res3.match?.id.isNullOrBlank()) {
                return@withContext res3.match?.id
            }
        } catch (_: Exception) {}

        // 4. Fallback: If seriesName is available, probe series fixtures
        if (sName.isNotBlank()) {
            try {
                val resSeries = api.findMatchBySeries(series = sName, startTime = startTime)
                if (resSeries.matched && !resSeries.match?.id.isNullOrBlank()) {
                    return@withContext resSeries.match?.id
                }
            } catch (_: Exception) {}
        }

        null
    }

    suspend fun fetchMatchDetail(matchId: String, syncWithStream: Boolean = true): CricketScoreUiData? = withContext(Dispatchers.IO) {
        try {
            val delayParam = if (syncWithStream) 45 else 0
            val response = api.getMatchDetail(id = matchId, delay = delayParam, json = true)
            val match = response.match ?: return@withContext null

            // Lazy fetch squad if not yet cached for this match
            if (cachedSquadTeams == null || cachedSquadMatchId != matchId) {
                try {
                    val sqRes = api.getMatchSquad(id = matchId, json = true)
                    if (sqRes.success && sqRes.squad?.teams != null) {
                        cachedSquadTeams = sqRes.squad.teams
                        cachedSquadMatchId = matchId
                    }
                } catch (_: Exception) {}
            }

            val now = System.currentTimeMillis()
            snapshotBuffer.add(Pair(now, match))

            // Prune snapshots older than 3 minutes
            val cutOff = now - 180_000L
            snapshotBuffer.removeAll { it.first < cutOff }

            // Select snapshot based on 45s stream sync
            val selectedDetail: CrexMatchDetail
            val isDelayed: Boolean

            if (syncWithStream) {
                val targetTime = now - 45_000L // 45s buffer
                val olderSnapshots = snapshotBuffer.filter { it.first <= targetTime }
                selectedDetail = if (olderSnapshots.isNotEmpty()) {
                    olderSnapshots.maxByOrNull { it.first }?.second ?: match
                } else {
                    snapshotBuffer.firstOrNull()?.second ?: match
                }
                isDelayed = true
            } else {
                selectedDetail = match
                isDelayed = false
            }

            // Fetch latest commentary (with fallback to rich.recentBalls)
            val commBalls = try {
                val commRes = api.getMatchCommentary(id = matchId, json = true)
                if (commRes.recentBalls.isNotEmpty()) {
                    commRes.recentBalls.take(30)
                } else {
                    selectedDetail.rich?.recentBalls?.map { b ->
                        CrexCommentaryBall(
                            over = b.over,
                            ball = b.ball,
                            text = b.text,
                            score = b.score,
                            commentary = b.commentary,
                            isBoundary = b.isBoundary,
                            isWicket = b.isWicket,
                            isExtra = b.isExtra
                        )
                    } ?: emptyList()
                }
            } catch (_: Exception) {
                selectedDetail.rich?.recentBalls?.map { b ->
                    CrexCommentaryBall(
                        over = b.over,
                        ball = b.ball,
                        text = b.text,
                        score = b.score,
                        commentary = b.commentary,
                        isBoundary = b.isBoundary,
                        isWicket = b.isWicket,
                        isExtra = b.isExtra
                    )
                } ?: emptyList()
            }

            mapToUiData(
                detail = selectedDetail,
                isDelayed = isDelayed,
                squadTeams = cachedSquadTeams,
                commentaryBalls = commBalls
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToUiData(
        detail: CrexMatchDetail,
        isDelayed: Boolean,
        squadTeams: CrexSquadTeams?,
        commentaryBalls: List<CrexCommentaryBall>
    ): CricketScoreUiData {
        val rich = detail.rich
        val teams = detail.teams

        val team1 = teams?.team1
        val team2 = teams?.team2

        val battingCode = rich?.battingTeamCode
        val battingTeam = when {
            battingCode != null && battingCode.equals(team1?.code, ignoreCase = true) -> team1
            battingCode != null && battingCode.equals(team2?.code, ignoreCase = true) -> team2
            rich?.battingTeam != null && rich.battingTeam.equals(team1?.name, ignoreCase = true) -> team1
            rich?.battingTeam != null && rich.battingTeam.equals(team2?.name, ignoreCase = true) -> team2
            team1?.score != null && team2?.score == null -> team1
            team2?.score != null && team1?.score == null -> team2
            else -> team1
        }

        val battingName = rich?.battingTeam ?: battingTeam?.name ?: ""
        val battingShort = rich?.battingTeamShort ?: battingTeam?.shortName ?: battingTeam?.code ?: ""

        val scoreObj = battingTeam?.score
        val rawScoreStr = battingTeam?.scoreRaw

        // CREX regex parser to separate main score from overs cleanly (removes unclosed brackets)
        val (scoreMain, oversFormatted) = parseScoreParts(
            raw = rawScoreStr,
            fallbackRuns = scoreObj?.runs,
            fallbackWkts = scoreObj?.wickets,
            fallbackOvers = scoreObj?.overs
        )

        // Preserve stationary slots for batsmen
        val striker = rich?.striker?.copy(strike = true)
        val nonStriker = rich?.nonStriker?.copy(strike = false)

        updateAnchoredBatsmen(striker, nonStriker)

        // Parse recent balls
        val ballList = mutableListOf<String>()
        val recentBalls = rich?.recentBalls
        if (!recentBalls.isNullOrEmpty()) {
            recentBalls.takeLast(12).forEach { b ->
                b.ball?.let { ballList.add(it) }
            }
        } else if (!rich?.lastOvers.isNullOrEmpty()) {
            val lastOver = rich.lastOvers.lastOrNull()
            lastOver?.balls?.let { ballList.addAll(it) }
        }

        // Clean equation: don't show bare numbers without context
        val eqRaw = rich?.equation?.trim().orEmpty()
        val eqClean = when {
            eqRaw.matches(Regex("""^[\d.]+$""")) -> null
            eqRaw.isNotBlank() -> eqRaw
            !rich?.comment.isNullOrBlank() && !rich.comment.matches(Regex("""^[\d.]+$""")) -> rich.comment
            else -> null
        }

        val lastOversList = rich?.lastOvers?.takeLast(4) ?: emptyList()

        return CricketScoreUiData(
            matchId = detail.id,
            seriesName = detail.series ?: rich?.series ?: "",
            matchDesc = detail.matchDesc ?: rich?.matchDesc ?: "",
            status = detail.status ?: "live",
            statusText = detail.statusText ?: detail.result ?: "",
            battingTeamName = battingName,
            battingTeamShort = battingShort,
            scoreMain = scoreMain,
            oversFormatted = oversFormatted,
            runs = scoreObj?.runs,
            wickets = scoreObj?.wickets,
            overs = scoreObj?.overs,
            crr = rich?.crr,
            rrr = rich?.rrr,
            target = rich?.target,
            equation = eqClean,
            slot1Batsman = anchoredSlot1,
            slot2Batsman = anchoredSlot2,
            bowler = rich?.bowler,
            recentBalls = ballList,
            lastOvers = lastOversList,
            team1Squad = squadTeams?.team1,
            team2Squad = squadTeams?.team2,
            commentaryBalls = commentaryBalls,
            isDelayedByStreamSync = isDelayed
        )
    }

    private fun parseScoreParts(
        raw: String?,
        fallbackRuns: Int?,
        fallbackWkts: Int?,
        fallbackOvers: Float?
    ): Pair<String, String?> {
        if (raw.isNullOrBlank()) {
            val s = if (fallbackRuns != null) "$fallbackRuns/${fallbackWkts ?: 0}" else "0/0"
            val ov = fallbackOvers?.let { "$it ov" }
            return Pair(s, ov)
        }

        val s = raw.trim()
        val andIdx = s.indexOf(" & ")
        if (andIdx != -1) {
            val firstRaw = s.substring(0, andIdx).replace(Regex("""\([^)]*"""), "").trim()
            val secondPart = s.substring(andIdx + 3).trim()
            val (secondMain, secondOvers) = extractSingleScore(secondPart, fallbackOvers)
            return Pair("$firstRaw & $secondMain", secondOvers)
        }

        return extractSingleScore(s, fallbackOvers)
    }

    private fun extractSingleScore(s: String, fallbackOvers: Float?): Pair<String, String?> {
        val regex = Regex("""^(\d+(?:\s*[/-]\s*\d+)?)\s*(?:\(([\d.]+)\)?)?""")
        val match = regex.find(s)
        if (match != null) {
            val main = match.groupValues[1].replace(Regex("""\s+"""), "")
            val oversStr = match.groupValues.getOrNull(2)?.ifBlank { null } ?: fallbackOvers?.toString()
            val ovFormatted = oversStr?.let { "$it ov" }
            return Pair(main, ovFormatted)
        }
        val cleaned = s.replace("(", "").replace(")", "").trim()
        val ovFallback = fallbackOvers?.let { "$it ov" }
        return Pair(cleaned, ovFallback)
    }

    private fun updateAnchoredBatsmen(striker: CrexPlayerStats?, nonStriker: CrexPlayerStats?) {
        if (striker == null && nonStriker == null) {
            anchoredSlot1 = null
            anchoredSlot2 = null
            return
        }

        fun isMatch(a: CrexPlayerStats?, b: CrexPlayerStats?): Boolean {
            if (a == null || b == null) return false
            if (!a.fkey.isNullOrBlank() && !b.fkey.isNullOrBlank()) return a.fkey == b.fkey
            val nA = a.name?.trim()?.lowercase().orEmpty()
            val nB = b.name?.trim()?.lowercase().orEmpty()
            return nA.isNotBlank() && nA == nB
        }

        val s = striker ?: nonStriker!!
        val ns = if (striker != null) nonStriker else null

        if (anchoredSlot1 == null && anchoredSlot2 == null) {
            anchoredSlot1 = s
            anchoredSlot2 = ns
            return
        }

        if (isMatch(anchoredSlot1, s)) {
            anchoredSlot1 = s.copy(strike = true)
            anchoredSlot2 = ns?.copy(strike = false) ?: anchoredSlot2?.copy(strike = false)
        } else if (isMatch(anchoredSlot2, s)) {
            anchoredSlot2 = s.copy(strike = true)
            anchoredSlot1 = ns?.copy(strike = false) ?: anchoredSlot1?.copy(strike = false)
        } else if (ns != null && isMatch(anchoredSlot1, ns)) {
            anchoredSlot1 = ns.copy(strike = false)
            anchoredSlot2 = s.copy(strike = true)
        } else if (ns != null && isMatch(anchoredSlot2, ns)) {
            anchoredSlot2 = ns.copy(strike = false)
            anchoredSlot1 = s.copy(strike = true)
        } else {
            anchoredSlot1 = s.copy(strike = true)
            anchoredSlot2 = ns?.copy(strike = false)
        }
    }

    private fun cleanTeamName(name: String): String {
        return name
            .replace(Regex("(?i)\\b(vs|v|live|stream|t20|odi|test|match)\\b"), "")
            .trim()
    }
}
