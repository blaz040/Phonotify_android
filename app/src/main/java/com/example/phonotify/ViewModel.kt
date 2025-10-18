package com.example.phonotify

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.phonotify.service.CommunicationService
import kotlinx.coroutines.launch

class ViewModel(application: Application): AndroidViewModel(application) {

    var main_switch_checked = mutableStateOf(false)

    fun startBLE(){
        Log.d("notListener","Sent START for notification")
        send(CommunicationService.START)
    }
    fun stopBLE(){
        Log.d("notListener","Sent STOP for notification")
        send(CommunicationService.STOP)
    }
    fun disconnectDevice(device: BluetoothDevice){
        val address = device.address
        Intent(application, CommunicationService::class.java).also{
            it.action = CommunicationService.DISCONNECT_DEVICE
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
        send(CommunicationService.RESEND)
    }
    init {
        viewModelScope.launch{
            ViewModelData.notyData.collect{
                ViewModelData.setNotification(it)
            }
        }

    }
}