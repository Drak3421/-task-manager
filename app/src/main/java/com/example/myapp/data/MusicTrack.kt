package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class MusicTrack(
    val id: String,
    val filePath: String,
    val title: String,
    val artist: String,
    val durationMs: Long
)
