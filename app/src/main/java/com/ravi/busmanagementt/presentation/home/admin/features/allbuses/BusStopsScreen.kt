package com.ravi.busmanagementt.presentation.home.admin.features.allbuses

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.home.BusStopPoints
import com.ravi.busmanagementt.presentation.viewmodels.MapViewModel
import com.ravi.busmanagementt.ui.theme.AppColors

@Composable
fun BusStopsScreen(
    navController: NavController,
    mapViewModel: MapViewModel,
    busId: String? = null
) {

    val busStopsState by mapViewModel.busStopsState.collectAsStateWithLifecycle()
    val realtimeLocations by mapViewModel.realtimeLocationState.collectAsStateWithLifecycle()


    LaunchedEffect(busId) {
        busId?.let {
            mapViewModel.getBusStopsByBusId(it)
            mapViewModel.getLocationUpdates(it)
        }
    }

    BusStopsContent(
        screenTitle = if (busId != null) "$busId Stops" else "Bus Stops"  ,
        busStops = busStopsState ?: emptyList(),
        realtimeLocation = realtimeLocations,
        onNavBackClick = { navController.popBackStack() }
    )

}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BusStopsContent(
    screenTitle: String,
    busStops: List<com.ravi.busmanagementt.domain.model.BusStop> = emptyList(),
    realtimeLocation: List<RealtimeLocation>? = null,
    onNavBackClick: () -> Unit = {}
) {

    NavBackScaffold(
        barTitle = screenTitle,
        onBackClick = onNavBackClick
    ) {

        if (busStops.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Bus Stops Found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                item {
                    val totalStopsReached = if (realtimeLocation.isNullOrEmpty()) 0 else realtimeLocation.last().numberOfStopsReached
                    BusStopInfoSection(
                        totalStops = busStops.size,
                        reachedStops = totalStopsReached
                    )
                }

                item{
                    BusStopPoints(
                        modifier = Modifier.padding(24.dp),
                        stops = busStops,
                        realtimeLocation = realtimeLocation
                    )
                }

            }
        }

    }

}

@Composable
private fun BusStopInfoSection(
    totalStops: Int,
    reachedStops: Int,
){
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Primary.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total Stops",
                value = totalStops.toString(),
                icon = Icons.Default.Route
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                modifier = Modifier.weight(1f),
                label = "Reached",
                value = reachedStops.toString(),
                icon = Icons.Default.CheckCircle,
                valueColor = Color(0xFF4CAF50)
            )
        }
    }
}