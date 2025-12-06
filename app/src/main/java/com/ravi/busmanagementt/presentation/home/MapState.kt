package com.ravi.busmanagementt.presentation.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MapState(
    val cameraPositionState: CameraPositionState,
    private val coroutineScope: CoroutineScope
) {
    var isMapInitialized by mutableStateOf(false)

    fun animateCamera(position: LatLng, zoom: Float = 17f) {
        if (!cameraPositionState.isMoving){
            coroutineScope.launch {
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(position, zoom),
                        durationMs = 1000
                    )
                } catch (e: CancellationException){
                    Log.e("MapState", "animateCamera: $e")
                    return@launch
                } catch (e: Exception){
                    Log.e("MapState", "An unexpected error occurred during camera animation.", e)                }

            }
        }else{
            Log.d("MapState", "Skipping animation request because camera is already moving.")
        }

    }

}