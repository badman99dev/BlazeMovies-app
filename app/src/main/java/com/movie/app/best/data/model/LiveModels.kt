package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class LiveChannel(
    val id: Int,
    val name: String,
    @SerializedName("logoUrl") val logoUrl: String,
    val category: String,
    @SerializedName("streamUrl") val streamUrl: String,
)

data class BroadcastResponse(
    val channels: List<LiveChannel>
)

// TVStream API response (iptv-india repo)
data class TvStreamChannel(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val category: String = "",
    val quality: String = "",
    val country: String = "",
    @SerializedName("stream_url") val streamUrl: String = "",
    @SerializedName("stream_urls") val streamUrls: List<String> = emptyList(),
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

// ── Unified channel model (normalizes BOTH broadcast + tv-stream sources) ──
enum class ChannelSource { BROADCAST, TV_STREAM }

data class UnifiedChannel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val category: String,
    val streamUrl: String,
    val quality: String = "",
    val source: ChannelSource = ChannelSource.TV_STREAM,
    val uniqueKey: String = ""
)

// Broadcast — ids already globally unique Int (1..44), not paginated.
// indexInBatch provides extra armor so even same-channel + same-url (impossible per backend)
// would produce a different key per list slot.
fun LiveChannel.toUnified(indexInBatch: Int = 0): UnifiedChannel {
    val streamHash = streamUrl.hashCode().toUInt().toString(16).padStart(8, '0')
    return UnifiedChannel(
        id = id.toString(),
        name = name,
        logoUrl = logoUrl,
        category = category,
        streamUrl = streamUrl,
        quality = "",
        source = ChannelSource.BROADCAST,
        uniqueKey = "bc_${id}__${streamHash}_idx$indexInBatch"
    )
}

// IPTV channel — ids can repeat (same channel, multiple stream URLs).
// batchOffset is the pagination offset; (indexInBatch + batchOffset) yields a globally unique
// position across pages, guaranteeing no Compose key collisions even across paged loads.
fun TvStreamChannel.toUnified(indexInBatch: Int = 0, batchOffset: Int = 0): UnifiedChannel {
    val safeId = id.ifEmpty { name.ifEmpty { "_noname" } }
    val streamHash = streamUrl.hashCode().toUInt().toString(16).padStart(8, '0')
    return UnifiedChannel(
        id = safeId,
        name = name,
        logoUrl = logo,
        category = category,
        streamUrl = streamUrl,
        quality = quality,
        source = ChannelSource.TV_STREAM,
        uniqueKey = "uc_${safeId}__${streamHash}_idx${indexInBatch + batchOffset}"
    )
}
