package com.ravi.busmanagementt.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ravi.busmanagementt.domain.model.Attendance
import com.ravi.busmanagementt.domain.model.Ride
import com.ravi.busmanagementt.domain.repository.CaretakerRepository
import com.ravi.busmanagementt.presentation.home.caretaker.Child
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Scanner
import javax.inject.Inject


class CaretakerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CaretakerRepository {

    override suspend fun getStudentsForCaretaker(): Flow<Resource<List<Child>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) return@flow

                val busId = firestore.collection("caretakers")
                    .document(uid)
                    .get().await().getString("assignedBusId")

                val studentsDoc = firestore.collection("students")
                    .whereEqualTo("assignedBusId", busId)
                    .get().await()


                val students = withContext(Dispatchers.Default) {
                    coroutineScope {
                        studentsDoc.documents.map { student ->

                            async(Dispatchers.IO) {
                                val attendance = try {
                                    val attDoc = firestore.collection("students")
                                        .document(student.id)
                                        .collection("attendance")
                                        .document(Attendance.dateFormat)
                                        .get().await()
                                    val pickupMap = attDoc.get("pickup") as? Map<String, Any>
                                    val dropMap = attDoc.get("drop") as? Map<String, Any>
                                    Log.d(
                                        "AttendanceTest",
                                        "pickupMap: $pickupMap - dropMap: $dropMap"
                                    )

                                    Attendance(
                                        date = attDoc.id, // use docId as date
                                        pickup = pickupMap?.let {
                                            Ride(
                                                status = it["status"] as? String ?: "",
                                                timestamp = it["timestamp"] as? Long ?: 0L,
                                            )
                                        },
                                        drop = dropMap?.let {
                                            Ride(
                                                status = it["status"] as? String ?: "",
                                                timestamp = it["timestamp"] as? Long ?: 0L,
                                            )
                                        }
                                    )
                                } catch (e: Exception) {
                                    null
                                }


                                Child(
                                    id = student.id,
                                    name = student.getString("name") ?: "null",
                                    age = -1,
                                    grade = student.getString("std") ?: "N/A",
                                    parentName = student.getString("parentName") ?: "null",
                                    parentContact = student.getString("parentContact") ?: "null",
                                    busNumber = student.getString("assignedBusId"),
                                    todayRideAttendance = attendance
                                )
                            }

                        }.awaitAll()

                    }
                }
                emit(Resource.Success(students))

            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Something went wrong."))
            }
        }
    }

    override suspend fun markAttendance(
        studentId: String,
        status: String,
        isPickup: Boolean?
    ): Flow<Resource<String>> {

        return flow {
            if (isPickup == null) {
                emit(Resource.Error("Select ride type first!"))
                return@flow
            }
            emit(Resource.Loading())
            try {
                val attendanceDocumentRef = firestore
                    .collection("students")
                    .document(studentId).collection("attendance").document(Attendance.dateFormat)

                val attendance = if (isPickup) {
                    Attendance(pickup = Ride(status), drop = null)
                } else {
                    val pickup = try {
                        val attendanceDocument = attendanceDocumentRef
                            .get().await()
                        val pickupMap = attendanceDocument.get("pickup") as? Map<String, Any>
                        pickupMap?.let {
                            Ride(
                                status = it["status"] as? String ?: "",
                                timestamp = it["timestamp"] as? Long ?: 0L
                            )
                        }

                    } catch (e: Exception) {
                        null
                    }
                    Attendance(pickup = pickup, drop = Ride(status))

                }


                val data = mapOf(
                    "pickup" to attendance.pickup,
                    "drop" to attendance.drop
                )
                // Set the attendance status
                attendanceDocumentRef.set(data).await()
                emit(Resource.Success("Status marked"))
            } catch (e: Exception) {
                emit(Resource.Error("Something went wrong, ${e.message}"))
            }
        }

    }
}

