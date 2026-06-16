package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeChannel(
    val id: String,
    val name: String,
    val handle: String = ""
)

@Serializable
data class YouTubeChannelSearchInfo(
    val id: String,
    val name: String,
    val handle: String = "",
    val subCount: String = "",
    val thumbnailUrl: String = ""
)

