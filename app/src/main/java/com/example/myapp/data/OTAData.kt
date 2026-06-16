package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class OTABanner(
    val text: String = "",
    val colorHex: String = "#FF6200EE",
    val textColorHex: String = "#FFFFFFFF",
    val actionUrl: String = ""
)

@Serializable
data class OTACard(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val actionUrl: String = ""
)
