package com.ravi.busmanagementt.domain.repository

import com.ravi.busmanagementt.presentation.home.caretaker.Child
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow

interface CaretakerRepository {

    suspend fun getStudentsForCaretaker() : Flow<Resource<List<Child>>>

    suspend fun markAttendance(studentId: String, status: String, isPickup: Boolean?) : Flow<Resource<String>>


}