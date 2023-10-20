package com.poiuyreq0.koko

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CafesAPI {
    @GET("cafes")
    fun findAllApi(
    ): Call<List<Cafe>>

    @GET("cafes/byRadius")
    fun findByRadiusApi(
        @Query("lat") userLatitude: Double,
        @Query("lng") userLongitude: Double,
        @Query("rad") radius: Double
    ): Call<List<Cafe>>

    @GET("positions")
    fun positionsApi(
        @Query("name") cafeName: String
    ): Call<List<Long>>
}