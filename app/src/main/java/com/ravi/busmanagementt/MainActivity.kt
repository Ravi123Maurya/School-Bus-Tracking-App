package com.ravi.busmanagementt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravi.busmanagementt.presentation.navigation.NavGraph
import com.ravi.busmanagementt.presentation.viewmodels.MapViewModel
import com.ravi.busmanagementt.presentation.viewmodels.PortalViewModel

import com.ravi.busmanagementt.ui.theme.BusManagementTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val portalViewModel: PortalViewModel by viewModels()
        val mapViewModel: MapViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            BusManagementTheme {
                NavGraph(mapViewModel, portalViewModel)
            }
        }
    }
}