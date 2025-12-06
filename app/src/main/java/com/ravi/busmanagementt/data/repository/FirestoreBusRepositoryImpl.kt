package com.ravi.busmanagementt.data.repository


import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class StopResponse(
    val stopName: String = "",
    val coordinates: LatLng = LatLng(0.0, 0.0)
)

class FirestoreBusRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : FirestoreBusRepository {


    override suspend fun getBusRouteStops(busId: String): Flow<List<BusStop>> {
        return callbackFlow {
            val busDoc = firestore.collection("buses").document(busId).get().await()
            val routesArray = busDoc.get("routes") as? List<Map<String, Any>>
            val stopsList = mutableListOf<BusStop>()
            if (routesArray != null){
                for (routeMap in routesArray){
                    try {
                        val stopName = routeMap["stopName"] as? String ?: "No Stop Name"
                        val locationName = routeMap["location"] as? String ?: "N/A"
                        val coordinates = routeMap["geoPoint"] as? GeoPoint
                        if( coordinates != null){
                            stopsList.add(
                                BusStop(
                                    stopName = stopName,
                                    location = locationName,
                                    geoPoint = coordinates
                                )
                            )
                        }
                    } catch (e: Exception){
                        Log.d("AdminRepositoryImpl", "Exception - Error passing routeMap: ${e.message}")
                    }
                }
            }
            trySend(stopsList)
            awaitClose {

            }

        }
    }

    override suspend fun getBusIdForParentFromEmail(email: String): String? {
        return try {
            val qsnap = firestore.collection("parents")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()


            if (qsnap.isEmpty) {
                Log.d("BusRepository", "No parent found with email: $email")
                null
            }else{
                val parentDoc = qsnap.documents.first()
                val busId = parentDoc.getString("assignedBusId")
                Log.d("BusRepository", "Bus ID for email $email: $busId")
                busId //Return the found Id
            }


        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching bus ID for stop: ", e)
            null
        }
    }

    override suspend fun getBusIdForDriverFromEmail(email: String): String? {
        return try {
            val qsnap = firestore.collection("buses")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()


            if (qsnap.isEmpty) {
                Log.d("BusRepository", "No bus found with email: $email")
                null
            }else{
                val parentDoc = qsnap.documents.first()
                val busId = parentDoc.getString("busId")
                Log.d("BusRepository", "Bus ID for email $email: $busId")
                busId //Return the found Id
            }


        } catch (e: Exception) {
            Log.e("BusRepository", "Error fetching bus ID for stop: ", e)
            null
        }
    }
}