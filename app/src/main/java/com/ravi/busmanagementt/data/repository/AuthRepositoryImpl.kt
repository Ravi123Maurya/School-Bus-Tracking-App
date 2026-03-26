package com.ravi.busmanagementt.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ravi.busmanagementt.domain.repository.AuthRepository
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        portal: String
    ): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {

                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid

                if (uid != null) {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    if (userDoc.exists()) {
                        val userRole = userDoc.getString("role") ?: "n/a"
                        if (userRole.lowercase().trim() == portal.lowercase().trim()) {
                            emit(Resource.Success("Login Success"))
                        } else {
                            emit(Resource.Error("This account doesn't belong to $portal portal"))
                        }
                    } else {
                        emit(Resource.Error("Account not found"))
                    }
                } else {
                    emit(Resource.Error("Email doesn't exist"))
                }

            } catch (e: FirebaseFirestoreException) {
                emit(Resource.Error("Database access denied. Please contact admin."))
            } catch (e: FirebaseAuthException) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            } catch (e: Exception) {
                emit(Resource.Error("An unexpected error occurred. Please try again."))
            }
        }


    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

}