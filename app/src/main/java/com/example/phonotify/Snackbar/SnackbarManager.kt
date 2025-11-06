package com.example.ble_con.Snackbar

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

object SnackbarManager {

    data class SnackbarEvent(
        val message:String,
        val action: SnackbarAction?,
        val duration: SnackbarDuration
    )

    data class SnackbarAction(
        val label:String,
        val callback:()->Unit
    )

    private val TAG = "SnackbarManager"
    private val defaultDuration:SnackbarDuration = SnackbarDuration.Short

    private val _events: Channel<SnackbarEvent> = Channel<SnackbarEvent>()
    var events: Flow<SnackbarEvent> = _events.receiveAsFlow()

    val scope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Coroutine failed", e)
        })

    fun send(message: String,duration: SnackbarDuration = defaultDuration) {
        val event = SnackbarEvent(message,null,duration)
        scope.launch {
            _events.send(event)
        }
    }
    fun send(message: String,duration: SnackbarDuration = defaultDuration, label:String, callback: () -> Unit){
        val event = SnackbarEvent(message, SnackbarAction(label,callback),duration)
        scope.launch {
            _events.send(event)
        }
    }
}