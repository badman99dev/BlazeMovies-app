package com.movie.app.best.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build

/**
 * Device / CPU / decoder fingerprint attached to every incident.
 *
 * There is no public "which decoder is currently in use" API — so we ship the
 * two things that *are* available:
 *   • [CodecTracker] — the decoder ExoPlayer actually picked at runtime (see there).
 *   • [decoders] — the platform's full hardware/software decoder list per MIME,
 *     whose names expose the SoC vendor (qcom / exynos / mtk / …), so the backend
 *     can reason about the decoder and the silicon.
 */
object DeviceProfile {

    @Volatile
    private var cached: Map<String, Any?>? = null

    fun build(context: Context): Map<String, Any?> {
        cached?.let { return it }
        val map = LinkedHashMap<String, Any?>()
        map["manufacturer"] = Build.MANUFACTURER
        map["brand"] = Build.BRAND
        map["model"] = Build.MODEL
        map["device"] = Build.DEVICE
        map["product"] = Build.PRODUCT
        map["board"] = Build.BOARD
        map["hardware"] = Build.HARDWARE
        map["sdk"] = Build.VERSION.SDK_INT
        map["android"] = Build.VERSION.RELEASE
        map["abis"] = Build.SUPPORTED_ABIS.toList()
        map["cpu_cores"] = Runtime.getRuntime().availableProcessors()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            map["soc_model"] = Build.SOC_MODEL
            map["soc_manufacturer"] = Build.SOC_MANUFACTURER
        }
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            map["ram_total_mb"] = mi.totalMem / (1024 * 1024)
            map["low_ram"] = am.isLowRamDevice
        }
        map["decoders"] = videoDecoders()
        cached = map
        return map
    }

    private fun videoDecoders(): List<Map<String, Any?>> {
        val out = ArrayList<Map<String, Any?>>()
        runCatching {
            val mimes = listOf(
                "video/avc", "video/hevc", "video/vp9", "video/av01", "video/mp4v-es", "video/x-vnd.on2.vp8"
            )
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in list.codecInfos) {
                if (info.isEncoder) continue
                val mime = info.supportedTypes.firstOrNull { it in mimes } ?: continue
                out.add(
                    linkedMapOf(
                        "name" to info.name,
                        "mime" to mime,
                        "hw" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isHardwareAccelerated else null,
                        "sw" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isSoftwareOnly else null,
                        "vendor" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isVendor else null
                    )
                )
            }
        }
        return out.take(40)
    }
}
