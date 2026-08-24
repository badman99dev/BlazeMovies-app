package com.movie.app.best.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentModeration(
    val poster: String = "safe",
    val screenshots: String = "safe",
    val storyline: String = "none"
) : Parcelable {
    val isPosterSexual get() = poster == "sexual"
    val isScreenshotsSexual get() = screenshots == "sexual"
    val isStorylineSexual get() = storyline == "sexual"
    val hasAnyFlag get() = isPosterSexual || isScreenshotsSexual || isStorylineSexual

    fun toModerationMap(): Map<String, String> = mapOf(
        "poster" to poster,
        "screenshots" to screenshots,
        "storyline" to storyline
    )
}

fun Map<String, String>?.toContentModeration(): ContentModeration? = this?.let {
    ContentModeration(
        poster = get("poster") ?: "safe",
        screenshots = get("screenshots") ?: "safe",
        storyline = get("storyline") ?: "none"
    )
}

data class ContentModerationResponse(
    val poster: String = "safe",
    val screenshots: String = "safe",
    val storyline: String = "none",
    val confidence: String = "low",
    val reasoning: String = "",
    val model: String = ""
)

@Parcelize
data class Movie(
    val id: Int,
    val slug: String,
    val title: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("release_year") val releaseYear: String,
    val rating: String,
    @SerializedName("audio_label") val audioLabel: String,
    @SerializedName("is_series") val isSeries: Boolean,
    @SerializedName("has_stream") val hasStream: Boolean,
    val views: Int,
    @SerializedName("_rank") val rank: Int,
    @SerializedName("content_moderation") val contentModeration: ContentModeration? = null,
    @SerializedName("poster_moderation") val posterModeration: String? = null,
    @SerializedName("stream_avl") val streamAvl: Boolean? = null
) : Parcelable {
    val shouldBlurPoster: Boolean
        get() = contentModeration?.isPosterSexual == true || posterModeration == "sexual"
}

@Parcelize
data class MovieDetails(
    val id: Int,
    val slug: String,
    val title: String,
    @SerializedName("original_title") val originalTitle: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("backdrop_url") val backdropUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("release_year") val releaseYear: String,
    val rating: String,
    val runtime: String,
    val director: String,
    val cast: String,
    @SerializedName("audio_label") val audioLabel: String,
    val language: String,
    val description: String,
    val country: String,
    @SerializedName("youtube_id") val youtubeId: String,
    @SerializedName("imdb_id") val imdbId: String,
    val views: Int,
    @SerializedName("is_series") val isSeries: Boolean,
    @SerializedName("has_stream") val hasStream: Boolean,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("player_url") val playerUrl: String,
    @SerializedName("content_moderation") val contentModeration: ContentModeration? = null,
    val status: String = "",
    @SerializedName("season_label") val seasonLabel: String = "",
    @SerializedName("total_episodes") val totalEpisodes: Int = 0,
    @SerializedName("series_group_id") val seriesGroupId: Int = 0
) : Parcelable

@Parcelize
data class Category(
    val id: Int,
    @SerializedName("category_name") val categoryName: String,
    val slug: String,
    @SerializedName("banner_url") val bannerUrl: String,
    val count: Int
) : Parcelable

@Parcelize
data class DownloadLink(
    val id: Int,
    val label: String,
    @SerializedName("link_url") val linkUrl: String,
    val type: String,
    @SerializedName("file_size") val fileSize: String,
    @SerializedName("episode_id") val episodeId: Int?
) : Parcelable

@Parcelize
data class Comment(
    val id: Int,
    @SerializedName("user_name") val userName: String,
    val comment: String,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

@Parcelize
data class Episode(
    val id: Int,
    @SerializedName("season_no") val seasonNo: Int,
    @SerializedName("episode_no") val episodeNo: Int,
    val title: String,
    val overview: String,
    @SerializedName("still_path") val stillPath: String,
    @SerializedName("air_date") val airDate: String,
    @SerializedName("vote_average") val voteAverage: Double
) : Parcelable

@Parcelize
data class Season(
    val id: Int,
    val title: String,
    val slug: String = "",
    @SerializedName("season_label") val seasonLabel: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("total_episodes") val totalEpisodes: Int
) : Parcelable

@Parcelize
data class AppNotification(
    val id: Int,
    val type: String,
    val content: String,
    @SerializedName("btn_text") val btnText: String,
    @SerializedName("btn_link") val btnLink: String,
    @SerializedName("is_active") val isActive: Boolean
) : Parcelable

data class ApiResponse<T>(
    val status: String,
    val data: T?,
    val message: String?
)

data class SearchResult(
    val query: String,
    val results: List<Movie>,
    val total: Int,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class OffsetResult(
    val items: List<Movie>,
    val total: Int,
    val offset: Int,
    val limit: Int
)

data class NewReleaseResult(
    val items: List<Movie> = emptyList(),
    val page: Int = 1,
    val max: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class CelebIntro(
    @SerializedName("name_id") val nameId: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("photo_url") val photoUrl: String = "",
    @SerializedName("photo_width") val photoWidth: Int = 0,
    @SerializedName("photo_height") val photoHeight: Int = 0,
    val professions: List<String> = emptyList(),
    @SerializedName("birth_date") val birthDate: String = "",
    val biography: String = ""
)

data class CelebsResult(
    val name: CelebIntro = CelebIntro(),
    val items: List<Movie> = emptyList(),
    val page: Int = 1,
    val max: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class CategoryOffsetResult(
    val category: Category,
    val items: List<Movie>,
    val total: Int,
    val offset: Int,
    val limit: Int
)

data class SliderResult(
    val mode: String,
    val limit: Int,
    val movies: List<Movie>
)

data class ContentDetailResponse(
    @SerializedName("content_type") val contentType: String,
    val movie: MovieDetails,
    val genres: List<String>,
    @SerializedName("download_links") val downloadLinks: List<DownloadLink>,
    val comments: List<Comment>,
    val screenshots: List<String>,
    @SerializedName("episodes_by_season") val episodesBySeason: Map<String, List<Episode>> = emptyMap(),
    @SerializedName("links_by_episode") val linksByEpisode: Map<String, List<DownloadLink>> = emptyMap(),
    @SerializedName("links_no_episode") val linksNoEpisode: List<DownloadLink> = emptyList(),
    @SerializedName("more_seasons") val moreSeasons: List<Season> = emptyList()
)

@Parcelize
data class CategorySimple(
    val id: String,
    @SerializedName("category_name") val categoryName: String,
    val slug: String
) : Parcelable

