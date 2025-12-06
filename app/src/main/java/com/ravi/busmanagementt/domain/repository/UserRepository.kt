package com.ravi.busmanagementt.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getParentProfile() : Flow<Resource<Parent>>

    suspend fun setFcmToken(token: String)

    suspend fun setBusStopLocation(busStop: BusStop)


}