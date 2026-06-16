package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class DownloadedNewsArticle(
    val title: String,
    val link: String,
    val pubDate: String,
    val source: String,
    val timestampMs: Long,
    val localFilePath: String
)
