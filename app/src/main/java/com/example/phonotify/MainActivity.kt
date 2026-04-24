package com.example.phonotify

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.example.phonotify.presentation.Navigation
import com.example.phonotify.ui.theme.NotificationSharringTheme

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter


    private var interacted = true // used so that there arent multiple BLE_notification windows

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mReceiver,filter)

        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (isNotificationListenerEnabled(this) == false) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        enableEdgeToEdge()
        setContent {
            NotificationSharringTheme {
                Navigation()
            }
        }
    }
    override fun onDestroy(){
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }
    override fun onStart() {
        super.onStart()
        requestBLEPermission()
        showBluetoothDialog()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
    private fun showLocationDialog() {
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){
        }
    fun requestPermission(permission:String){
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }.launch(permission)
    }
    fun requestBLEPermission() = requestBluetoothPermission.launch(
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH
        )
    )

    val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            showLocationDialog()
        }

    val requestBluetoothIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
            interacted = true
            if(result.resultCode != RESULT_OK) {
                showBluetoothDialog()
            }
        }
    private fun showBluetoothDialog() {
        if(bluetoothAdapter.isEnabled == false && interacted) {
            interacted = false
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetoothIntent.launch(enableBluetoothIntent)
        }
    }

    private val mReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> showBluetoothDialog()
            }
        }
    }
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return packageNames.contains(context.packageName)
    }
}