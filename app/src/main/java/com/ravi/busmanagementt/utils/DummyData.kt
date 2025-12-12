package com.ravi.busmanagementt.utils

import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.presentation.home.BusStop
import com.ravi.busmanagementt.presentation.home.StopStatus

data class BusStop(
    val id: Int,
    val stopName: String,
    val coordinates: LatLng,
    val busId: String
)

data class Bus(
    val id: String,
    val name: String,
    val stops: List<BusStop>
)


val dummyBusStops = listOf(
    BusStop(1, "Lulumall", LatLng(19.184176, 73.042684),"bus1"),
    BusStop(2, "Pulumall", LatLng(19.167085,73.046446),"bus1"),
    BusStop(3, "Bubumall", LatLng(19.162585, 73.045997),"bus1"),
    BusStop(4, "Dudumall", LatLng(19.161234, 73.046667),"bus1"),
)



val sampleStops = listOf(
        BusStop(1, "Global School", "11:00 AM", "11:20 AM", StopStatus.REACHED),
        BusStop(2, "City Center Mall", "11:30 AM", status = StopStatus.CURRENT),
        BusStop(3, "Park Avenue", "11:45 AM", status = StopStatus.PENDING),
        BusStop(4, "Railway Station", "12:00 PM", status = StopStatus.PENDING),
        BusStop(5, "Airport Road", "12:15 PM", status = StopStatus.PENDING),
        BusStop(6, "Final Destination", "12:30 PM", status = StopStatus.PENDING)
    )

