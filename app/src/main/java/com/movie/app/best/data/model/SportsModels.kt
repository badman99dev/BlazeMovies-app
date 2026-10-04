package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SportCategory(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = ""
)

data class SportCategoriesResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("data") val data: List<SportCategory> = emptyList()
)

data class SportFormat(
    @SerializedName("title") val title: String? = null,
    @SerializedName("webLink") val webLink: String? = null
)

data class SportEventInfo(
    @SerializedName("teamA") val teamA: String? = null,
    @SerializedName("teamB") val teamB: String? = null,
    @SerializedName("teamAFlag") val teamAFlag: String? = null,
    @SerializedName("teamBFlag") val teamBFlag: String? = null,
    @SerializedName("eventCat") val eventCat: String? = null,
    @SerializedName("eventName") val eventName: String? = null,
    @SerializedName("eventLogo") val eventLogo: String? = null,
    @SerializedName("isHot") val isHot: Int = 0,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null
)

data class SportEvent(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String? = null,
    @SerializedName("slug") val slug: String = "",
    @SerializedName("cat") val cat: String? = null,
    @SerializedName("eventInfo") val eventInfo: SportEventInfo? = null,
    @SerializedName("publish") val publish: String? = null,
    @SerializedName("formats") val formats: List<SportFormat> = emptyList(),
    @SerializedName("streams") val streams: List<SportStream> = emptyList()
) {
    enum class MatchStatus {
        LIVE,
        UPCOMING,
        RECENT
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val formats = listOf(
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        )
        for (sdf in formats) {
            try {
                return sdf.parse(dateStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }

    val startDate: Date? get() = parseDate(eventInfo?.startTime)
    val endDate: Date? get() = parseDate(eventInfo?.endTime)

    val matchStatus: MatchStatus
        get() {
            val now = System.currentTimeMillis()
            val start = startDate?.time ?: 0L
            val end = endDate?.time ?: 0L

            return when {
                start > now -> MatchStatus.UPCOMING
                end > 0L && now > end -> MatchStatus.RECENT
                start in 1..now -> MatchStatus.LIVE
                else -> MatchStatus.LIVE
            }
        }

    val isLive: Boolean get() = matchStatus == MatchStatus.LIVE
    val isUpcoming: Boolean get() = matchStatus == MatchStatus.UPCOMING
    val isRecent: Boolean get() = matchStatus == MatchStatus.RECENT
    val isHot: Boolean get() = (eventInfo?.isHot ?: 0) == 1
    val isCricket: Boolean
        get() {
            val cat = (eventInfo?.eventCat ?: this.cat ?: "").lowercase()
            val name = (eventInfo?.eventName ?: this.title).lowercase()
            val teamA = (eventInfo?.teamA ?: "").lowercase()
            val teamB = (eventInfo?.teamB ?: "").lowercase()
            return cat.contains("cricket") || name.contains("cricket") || teamA.contains("cricket") || teamB.contains("cricket")
        }

    val formattedStartTime: String
        get() {
            val d = startDate ?: return ""
            val outputFormat = SimpleDateFormat("hh:mm a  dd/MM/yyyy", Locale.getDefault())
            return outputFormat.format(d)
        }

    val formattedTimeOnly: String
        get() {
            val d = startDate ?: return ""
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d)
        }

    val formattedDateOnly: String
        get() {
            val d = startDate ?: return ""
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
        }

    fun getNotchText(now: Long = System.currentTimeMillis()): String {
        val actualNow = if (now <= 0L) System.currentTimeMillis() else now
        return when {
            isLive -> {
                val start = startDate?.time ?: 0L
                val elapsed = actualNow - start
                if (elapsed <= 0) "00:00"
                else {
                    val hours = elapsed / (1000 * 60 * 60)
                    val minutes = (elapsed / (1000 * 60)) % 60
                    val seconds = (elapsed / 1000) % 60
                    if (hours > 0) {
                        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    }
                }
            }
            isUpcoming -> {
                val start = startDate?.time ?: return ""
                val diff = start - actualNow
                if (diff <= 0) return "Match Starting"

                val totalDays = diff / (1000L * 60 * 60 * 24)
                val totalWeeks = totalDays / 7L
                val totalHours = diff / (1000L * 60 * 60)
                val minutes = (diff / (1000L * 60)) % 60
                val seconds = (diff / 1000L) % 60

                when {
                    totalDays >= 7L -> if (totalWeeks == 1L) "Starts in 1 Week" else "Starts in $totalWeeks Weeks"
                    totalDays >= 1L -> if (totalDays == 1L) "Starts in 1 Day" else "Starts in $totalDays Days"
                    totalHours >= 2L -> if (totalHours == 1L) "Starts in 1 Hour" else "Starts in $totalHours Hours"
                    totalHours >= 1L -> String.format(Locale.getDefault(), "Match Starting in %d:%02d:%02d", totalHours, minutes, seconds)
                    minutes >= 1L -> String.format(Locale.getDefault(), "Match Starting in %02d:%02d", minutes, seconds)
                    seconds > 0L -> String.format(Locale.getDefault(), "Match Starting in %02ds", seconds)
                    else -> "Match Starting"
                }
            }
            else -> "Match Ended"
        }
    }

    val countdownText: String
        get() = getNotchText()

    val elapsedLiveText: String
        get() {
            val start = startDate?.time ?: return ""
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 0) return ""
            val hours = elapsed / (1000 * 60 * 60)
            val minutes = (elapsed / (1000 * 60)) % 60
            val seconds = (elapsed / 1000) % 60
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
}

data class SportEventsResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("data") val data: List<SportEvent> = emptyList()
)

data class SportDrm(
    @SerializedName("scheme") val scheme: String? = null,
    @SerializedName("key") val key: String? = null
)

data class SportStream(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("url") val url: String = "",
    @SerializedName("headers") val headers: Map<String, String>? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("drm") val drm: SportDrm? = null,
    @SerializedName("webLink") val webLink: String? = null
) {
    val isDash: Boolean get() = format.equals("dash", ignoreCase = true) || url.endsWith(".mpd", ignoreCase = true)
    val isHls: Boolean get() = format.equals("hls", ignoreCase = true) || url.contains(".m3u8", ignoreCase = true)
    val hasClearKey: Boolean get() = drm?.scheme?.equals("clearkey", ignoreCase = true) == true && !drm.key.isNullOrBlank()
}
