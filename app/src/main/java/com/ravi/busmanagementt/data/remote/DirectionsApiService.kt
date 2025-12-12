package com.ravi.busmanagementt.data.remote

import android.text.Layout
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @QueryMap options: Map<String, String>
    ): DirectionsResponse
}