package com.example.phonotify.presentation.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phonotify.presentation.LogsViewModel
import com.example.phonotify.presentation.MainViewModel
import com.example.phonotify.presentation.MyHorizontalDivider

@Composable
fun LogScreen(vm: LogsViewModel = viewModel()){
    Column(Modifier.fillMaxSize()) {
        DisplayHeader("Logs"){
            Button({vm.clearLogs()}){
                Text("Clear Logs")
            }
        }
        val logs = vm.logs.observeAsState("").value

        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()
        Box(
            modifier = Modifier.width(2000.dp)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        ) {
            Text(logs)
        }
        /*
        val fileList = ViewModelData.fileList.observeAsState().value
        val selectedFile = remember{ mutableStateOf(FileData("null", "null")) }

        var first = true
        fileList?.forEach{ file_data->
            if(!first){
                MyHorizontalDivider()
            }
            first = false

            ShowFile(
                Modifier.clickable { selectedFile.value = file_data },
                file_data,
                (selectedFile.value.name == file_data.name),
                onLoadClick = {
                    navController.navigate(Routes.AnalyzeScreen)
                    vm.loadFile(file_data.name)
                },
                onDeleteClick = {
                    SnackbarManager.send("Deleted file ${file_data.name}")
                    vm.deleteFile(file_data.name)
                },
            )
        }
         */
    }
}
@Composable
fun DisplayHeader(title:String = "Saved Recordings", callback: @Composable ColumnScope.()->Unit){
    Box(
        Modifier.fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,

    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Text(title, fontSize = 40.sp, color = MaterialTheme.colorScheme.onPrimary)
            callback()
        }
    }
    MyHorizontalDivider()
}
