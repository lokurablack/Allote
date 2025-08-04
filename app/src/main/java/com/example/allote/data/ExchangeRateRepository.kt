package com.example.allote.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val apiService: BluelyticsApiService
) {
    suspend fun getDolarApiResponse(): DolarApiResponse? {
        return try {
            apiService.getLatestDolarValues()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}