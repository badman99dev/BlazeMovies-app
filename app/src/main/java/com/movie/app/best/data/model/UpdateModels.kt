package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class UpdateResponse(
    @SerializedName("update_available") val updateAvailable: Boolean = false,
    @SerializedName("force_update") val forceUpdate: Boolean = false,
    @SerializedName("force_update_message") val forceUpdateMessage: String = "",
    val version: String = "",
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("download_url") val downloadUrl: String = "",
    @SerializedName("download_size_mb") val downloadSizeMb: Double = 0.0,
    val abi: String = "",
    @SerializedName("whats_new") val whatsNew: String = "",
    val sha: String = "",
    @SerializedName("updated_on") val updatedOn: String = "",
    val maintenance: Boolean = false,
    @SerializedName("maintenance_message") val maintenanceMessage: String = ""
)
