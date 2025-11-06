package com.example.phonotify.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ble_con.Snackbar.SnackbarManager
import com.example.phonotify.presentation.Screens.LogScreen
import com.example.phonotify.presentation.Screens.MainScreen
import com.example.phonotify.presentation.Screens.SecondScreen
import com.example.phonotify.repository.Routes
import timber.log.Timber


enum class Destination(
    val route: String,
    val icon: ImageVector,
    val contentDescription: String,
    val label: String) {

    MainScreen(Routes.MainScreen, Icons.Default.Home,"Home Screen","Home"),
    SecondScreen(Routes.SecondScreen,Icons.Default.Settings,"Second Screen","Screen 2"),
    LogScreen(Routes.LogScreen, Icons.Default.Info, "Log","Log")

}
@Composable
fun Navigation(
    vm: MainViewModel = viewModel()
) {
    val navController = rememberNavController()

    //======================== Snackbar ===================================
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(true) {
        SnackbarManager.events.collect{ event->
            Timber.d("Got message : ${event.message}")
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                withDismissAction = true,
                actionLabel = event.action?.label,
                duration = event.duration
            )
            if(result == SnackbarResult.ActionPerformed){
                event.action!!.callback()
            }
        }
    }
    //========================== Scaffold =================================
    var selectedDestination = rememberSaveable { mutableIntStateOf(Destination.MainScreen.ordinal) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {

            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination.value == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination.value = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }

        }
    ) { contentPadding ->
        NavHost(
            navController = navController, startDestination = Routes.MainScreen,
            modifier = Modifier.padding(contentPadding)
        )
        {
            composable(Routes.MainScreen) {
                MainScreen(vm)
            }
            composable(Routes.SecondScreen) {
                SecondScreen(vm)
            }
            composable(Routes.LogScreen){
                LogScreen()
            }
        }
    }
}
