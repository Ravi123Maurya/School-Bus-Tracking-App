package com.ravi.busmanagementt.testing

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.presentation.home.calculateDistance
import com.ravi.busmanagementt.utils.DistanceMatrix
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationSimulator {

    fun simulateDrivingRoute(
        path: List<LatLng>,
        speedKmph: Double = 40.0,
        updateIntervalMillis: Long = 1000L,
    ): Flow<Location> = flow {

        if (path.size < 2) return@flow

        var currentPointIndex = 0
        var currentLat = path[0].latitude
        var currentLng = path[0].longitude

        emit(createLocation(currentLat, currentLng, 0f))

        while (currentPointIndex < path.size - 1) {
            val start = LatLng(currentLat, currentLng)
            val end = path[currentPointIndex + 1]

            val distanceMeters = calculateDistance(start, end)
            val speedMps = speedKmph * 0.27778
            val stepDistance = speedMps * (updateIntervalMillis / 1000.0)

            if (distanceMeters <= stepDistance) {
                currentLat = end.latitude
                currentLng = end.longitude
                currentPointIndex++
            } else {
                val bearing = calculateBearing(start, end)
                val newPoint = movePoint(start, bearing, stepDistance)
                currentLat = newPoint.latitude
                currentLng = newPoint.longitude
            }

            val location = createLocation(currentLat, currentLng, calculateBearing(start, end))
            emit(location)
            delay(updateIntervalMillis)
        }

    }

    private fun createLocation(
        latitude: Double,
        longitude: Double,
        bearing: Float,
    ): Location {
        val location = Location("Simulator")
        location.latitude = latitude
        location.longitude = longitude
        location.bearing = bearing
        location.time = System.currentTimeMillis()
        return location
    }

    private fun calculateDistance(
        start: LatLng,
        end: LatLng
    ): Double {
        val r = 6371000 // Earth radius in meters
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    
    
    private fun calculateBearing(
        start: LatLng,
        end: LatLng
    ) : Float{
        val startLat = Math.toRadians(start.latitude)
        val startLong = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLong = Math.toRadians(end.longitude)
        val dLong = endLong - startLong
        val dPhi = Math.log(Math.tan(endLat / 2.0 + Math.PI / 4.0) / Math.tan(startLat / 2.0 + Math.PI / 4.0))
        if(abs(dLong) > Math.PI){
            if(dLong > 0.0){
                -(2.0 * Math.PI - dLong)
            } else {
                (2.0 * Math.PI + dLong)
            }
        }
        val bearing = (Math.toDegrees(atan2(dLong, dPhi)) + 360.0) % 360.0
        return bearing.toFloat()
    }

    private fun movePoint(start: LatLng, bearing: Float, distanceMeters: Double): LatLng {
        val r = 6371000.0
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val brng = Math.toRadians(bearing.toDouble())

        val lat2 = Math.asin(
            sin(lat1) * cos(distanceMeters / r) +
                    cos(lat1) * sin(distanceMeters / r) * cos(brng)
        )
        val lon2 = lon1 + atan2(
            sin(brng) * sin(distanceMeters / r) * cos(lat1),
            cos(distanceMeters / r) - sin(lat1) * sin(lat2)
        )

        return LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }


} 