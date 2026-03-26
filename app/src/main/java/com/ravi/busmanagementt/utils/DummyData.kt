package com.ravi.busmanagementt.utils

import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.presentation.home.BusStop

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
