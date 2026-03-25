package com.ravi.busmanagementt.presentation.home

import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.presentation.components.BusTopAppBar
import com.ravi.busmanagementt.presentation.components.PermissionHandler
import com.ravi.busmanagementt.presentation.components.StartStopButton
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.presentation.viewmodels.AuthViewModel
import com.ravi.busmanagementt.presentation.viewmodels.MapViewModel
import com.ravi.busmanagementt.presentation.viewmodels.PortalViewModel
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.MapUtils
import com.ravi.busmanagementt.data.datastore.Portals
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.ravi.busmanagementt.data.serivce.LocationService
import com.ravi.busmanagementt.data.serivce.LocationSharingState
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.presentation.components.AlertDialogBus
import com.ravi.busmanagementt.presentation.components.InternetConnectionAlertView
import com.ravi.busmanagementt.presentation.home.admin.AdminPortal
import com.ravi.busmanagementt.presentation.home.caretaker.CaretakerScreen
import com.ravi.busmanagementt.presentation.viewmodels.BusViewModel
import com.ravi.busmanagementt.utils.DistanceMatrix
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    newBusId: String? = null, // todo: animate camera to bus location
    mapViewModel: MapViewModel,
    authViewModel: AuthViewModel,
    portalViewModel: PortalViewModel,
    busViewModel: BusViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapViewState by mapViewModel.viewState.collectAsStateWithLifecycle()
    val realtimeLocation by mapViewModel.realtimeLocationState.collectAsStateWithLifecycle()
    val portal by portalViewModel.portal.collectAsStateWithLifecycle()
    val stopLocation by portalViewModel.stopLocation.collectAsStateWithLifecycle()
    val busId by busViewModel.busId.collectAsStateWithLifecycle()
    val busStops by busViewModel.busStops.collectAsStateWithLifecycle()
    val sharingLocationState by mapViewModel.sharingState.collectAsStateWithLifecycle()
    val hasInternetConnection by mapViewModel.hasInternetConnection.collectAsStateWithLifecycle()
    var isSharingButtonClick by remember { mutableStateOf(false) }
    var hasLogoutClick by remember { mutableStateOf(false) }
    val busEta by mapViewModel.eta.collectAsStateWithLifecycle()
    val remainingDistance by mapViewModel.remainingDistance.collectAsStateWithLifecycle()
    val liveLocationsPoints = remember(realtimeLocation) { realtimeLocation?.map {
        LatLng(
            it.latitude,
            it.longitude
        )
    } }

    LaunchedEffect(realtimeLocation) {
        Log.d("Location Sharing", "RealtimeLocation - ${realtimeLocation?.size}")
        Log.d("Location Sharing", "LiveLocation - ${liveLocationsPoints?.size}")
    }
    //Testing
    val busRoute by mapViewModel.busRouteLatLng.collectAsStateWithLifecycle()


    //////////////// -------- Map Content ---------- //////////////////////////////
    val mapContent = mapViewModel.mapContent
    val mapPlaceholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f) // Use an aspect ratio or fixed height
                .background(Color.LightGray.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading Map...")
        }
    }
    val currentMapContent = mapContent ?: mapPlaceholder
    ///////////////////////////////////////////////////////////////////////

    LaunchedEffect(portal) {
        mapViewModel.setIsAdmin(portal?.value == Portals.ADMIN.value)
    }
    LaunchedEffect(stopLocation) {
        mapViewModel.stopLocation.value = stopLocation
    }


    LaunchedEffect(busStops) {

        mapViewModel.busStops.value = busStops
    }
    LaunchedEffect(busId) {
        mapViewModel.busId.value = busId
        busId?.let {
            mapViewModel.getLocationUpdates(it)

        }
    }
    LaunchedEffect(newBusId) {
        Log.d("HomeScreen", "NavBusId: $newBusId")
        if (!newBusId.isNullOrEmpty()) {
            mapViewModel.navBusId.value = newBusId
            mapViewModel.toggleMapSize()
        }
    }


    PermissionHandler(
        MapUtils.permissionToRequest,
        onPermissionGranted = {

            HomeScreenContent(
                busRoute = busRoute, // Testing
                hasInternetConnection = hasInternetConnection,
                portal = portal?.value ?: "No Value",
                busId = busId,
                busEta = busEta, // Parent only
                remainingDistance = remainingDistance, // Parent only
                email = authViewModel.email ?: "No Email",
                sharingState = sharingLocationState,
                isMapExpanded = mapViewState.isMapExpanded,
                realtimeLocation = realtimeLocation,
                mapContent = currentMapContent,
                initialMarkerPoint = mapViewState.initialLocation,
                userLocation = mapViewState.userLocation,
                parentStopLocation = stopLocation,
                busStopPoints = busStops,
                liveLocationPoints = liveLocationsPoints,
                logoutClick = {
                    hasLogoutClick = true
                },
                onStartStopButtonClick = {
                    if (!hasInternetConnection && !isSharingButtonClick) {
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                        return@HomeScreenContent
                    }
                    isSharingButtonClick = !isSharingButtonClick
                },
                onMapExpandClick = { mapViewModel.toggleMapSize() },
                onSettingClick = {
                    navController.navigate(NavRoutes.PROFILE_SCREEN) {
                        restoreState = true
                    }
                },
                navController = navController
            )


        },
        onPermissionDenied = { requestPermissions ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = requestPermissions) {
                    Text("Grant Location Permissions")
                }
            }
        }
    )


    // Sharing Location Alert Dialog (Driver)
    if (isSharingButtonClick) {
        val isCurrentlySharing = sharingLocationState == LocationSharingState.SHARING
        AlertDialogBus(
            title = "Sharing Location",
            text = if (!isCurrentlySharing) "Are you sure you want to share your location?" else "Are you sure you want to stop sharing your location?",
            confirmButtonText = if (isCurrentlySharing) "Stop" else "Start",
            dismissButtonText = if (isCurrentlySharing) "No" else "Cancel",
            onDismiss = { isSharingButtonClick = false },
            onConfirm = {
                scope.launch {
//                    mapViewModel.toggleSharingLocationState()
                    Intent(context, LocationService::class.java).also {
                        it.action = if (isCurrentlySharing) {
                            LocationService.ACTION_STOP
                        } else {
                            LocationService.ACTION_START
                        }
                        context.startService(it)
                    }
                }
                isSharingButtonClick = false
            }
        )
    }

    // Logout Alert Dialog (All)
    if (hasLogoutClick) {
        AlertDialogBus(
            title = "Logout",
            text = "Are you sure you want to logout?",
            onDismiss = { hasLogoutClick = false },
            onConfirm = {
                authViewModel.logout()
                hasLogoutClick = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeScreenContent(
    busRoute: List<LatLng>, // Testing
    hasInternetConnection: Boolean,
    portal: String,
    busId: String? = null,
    busEta: String,
    remainingDistance: String,
    email: String,
    sharingState: LocationSharingState,
    isMapExpanded: Boolean,
    realtimeLocation: List<RealtimeLocation>?,
    mapContent: @Composable () -> Unit,
    initialMarkerPoint: LatLng?,
    userLocation: LatLng?,
    parentStopLocation: LatLng?,
    liveLocationPoints: List<LatLng>? = null,
    busStopPoints: List<BusStop>? = null,
    logoutClick: () -> Unit = {},
    onStartStopButtonClick: () -> Unit = {},
    onMapExpandClick: () -> Unit = {},
    onSettingClick: () -> Unit = {},
    navController: NavController
) {
    val context = LocalContext.current
    val modifier =
        if (isMapExpanded) Modifier.fillMaxSize() else Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())


    BackHandler(enabled = isMapExpanded) {
        onMapExpandClick()
    }

    Scaffold(
        topBar = {
            BusTopAppBar(
                isMapExpanded = isMapExpanded,
                isParentPortal = portal == Portals.PARENT.value,
                title = "BUS TRACK",
                onSettingClick = onSettingClick
            ) {
                // Collapse Map
                onMapExpandClick()
            }
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedVisibility(
                visible = !hasInternetConnection,
                modifier = Modifier // Can be on top of the Box too
            ) {
                InternetConnectionAlertView()
                Spacer(Modifier.height(16.dp))
            }

            mapContent()

            if (!isMapExpanded) {
                Column(
                    modifier = Modifier
                        .offset(y = (-28).dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .shadow(
                            8.dp,
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) // Apply shadow with the same shape for best effect
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )

                    // Only for Driver
                    if (portal == Portals.DRIVER.value) {
                        StartStopButton(
                            isLocationPointsEmpty = liveLocationPoints.isNullOrEmpty(),
                            sharingState
                        ) { onStartStopButtonClick() }
                    }

                    // Bus Live Info - ETA and Remaining Distance
                    if (portal == Portals.PARENT.value) {
                        BusLiveUpdate(
                            eta = busEta,
                            remainingDistance = remainingDistance
                        )
                    }



                    if (portal == Portals.ADMIN.value) {
                        AdminPortal(navController = navController)
                    }
                    if (portal == Portals.CARETAKER.value) {
                        CaretakerScreen()
                    }


                    if (portal != Portals.ADMIN.value && portal != Portals.CARETAKER.value) {
                        if (busStopPoints != null)
                            BusStopPoints(
                                stops = busStopPoints,
                                realtimeLocation = realtimeLocation
                            )
                    }

                    // Logout Button todo: Remove logout button
                    BigButton("Logout", icon = Icons.Default.Logout) { logoutClick() }

                    /// DataStore Values // todo: Remove
//                    Text("Portal: $portal")
//                    Text("BusId: $busId")
//                    Text("Email: ${email}")
//                    if (portal == Portals.PARENT.value)
//                        Text(realtimeLocation.toString())
                }
            }

        }
    }
}


@Composable
fun BigButton(
    text: String = "Expand Map",
    icon: ImageVector = Icons.Default.Route,
    onButtonClick: () -> Unit
) {
    Card(
        onClick = onButtonClick,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Primary
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = AppColors.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(text, color = AppColors.onPrimary, fontSize = 24.sp)
        }

    }
}

@Composable
fun BusLiveInfoCard(

) {

    OutlinedCard(
        border = BorderStroke(2.dp, AppColors.Primary),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,

            )
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {

            ListItem(
                supportingContent = {
                    Text("Bus-1 has been reached the second stop at khattarpur bajar near lulumall")
                },
                headlineContent = {
                    Text("Bus 1 - Manisha")
                },
                trailingContent = {
                    Text("23mins", color = AppColors.Primary)
                },
                leadingContent = {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "",
                        tint = AppColors.Primary
                    )
                }

            )

        }

    }

}


@Composable
fun BusLiveUpdate(
    eta: String,
    remainingDistance: String
) {

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = AppColors.PurpleBlue,
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bus Live Update", fontSize = 18.sp, color = Color.White)
                LivePulseIndicator(size = 32.dp, color = AppColors.OnPurpleBlue)
            }
            Spacer(Modifier.height(12.dp))
            InfoRow(label = "Estimated Time", "$eta")
            InfoRow(label = "Remaining Distance", remainingDistance)
        }

    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppColors.OnPurpleBlue)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun LivePulseIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    circleCount: Int = 3,
    color: Color = AppColors.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Create multiple circles with staggered animations
        repeat(circleCount) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * (2000 / circleCount))
                ),
                label = "scale_$index"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * (2000 / circleCount))
                ),
                label = "alpha_$index"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.toPx() / 2) * scale
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Center solid circle
        Canvas(modifier = Modifier.size(size * 0.35f)) {
            drawCircle(
                color = color
            )
        }
    }
}






