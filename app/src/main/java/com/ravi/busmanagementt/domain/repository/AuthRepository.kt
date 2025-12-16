package com.ravi.busmanagementt.domain.repository

import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun login(email: String, password: String, portal: String): Flow<Resource<String>>
    suspend fun logout()

}