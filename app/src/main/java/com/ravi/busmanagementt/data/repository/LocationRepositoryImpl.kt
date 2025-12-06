package com.ravi.busmanagementt.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ravi.busmanagementt.data.datastore.UserPrefManager
import com.ravi.busmanagementt.data.serivce.LocationSharingStateManager
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


data class RealtimeLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: String = "",
    val numberOfStopsReached: Int = 0
)

class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val locationSharingStateManager: LocationSharingStateManager,
    private val userPrefManager: UserPrefManager
) : RealtimeLocationRepository {

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)


    override fun startLocationUpdates(): Flow<Location> {
        return callbackFlow {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                close(IllegalStateException("Location permission was not granted."))
                return@callbackFlow
            }

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            ).build()
            val locationCallback = object : LocationCallback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.locations.lastOrNull()?.let { location ->
                        trySend(location)
                    }
                }
            }

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Close the location access flow when the coroutine is cancelled or closed
            awaitClose {
//                locationDbRef.removeValue()
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }

        }
    }

    override fun getLocationUpdatesFromFRTD(busId: String): Flow<List<RealtimeLocation>> {
        return callbackFlow {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                close(IllegalStateException("User is not logged in."))
                return@callbackFlow
            }
            val locationDbRef = firebaseDatabase.getReference("liveLocations").child(busId)

            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val locations = snapshot.children.mapNotNull { dataSnapshot ->
                        dataSnapshot.getValue(RealtimeLocation::class.java)
                    }
                    trySend(locations)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            locationDbRef.addValueEventListener(valueEventListener)
            awaitClose {
                locationDbRef.removeEventListener(valueEventListener)
            }
        }

    }

    override suspend fun pushLocationToFRTD(
        busId: String,
        location: Location,
        numberOfStopsReached: Int
    ) {
        try {
            // Database Reference
            val locationDbRef = firebaseDatabase.getReference("liveLocations").child(busId)
            val locationData = RealtimeLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis().toString(),
                numberOfStopsReached = numberOfStopsReached
            )
            locationDbRef.push().setValue(locationData).await()


        } catch (e: Exception) {
            Log.d("LocationRepository", "Error: ${e.message}")
        }
    }

    override suspend fun setBusIsLive(busId: String, isLive: Boolean) {
        try {
            val realtimeDbRef = firebaseDatabase.getReference("Live Buses").child(busId)
            realtimeDbRef.setValue(isLive).await()

            val locationDbRef = firebaseDatabase.getReference("liveLocations").child(busId)
            locationDbRef.removeValue().await()

        } catch (e: Exception) {
            Log.d("LocationRepository", "Error: ${e.message}")
        }

    }

    override suspend fun stopLocationUpdates() {

    }

}