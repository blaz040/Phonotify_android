package com.example.phonotify.services.updater

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Environment
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

    fun checkAndUpdate(currentVersion: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val release = fetchLatestRelease() ?: return@launch
                val latestVersion = release.tagName.trimStart('v')
                val current = currentVersion.trimStart('v')

                if (latestVersion == current) return@launch  // already up to date

                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@launch

                withContext(Dispatchers.Main) {
                    showUpdateDialog(latestVersion, apkAsset.downloadUrl)
                }
            } catch (e: Exception) {
                Timber.e("Update check failed, ${e}")
            }
        }
    }

    private fun fetchLatestRelease(): GithubRelease? {
        val request = Request.Builder()
            .url(Constants.apkUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return gson.fromJson(response.body?.string(), GithubRelease::class.java)
        }
    }

    private fun showUpdateDialog(newVersion: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Update available")
            .setMessage("Version $newVersion is available. Update now?")
            .setPositiveButton("Update") { _, _ -> downloadAndInstall(downloadUrl) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstall(downloadUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
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

                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                Timber.e("Download failed ${e}")
            }
        }
    }

    private fun installApk(apkFile: File) {
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