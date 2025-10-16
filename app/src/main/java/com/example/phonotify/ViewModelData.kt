package com.example.phonotify

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.phonotify.service.NotificationData
import kotlinx.coroutines.flow.MutableSharedFlow

object ViewModelData {
   val _advertising = MutableLiveData<Boolean>(false)
    val advertising: LiveData<Boolean> = _advertising

    private val _sendStatus = MutableLiveData<Boolean>(false)
    val sendStatus: LiveData<Boolean> = _sendStatus

    private val _connectedDevices = MutableLiveData<MutableList<BluetoothDevice>>(mutableListOf())
    val connectedDevices: LiveData<MutableList<BluetoothDevice>> = _connectedDevices

    val notyData = MutableSharedFlow<NotificationData>()

    private val _liveNotData = MutableLiveData<NotificationData>()
    val liveNotData: LiveData<NotificationData> = _liveNotData

    fun addDevice(device: BluetoothDevice){
        val newList = _connectedDevices.value.toMutableList().apply{ add(device) }
        _connectedDevices.postValue(newList)
    }
    fun removeDevice(device: BluetoothDevice){
        val newList = _connectedDevices.value.toMutableList().apply{ remove(device) }
        _connectedDevices.postValue(newList)
    }
    fun setAdvertising(state: Boolean){
        _advertising.postValue(state)
    }
    fun setNotification(nData: NotificationData){
        _liveNotData.postValue(nData)
    }
    fun setSentStatus(state: Boolean){
        _sendStatus.postValue(state)
    }
}