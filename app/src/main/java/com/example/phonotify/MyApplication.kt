package com.example.phonotify

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.example.ble_con.fileManager.FileManager
import timber.log.Timber

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        val ble_channel = NotificationChannel(ble_notification_channel,"Running Notifications",
            NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(ble_channel)

        Timber.plant(MyDebugTree(this))

    }

    companion object{
        val ble_notification_channel = "BLE_Channel"
    }
}
