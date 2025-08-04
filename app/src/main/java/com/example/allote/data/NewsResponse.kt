package com.example.allote.data

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val results: List<Article>,
    val nextPage: String?
)

data class Article(
    @SerializedName("article_id")
    val articleId: String,
    val title: String,
    val link: String,
    val description: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("pubDate")
    val pubDate: String,
    val source_id: String
)
