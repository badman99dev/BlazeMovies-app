package com.movie.app.best.data.remote

import com.google.gson.annotations.SerializedName

data class BypassDoneEvent(
    val results: List<BypassResult> = emptyList(),
    @SerializedName("total_found") val totalFound: Int = 0,
    @SerializedName("success_count") val successCount: Int = 0
)

data class BypassResult(
    val jackpot: String?,
    val status: String,
    @SerializedName("root_source") val rootSource: String?,
    @SerializedName("original_source") val originalSource: String?,
    @SerializedName("wrapper_resolved") val wrapperResolved: String?,
    @SerializedName("file_info") val fileInfo: BypassFileInfo?
)

data class BypassFileInfo(
    val filename: String?,
    val size: String?,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    val resumable: Boolean?,
    @SerializedName("content_type") val contentType: String?
)
