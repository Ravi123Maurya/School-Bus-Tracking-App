package com.ravi.busmanagementt.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.model.Caretaker
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.domain.model.Student
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

    suspend fun getDirectionsRoute(stops: List<LatLng>): List<LatLng>


    suspend fun getBusWithId(busId: String): Flow<Resource<BusAndDriver>>
    suspend fun updateBus(busAndDriver: BusAndDriver, adminPassword: String): Flow<Resource<String>>
    suspend fun deleteBus(busId: String): Flow<Resource<String>>


    suspend fun addCaretaker(caretaker: Caretaker): Flow<Resource<String>>

    suspend fun getAllCaretakers() : Flow<Resource<List<Caretaker>>>
    suspend fun updateCaretaker(caretaker: Caretaker) : Flow<Resource<String>>

    suspend fun getAllStudents() : Flow<Resource<List<Student>>>
    suspend fun addStudent(student: Student) : Flow<Resource<String>>
    suspend fun updateStudent(student: Student) : Flow<Resource<String>>
    suspend fun deleteStudent(stuId: String) : Flow<Resource<String>>

}