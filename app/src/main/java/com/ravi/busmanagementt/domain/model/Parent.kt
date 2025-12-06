package com.ravi.busmanagementt.domain.model

import com.google.firebase.firestore.GeoPoint

data class Parent(
    val parentId: String = "",
    val name: String,
    val email: String,
    val password: String,
    val assignedBusId: String,
    val busStopLocation: GeoPoint,
    val stopName: String = "",
    val location: String = ""
)