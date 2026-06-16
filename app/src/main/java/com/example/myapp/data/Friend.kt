package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val username: String,
    val status: String = "Online",
    val avatarColorHex: String = "#FF3F51B5"
)
