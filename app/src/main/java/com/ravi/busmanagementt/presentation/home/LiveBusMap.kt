package com.ravi.busmanagementt.presentation.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravi.busmanagementt.utils.bitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.presentation.components.CameraAnimateFaB
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


@Composable
fun LiveBusMap(
    modifier: Modifier = Modifier,
    mapState: MapState,
    busMarkerIcon: BitmapDescriptor? = null,
    isMapExpanded: Boolean = false,
    initialMarkerPoint: LatLng?,
    userLocation: LatLng?,
    stopLocation: LatLng? = null,
    liveLocationPoints: List<LatLng>? = null,
    allBusesLiveLocations: Map<String, List<RealtimeLocation>>? = null,
    animateToBus: String? = null,
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

            // Markers - For Admin only
            allBusesLiveLocations?.let { buses ->
                buses.forEach { (busId, liveLocations) ->
                    liveLocations.let { location ->

                        if (location.isNotEmpty()) {
                            val firstLocation = location.first()
                            val latLng = LatLng(firstLocation.latitude, firstLocation.longitude)
                            Marker(
                                state = rememberUpdatedMarkerState(latLng),
                                title = "${busId} Start"
                            )

                            if (location.size > 1) {
                                val lastLocation = location.last()
                                val latLng2 = LatLng(lastLocation.latitude, lastLocation.longitude)
                                if (animateToBus != null && (animateToBus == busId)) {
                                    animateBusLatLng = latLng2
                                }
                                Marker(
                                    state = rememberUpdatedMarkerState(latLng2),
                                    title = busId,
                                    icon = busMarkerIcon,
                                    anchor = Offset(0.5f, 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Paths - For Admin only
            allBusesLiveLocations?.let { buses ->
                buses.forEach { (busId, liveLocations) ->
                    val points = liveLocations.map { LatLng(it.latitude, it.longitude) }
                    Polyline(
                        points = points,
                        color = AppColors.Primary,
                        width = 16f,
                        startCap = ButtCap()
                    )
                }

            }


            // Bus Marker: Source and Current Location (Parent and Driver)
            liveLocationPoints?.let { points ->

                if (points.isNotEmpty()) {
                    // Bus Starting Point Marker
                    Marker(
                        state = rememberUpdatedMarkerState(points.first()),
                        title = "Start"
                    )

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

            // Bus live Path
            liveLocationPoints?.let {
                if (it.size > 1)
                    Polyline(
                        points = it,
                        color = AppColors.Primary,
                        width = 16f,
                        startCap = ButtCap()
                    )
            }

            // Parent's bus stop Marker
            stopLocation?.let {
                Marker(
                    state = rememberUpdatedMarkerState(it),
                    title = "Your stop"
                )
            }

        }

        // My Location
        if (isMapExpanded) {
            CameraAnimateFaB(
                Modifier.align(Alignment.BottomEnd),
                onBusLocationClick = {
                    if (liveLocationPoints.isNullOrEmpty()) {
                        context.showToast("No Bus location found")
                        return@CameraAnimateFaB
                    } else {
                        liveLocationPoints.last().let {
                            mapState.animateCamera(it)
                            context.showToast("Bus Location")
                        }
                    }

                },
                onMyLocationClick = {
                    if (userLocation == null) {
                        context.showToast("No location found")
                    } else
                        userLocation.let {
                            mapState.animateCamera(it)
                        }
                }
            )
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

@Preview(showBackground = true, showSystemUi = true)
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


// todo: Test custom compose marker - remove it not in use
@Composable
fun CustomMarker() {

    val infiniteTransition = rememberInfiniteTransition()
    val animateCircle by infiniteTransition.animateFloat(
        initialValue = .2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer {
                scaleX = animateCircle
                scaleY = animateCircle
            }
            .clip(CircleShape)
            .background(Color.Magenta)

    )

}