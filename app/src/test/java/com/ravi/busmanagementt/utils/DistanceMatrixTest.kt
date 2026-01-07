package com.ravi.busmanagementt.utils


import org.junit.Test
import com.google.common.truth.Truth.assertThat
import com.google.maps.model.LatLng
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.presentation.home.calculateDistance
import java.util.concurrent.TimeUnit

class DistanceMatrixTest {


    @Test
    fun `isArrived returns true when within threshold`() {
        val stopLocation = LatLng(12.9716, 77.5946) // Bangalore coords
        val busLocation = LatLng(12.9716, 77.5947) // Very close (approx 11 meters away)
        val threshold = 20
        val result = isArrived(busLocation, stopLocation, threshold)
        assertThat(result).isTrue()
    }

    @Test
    fun `isArrived returns false when bus is too far`() {
        val stopLocation = LatLng(12.9716, 77.5946)
        val busLocation = LatLng(13.0000, 77.5946) // Far away
        val threshold = 50
        val result = isArrived(busLocation, stopLocation, threshold)
        assertThat(result).isFalse()
    }

    @Test
    fun `formatEta returns correct string for various minutes`() {
        // Given
        val minutesSmall = 5
        val minutesLarge = 65

        // When
        val resultSmall = TimeMatrix.formatEtaForUi(minutesSmall)
        val resultLarge = TimeMatrix.formatEtaForUi(minutesLarge)

        assertThat(resultSmall).isEqualTo("5 mins")
        assertThat(resultLarge).isEqualTo("1 hr 5 mins")
    }

    @Test
    fun `formatEta returns clean string for exact hours`() {
        val result = TimeMatrix.formatEtaForUi(60)
        val expected = "1 hr"
        assertThat(result).isEqualTo(expected)
        val result2 = TimeMatrix.formatEtaForUi(120)
        val expected2 = "2 hr"
        assertThat(result2).isEqualTo(expected2)
    }

    @Test
    fun `isDistanceInRange returns true when within range 50m`() {
        val point1 = LatLng(12.9716, 77.5946)
        val point2 = LatLng(12.9716, 77.5947)
        val result = isDistanceInRange(point1, point2, 50)
        assertThat(result).isTrue()
    }

    @Test
    fun `isDistanceInRange returns true when points are equal`() {
        val point1 = LatLng(12.9716, 77.5946)
        val point2 = LatLng(12.9716, 77.5947)
        val result = isDistanceInRange(point1, point2, 50)
        assertThat(result).isTrue()
    }

    @Test
    fun `isDistanceInRange returns false when distance is greater than range`() {
        val point1 = LatLng(12.9716, 77.5946)
        val point2 = LatLng(12.9716, 77.5999)
        val result = isDistanceInRange(point1, point2, 50)
        assertThat(result).isFalse()
    }


    private fun isArrived(
        busLocation: LatLng,
        stopLocation: LatLng,
        threshold: Int
    ): Boolean {
        val distance = calculateDistance(
            busLocation.lat,
            busLocation.lng,
            stopLocation.lat,
            stopLocation.lng
        )
        return distance <= threshold
    }

    private fun isDistanceInRange(point1: LatLng, point2: LatLng, range: Int = 50): Boolean {
        val distance =
            calculateDistance(
                point1.lat,
                point1.lng,
                point2.lat,
                point2.lng,
            )
        return distance <= range
    }


}