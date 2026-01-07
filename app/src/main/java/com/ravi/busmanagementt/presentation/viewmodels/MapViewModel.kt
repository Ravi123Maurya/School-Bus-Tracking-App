package com.ravi.busmanagementt.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.core.graphics.values
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.data.serivce.LocationSharingStateManager
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import com.ravi.busmanagementt.presentation.home.LiveBusMap
import com.ravi.busmanagementt.presentation.home.MapState
import com.ravi.busmanagementt.utils.DistanceMatrix
import com.ravi.busmanagementt.utils.NetworkConnectivityManager
import com.ravi.busmanagementt.utils.NetworkStatus
import com.ravi.busmanagementt.utils.TimeMatrix
import com.ravi.busmanagementt.utils.bitmapDescriptor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt


@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val realtimeLocationRepository: RealtimeLocationRepository,
    private val locationSharingStateManager: LocationSharingStateManager,
    private val adminRepository: AdminRepository,
    private val firestoreBusRepository: FirestoreBusRepository,
    private val connectivityManager: NetworkConnectivityManager
) : ViewModel() {


    val mapState = MapState(CameraPositionState(), viewModelScope)

    private val _viewState = MutableStateFlow(MapViewState())
    val viewState = _viewState.asStateFlow()

    // 1. Lightweight: Just the single latest point for every bus (50 objects)
    private val _allBusesCurrentLocations =
        MutableStateFlow<Map<String, RealtimeLocation?>>(emptyMap())
    val allBusesCurrentLocations = _allBusesCurrentLocations.asStateFlow()

    // 2. Heavy: Full history ONLY for the selected bus (1 list of ~1000 objects)
    private val _rawFullData = MutableStateFlow<Map<String, List<RealtimeLocation>>>(emptyMap())
    val navBusId = MutableStateFlow<String?>(null)

    // 2. Derive selectedBusPath automatically
    val selectedBusPath: StateFlow<List<LatLng>> =
        combine(_rawFullData, navBusId) { data, selectedId ->
            // Auto-select logic if nothing selected
            val targetId = selectedId ?: data.keys.firstOrNull()

            if (targetId != null) {
                data[targetId]?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allBusesStopsLatLng = MutableStateFlow<Map<String, List<BusStop>>>(emptyMap())
    val allBusesStopsLatLng = _allBusesStopsLatLng.asStateFlow()
    val allBusesRoutesLatLng: StateFlow<Map<String, List<LatLng>>> =
        allBusesStopsLatLng.map { allStops ->
            // This will re-run whenever allBusesStopsLatLng changes
            val map = allStops.map { singleBusStopList ->
                val schoolPoint = LatLng(
                    19.185766,
                    73.051846
                ) // Todo: This is dummy school location - remove in release
                val latLngList = singleBusStopList.value.map { stop ->
                    LatLng(stop.geoPoint.latitude, stop.geoPoint.longitude)
                }
                val route = adminRepository.getDirectionsRoute(latLngList)
                singleBusStopList.key to route
            }
            map.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    private val _realtimeLocationState = MutableStateFlow<List<RealtimeLocation>?>(null)
    val realtimeLocationState = _realtimeLocationState.asStateFlow()

    private val _busStopsState = MutableStateFlow<List<BusStop>>(emptyList())
    val busStopsState = _busStopsState.asStateFlow()

    val busStops = MutableStateFlow<List<BusStop>>(emptyList())
    val busRouteLatLng: StateFlow<List<LatLng>> = busStops.map { stops ->
        val latLntList = stops.map { stop ->
            LatLng(stop.geoPoint.latitude, stop.geoPoint.longitude)
        }
        adminRepository.getDirectionsRoute(latLntList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _realtimeAllBusesLocationState =
        MutableStateFlow<Map<String, List<RealtimeLocation>>?>(null)
    val realtimeAllBusesLocationState = _realtimeAllBusesLocationState.asStateFlow()

    val isSharingLocation = locationSharingStateManager.isSharingLocation
    val sharingState = locationSharingStateManager.sharingState
    val driverSharingLocation = locationSharingStateManager.locationPoints.asStateFlow()
    val hasInternetConnection: StateFlow<Boolean> =
        connectivityManager.networkStatus
            .map { it == NetworkStatus.Available }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = connectivityManager.isNetworkAvailable()
            )


    // Load Map here to avoid ui lag
    val busMarkerIcon = MutableStateFlow<BitmapDescriptor?>(null)

    private val _isAdminPortal = MutableStateFlow(false)
    val isAdminPortal = _isAdminPortal.asStateFlow()
    fun setIsAdmin(isAdmin: Boolean) {
        _isAdminPortal.value = isAdmin
    }

    val stopLocation = MutableStateFlow(null as LatLng?)
    val eta = MutableStateFlow("No data")
    val remainingDistance = MutableStateFlow("No data")

    val mapContent: @Composable () -> Unit = movableContentOf {
        val currentViewState by viewState.collectAsState()
        val currentRealtimeLocation by realtimeLocationState.collectAsState()
        val animateToThisBus by navBusId.collectAsState()
        val busIcon by busMarkerIcon.collectAsState()
        val allBusesRoutes by allBusesRoutesLatLng.collectAsState()
        val allBusesStops by allBusesStopsLatLng.collectAsState()
        val busRouteLatLng by busRouteLatLng.collectAsState()
        val parentStopLocation by stopLocation.collectAsState()
        val isAdmin by isAdminPortal.collectAsState()
        val busStops by busStops.collectAsState()
        val currentLocationsMap by allBusesCurrentLocations.collectAsState()
        val currentSelectedPath by selectedBusPath.collectAsState()

        LiveBusMap(
            isAdminPortal = isAdmin,
            mapState = mapState,
            busMarkerIcon = busIcon,
            isMapExpanded = currentViewState.isMapExpanded,
            initialMarkerPoint = currentViewState.initialLocation,
            userLocation = currentViewState.userLocation,
            stopLocation = parentStopLocation,
            liveLocationPoints = if (!isAdmin) currentRealtimeLocation?.map {
                LatLng(
                    it.latitude,
                    it.longitude
                )
            } else null,
            busRouteLatLng = busRouteLatLng,
            busStops = busStops,

            allBusesCurrentLocations = if (isAdmin) currentLocationsMap else null,
            selectedBusPath = if (isAdmin) currentSelectedPath else null,
            allBusesRoutesLatLng = if (isAdmin) allBusesRoutes else null,
            allBusesStopsLatLng = if (isAdmin) allBusesStops else null,
            focusToBus = animateToThisBus,
            onExpandClick = { toggleMapSize() },
            onMapLoaded = {
                busMarkerIcon.value = bitmapDescriptor(context, R.drawable.bus_marker_icon)
            },
            onBusSelected = { busId ->
                navBusId.value = busId
            }
        )
    }

    init {
        // Start User Location
        startLocationUpdates()

        viewModelScope.launch {
            isAdminPortal.collect { isAdmin ->
                if (isAdmin) {
                    startAdminDataUpdates()
                } else {
                    _allBusesStopsLatLng.value = emptyMap()
                    _realtimeAllBusesLocationState.value = null
                }
            }
        }
    }


    // Current Location Updates for (Parent, Caretaker, Admin)

    fun startLocationUpdates() {
        realtimeLocationRepository.startLocationUpdates()
            .onEach { location ->
                val currentLocation = LatLng(location.latitude, location.longitude)
                Log.d("MapViewModel", "Device Current Location: $currentLocation")
                _viewState.value = _viewState.value.copy(
                    userLocation = currentLocation,
                    distancePoints = locationSharingStateManager.locationPoints.value
                )
                if (_viewState.value.initialLocation == null) {
                    _viewState.value = _viewState.value.copy(
                        initialLocation = currentLocation,
                    )
                }

            }
            .catch {
                Log.d("MapViewModel", "Error: ${it.message}")
            }
            .launchIn(viewModelScope)

    }

    // Get Location Updates from Firebase Realtime (Parent, Driver, Caretaker)
    fun getLocationUpdates(busId: String) {
        realtimeLocationRepository.getLocationUpdatesFromFRTD(busId)
            .flowOn(Dispatchers.IO)
            .conflate()
            .onEach { location ->
                _realtimeLocationState.value = location

                // Eta and distance calculation
                viewModelScope.launch(Dispatchers.Default) {
                    eta.value = if (stopLocation.value == null) "Set stop location" else {
                        if (!location.isNullOrEmpty()) {
                            val fiveMinutesAgo =
                                System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
                            if (location.last().timestamp.toLong() < fiveMinutesAgo) {
                                "Offline" // Bus will show offline if las location is older than 5 minutes
                            } else {
                                val etaMinutes = DistanceMatrix.calculateScheduleTimeETA(
                                    location,
                                    stopLocation.value!!
                                )
                                TimeMatrix.formatEtaForUi(etaMinutes)
                            }
                        } else "Offline"

                    }

                    val sortedLocations =
                        location.sortedBy { it.timestamp.toLongOrNull() ?: 0L }

                    val busCurrentLocation = if (sortedLocations.isEmpty()) {
                        stopLocation.value
                    } else {
                        LatLng(
                            sortedLocations.last().latitude,
                            sortedLocations.last().longitude
                        )
                    }

                    if (busCurrentLocation != null && stopLocation.value != null) {
                        remainingDistance.value = (SphericalUtil.computeDistanceBetween(
                            busCurrentLocation,
                            stopLocation.value
                        )
                            .roundToInt() / 1000f).toString() + " km"
                    }

                }
            }.catch {

            }.launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    fun getAllBusesRealtimeLocations() {

        if (!isAdminPortal.value) return

        adminRepository.getRealtimeLocationsOfAllBuses()
            .flowOn(Dispatchers.IO)
            .conflate()
            .sample(1000L)
            .onEach { data ->
                // 1. Update Raw Data (triggers the combine above)
                _rawFullData.value = data

                // 2. Update Markers (Lightweight)
                val markersMap = data.mapValues { it.value.lastOrNull() }
                _allBusesCurrentLocations.value = markersMap

                // If no bus selected yet, select the first one to trigger the combine flow defaults
                if (navBusId.value == null && data.isNotEmpty()) {
                    navBusId.value = data.keys.first()
                }
            }.catch {

            }.launchIn(viewModelScope)
    }

    fun onBusClick(busId: String) {
        navBusId.value = busId
    }

    fun getBusStopsByBusId(busId: String) = viewModelScope.launch {
        firestoreBusRepository.getBusRouteStops(busId).collect { busStops ->
            _busStopsState.value = busStops
        }
    }

    private fun startAdminDataUpdates() {

        adminRepository.getAllBuses()
            .onEach { buses ->
                val stopLists = buses.data?.map { busesStops ->
                    busesStops.busId to busesStops.routes
                }
                _allBusesStopsLatLng.value = stopLists?.toMap() ?: emptyMap()
            }
            .catch { }
            .launchIn(viewModelScope)

        getAllBusesRealtimeLocations()

    }


    fun toggleMapSize() {
        _viewState.value = _viewState.value.copy(
            isMapExpanded = !_viewState.value.isMapExpanded
        )
    }


}


data class MapViewState(
    val initialLocation: LatLng? = null,
    val userLocation: LatLng? = null,
    val distancePoints: List<LatLng> = emptyList(),
    val isMapExpanded: Boolean = false,
)
