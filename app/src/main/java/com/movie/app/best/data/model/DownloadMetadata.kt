package com.movie.app.best.data.model

data class DownloadMetadata(
    val slug: String,
    val title: String,
    val posterUrl: String = "",
    val localPosterPath: String = "",
    val fileName: String,
    val filePath: String,
    val ketchId: Int = -1,
    val isZip: Boolean = false,
    val extractPath: String? = null,
    val contentType: String = "movie",
    val episodeId: Int? = null,
    val episodeLabel: String? = null,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: String = "initializing",
    val extractionProgress: Int = 0,
    val url: String = "",
    val bypassAttempts: Int = 0
)

enum class DownloadPhase {
    NONE,
    INITIALIZING,
    DOWNLOADING,
    BYPASSING,
    EXTRACTING,
    COMPLETE,
    CANCELLED,
    FAILED
}
