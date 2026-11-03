package com.movie.app.best.data.update

import android.os.Build
import android.os.Process

object DeviceAbi {

    val primary: String by lazy { normalize(Build.SUPPORTED_ABIS.firstOrNull()) }

    val list: String by lazy {
        Build.SUPPORTED_ABIS
            .map { normalize(it) }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
    }

    val bits: Int by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Process.is64Bit()) 64 else 32
    }

    val sdk: Int get() = Build.VERSION.SDK_INT

    val device: String by lazy {
        val raw = "${Build.MANUFACTURER}/${Build.MODEL}"
        raw.replace(Regex("[^A-Za-z0-9._/ -]"), "").trim().take(48)
    }

    private fun normalize(raw: String?): String = when (raw?.lowercase()) {
        "arm64-v8a" -> "arm64-v8a"
        "armeabi-v7a", "armeabi" -> "armeabi-v7a"
        "x86_64" -> "x86_64"
        "x86" -> "x86"
        else -> ""
    }
}