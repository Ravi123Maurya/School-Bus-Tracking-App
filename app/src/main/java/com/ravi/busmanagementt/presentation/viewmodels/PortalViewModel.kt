package com.ravi.busmanagementt.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.data.datastore.PortalManager
import com.ravi.busmanagementt.data.datastore.Portals
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortalViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _portal = MutableStateFlow<Portals?>(null)
    val portal = _portal.asStateFlow()

    private val _stopLocation = MutableStateFlow<LatLng?>(null)
    val stopLocation = _stopLocation.asStateFlow()


    init {
        getStopLocation()
        getPortal()
    }


    fun setPortal(portal: Portals) {
        viewModelScope.launch {
            PortalManager.setPortal(
                context = context,
                portal = portal
            )
        }
    }

    fun getPortal() {
        viewModelScope.launch {
            try {
                PortalManager.getPortal(context).collectLatest { portal ->
                    _portal.value = Portals.entries.find { it.value == portal}
                }

            } catch (e: Exception) {
                Log.e("PortalViewModel", "Error getting portal: ${e.message}")
            }
        }
    }

    fun setStopLocation(stopLocation: LatLng) {
        viewModelScope.launch {
            PortalManager.setParentBusStopLocation(context, stopLocation)
        }
    }

    fun getStopLocation() = viewModelScope.launch {
        try {
            PortalManager.getParentBusStopLocation(context).collectLatest { stopLocation ->
               _stopLocation.value = stopLocation
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setStopLocationToFireStore(busStop: BusStop) = viewModelScope.launch {
        userRepository.setBusStopLocation(busStop)
    }

}