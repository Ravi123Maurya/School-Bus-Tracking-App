package com.ravi.busmanagementt.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ravi.busmanagementt.domain.repository.CaretakerRepository
import com.ravi.busmanagementt.presentation.home.caretaker.Child
import com.ravi.busmanagementt.utils.Resource
import kotlinx.coroutines.Dispatchers
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

    override suspend fun getStudentsOfBus(): Flow<Resource<List<Child>>> {
        return flow {
            Log.d("GetStudent", "Just Entered")
            emit(Resource.Loading())
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) return@flow
                Log.d("GetStudent", "Just Entered")

                val busId = firestore.collection("caretakers")
                    .document(uid)
                    .get().await().getString("assignedBusId")

                val studentsDoc = firestore.collection("attendance")
                    .whereEqualTo("assignedBusId", busId)
                    .get().await()

                val students = withContext(Dispatchers.Default) {
                    studentsDoc.documents.map { student ->
                        Child(
                            id = student.getString("studentId") ?: "null",
                            name = student.getString("studentId") ?: "null",
                            age = -1,
                            grade = "-1",
                            parentName = student.getString("parentName") ?: "null",
                            parentContact = student.getString("parentContact") ?: "null",
                            busNumber = busId,
                        )
                    }

                }
                Log.d("GetStudent", "Success - ${students.size}")
                emit(Resource.Success(students))

            } catch (e: Exception) {
                Log.d("GetStudent", "Exception - ${e.message}")
                emit(Resource.Error(e.message ?: "Something went wrong."))
            }
        }
    }

    override suspend fun markAttendance(studentId: String, status: String): Flow<Resource<String>> {

        return flow {
            emit(Resource.Loading())

            try {
                Log.d("ATTEND", "Just Entered repo")
                val attendanceDocument = firestore
                    .collection("attendance")
                    .document(studentId)


                val data = mapOf(
                    "studentId" to studentId,
                    "status" to status
                )
                // Update the attendance status
                attendanceDocument.update(data).await()
                Log.d("ATTEND", "Success")
                emit(Resource.Success("Status marked"))
            } catch (e: Exception) {
                Log.d("ATTEND", "Error - ${e.message}")
                emit(Resource.Error("Something went wrong, ${e.message}"))
            }
        }

    }
}

