package com.ravi.busmanagementt.data.serivce

import android.content.Context
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton


//  Location Sharing State Manager (Driver)


enum class LocationSharingState {
    IDLE,
    LOADING,
    SHARING
}

@Singleton
class LocationSharingStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    var locationPoints = MutableStateFlow<List<LatLng>>(emptyList())
    private val _isSharingLocation = MutableStateFlow(false)
    val isSharingLocation = _isSharingLocation.asStateFlow()
    private val _sharingState = MutableStateFlow( LocationSharingState.IDLE)
    val sharingState = _sharingState.asStateFlow()


    fun setSharingState(newState: LocationSharingState){
        _sharingState.value = newState
    }

    fun toggleSharingLocationState() {
        _isSharingLocation.value = !_isSharingLocation.value
    }
}