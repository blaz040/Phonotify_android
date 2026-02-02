package com.example.phonotify.service

import android.bluetooth.BluetoothDevice

data class MyBluetoothDevice(val device: BluetoothDevice, var lastHeartBeat: Long = 0)
