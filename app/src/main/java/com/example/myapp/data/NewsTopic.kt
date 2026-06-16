package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class NewsTopic(
    val id: String,
    val query: String
)
