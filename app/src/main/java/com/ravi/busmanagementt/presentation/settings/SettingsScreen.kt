package com.ravi.busmanagementt.presentation.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.viewmodels.BusViewModel
import com.ravi.busmanagementt.presentation.viewmodels.PortalViewModel
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.bus1StopsList


// todo : Save stopName and location as well in firestore (parent Doc)
@Composable
fun SettingsScreen(navController: NavController, busViewModel: BusViewModel = hiltViewModel()) {

    val portalViewModel: PortalViewModel = hiltViewModel()
    val parentSavedStopLocation by portalViewModel.stopLocation.collectAsStateWithLifecycle()
    val busStops by busViewModel.busStops.collectAsStateWithLifecycle()


    SettingsScreenContent(
        stopLocation = parentSavedStopLocation,
        busStops = busStops,
        onBackClick = { navController.popBackStack() },
        onSetStopClick = { buStop ->
                val latLng = LatLng(buStop.geoPoint.latitude, buStop.geoPoint.longitude)
                portalViewModel.setStopLocationToFireStore(buStop)
                portalViewModel.setStopLocation(latLng)
        }
    )
}


@Composable
private fun SettingsScreenContent(
    stopLocation: LatLng? = null,
    busStops: List<BusStop> = emptyList(),
    onBackClick: () -> Unit = {},
    onSetStopClick: (BusStop) -> Unit
) {

    val context = LocalContext.current

    NavBackScaffold(
        barTitle = "Settings",
        onBackClick = onBackClick
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (busStops.isEmpty()){
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                        Text("No stops found for your bus.")
                    }
                }
            }else{
                item {
                    Spacer(Modifier.height(24.dp))
                    HeadingText()
                    Spacer(Modifier.height(24.dp))
                }

                items(busStops) { item ->
                    val latLng = LatLng(item.geoPoint.latitude, item.geoPoint.longitude)
                    val hasStopAlreadySet = stopLocation == latLng
                    BusStopItem(
                        hasStopAlreadySet = hasStopAlreadySet,
                        headline = item.stopName, address = item.location
                    ) {
                        onSetStopClick(item)
                        Toast.makeText(context, "Your stop point has been set.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            }


        }
    }
}

@Composable
private fun HeadingText() {
    Text("Choose your bus stop location", fontSize = 20.sp, fontWeight = FontWeight.Medium)
}


@Preview
@Composable
private fun BusStopItem(
    hasStopAlreadySet: Boolean = true,
    headline: String = "",
    address: String = "",
    onSetStopClick: () -> Unit = {}
) {
    ListItem(
        leadingContent = {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.Default.LocationOn,
                contentDescription = "",
                tint = AppColors.Primary
            )
        },
        headlineContent = { Text(headline) },
        supportingContent = { Text(address) },
        trailingContent = {
            OutlinedButton(
                modifier = Modifier.animateContentSize(),
                onClick = onSetStopClick,
                border = if (hasStopAlreadySet) null else ButtonDefaults.outlinedButtonBorder(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (hasStopAlreadySet) Color.Green else Color.White
                )
            ) {
                Text(text = if (hasStopAlreadySet) "Your Stop" else "Set Stop")
            }
        },

        )
}




