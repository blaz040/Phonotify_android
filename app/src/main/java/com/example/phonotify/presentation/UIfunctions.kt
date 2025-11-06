package com.example.phonotify.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
fun MyHorizontalDivider(){
    HorizontalDivider(thickness = 5.dp,color = MaterialTheme.colorScheme.inversePrimary)
}