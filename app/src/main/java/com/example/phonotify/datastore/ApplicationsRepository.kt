package com.example.phonotify.datastore

import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.collections.map

class ApplicationsRepository(
    val context: Context
) {
    private val datastore = context.applicationsDataStore

    // Memory cache: Key = PackageName + Hash of ByteString, Value = Decoded Drawable
    private val iconCache = mutableMapOf<String, Pair<Int, Drawable>>()
    val  applications: Flow<List<ApplicationUi>> = datastore.data.map { protoApps ->
       convertApplicationDataToApplicationUi(listAppData = protoApps.applicationDataList.toList())
    }

    //val appDataList: Flow<List<ApplicationData>> = datastore.data.map { it.applicationDataList.toList() }
    suspend fun addApplication(app: ApplicationUi){
        datastore.updateData {
            val appData = convertApplicationUiToApplicationData(app)
            it.toBuilder()
                .addApplicationData(appData)
                .build()
        }
    }
    suspend fun addApplicationIfNotExists(app: ApplicationUi) {
        val appData = convertApplicationUiToApplicationData(app)
        datastore.updateData { curr ->
            val index = getIndex(curr, appData)
            if (index != -1) {
                Timber.d("App with ${app.packageName} already exists")
                return@updateData curr
            }
            curr.toBuilder()
                .addApplicationData(appData)
                .build()
        }
    }
    suspend fun getApplications(): List<ApplicationUi>{
        return convertApplicationDataToApplicationUi(listAppData = datastore.data.first().applicationDataList.toList())
    }

    suspend fun removeAllApplications(){
        datastore.updateData {
            it.toBuilder()
                .clearApplicationData()
                .build()
        }
    }
    suspend fun removeApplication(app: ApplicationUi){
        datastore.updateData {  curr->

            val appData = convertApplicationUiToApplicationData(app)
            val index = getIndex(curr, appData)
            if (index == -1){
                Timber.d("No such app with ${app.packageName} not found")
                return@updateData curr
            }
            curr.toBuilder()
                .removeApplicationData(index)
                .build()
        }
    }
    suspend fun updateApplication(app: ApplicationUi){
        val appData = convertApplicationUiToApplicationData(app)
        datastore.updateData { curr ->
            val index = getIndex(curr, appData)
            if (index == -1){
                Timber.d("No such app with ${app.packageName} not found")
                return@updateData curr
            }
            curr.toBuilder()
                .setApplicationData(index, appData)
                .build()
        }
    }
    private fun getIndex(apps: Applications, app: ApplicationData): Int{
        val index = apps.applicationDataList.indexOfFirst{ it.packageName == app.packageName }
        return index
    }
    fun createApplicationUi(packageName: String, appName: String, icon: Drawable, allowNotifications: Boolean): ApplicationUi {
//        val app = ApplicationData.newBuilder()
//            .setAppName(appName) // The user-friendly name (e.g., "WhatsApp")
//            .setPackageName(packageName)      // The unique ID (e.g., "com.whatsapp")
//            .setIconData(IconSerializer.toByteString(icon))
//            .setAllowNotifications(allowNotifications)
//            .build()
        val app = ApplicationUi(
            appName = appName,
            packageName = packageName,
            icon = icon,
            allowNotifications = allowNotifications
        )
        return app
    }

    private fun convertApplicationUiToApplicationData(appUi: ApplicationUi): ApplicationData{
        val appData = ApplicationData.newBuilder()
            .setAppName(appUi.appName)
            .setPackageName(appUi.packageName)
            .setIconData(IconSerializer.toByteString(appUi.icon))
            .setAllowNotifications(appUi.allowNotifications)
            .build()
        return appData
    }
    private fun convertApplicationDataToApplicationUi(listAppData: List<ApplicationData>): List<ApplicationUi>{
        return listAppData.map { protoItem ->

            // Check if we already have this icon decoded and if it's the same version
            val iconHash = protoItem.iconData.hashCode()
            val cachedIcon = iconCache[protoItem.packageName]

            val finalIcon = if (cachedIcon != null && cachedIcon.first == iconHash) {
                // Efficiency Win: Use the already decoded Drawable
                cachedIcon.second
            } else {
                // Only decode if it's new or changed
                val decoded = IconSerializer.toDrawable(context, protoItem.iconData)
                if (decoded != null) {
                    iconCache[protoItem.packageName] = Pair(iconHash, decoded)
                }
                decoded
            }

            ApplicationUi(
                packageName = protoItem.packageName,
                appName = protoItem.appName,
                icon = finalIcon,
                allowNotifications = protoItem.allowNotifications
            )
        }
    }
}