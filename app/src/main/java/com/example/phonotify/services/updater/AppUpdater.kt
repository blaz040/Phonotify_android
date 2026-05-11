package com.example.phonotify.services.updater

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.AlertDialog
import androidx.core.content.FileProvider
import com.example.phonotify.Constants
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class AppUpdater(private val context: Context) {

    private val client = OkHttpClient()
    private val gson = Gson()

    data class GithubRelease(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("assets") val assets: List<GithubAsset>
    )

    data class GithubAsset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String
    )

    suspend fun fetchLatestRelease(): GithubRelease? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/blaz040/Phonotify_android/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e("API failed: ${response.code}")
                        return@withContext null
                    }
                    gson.fromJson(response.body?.string(), GithubRelease::class.java)
                }
            } catch (e: Exception) {
                Timber.e("Fetch failed: ${e.message}")
                null
            }
        }
    }

    suspend fun downloadApk(downloadUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val apkFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "phonotify-update.apk"
                )

                val request = Request.Builder().url(downloadUrl).build()
                client.newCall(request).execute().use { response ->
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                apkFile
            } catch (e: Exception) {
                Timber.e("Download failed: ${e.message}")
                null
            }
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}