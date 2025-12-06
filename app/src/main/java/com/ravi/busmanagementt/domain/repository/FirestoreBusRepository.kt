package com.ravi.busmanagementt.domain.repository


import com.ravi.busmanagementt.domain.model.BusStop
import kotlinx.coroutines.flow.Flow

interface FirestoreBusRepository {

    suspend fun getBusRouteStops(busId: String): Flow<List<BusStop>>

    suspend fun getBusIdForParentFromEmail(email: String): String?

    suspend fun getBusIdForDriverFromEmail(email: String): String?

}