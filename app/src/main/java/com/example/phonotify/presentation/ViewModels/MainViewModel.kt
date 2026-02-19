package com.example.phonotify.presentation.ViewModels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.phonotify.ViewModelData
import com.example.phonotify.services.CommunicationService
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(application: Application): AndroidViewModel(application) {

    //var mainSwitchStatus = mutableStateOf(false)

    val mainSwitchStatus: LiveData<Boolean> = ViewModelData.serviceRunning

    fun startBLE(){
        ViewModelData.serviceRunning.value = true
        Timber.d("Sent START for notification")
        send(CommunicationService.Companion.START)
    }
    fun stopBLE(){
        ViewModelData.serviceRunning.value = false
        Timber.d("Sent STOP for notification")
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