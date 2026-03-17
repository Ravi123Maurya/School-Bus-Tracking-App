package com.ravi.busmanagementt.domain.model

import com.ravi.busmanagementt.utils.TimeMatrix
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

data class Student(
    val id: String,
    val name: String,
    val std: String,
    val parentName: String,
    val assignedBusId: String,
    val attendanceList: List<Attendance>? = null
)


data class Attendance(
    var date: String = this.dateFormat,
    val pickup: Ride? = null,
    val drop: Ride? = null,
) {
    companion object {
        val now = System.currentTimeMillis()
        val dateFormat = TimeMatrix.formatTimestampToReadableTime(now, "YYYY-MM-DD")
    }
}

data class Ride(
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun main() = runBlocking {
   val date = LocalDate.now()
    println("Date : $date")
}