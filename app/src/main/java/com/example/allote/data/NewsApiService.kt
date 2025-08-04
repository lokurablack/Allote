package com.example.allote.data

import com.example.allote.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("api/1/news")
    suspend fun getNews(
        @Query("apikey") apiKey: String = BuildConfig.NEWS_API_KEY,
        @Query("country") country: String = "ar",
        @Query("category") category: String = "business", // Usamos 'business' que a menudo incluye agricultura
        @Query("language") language: String = "es",
        @Query("page") page: String? = null
    ): NewsResponse
}
