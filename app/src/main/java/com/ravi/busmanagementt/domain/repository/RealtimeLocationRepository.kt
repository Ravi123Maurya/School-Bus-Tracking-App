package com.ravi.busmanagementt.domain.repository

import android.location.Location
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import kotlinx.coroutines.flow.Flow

interface RealtimeLocationRepository {

     fun startLocationUpdates() : Flow<Location>

     fun getLocationUpdatesFromFRTD(busId: String): Flow<List<RealtimeLocation>>

    suspend fun pushLocationToFRTD(busId: String, location: Location, numberOfStopsReached: Int)

    suspend fun stopLocationUpdates()

    suspend fun setBusIsLive(busId: String, isLive: Boolean)

}