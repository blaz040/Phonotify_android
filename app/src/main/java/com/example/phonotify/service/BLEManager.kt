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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.delay
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
        set(value) {
            ViewModelData.setAdvertising(value);
            field = value
        }
    private var connection = false

    private var connectedDevices: List<BluetoothDevice> =  listOf()
        set(value) {
            ViewModelData.setConnectedDevices(value)
            field = value
        }
    // -----------------------------------UUIDS-----------------------------------------------
    private val notificationServiceUUID= UUID.fromString("91d76000-ac7b-4d70-ab3a-8b87a357239e")
    private val titleCharacteristicUUID = UUID.fromString("91d76001-ac7b-4d70-ab3a-8b87a357239e")
    private val contextCharacteristicUUID = UUID.fromString("91d76002-ac7b-4d70-ab3a-8b87a357239e")
    private val packageCharacteristicUUID = UUID.fromString("91d76003-ac7b-4d70-ab3a-8b87a357239e")
    private val notifyCompleteUUID = UUID.fromString("91d76004-ac7b-4d70-ab3a-8b87a357239e")
    private val disconnectUUID = UUID.fromString("91d76005-ac7b-4d70-ab3a-8b87a357239e")

    private val descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // --------------------------------------GATT CALLBACK--------------------------------------------
    private val gattServerCallback = object: BluetoothGattServerCallback(){
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.d(TAG,"Connected Name: ${device} ${device?.name} Addr: ${device?.address}")
                /*if(device != null) {
                    connectedDevices.add(device)
                    ViewModelData.addDevice(device)
                }
                 */
                connection = true
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d(TAG,"Disconnected Name: ${device?.name} Addr: ${device?.address} ")
                /*
                if(device != null) {
                    connectedDevices.remove(device)
                    ViewModelData.removeDevice(device)
                }

                 */
                if(connectedDevices.size == 0)
                    connection = false
                advertise()
            }
            connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
            Log.d(TAG,"Connected Devices : ${connectedDevices} ")
            //Log.d(TAG,"Connected Devices : ${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)}")
            //Log.d(TAG,"Connected Devices : ${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)}")

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
    val disconnectCharacteristic = BluetoothGattCharacteristic(disconnectUUID,
        BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PERMISSION_READ)
    private val characteristics = listOf(titleCharacteristic,contextCharacteristic,packageCharacteristic,notifyCompleteCharacteristic,disconnectCharacteristic)

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
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)

            isAdvertising = true
            Log.d(TAG,"Advertising success")
        }
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            isAdvertising = false
            val text: String = when(errorCode){
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too Large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->{ isAdvertising = true;" TOO Many advertisers"}
                ADVERTISE_FAILED_ALREADY_STARTED -> { isAdvertising = true; "Advertise already started"}
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Not supported"
                else-> "${errorCode}"
            }
            Log.d(TAG,"Advertising failure: $text")
        }

    }
    // ----------------------------------------------------------------------------------
    init {

        Log.d(TAG,"Initializing...")
        //Log.d(TAG,bluetoothGattServer.services.toString())
        bluetoothGattServer.clearServices()
        connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
        Log.d(TAG,"adding Services")
        characteristics.forEach {
            it?.addDescriptor(BluetoothGattDescriptor(descriptorUUID, BluetoothGattDescriptor.PERMISSION_WRITE))
            notificationService.addCharacteristic(it)
        }

        bluetoothGattServer.addService(notificationService)

        bluetoothAdapter.name = "Redmi"
        advertise()
    }
    fun advertise(){
        isAdvertising = true

        bluetoothLeAdvertiser.startAdvertising(advertisingSettings,advertiseData,advertiseCallback)
    }
    fun stop(){
        isAdvertising = false
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
        connectedDevices.forEach { device->
            disconnectDevice(device.address)
        }
        connectedDevices = listOf()
        bluetoothGattServer.clearServices()
        bluetoothGattServer.close()
    }
    fun disconnectDevice(address: String){
        var device: BluetoothDevice? = null
        connectedDevices.forEach {
            if(it.address == address)
                device = it
        }
        if(device == null) Log.d(TAG,"Cant disconnect $address not found (NULL)")
        else {
            disconnectCharacteristic.setValue("OK")
            bluetoothGattServer.notifyCharacteristicChanged(device, disconnectCharacteristic,true)
            bluetoothGattServer.cancelConnection(device)
            Log.d(TAG,"Notifying $device to disconnect")
            Log.d(TAG,"Cancelling connection to $device")
        }
    }
    fun sendNotification(nData: NotificationData): Boolean{
        val title = nData.title
        val text = nData.text
        val pckg = nData.pckg
        var succ = true
        succ =  succ && titleCharacteristic.setValue(title)
        succ =  succ && contextCharacteristic.setValue(text)
        succ =  succ && packageCharacteristic.setValue(pckg)
        succ =  succ && notifyCompleteCharacteristic.setValue("Ok")
        Log.d(TAG,"Writing to characteristics $succ: $title $text $pckg")
        notifyChar()
        return succ
    }
    private fun notifyChar(){
        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).forEach { device->
            Log.d(TAG,"Notifying device: ${device?.name} ${device?.address}")

            bluetoothGattServer.notifyCharacteristicChanged(device, notifyCompleteCharacteristic,false)
        }

    }
}