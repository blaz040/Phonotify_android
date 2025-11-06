package com.example.phonotify.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble_con.fileManager.FileManager
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.launch

class LogsViewModel(application: Application): AndroidViewModel(application) {
    val myFileManager = FileManager(application)

    private val _logs = MutableLiveData<String>("")
    val logs: LiveData<String> = _logs

    fun clearLogs(){
        myFileManager.clearLogs()
    }
    init {
        _logs.postValue(myFileManager.getLogs())
        viewModelScope.launch {
            ViewModelData.newLogs.collect{
                //Log.d("TIMBER","collected  $it")
                if(it == true){
                    _logs.postValue(myFileManager.getLogs())
                }
            }
        }
    }
}