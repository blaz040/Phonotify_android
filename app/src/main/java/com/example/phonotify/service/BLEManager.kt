package com.example.phonotify.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import com.example.phonotify.ViewModelData
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEManager(
    private val context: Context
){
    private val TAG = "BLEManager"
    private val bluetoothManager by lazy {context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter}
    //private val bluetoothLeScanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private val bluetoothLeAdvertiser by lazy { bluetoothAdapter.bluetoothLeAdvertiser}
    private var isAdvertising = false
    private var connection = false

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()
    // -----------------------------------UUIDS-----------------------------------------------
    private val notificationServiceUUID= UUID.fromString("91d76000-ac7b-4d70-ab3a-8b87a357239e")
    private val titleCharacteristicUUID = UUID.fromString("91d76001-ac7b-4d70-ab3a-8b87a357239e")
    private val contextCharacteristicUUID = UUID.fromString("91d76002-ac7b-4d70-ab3a-8b87a357239e")
    private val packageCharacteristicUUID = UUID.fromString("91d76003-ac7b-4d70-ab3a-8b87a357239e")
    private val notifyCompleteUUID = UUID.fromString("91d76004-ac7b-4d70-ab3a-8b87a357239e")

    private val descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // --------------------------------------GATT CALLBACk--------------------------------------------
    private val gattServerCallback = object: BluetoothGattServerCallback(){
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.d(TAG,"Connected Name: ${device} ${device?.name} Addr: ${device?.address}")
                if(device != null) {
                    connectedDevices.add(device)
                    ViewModelData.addDevice(device)
                }
                connection = true
                //ViewModelData._connectionStatus.postValue(connection)
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d(TAG,"Disconnected Name: ${device?.name} Addr: ${device?.address} ")

                if(device != null) {
                    connectedDevices.remove(device)
                    ViewModelData.removeDevice(device)
                }
                if(connectedDevices.size == 0)
                    connection = false
                //ViewModelData._connectionStatus.postValue(connection)
                advertise()
            }
            super.onConnectionStateChange(device, status, newState)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            if (characteristic == null){
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                return
            }
            val value = characteristic.value
            val payload = if (offset > 0 && offset < value.size) value.copyOfRange(offset, value.size) else value

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, payload)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            if(descriptor != null)
                descriptor.value = value
            bluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,null)
        }
    }
    // -------------------------------SERVICES-&-CHARACTERISTICS--------------------------------------------------
    val titleCharacteristic = BluetoothGattCharacteristic(titleCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
    val contextCharacteristic = BluetoothGattCharacteristic(contextCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
    val packageCharacteristic = BluetoothGattCharacteristic(packageCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
    val notifyCompleteCharacteristic = BluetoothGattCharacteristic(notifyCompleteUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)

    private val characteristics = listOf(titleCharacteristic,contextCharacteristic,packageCharacteristic,notifyCompleteCharacteristic)

    private val notificationService = BluetoothGattService(notificationServiceUUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY)

    private var bluetoothGattServer: BluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

    // ------------------------------ADVERTISE----------------------------------------------------------------
    private val advertisingSettings = AdvertiseSettings.Builder()
        .setConnectable(true)
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    private val advertiseData = AdvertiseData.Builder()
        //.addServiceUuid(ParcelUuid(notificationServiceUUID))
        .setIncludeDeviceName(true)
        .build()
    private val advertiseCallback = object: AdvertiseCallback(){
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            isAdvertising = false
            ViewModelData.setAdvertising(isAdvertising)
            Log.d(TAG,"Advertising failure: $errorCode")
        }
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true

            ViewModelData.setAdvertising(isAdvertising)
            Log.d(TAG,"Advertising success")
        }

    }
    // ----------------------------------------------------------------------------------
    init {

        Log.d(TAG,"Initializing...")
        Log.d(TAG,bluetoothGattServer.services.toString())
        bluetoothGattServer.clearServices()
        Log.d(TAG,"adding Services")
        characteristics.forEach {
            it?.addDescriptor(BluetoothGattDescriptor(descriptorUUID,
                BluetoothGattDescriptor.PERMISSION_WRITE))
            notificationService.addCharacteristic(it)
        }

        bluetoothGattServer.addService(notificationService)

        bluetoothAdapter.name = "Redmi"
        advertise()
    }
    fun advertise(){
        bluetoothLeAdvertiser.startAdvertising(advertisingSettings,advertiseData,advertiseCallback)
    }
    fun stop(){
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
        isAdvertising = false
        ViewModelData.setAdvertising(isAdvertising)

        bluetoothGattServer.clearServices()
        bluetoothGattServer.close()
    }
    fun sendNotification(nData: NotificationData): Boolean{
        val title = nData.title
        val text = nData.text
        val pckg = nData.pckg
        var succ = true
        val data = title + "&"+ text + "&"+ pckg
        succ =  succ && titleCharacteristic.setValue(title)
        succ =  succ && contextCharacteristic.setValue(text)
        succ =  succ && packageCharacteristic.setValue(pckg)
        succ =  succ && notifyCompleteCharacteristic.setValue("Ok")
        Log.d(TAG,"Writing to characteristics $succ: $title $text $pckg")
        notifyChar()
        return succ
    }
    private fun notifyChar(){
        connectedDevices.forEach {  device->
            Log.d(TAG,"Notifying device: ${device.name}")
            //characteristics.forEach { characteristic->
                bluetoothGattServer.notifyCharacteristicChanged(device, notifyCompleteCharacteristic,false)
            //}
        }
    }
    /*
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }
    fun closeConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
    fun broadcastUpdate(action:String) {
        val intent = Intent(action)
            .setPackage(context.getPackageName());
        context.sendBroadcast(intent)
    }
    fun connectToDevice(device: BluetoothDevice) {
        // Disconnect from any previously connected device
        disconnect()

        val gattQueue = mutableListOf<() ->Unit>()
        var gattQueueBusy = false

        fun nextGattOperation() {
            if(gattQueue.isNotEmpty() && !gattQueueBusy)
            {
                gattQueueBusy = true
                gattQueue.removeAt(0).invoke()
            }
        }
        // Attempt to connect to the GATT server
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            // Callback for connection state changes
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Device connected
                    Log.d("GATT_CONN", "Connected to GATT server.")
                    gatt.discoverServices()

                    broadcastUpdate(BluetoothBroadcastAction.CONNECTED)

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Device disconnected
                    Log.d("GATT_CONN", "Disconnected from GATT server.")

                    broadcastUpdate(BluetoothBroadcastAction.DISCONNECTED)

                    closeConnection()
                }
            }
            // Callback for services discovered
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GATT_CONN", "Services discovered successfully")
                    //gatt.printGattTable()
                    gatt.services.forEach {
                        Log.d("GATT_CONN","service ${it.type}: ${it.uuid} discovered")
                    }

                    //Enable notifications for each characteristic in list
                    env_characteristics_list.forEach {
                        val tmpChar = gatt.getService(envService_UUID)?.getCharacteristic(it)
                        if(tmpChar != null) {
                            gattQueue.add { enableCharacteristicNotification(gatt, tmpChar) }
                        }
                    }
                    other_characteristics_list.forEach {
                        val tmpChar = gatt.getService(otherService_UUID)?.getCharacteristic(it)
                        if(tmpChar != null) {
                            gattQueue.add { enableCharacteristicNotification(gatt, tmpChar) }
                        }
                    }

                    nextGattOperation()
                } else {
                    Log.e("GATT_CONN", "Service discovery failed with status: $status")
                }
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)
                gattQueueBusy = false
                nextGattOperation()
            }

            private fun enableCharacteristicNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(CCCD_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val writeSuccess = gatt.writeDescriptor(descriptor)
                    Log.d("GATT_CONN", "Attempting to write CCCD for ${characteristic.uuid}: $writeSuccess")
                } else {
                    Log.e("GATT_CONN", "CCCD descriptor is NULL for characteristic: ${characteristic.uuid}. Continuing with next op.")
                    gattQueueBusy = false
                    nextGattOperation()
                }
            }
            // Callback for characteristic read operations
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: android.bluetooth.BluetoothGattCharacteristic, status: Int) {
                //super.onCharacteristicRead(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0) // Get the byte array value
                    Log.d("GATT_CON", "Characteristic ${characteristic.uuid} read: ${value}")
                    // Process the read value here
                } else {
                    Log.d("GATT_CON", "Characteristic ${characteristic.uuid} read failed with status: $status")
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.let { char -> onDataReceived(char) }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                Log.d("GATT_WRITE","Writing to characteristic : $status")
            }
        })
    }

    fun calcAltitude(pressure: Float): Float {
        val sea_press = SensorData.seaLevelPressure
        val temp     = SensorData.seaLevelTemperature
        //val temp = SensorData.temperature.getList().last().y
        return round((((sea_press / pressure).pow(1 / 5.257f) - 1.0f) * (temp + 273.15f)) / 0.0065f)
    }
    fun onDataReceived(char: BluetoothGattCharacteristic) {
        val shortFormat = BluetoothGattCharacteristic.FORMAT_UINT16
        val intFormat = BluetoothGattCharacteristic.FORMAT_UINT32
        when (char.uuid) {
            temp_UUID -> {
                val floatValue = char.getIntValue(shortFormat,0).toFloat()/100
                SensorData.temperature.add(floatValue)
                Log.d("GATT_NOTIFY", "Characteristic temp: $floatValue")
            }
            humidity_UUID -> {
                val value = char.getIntValue(shortFormat,0)
                SensorData.humidity.add(value)
                Log.d("GATT_NOTIFY", "Characteristic humidity: $value ")
            }
            pressure_UUID ->{
                val pressure = char.getIntValue(intFormat,0).toFloat()/100
                SensorData.pressure.add(pressure)
                SensorData.altitude.add(calcAltitude(pressure))

                Log.d("GATT_NOTIFY", "Characteristic pressure: $pressure ")
            }
            IAQ_UUID -> {
                val value = char.getIntValue(shortFormat,0)
                SensorData.iaq.add(value)

                Log.d("GATT_NOTIFY", "Characteristic IAQ: $value")
            }
            bVOC_UUID -> {
                val floatValue = char.getIntValue(shortFormat,0).toFloat()/100
                SensorData.voc.add(floatValue)
                Log.d("GATT_NOTIFY", "Characteristic bVOC: $floatValue")
            }
            CO2_UUID -> {
                val value = char.getIntValue(shortFormat,0)
                SensorData.co2.add(value)
                Log.d("GATT_NOTIFY", "Characteristic CO2: $value ")
            }
            step_UUID -> {
                val value = char.getIntValue(shortFormat,0)
                SensorData.steps.add(value)
                Log.d("GATT_NOTIFY","Characteristic Steps: $value")
            }
            else -> {
                Log.d("GATT_NOTIFY", "Characteristic Unknown")
            }
        }
    }
    fun send(value: Int)
    {
        val service = bluetoothGatt?.getService(otherService_UUID)
        val commandChar = service?.getCharacteristic(messageReceiver_UUID)

        if(commandChar == null) {bluetoothGatt?.printGattTable(); return}

        commandChar?.setValue(value,BluetoothGattCharacteristic.FORMAT_UINT16,0)
        val writeSuccess = bluetoothGatt?.writeCharacteristic(commandChar)
        Log.d("BlE_WRITE","Attempting to write command: $value. Success: $writeSuccess")
    }
    */
}