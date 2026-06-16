package com.example.myapp.data

import kotlinx.serialization.Serializable

@Serializable
data class FileAttachment(
    val fileName: String,
    val filePath: String,
    val fileType: String, // "audio", "news_article", "image", "generic"
    val fileSize: Long = 0L,
    val downloadUrl: String? = null
)

@Serializable
data class ChatMessage(
    val id: String,
    val friendUsername: String,
    val sender: String, // "me" or friend's username
    val text: String,
    val timestampMs: Long,
    val fileAttachment: FileAttachment? = null,
    val status: String = "sent" // "sent", "delivered", "read"
)
