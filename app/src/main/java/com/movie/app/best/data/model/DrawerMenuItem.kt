package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class DrawerMenuItem(
    val id: Int,
    val label: String,
    @SerializedName("icon_url") val iconUrl: String = "",
    @SerializedName("deep_link") val deepLink: String = "",
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("is_hot") val isHot: Boolean = false
)

data class DrawerMenuResponse(
    val items: List<DrawerMenuItem> = emptyList()
)