package com.ravi.busmanagementt.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.domain.repository.UserRepository
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fireStore: FirebaseFirestore
) : UserRepository {


    override suspend fun getParentProfile(): Flow<Resource<Parent>> {
        return flow {
            emit(Resource.Loading())
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) {
                    emit(Resource.Error("User is not logged in"))
                    return@flow
                }
                val parentDoc = fireStore.collection("parents").document(uid).get().await()
                if (parentDoc == null) emit(Resource.Error("No parent found"))

                val name = parentDoc.getString("name") ?: "No Name"
                val email = parentDoc.getString("email") ?: "No Email found"
                val assignedBusId = parentDoc.getString("assignedBusId") ?: "No Bus Assigned"
                val busStopLocation = parentDoc.getGeoPoint("busStopLocation") ?: GeoPoint(0.0, 0.0)
                val stopName = parentDoc.getString("stopName") ?: "No Stop Name"
                val location = parentDoc.getString("location") ?: "No Location"

                val parentData = Parent(
                    parentId = uid,
                    name = name,
                    email = email,
                    password = "",
                    assignedBusId = assignedBusId,
                    busStopLocation = busStopLocation,
                    stopName = stopName,
                    location = location
                )

                emit(Resource.Success(parentData))


            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.message.toString()))
            }
        }

    }


    override suspend fun setFcmToken(token: String) {
        Log.d("UserAuthRepository", " SetFCMToken Token: $token")
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("UserRepositoryImpl", "User is not logged in")
            return
        }
        val parentId = getParentDocumentId()
        if (parentId == null) {
            Log.e("UserRepositoryImpl", "No parent found")
            return
        }
        val dbRef = fireStore.collection("parents").document(parentId)
        dbRef.set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("UserRepositoryImpl", "Token sent to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("UserRepositoryImpl", "Error sending token to Firestore", e)
            }
    }

    override suspend fun setBusStopLocation(busStop: BusStop) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("UserRepositoryImpl", "User is not logged in")
            return
        }
        val parentId = getParentDocumentId()
        if (parentId == null) {
            Log.e("UserRepositoryImpl", "No parent found")
            return
        }
        val geoPoint = GeoPoint(busStop.geoPoint.latitude, busStop.geoPoint.longitude)
        val parentDbRef = fireStore.collection("parents").document(parentId)

        val busStopData = mapOf(
            "busStopLocation" to geoPoint,
            "stopName" to busStop.stopName,
            "location" to busStop.location
        )

        parentDbRef.set(busStopData,SetOptions.merge())
            .addOnCompleteListener {
                Log.d("UserRepositoryImpl", "Token sent to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("UserRepositoryImpl", "Error sending token to Firestore", e)
            }
    }

    private suspend fun getParentDocumentId(): String? {
        val email = firebaseAuth.currentUser?.email
        if (email == null) {
            Log.e("UserRepositoryImpl", "User is not logged in")
            return null
        }

        val dbRef = fireStore.collection("parents")
        val snapshot = dbRef.whereEqualTo("email", email).limit(1).get().await()
        if (snapshot.isEmpty) {
            Log.e("UserRepositoryImpl", "No parent found with email: $email")
            return null
        }
        val parentDoc = snapshot.documents.first()
        val parentDocumentId = parentDoc.id
        return parentDocumentId

    }
}