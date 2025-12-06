package com.ravi.busmanagementt.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Portals(val value: String) {
    DRIVER("Driver"),
    PARENT("Parent"),
    CARETAKER("Caretaker"),
    ADMIN("Admin")
}

object PortalManager {

    suspend fun setPortal(context: Context, portal: Portals) {
        context.dataStore.edit { settings ->
            settings[ACC_PORTAL_KEY] = portal.value
        }
    }

    fun getPortal(context: Context): Flow<String> {
        val preferences = context.dataStore.data.map { preferences ->
            preferences[ACC_PORTAL_KEY] ?: ""
        }
        return preferences
    }

    suspend fun setParentBusStopLocation(context: Context, stopLocation: LatLng?) {
        context.dataStore.edit { settings ->
            if (stopLocation == null) settings.remove(STOP_LOCATION_LAT_KEY)
            else{
                settings[STOP_LOCATION_LAT_KEY] = stopLocation.latitude
                settings[STOP_LOCATION_LONG_KEY] = stopLocation.longitude
            }

        }
    }

    fun getParentBusStopLocation(context: Context): Flow<LatLng?> {
        return context.dataStore.data.map { preferences ->
            val lat = preferences[STOP_LOCATION_LAT_KEY]
            val lng = preferences[STOP_LOCATION_LONG_KEY]

            if (lat != null && lng != null) {
                LatLng(lat, lng)
            } else {
                null
            }

        }
    }
}