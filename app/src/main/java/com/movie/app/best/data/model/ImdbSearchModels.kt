package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class ImdbSearchResponse(
    val titles: List<ImdbSearchTitle> = emptyList()
)

data class ImdbSearchTitle(
    val id: String = "",
    val type: String = "",
    @SerializedName("primaryTitle") val primaryTitle: String = "",
    @SerializedName("originalTitle") val originalTitle: String? = null,
    @SerializedName("primaryImage") val primaryImage: ImdbImage? = null,
    @SerializedName("startYear") val startYear: Int = 0,
    @SerializedName("endYear") val endYear: Int? = null,
    @SerializedName("runtimeSeconds") val runtimeSeconds: Int = 0
) {
    val posterUrl: String get() = primaryImage?.url ?: ""
    val displayTitle: String get() = primaryTitle.ifBlank { originalTitle ?: "" }
    val displayYear: String get() = if (startYear > 0) startYear.toString() else ""
    val isSeries: Boolean get() = type.startsWith("tv") && type != "tvMovie"
}