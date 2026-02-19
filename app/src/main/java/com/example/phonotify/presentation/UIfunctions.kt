package com.example.phonotify.presentation

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp


@Composable
fun MyHorizontalDivider(){
    HorizontalDivider(thickness = 5.dp,color = MaterialTheme.colorScheme.inversePrimary)
}
