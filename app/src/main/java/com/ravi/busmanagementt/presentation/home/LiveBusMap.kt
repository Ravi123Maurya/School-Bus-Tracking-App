package com.ravi.busmanagementt.presentation.home

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.presentation.components.CameraAnimateFaB
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


@Composable
fun LiveBusMap(
    modifier: Modifier = Modifier,
    isAdminPortal: Boolean = false,
    mapState: MapState,
    busMarkerIcon: BitmapDescriptor? = null,
    isMapExpanded: Boolean = false,
    initialMarkerPoint: LatLng?,
    userLocation: LatLng?,
    stopLocation: LatLng? = null,
    liveLocationPoints: List<LatLng>? = null,
    busRouteLatLng: List<LatLng>? = null,
    busStops: List<BusStop>? = null,
    allBusesLiveLocations: Map<String, List<RealtimeLocation>>? = null,
    allBusesRoutesLatLng: Map<String, List<LatLng>>? = null,
    allBusesStopsLatLng: Map<String, List<BusStop>>? = null,
    focusToBus: String? = null,
    onExpandClick: () -> Unit,
    onMapLoaded: () -> Unit = {}
) {

    val context = LocalContext.current

    val mapUiSettings =
        remember {
            MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            )
        }
    val PATTERN_DASHED = remember { listOf(Dash(30f), Gap(20f), Dot(), Gap(20f)) }
    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
    val customModifier =
        if (isMapExpanded) modifier.fillMaxSize() else modifier
            .fillMaxWidth()
            .height(300.dp)


    /// Animate Bus Marker
    val busMarkerState =
        remember { MarkerState(liveLocationPoints?.lastOrNull() ?: LatLng(0.0, 0.0)) }

    LaunchedEffect(liveLocationPoints?.lastOrNull()) {
        val newPosition = liveLocationPoints?.lastOrNull()
        if (newPosition != null) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000)
            ) { value, _ ->
                val lat =
                    (busMarkerState.position.latitude * (1 - value)) + (newPosition.latitude * value)
                val lng =
                    (busMarkerState.position.longitude * (1 - value)) + (newPosition.longitude * value)
                busMarkerState.position = LatLng(lat, lng)
            }
        }

    }

    LaunchedEffect(initialMarkerPoint) {
        if (initialMarkerPoint != null && !mapState.isMapInitialized) {
            mapState.animateCamera(initialMarkerPoint)
            mapState.isMapInitialized = true
        }
    }

    var animateBusLatLng by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(animateBusLatLng) {
        animateBusLatLng?.let {
            mapState.animateCamera(it, zoom = 24f)
        }
    }

    var selectedBusId by remember { mutableStateOf<String?>(null) }
    var selectedBusLatLng by remember { mutableStateOf<LatLng?>(null) }
    val zoomLevel = mapState.cameraPositionState.position.zoom
    val showDetails = zoomLevel > 12f

    LaunchedEffect(allBusesLiveLocations, focusToBus) {
        if (focusToBus != null && allBusesLiveLocations != null) {
            val targetBus = allBusesLiveLocations[focusToBus]
            if (!targetBus.isNullOrEmpty()) {
                val lastLocation = targetBus.last()
                animateBusLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            }
        }
        if (selectedBusId == null && !allBusesLiveLocations.isNullOrEmpty()) {
            // Pick the first busID from the map keys
            val firstBusId = allBusesLiveLocations.keys.firstOrNull()
            if (firstBusId != null) {
                selectedBusId = firstBusId
            }
        }
    }
    LaunchedEffect(selectedBusId, allBusesLiveLocations) {
        if (selectedBusId != null && allBusesLiveLocations != null) {
            val locations = allBusesLiveLocations[selectedBusId]
            if (!locations.isNullOrEmpty()) {
                val last = locations.last()
                selectedBusLatLng = LatLng(last.latitude, last.longitude)
            }
        }
    }
    LaunchedEffect(selectedBusLatLng) {
        selectedBusLatLng?.let { mapState.animateCamera(it, 12f) }
    }

    Box(
        modifier = Modifier
            .animateContentSize()
            .then(customModifier)
    ) {
        GoogleMap(
            cameraPositionState = mapState.cameraPositionState,
            modifier = Modifier.fillMaxSize(),
            uiSettings = mapUiSettings,
            properties = mapProperties,
            onMapLoaded = {
                onMapLoaded()
            }
        ) {


            // Bus Marker: Source and Current Location (Parent and Driver)
            liveLocationPoints?.let { points ->

                if (points.isNotEmpty()) {
                    // Bus Starting Point Marker
                    MarkerComposable(
                        state = rememberUpdatedMarkerState(points.first()),
                        title = "Start"
                    ) {
                        BusStartLocationMarker(1)
                    }

                    // Bus Current Location Marker
                    if (points.size > 1) {
                        Marker(
                            state = busMarkerState,
                            title = "Your Bus",
                            icon = busMarkerIcon,
                            anchor = Offset(0.5f, 0.5f)
                        )
                    }
                }
            }

            // Bus live Path (Parent - Driver)
            liveLocationPoints?.let {
                if (it.size > 1)
                    Polyline(
                        points = it,
                        color = AppColors.Primary,
                        width = 16f,
                        startCap = ButtCap()
                    )
            }

            // Bus Route Path (Parent and Driver)
            busRouteLatLng?.let { it ->
                Polyline(
                    points = it,
                    color = Color.Red.copy(alpha = 0.5f),
                    width = 16f,
                    startCap = ButtCap(),
//                        pattern = PATTERN_DASHED,
                    jointType = JointType.ROUND
                )
                Polyline(
                    points = it,
                    color = Color.White,
                    width = 6f,
                    startCap = ButtCap(),
                    pattern = PATTERN_DASHED,
                    jointType = JointType.ROUND
                )
            }

            // Bus Stop Markers (Parent and Driver)
            busStops?.let { busStops ->

                busStops.forEachIndexed { i, point ->
                    val latLng = LatLng(point.geoPoint.latitude, point.geoPoint.longitude)
                    val stopName = point.stopName
                    MarkerComposable(
                        state = rememberUpdatedMarkerState(latLng),
                        title = "Stop ${i + 1}",
                        snippet = stopName
                    ) {
                        BusStopLocationsMarker()
                    }
                }

            }

            // Parent's bus stop Marker
            stopLocation?.let {
                MarkerComposable(
                    state = rememberUpdatedMarkerState(it),
                    title = "Your Stop"
                ) {
                    ParentStopLocationMarker()
                }
            }


            /**
             * --------- ADMIN --------
             **/
            // All Buses Markers - For Admin only
            allBusesLiveLocations?.let { buses ->
                var index = 0
                buses.forEach { (busId, liveLocations) ->
                    index++
                    if (busId == selectedBusId) {
                        liveLocations.let { location ->
                            if (location.isNotEmpty()) {
                                val firstLocation = location.first()
                                val latLng = LatLng(firstLocation.latitude, firstLocation.longitude)

                                MarkerComposable(
                                    state = rememberUpdatedMarkerState(latLng),
                                    title = "$busId",
                                    snippet = "Starting point"
                                ) {
                                    BusStartLocationMarker(index)
                                }


                                if (location.size > 1) {
                                    val lastLocation = location.last()
                                    val latLng2 =
                                        LatLng(lastLocation.latitude, lastLocation.longitude)
                                    selectedBusLatLng = latLng2
                                    Marker(
                                        state = rememberUpdatedMarkerState(latLng2),
                                        title = busId,
                                        icon = busMarkerIcon,
                                        anchor = Offset(0.5f, 0.5f),
                                        snippet = "Current location",
                                        onClick = {
                                            selectedBusId = busId
                                            false
                                        }
                                    )

                                }
                            }
                        }
                    }

                }
            }

            // All Buses Live Paths - For Admin only
            if (selectedBusId != null) {
                allBusesLiveLocations?.let { buses ->
                    buses.forEach { (busId, liveLocations) ->
                        if (busId == selectedBusId) {
                            val points = liveLocations.map { LatLng(it.latitude, it.longitude) }
                            Polyline(
                                points = points,
                                color = AppColors.Primary,
                                width = 16f,
                                startCap = ButtCap()
                            )
                        }

                    }
                }
            }

            // Buses Routes Path (Admin)
            allBusesRoutesLatLng?.forEach { (busId, busRoute) ->
                if (busId == selectedBusId) {
                    Polyline(
                        points = busRoute,
                        color = Color.Red.copy(alpha = 0.5f),
                        width = 16f,
                        startCap = ButtCap(),
//                        pattern = PATTERN_DASHED,
                        jointType = JointType.ROUND
                    )
                    Polyline(
                        points = busRoute,
                        color = Color.White,
                        width = 6f,
                        startCap = ButtCap(),
                        pattern = PATTERN_DASHED,
                        jointType = JointType.ROUND
                    )
                }

            }

            // Buses Stops (Admin)
            allBusesStopsLatLng?.let { busesStopsMap ->
                busesStopsMap.forEach { (busId, stops) ->
                    if (busId == selectedBusId) {
                        if (stops.isNotEmpty()) {
                            stops.forEachIndexed { i, point ->
                                val latLng =
                                    LatLng(point.geoPoint.latitude, point.geoPoint.longitude)
                                MarkerComposable(
                                    state = rememberUpdatedMarkerState(latLng),
                                    title = "Stop ${i + 1}",
                                    snippet = point.stopName
                                ) {
                                    BusStopLocationsMarker()
                                }
                            }
                        }
                    }

                }
            }


        }


        // My Location
        if (isMapExpanded) {
            CameraAnimateFaB(
                Modifier.align(Alignment.BottomEnd),
                onBusLocationClick = if (!isAdminPortal) {
                    {
                        if (liveLocationPoints.isNullOrEmpty()) {
                            context.showToast("No Bus location found")
                            return@CameraAnimateFaB
                        } else {
                            liveLocationPoints.last().let {
                                mapState.animateCamera(it)
                                context.showToast("Bus Location")
                            }
                        }
                    }
                } else null,
                onMyLocationClick = {
                    if (userLocation == null) {
                        context.showToast("No location found")
                    } else
                        userLocation.let {
                            mapState.animateCamera(it)
                        }
                }
            )

            if (isAdminPortal) {

                var selectedBus: String? = null

                try {
                    selectedBus =
                        if (selectedBusId == null) allBusesRoutesLatLng?.keys?.toList()?.first()
                            ?: "Select Bus" else selectedBusId
                    selectedBusLatLng = allBusesRoutesLatLng?.values?.toList()?.first()?.first()
                } catch (e: Exception) {

                }
                BusesList(
                    modifier = Modifier.align(Alignment.TopEnd),
                    selectedBusId = selectedBus ?: "Select Bus",
                    listOfBuses = allBusesRoutesLatLng?.keys?.toList() ?: emptyList(),
                    onBusClick = { busId ->
                        selectedBusId = busId
                    }
                )
            }


        } else {

            Box(
                Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                MapExpandButton { onExpandClick() }
            }

        }
    }
}


@Composable
private fun MapExpandButton(onExpandClick: () -> Unit = {}) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        )
    ) {
        IconButton(
            onClick = onExpandClick,
            shape = CircleShape,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "",
                tint = Color.Gray
            )
        }
    }
}


@Composable
private fun ParentStopLocationMarker(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE91E63),
) {

    Box(
        modifier = modifier
            .size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = modifier
                .size(40.dp)
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(x = width / 2, y = height / 2)

            // Outer Ring
            drawCircle(
                color = color.copy(alpha = .2f),
                radius = width / 2f,
                center = center
            )

            // Inner Ring
            drawCircle(
                color = color.copy(alpha = .6f),
                radius = width / 2.5f,
                center = center
            )

            // Inner White Circle
            drawCircle(
                color = Color.White,
                radius = width / 4f,
                center = center
            )
        }

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }


}


/******
 * Live Bus Start Marker (All)
 * @param(busCount) - Bus Count
 * *******/
@Composable
private fun BusStartLocationMarker(busCount: Int) {

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(24.dp)
        ) {

            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer White Circle
            drawCircle(
                color = Color.White,
                radius = size.width / 2f,
                center = center
            )

            // Inner Red Circle
            drawCircle(
                color = Color.Red,
                radius = size.width / 2.5f,
                center = center
            )
        }

        Text(
            text = busCount.toString(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }

}


@Composable
private fun BusStopLocationsMarker() {

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(1.dp, Color.Red, CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = Icons.Default.Stop,
            contentDescription = "",
            tint = Color.Gray
        )
    }

}


@Preview(showBackground = true)
@Composable
private fun BusesList(
    modifier: Modifier = Modifier,
    selectedBusId: String = "Bus_4001",
    listOfBuses: List<String> = emptyList(),
    onBusClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .animateContentSize()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White),
        colors = CardDefaults.cardColors(
            containerColor = if (!isExpanded) Color.White.copy(alpha = 0.7f) else Color.White
        )
    ) {

        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .clickable {
                        if (listOfBuses.isNotEmpty()) {
                            isExpanded = true
                        } else {
                            context.showToast("Loading...")
                        }
                    }
                    .padding(8.dp)
            ) {
                Text(selectedBusId)
            }
        } else {
            LazyColumn {
                itemsIndexed(listOfBuses, key = { i, bus -> "$bus-$i" }) { i, bus ->
                    Box(
                        modifier = Modifier
                            .clickable {
                                onBusClick(bus)
                                isExpanded = false
                            }
                            .padding(8.dp)
                    ) {
                        Text("${i + 1}. $bus")
                    }
                }
            }
        }
    }
}

