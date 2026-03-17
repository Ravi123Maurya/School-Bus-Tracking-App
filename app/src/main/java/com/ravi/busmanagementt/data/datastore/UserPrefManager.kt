package com.ravi.busmanagementt.data.datastore

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPrefManager @Inject constructor(
    @ApplicationContext private val context: Context
) {


    // Store the user's bus ID in DataStore
    suspend fun setBusId(busId: String?) {
        context.userDatastore.edit { preferences ->
            if (busId == null) {
                preferences.remove(BUS_ID_KEY)
            } else
                preferences[BUS_ID_KEY] = busId
        }
    }

    // Retrieve the user's bus ID from DataStore
    fun getBusId(): Flow<String?> = context.userDatastore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[BUS_ID_KEY]
        }


    suspend fun setRideType(isPickup: Boolean?) {
        context.userDatastore.edit { preferences ->
            if (isPickup == null) preferences.remove(RIDE_TYPE_KEY)
            else preferences[RIDE_TYPE_KEY] = isPickup
        }
    }

    fun getRideType(): Flow<Boolean?> {
        return context.userDatastore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[RIDE_TYPE_KEY]
            }
    }

    suspend fun setCurrentAttendanceDate(date: String){
        context.userDatastore.edit { preferences ->
            preferences[ATTENDANCE_DATE_KEY] = date
        }
    }
    fun getCurrentAttendanceDate(): Flow<String?>  {
       return context.userDatastore.data
           .catch {
               if (it is IOException) {
                   emit(emptyPreferences())
               } else {
                   throw it
               }
           }
            .map {preferences ->
                preferences[ATTENDANCE_DATE_KEY]
            }
    }
}