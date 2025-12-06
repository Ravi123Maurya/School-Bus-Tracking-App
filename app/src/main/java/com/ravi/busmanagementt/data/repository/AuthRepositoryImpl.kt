package com.ravi.busmanagementt.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.ravi.busmanagementt.domain.repository.AuthRepository
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun login(email: String, password: String): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {

                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

                if (authResult.user != null) {
                    emit(Resource.Success("Login Success"))
                } else {
                    emit(Resource.Error("Account not found"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }


    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

}