package com.example.phonotify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.phonotify.MyApplication
import com.example.phonotify.R
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CommunicationService: Service() {

    private val TAG = "ComService"

    private val ble_api by lazy { BLEManager(applicationContext)}
    private val notificationID = 1

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var latestNotification: NotificationData = NotificationData("NUll","Null","Null")
    @Override
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"got action")
        when(intent?.action){
            START -> start()
            STOP -> stop()
            RESEND -> resend()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    fun start(){
        Log.d(TAG,"starting....")
        //val disconnectIntent = Intent(this, SensorDataManagerService::class.java).setAction(DEVICE_DISCONNECT)
        //val disconnectPendingIntent: PendingIntent = PendingIntent.getService(this,0,disconnectIntent,PendingIntent.FLAG_IMMUTABLE)

        val ble_builder = NotificationCompat.Builder(this, MyApplication.ble_notification_channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sending notifications")
            .setContentText("...")
            //.addAction(0,"Disconnect",disconnectPendingIntent)
            .build()
        startForeground(notificationID,ble_builder)
        ble_api.advertise()
        serviceScope.launch {
            ViewModelData.notyData.collect {
                Log.d(TAG,"Got Notification Data : $it")
                val ch = ble_api.sendNotification(it)
                ViewModelData.setSentStatus(ch)
                latestNotification = it
            }
        }
    }
    fun stop(){
        ble_api.stop()
        stopSelf()
    }
    fun resend(){
        serviceScope.launch {
            ViewModelData.notyData.emit(latestNotification)
        }
    }

    @Override
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    companion object{
        val START = "start"
        val STOP = "stop"
        val RESEND = "resend"
    }
}