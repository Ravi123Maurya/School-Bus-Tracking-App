package com.ravi.busmanagementt.presentation.home.admin.features.allbuses

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BusesWithStatus(
    val busDetail: BusAndDriver,
    val isLive: Boolean,
    val realtimeLocations: List<RealtimeLocation> = emptyList()
)

@HiltViewModel
class AllBusesViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val allBusesFlow = adminRepository.getAllBuses()
        .onEach { Log.d("DEBUG_FLOW", "Buses Flow Emitted: ${it.data?.size}") }

    private val liveStatusFlow = adminRepository.getAllLiveBusesStatus()
        .onEach { Log.d("DEBUG_FLOW", "Live Status Flow Emitted: ${it.keys.size}") }
        .onStart { emit(emptyMap()) }

    private val allBusesRealtimeLocationsFlow = adminRepository.getRealtimeLocationsOfAllBuses()
        .onEach { Log.d("DEBUG_FLOW", "Locations Flow Emitted: ${it.keys.size}") }
        .onStart { emit(emptyMap()) }

    @OptIn(FlowPreview::class)
    val busesWithStatus: StateFlow<GetAllBusesState> =
        combine(
            allBusesFlow,
            liveStatusFlow,
            allBusesRealtimeLocationsFlow
        ) { buses, liveStatus, realtimeLocations ->
            when (buses) {
                is Resource.Error -> {
                    GetAllBusesState.Error(buses.message ?: "An error occurred")
                }

                is Resource.Loading -> {
                    GetAllBusesState.Loading
                }

                is Resource.Success -> {
                    val buses = buses.data ?: emptyList()
                    val combinedList = buses.map { bus ->
                        val isLive = liveStatus[bus.busId] ?: false
                        val realtimeLocations = realtimeLocations[bus.busId] ?: emptyList()
                        BusesWithStatus(
                            busDetail = bus,
                            isLive = isLive,
                            realtimeLocations = realtimeLocations
                        )
                    }
                    GetAllBusesState.Success(combinedList)
                }
            }
        }
            .sample(1000L)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = GetAllBusesState.Loading
            )


}

sealed class GetAllBusesState {
    object Loading : GetAllBusesState()
    data class Error(val message: String) : GetAllBusesState()
    data class Success(val buses: List<BusesWithStatus>) : GetAllBusesState()
}