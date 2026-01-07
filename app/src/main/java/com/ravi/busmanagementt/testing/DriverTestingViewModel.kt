package com.ravi.busmanagementt.testing

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import com.ravi.busmanagementt.utils.DistanceMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.isNotEmpty

/** TESTINGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG**/

data class DriverTestingState(
    val isSimulating: Boolean = false,
)

sealed interface DriverTestingAction {
    data class OnStartTrip(
        val busId: String,
        val route: List<LatLng>,
        val speedKmph: Double = 60.0,
        val timeIntervalMillis: Long = 1000L
    ) : DriverTestingAction

    data class OnStopTrip(val busId: String) : DriverTestingAction
}

@HiltViewModel
class DriverTestingViewModel @Inject constructor(
    private val locationRepository: RealtimeLocationRepository,
    private val firestoreBusRepository: FirestoreBusRepository
) : ViewModel() {
    var simulationJob: Job? = null
    private val _state = MutableStateFlow(DriverTestingState())
    val state = _state.asStateFlow()

    private var numberOfStopsReached = 0

    private val _busStops = MutableStateFlow<List<BusStop>>(emptyList())
    val busStops = _busStops.asStateFlow()

    init {
        viewModelScope.launch {
            _busStops.value =
                firestoreBusRepository.getBusRouteStops("bus_test_2").firstOrNull() ?: emptyList()
        }
    }

    fun onAction(action: DriverTestingAction) {
        when (action) {
            is DriverTestingAction.OnStartTrip -> {
                startSimulation(
                    busId = action.busId,
                    routePoints = action.route,
                    speedKmph = action.speedKmph,
                    timeIntervalMillis = action.timeIntervalMillis
                )
            }

            is DriverTestingAction.OnStopTrip -> {
                stopTrip(busId = action.busId)
            }
        }
    }

    private fun startSimulation(
        busId: String,
        routePoints: List<LatLng>,
        speedKmph: Double = 60.0,
        timeIntervalMillis: Long = 1000L
    ) {
        _state.update { it.copy(isSimulating = true) }
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.IO) {

            locationRepository.setBusIsLive(busId, true)

            LocationSimulator.simulateDrivingRoute(
                path = routePoints,
                speedKmph = speedKmph,
                updateIntervalMillis = timeIntervalMillis
            )
                .collect { mockLocation ->

                    if (busStops.value.isNotEmpty() && numberOfStopsReached < busStops.value.size) {
                        val hasStopReached = DistanceMatrix.isDistanceInRange(
                            LatLng(mockLocation.latitude, mockLocation.longitude),
                            LatLng(
                                busStops.value[numberOfStopsReached].geoPoint.latitude,
                                busStops.value[numberOfStopsReached].geoPoint.longitude
                            ),
                            60 // todo: make the range smaller in real testing:  (50-60m)
                        )
                        if (hasStopReached) {
                            numberOfStopsReached++
                        }
                    }

                    locationRepository.pushLocationToFRTD(
                        busId = busId,
                        location = mockLocation,
                        numberOfStopsReached = numberOfStopsReached
                    )
                }

        }
    }

    private fun stopTrip(busId: String) {
        viewModelScope.launch {
            locationRepository.setBusIsLive(busId, false)
        }
        _state.update { it.copy(isSimulating = false) }
        simulationJob?.cancel()
    }
}