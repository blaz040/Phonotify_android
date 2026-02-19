package com.example.phonotify.datastore

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore


private const val DATASTORE_FILE_NAME = "applications.pb"

val Context.applicationsDataStore: DataStore<Applications> by dataStore(
    fileName = DATASTORE_FILE_NAME,
    serializer = ApplicationsSerializer
)

data class ApplicationUi(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var allowNotifications: Boolean
)