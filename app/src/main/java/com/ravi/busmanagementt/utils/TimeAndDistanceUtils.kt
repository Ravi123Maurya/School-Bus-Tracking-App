package com.ravi.busmanagementt.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.TimeUtils
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime


object TimeMatrix {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTime::class)
    fun formatTimestampToReadableTime(
        timestamp: Long,
        format: String = "dd/MM/yyyy h:mm a"
    ): String {
        // An Instant represents a single moment on the timeline in UTC.
        val instant: Instant? = Instant.ofEpochMilli(timestamp)

        // A formatter defines how the time will be displayed.
        // "h:mm a" -> 10:30 AM
        // "HH:mm:ss" -> 22:30:05
        // "dd/MM/yyyy h:mm a" -> 15/11/2025 10:30 AM
        val formatter = DateTimeFormatter.ofPattern(format)

        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        return formatter.format(localDateTime)
    }

    fun isTime1Older(time1: Long, time2: Long): Boolean = time1 < time2


}

object DistanceMatrix {

    fun isDistanceInRange(point1: LatLng, point2: LatLng, range: Int = 50): Boolean {
        val distance = SphericalUtil.computeDistanceBetween(point1, point2)
        return distance <= range
    }

    fun calculateDistance(point1: LatLng, point2: LatLng): Int {
        return SphericalUtil.computeDistanceBetween(point1, point2).toInt()
    }

    fun calculateScheduleTimeETA(
        realtimeLocations: List<RealtimeLocation>,
        stopLocation: LatLng
    ): Int {

        Log.d("ETA", "ETA: RTL Size ${realtimeLocations.size} --- StopLocation: $stopLocation")

        if (realtimeLocations.size < 3){
            Log.d("ETA", "Returning... RealtimeLocations: ${realtimeLocations.size} is less than 3")
            return 0
        }

        val threeMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3)

        val recentFilteredLocations = realtimeLocations.filter { it.timestamp.toLong() > threeMinutesAgo }.sortedByDescending { it.timestamp.toLong() }

        if (recentFilteredLocations.size < 3){
            Log.d("ETA", "Returning... Recent Filtered Locations: ${recentFilteredLocations.size} is less than 3")
            return 0
        }

        val totalDistanceTravelledInMeters = recentFilteredLocations.zipWithNext{a, b ->
            val point1 = LatLng(a.latitude, a.longitude)
            val point2 = LatLng(b.latitude, b.longitude)
            SphericalUtil.computeDistanceBetween(point1, point2)
        }.sum()
        val totalTimeTakenMillis = recentFilteredLocations.last().timestamp.toLong() - recentFilteredLocations.first().timestamp.toLong()
        Log.d("ETA", "Total Distance: $totalDistanceTravelledInMeters and TotalTimeTaken: $totalTimeTakenMillis")
        if(totalTimeTakenMillis <= 0 ){
            Log.d("ETA", "Returning... TotalTimeTaken: $totalTimeTakenMillis is less than 0 or equal to 0")
//            return 0
        }

        val averageSpeed = totalDistanceTravelledInMeters / totalTimeTakenMillis
        Log.d("ETA", "Average Speed: $averageSpeed")
        if (averageSpeed < 1.0){
            Log.d("ETA", "Returning... Average Speed: $averageSpeed is less than 1.0")
//            return 0
        }

        Log.d("ETA", "All checks passed")
        val currentLocationPoint = recentFilteredLocations.last()
        val currentLocation = LatLng(currentLocationPoint.latitude, currentLocationPoint.longitude)

        val distanceToStop = SphericalUtil.computeDistanceBetween(currentLocation, stopLocation)

        val etaInSeconds = distanceToStop / averageSpeed
        Log.d("ETA", "ETA in Seconds: $etaInSeconds returning eta...")
        return TimeUnit.SECONDS.toMinutes(etaInSeconds.toLong()).toInt()

    }

}