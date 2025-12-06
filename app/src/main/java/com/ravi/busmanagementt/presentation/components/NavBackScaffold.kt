package com.ravi.busmanagementt.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ravi.busmanagementt.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBackScaffold(
    barTitle: String = "Go Back",
    onBackClick: () -> Unit = {},
    fabIcon: ImageVector? = null,
    onFabClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(barTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "go back",
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            fabIcon?.let { icon ->
                FloatingActionButton(onClick = onFabClick, containerColor = AppColors.Primary) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        content(paddingValues)
    }
}