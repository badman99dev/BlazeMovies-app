package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class FindMatchResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("matched") val matched: Boolean = false,
    @SerializedName("timeMatch") val timeMatch: Boolean = false,
    @SerializedName("timeMatchType") val timeMatchType: String? = null,
    @SerializedName("match") val match: CrexMatchBrief? = null,
    @SerializedName("candidates") val candidates: List<CrexMatchBrief>? = null,
    @SerializedName("reason") val reason: String? = null
)

data class CrexMatchBrief(
    @SerializedName("id") val id: String = "",
    @SerializedName("seriesName") val seriesName: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("timestamp") val timestamp: Long = 0L,
    @SerializedName("teams") val teams: CrexBriefTeams? = null
)

data class CrexBriefTeams(
    @SerializedName("team1") val team1: CrexBriefTeam? = null,
    @SerializedName("team2") val team2: CrexBriefTeam? = null
)

data class CrexBriefTeam(
    @SerializedName("name") val name: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("slug") val slug: String? = null
)

data class CrexMatchDetailResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("match") val match: CrexMatchDetail? = null
)

data class CrexMatchDetail(
    @SerializedName("id") val id: String = "",
    @SerializedName("series") val series: String? = null,
    @SerializedName("matchDesc") val matchDesc: String? = null,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("statusText") val statusText: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("timestamp") val timestamp: Long = 0L,
    @SerializedName("result") val result: String? = null,
    @SerializedName("winner") val winner: String? = null,
    @SerializedName("teams") val teams: CrexTeamsDetail? = null,
    @SerializedName("rich") val rich: CrexRichDetail? = null
)

data class CrexTeamsDetail(
    @SerializedName("team1") val team1: CrexTeamDetail? = null,
    @SerializedName("team2") val team2: CrexTeamDetail? = null
)

data class CrexTeamDetail(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("shortName") val shortName: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("jersey") val jersey: String? = null,
    @SerializedName("score") val score: CrexTeamScore? = null,
    @SerializedName("scoreRaw") val scoreRaw: String? = null
)

data class CrexTeamScore(
    @SerializedName("runs") val runs: Int? = null,
    @SerializedName("wickets") val wickets: Int? = null,
    @SerializedName("overs") val overs: Float? = null,
    @SerializedName("balls") val balls: Int? = null
)

data class CrexRichDetail(
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("series") val series: String? = null,
    @SerializedName("matchDesc") val matchDesc: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("day") val day: String? = null,
    @SerializedName("totalDays") val totalDays: String? = null,
    @SerializedName("inning") val inning: Int? = null,
    @SerializedName("target") val target: Int? = null,
    @SerializedName("crr") val crr: Double? = null,
    @SerializedName("rrr") val rrr: Double? = null,
    @SerializedName("equation") val equation: String? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("battingTeam") val battingTeam: String? = null,
    @SerializedName("battingTeamShort") val battingTeamShort: String? = null,
    @SerializedName("battingTeamCode") val battingTeamCode: String? = null,
    @SerializedName("striker") val striker: CrexPlayerStats? = null,
    @SerializedName("nonStriker") val nonStriker: CrexPlayerStats? = null,
    @SerializedName("bowler") val bowler: CrexBowlerStats? = null,
    @SerializedName("lastOvers") val lastOvers: List<CrexLastOver>? = null,
    @SerializedName("recentBalls") val recentBalls: List<CrexRecentBall>? = null
)

data class CrexPlayerStats(
    @SerializedName("fkey") val fkey: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("shortName") val shortName: String? = null,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("balls") val balls: Int = 0,
    @SerializedName("strikeRate") val strikeRate: Double = 0.0,
    @SerializedName("fours") val fours: Int = 0,
    @SerializedName("sixes") val sixes: Int = 0,
    @SerializedName("strike") val strike: Boolean = false,
    @SerializedName("head") val head: String? = null,
    @SerializedName("jersey") val jersey: String? = null
)

data class CrexBowlerStats(
    @SerializedName("fkey") val fkey: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("shortName") val shortName: String? = null,
    @SerializedName("overs") val overs: String? = null,
    @SerializedName("maidens") val maidens: Int = 0,
    @SerializedName("runs") val runs: Int = 0,
    @SerializedName("wickets") val wickets: Int? = null,
    @SerializedName("economy") val economy: Double = 0.0,
    @SerializedName("head") val head: String? = null,
    @SerializedName("jersey") val jersey: String? = null
)

data class CrexLastOver(
    @SerializedName("over") val over: String? = null,
    @SerializedName("balls") val balls: List<String> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

data class CrexRecentBall(
    @SerializedName("over") val over: String? = null,
    @SerializedName("ball") val ball: String? = null
)
