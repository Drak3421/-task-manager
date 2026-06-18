package com.example.myapp.data

import kotlinx.serialization.Serializable

/**
 * A task belonging to another user that I have subscribed to view (read-only).
 * The owner broadcasts updates over their ntfy topic; I store a local cache
 * keyed by (ownerUsername, taskId) and render them in the home alarm list.
 */
@Serializable
data class SharedTask(
    val ownerUsername: String,    // lowercase
    val taskId: Int,
    val title: String,
    val description: String,
    val timestampMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
