package com.movie.app.best.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.movie.app.best.data.update.DeviceAbi

/**
 * Device facts shared by /v1/register and /v1/promo, so both report the same identity.
 * All values are best-effort — an empty/zero value is fine for the backend.
 */
object DeviceFacts {

    /** Marketing SoC name on API 31+, otherwise the platform codename. */
    fun cpu(context: Context): String {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else ""
        return soc.ifBlank { Build.HARDWARE }.ifBlank { Build.BOARD }
    }

    fun ramMb(context: Context): Int = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        (info.totalMem / (1024 * 1024)).toInt()
    } catch (_: Exception) {
        0
    }

    fun cores(): Int = try {
        Runtime.getRuntime().availableProcessors()
    } catch (_: Exception) {
        0
    }

    fun abi(): String = DeviceAbi.primary

    fun sdk(): Int = DeviceAbi.sdk

    fun device(): String = DeviceAbi.device
}
