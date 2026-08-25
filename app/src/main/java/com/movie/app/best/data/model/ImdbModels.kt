package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class ImdbEpisodeResponse(
    val episodes: List<ImdbEpisode> = emptyList()
)

data class ImdbEpisode(
    val id: String = "",
    val title: String = "",
    @SerializedName("primaryImage") val primaryImage: ImdbImage? = null,
    val season: String = "",
    @SerializedName("episodeNumber") val episodeNumber: Int = 0,
    @SerializedName("runtimeSeconds") val runtimeSeconds: Int = 0,
    val plot: String = "",
    val rating: ImdbRating? = null,
    @SerializedName("releaseDate") val releaseDate: ImdbReleaseDate? = null
) {
    val stillImageUrl: String get() = primaryImage?.url ?: ""
    val runtimeMinutes: Int get() = runtimeSeconds / 60
    val formattedDate: String get() {
        val d = releaseDate ?: return ""
        val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val m = if (d.month in 1..12) months[d.month] else "${d.month}"
        return "${d.day} $m ${d.year}"
    }
    val ratingValue: Double get() = rating?.aggregateRating ?: 0.0
}

data class ImdbImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val type: String? = null
)

data class ImdbRating(
    @SerializedName("aggregateRating") val aggregateRating: Double = 0.0,
    @SerializedName("voteCount") val voteCount: Int = 0
)

data class ImdbReleaseDate(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0
)

data class ImdbMetacritic(
    val score: Int = 0,
    @SerializedName("reviewCount") val reviewCount: Int = 0
)

data class ImdbName(
    val id: String = "",
    @SerializedName("displayName") val displayName: String = "",
    @SerializedName("primaryImage") val primaryImage: ImdbImage? = null,
    @SerializedName("primaryProfessions") val primaryProfessions: List<String> = emptyList()
) {
    val photoUrl: String get() = primaryImage?.url ?: ""
}

data class ImdbNameDetails(
    val id: String = "",
    @SerializedName("displayName") val displayName: String = "",
    @SerializedName("primaryImage") val primaryImage: ImdbImage? = null,
    @SerializedName("primaryProfessions") val primaryProfessions: List<String> = emptyList(),
    @SerializedName("birthDate") val birthDate: ImdbReleaseDate? = null,
    val biography: String = "",
    @SerializedName("heightCm") val heightCm: Int = 0,
    @SerializedName("birthName") val birthName: String = "",
    @SerializedName("birthLocation") val birthLocation: String = ""
) {
    val photoUrl: String get() = primaryImage?.url ?: ""
    val photoWidth: Int get() = primaryImage?.width ?: 0
    val photoHeight: Int get() = primaryImage?.height ?: 0
    val birthDateText: String get() {
        val d = birthDate ?: return ""
        return if (d.year > 0) "%04d-%02d-%02d".format(d.year, d.month, d.day) else ""
    }
    val age: Int get() {
        val d = birthDate ?: return 0
        if (d.year <= 0) return 0
        val now = java.util.Calendar.getInstance()
        var a = now.get(java.util.Calendar.YEAR) - d.year
        val curMonth = now.get(java.util.Calendar.MONTH) + 1
        val curDay = now.get(java.util.Calendar.DAY_OF_MONTH)
        if (curMonth < d.month || (curMonth == d.month && curDay < d.day)) a--
        return a
    }
    val heightText: String get() = if (heightCm > 0) "${heightCm} cm" else ""
}

data class ImdbRelationshipsResponse(
    val relationships: List<ImdbRelationship> = emptyList()
)

data class ImdbRelationship(
    val name: ImdbName = ImdbName(),
    @SerializedName("relationType") val relationType: String = ""
)

data class ImdbCountry(
    val code: String = "",
    val name: String = ""
)

data class ImdbLanguage(
    val code: String = "",
    val name: String = ""
)

data class ImdbInterest(
    val id: String = "",
    val name: String = "",
    @SerializedName("isSubgenre") val isSubgenre: Boolean? = null
)

data class ImdbTitleDetails(
    val id: String = "",
    val type: String = "",
    @SerializedName("primaryTitle") val primaryTitle: String = "",
    @SerializedName("originalTitle") val originalTitle: String? = null,
    @SerializedName("primaryImage") val primaryImage: ImdbImage? = null,
    @SerializedName("startYear") val startYear: Int = 0,
    @SerializedName("endYear") val endYear: Int? = null,
    @SerializedName("runtimeSeconds") val runtimeSeconds: Int = 0,
    val genres: List<String> = emptyList(),
    val interests: List<ImdbInterest> = emptyList(),
    val rating: ImdbRating? = null,
    val metacritic: ImdbMetacritic? = null,
    val plot: String = "",
    val directors: List<ImdbName> = emptyList(),
    val writers: List<ImdbName> = emptyList(),
    val stars: List<ImdbName> = emptyList(),
    @SerializedName("originCountries") val originCountries: List<ImdbCountry> = emptyList(),
    @SerializedName("spokenLanguages") val spokenLanguages: List<ImdbLanguage> = emptyList()
) {
    val posterUrl: String get() = primaryImage?.url ?: ""
    val runtimeMinutes: Int get() = runtimeSeconds / 60
    val ratingValue: Double get() = rating?.aggregateRating ?: 0.0
    val voteCount: Int get() = rating?.voteCount ?: 0
    val metacriticScore: Int get() = metacritic?.score ?: 0
    val countriesText: String get() = originCountries.joinToString(", ") { it.name }
    val languagesText: String get() = spokenLanguages.joinToString(", ") { it.name }
    val directorsText: String get() = directors.joinToString(", ") { it.displayName }
    val writersText: String get() = writers.joinToString(", ") { it.displayName }
}

data class ImdbCertificatesResponse(
    val certificates: List<ImdbCertificate> = emptyList()
)

data class ImdbCertificate(
    val rating: String = "",
    val country: ImdbCertificateCountry? = null,
    val attributes: List<String>? = null
)

data class ImdbCertificateCountry(
    val code: String = "",
    val name: String = ""
)

data class ImdbCreditsResponse(
    val credits: List<ImdbCredit> = emptyList()
)

data class ImdbCredit(
    val name: ImdbName = ImdbName(),
    val category: String = "",
    val characters: List<String> = emptyList()
) {
    val photoUrl: String get() = name.photoUrl
    val displayName: String get() = name.displayName
    val characterText: String get() = characters.joinToString(", ")
    val isActor: Boolean get() = category == "actor" || category == "actress"
}
