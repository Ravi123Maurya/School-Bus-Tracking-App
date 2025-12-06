package com.ravi.busmanagementt.presentation.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.data.repository.AdminRepositoryImpl
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.data.serivce.LocationSharingStateManager
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import com.ravi.busmanagementt.presentation.home.LiveBusMap
import com.ravi.busmanagementt.presentation.home.MapState
import com.ravi.busmanagementt.utils.NetworkConnectivityManager
import com.ravi.busmanagementt.utils.NetworkStatus
import com.ravi.busmanagementt.utils.bitmapDescriptor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val realtimeLocationRepository: RealtimeLocationRepository,
    private val locationSharingStateManager: LocationSharingStateManager,
    private val adminRepositoryImpl: AdminRepositoryImpl,
    private val firestoreBusRepository: FirestoreBusRepository,
    private val connectivityManager: NetworkConnectivityManager
) : ViewModel() {


    val mapState = MapState(CameraPositionState(), viewModelScope)

    private val _viewState = MutableStateFlow(MapViewState())
    val viewState = _viewState.asStateFlow()

    private val _realtimeLocationState = MutableStateFlow<List<RealtimeLocation>?>(null)
    val realtimeLocationState = _realtimeLocationState.asStateFlow()

    private val _busStopsState = MutableStateFlow<List<BusStop>?>(null)
    val busStopsState = _busStopsState.asStateFlow()

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
    private val _mapContent = MutableStateFlow<(@Composable () -> Unit)?>(null)
    val mapContent: StateFlow<(@Composable () -> Unit)?> = _mapContent.asStateFlow()
    val navBusId = MutableStateFlow<String?>(null)
    val busMarkerIcon = MutableStateFlow<BitmapDescriptor?>(null)
    val isAdminPortal = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _mapContent.value = {

                val currentViewState by viewState.collectAsState()
                val currentRealtimeLocation by realtimeLocationState.collectAsState()
                val animateToThisBus by navBusId.collectAsState()
                val busIcon by busMarkerIcon.collectAsState()

                LiveBusMap(
                    mapState = mapState,
                    busMarkerIcon = busIcon,
                    isMapExpanded = currentViewState.isMapExpanded,
                    initialMarkerPoint = currentViewState.initialLocation,
                    userLocation = currentViewState.userLocation,
                    liveLocationPoints = if (!isAdminPortal.value) currentRealtimeLocation?.map {
                        LatLng(
                            it.latitude,
                            it.longitude
                        )
                    } else null,
                    allBusesLiveLocations = if (isAdminPortal.value) realtimeAllBusesLocationState.value else null,
                    animateToBus = animateToThisBus,
                    onExpandClick = { toggleMapSize() },
                    onMapLoaded = {
                        busMarkerIcon.value = bitmapDescriptor(context, R.drawable.bus_marker_icon)
                    }
                )
            }
        }
        startLocationUpdates()
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

    // Get Location Updates from Firebase (Parent, Admin, Caretaker)
    fun getLocationUpdates(busId: String) {
        realtimeLocationRepository.getLocationUpdatesFromFRTD(busId)
            .onEach { location ->
                Log.d("MapViewModel", "${busId} Location: $location")
                _realtimeLocationState.value = location
            }.catch {

            }.launchIn(viewModelScope)
    }

    fun getAllBusesRealtimeLocations() {
        adminRepositoryImpl.getRealtimeLocationsOfAllBuses()
            .onEach { data ->
                Log.d("MapViewModel", "All Buses Locations Data: $data")
                _realtimeAllBusesLocationState.value = data
            }.catch {

            }.launchIn(viewModelScope)
    }

    fun getBusStopsByBusId(busId: String) = viewModelScope.launch {
        firestoreBusRepository.getBusRouteStops(busId).collect { busStops ->
            _busStopsState.value = busStops
        }
    }


    fun toggleMapSize() {
        _viewState.value = _viewState.value.copy(
            isMapExpanded = !_viewState.value.isMapExpanded
        )
    }


    // For Driver only - start/stop toggle button

    fun toggleSharingLocationState() = viewModelScope.launch {
        if (!isSharingLocation.value && !connectivityManager.isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return@launch
        }
        locationSharingStateManager.toggleSharingLocationState()
    }


}


data class MapViewState(
    val initialLocation: LatLng? = null,
    val userLocation: LatLng? = null,
    val distancePoints: List<LatLng> = emptyList(),
    val isMapExpanded: Boolean = false,
)
