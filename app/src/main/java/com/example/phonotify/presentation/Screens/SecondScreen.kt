package com.example.phonotify.presentation.Screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.phonotify.datastore.ApplicationData
import com.example.phonotify.datastore.ApplicationUi
import com.example.phonotify.presentation.ViewModels.SecondScreenViewModel

@Composable
fun SecondScreen(vm: SecondScreenViewModel){
    //val appList = vm.installedApplications.observeAsState().value!!
    val enabledAppList = vm.enabledApplications.observeAsState().value
    val disabledAppList = vm.disabledApplications.observeAsState().value


    Column{
        if(enabledAppList == null){
            Text("Enabled is null")
        }
        else if(disabledAppList == null){
            Text("Disabled is null")
        }
        else{
        AppSearchBar(
            vm.searchQuery.value,
            { vm.onSearchQueryChange(it) },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
//            items(items = enabledAppList){ app ->
//                AppItem(app, vm)
//            }
//        }
            // SECTION: ENABLED APPS
            if (enabledAppList.isNotEmpty()) {
                item {
                    SectionHeader("Enabled Apps")
                }
                items(items = enabledAppList, key = { it.packageName }) { app ->
                    AppItem(app, vm)
                }
            }

            // Spacer between sections
            item { Spacer(modifier = Modifier.size(16.dp)) }

            // SECTION: DISABLED APPS
            if (disabledAppList.isNotEmpty()) {
                item {
                    SectionHeader("Disabled Apps")
                }
                items(items = disabledAppList, key = { it.packageName }) { app ->
                    AppItem(app, vm)
                }
            }

            // Handle Empty State
            if (enabledAppList.isEmpty() && disabledAppList.isEmpty()) {
                item {
                    Text(
                        "No apps found",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    }

}
@Composable
fun AppItem(app: ApplicationUi, vm: SecondScreenViewModel) {
    val checked = remember { mutableStateOf(app.allowNotifications) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp // Gives a subtle "card" look
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Far Left: Space reserved for App Icon
            AsyncImage(
                model = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Middle: App Name (Takes up remaining space)
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f) // This is the secret sauce!
            )

            // 3. Far Right: The Switch
            Switch(
                checked = checked.value,
                onCheckedChange = {
                    checked.value = !checked.value
                    app.allowNotifications = checked.value
                    vm.updateApp(app)
                },

            )
        }
    }
}
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search apps...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
    )
}
