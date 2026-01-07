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

/**
 * Utility object for handling time-related operations.
 * Provides methods for formatting timestamps and ETA durations.
 */
object TimeMatrix {

    /**
     * Formats a timestamp (in milliseconds) into a readable date/time string.
     *
     * @param timestamp The timestamp in milliseconds to format.
     * @param format The pattern to use for formatting. Defaults to "dd/MM/yyyy h:mm a".
     *               Examples:
     *               - "h:mm a" -> 10:30 AM
     *               - "HH:mm:ss" -> 22:30:05
     *               - "dd/MM/yyyy h:mm a" -> 15/11/2025 10:30 AM
     * @return A formatted string representation of the timestamp.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTime::class)
    fun formatTimestampToReadableTime(
        timestamp: Long,
        format: String = "dd/MM/yyyy h:mm a"
    ): String {
        // An Instant represents a single moment on the timeline in UTC.
        val instant: Instant? = Instant.ofEpochMilli(timestamp)

        // A formatter defines how the time will be displayed.
        val formatter = DateTimeFormatter.ofPattern(format)

        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        return formatter.format(localDateTime)
    }

    /**
     * Formats an estimated time of arrival (ETA) in minutes into a user-friendly string.
     *
     * @param minutes The duration in minutes.
     * @return A string representation of the duration (e.g., "45 mins", "1 hr", "1 hr 30 mins").
     */
    fun formatEtaForUi(minutes: Int): String{
        if(minutes < 60) return "$minutes mins"
        val hrs = minutes / 60
        val mins = minutes % 60
        return if(mins == 0) "$hrs hr" else "$hrs hr $mins mins"
    }


}

/**
 * Utility object for handling distance and location-related operations.
 * Uses SphericalUtil to compute distances between coordinates.
 */
object DistanceMatrix {

    /**
     * Checks if the distance between two points is within a specified range.
     *
     * @param point1 The first LatLng point.
     * @param point2 The second LatLng point.
     * @param range The maximum allowed distance in meters. Defaults to 50 meters.
     * @return True if the distance is less than or equal to the range, false otherwise.
     */
    fun isDistanceInRange(point1: LatLng, point2: LatLng, range: Int = 50): Boolean {
        val distance = SphericalUtil.computeDistanceBetween(point1, point2)
        return distance <= range
    }

    /**
     * Calculates the distance between two points in meters.
     *
     * @param point1 The first LatLng point.
     * @param point2 The second LatLng point.
     * @return The distance between the points in meters.
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Int {
        return SphericalUtil.computeDistanceBetween(point1, point2).toInt()
    }

    /**
     * Calculates the Estimated Time of Arrival (ETA) to a specific stop location based on recent realtime locations.
     *
     * It filters locations from the last 3 minutes, calculates the total distance travelled and total time taken
     * to determine the average speed. Using this speed and the distance to the stop, it estimates the arrival time.
     *
     * @param realtimeLocations A list of [RealtimeLocation] objects representing the bus's recent movements.
     * @param stopLocation The [LatLng] of the destination stop.
     * @return The estimated time of arrival in minutes. Returns 0 if there isn't enough data or speed is too low.
     */
    fun calculateScheduleTimeETA(
        realtimeLocations: List<RealtimeLocation>,
        stopLocation: LatLng
    ): Int {

        if (realtimeLocations.size < 3){
            return 0
        }

        val threeMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3)

        val recentFilteredLocations = realtimeLocations.filter { it.timestamp.toLong() > threeMinutesAgo }.sortedBy { it.timestamp.toLong() }

        if (recentFilteredLocations.size < 3){
            return 0
        }

        val totalDistanceTravelledInMeters = recentFilteredLocations.zipWithNext{a, b ->
            val point1 = LatLng(a.latitude, a.longitude)
            val point2 = LatLng(b.latitude, b.longitude)
            SphericalUtil.computeDistanceBetween(point1, point2)
        }.sum()

        val totalTimeTakenMillis = recentFilteredLocations.last().timestamp.toLong() - recentFilteredLocations.first().timestamp.toLong()
        if(totalTimeTakenMillis <= 0 ){
            return 0
        }

        val averageSpeed = totalDistanceTravelledInMeters / (totalTimeTakenMillis/1000.0)
        if (averageSpeed < 1.0){
            return 0
        }

        val currentLocationPoint = recentFilteredLocations.last()
        val currentLocation = LatLng(currentLocationPoint.latitude, currentLocationPoint.longitude)

        val distanceToStop = SphericalUtil.computeDistanceBetween(currentLocation, stopLocation)

        val etaInSeconds = distanceToStop / averageSpeed
        val etaMinutes = TimeUnit.SECONDS.toMinutes(etaInSeconds.toLong()).toInt()
        return if (etaMinutes == 0 && etaInSeconds > 0) 1 else etaMinutes
    }

}



