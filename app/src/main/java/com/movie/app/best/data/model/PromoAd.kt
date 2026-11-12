package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

/** Payload of GET /v1/promo. `null` data means "no ad for this device" → the app clears its cache. */
data class PromoAdResponse(
    val status: String? = null,
    val data: PromoAd? = null,
    val message: String? = null
)

data class PromoAd(
    @SerializedName("dialog_id") val dialogId: String = "",
    @SerializedName("render_mode") val renderMode: String = "image",
    @SerializedName("display_after") val displayAfter: Long = 0L,
    @SerializedName("display_before") val displayBefore: Long = 0L,
    val template: PromoTemplate = PromoTemplate(),
    val content: PromoContent = PromoContent(),
    val image: PromoImage? = null,
    @SerializedName("image_action") val imageAction: PromoAction = PromoAction(),
    val button: PromoButton = PromoButton(),
    @SerializedName("server_time") val serverTime: Long = 0L
)

data class PromoTemplate(
    val dialog: String = "image_alpha",
    val button: String = "custom_png",
    val accent: String = ""
)

data class PromoContent(
    val title: String = "",
    val subtitle: String = "",
    @SerializedName("button_label") val buttonLabel: String = ""
)

data class PromoImage(
    val url: String = "",
    /** "image" (png/webp/gif) or "lottie" (.json / .lottie animation). */
    val type: String = "image",
    val width: Int = 0,
    val height: Int = 0
) {

    val isLottie: Boolean get() = type.equals("lottie", ignoreCase = true)
}

data class PromoAction(
    val type: String = "none",
    val value: String = ""
) {
    val isNone: Boolean get() = type == "none" || value.isBlank()
    val isUrl: Boolean get() = type == "url"
    val isDeeplink: Boolean get() = type == "deeplink"
}

data class PromoButton(
    val enabled: Boolean = false,
    val image: PromoImage? = null,
    val action: PromoAction = PromoAction(),
    val position: PromoButtonPosition = PromoButtonPosition()
)

data class PromoButtonPosition(
    val align: String = "bottom-center",
    @SerializedName("offset_x") val offsetX: Int = 0,
    @SerializedName("offset_y") val offsetY: Int = 28
)
