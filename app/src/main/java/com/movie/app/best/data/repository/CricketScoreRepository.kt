package com.movie.app.best.data.repository

import com.google.gson.GsonBuilder
import com.movie.app.best.data.model.CrexBowlerStats
import com.movie.app.best.data.model.CrexMatchDetail
import com.movie.app.best.data.model.CrexPlayerStats
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
    val scoreRaw: String = "",
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

    // Circular snapshot buffer for 30s stream sync
    private val snapshotBuffer = Collections.synchronizedList(mutableListOf<Pair<Long, CrexMatchDetail>>())

    // Stationary batsman tracking
    private var anchoredSlot1: CrexPlayerStats? = null
    private var anchoredSlot2: CrexPlayerStats? = null

    fun resetBuffer() {
        snapshotBuffer.clear()
        anchoredSlot1 = null
        anchoredSlot2 = null
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

    suspend fun fetchMatchDetail(matchId: String, syncWithStream: Boolean): CricketScoreUiData? = withContext(Dispatchers.IO) {
        try {
            val response = api.getMatchDetail(id = matchId, json = true)
            val match = response.match ?: return@withContext null

            val now = System.currentTimeMillis()
            snapshotBuffer.add(Pair(now, match))

            // Prune snapshots older than 2 minutes
            val cutOff = now - 120_000L
            snapshotBuffer.removeAll { it.first < cutOff }

            // Select snapshot based on syncWithStream toggle
            val selectedDetail: CrexMatchDetail
            val isDelayed: Boolean

            if (syncWithStream) {
                val targetTime = now - 30_000L
                val olderSnapshots = snapshotBuffer.filter { it.first <= targetTime }
                selectedDetail = if (olderSnapshots.isNotEmpty()) {
                    // Pick the snapshot closest to targetTime
                    olderSnapshots.maxByOrNull { it.first }?.second ?: match
                } else {
                    // Buffer is still filling up; use the earliest available snapshot
                    snapshotBuffer.firstOrNull()?.second ?: match
                }
                isDelayed = true
            } else {
                selectedDetail = match
                isDelayed = false
            }

            mapToUiData(selectedDetail, isDelayed)
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToUiData(detail: CrexMatchDetail, isDelayed: Boolean): CricketScoreUiData {
        val rich = detail.rich
        val teams = detail.teams

        // Identify batting team
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
        val scoreRaw = battingTeam?.scoreRaw ?: if (scoreObj?.runs != null) "${scoreObj.runs}/${scoreObj.wickets ?: 0}" else ""

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

        return CricketScoreUiData(
            matchId = detail.id,
            seriesName = detail.series ?: rich?.series ?: "",
            matchDesc = detail.matchDesc ?: rich?.matchDesc ?: "",
            status = detail.status ?: "live",
            statusText = detail.statusText ?: detail.result ?: "",
            battingTeamName = battingName,
            battingTeamShort = battingShort,
            scoreRaw = scoreRaw,
            runs = scoreObj?.runs,
            wickets = scoreObj?.wickets,
            overs = scoreObj?.overs,
            crr = rich?.crr,
            rrr = rich?.rrr,
            target = rich?.target,
            equation = rich?.equation ?: rich?.comment,
            slot1Batsman = anchoredSlot1,
            slot2Batsman = anchoredSlot2,
            bowler = rich?.bowler,
            recentBalls = ballList,
            isDelayedByStreamSync = isDelayed
        )
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
            // New pair of batsmen
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
