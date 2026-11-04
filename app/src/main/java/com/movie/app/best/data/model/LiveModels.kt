package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

// TVStream API response (v1 API) — list payload is slug-based and carries no stream URLs.
// The client asks for a channel's servers with /tv-stream/detail?slug=<slug>.
data class TvStreamChannel(
    val slug: String = "",
    val name: String = "",
    val logo: String = "",
)

data class TvStreamResponse(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val source: String = "",
    val channels: List<TvStreamChannel> = emptyList(),
)

data class TvStreamCategory(
    val name: String,
    val count: Int
)

data class TvStreamCategoriesResponse(
    val categories: List<TvStreamCategory> = emptyList(),
    val total: Int = 0
)

// ── Channel detail (all alternate servers for one slug) ──
data class LiveStreamSource(
    val id: Int = 0,
    val url: String = "",
    val label: String = "",
    val language: String = "",
    val host: String = "",
    val quality: String = "",
)

data class LiveChannelDetail(
    val slug: String = "",
    val name: String = "",
    val logo: String = "",
    val category: String = "",
    val country: String = "",
    val quality: String = "",
    @SerializedName("chat_rid") val chatRid: String? = null,
    @SerializedName("stream_count") val streamCount: Int = 0,
    @SerializedName("stream_url") val streamUrl: String = "",
    @SerializedName("stream_urls") val streamUrls: List<String> = emptyList(),
    val streams: List<LiveStreamSource> = emptyList(),
)

// ── Unified channel model used by Home / Live TV / Search lists ──
data class UnifiedChannel(
    val slug: String,
    val name: String,
    val logoUrl: String,
    val uniqueKey: String = ""
)

// IPTV channel — ids can repeat (same channel, multiple stream URLs).
// batchOffset is the pagination offset; (indexInBatch + batchOffset) yields a globally unique
// position across pages, guaranteeing no Compose key collisions even across paged loads.
fun TvStreamChannel.toUnified(indexInBatch: Int = 0, batchOffset: Int = 0): UnifiedChannel {
    val safeSlug = slug.ifEmpty { name.ifEmpty { "_noname" } }
    return UnifiedChannel(
        slug = safeSlug,
        name = name,
        logoUrl = logo,
        uniqueKey = "uc_${safeSlug}_idx${indexInBatch + batchOffset}"
    )
}
