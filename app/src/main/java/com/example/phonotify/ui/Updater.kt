package com.example.phonotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phonotify.BuildConfig
import com.example.phonotify.ui.ViewModels.AppUpdateViewModel
import com.example.phonotify.ui.ViewModels.UpdateState

@Composable
fun UpdateScreen(
    viewModel: AppUpdateViewModel = viewModel()
) {
    val updateState by viewModel.updateState.observeAsState(UpdateState.Idle)
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateState.UpdateAvailable?>(null) }

    // Show dialog when update is available
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.UpdateAvailable) {
            pendingUpdate = updateState as UpdateState.UpdateAvailable
            showDialog = true
        }
    }

    if (showDialog && pendingUpdate != null) {
        UpdateDialog(
            version = pendingUpdate!!.version,
            onConfirm = {
                showDialog = false
                viewModel.downloadAndInstall(pendingUpdate!!.downloadUrl)
            },
            onDismiss = {
                showDialog = false
                viewModel.resetState()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UpdateButton(
            state = updateState,
            onClick = { viewModel.checkForUpdates(BuildConfig.VERSION_NAME) }
        )

        UpdateStatusText(state = updateState)
    }
}

@Composable
fun UpdateButton(
    state: UpdateState,
    onClick: () -> Unit
) {
    val isLoading = state is UpdateState.Checking || state is UpdateState.Downloading

    Button(
        onClick = onClick,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = when (state) {
                is UpdateState.Checking -> "Checking..."
                is UpdateState.Downloading -> "Downloading..."
                else -> "Check for updates"
            }
        )
    }
}

@Composable
fun UpdateStatusText(state: UpdateState) {
    val (text, color) = when (state) {
        is UpdateState.UpToDate -> "You're up to date!" to MaterialTheme.colorScheme.primary
        is UpdateState.Error -> state.message to MaterialTheme.colorScheme.error
        is UpdateState.Downloading -> "Downloading update..." to MaterialTheme.colorScheme.secondary
        else -> null to MaterialTheme.colorScheme.onBackground
    }

    text?.let {
        Text(
            text = it,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun UpdateDialog(
    version: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available") },
        text = { Text("Version $version is available. Would you like to update now?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}