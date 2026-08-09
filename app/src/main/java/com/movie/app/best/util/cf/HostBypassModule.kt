package com.movie.app.best.util.cf

import android.app.Activity

interface HostBypassModule {
    val tag: String
    fun matches(host: String): Boolean
    suspend fun scrape(
        activity: Activity,
        url: String,
        onLog: (String) -> Unit
    ): ModuleResult?
}

data class ModuleResult(
    val directUrl: String,
    val fileName: String? = null,
    val cookies: String? = null
)
