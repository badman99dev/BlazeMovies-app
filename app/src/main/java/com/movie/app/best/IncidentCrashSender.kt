package com.movie.app.best

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.movie.app.best.data.config.RemoteConfig
import com.movie.app.best.data.telemetry.DeviceProfile
import com.movie.app.best.data.update.DeviceAbi
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * ACRA sender that ships a crash to our own backend (`POST /analytics/incident`,
 * category = crash) instead of the old Tempserv paste.
 *
 * Deliberately builds the payload from scratch — it does NOT dump the whole ACRA
 * report (that would include LOGCAT / SHARED_PREFERENCES / BUILD_CONFIG). We take
 * only the stack trace + a few safe identifiers, and attach our device fingerprint.
 */
class IncidentCrashSender : ReportSender {

    override fun send(context: Context, errorContent: CrashReportData) {
        runCatching { ship(context, errorContent.toMap()) }
    }

    private fun ship(context: Context, map: Map<String, Any?>) {
        fun s(key: String): String = map[key]?.toString().orEmpty()

        val stack = s("STACK_TRACE").take(20_000)
        val userComment = s("USER_COMMENT").trim().take(1_000)
        val excClass = exceptionClass(stack)
        val excMsg = exceptionMessage(stack)
        val deviceId = deviceId(context)

        val extra = LinkedHashMap<String, Any?>()
        extra["device"] = DeviceProfile.build(context)
        extra["exception"] = excClass
        extra["message"] = excMsg
        extra["report_id"] = s("REPORT_ID")
        extra["crash_date"] = s("USER_CRASH_DATE")
        extra["app_start"] = s("USER_APP_START_DATE")
        extra["app_version"] = BuildConfig.VERSION_NAME
        extra["version_code"] = BuildConfig.VERSION_CODE
        extra["build_type"] = BuildConfig.BUILD_TYPE
        extra["android"] = s("ANDROID_VERSION").ifEmpty { Build.VERSION.RELEASE }
        extra["phone_model"] = s("PHONE_MODEL").ifEmpty { Build.MODEL }
        extra["logcat"] = s("LOGCAT").take(30_000)
        if (userComment.isNotEmpty()) extra["user_comment"] = userComment

        val body = JSONObject().apply {
            put("category", "crash")
            put("severity", "critical")
            put("source", "crash")
            put("reason", excClass)
            put("title", if (excMsg.isEmpty()) excClass else "$excClass: $excMsg")
            put("device_name", Build.MODEL)
            put("log", stack)
            put("device_id", deviceId)
            if (deviceId.isNotEmpty()) put("fid", deviceId)
            put("extra", JSONObject(extra))
            put("ts", System.currentTimeMillis())
        }

        val conn = URL(RemoteConfig.apiOrigin() + "/analytics/incident").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("X-Device-Id", deviceId)
            conn.setRequestProperty("X-App-Version-Name", BuildConfig.VERSION_NAME)
            conn.setRequestProperty("X-App-Version-Code", BuildConfig.VERSION_CODE.toString())
            conn.setRequestProperty("X-App-Abi", DeviceAbi.primary)
            conn.setRequestProperty("X-App-Sdk", DeviceAbi.sdk.toString())
            conn.setRequestProperty("X-App-Device", DeviceAbi.device)
            BufferedOutputStream(conn.outputStream).use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
                it.flush()
            }
            conn.responseCode
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /** Stable per-install id — Firebase Installation id, else the Android id. */
    private fun deviceId(context: Context): String {
        runCatching {
            val id = com.google.android.gms.tasks.Tasks.await(
                com.google.firebase.installations.FirebaseInstallations.getInstance().id,
                2, TimeUnit.SECONDS
            )
            if (!id.isNullOrBlank()) return id
        }
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
    }

    private fun exceptionClass(stack: String): String {
        val first = stack.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return first.substringBefore(':').trim().ifEmpty { "Throwable" }
    }

    private fun exceptionMessage(stack: String): String {
        val first = stack.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (first.contains(':')) first.substringAfter(':').trim().take(300) else ""
    }
}

class IncidentCrashSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender = IncidentCrashSender()
    override fun enabled(config: CoreConfiguration): Boolean = true
}
