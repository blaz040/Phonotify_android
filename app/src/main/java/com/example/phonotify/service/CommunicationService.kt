package com.example.phonotify.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.phonotify.MainActivity
import com.example.phonotify.MyApplication
import com.example.phonotify.R
import com.example.phonotify.ViewModelData
import com.example.phonotify.service.notification.NotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class CommunicationService: Service() {

    private val TAG = "ComService"

    private val ble_api by lazy { BLEManager(applicationContext)}
    private val notificationID = 1
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var latestNotification: NotificationData = NotificationData("NUll", "Null", "Null")

    @Override
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("got action")
        when(intent?.action){
            START -> start()
            STOP -> stop()
            RESEND -> resend()
            DISCONNECT_DEVICE -> disconnectDevice(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }
    fun start(){
        Timber.d("starting....")

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val bleNotification = NotificationCompat.Builder(this, MyApplication.ble_notification_channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Listening to notifications")
            .setContentText("...")
            .setContentIntent(pendingIntent)
            //.addAction(0,"Disconnect",disconnectPendingIntent)
            .build()
        startForeground(notificationID, bleNotification)

        ble_api.advertise()

        serviceScope.launch {
            ViewModelData.notyData.collect {
                Timber.d("Got Notification Data : $it")
                val status = ble_api.sendNotification(it)
                ViewModelData.setSentStatus(status)
                latestNotification = it
            }
        }
    }
    fun stop(){
        ble_api.stop()
        stopSelf()
    }
    fun disconnectDevice(intent: Intent){
        val address = intent.getStringExtra("device_address")
        if(address != null)
            ble_api.disconnectDevice(address)
    }
    fun resend(){
        serviceScope.launch {
            ViewModelData.notyData.emit(latestNotification)
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    companion object{
        val START = "start"
        val STOP = "stop"
        val RESEND = "resend"
        val DISCONNECT_DEVICE = "DC_device"
    }
}