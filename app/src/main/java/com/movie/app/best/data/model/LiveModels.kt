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
    val source: ChannelSource = ChannelSource.TV_STREAM
)

fun LiveChannel.toUnified(): UnifiedChannel = UnifiedChannel(
    id = id.toString(),
    name = name,
    logoUrl = logoUrl,
    category = category,
    streamUrl = streamUrl,
    quality = "",
    source = ChannelSource.BROADCAST
)

fun TvStreamChannel.toUnified(): UnifiedChannel = UnifiedChannel(
    id = id.ifEmpty { name },
    name = name,
    logoUrl = logo,
    category = category,
    streamUrl = streamUrl,
    quality = quality,
    source = ChannelSource.TV_STREAM
)
