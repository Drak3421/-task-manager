package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int = 0,
    val title: String,
    val description: String,
    val timestampMs: Long,
    val autoQuitDurationMinutes: Int = 5,
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null
)
