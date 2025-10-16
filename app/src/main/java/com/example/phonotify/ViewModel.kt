package com.example.phonotify

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.phonotify.service.CommunicationService
import kotlinx.coroutines.launch

class ViewModel(application: Application): AndroidViewModel(application) {

    var main_switch_checked = mutableStateOf(false)

    fun startBLE(){
        Log.d("notListener","Sent START for notification")
        Intent(application, CommunicationService::class.java).also{
            it.action = CommunicationService.START
            application.startService(it)
        }
    }
    fun stopBLE(){
        Log.d("notListener","Sent STOP for notification")
        Intent(application, CommunicationService::class.java).also{
            it.action = CommunicationService.STOP
            application.startService(it)
        }
    }
    fun resendRecentNotification(){
        Intent(application, CommunicationService::class.java).also{
            it.action = CommunicationService.RESEND
            application.startService(it)
        }
    }
    init {
        viewModelScope.launch{
            ViewModelData.notyData.collect{
                ViewModelData.setNotification(it)
            }
        }

    }
}