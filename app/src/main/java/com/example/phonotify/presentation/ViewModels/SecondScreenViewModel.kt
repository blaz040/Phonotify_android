package com.example.phonotify.presentation.ViewModels

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.phonotify.datastore.ApplicationUi
import com.example.phonotify.datastore.ApplicationsRepository
import com.example.phonotify.datastore.IconSerializer
import com.example.phonotify.services.notification.NotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class SecondScreenViewModel(application: Application): AndroidViewModel(application) {

    private val applicationsRepository = ApplicationsRepository(application)
    private val _installedApplications = MutableLiveData<List<ApplicationUi>>()
    val installedApplications: LiveData<List<ApplicationUi>> = _installedApplications

    val enabledApplications: LiveData<List<ApplicationUi>> = installedApplications.map { list ->
        list.filter { it.allowNotifications } // Assuming 'allowNotifications' is your boolean
    }

    val disabledApplications: LiveData<List<ApplicationUi>> = installedApplications.map { list ->
        list.filter { it.allowNotifications == false }
    }
    private var installedApplicationsList: List<ApplicationUi> = mutableListOf()
        set(value) {
            val sortedList = value.sortByName()
            field = sortedList
            _installedApplications.postValue(sortedList)
            onSearchQueryChange(searchQuery.value)
        }
    private val workingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun List<ApplicationUi>.sortByName(): List<ApplicationUi>{
        return this.sortedBy { it.appName.lowercase() }
    }
    var searchQuery = mutableStateOf("")
    // 3. The list the UI actually sees
    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
        val filteredList = installedApplicationsList.filter { it.appName.contains(newQuery, ignoreCase = true) }.sortByName()
        _installedApplications.postValue(filteredList)
    }
    private fun getInstalledApps() {
        val pm = application.packageManager
        // Use getInstalledPackages for a full list
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            if((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue // SYSTEM PACKAGE
            val app = applicationsRepository.createApplicationUi(
                appName = appInfo.loadLabel(pm).toString(),
                packageName = appInfo.packageName,
                icon = appInfo.loadIcon(pm),
                allowNotifications = false
            )
            addApplication(app)
        }
       // _installedApps.postValue(__installedApps.values.toList().sortedBy { it.appName })
    }
    fun updateApp(app: ApplicationUi) {
        //__installedApps[app.packageName]!!.allowNotifications = app.allowNotifications
        if (app.allowNotifications) {
            _protoUpdateApplication(app)
            NotificationData.allowedPackages.add(app.packageName)

            Timber.d("Added ${app.packageName}")
            Timber.d("Current Allowed packages: ${NotificationData.allowedPackages.toString()}")
        } else {
            _protoUpdateApplication(app)
            NotificationData.allowedPackages.remove(app.packageName)
        }
    }
    // Updates application in datastore
    private fun _protoUpdateApplication(app: ApplicationUi){
        viewModelScope.launch {
            applicationsRepository.updateApplication(app)
        }
    }
    private fun addApplication(app: ApplicationUi){
        viewModelScope.launch {
            applicationsRepository.addApplication(app)
        }
         //__installedApps[app.packageName] = app
    }
    //data class App(val packageName: String, val appName: String, val appIcon: Drawable? = null, var allowNotifications: Boolean = false)

    init {
        viewModelScope.launch {
            applicationsRepository.applications.collect {
                installedApplicationsList = it
            }
        }
        workingScope.launch {
            if(applicationsRepository.getApplications().isEmpty()){
                getInstalledApps()
            }
        }
    }
}