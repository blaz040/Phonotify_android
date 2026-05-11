package com.example.phonotify.ui.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.phonotify.services.updater.AppUpdater
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val version: String, val downloadUrl: String) : UpdateState()
    object Downloading : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updater = AppUpdater(application)

    private val _updateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val updateState: LiveData<UpdateState> = _updateState

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            val release = updater.fetchLatestRelease()

            if (release == null) {
                _updateState.value = UpdateState.Error("Could not reach GitHub")
                return@launch
            }

            val latest = release.tagName.trimStart('v')
            val current = currentVersion.trimStart('v')

            if (latest == current) {
                _updateState.value = UpdateState.UpToDate
                return@launch
            }

            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
            if (apk == null) {
                _updateState.value = UpdateState.Error("No APK found in release")
                return@launch
            }

            _updateState.value = UpdateState.UpdateAvailable(latest, apk.downloadUrl)
        }
    }

    fun downloadAndInstall(downloadUrl: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading

            val file = updater.downloadApk(downloadUrl)
            if (file == null) {
                _updateState.value = UpdateState.Error("Download failed")
                return@launch
            }

            updater.installApk(file)
            _updateState.value = UpdateState.Idle
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}