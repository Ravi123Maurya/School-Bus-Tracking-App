package com.ravi.busmanagementt.domain.repository

import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AdminRepository {

    suspend fun addBus(driver: BusAndDriver): Flow<Resource<String>>


    suspend fun addParent(parent: Parent): Flow<Resource<String>>

     fun getAllBuses(): Flow<Resource<List<BusAndDriver>>>

    suspend fun getAllParents(): Flow<Resource<List<Parent>>>

     fun getAllLiveBusesStatus(): Flow<Map<String, Boolean>>

    fun getRealtimeLocationsOfAllBuses(): Flow<Map<String, List<RealtimeLocation>>>

    suspend fun updateParent(parent: Parent): Flow<Resource<String>>
    suspend fun deleteParent(parentId: String, parentEmail: String): Flow<Resource<String>>

}