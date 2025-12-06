package com.ravi.busmanagementt.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CircularLoading(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onPrimary) {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = color
    )
}