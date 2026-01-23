package com.ravi.busmanagementt.domain.model

data class Caretaker(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val assignedBusId: String,
)