package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val publishedDate: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val timestampMs: Long = 0L,
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val channelId: String = "",
    val description: String = ""
)
