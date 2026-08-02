package com.movie.app.best.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ketch.DownloadModel
import com.ketch.Status
import com.movie.app.best.MovieApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkUpdateRepository @Inject constructor() {

    companion object {
        private const val UPDATE_DIR = "updates"
        private const val APK_NAME = "blazemovies-update.apk"
    }

    fun downloadApk(context: Context, url: String): Int {
        val app = context.applicationContext as MovieApplication
        val updateDir = File(context.cacheDir, UPDATE_DIR).apply { if (!exists()) mkdirs() }

        val headers = HashMap<String, String>().apply {
            put("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            put("Accept", "*/*")
        }

        return app.ketch.download(
            url = url,
            path = updateDir.path,
            fileName = APK_NAME,
            tag = "apk_update",
            headers = headers
        )
    }

    fun observeUpdateProgress(context: Context, ketchId: Int): Flow<ApkUpdateState> {
        val ctx = context.applicationContext
        val app = ctx as MovieApplication
        return app.ketch.observeDownloads().map { downloads ->
            val dl = downloads.find { it.id == ketchId }
            if (dl == null) {
                ApkUpdateState.Idle
            } else {
                when (dl.status) {
                    Status.QUEUED -> ApkUpdateState.Downloading(0)
                    Status.STARTED, Status.PROGRESS -> ApkUpdateState.Downloading(dl.progress)
                    Status.PAUSED -> ApkUpdateState.Paused(dl.progress)
                    Status.SUCCESS -> {
                        val actualFile = File(dl.path, dl.fileName)
                        if (actualFile.isFile) {
                            ApkUpdateState.Completed(actualFile)
                        } else {
                            val fallback = getDownloadedApk(ctx)
                            if (fallback != null && fallback.isFile) {
                                ApkUpdateState.Completed(fallback)
                            } else {
                                ApkUpdateState.Error("APK file not found. path=${dl.path} name=${dl.fileName}")
                            }
                        }
                    }
                    Status.FAILED -> ApkUpdateState.Error("Download failed")
                    Status.CANCELLED -> ApkUpdateState.Cancelled
                    else -> ApkUpdateState.Downloading(dl.progress)
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.isFile) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }

    fun getDownloadedApk(context: Context): File? {
        val file = File(context.cacheDir, "$UPDATE_DIR/$APK_NAME")
        return if (file.isFile) file else null
    }

    fun cleanupOldApk(context: Context) {
        val updateDir = File(context.cacheDir, UPDATE_DIR)
        if (updateDir.exists()) {
            updateDir.listFiles()?.forEach { it.delete() }
        }
    }
}

sealed class ApkUpdateState {
    data object Idle : ApkUpdateState()
    data class Downloading(val progress: Int) : ApkUpdateState()
    data class Paused(val progress: Int) : ApkUpdateState()
    data class Completed(val file: File) : ApkUpdateState()
    data class Error(val message: String) : ApkUpdateState()
    data object Cancelled : ApkUpdateState()
}
