package com.ravi.busmanagementt.domain.model

import com.google.firebase.firestore.GeoPoint



data class BusAndDriver(
    val id: Int,
    val driverName: String,
    val email: String,
    val password: String,
    val busId: String,
    val routes: List<BusStop> = emptyList()
)


data class BusStop(
    val stopName: String = "",
    val location: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
)