package com.example.allote.data

import retrofit2.http.GET

interface BluelyticsApiService {
    @GET("v2/latest")
    suspend fun getLatestDolarValues(): DolarApiResponse
}