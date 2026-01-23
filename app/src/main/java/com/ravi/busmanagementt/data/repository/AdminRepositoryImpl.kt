package com.ravi.busmanagementt.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.UserWriteRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.ravi.busmanagementt.BuildConfig
import com.ravi.busmanagementt.data.remote.DirectionsApiService
import com.ravi.busmanagementt.data.remote.decodePolyline
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.model.Caretaker
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Scanner
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    val fireStore: FirebaseFirestore,
    val firebaseAuth: FirebaseAuth,
    val realtimeDb: FirebaseDatabase,
    private val directionsApiService: DirectionsApiService
) : AdminRepository {

    override suspend fun addBus(driver: BusAndDriver): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {
                // ----- Check if bus already exists -----
                val isBusExists = fireStore.collection("buses").document(driver.busId).get().await()
                if (isBusExists.exists()) {
                    emit(Resource.Error("Bus already exists"))
                    return@flow
                }

                // ----- Check if email already exists -----
                val isEmailExists = firebaseAuth.fetchSignInMethodsForEmail(driver.email).await()
                if (isEmailExists.signInMethods?.isNotEmpty() == true) {
                    emit(Resource.Error("Email already exists"))
                    return@flow
                }

                // ----- Create account -----
                val driverAuth =
                    firebaseAuth.createUserWithEmailAndPassword(driver.email, driver.password)
                        .await()
                val uid = driverAuth.user?.uid
                if (uid != null) {
                    // ----- Add driver to firestore -----
                    val busDoc = fireStore.collection("buses").document(driver.busId)
                    val busData = mapOf(
                        "busId" to driver.busId,
                        "email" to driver.email,
                        "driverName" to driver.driverName,
                        "busName" to driver.busId,
                        "routes" to driver.routes
                    )
                    busDoc.set(busData).await()
                    val usersDoc = fireStore.collection("users").document(uid)
                    val userData = mapOf(
                        "email" to driver.email,
                        "role" to "driver"
                    )
                    usersDoc.set(userData)
                    emit(Resource.Success("Bus added successfully"))
                } else {
                    emit(Resource.Error("Couldn't create account"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override suspend fun addParent(parent: Parent): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {
                val parentDoc = fireStore.collection("parents").document(parent.email).get().await()
                if (parentDoc.exists()) {
                    emit(Resource.Error("Parent already exists"))
                    return@flow
                }
                val isEmailExists = firebaseAuth.fetchSignInMethodsForEmail(parent.email).await()
                if (isEmailExists.signInMethods?.isNotEmpty() == true) {
                    Log.d("AdminRepositoryImpl", "Parent Email already exists")
                    emit(Resource.Error("Parent Email already exists"))
                    return@flow
                }
                val parentAuth =
                    firebaseAuth.createUserWithEmailAndPassword(parent.email, parent.password)
                        .await()
                val uid = parentAuth.user?.uid
                if (uid != null) {
                    val parentDoc = fireStore.collection("parents").document(uid)
                    val parentData = mapOf(
                        "name" to parent.name,
                        "email" to parent.email,
                        "assignedBusId" to parent.assignedBusId,
                    )
                    parentDoc.set(parentData).await()
                    val usersDoc = fireStore.collection("users").document(uid)
                    val userData = mapOf(
                        "email" to parent.email,
                        "role" to "parent"
                    )
                    usersDoc.set(userData)
                    emit(Resource.Success("Parent added successfully"))
                } else {
                    emit(Resource.Error("Couldn't create account"))
                }


            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override fun getAllBuses(): Flow<Resource<List<BusAndDriver>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val busSnapshot =
                    fireStore.collection("buses").get().await()
                val busList = withContext(Dispatchers.Default) {
                    busSnapshot.documents.mapNotNull { busDoc ->
                        try {
                            val busId = busDoc.getString("busId")
                            val email = busDoc.getString("email")
                            if (busId == null || email == null) return@mapNotNull null
                            val busName = busDoc.getString("busName") ?: "No Name"
                            val driverName = busDoc.getString("driverName") ?: "No Name"
                            val routesArray = busDoc.get("routes") as? List<Map<String, Any>>
                            val routes = mutableListOf<BusStop>()

                            if (routesArray != null) {
                                for (routeMap in routesArray) {
                                    try {
                                        val stopName =
                                            routeMap["stopName"] as? String ?: "No Stop Name"
                                        val locationName = routeMap["location"] as? String ?: "N/A"
                                        val coordinates = routeMap["geoPoint"] as? GeoPoint
                                        if (coordinates != null) {
                                            routes.add(
                                                BusStop(
                                                    stopName = stopName,
                                                    location = locationName,
                                                    geoPoint = coordinates
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.d(
                                            "AdminRepositoryImpl",
                                            "Exception - Error passing routeMap: ${e.message}"
                                        )
                                    }
                                }
                            }
                            Log.d("AdminRepositoryImpl", "routes: $routes")
                            BusAndDriver(
                                busId = busId,
                                driverName = driverName,
                                email = email,
                                id = -1,
                                password = "",
                                routes = routes,
                            )

                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                    }
                }
                emit(Resource.Success(busList))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override suspend fun getAllParents(): Flow<Resource<List<Parent>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val parentsSnapshot =
                    fireStore.collection("parents").get().await()
                val parentList = withContext(Dispatchers.Default) {
                    parentsSnapshot.documents.mapNotNull { parentDoc ->
                        try {
                            val parentName = parentDoc.getString("name") ?: "No Name"
                            val email = parentDoc.getString("email")
                            val assignedBusId = parentDoc.getString("assignedBusId")
                            val stopName = parentDoc.getString("stopName") ?: ""
                            val location = parentDoc.getString("location") ?: ""
                            val busStopLocation =
                                parentDoc.getGeoPoint("busStopLocation") ?: GeoPoint(0.0, 0.0)
                            if (email != null && assignedBusId != null) {
                                Parent(
                                    parentId = parentDoc.id,
                                    name = parentName,
                                    email = email,
                                    assignedBusId = assignedBusId,
                                    busStopLocation = busStopLocation,
                                    password = "",
                                    stopName = stopName,
                                    location = location
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }
                emit(Resource.Success(parentList))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override fun getAllLiveBusesStatus(): Flow<Map<String, Boolean>> {
        return callbackFlow {

            val liveBusesSnapshot = realtimeDb.getReference("Live Buses")

            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    this@callbackFlow.launch(Dispatchers.Default) {
                        val busesStatusMap = mutableMapOf<String, Boolean>()

                        for (childSnapshot in snapshot.children) {
                            try {
                                val busId = childSnapshot.key
                                val isLive = childSnapshot.getValue(Boolean::class.java)
                                if (busId != null && isLive != null) {
                                    busesStatusMap[busId] = isLive
                                }
                            } catch (e: Exception) {
                                Log.d("AdminRepositoryImpl", "Exception: ${e.message}")
                            }
                        }
                        trySend(busesStatusMap)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }

            }
            liveBusesSnapshot.addValueEventListener(valueEventListener)
            awaitClose {
                liveBusesSnapshot.removeEventListener(valueEventListener)
            }

        }
    }

    override fun getRealtimeLocationsOfAllBuses(): Flow<Map<String, List<RealtimeLocation>>> {
        return callbackFlow {
            val rootRef = realtimeDb.getReference("liveLocations")
            val localCache = mutableMapOf<String, MutableList<RealtimeLocation>>()

            // 1. Initial Load (Get current state once)
            // This might take a moment, but it's done only once.
            rootRef.get().addOnSuccessListener { snapshot ->
                this@callbackFlow.launch(Dispatchers.Default) {
                    snapshot.children.forEach { busSnapshot ->
                        val busId = busSnapshot.key ?: return@forEach
                        val locations = busSnapshot.children.mapNotNull {
                            it.getValue(RealtimeLocation::class.java)
                        }.toMutableList()
                        localCache[busId] = locations
                    }
                    // Emit initial state
                    trySend(localCache.toMap())
                }


            }.addOnFailureListener {
                close(it)
            }

            val childEventListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    this@callbackFlow.launch {
                        // Logic handles new buses appearing
                        val busId = snapshot.key ?: return@launch
                        if (localCache.containsKey(busId)) return@launch

                        val list =
                            snapshot.children.mapNotNull { it.getValue(RealtimeLocation::class.java) }
                                .toMutableList()
                        synchronized(localCache) {
                            localCache[busId] = list
                            trySend(localCache.toMap())
                        }
                    }

                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    this@callbackFlow.launch(Dispatchers.Default) {
                        // Logic handles existing bus updates
                        val busId = snapshot.key ?: return@launch
                        // Still downloads full list of THIS bus, but avoids the other 49.
                        val list =
                            snapshot.children.mapNotNull { it.getValue(RealtimeLocation::class.java) }
                                .toMutableList()

                        synchronized(localCache) {
                            localCache[busId] = list
                            trySend(localCache.toMap())
                        }
                    }

                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            rootRef.addChildEventListener(childEventListener)
            awaitClose { rootRef.removeEventListener(childEventListener) }
        }


    }

    override suspend fun updateParent(parent: Parent): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            if (parent.parentId.isBlank()) {
                emit(Resource.Error("Parent ID is missing. Cannot update."))
                return@flow
            }
            try {
                val parentDoc = fireStore.collection("parents").document(parent.parentId)

                val updatedParentData = mapOf(
                    "name" to parent.name,
                    "assignedBusId" to parent.assignedBusId
                )
                parentDoc.update(updatedParentData).await()
                emit(Resource.Success("Parent updated successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }

        }
    }

    override suspend fun deleteParent(
        parentId: String,
        parentEmail: String
    ): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            if (parentId.isBlank()) {
                emit(Resource.Error("Parent ID is missing. Cannot delete."))
                return@flow
            }
            try {
                // Future todo: delete parent email in firebase "Authentication" as well*

                val parentDoc = fireStore.collection("parents").document(parentId)
                parentDoc.delete().await()
                emit(Resource.Success("Parent deleted successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong during deletion"))
            }


        }
    }

    override suspend fun getDirectionsRoute(stops: List<LatLng>): List<LatLng> {
        if (stops.size < 2) return emptyList()
        val apiKey = BuildConfig.MAP_API_KEY
        val queryOptions = mutableMapOf(
            "origin" to "${stops.first().latitude},${stops.first().longitude}",
            "destination" to "${stops.last().latitude},${stops.last().longitude}",
            "key" to apiKey
        )

        if (stops.size > 2) {
            val waypointsString = stops.subList(1, stops.size - 1)
                .joinToString(separator = "|") { "${it.latitude},${it.longitude}" }
            queryOptions["waypoints"] = waypointsString
        }


        try {
            withContext(Dispatchers.IO) {
                // Make the call with the single map object
                val response = directionsApiService.getDirections(queryOptions)

                if (response.routes.isNotEmpty()) {
                    val encodedPolyline = response.routes.first().overview_polyline.points
                    return@withContext decodePolyline(encodedPolyline)
                } else {

                }
            }
        } catch (e: Exception) {
            // This will catch network errors (like HTTP 400) or JSON parsing errors.
        }
        return emptyList()
    }

    override suspend fun getBusWithId(busId: String): Flow<Resource<BusAndDriver>> {
        return flow {
            emit(Resource.Loading())
            try {
                val busDoc = fireStore.collection("buses").document(busId).get().await()
                if (busDoc.exists()) {
                    val busDriderId = busDoc.getString("busId") ?: ""
                    val routesArray = busDoc.get("routes") as? List<Map<String, Any>> ?: emptyList()
                    val routes = mutableListOf<BusStop>()
                    if (routesArray != null) {
                        withContext(Dispatchers.Default) {
                            for (routeMap in routesArray) {
                                try {
                                    val stopName = routeMap["stopName"] as? String ?: "No Stop Name"
                                    val locationName = routeMap["location"] as? String ?: "N/A"
                                    val coordinates = routeMap["geoPoint"] as? GeoPoint
                                    if (coordinates != null) {
                                        routes.add(
                                            BusStop(
                                                stopName = stopName,
                                                location = locationName,
                                                geoPoint = coordinates
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.d(
                                        "AdminRepositoryImpl",
                                        "Exception - Error passing routeMap: ${e.message}"
                                    )
                                }
                            }
                        }

                    }
                    val busData = BusAndDriver(
                        busId = busDriderId,
                        driverName = busDoc.getString("driverName") ?: "",
                        email = busDoc.getString("email") ?: "",
                        id = -1,
                        password = "",
                        routes = routes
                    )
                    emit(Resource.Success(busData))
                } else {
                    emit(Resource.Error("Bus not found"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override suspend fun updateBus(
        busAndDriver: BusAndDriver,
        adminPassword: String
    ): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())

            if (!reAuthenticateAdmin(adminPassword)) {
                emit(Resource.Error("Couldn't verify admin password"))
                return@flow
            }

            if (busAndDriver.busId.isBlank()) {
                emit(Resource.Error("Bus ID is missing. Cannot update."))
                return@flow
            }
            try {

                if (busAndDriver.password.isNotBlank()) {
                    val data = hashMapOf(
                        "email" to busAndDriver.email.trim(),
                        "newPassword" to busAndDriver.password
                    )
                    // Call cloud function and wait for it's result
                    try {
                        // Call cloud function and wait for its result
                        val functions = FirebaseFunctions.getInstance("asia-southeast1")
                        val result = functions.getHttpsCallable("updateDriverPassword")
                            .call(data)
                            .await()

                    } catch (e: Exception) {
                        emit(Resource.Error(e.message ?: "Couldn't update password"))
                        return@flow
                    }

                }
                val busDoc = fireStore.collection("buses").document(busAndDriver.busId)

                val updatedBusData = mapOf(
                    "busId" to busAndDriver.busId,
                    "email" to busAndDriver.email,
                    "driverName" to busAndDriver.driverName,
                    "busName" to busAndDriver.busId,
                    "routes" to busAndDriver.routes
                )
                busDoc.update(updatedBusData).await()
                emit(Resource.Success("Updated successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error(e.localizedMessage ?: "Something went wrong"))
            }
        }
    }

    override suspend fun deleteBus(busId: String): Flow<Resource<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun addCaretaker(caretaker: Caretaker): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {

                val snapshot =
                    fireStore.collection("caretakers").document(caretaker.id).get().await()
                if (snapshot.exists()) {
                    emit(Resource.Error("Already has account"))
                    return@flow
                } else {
                    // ----- Check if email already exists -----
                    val isEmailExists =
                        firebaseAuth.fetchSignInMethodsForEmail(caretaker.email).await()
                    if (isEmailExists.signInMethods?.isNotEmpty() == true) {
                        emit(Resource.Error("Email already exists"))
                        return@flow
                    }
                }

                // ----- Create account -----
                val caretakerAuth =
                    firebaseAuth.createUserWithEmailAndPassword(caretaker.email, caretaker.password)
                        .await()
                val uid = caretakerAuth.user?.uid


                if (uid != null) {
                    val doc = fireStore.collection("caretakers").document(uid)
                    val data = mapOf(
                        "id" to uid,
                        "name" to caretaker.name,
                        "email" to caretaker.email,
                        "assignedBusId" to caretaker.assignedBusId
                    )
                    val usersDoc = fireStore.collection("users").document(uid)
                    val userData = mapOf(
                        "email" to caretaker.email,
                        "role" to "caretaker"
                    )
                    doc.set(data)
                    usersDoc.set(userData)
                    emit(Resource.Success("Added successfully"))
                } else {
                    emit(Resource.Error("Something went wrong."))
                    return@flow
                }

            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override suspend fun getAllCaretakers(): Flow<Resource<List<Caretaker>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val caretakerSnapshot =
                    fireStore.collection("caretakers").get().await()

                val caretakerList = withContext(Dispatchers.Default) {
                    caretakerSnapshot.documents.mapNotNull { caretakerDoc ->
                        try {
                            Caretaker(
                                id = caretakerDoc.id,
                                name = caretakerDoc.getString("name") ?: "No name",
                                email = caretakerDoc.getString("email") ?: "No email found",
                                password = "",
                                assignedBusId = caretakerDoc.getString("assignedBusId")
                                    ?: "not found",
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                emit(Resource.Success(caretakerList))
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    override suspend fun updateCaretaker(caretaker: Caretaker): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            try {

                if (caretaker.password.isNotBlank()) {
                    val data = hashMapOf(
                        "email" to caretaker.email.trim(),
                        "newPassword" to caretaker.password
                    )
                    // Call cloud function and wait for it's result
//                    try {
//                        // Call cloud function and wait for its result
//                        val functions = FirebaseFunctions.getInstance("asia-southeast1")
//                        val result = functions.getHttpsCallable("updateDriverPassword")
//                            .call(data)
//                            .await()
//
//                    } catch (e: Exception) {
//                        emit(Resource.Error(e.message ?: "Couldn't update password"))
//                        return@flow
//                    }

                }

                val doc = fireStore.collection("caretakers").document(caretaker.id)
                val updatedData = mapOf(
                    "name" to caretaker.name,
//                    "email" to caretaker.email,
                    "assignedBusId" to caretaker.assignedBusId
                )
                doc.update(updatedData).await()
                emit(Resource.Success("Not implemented yet"))
                return@flow
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun reAuthenticateAdmin(password: String): Boolean {
        val user = firebaseAuth.currentUser
        val email = user?.email
        val credentials = EmailAuthProvider.getCredential(email ?: "", password)
        return try {
            user?.reauthenticate(credentials)?.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}


