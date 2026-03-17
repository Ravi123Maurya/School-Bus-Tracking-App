package com.ravi.busmanagementt.data.serivce

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.data.datastore.UserPrefManager
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import com.ravi.busmanagementt.utils.DistanceMatrix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


// This service is for driver only - driver click the start button to start the service ( AlertDialog in HomeScreen)
// Drivers won't get their current location from  viewModel all the time
// but Other users(parents, caretaker, admin) will get location from viewModel using startLocationUpdates fun in LocationRepository

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var realtimeLocationRepository: RealtimeLocationRepository

    @Inject
    lateinit var userPrefManager: UserPrefManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firestoreBusRepository: FirestoreBusRepository

    @Inject
    lateinit var locationSharingStateManager: LocationSharingStateManager
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPushedLocation: Location? = null
    private var busId: String? = null
    private var numberOfStopsReached = 0


    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun start() {
        locationSharingStateManager.setSharingState(LocationSharingState.LOADING)
        lastPushedLocation = null
        serviceScope.launch {
            busId = getBusId()
            if (busId == null) {
                stop()
                return@launch
            }

            val busStops = firestoreBusRepository.getBusRouteStops(busId!!).firstOrNull() ?: emptyList()

            withContext(Dispatchers.Main) {
                val notification =
                    NotificationCompat.Builder(this@LocationService, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Sharing Bus Location...")
                        .setContentText("Location is active in the background")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setOngoing(true)
                val notificationManager = NotificationManagerCompat.from(this@LocationService)
                createNotificationChannel(notificationManager)
                startForeground(NOTIFICATION_ID, notification.build())
            }
            busId?.let {
                realtimeLocationRepository.setBusIsLive(
                    it,
                    true
                )
            }
            startLocationUpdates(busStops)
            locationSharingStateManager.setSharingState(LocationSharingState.SHARING)

        }
        Log.d("MyLocationService", "Location Sharing Started")
    }

    private fun startLocationUpdates(busStops: List<com.ravi.busmanagementt.domain.model.BusStop>) {
        locationSharingStateManager.locationPoints.value = emptyList()
        realtimeLocationRepository.startLocationUpdates()
            .onEach { location ->
                if (locationSharingStateManager.sharingState.value == LocationSharingState.SHARING) {
                    val currentBusId = busId ?: return@onEach
                    if (doesPassLocationFilters(location)) {
                        serviceScope.launch {
                            locationSharingStateManager.locationPoints.value =
                                locationSharingStateManager.locationPoints.value + LatLng(
                                    location.latitude,
                                    location.longitude
                                )

                            if (busStops.isNotEmpty() && numberOfStopsReached < busStops.size) {
                                val hasStopReached = DistanceMatrix.isDistanceInRange(
                                    LatLng(location.latitude, location.longitude),
                                    LatLng(
                                        busStops[numberOfStopsReached].geoPoint.latitude,
                                        busStops[numberOfStopsReached].geoPoint.longitude
                                    ),
                                    60 // todo: make the range smaller in real testing:  (50-60m)
                                )
                                val distance = DistanceMatrix.calculateDistance(LatLng(location.latitude, location.longitude),
                                    LatLng(
                                        busStops[numberOfStopsReached].geoPoint.latitude,
                                        busStops[numberOfStopsReached].geoPoint.longitude
                                    ))
                                if (hasStopReached) {
                                    numberOfStopsReached++
                                }
                            }

                            realtimeLocationRepository.pushLocationToFRTD(currentBusId, location, numberOfStopsReached)
                            lastPushedLocation = location
                        }
                    }
                }
            }
            .catch { e ->
                stop()
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        serviceScope.launch {
            busId?.let {
                realtimeLocationRepository.setBusIsLive(it, false)
            }
        }
        locationSharingStateManager.setSharingState(LocationSharingState.IDLE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("MyLocationService", "Location Sharing Stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        locationSharingStateManager.setSharingState(LocationSharingState.IDLE)
        serviceScope.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManagerCompat) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Sharing",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    // -------- ----------- ------------ //


    private fun doesPassLocationFilters(location: Location): Boolean {
        Log.d("LocationFilter", "---------------------------------")
        Log.d("LocationFilter", "New Location: Lat=${location.latitude}, Lon=${location.longitude}, Acc=${location.accuracy}, Spd=${location.speed}")

        // The very first location update should always be accepted to initialize our state.
        if (lastPushedLocation == null) {
            Log.d("LocationFilter", "PASSED: First location.")
            return true
        }

        val lastLocation = lastPushedLocation!!
        val timeDelta = (location.time - lastLocation.time) / 1000.0 // Time difference in seconds
        val distanceDelta = location.distanceTo(lastLocation) // Distance difference in meters

        Log.d("LocationFilter", "Comparing with: Lat=${lastLocation.latitude}, Lon=${lastLocation.longitude}, Acc=${lastLocation.accuracy}")
        Log.d("LocationFilter", "Time Delta: ${timeDelta}s, Distance Delta: ${distanceDelta}m")


        // Filter 1: Accuracy is too poor.
        // If the accuracy circle is larger than 40 meters, the point is unreliable.
        if (location.accuracy > 40.0f) {
            Log.d("LocationFilter", "REJECTED: Poor accuracy (${location.accuracy}m > 40m).")
            return false
        }

        // Filter 2: The device is likely stationary.
        // If the device has moved less than a few meters, it's likely just GPS jitter.
        // We only accept the point if it's been a while, to provide periodic updates even when stopped.
        if (distanceDelta < 5.0f) {
            if (timeDelta < 10.0) { // If less than 10 seconds have passed
                Log.d("LocationFilter", "REJECTED: Stationary (distance < 5m and time < 10s).")
                return false
            } else {
                Log.d("LocationFilter", "PASSED: Stationary but updating after 10s timeout.")
            }
        }

        // Filter 3: Unrealistic speed.
        // This helps filter out sudden GPS jumps.
        if (timeDelta > 0) {
            val speed = distanceDelta / timeDelta // meters per second
            if (speed > 35) { // Roughly 126 km/h or 78 mph. A bus shouldn't jump faster than this.
                Log.d("LocationFilter", "REJECTED: Unrealistic speed (${speed} m/s).")
                return false
            }
        }

        Log.d("LocationFilter", "PASSED: All checks passed.")
        return true
    }
    private suspend fun getBusId(): String? {
        var currentBusId = userPrefManager.getBusId().firstOrNull()
        if (currentBusId == null) {
            val email = firebaseAuth.currentUser?.email
            if (email != null) {
                currentBusId = firestoreBusRepository.getBusIdForDriverFromEmail(email)
                userPrefManager.setBusId(currentBusId)
            }
        }
        return currentBusId
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "location"
    }
}