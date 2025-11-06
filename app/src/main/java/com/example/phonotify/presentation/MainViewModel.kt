package com.example.phonotify.presentation

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.ble_con.fileManager.FileManager
import com.example.phonotify.ViewModelData
import com.example.phonotify.service.CommunicationService
import kotlinx.coroutines.launch

class MainViewModel(application: Application): AndroidViewModel(application) {

    //var main_switch_checked = mutableStateOf(false)

    val main_switch_checked: LiveData<Boolean> = ViewModelData.serviceRunning

    fun startBLE(){
        ViewModelData.serviceRunning.postValue(true)
        Log.d("notListener","Sent START for notification")
        send(CommunicationService.Companion.START)
    }
    fun stopBLE(){
        ViewModelData.serviceRunning.postValue(false)
        Log.d("notListener","Sent STOP for notification")
        send(CommunicationService.Companion.STOP)
    }
    fun disconnectDevice(device: BluetoothDevice){
        val address = device.address
        Intent(application, CommunicationService::class.java).also{
            it.action = CommunicationService.Companion.DISCONNECT_DEVICE
            it.putExtra("device_address",address)
            application.startService(it)
        }
    }
    private fun send(action: String){
        Intent(application, CommunicationService::class.java).also{
            it.action = action
            application.startService(it)
        }
    }
    fun resendRecentNotification(){
        send(CommunicationService.Companion.RESEND)
    }
    init {
        viewModelScope.launch {
            ViewModelData.notyData.collect{
                ViewModelData.setNotification(it)
            }
        }

    }
}