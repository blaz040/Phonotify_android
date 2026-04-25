package com.example.phonotify.services

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
import android.os.ParcelUuid
import androidx.compose.material3.SnackbarDuration
import com.example.ble_con.Snackbar.SnackbarManager
import com.example.phonotify.ViewModelData
import com.example.phonotify.services.notification.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEManager(
    private val context: Context
){
    private val TAG = "BLEManager"
    private val bluetoothManager by lazy {context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter}
    // private val bluetoothLeScanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private val bluetoothLeAdvertiser by lazy { bluetoothAdapter.bluetoothLeAdvertiser}

    private val monitor = Monitoring()
    private var isAdvertising = false
        set(value) {
            ViewModelData.setAdvertising(value);
            field = value
        }
    private var connection = false

    private var connectedDevices: MutableMap<String,MyBluetoothDevice> =  mutableMapOf()


    private val heartBeatTimeoutMillis = 15L * 1000L  // 10 seconds

// --------------------------------------GATT CALLBACK--------------------------------------------
    private val gattServerCallback = object: BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Timber.d("Connected Name: ${device} ${device?.name} Addr: ${device?.address}")
                if(device != null){
                    monitor.addDevice(MyBluetoothDevice(device, System.currentTimeMillis()))
                }
                /*
                if(device != null) {
                    connectedDevices.add(device)
                    ViewModelData.addDevice(device)
                }
                 */
                connection = true
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Timber.d("Disconnected Name: ${device?.name} Addr: ${device?.address} ")
                /*
                if(device != null) {
                    connectedDevices.remove(device)
                    ViewModelData.removeDevice(device)
                }
                 */
                if(device != null){
                    monitor.removeDevice(
                        device = MyBluetoothDevice(device, System.currentTimeMillis()),
                        disconnect = false,
                    )
                }
                if(connectedDevices.size == 0)
                    connection = false
                advertise()
            }
            //connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
            Timber.d("Connected Devices : ${connectedDevices} ")
            //Timber.d("Connected Devices : ${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)}")
            //Timber.d("Connected Devices : ${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)}")

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

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            if (characteristic == null || device == null) return
            if (characteristic == heartBeatCharacteristic) {
                connectedDevices[device.address]?.lastHeartBeat = System.currentTimeMillis()
            }
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
        }
    }
/* -------------------------------SERVICES-&-CHARACTERISTICS--------------------------------------------------*/
    val titleCharacteristic = BluetoothGattCharacteristic(UUIDS.titleCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
    val contextCharacteristic = BluetoothGattCharacteristic(UUIDS.contextCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
    val packageCharacteristic = BluetoothGattCharacteristic(UUIDS.packageCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
    val notifyCompleteCharacteristic = BluetoothGattCharacteristic(UUIDS.notifyCompleteUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
    val disconnectCharacteristic = BluetoothGattCharacteristic(UUIDS.disconnectUUID,
        BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ)

    val heartBeatCharacteristic = BluetoothGattCharacteristic(UUIDS.heartBeatUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)

    private val characteristics = listOf(titleCharacteristic,contextCharacteristic,packageCharacteristic,notifyCompleteCharacteristic,disconnectCharacteristic, heartBeatCharacteristic)

    private val notificationService = BluetoothGattService(UUIDS.notificationServiceUUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY)

    private var bluetoothGattServer: BluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

// ------------------------------ADVERTISE----------------------------------------------------------------
    private var advertiseTries = 0
    private val maxAdvertiseTries = 10
    private val advertisingSettings = AdvertiseSettings.Builder()
        .setConnectable(true)
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    // MAXIMUM data size is 32 B
    private val advertiseData = AdvertiseData.Builder()
        .addServiceUuid(ParcelUuid(UUIDS.notificationServiceUUID)) // UUID is 16 bytes + 2 overhead bytes
        .setIncludeDeviceName(true) // 1 letter is 1 byte + 2 bytes of overhead // 32- 18 = 16 = 14 - 2(overhead) so max length of name is 14 letters
        .build()
    private val advertiseCallback = object: AdvertiseCallback(){
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            advertiseTries = 0

            SnackbarManager.send("Successfully Advertising")
            Timber.d("Advertising success")
        }
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            if(isAdvertising == true) return
            isAdvertising = false
            val text: String = when(errorCode){
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too Large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->{ isAdvertising = true;" TOO Many advertisers"}
                ADVERTISE_FAILED_ALREADY_STARTED -> { isAdvertising = true; "Advertise already started"}
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Not supported"
                else -> "${errorCode}"
            }
            advertiseTries++
            if(advertiseTries < maxAdvertiseTries)
                advertise()
            else{
                SnackbarManager.send(
                    "Failed to advertise",
                    SnackbarDuration.Short
                )
            }
            //SnackbarManager.send("Failed to advertise trying, Reset: $advertiseTries of $maxAdvertiseTries",
            //   SnackbarDuration.Short)
            Timber.d("Advertising failure: $text")
        }

    }
// ----------------------------------- Device & Monitor functions --------------------------------------------------------------
    inner class Monitoring {

    val monitoringScope = CoroutineScope(Dispatchers.Default + Job())

    fun addDevice(device: MyBluetoothDevice) {
            Timber.d("Added device ${device.device.address}")
            connectedDevices.put(device.device.address, device)
            updateViewModel()
        }

        fun removeDevice(device: MyBluetoothDevice, disconnect: Boolean = true) {
            Timber.d("Removing device ${device.device.address}")
            if (disconnect) {
                disconnectDevice(device.device.address)
            }
            connectedDevices.remove(device.device.address)

            updateViewModel()
        }

        private fun updateViewModel() {
            val devices = connectedDevices.values.map { it.device }.toList()
            ViewModelData.setConnectedDevices(devices)
        }

        fun monitorClients() {
            Timber.d("Monitoring")
            val now = System.currentTimeMillis()
            val disconnected =
                connectedDevices.filter { now - it.value.lastHeartBeat > heartBeatTimeoutMillis }
            disconnected.forEach { device ->
                Timber.d("Client ${device.value.device.address} timed out, assumed disconnected")
                //removeDevice(device.value)
            }
        }

        fun startMonitoring() {
            Timber.d("Starting Monitoring")

            monitoringScope.launch {
                while (true) {
                    monitorClients()
                    delay(heartBeatTimeoutMillis)
                }
            }
        }

        // To stop
        fun stopMonitoring() {
            monitoringScope.cancel()
        }
    }
// -------------------------------------------------------------------------------------------------------------------
    init {
        Timber.d("Initializing...")
        //Timber.d(bluetoothGattServer.services.toString())
        bluetoothGattServer.clearServices()
        //connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
        Timber.d("adding Services")
        // add characteristics to service
        characteristics.forEach {
            it?.addDescriptor(BluetoothGattDescriptor(UUIDS.descriptorUUID, BluetoothGattDescriptor.PERMISSION_WRITE))
            notificationService.addCharacteristic(it)
        }
        bluetoothGattServer.addService(notificationService)
        bluetoothAdapter.name = "phServer"

        advertise()
        // monitor.startMonitoring()
    }
    fun advertise(){
        bluetoothLeAdvertiser.startAdvertising(advertisingSettings, advertiseData, advertiseCallback)
    }
    fun stop(){
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
        isAdvertising = false

        monitor.stopMonitoring()

        connectedDevices.forEach { device->
            monitor.removeDevice(device.value)
        }
        //connectedDevices = listOf()
        bluetoothGattServer.clearServices()
        bluetoothGattServer.close()
    }
    fun disconnectDevice(address: String){
        var device: BluetoothDevice? = null
        connectedDevices.forEach {
            if(it.value.device.address == address)
                device = it.value.device
        }
       val systemConnectedDevices = bluetoothManager.getConnectedDevices(BluetoothGattServer.GATT)

        val deviceToDisconnect = systemConnectedDevices.find { it.address == address }
        if(deviceToDisconnect == null) {
            Timber.d("System says $address is not connected. Link might be a 'Ghost'.")
            // If it's a ghost, your only choice is to restart advertising or
            // cycle the Bluetooth adapter.
        }
        else {
            // disconnectCharacteristic.setValue("OK")
            // bluetoothGattServer.notifyCharacteristicChanged(device, disconnectCharacteristic,true)
            bluetoothGattServer.cancelConnection(deviceToDisconnect)
            //Timber.d("Notifying $device to disconnect")
            Timber.d("Cancelling connection to $device")
            SnackbarManager.send("Disconnected from ${device?.name}:$address")
        }
    }
    fun sendNotification(nData: Notification): Boolean{
        val title = nData.title
        val text = nData.text
        val pckg = nData.pckg
        var success = true
        success =  success && titleCharacteristic.setValue(title)
        success =  success && contextCharacteristic.setValue(text)
        success =  success && packageCharacteristic.setValue(pckg)
        success =  success && notifyCompleteCharacteristic.setValue("Ok")
        Timber.d("Writing to characteristics $success: $title $text $pckg")
        notifyChar()
        return success
    }
    private fun notifyChar(){
        connectedDevices.forEach { myBLEDevice ->
            val device = myBLEDevice.value.device
            Timber.d("Notifying device: ${device.name} ${device.address}")
            bluetoothGattServer.notifyCharacteristicChanged(device, notifyCompleteCharacteristic,false)
        }
    }
}