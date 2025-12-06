package com.ravi.busmanagementt.data.serivce

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton


// todo : redundant code - same as LocationSharingService file

@Singleton
class LocationStateManager @Inject constructor() {

    private val _sharingState = MutableStateFlow(LocationSharingState.IDLE)
    val sharingState = _sharingState.asStateFlow()



}