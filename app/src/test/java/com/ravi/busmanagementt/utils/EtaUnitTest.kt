package com.ravi.busmanagementt.utils

import com.google.common.truth.Truth.assertThat
import com.google.maps.model.LatLng
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.presentation.home.calculateDistance
import org.junit.Test
import java.util.concurrent.TimeUnit

class EtaUnitTest {

    private val now = System.currentTimeMillis()
    private val dummyBusPath = listOf<RealtimeLocation>(
        RealtimeLocation(latitude = 12.0000, longitude = 73.0000, timestamp = (now - 30000).toString()),
        RealtimeLocation(latitude = 12.0015, longitude = 73.0000, timestamp = (now - 15000).toString()),
        RealtimeLocation(latitude = 12.0030, longitude = 73.0000, timestamp = now.toString())
    )

    @Test
    fun `calculateEta returns correct ETA for 3 points`() {
        val now = System.currentTimeMillis()

        // Bus p1 30s ago moving at approx 11 meters per second (~40km/h)
        val p1 = RealtimeLocation(
            latitude = 12.0000,
            longitude = 73.0000,
            timestamp = (now - 30000).toString()
        )

        // Bus p2 15s ago - moved ~165 meters north
        val p2 = RealtimeLocation(
            latitude = 12.0015,
            longitude = 73.0000,
            timestamp = (now - 15000).toString()
        )

        // Bus now - moved ~165 meters north
        val p3 = RealtimeLocation(
            latitude = 12.0030,
            longitude = 73.0000,
            timestamp = now.toString()
        )

        val dummyBusPath = listOf(p1, p2, p3)
        val stopLocation = LatLng(12.0330, 73.000)
        val eta = calculateScheduleTimeETA(dummyBusPath, stopLocation)

        assertThat(eta).isEqualTo(4)
    }

    @Test
    fun `calculateETa returns 0 when bus is stationary (speed too low)`(){
        val now = System.currentTimeMillis()
        val stopLocation = LatLng(12.0330,73.000)

        // Bus staying at the exact same spot for 3 updates
        val stationaryPath = listOf(
            RealtimeLocation(12.0000, 73.0000, (now - 30000).toString()),
            RealtimeLocation(12.0000, 73.0000, (now - 15000).toString()),
            RealtimeLocation(12.0000, 73.0000, now.toString())
        )

        val eta = calculateScheduleTimeETA(stationaryPath, stopLocation)

        // Should return 0 because speed is < 1.0 m/s
        assertThat(eta).isEqualTo(0)
    }

    @Test
    fun `calculateEta returns 0 when less than 3 updates`(){
        val now = System.currentTimeMillis()
        val stopLocation = LatLng(12.0330,73.000)
        val eta = calculateScheduleTimeETA(dummyBusPath.take(2), stopLocation)
        assertThat(eta).isEqualTo(0)
    }

    @Test
    fun `calculateEta returns 0 when no location history (empty list)`(){
        val stopLocation = LatLng(12.0330,73.000)
        val eta = calculateScheduleTimeETA(emptyList(), stopLocation)
        assertThat(eta).isEqualTo(0)
    }


    private fun calculateScheduleTimeETA(
        realtimeLocations: List<RealtimeLocation>,
        stopLocation: LatLng
    ): Int {

//        Log.d("ETA", "ETA: RTL Size ${realtimeLocations.size} --- StopLocation: $stopLocation")

        if (realtimeLocations.size < 3) {
//            Log.d("ETA", "Returning... RealtimeLocations: ${realtimeLocations.size} is less than 3")
            return 0
        }

        val threeMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3)

        val recentFilteredLocations =
            realtimeLocations.filter { it.timestamp.toLong() > threeMinutesAgo }
                .sortedBy { it.timestamp.toLong() }

        if (recentFilteredLocations.size < 3) {
//            Log.d("ETA", "Returning... Recent Filtered Locations: ${recentFilteredLocations.size} is less than 3")
            return 0
        }

        val totalDistanceTravelledInMeters = recentFilteredLocations.zipWithNext { a, b ->
            val point1 = LatLng(a.latitude, a.longitude)
            val point2 = LatLng(b.latitude, b.longitude)
            calculateDistance(point1.lat, point1.lng, point2.lat, point2.lng)
        }.sum()

        val totalTimeTakenMillis =
            recentFilteredLocations.last().timestamp.toLong() - recentFilteredLocations.first().timestamp.toLong()
//        Log.d("ETA", "Total Distance: $totalDistanceTravelledInMeters and TotalTimeTaken: $totalTimeTakenMillis")
        if (totalTimeTakenMillis <= 0) {
//            Log.d("ETA", "Returning... TotalTimeTaken: $totalTimeTakenMillis is less than 0 or equal to 0")
            return 0
        }

        val averageSpeed = totalDistanceTravelledInMeters / (totalTimeTakenMillis / 1000.0)
//        Log.d("ETA", "Average Speed: $averageSpeed")
        if (averageSpeed < 1.0) {
//            Log.d("ETA", "Returning... Average Speed: $averageSpeed is less than 1.0")
            return 0
        }

//        Log.d("ETA", "All checks passed")
        val currentLocationPoint = recentFilteredLocations.last()
        val currentLocation = LatLng(currentLocationPoint.latitude, currentLocationPoint.longitude)

        val distanceToStop = calculateDistance(
            currentLocation.lat,
            currentLocation.lng,
            stopLocation.lat,
            stopLocation.lng
        )

        val etaInSeconds = distanceToStop / averageSpeed
//        Log.d("ETA", "ETA in Seconds: $etaInSeconds returning eta...")
        val etaMinutes = TimeUnit.SECONDS.toMinutes(etaInSeconds.toLong()).toInt()
        return if (etaMinutes == 0 && etaInSeconds > 0) 1 else etaMinutes
    }
}