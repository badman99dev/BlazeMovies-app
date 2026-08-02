package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class MeiliHit(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    @SerializedName("poster_url") val posterUrl: String = "",
    @SerializedName("release_year") val releaseYear: Int = 0,
    val rating: Double = 0.0,
    @SerializedName("quality_label") val qualityLabel: String = "",
    @SerializedName("is_series") val isSeries: Int = 0,
    @SerializedName("stream_avl") val streamAvl: Boolean? = null,
    @SerializedName("audio_label") val audioLabel: String = "",
    @SerializedName("poster_moderation") val posterModeration: String? = null,
    @SerializedName("content_moderation") val contentModeration: Map<String, String>? = null,
    @SerializedName("_formatted") val formatted: Map<String, String>? = null
) {
    val isSeriesBool: Boolean get() = isSeries == 1
    val hasStream: Boolean get() = streamAvl ?: false
    val releaseYearStr: String get() = releaseYear.toString()
    val ratingStr: String get() = if (rating > 0) String.format("%.1f", rating) else ""
    val highlightedTitle: String get() = formatted?.get("title") ?: title

    fun toMovie(): Movie = Movie(
        id = id,
        slug = slug,
        title = title,
        posterUrl = posterUrl,
        qualityLabel = qualityLabel,
        releaseYear = releaseYearStr,
        rating = ratingStr,
        audioLabel = audioLabel,
        isSeries = isSeries == 1,
        hasStream = hasStream,
        views = 0,
        rank = 0,
        contentModeration = contentModeration?.toContentModeration(),
        posterModeration = posterModeration
    )
}
