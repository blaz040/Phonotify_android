package com.example.phonotify.presentation.Screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.phonotify.presentation.MainViewModel
import com.example.phonotify.ViewModelData
import com.example.phonotify.service.NotificationData

@SuppressLint("MissingPermission")
@Composable
fun MainScreen(vm: MainViewModel){

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Row (verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val checked = vm.main_switch_checked.observeAsState(false).value
            Text("Turn ON: ")
            Switch(
                checked = checked,
                onCheckedChange = {
                    when(checked){
                        false -> vm.startBLE()
                        true -> vm.stopBLE()
                    }
                }
            )
        }
        val advertising = ViewModelData.advertising.observeAsState().value
        Text("Advertising: $advertising")

        Row(Modifier.padding(10.dp)){
            Column() {
                Text("Recent Notification: ")
                TextButton({vm.resendRecentNotification()}){
                    Text("Resend Recent Notification")
                }
            }
            Card(modifier = Modifier.sizeIn(300.dp,100.dp,500.dp,200.dp)){Box(Modifier.padding(10.dp)){
                val nData = ViewModelData.liveNotData.observeAsState(NotificationData("Null","null","null")).value
                val sendStatus = ViewModelData.sendStatus.observeAsState(false).value
                Column(Modifier.padding(10.dp)) {
                    Text("package: ${nData.pckg}\n")
                    Text("Title: ${nData.title}\n")
                    Text("Context: ${nData.text}\n")

                    when(sendStatus) {
                        true -> Text("Send: Success")
                        false-> Text("Sent: Failed")
                    }
                }
            }}
        }
        Row(Modifier.padding(10.dp)){
            Text("Connected devices:")
            Card(modifier = Modifier.sizeIn(300.dp,100.dp,500.dp,200.dp).padding(0.dp,5.dp),){
                Box(Modifier.padding(10.dp)){
                    val connectedDevices = ViewModelData.connectedDevices.observeAsState(mutableListOf()).value
                    connectedDevices.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("${it.name}: ${it.address}")
                            TextButton({vm.disconnectDevice(it)}) { Text("X",color = Color.White)}
                        }
                    }
                }
            }
        }
    }

}