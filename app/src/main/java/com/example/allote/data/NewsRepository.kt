package com.example.allote.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsApiService: NewsApiService
) {
    suspend fun getNews(page: String? = null): Result<NewsResponse> {
        return try {
            val response = newsApiService.getNews(page = page)
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
