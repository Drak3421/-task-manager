package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteWebsite(
    val title: String,
    val url: String
)
