package com.example.phonotify.services.bluetooth

import android.bluetooth.BluetoothDevice

data class MyBluetoothDevice(val device: BluetoothDevice, var lastHeartBeat: Long = 0)
