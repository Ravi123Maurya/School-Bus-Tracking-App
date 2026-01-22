package com.ravi.busmanagementt.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.userDatastore: DataStore<Preferences> by preferencesDataStore("user_prefStore")
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "portal_settings")

val ACC_PORTAL_KEY = stringPreferencesKey("acc_portal_key")
val STOP_LOCATION_LAT_KEY = doublePreferencesKey("stop_location_lat_key")
val STOP_LOCATION_LONG_KEY = doublePreferencesKey("stop_location_long_key")

val BUS_ID_KEY = stringPreferencesKey("bus_id_key")