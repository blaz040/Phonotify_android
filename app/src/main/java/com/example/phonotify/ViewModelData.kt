package com.example.phonotify

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.phonotify.service.NotificationData
import kotlinx.coroutines.flow.MutableSharedFlow

object ViewModelData {
    private val _advertising = MutableLiveData<Boolean>(false)
    val advertising: LiveData<Boolean> = _advertising

    private val _sendStatus = MutableLiveData<Boolean>(false)
    val sendStatus: LiveData<Boolean> = _sendStatus

    val serviceRunning = MutableLiveData<Boolean>(false)

    private val _connectedDevices = MutableLiveData<List<BluetoothDevice>>(listOf())
    val connectedDevices: LiveData<List<BluetoothDevice>> = _connectedDevices

    val notyData = MutableSharedFlow<NotificationData>()

    val newLogs = MutableSharedFlow<Boolean>()

    private val _liveNotData = MutableLiveData<NotificationData>()
    val liveNotData: LiveData<NotificationData> = _liveNotData
    
    fun setConnectedDevices(list: List<BluetoothDevice>){
        _connectedDevices.postValue(list.toList())
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