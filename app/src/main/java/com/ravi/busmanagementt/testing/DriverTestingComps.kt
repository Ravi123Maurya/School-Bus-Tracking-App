package com.ravi.busmanagementt.testing

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng

@Composable
fun DriverTestingButton(
    modifier: Modifier = Modifier,
    routePoints: List<LatLng>,
    driverTestingViewModel: DriverTestingViewModel = hiltViewModel()
) {

    val state by driverTestingViewModel.state.collectAsStateWithLifecycle()

    Button(
        onClick = {
            if(state.isSimulating){
                driverTestingViewModel.onAction(DriverTestingAction.OnStopTrip(busId = "bus_test_2"))
            }else{
                driverTestingViewModel.onAction(
                    DriverTestingAction.OnStartTrip(
                        busId = "bus_test_2",
                        route = routePoints,
                        speedKmph = 120.0,
                        timeIntervalMillis = 2000L
                    ))
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if(state.isSimulating) Color.Red else Color.Green
        )
    ) {
        Text(
            text = if(!state.isSimulating) "Start Trip" else "Stop Trip",
        )
    }

}