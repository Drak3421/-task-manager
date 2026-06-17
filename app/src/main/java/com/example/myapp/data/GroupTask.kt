package com.example.myapp.data

import kotlinx.serialization.Serializable

/**
 * A collaborative group whose members share a task list.
 * Synced peer-to-peer over each member's ntfy.sh topic (no central server).
 */
@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    val members: List<String>,   // lowercase usernames, includes the creator
    val createdBy: String,       // lowercase username
    val createdAt: Long
)

/**
 * A single shared task within a [TaskGroup]. Anyone in the group can create one
 * and assign it to a specific member (or leave it open to anyone). When marked
 * done, [doneBy] records who completed it and [createdBy] records who assigned it.
 */
@Serializable
data class GroupTask(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String = "",
    val dueAt: Long = 0L,           // 0 = no due time
    val assignedTo: String = "",    // lowercase username, "" = anyone
    val createdBy: String,          // "assigned by" — lowercase username
    val createdAt: Long,
    val status: String = "pending", // "pending" | "done"
    val doneBy: String = "",        // lowercase username who marked it done
    val doneAt: Long = 0L
)
