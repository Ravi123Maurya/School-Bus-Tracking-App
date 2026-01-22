package com.ravi.busmanagementt

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.ravi.busmanagementt.presentation.navigation.NavGraph
import com.ravi.busmanagementt.presentation.viewmodels.MapViewModel
import com.ravi.busmanagementt.ui.theme.BusManagementTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val mapViewModel: MapViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            BusManagementTheme {
                NavGraph(mapViewModel)
            }
        }
    }
}