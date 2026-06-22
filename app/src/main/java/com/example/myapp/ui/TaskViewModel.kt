package com.example.myapp.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.alarm.AlarmScheduler
import com.example.myapp.data.PreferencesManager
import com.example.myapp.data.Task
import com.example.myapp.data.MusicTrack
import com.example.myapp.data.MusicPlayerManager
import com.example.myapp.data.YouTubeChannel
import com.example.myapp.data.YouTubeChannelSearchInfo
import com.example.myapp.data.YouTubeVideo
import com.example.myapp.data.YouTubeFeedParser
import com.example.myapp.data.AiNewsArticle
import com.example.myapp.data.AiNewsParser
import com.example.myapp.data.NewsTopic
import com.example.myapp.data.DownloadedNewsArticle
import com.example.myapp.data.Friend
import com.example.myapp.data.ChatMessage
import com.example.myapp.data.FileAttachment
import com.example.myapp.data.FavoriteWebsite
import com.example.myapp.data.TaskGroup
import com.example.myapp.data.GroupTask
import com.example.myapp.data.SharedTask
import java.io.IOException
import kotlinx.coroutines.Job
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.widget.Toast
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import android.provider.Settings
import org.json.JSONObject

const val GITHUB_OWNER = "Drak3421"
const val GITHUB_REPO = "-task-manager"

data class UpdateInfo(
    val versionTag: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmScheduler = AlarmScheduler(application)
    val preferencesManager = PreferencesManager(application)
    val musicPlayerManager = MusicPlayerManager(application)

    val tasks = preferencesManager.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskGroups = preferencesManager.taskGroupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupTasks = preferencesManager.groupTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerFloatEnabled = preferencesManager.playerFloatEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playerFloatX = preferencesManager.playerFloatXFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val playerFloatY = preferencesManager.playerFloatYFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun setPlayerFloatEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setPlayerFloatEnabled(enabled) }
    }

    fun setPlayerFloatPosition(x: Float, y: Float) {
        viewModelScope.launch { preferencesManager.setPlayerFloatPosition(x, y) }
    }

    val subscribedUsers = preferencesManager.subscribedUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sharedTasks = preferencesManager.sharedTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val welcomeName = preferencesManager.welcomeNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")
        
    private val initialBackgroundUri = kotlinx.coroutines.runBlocking {
        preferencesManager.backgroundUriFlow.firstOrNull()
    }

    private val initialDarkMode = kotlinx.coroutines.runBlocking {
        preferencesManager.darkModeFlow.firstOrNull() ?: true
    }

    val isDarkMode = preferencesManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDarkMode)
        
    val backgroundUri = preferencesManager.backgroundUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialBackgroundUri)

    val clockLayout = preferencesManager.clockLayoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "digital")

    val musicTracks = preferencesManager.musicTracksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _profileUpdateTrigger = MutableStateFlow(0L)
    val profileUpdateTrigger: StateFlow<Long> = _profileUpdateTrigger.asStateFlow()

    val activeAppIcon = preferencesManager.activeAppIconFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MainActivityDefault")

    private val _deviceSongs = MutableStateFlow<List<MusicTrack>>(emptyList())
    val deviceSongs: StateFlow<List<MusicTrack>> = _deviceSongs.asStateFlow()

    val allMusicTracks = kotlinx.coroutines.flow.combine(
        musicTracks,
        deviceSongs
    ) { imported, device ->
        imported + device
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTrack = musicPlayerManager.currentTrack
    val isPlaying = musicPlayerManager.isPlaying
    val currentPositionMs = musicPlayerManager.currentPositionMs
    val durationMs = musicPlayerManager.durationMs
    val isRepeatEnabled = musicPlayerManager.isRepeatEnabled

    val currentVersionCode: Int
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }

    val latestVersion = preferencesManager.latestVersionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val updateDownloadUrl = preferencesManager.updateDownloadUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isPublishingUpdate = MutableStateFlow(false)
    val isPublishingUpdate: StateFlow<Boolean> = _isPublishingUpdate.asStateFlow()

    private val _registeredUsers = MutableStateFlow<List<String>>(emptyList())
    val registeredUsers: StateFlow<List<String>> = _registeredUsers.asStateFlow()

    private val _isFetchingRegisteredUsers = MutableStateFlow(false)
    val isFetchingRegisteredUsers: StateFlow<Boolean> = _isFetchingRegisteredUsers.asStateFlow()

    val youtubeChannels = preferencesManager.youtubeChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _youtubeVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = _youtubeVideos.asStateFlow()

    private val _isNewsRefreshing = MutableStateFlow(false)
    val isNewsRefreshing: StateFlow<Boolean> = _isNewsRefreshing.asStateFlow()

    private val _searchedChannels = MutableStateFlow<List<YouTubeChannelSearchInfo>>(emptyList())
    val searchedChannels: StateFlow<List<YouTubeChannelSearchInfo>> = _searchedChannels.asStateFlow()

    private val _isChannelSearching = MutableStateFlow(false)
    val isChannelSearching: StateFlow<Boolean> = _isChannelSearching.asStateFlow()

    val selectedChannelId = MutableStateFlow<String?>(null)
    val timeFilter = MutableStateFlow<String>("all")
    val typeFilter = MutableStateFlow<String>("all") // "all", "shorts", "videos"

    val newsTopics = preferencesManager.newsTopicsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedNewsTopicId = MutableStateFlow<String?>(null)
    val newsTimeFilter = MutableStateFlow<String>("1_week") // "1_day", "1_week", "1_month"

    private val _aiNewsArticles = MutableStateFlow<List<AiNewsArticle>>(emptyList())
    val aiNewsArticles: StateFlow<List<AiNewsArticle>> = _aiNewsArticles.asStateFlow()

    private val _isAiNewsRefreshing = MutableStateFlow(false)
    val isAiNewsRefreshing: StateFlow<Boolean> = _isAiNewsRefreshing.asStateFlow()

    val recentVideos = preferencesManager.recentVideosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNews = preferencesManager.recentNewsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedNews = preferencesManager.downloadedNewsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myUsername = preferencesManager.myUsernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val friends = preferencesManager.friendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcutPositions = preferencesManager.shortcutPositionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val chatMessages = preferencesManager.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteWebsites = preferencesManager.favoriteWebsitesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRegisteringUsername = MutableStateFlow(false)
    val isRegisteringUsername: StateFlow<Boolean> = _isRegisteringUsername.asStateFlow()
    val typingFriends = MutableStateFlow<Map<String, Long>>(emptyMap())
    val activeChatFriend = MutableStateFlow<String?>(null)

    private val _isSearchingUser = MutableStateFlow(false)
    val isSearchingUser: StateFlow<Boolean> = _isSearchingUser.asStateFlow()

    val incomingRequests = preferencesManager.incomingRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentRequests = preferencesManager.sentRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoverableUsers = MutableStateFlow<List<String>>(emptyList())
    
    private val _isRefreshingDiscoverable = MutableStateFlow(false)
    val isRefreshingDiscoverable: StateFlow<Boolean> = _isRefreshingDiscoverable.asStateFlow()

    private val _searchedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val searchedVideos: StateFlow<List<YouTubeVideo>> = _searchedVideos.asStateFlow()

    private val _isVideoSearching = MutableStateFlow(false)
    val isVideoSearching: StateFlow<Boolean> = _isVideoSearching.asStateFlow()

    private var sseJob: kotlinx.coroutines.Job? = null

    val filteredNewsArticles = kotlinx.coroutines.flow.combine(
        aiNewsArticles,
        selectedNewsTopicId,
        newsTimeFilter,
        recentNews
    ) { articles, topicId, filter, recents ->
        if (topicId == "recent_history") {
            return@combine recents
        }
        val now = System.currentTimeMillis()
        val list = when (filter) {
            "1_day" -> articles.filter { now - it.timestampMs <= 24L * 60 * 60 * 1000 }
            "1_week" -> articles.filter { now - it.timestampMs <= 7L * 24 * 60 * 60 * 1000 }
            "1_month" -> articles.filter { now - it.timestampMs <= 30L * 24 * 60 * 60 * 1000 }
            else -> articles
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredVideos = kotlinx.coroutines.flow.combine(
        youtubeVideos,
        selectedChannelId,
        timeFilter,
        typeFilter,
        recentVideos
    ) { videos, channelId, timeFilterVal, typeFilterVal, recents ->
        if (channelId == "recent_history") {
            return@combine recents
        }
        var list = if (channelId != null) {
            val channelName = youtubeChannels.value.find { it.id == channelId }?.name
            if (channelName != null) {
                videos.filter { it.channelName == channelName }
            } else {
                videos
            }
        } else {
            videos
        }

        list = when (typeFilterVal) {
            "shorts" -> list.filter { it.isShort }
            "videos" -> list.filter { !it.isShort }
            else -> list
        }

        val now = System.currentTimeMillis()
        list = when (timeFilterVal) {
            "latest" -> {
                if (list.isNotEmpty()) listOf(list.first()) else emptyList()
            }
            "1_week" -> {
                list.filter { now - it.timestampMs <= 7L * 24 * 60 * 60 * 1000 }
            }
            "1_month" -> {
                list.filter { now - it.timestampMs <= 30L * 24 * 60 * 60 * 1000 }
            }
            else -> list
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val lastReset = preferencesManager.lastHistoryResetFlow.firstOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (lastReset == 0L) {
                preferencesManager.saveLastHistoryResetTime(now)
            } else if (now - lastReset >= 48L * 60 * 60 * 1000) {
                preferencesManager.saveRecentVideos(emptyList())
                preferencesManager.saveRecentNews(emptyList())
                preferencesManager.saveLastHistoryResetTime(now)
            }
        }

        musicPlayerManager.onTrackComplete = {
            musicPlayerManager.skipNext(musicTracks.value)
        }
        
        viewModelScope.launch {
            youtubeChannels.collect {
                refreshYouTubeFeeds()
            }
        }

        // Auto-refresh news updates whenever news topics load or selection changes
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                newsTopics,
                selectedNewsTopicId
            ) { _, _ -> }.collect {
                if (selectedNewsTopicId.value == null && newsTopics.value.isNotEmpty()) {
                    selectedNewsTopicId.value = newsTopics.value.first().id
                }
                refreshAiNews()
            }
        }

        viewModelScope.launch {
            myUsername.collect { username ->
                if (username.isNotEmpty()) {
                    startNtfyListener(username)
                    startPresenceHeartbeat(username)
                    startFriendsPresenceCheck()
                    syncWithServer()
                } else {
                    sseJob?.cancel()
                    presenceJob?.cancel()
                    friendsPresenceJob?.cancel()
                }
            }
        }

        startOtaNtfyListener()
        checkForUpdates()
    }

    fun toggleRepeat() {
        val current = isRepeatEnabled.value
        musicPlayerManager.setRepeatEnabled(!current)
    }

    fun updateWelcomeName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveWelcomeName(name)
        }
    }

    fun setClockLayout(layout: String) {
        viewModelScope.launch {
            preferencesManager.setClockLayout(layout)
        }
    }
    
    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(isDark)
        }
    }
    
    fun setBackgroundUri(uri: String?) {
        viewModelScope.launch {
            preferencesManager.setBackgroundUri(uri)
        }
    }

    fun saveTask(task: Task, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentTasks = tasks.value.toMutableList()
            var savedTask = task
            if (task.id == 0 || task.id == -1) {
                savedTask = task.copy(id = (currentTasks.maxOfOrNull { it.id } ?: 0) + 1)
                currentTasks.add(savedTask)
            } else {
                val index = currentTasks.indexOfFirst { it.id == task.id }
                if (index != -1) currentTasks[index] = savedTask
            }
            preferencesManager.saveTasks(currentTasks)
            alarmScheduler.scheduleTaskAlarm(savedTask)
            broadcastSharedTaskUpsert(savedTask)
            onComplete()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            val currentTasks = tasks.value.toMutableList()
            currentTasks.removeIf { it.id == task.id }
            preferencesManager.saveTasks(currentTasks)
            alarmScheduler.cancelTaskAlarm(task.id)
            broadcastSharedTaskDelete(task.id)
        }
    }

    // ---------------------------------------------------------------------
    // Read-only task sharing — subscribers see my alarms in their home list.
    // ---------------------------------------------------------------------

    private fun broadcastSharedTaskUpsert(task: Task) {
        val me = myUsername.value.lowercase()
        if (me.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val friendsList = preferencesManager.friendsFlow.firstOrNull() ?: return@launch
            if (friendsList.isEmpty()) return@launch
            val shared = SharedTask(
                ownerUsername = me,
                taskId = task.id,
                title = task.title,
                description = task.description,
                timestampMs = task.timestampMs
            )
            val payload = Json.encodeToString(shared)
            val msg = ChatMessage(
                id = "shared_task_upsert_${me}_${task.id}",
                friendUsername = "",
                sender = myUsername.value,
                text = payload,
                timestampMs = System.currentTimeMillis()
            )
            friendsList.forEach { friend ->
                postMessageToNetwork(friend.username.lowercase(), msg)
            }
        }
    }

    private fun broadcastSharedTaskDelete(taskId: Int) {
        val me = myUsername.value.lowercase()
        if (me.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val friendsList = preferencesManager.friendsFlow.firstOrNull() ?: return@launch
            if (friendsList.isEmpty()) return@launch
            val msg = ChatMessage(
                id = "shared_task_delete_${me}_${taskId}",
                friendUsername = "",
                sender = myUsername.value,
                text = "${me}|${taskId}",
                timestampMs = System.currentTimeMillis()
            )
            friendsList.forEach { friend ->
                postMessageToNetwork(friend.username.lowercase(), msg)
            }
        }
    }

    fun subscribeToUser(username: String, onResult: (Boolean, String?) -> Unit) {
        val u = username.trim().lowercase()
        if (u.isEmpty()) { onResult(false, "Username cannot be empty"); return }
        if (u == myUsername.value.lowercase()) { onResult(false, "Cannot subscribe to yourself"); return }
        viewModelScope.launch {
            val current = preferencesManager.subscribedUsersFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            if (current.any { it.equals(u, ignoreCase = true) }) {
                onResult(false, "Already added"); return@launch
            }
            current.add(u)
            preferencesManager.saveSubscribedUsers(current)
            withContext(Dispatchers.Main) { onResult(true, null) }
        }
    }

    fun unsubscribeFromUser(username: String) {
        val u = username.lowercase()
        viewModelScope.launch {
            val current = preferencesManager.subscribedUsersFlow.firstOrNull()?.toMutableList() ?: return@launch
            current.removeIf { it.equals(u, ignoreCase = true) }
            preferencesManager.saveSubscribedUsers(current)
            // Drop any cached tasks from that user too.
            val sharedNow = preferencesManager.sharedTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            sharedNow.removeIf { it.ownerUsername.equals(u, ignoreCase = true) }
            preferencesManager.saveSharedTasks(sharedNow)
        }
    }

    suspend fun getTaskById(id: Int): Task? {
        return tasks.value.find { it.id == id }
    }

    // ---------------------------------------------------------------------
    // Group Tasks (shared collaborative to-do, synced over ntfy.sh)
    // ---------------------------------------------------------------------

    /** Sends a group event to every member's ntfy topic except my own. */
    private fun broadcastToGroup(members: List<String>, eventId: String, payloadJson: String) {
        val me = myUsername.value.lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            val msg = ChatMessage(
                id = eventId,
                friendUsername = "",
                sender = myUsername.value,
                text = payloadJson,
                timestampMs = System.currentTimeMillis()
            )
            members.map { it.lowercase() }.distinct().forEach { member ->
                if (member.isNotEmpty() && member != me) {
                    postMessageToNetwork(member, msg)
                }
            }
        }
    }

    private suspend fun upsertGroupTaskLocal(task: GroupTask) {
        val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
        val idx = current.indexOfFirst { it.id == task.id }
        if (idx != -1) current[idx] = task else current.add(task)
        preferencesManager.saveGroupTasks(current)
    }

    fun createTaskGroup(name: String, memberUsernames: List<String>, onResult: (Boolean, String?) -> Unit) {
        val me = myUsername.value.lowercase()
        if (me.isEmpty()) {
            onResult(false, "Set up your account in Friends first")
            return
        }
        val groupName = name.trim()
        if (groupName.isEmpty()) {
            onResult(false, "Group name cannot be empty")
            return
        }
        viewModelScope.launch {
            val members = (memberUsernames.map { it.lowercase() } + me).distinct()
            val group = TaskGroup(
                id = java.util.UUID.randomUUID().toString(),
                name = groupName,
                members = members,
                createdBy = me,
                createdAt = System.currentTimeMillis()
            )
            val current = preferencesManager.taskGroupsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            current.add(group)
            preferencesManager.saveTaskGroups(current)
            broadcastToGroup(members, "group_sync_${group.id}", Json.encodeToString(group))
            withContext(Dispatchers.Main) { onResult(true, null) }
        }
    }

    fun addMemberToGroup(groupId: String, username: String) {
        val newMember = username.lowercase()
        if (newMember.isEmpty()) return
        viewModelScope.launch {
            val groups = preferencesManager.taskGroupsFlow.firstOrNull()?.toMutableList() ?: return@launch
            val idx = groups.indexOfFirst { it.id == groupId }
            if (idx == -1) return@launch
            val group = groups[idx]
            if (group.members.contains(newMember)) return@launch
            val updated = group.copy(members = (group.members + newMember).distinct())
            groups[idx] = updated
            preferencesManager.saveTaskGroups(groups)
            broadcastToGroup(updated.members, "group_sync_${updated.id}", Json.encodeToString(updated))
        }
    }

    fun createGroupTask(groupId: String, title: String, description: String, dueAt: Long, assignedTo: String) {
        val me = myUsername.value.lowercase()
        val taskTitle = title.trim()
        if (me.isEmpty() || taskTitle.isEmpty()) return
        viewModelScope.launch {
            val group = preferencesManager.taskGroupsFlow.firstOrNull()?.find { it.id == groupId } ?: return@launch
            val task = GroupTask(
                id = java.util.UUID.randomUUID().toString(),
                groupId = groupId,
                title = taskTitle,
                description = description.trim(),
                dueAt = dueAt,
                assignedTo = assignedTo.lowercase(),
                createdBy = me,
                createdAt = System.currentTimeMillis()
            )
            upsertGroupTaskLocal(task)
            broadcastToGroup(group.members, "grouptask_upsert_${task.id}", Json.encodeToString(task))
        }
    }

    fun markGroupTaskDone(groupId: String, taskId: String) {
        val me = myUsername.value.lowercase()
        viewModelScope.launch {
            val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: return@launch
            val idx = current.indexOfFirst { it.id == taskId }
            if (idx == -1) return@launch
            val done = current[idx].copy(status = "done", doneBy = me, doneAt = System.currentTimeMillis())
            current[idx] = done
            preferencesManager.saveGroupTasks(current)
            val group = preferencesManager.taskGroupsFlow.firstOrNull()?.find { it.id == groupId }
            if (group != null) {
                broadcastToGroup(group.members, "grouptask_done_${done.id}", Json.encodeToString(done))
            }
        }
    }

    fun deleteGroupTask(groupId: String, taskId: String) {
        viewModelScope.launch {
            val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: return@launch
            current.removeIf { it.id == taskId }
            preferencesManager.saveGroupTasks(current)
            val group = preferencesManager.taskGroupsFlow.firstOrNull()?.find { it.id == groupId }
            if (group != null) {
                broadcastToGroup(group.members, "grouptask_delete_${taskId}", "$groupId|$taskId")
            }
        }
    }

    fun leaveGroup(groupId: String) {
        val me = myUsername.value.lowercase()
        viewModelScope.launch {
            val group = preferencesManager.taskGroupsFlow.firstOrNull()?.find { it.id == groupId }
            val groups = preferencesManager.taskGroupsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            groups.removeIf { it.id == groupId }
            preferencesManager.saveTaskGroups(groups)
            val remaining = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            remaining.removeIf { it.groupId == groupId }
            preferencesManager.saveGroupTasks(remaining)
            if (group != null) {
                broadcastToGroup(group.members, "group_member_left_${groupId}", "$groupId|$me")
            }
        }
    }

    fun importAudioFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val musicDir = File(context.filesDir, "music")
                if (!musicDir.exists()) musicDir.mkdirs()
                
                val id = java.util.UUID.randomUUID().toString()
                var extension = "mp3"
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != null && mimeType.contains("/")) {
                    val ext = mimeType.substringAfter("/")
                    if (ext.isNotEmpty()) extension = ext
                }
                val file = File(musicDir, "$id.$extension")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                var title = "Unknown Title"
                var artist = "Unknown Artist"
                var durationMs = 0L

                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val metaTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val metaArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val metaDuration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)

                    if (!metaTitle.isNullOrEmpty()) title = metaTitle
                    if (!metaArtist.isNullOrEmpty()) artist = metaArtist
                    if (!metaDuration.isNullOrEmpty()) durationMs = metaDuration.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }

                if (title == "Unknown Title") {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (displayNameIndex != -1) {
                                val name = it.getString(displayNameIndex)
                                if (!name.isNullOrEmpty()) {
                                    title = name.substringBeforeLast(".")
                                }
                            }
                        }
                    }
                }

                val newTrack = MusicTrack(
                    id = id,
                    filePath = file.absolutePath,
                    title = title,
                    artist = artist,
                    durationMs = durationMs
                )

                val updatedList = musicTracks.value + newTrack
                preferencesManager.saveMusicTracks(updatedList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importReceivedAudioTrack(context: Context, attachment: FileAttachment, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(attachment.filePath)
                if (!sourceFile.exists()) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }
                
                val musicDir = File(context.filesDir, "music")
                if (!musicDir.exists()) musicDir.mkdirs()
                
                val id = java.util.UUID.randomUUID().toString()
                val extension = sourceFile.extension.ifEmpty { "mp3" }
                val destFile = File(musicDir, "$id.$extension")
                
                sourceFile.copyTo(destFile, overwrite = true)
                
                var title = attachment.fileName.substringBeforeLast(".")
                var artist = "Received Track"
                var durationMs = 0L
                
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destFile.absolutePath)
                    val metaTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val metaArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val metaDuration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    
                    if (!metaTitle.isNullOrEmpty()) title = metaTitle
                    if (!metaArtist.isNullOrEmpty()) artist = metaArtist
                    if (!metaDuration.isNullOrEmpty()) durationMs = metaDuration.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
                
                val newTrack = MusicTrack(
                    id = id,
                    filePath = destFile.absolutePath,
                    title = title,
                    artist = artist,
                    durationMs = durationMs
                )
                
                withContext(Dispatchers.Main) {
                    val currentTracks = (preferencesManager.musicTracksFlow.firstOrNull() ?: emptyList()).toMutableList()
                    currentTracks.add(newTrack)
                    preferencesManager.saveMusicTracks(currentTracks)
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun deleteAudioFile(track: MusicTrack) {
        viewModelScope.launch {
            if (currentTrack.value?.id == track.id) {
                musicPlayerManager.stopPlayback()
            }
            val file = File(track.filePath)
            if (file.exists()) {
                file.delete()
            }
            val updatedList = musicTracks.value.filter { it.id != track.id }
            preferencesManager.saveMusicTracks(updatedList)
        }
     }

    fun deleteAudioFiles(tracks: List<MusicTrack>) {
        viewModelScope.launch {
            var stopPlayer = false
            val currentPlayingId = currentTrack.value?.id
            val idsToDelete = tracks.map { it.id }.toSet()
            
            for (track in tracks) {
                if (track.id == currentPlayingId) {
                    stopPlayer = true
                }
                val file = File(track.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            if (stopPlayer) {
                musicPlayerManager.stopPlayback()
            }
            val updatedList = musicTracks.value.filter { it.id !in idsToDelete }
            preferencesManager.saveMusicTracks(updatedList)
        }
    }

    fun renameMusicTrack(trackId: String, newTitle: String, newArtist: String) {
        viewModelScope.launch {
            val current = musicTracks.value.toMutableList()
            val index = current.indexOfFirst { it.id == trackId }
            if (index != -1) {
                val updatedTrack = current[index].copy(title = newTitle, artist = newArtist)
                current[index] = updatedTrack
                preferencesManager.saveMusicTracks(current)
                
                // Update active track in player if it's currently playing
                if (currentTrack.value?.id == trackId) {
                    musicPlayerManager.updateActiveTrack(updatedTrack)
                }
            }
        }
    }

    fun loadDeviceSongs(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val songs = mutableListOf<MusicTrack>()
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.DURATION
            )
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            try {
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val durationCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Title"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val duration = cursor.getLong(durationCol)
                        val contentUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ).toString()
                        songs.add(
                            MusicTrack(
                                id = "device_$id",
                                filePath = contentUri,
                                title = title,
                                artist = artist,
                                durationMs = duration
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _deviceSongs.value = songs
        }
    }

    fun refreshYouTubeFeeds() {
        viewModelScope.launch {
            _isNewsRefreshing.value = true
            val channels = youtubeChannels.value
            val allVideos = mutableListOf<YouTubeVideo>()
            channels.forEach { channel ->
                val videos = YouTubeFeedParser.fetchChannelFeed(channel.id, channel.name)
                allVideos.addAll(videos)
            }
            _youtubeVideos.value = allVideos.sortedByDescending { it.timestampMs }
            _isNewsRefreshing.value = false
        }
    }

    fun addYouTubeChannel(channelUrl: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isNewsRefreshing.value = true
            val resolvedId = YouTubeFeedParser.resolveChannelIdFromLink(channelUrl)
            if (resolvedId != null) {
                val channelName = YouTubeFeedParser.fetchChannelName(resolvedId) ?: "YouTube Channel"
                val current = youtubeChannels.value.toMutableList()
                if (current.none { it.id == resolvedId }) {
                    current.add(YouTubeChannel(id = resolvedId, name = channelName))
                    preferencesManager.saveYouTubeChannels(current)
                }
                _isNewsRefreshing.value = false
                onResult(true)
            } else {
                _isNewsRefreshing.value = false
                onResult(false)
            }
        }
    }

    fun addYouTubeChannelDirect(id: String, name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val current = youtubeChannels.value.toMutableList()
            if (current.none { it.id == id }) {
                current.add(YouTubeChannel(id = id, name = name))
                preferencesManager.saveYouTubeChannels(current)
            }
            onResult(true)
        }
    }

    fun searchYouTubeChannels(topic: String) {
        viewModelScope.launch {
            _isChannelSearching.value = true
            val results = YouTubeFeedParser.searchChannelsOnYouTube(topic)
            _searchedChannels.value = results
            _isChannelSearching.value = false
        }
    }

    fun clearSearchedChannels() {
        _searchedChannels.value = emptyList()
    }

    fun deleteYouTubeChannel(channel: YouTubeChannel) {
        viewModelScope.launch {
            val current = youtubeChannels.value.filter { it.id != channel.id }
            preferencesManager.saveYouTubeChannels(current)
            if (selectedChannelId.value == channel.id) {
                selectedChannelId.value = null
            }
        }
    }

    fun selectChannel(channelId: String?) {
        selectedChannelId.value = channelId
    }

    fun setTimeFilter(filter: String) {
        timeFilter.value = filter
    }

    fun setTypeFilter(filter: String) {
        typeFilter.value = filter
    }

    fun setCustomBackground(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val oldPath = backgroundUri.value
                if (!oldPath.isNullOrEmpty()) {
                    val oldFile = File(oldPath)
                    if (oldFile.exists() && oldFile.parent == context.filesDir.absolutePath) {
                        oldFile.delete()
                    }
                }

                val newFile = File(context.filesDir, "background_img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    newFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                setBackgroundUri(newFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                setBackgroundUri(uri.toString())
            }
        }
    }

    fun removeBackground(context: Context) {
        viewModelScope.launch {
            val oldPath = backgroundUri.value
            if (!oldPath.isNullOrEmpty()) {
                val oldFile = File(oldPath)
                if (oldFile.exists() && oldFile.parent == context.filesDir.absolutePath) {
                    oldFile.delete()
                }
            }
            setBackgroundUri(null)
        }
    }

    fun addNewsTopic(query: String) {
        viewModelScope.launch {
            val current = newsTopics.value.toMutableList()
            val id = java.util.UUID.randomUUID().toString()
            if (current.none { it.query.equals(query, ignoreCase = true) }) {
                current.add(NewsTopic(id = id, query = query))
                preferencesManager.saveNewsTopics(current)
                if (selectedNewsTopicId.value == null) {
                    selectedNewsTopicId.value = id
                }
            }
        }
    }

    fun deleteNewsTopic(topic: NewsTopic) {
        viewModelScope.launch {
            val current = newsTopics.value.filter { it.id != topic.id }
            preferencesManager.saveNewsTopics(current)
            if (selectedNewsTopicId.value == topic.id) {
                selectedNewsTopicId.value = current.firstOrNull()?.id
            }
        }
    }

    fun selectNewsTopic(topicId: String?) {
        selectedNewsTopicId.value = topicId
    }

    fun setNewsTimeFilter(filter: String) {
        newsTimeFilter.value = filter
    }

    fun openYouTubeVideo(video: YouTubeVideo) {
        viewModelScope.launch {
            val current = recentVideos.value.toMutableList()
            current.removeIf { it.videoId == video.videoId }
            current.add(0, video)
            if (current.size > 20) {
                current.removeAt(current.size - 1)
            }
            preferencesManager.saveRecentVideos(current)
        }
    }

    fun openNewsArticle(article: AiNewsArticle) {
        viewModelScope.launch {
            val current = recentNews.value.toMutableList()
            current.removeIf { it.link == article.link }
            current.add(0, article)
            if (current.size > 20) {
                current.removeAt(current.size - 1)
            }
            preferencesManager.saveRecentNews(current)
        }
    }

    fun refreshAiNews() {
        viewModelScope.launch {
            _isAiNewsRefreshing.value = true
            val topicsList = newsTopics.value
            val currentTopicId = selectedNewsTopicId.value ?: topicsList.firstOrNull()?.id
            val topic = topicsList.find { it.id == currentTopicId }
            val query = topic?.query ?: "Artificial Intelligence"
            val articles = AiNewsParser.fetchAiNews(query)
            _aiNewsArticles.value = articles.sortedByDescending { it.timestampMs }
            _isAiNewsRefreshing.value = false
        }
    }

    fun downloadNewsArticle(article: AiNewsArticle, customUrl: String? = null, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val resolvedHtml = withContext(Dispatchers.IO) {
                    try {
                        var currentUrl = customUrl ?: article.link
                        if (currentUrl.contains("news.google.com")) {
                            currentUrl = resolveGoogleNewsUrl(currentUrl)
                        }
                        var connection: HttpURLConnection? = null
                        var redirectCount = 0
                        val maxRedirects = 5
                        var htmlResult: String? = null
                        
                        while (redirectCount < maxRedirects) {
                            val url = URL(currentUrl)
                            connection = (url.openConnection() as HttpURLConnection).apply {
                                readTimeout = 10000
                                connectTimeout = 10000
                                requestMethod = "GET"
                                instanceFollowRedirects = false
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            }
                            connection.connect()
                            
                            val status = connection.responseCode
                            if (status == HttpURLConnection.HTTP_MOVED_PERM || 
                                status == HttpURLConnection.HTTP_MOVED_TEMP || 
                                status == 307 || status == 308 || status == 303) {
                                
                                val newUrl = connection.getHeaderField("Location")
                                if (newUrl == null) {
                                    break
                                }
                                currentUrl = if (newUrl.startsWith("http")) {
                                    newUrl
                                } else {
                                    java.net.URI(currentUrl).resolve(newUrl).toString()
                                }
                                connection.disconnect()
                                redirectCount++
                            } else if (status == HttpURLConnection.HTTP_OK) {
                                htmlResult = connection.inputStream.bufferedReader().use { it.readText() }
                                break
                            } else {
                                break
                            }
                        }
                        htmlResult
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                val finalHtml = resolvedHtml ?: """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <title>${article.title}</title>
                        <style>
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                                line-height: 1.6;
                                padding: 24px;
                                max-width: 600px;
                                margin: 0 auto;
                                color: #333;
                                background-color: #fafafa;
                            }
                            h1 {
                                font-size: 22px;
                                margin-bottom: 8px;
                                color: #111;
                            }
                            .metadata {
                                font-size: 14px;
                                color: #666;
                                margin-bottom: 24px;
                                border-bottom: 1px solid #eee;
                                padding-bottom: 12px;
                            }
                            .content {
                                font-size: 16px;
                                background: white;
                                padding: 20px;
                                border-radius: 8px;
                                border: 1px solid #eaeaea;
                            }
                            .btn {
                                display: inline-block;
                                background-color: #007bff;
                                color: white;
                                padding: 10px 20px;
                                text-decoration: none;
                                border-radius: 4px;
                                margin-top: 16px;
                                font-weight: bold;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>${article.title}</h1>
                        <div class="metadata">
                            <strong>Source:</strong> ${article.source} | <strong>Date:</strong> ${article.pubDate}
                        </div>
                        <div class="content">
                            <p>This article page could not be fully downloaded for offline reading because the publisher website restricted automated downloads.</p>
                            <p>You can read the full article online by clicking the link below:</p>
                            <a class="btn" href="${article.link}" target="_blank">View Original Article</a>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val context = getApplication<Application>().applicationContext
                val offlineDir = File(context.filesDir, "offline_articles")
                if (!offlineDir.exists()) offlineDir.mkdirs()
                
                val safeTitle = article.title.take(30).replace(Regex("[^a-zA-Z0-9]"), "_")
                val fileName = "article_${System.currentTimeMillis()}_${safeTitle}.html"
                val file = File(offlineDir, fileName)
                file.writeText(finalHtml)
                
                val currentList = downloadedNews.value.toMutableList()
                if (currentList.none { it.link == article.link }) {
                    currentList.add(
                        DownloadedNewsArticle(
                            title = article.title,
                            link = article.link,
                            pubDate = article.pubDate,
                            source = article.source,
                            timestampMs = article.timestampMs,
                            localFilePath = file.absolutePath
                        )
                    )
                    preferencesManager.saveDownloadedNews(currentList)
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    private suspend fun resolveGoogleNewsUrl(googleNewsUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val uri = java.net.URI(googleNewsUrl)
            val pathSegments = uri.path.split("/")
            if (uri.host == "news.google.com" && pathSegments.size >= 2) {
                val segmentBeforeLast = pathSegments[pathSegments.size - 2]
                if (segmentBeforeLast == "articles" || segmentBeforeLast == "read") {
                    var id = pathSegments.last()
                    if (id.contains("?")) {
                        id = id.split("?")[0]
                    }
                    
                    // Fetch parameters from articles URL
                    var sg: String? = null
                    var ts: String? = null
                    
                    val fetchParams = { urlString: String ->
                        try {
                            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                                readTimeout = 8000
                                connectTimeout = 8000
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            }
                            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                val html = conn.inputStream.bufferedReader().use { it.readText() }
                                val sgRegex = Regex("""data-n-a-sg="([^"]+)"""")
                                val tsRegex = Regex("""data-n-a-ts="([^"]+)"""")
                                val sgMatch = sgRegex.find(html)?.groupValues?.get(1)
                                val tsMatch = tsRegex.find(html)?.groupValues?.get(1)
                                if (sgMatch != null && tsMatch != null) {
                                    Pair(sgMatch, tsMatch)
                                } else null
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    var params = fetchParams("https://news.google.com/articles/$id")
                    if (params == null) {
                        params = fetchParams("https://news.google.com/rss/articles/$id")
                    }
                    
                    if (params != null) {
                        sg = params.first
                        ts = params.second
                        
                        val innerJson = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$id\",$ts,\"$sg\"]"
                        val escapedInner = innerJson.replace("\"", "\\\"")
                        val fReq = "[[[\"Fbv4je\",\"$escapedInner\",null,\"generic\"]]]"
                        
                        val postUrl = URL("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                        val conn = (postUrl.openConnection() as HttpURLConnection).apply {
                            readTimeout = 10000
                            connectTimeout = 10000
                            requestMethod = "POST"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        }
                        
                        val postBody = "f.req=" + java.net.URLEncoder.encode(fReq, "UTF-8")
                        conn.outputStream.use { out ->
                            out.write(postBody.toByteArray(Charsets.UTF_8))
                        }
                        
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                            val prefix = "[\\\"garturlres\\\",\\\""
                            val startIdx = responseText.indexOf(prefix)
                            if (startIdx != -1) {
                                val urlStart = startIdx + prefix.length
                                val endIdx = responseText.indexOf("\\\",", urlStart)
                                if (endIdx != -1) {
                                    val resolvedUrl = responseText.substring(urlStart, endIdx)
                                    val cleanUrl = resolvedUrl.replace("\\/", "/")
                                    if (cleanUrl.startsWith("http")) {
                                        return@withContext cleanUrl
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        googleNewsUrl
    }

    fun deleteDownloadedNewsArticle(article: DownloadedNewsArticle) {
        viewModelScope.launch {
            try {
                val file = File(article.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val currentList = downloadedNews.value.filter { it.link != article.link }
            preferencesManager.saveDownloadedNews(currentList)
        }
    }

    fun isArticleDownloaded(link: String): Boolean {
        return downloadedNews.value.any { it.link == link }
    }

    private fun encodeBase64(value: String): String {
        return android.util.Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
    }

    private fun decodeBase64(value: String): String {
        return try {
            val decodedBytes = android.util.Base64.decode(
                value,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun getRemoteValue(key: String): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://keyvalue.immanuel.co/api/KeyVal/GetValue/jsi08n3r/$key")
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 8000
                connectTimeout = 8000
                requestMethod = "GET"
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                val cleaned = responseText.removeSurrounding("\"")
                if (cleaned == "null") "" else {
                    try {
                        java.net.URLDecoder.decode(cleaned, "UTF-8")
                    } catch (e: Exception) {
                        cleaned
                    }
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun updateRemoteValue(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val encodedValue = java.net.URLEncoder.encode(value, "UTF-8")
            val url = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/$key/$encodedValue")
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 8000
                connectTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Length", "0")
            }
            connection.connect()
            val code = connection.responseCode
            code == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Upload a large value by splitting it into chunks of ~1500 chars each.
     * Stores metadata key "${key}_meta" with the chunk count,
     * and chunks under "${key}_0", "${key}_1", etc.
     */
    private suspend fun updateRemoteValueChunked(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        val chunkSize = 1500
        val chunks = value.chunked(chunkSize)
        val totalChunks = chunks.size
        
        // Store metadata (chunk count)
        val metaOk = updateRemoteValue("${key}_meta", totalChunks.toString())
        if (!metaOk) return@withContext false
        
        // Store each chunk
        for (i in chunks.indices) {
            val ok = updateRemoteValue("${key}_$i", chunks[i])
            if (!ok) return@withContext false
        }
        true
    }

    /**
     * Download a large value that was stored in chunks.
     * Reads metadata key "${key}_meta" for chunk count,
     * then fetches "${key}_0", "${key}_1", etc. and reassembles.
     */
    private suspend fun getRemoteValueChunked(key: String): String = withContext(Dispatchers.IO) {
        val meta = getRemoteValue("${key}_meta")
        if (meta.isEmpty()) {
            // Fallback: try reading the value directly (legacy non-chunked data)
            return@withContext getRemoteValue(key)
        }
        val totalChunks = meta.toIntOrNull() ?: return@withContext getRemoteValue(key)
        if (totalChunks <= 0) return@withContext ""
        
        val sb = StringBuilder()
        for (i in 0 until totalChunks) {
            val chunk = getRemoteValue("${key}_$i")
            sb.append(chunk)
        }
        sb.toString()
    }

    fun searchYouTubeVideos(topic: String, countryCode: String) {
        viewModelScope.launch {
            _isVideoSearching.value = true
            val results = YouTubeFeedParser.searchVideosOnYouTube(topic, countryCode)
            _searchedVideos.value = results
            _isVideoSearching.value = false
        }
    }

    fun clearSearchedVideos() {
        _searchedVideos.value = emptyList()
    }

    fun refreshDiscoverableUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingDiscoverable.value = true
            val raw = getRemoteValue("all_registered_users")
            val list = if (raw.isEmpty()) {
                emptyList<String>()
            } else {
                try {
                    val decoded = decodeBase64(raw)
                    Json.decodeFromString<List<String>>(decoded)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            
            val myName = myUsername.value
            if (myName.isNotEmpty() && !list.contains(myName)) {
                val updatedList = list + myName
                val encoded = encodeBase64(Json.encodeToString(updatedList))
                updateRemoteValue("all_registered_users", encoded)
                discoverableUsers.value = list.filter { !it.equals(myName, ignoreCase = true) }
            } else {
                discoverableUsers.value = list.filter { !it.equals(myName, ignoreCase = true) }
            }
            
            val context = getApplication<Application>()
            var profilePicsUpdated = false
            list.filter { !it.equals(myName, ignoreCase = true) }.forEach { uName ->
                val remotePic = getRemoteValueChunked("profile_pic_${uName.lowercase()}")
                val dir = File(context.filesDir, "profile_pics")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "${uName.lowercase()}.jpg")
                if (remotePic.isNotEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(
                            remotePic,
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                        )
                        if (!file.exists() || !file.readBytes().contentEquals(bytes)) {
                            file.writeBytes(bytes)
                            profilePicsUpdated = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    if (file.exists()) {
                        file.delete()
                        profilePicsUpdated = true
                    }
                }
            }
            if (profilePicsUpdated) {
                withContext(Dispatchers.Main) {
                    _profileUpdateTrigger.value = System.currentTimeMillis()
                }
            }
            
            _isRefreshingDiscoverable.value = false
        }
    }

    fun syncWithServer() {
        val myName = myUsername.value
        if (myName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Sync Friends List
            val rawFriends = getRemoteValue("friends_${myName.lowercase()}")
            if (rawFriends.isNotEmpty()) {
                try {
                    val decoded = decodeBase64(rawFriends)
                    val serverFriends = Json.decodeFromString<List<String>>(decoded)
                    
                    withContext(Dispatchers.Main) {
                        val localFriends = (preferencesManager.friendsFlow.firstOrNull() ?: emptyList()).toMutableList()
                        var localChanged = false
                        
                        // Add new friends from server
                        serverFriends.forEach { friendName ->
                            if (localFriends.none { it.username.equals(friendName, ignoreCase = true) }) {
                                localFriends.add(Friend(username = friendName))
                                localChanged = true
                            }
                        }
                        
                        // Remove friends not on server
                        val iterator = localFriends.iterator()
                        while (iterator.hasNext()) {
                            val f = iterator.next()
                            if (serverFriends.none { it.equals(f.username, ignoreCase = true) }) {
                                iterator.remove()
                                localChanged = true
                            }
                        }
                        
                        // Clear friend requests of any newly added friends
                        val currentSent = (preferencesManager.sentRequestsFlow.firstOrNull() ?: emptyList()).toMutableList()
                        val currentIncoming = (preferencesManager.incomingRequestsFlow.firstOrNull() ?: emptyList()).toMutableList()
                        var reqChanged = false
                        serverFriends.forEach { fName ->
                            if (currentSent.contains(fName)) {
                                currentSent.remove(fName)
                                reqChanged = true
                            }
                            if (currentIncoming.contains(fName)) {
                                currentIncoming.remove(fName)
                                reqChanged = true
                            }
                        }
                        if (reqChanged) {
                            preferencesManager.saveSentRequests(currentSent)
                            preferencesManager.saveIncomingRequests(currentIncoming)
                        }

                        if (localChanged) {
                            preferencesManager.saveFriends(localFriends)
                        }
                    }

                    val context = getApplication<Application>()
                    var profilePicsUpdated = false
                    serverFriends.forEach { friendName ->
                        val remotePic = getRemoteValueChunked("profile_pic_${friendName.lowercase()}")
                        val dir = File(context.filesDir, "profile_pics")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "${friendName.lowercase()}.jpg")
                        if (remotePic.isNotEmpty()) {
                            try {
                                val bytes = android.util.Base64.decode(
                                    remotePic,
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                )
                                if (!file.exists() || !file.readBytes().contentEquals(bytes)) {
                                    file.writeBytes(bytes)
                                    profilePicsUpdated = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            if (file.exists()) {
                                file.delete()
                                profilePicsUpdated = true
                            }
                        }
                    }
                    if (profilePicsUpdated) {
                        withContext(Dispatchers.Main) {
                            _profileUpdateTrigger.value = System.currentTimeMillis()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 2. Sync Friend Requests
            val rawRequests = getRemoteValue("friend_requests_${myName.lowercase()}")
            if (rawRequests.isNotEmpty()) {
                try {
                    val decoded = decodeBase64(rawRequests)
                    val serverRequests = Json.decodeFromString<List<String>>(decoded)
                    
                    withContext(Dispatchers.Main) {
                        preferencesManager.saveIncomingRequests(serverRequests)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                withContext(Dispatchers.Main) {
                    preferencesManager.saveIncomingRequests(emptyList())
                }
            }
            
            // Refresh discoverable users
            refreshDiscoverableUsers()
        }
    }

    private fun sendSseSignal(friendUsername: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val signalMsg = ChatMessage(
                id = "${type}_${java.util.UUID.randomUUID()}",
                friendUsername = friendUsername,
                sender = myUsername.value,
                text = type,
                timestampMs = System.currentTimeMillis(),
                fileAttachment = null,
                status = type
            )
            val ntfyTopic = "gemini_chat_2026_${friendUsername.lowercase()}"
            var connection: HttpURLConnection? = null
            try {
                val jsonPayload = Json.encodeToString(signalMsg)
                val url = URL("https://ntfy.sh/$ntfyTopic")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 10000
                    connectTimeout = 10000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "text/plain")
                }
                connection.outputStream.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }
                connection.connect()
                connection.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun sendFriendRequest(toUsername: String, onResult: (Boolean, String?) -> Unit) {
        val myName = myUsername.value
        if (myName.isEmpty()) {
            onResult(false, "You are not registered")
            return
        }
        if (myName.equals(toUsername, ignoreCase = true)) {
            onResult(false, "You cannot add yourself")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val localFriends = preferencesManager.friendsFlow.firstOrNull() ?: emptyList()
            if (localFriends.any { it.username.equals(toUsername, ignoreCase = true) }) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Friend already added")
                }
                return@launch
            }
            val localSent = preferencesManager.sentRequestsFlow.firstOrNull() ?: emptyList()
            if (localSent.contains(toUsername)) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Request already sent")
                }
                return@launch
            }
            try {
                // 1. Update remote requests for B (toUsername)
                val rawRequests = getRemoteValue("friend_requests_${toUsername.lowercase()}")
                val requestsList = if (rawRequests.isEmpty()) {
                    mutableListOf()
                } else {
                    Json.decodeFromString<List<String>>(decodeBase64(rawRequests)).toMutableList()
                }
                if (!requestsList.contains(myName)) {
                    requestsList.add(myName)
                }
                val success = updateRemoteValue("friend_requests_${toUsername.lowercase()}", encodeBase64(Json.encodeToString(requestsList)))
                
                if (success) {
                    // 2. Save locally in A's sentRequests
                    withContext(Dispatchers.Main) {
                        val currentSent = (preferencesManager.sentRequestsFlow.firstOrNull() ?: emptyList()).toMutableList()
                        if (!currentSent.contains(toUsername)) {
                            currentSent.add(toUsername)
                            preferencesManager.saveSentRequests(currentSent)
                        }
                        onResult(true, null)
                    }
                    
                    // 3. Send SSE signal
                    sendSseSignal(toUsername, "friend_request")
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Failed to send request. Server error.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.message)
                }
            }
        }
    }

    fun acceptFriendRequest(fromUsername: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val myName = myUsername.value
        if (myName.isEmpty()) {
            onResult(false, "You are not registered")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update B's (our) remote friends
                val rawMyFriends = getRemoteValue("friends_${myName.lowercase()}")
                val myFriendsList = if (rawMyFriends.isEmpty()) {
                    mutableListOf()
                } else {
                    Json.decodeFromString<List<String>>(decodeBase64(rawMyFriends)).toMutableList()
                }
                if (!myFriendsList.contains(fromUsername)) {
                    myFriendsList.add(fromUsername)
                }
                updateRemoteValue("friends_${myName.lowercase()}", encodeBase64(Json.encodeToString(myFriendsList)))
                
                // 2. Update A's remote friends
                val rawAFriends = getRemoteValue("friends_${fromUsername.lowercase()}")
                val aFriendsList = if (rawAFriends.isEmpty()) {
                    mutableListOf()
                } else {
                    Json.decodeFromString<List<String>>(decodeBase64(rawAFriends)).toMutableList()
                }
                if (!aFriendsList.contains(myName)) {
                    aFriendsList.add(myName)
                }
                updateRemoteValue("friends_${fromUsername.lowercase()}", encodeBase64(Json.encodeToString(aFriendsList)))
                
                // 3. Update B's remote requests (remove A)
                val rawMyRequests = getRemoteValue("friend_requests_${myName.lowercase()}")
                if (rawMyRequests.isNotEmpty()) {
                    val myRequestsList = Json.decodeFromString<List<String>>(decodeBase64(rawMyRequests)).toMutableList()
                    myRequestsList.remove(fromUsername)
                    updateRemoteValue("friend_requests_${myName.lowercase()}", encodeBase64(Json.encodeToString(myRequestsList)))
                }
                
                // 4. Update B's local state
                withContext(Dispatchers.Main) {
                    // Add to friends
                    val currentFriends = (preferencesManager.friendsFlow.firstOrNull() ?: emptyList()).toMutableList()
                    if (currentFriends.none { it.username.equals(fromUsername, ignoreCase = true) }) {
                        currentFriends.add(Friend(username = fromUsername))
                        preferencesManager.saveFriends(currentFriends)
                    }
                    
                    // Remove from incoming requests
                    val currentIncoming = (preferencesManager.incomingRequestsFlow.firstOrNull() ?: emptyList()).toMutableList()
                    currentIncoming.remove(fromUsername)
                    preferencesManager.saveIncomingRequests(currentIncoming)
                }
                
                // 5. Send SSE friend_accept to A
                sendSseSignal(fromUsername, "friend_accept")
                
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.message)
                }
            }
        }
    }

    fun declineFriendRequest(fromUsername: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val myName = myUsername.value
        if (myName.isEmpty()) {
            onResult(false, "You are not registered")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update B's (our) remote requests (remove A)
                val rawMyRequests = getRemoteValue("friend_requests_${myName.lowercase()}")
                if (rawMyRequests.isNotEmpty()) {
                    val myRequestsList = Json.decodeFromString<List<String>>(decodeBase64(rawMyRequests)).toMutableList()
                    myRequestsList.remove(fromUsername)
                    updateRemoteValue("friend_requests_${myName.lowercase()}", encodeBase64(Json.encodeToString(myRequestsList)))
                }
                
                // 2. Update B's local state (remove from incoming requests)
                withContext(Dispatchers.Main) {
                    val currentIncoming = (preferencesManager.incomingRequestsFlow.firstOrNull() ?: emptyList()).toMutableList()
                    currentIncoming.remove(fromUsername)
                    preferencesManager.saveIncomingRequests(currentIncoming)
                }
                
                // 3. Send SSE decline to A
                sendSseSignal(fromUsername, "friend_decline")
                
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.message)
                }
            }
        }
    }

    fun registerUsername(username: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanUsername = username.trim()
            if (cleanUsername.isEmpty()) {
                withContext(Dispatchers.Main) { onResult(false, "Username cannot be empty") }
                return@launch
            }
            if (!cleanUsername.matches(Regex("^[a-zA-Z0-9_]{3,15}$"))) {
                withContext(Dispatchers.Main) { onResult(false, "Username must be 3-15 alphanumeric characters or underscore") }
                return@launch
            }
            _isRegisteringUsername.value = true
            
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://keyvalue.immanuel.co/api/KeyVal/GetValue/jsi08n3r/user_${cleanUsername.lowercase()}")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "GET"
                }
                connection.connect()
                
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    connection.disconnect()
                    
                    val cleaned = responseText.removeSurrounding("\"")
                    if (cleaned.isNotEmpty() && cleaned != "deleted") {
                        withContext(Dispatchers.Main) {
                            _isRegisteringUsername.value = false
                            onResult(false, "Username is already taken")
                        }
                    } else {
                        // Key does not exist, we can register it
                        val writeUrl = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/user_${cleanUsername.lowercase()}/registered")
                        val writeConn = (writeUrl.openConnection() as HttpURLConnection).apply {
                            readTimeout = 8000
                            connectTimeout = 8000
                            requestMethod = "POST"
                            doOutput = true
                            setRequestProperty("Content-Length", "0")
                        }
                        writeConn.connect()
                        
                        val writeCode = writeConn.responseCode
                        if (writeCode == HttpURLConnection.HTTP_OK) {
                            val writeResp = writeConn.inputStream.bufferedReader().use { it.readText() }.trim()
                            if (writeResp.contains("true", ignoreCase = true)) {
                                // Add to global registry of discoverable users
                                val rawReg = getRemoteValue("all_registered_users")
                                val regList = if (rawReg.isEmpty()) {
                                    mutableListOf()
                                } else {
                                    try {
                                        Json.decodeFromString<List<String>>(decodeBase64(rawReg)).toMutableList()
                                    } catch (e: Exception) {
                                        mutableListOf()
                                    }
                                }
                                if (!regList.contains(cleanUsername)) {
                                    regList.add(cleanUsername)
                                    val encodedReg = encodeBase64(Json.encodeToString(regList))
                                    updateRemoteValue("all_registered_users", encodedReg)
                                }
                                
                                // Initialize friends and requests on the server
                                val emptyListEncoded = encodeBase64("[]")
                                updateRemoteValue("friends_${cleanUsername.lowercase()}", emptyListEncoded)
                                updateRemoteValue("friend_requests_${cleanUsername.lowercase()}", emptyListEncoded)

                                withContext(Dispatchers.Main) {
                                    preferencesManager.saveMyUsername(cleanUsername)
                                    _isRegisteringUsername.value = false
                                    onResult(true, null)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _isRegisteringUsername.value = false
                                    onResult(false, "Failed to register. Server returned false.")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                    _isRegisteringUsername.value = false
                                onResult(false, "Failed to register. Server error: $writeCode")
                            }
                        }
                        writeConn.disconnect()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isRegisteringUsername.value = false
                        onResult(false, "Server error: $code")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isRegisteringUsername.value = false
                    onResult(false, "Network error: ${e.message}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun removeFriend(friend: Friend) {
        val myName = myUsername.value
        if (myName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update A's remote friends
                val rawMyFriends = getRemoteValue("friends_${myName.lowercase()}")
                if (rawMyFriends.isNotEmpty()) {
                    val myFriendsList = Json.decodeFromString<List<String>>(decodeBase64(rawMyFriends)).toMutableList()
                    myFriendsList.remove(friend.username)
                    updateRemoteValue("friends_${myName.lowercase()}", encodeBase64(Json.encodeToString(myFriendsList)))
                }
                
                // 2. Update B's remote friends
                val rawBFriends = getRemoteValue("friends_${friend.username.lowercase()}")
                if (rawBFriends.isNotEmpty()) {
                    val bFriendsList = Json.decodeFromString<List<String>>(decodeBase64(rawBFriends)).toMutableList()
                    bFriendsList.remove(myName)
                    updateRemoteValue("friends_${friend.username.lowercase()}", encodeBase64(Json.encodeToString(bFriendsList)))
                }
                
                // 3. Send SSE unfriend to B
                sendSseSignal(friend.username, "friend_unfriend")
                
                // 4. Update locally
                withContext(Dispatchers.Main) {
                    val currentFriends = (preferencesManager.friendsFlow.firstOrNull() ?: emptyList()).toMutableList()
                    currentFriends.removeIf { it.username.equals(friend.username, ignoreCase = true) }
                    preferencesManager.saveFriends(currentFriends)
                    
                    val currentMessages = (preferencesManager.chatMessagesFlow.firstOrNull() ?: emptyList()).filter { 
                        !it.friendUsername.equals(friend.username, ignoreCase = true) 
                    }
                    preferencesManager.saveChatMessages(currentMessages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postMessageToNetwork(friendUsername: String, chatMsg: ChatMessage) {
        val ntfyTopic = "gemini_chat_2026_${friendUsername.lowercase()}"
        var connection: HttpURLConnection? = null
        try {
            val attachmentForFriend = chatMsg.fileAttachment?.copy(filePath = "")
            val jsonPayload = Json.encodeToString(
                chatMsg.copy(
                    sender = myUsername.value,
                    fileAttachment = attachmentForFriend
                )
            )
            val url = URL("https://ntfy.sh/$ntfyTopic")
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 10000
                connectTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "text/plain")
            }
            connection.outputStream.use { out ->
                out.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }
            connection.connect()
            val code = connection.responseCode
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }

    fun sendChatMessage(friendUsername: String, text: String, attachment: FileAttachment? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = java.util.UUID.randomUUID().toString()
            val chatMsg = ChatMessage(
                id = id,
                friendUsername = friendUsername,
                sender = "me",
                text = text,
                timestampMs = System.currentTimeMillis(),
                fileAttachment = attachment,
                status = "sent"
            )
            
            withContext(Dispatchers.Main) {
                val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                current.add(chatMsg)
                preferencesManager.saveChatMessages(current)
            }
            
            postMessageToNetwork(friendUsername, chatMsg)
        }
    }

    fun uploadAndSendFile(context: Context, friendUsername: String, filePath: String, fileType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            uploadAndSendFileSync(context, friendUsername, filePath, fileType)
        }
    }

    suspend fun uploadAndSendFileSync(context: Context, friendUsername: String, filePath: String, fileType: String) = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var fileName = "shared_file"
        var fileSize = 0L
        
        try {
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                var resolvedName: String? = null
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            resolvedName = c.getString(nameIdx)
                        }
                    }
                }
                fileName = resolvedName ?: "shared_audio.mp3"
                
                var uploadFileName = fileName
                if (fileType == "news_article" || fileName.endsWith(".html", ignoreCase = true)) {
                    if (uploadFileName.endsWith(".html", ignoreCase = true)) {
                        uploadFileName = uploadFileName.substring(0, uploadFileName.length - 5) + ".txt"
                    } else {
                        uploadFileName = "$uploadFileName.txt"
                    }
                }
                
                val cacheFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}_$uploadFileName")
                if (fileType == "news_article") {
                    val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                    val encodedBytes = android.util.Base64.encode(inputBytes, android.util.Base64.DEFAULT)
                    cacheFile.writeBytes(encodedBytes)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (cacheFile.exists()) {
                    tempFile = cacheFile
                    fileSize = cacheFile.length()
                }
            } else {
                val originalFile = File(filePath)
                if (originalFile.exists()) {
                    fileName = originalFile.name
                    fileSize = originalFile.length()
                    
                    var uploadFileName = fileName
                    if (fileType == "news_article" || fileName.endsWith(".html", ignoreCase = true)) {
                        if (uploadFileName.endsWith(".html", ignoreCase = true)) {
                            uploadFileName = uploadFileName.substring(0, uploadFileName.length - 5) + ".txt"
                        } else {
                            uploadFileName = "$uploadFileName.txt"
                        }
                    }
                    
                    val cacheFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}_$uploadFileName")
                    if (fileType == "news_article") {
                        val originalBytes = originalFile.readBytes()
                        val encodedBytes = android.util.Base64.encode(originalBytes, android.util.Base64.DEFAULT)
                        cacheFile.writeBytes(encodedBytes)
                    } else {
                        originalFile.inputStream().use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (cacheFile.exists()) {
                        tempFile = cacheFile
                        fileSize = cacheFile.length()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (tempFile == null || !tempFile.exists()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to read file for sharing", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }
        
        var uploadFileName = fileName
        if (fileType == "news_article" || fileName.endsWith(".html", ignoreCase = true)) {
            if (uploadFileName.endsWith(".html", ignoreCase = true)) {
                uploadFileName = uploadFileName.substring(0, uploadFileName.length - 5) + ".txt"
            } else {
                uploadFileName = "$uploadFileName.txt"
            }
        }
        
        val id = java.util.UUID.randomUUID().toString()
        val pendingAttachment = FileAttachment(
            fileName = fileName,
            filePath = filePath,
            fileType = fileType,
            fileSize = fileSize,
            downloadUrl = null
        )
        val pendingMsg = ChatMessage(
            id = id,
            friendUsername = friendUsername,
            sender = "me",
            text = "Shared a file: $fileName",
            timestampMs = System.currentTimeMillis(),
            fileAttachment = pendingAttachment,
            status = "going"
        )
        
        withContext(Dispatchers.Main) {
            val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            current.add(pendingMsg)
            preferencesManager.saveChatMessages(current)
        }
        
        var downloadUrl: String? = null
        
        // Primary: tmpfiles.org
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("https://tmpfiles.org/api/v1/upload")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 60000
                connectTimeout = 30000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }
            
            val header = "--$boundary\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$uploadFileName\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n"
            val headerBytes = header.toByteArray(Charsets.UTF_8)
            val footer = "\r\n--$boundary--\r\n"
            val footerBytes = footer.toByteArray(Charsets.UTF_8)
            val totalLength = headerBytes.size + tempFile.length() + footerBytes.size
            
            connection.setFixedLengthStreamingMode(totalLength)
            
            connection.outputStream.use { outputStream ->
                outputStream.write(headerBytes)
                tempFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
                outputStream.write(footerBytes)
                outputStream.flush()
            }
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseObj = org.json.JSONObject(response)
                if (responseObj.optString("status") == "success") {
                    val rawUrl = responseObj.optJSONObject("data")?.optString("url")
                    if (!rawUrl.isNullOrEmpty()) {
                        downloadUrl = rawUrl.replace("https://tmpfiles.org/", "https://tmpfiles.org/dl/")
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback: file.io
        if (downloadUrl == null) {
            try {
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = URL("https://file.io")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 60000
                    connectTimeout = 30000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
                
                val header = "--$boundary\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"$uploadFileName\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n"
                val headerBytes = header.toByteArray(Charsets.UTF_8)
                val footer = "\r\n--$boundary--\r\n"
                val footerBytes = footer.toByteArray(Charsets.UTF_8)
                val totalLength = headerBytes.size + tempFile.length() + footerBytes.size
                
                connection.setFixedLengthStreamingMode(totalLength)
                
                connection.outputStream.use { outputStream ->
                    outputStream.write(headerBytes)
                    tempFile.inputStream().use { input ->
                        input.copyTo(outputStream)
                    }
                    outputStream.write(footerBytes)
                    outputStream.flush()
                }
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseObj = org.json.JSONObject(response)
                    if (responseObj.optBoolean("success", false)) {
                        downloadUrl = responseObj.optString("link")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Clean up temporary file
        try {
            tempFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (downloadUrl != null) {
            withContext(Dispatchers.Main) {
                val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                val index = current.indexOfFirst { it.id == id }
                if (index != -1) {
                    val finalAttachment = pendingAttachment.copy(downloadUrl = downloadUrl)
                    val finalMsg = pendingMsg.copy(
                        fileAttachment = finalAttachment,
                        status = "sent"
                    )
                    current[index] = finalMsg
                    preferencesManager.saveChatMessages(current)
                    
                    viewModelScope.launch(Dispatchers.IO) {
                        postMessageToNetwork(friendUsername, finalMsg)
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                val index = current.indexOfFirst { it.id == id }
                if (index != -1) {
                    current[index] = pendingMsg.copy(
                        text = "Failed to share file: $fileName (Upload failed)",
                        status = "failed"
                    )
                    preferencesManager.saveChatMessages(current)
                }
            }
        }
    }

    private fun startNtfyListener(username: String) {
        sseJob?.cancel()
        sseJob = viewModelScope.launch(Dispatchers.IO) {
            val ntfyTopic = "gemini_chat_2026_${username.lowercase()}"
            var backoffMs = 2000L
            var lastEventTime = preferencesManager.lastEventTimeFlow.firstOrNull() ?: 0L
            while (isActive) {
                var connection: HttpURLConnection? = null
                var reader: java.io.BufferedReader? = null
                try {
                    val sinceParam = if (lastEventTime > 0L) {
                        "?since=$lastEventTime"
                    } else {
                        "?since=all"
                    }
                    val url = URL("https://ntfy.sh/$ntfyTopic/json$sinceParam")
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        readTimeout = 0
                        connectTimeout = 15000
                        requestMethod = "GET"
                    }
                    connection.connect()
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        backoffMs = 2000L
                        reader = connection.inputStream.bufferedReader()
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.trim().isEmpty()) continue

                            try {
                                val jsonObj = org.json.JSONObject(line)
                                val event = jsonObj.optString("event")
                                val timeSec = jsonObj.optLong("time", 0L)
                                if (timeSec > lastEventTime) {
                                    lastEventTime = timeSec
                                    preferencesManager.saveLastEventTime(lastEventTime)
                                }
                                
                                if (event == "message") {
                                    val rawMessageText = jsonObj.optString("message")
                                    val chatMsg = Json.decodeFromString<ChatMessage>(rawMessageText)
                                    if (chatMsg.id.startsWith("typing_")) {
                                        val sender = chatMsg.sender.lowercase()
                                        typingFriends.update { it + (sender to System.currentTimeMillis()) }
                                    } else if (chatMsg.id.startsWith("delivered_")) {
                                        val originalId = chatMsg.id.removePrefix("delivered_")
                                        updateMessageStatus(originalId, "delivered")
                                    } else if (chatMsg.id.startsWith("read_")) {
                                        val originalId = chatMsg.id.removePrefix("read_")
                                        updateMessageStatus(originalId, "read")
                                    } else if (chatMsg.id.startsWith("friend_request_")) {
                                        val sender = chatMsg.sender
                                        withContext(Dispatchers.Main) {
                                            val currentIncoming = preferencesManager.incomingRequestsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                            if (!currentIncoming.contains(sender)) {
                                                currentIncoming.add(sender)
                                                preferencesManager.saveIncomingRequests(currentIncoming)
                                            }
                                            showFriendNotification("Friend Request", "New friend request from $sender")
                                        }
                                    } else if (chatMsg.id.startsWith("friend_accept_")) {
                                        val sender = chatMsg.sender
                                        withContext(Dispatchers.Main) {
                                            val currentFriends = preferencesManager.friendsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                            if (currentFriends.none { it.username.equals(sender, ignoreCase = true) }) {
                                                currentFriends.add(Friend(username = sender))
                                                preferencesManager.saveFriends(currentFriends)
                                            }
                                            val currentSent = preferencesManager.sentRequestsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                            currentSent.remove(sender)
                                            preferencesManager.saveSentRequests(currentSent)
                                            showFriendNotification("Friend Request Accepted", "$sender accepted your friend request!")
                                        }
                                    } else if (chatMsg.id.startsWith("friend_decline_")) {
                                        val sender = chatMsg.sender
                                        withContext(Dispatchers.Main) {
                                            val currentSent = preferencesManager.sentRequestsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                            currentSent.remove(sender)
                                            preferencesManager.saveSentRequests(currentSent)
                                        }
                                    } else if (chatMsg.id.startsWith("friend_unfriend_")) {
                                        val sender = chatMsg.sender
                                        withContext(Dispatchers.Main) {
                                            val currentFriends = preferencesManager.friendsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                            currentFriends.removeIf { it.username.equals(sender, ignoreCase = true) }
                                            preferencesManager.saveFriends(currentFriends)
                                            
                                            val currentMessages = preferencesManager.chatMessagesFlow.firstOrNull()?.filter { 
                                                !it.friendUsername.equals(sender, ignoreCase = true) 
                                            } ?: emptyList()
                                            preferencesManager.saveChatMessages(currentMessages)
                                        }
                                    } else if (chatMsg.id.startsWith("profile_pic_update_")) {
                                        val sender = chatMsg.sender
                                        viewModelScope.launch(Dispatchers.IO) {
                                            val context = getApplication<Application>()
                                            val remotePic = getRemoteValueChunked("profile_pic_${sender.lowercase()}")
                                            val dir = File(context.filesDir, "profile_pics")
                                            if (!dir.exists()) dir.mkdirs()
                                            val file = File(dir, "${sender.lowercase()}.jpg")
                                            if (remotePic.isNotEmpty()) {
                                                try {
                                                    val bytes = android.util.Base64.decode(
                                                        remotePic,
                                                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                                    )
                                                    file.writeBytes(bytes)
                                                    withContext(Dispatchers.Main) {
                                                        _profileUpdateTrigger.value = System.currentTimeMillis()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                if (file.exists()) {
                                                    file.delete()
                                                    withContext(Dispatchers.Main) {
                                                        _profileUpdateTrigger.value = System.currentTimeMillis()
                                                    }
                                                }
                                            }
                                        }
                                    } else if (chatMsg.id.startsWith("force_logout_")) {
                                        withContext(Dispatchers.Main) {
                                            preferencesManager.saveMyUsername("")
                                            preferencesManager.saveFriends(emptyList())
                                            preferencesManager.saveChatMessages(emptyList())
                                            preferencesManager.saveIncomingRequests(emptyList())
                                            preferencesManager.saveSentRequests(emptyList())
                                        }
                                    } else if (chatMsg.id.startsWith("group_sync_")) {
                                        try {
                                            val group = Json.decodeFromString<TaskGroup>(chatMsg.text)
                                            val me = myUsername.value.lowercase()
                                            if (group.members.contains(me)) {
                                                withContext(Dispatchers.Main) {
                                                    val groups = preferencesManager.taskGroupsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    val idx = groups.indexOfFirst { it.id == group.id }
                                                    val isNew = idx == -1
                                                    if (isNew) groups.add(group) else groups[idx] = group
                                                    preferencesManager.saveTaskGroups(groups)
                                                    if (isNew) showFriendNotification("Group Task", "You were added to group '${group.name}'")
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("grouptask_upsert_")) {
                                        try {
                                            val task = Json.decodeFromString<GroupTask>(chatMsg.text)
                                            val me = myUsername.value.lowercase()
                                            val inGroup = preferencesManager.taskGroupsFlow.firstOrNull()
                                                ?.any { it.id == task.groupId && it.members.contains(me) } == true
                                            if (inGroup) {
                                                withContext(Dispatchers.Main) {
                                                    val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    val idx = current.indexOfFirst { it.id == task.id }
                                                    if (idx != -1) current[idx] = task else current.add(task)
                                                    preferencesManager.saveGroupTasks(current)
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("grouptask_done_")) {
                                        try {
                                            val task = Json.decodeFromString<GroupTask>(chatMsg.text)
                                            withContext(Dispatchers.Main) {
                                                val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                val idx = current.indexOfFirst { it.id == task.id }
                                                if (idx != -1) {
                                                    current[idx] = task
                                                    preferencesManager.saveGroupTasks(current)
                                                    showFriendNotification("Task Completed", "${task.doneBy} finished '${task.title}'")
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("grouptask_delete_")) {
                                        try {
                                            val taskId = chatMsg.text.split("|").getOrNull(1) ?: ""
                                            if (taskId.isNotEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    val current = preferencesManager.groupTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    current.removeIf { it.id == taskId }
                                                    preferencesManager.saveGroupTasks(current)
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("shared_task_upsert_")) {
                                        try {
                                            val shared = Json.decodeFromString<SharedTask>(chatMsg.text)
                                            val subscribed = preferencesManager.subscribedUsersFlow.firstOrNull() ?: emptyList()
                                            if (subscribed.any { it.equals(shared.ownerUsername, ignoreCase = true) }) {
                                                withContext(Dispatchers.Main) {
                                                    val list = preferencesManager.sharedTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    val idx = list.indexOfFirst {
                                                        it.ownerUsername.equals(shared.ownerUsername, ignoreCase = true) && it.taskId == shared.taskId
                                                    }
                                                    if (idx != -1) list[idx] = shared else list.add(shared)
                                                    preferencesManager.saveSharedTasks(list)
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("shared_task_delete_")) {
                                        try {
                                            val parts = chatMsg.text.split("|")
                                            val owner = parts.getOrNull(0)?.lowercase() ?: ""
                                            val taskId = parts.getOrNull(1)?.toIntOrNull() ?: -1
                                            if (owner.isNotEmpty() && taskId != -1) {
                                                withContext(Dispatchers.Main) {
                                                    val list = preferencesManager.sharedTasksFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    list.removeIf { it.ownerUsername.equals(owner, ignoreCase = true) && it.taskId == taskId }
                                                    preferencesManager.saveSharedTasks(list)
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else if (chatMsg.id.startsWith("group_member_left_")) {
                                        try {
                                            val parts = chatMsg.text.split("|")
                                            val groupId = parts.getOrNull(0) ?: ""
                                            val leaver = parts.getOrNull(1)?.lowercase() ?: ""
                                            if (groupId.isNotEmpty() && leaver.isNotEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    val groups = preferencesManager.taskGroupsFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                                                    val idx = groups.indexOfFirst { it.id == groupId }
                                                    if (idx != -1) {
                                                        val g = groups[idx]
                                                        groups[idx] = g.copy(members = g.members.filter { it != leaver })
                                                        preferencesManager.saveTaskGroups(groups)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    } else {
                                        handleIncomingMessage(chatMsg)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { reader?.close() } catch (_: Exception) {}
                    connection?.disconnect()
                }
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(60000L)
            }
        }
    }

    private suspend fun handleIncomingMessage(chatMsg: ChatMessage) {
        // Discard if the sender is not our friend (read directly from Flow to avoid StateFlow lag)
        val localFriends = preferencesManager.friendsFlow.firstOrNull() ?: emptyList()
        val isFriend = localFriends.any { it.username.equals(chatMsg.sender, ignoreCase = true) }
        if (!isFriend) return

        withContext(Dispatchers.Main) {
            val currentList = (preferencesManager.chatMessagesFlow.firstOrNull() ?: emptyList()).toMutableList()
            if (currentList.none { it.id == chatMsg.id }) {
                val isActiveChat = activeChatFriend.value?.equals(chatMsg.sender, ignoreCase = true) == true
                val finalStatus = if (isActiveChat) "read" else "delivered"
                
                val adjustedMsg = chatMsg.copy(
                    friendUsername = chatMsg.sender,
                    status = finalStatus
                )
                currentList.add(adjustedMsg)
                preferencesManager.saveChatMessages(currentList)
                
                val currentFriends = (preferencesManager.friendsFlow.firstOrNull() ?: emptyList()).toMutableList()
                if (currentFriends.none { it.username.equals(chatMsg.sender, ignoreCase = true) }) {
                    currentFriends.add(Friend(username = chatMsg.sender))
                    preferencesManager.saveFriends(currentFriends)
                }

                // Send receipts back to sender
                sendReceipt(chatMsg.sender, chatMsg.id, "delivered")
                if (isActiveChat) {
                    sendReceipt(chatMsg.sender, chatMsg.id, "read")
                } else {
                    showChatNotification(adjustedMsg)
                }

                chatMsg.fileAttachment?.let { attachment ->
                    val fileExists = if (attachment.filePath.isNotEmpty()) File(attachment.filePath).exists() else false
                    if (attachment.downloadUrl != null && (!fileExists || attachment.filePath.isEmpty())) {
                        downloadSharedFileInBackground(adjustedMsg, attachment)
                    }
                }
            }
        }
    }

    private fun downloadSharedFileInBackground(message: ChatMessage, attachment: FileAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(attachment.downloadUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 15000
                    connectTimeout = 15000
                    requestMethod = "GET"
                }
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val sharedDir = File(getApplication<Application>().filesDir, "shared_files")
                    if (!sharedDir.exists()) sharedDir.mkdirs()
                    
                    val file = File(sharedDir, "${System.currentTimeMillis()}_${attachment.fileName}")
                    connection.inputStream.use { input ->
                        if (attachment.fileType == "news_article") {
                            val encodedBytes = input.readBytes()
                            val decodedBytes = android.util.Base64.decode(encodedBytes, android.util.Base64.DEFAULT)
                            file.writeBytes(decodedBytes)
                        } else {
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        val currentList = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
                        val index = currentList.indexOfFirst { it.id == message.id }
                        if (index != -1) {
                            val updatedAttachment = attachment.copy(filePath = file.absolutePath)
                            currentList[index] = message.copy(fileAttachment = updatedAttachment)
                            preferencesManager.saveChatMessages(currentList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            val index = current.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val msg = current[index]
                msg.fileAttachment?.filePath?.let { path ->
                    if (path.isNotEmpty()) {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
                current.removeAt(index)
                preferencesManager.saveChatMessages(current)
            }
        }
    }

    fun deleteChatMessages(messageIds: Set<String>) {
        viewModelScope.launch {
            val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            var modified = false
            for (messageId in messageIds) {
                val index = current.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    val msg = current[index]
                    msg.fileAttachment?.filePath?.let { path ->
                        if (path.isNotEmpty()) {
                            val file = File(path)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    }
                    current.removeAt(index)
                    modified = true
                }
            }
            if (modified) {
                preferencesManager.saveChatMessages(current)
            }
        }
    }

    fun clearChatMessages(friendUsername: String) {
        viewModelScope.launch {
            val current = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            val toRemove = current.filter { it.friendUsername.equals(friendUsername, ignoreCase = true) }
            if (toRemove.isNotEmpty()) {
                for (msg in toRemove) {
                    msg.fileAttachment?.filePath?.let { path ->
                        if (path.isNotEmpty()) {
                            val file = File(path)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    }
                    current.remove(msg)
                }
                preferencesManager.saveChatMessages(current)
            }
        }
    }

    fun changeAppIcon(aliasName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val packageName = context.packageName
            val aliases = listOf(
                "com.example.myapp.MainActivityDefault",
                "com.example.myapp.MainActivityBlue",
                "com.example.myapp.MainActivityRed",
                "com.example.myapp.MainActivityGreen",
                "com.example.myapp.MainActivityGold",
                "com.example.myapp.MainActivityDark"
            )
            
            val activeAlias = "com.example.myapp.$aliasName"
            
            // First, enable the new component
            pm.setComponentEnabledSetting(
                ComponentName(packageName, activeAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            
            // Then, disable all others
            aliases.forEach { alias ->
                if (alias != activeAlias) {
                    pm.setComponentEnabledSetting(
                        ComponentName(packageName, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            // Save active icon name to PreferencesManager
            preferencesManager.saveActiveAppIcon(aliasName)
        }
    }

    fun updateProfilePicture(context: Context, uri: Uri) {
        val myName = myUsername.value
        if (myName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    // Resize bitmap to max 160x160 to keep Base64 small
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 160, 160, true)
                    
                    // Create directory if not exists
                    val dir = File(context.filesDir, "profile_pics")
                    if (!dir.exists()) dir.mkdirs()
                    
                    val file = File(dir, "${myName.lowercase()}.jpg")
                    FileOutputStream(file).use { outStream ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                        outStream.flush()
                    }
                    
                    // Read file bytes for base64 encoding
                    val bytes = file.readBytes()
                    val base64Str = android.util.Base64.encodeToString(
                        bytes,
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    
                    // Update remote key-value storage using chunked upload
                    updateRemoteValueChunked("profile_pic_${myName.lowercase()}", base64Str)
                    
                    // Notify all friends about the profile picture change
                    try {
                        val localFriends = preferencesManager.friendsFlow.firstOrNull() ?: emptyList()
                        localFriends.forEach { friend ->
                            sendSseSignal(friend.username, "profile_pic_update")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // Trigger state refresh
                    withContext(Dispatchers.Main) {
                        _profileUpdateTrigger.value = System.currentTimeMillis()
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    fun importReceivedNewsArticle(context: Context, attachment: FileAttachment, sender: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalFile = File(attachment.filePath)
                if (!originalFile.exists()) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }
                
                val offlineDir = File(context.filesDir, "offline_articles")
                if (!offlineDir.exists()) offlineDir.mkdirs()
                
                val newFile = File(offlineDir, originalFile.name)
                originalFile.inputStream().use { input ->
                    newFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val currentList = downloadedNews.value.toMutableList()
                    val cleanedTitle = attachment.fileName
                        .replace(".html", "", ignoreCase = true)
                        .replace(Regex("^article_\\d+_"), "")
                        .replace("_", " ")
                    
                    val newArticle = DownloadedNewsArticle(
                        title = cleanedTitle,
                        link = attachment.downloadUrl ?: "",
                        pubDate = "Shared",
                        source = "Shared by $sender",
                        timestampMs = System.currentTimeMillis(),
                        localFilePath = newFile.absolutePath
                    )
                    
                    if (currentList.none { it.localFilePath == newArticle.localFilePath }) {
                        currentList.add(newArticle)
                        preferencesManager.saveDownloadedNews(currentList)
                    }
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun toggleFavoriteWebsite(title: String, url: String) {
        viewModelScope.launch {
            val current = favoriteWebsites.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.url == url }
            if (existingIndex != -1) {
                current.removeAt(existingIndex)
            } else {
                current.add(FavoriteWebsite(title = title, url = url))
            }
            preferencesManager.saveFavoriteWebsites(current)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // If GitHub is configured, check GitHub Releases instead of KeyValue
                if (GITHUB_OWNER != "YOUR_GITHUB_USERNAME" && GITHUB_REPO != "YOUR_GITHUB_REPOSITORY") {
                    val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        readTimeout = 8000
                        connectTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        setRequestProperty("User-Agent", "MyApp-Updater")
                    }
                    connection.connect()
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(responseString)
                        val tagName = json.getString("tag_name")
                        
                        // Extract number from tag (e.g., "v16" -> 16, "v1.0.16" -> 16)
                        val cleaned = tagName.replace("[^0-9.]".toRegex(), "")
                        val remoteVersion = if (cleaned.contains(".")) {
                            cleaned.split(".").lastOrNull()?.toIntOrNull() ?: 1
                        } else {
                            cleaned.toIntOrNull() ?: 1
                        }
                        
                        // Find apk download url
                        val assets = json.getJSONArray("assets")
                        var dlUrl = ""
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk")) {
                                dlUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                        
                        if (dlUrl.isEmpty()) {
                            // If no APK asset is found, use the release HTML page url
                            dlUrl = json.getString("html_url")
                        }
                        
                        preferencesManager.saveLatestVersion(remoteVersion)
                        preferencesManager.saveUpdateDownloadUrl(dlUrl)
                        
                        withContext(Dispatchers.Main) {
                            _isUpdateAvailable.value = remoteVersion > currentVersionCode
                        }
                    }
                    connection.disconnect()
                    return@launch
                }

                // Fallback: Fetch Latest Version Code from KeyValue API
                val url = URL("https://keyvalue.immanuel.co/api/KeyVal/GetValue/jsi08n3r/latest_version")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "GET"
                }
                connection.connect()
                var remoteVersion = 1
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val raw = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    val cleaned = raw.removeSurrounding("\"")
                    if (cleaned.isNotEmpty()) {
                        remoteVersion = cleaned.toIntOrNull() ?: 1
                        preferencesManager.saveLatestVersion(remoteVersion)
                    }
                }
                connection.disconnect()

                // Fetch Download URL from KeyValue API
                val downloadUrl = URL("https://keyvalue.immanuel.co/api/KeyVal/GetValue/jsi08n3r/update_download_url")
                val dlConn = (downloadUrl.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "GET"
                }
                dlConn.connect()
                var dlUrl = ""
                if (dlConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val raw = dlConn.inputStream.bufferedReader().use { it.readText() }.trim()
                    val cleaned = raw.removeSurrounding("\"")
                    if (cleaned.isNotEmpty()) {
                        dlUrl = try {
                            val decodedBytes = android.util.Base64.decode(
                                cleaned,
                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                            )
                            String(decodedBytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            cleaned
                        }
                        preferencesManager.saveUpdateDownloadUrl(dlUrl)
                    }
                }
                dlConn.disconnect()

                withContext(Dispatchers.Main) {
                    _isUpdateAvailable.value = remoteVersion > currentVersionCode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadProgress.value = 0f
            
            // Check if this looks like a webpage rather than a direct APK download
            if (!downloadUrl.contains(".apk", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = null
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open browser link", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            }

            try {
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "MyApp-Updater")
                conn.connect()
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val fileLength = conn.contentLength
                    val updateDir = File(context.filesDir, "shared_files")
                    if (!updateDir.exists()) {
                        updateDir.mkdirs()
                    }
                    val apkFile = File(updateDir, "update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }
                    
                    conn.inputStream.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (fileLength > 0) {
                                    _downloadProgress.value = totalBytesRead.toFloat() / fileLength.toFloat()
                                }
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = null
                        installApk(context, apkFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = null
                        Toast.makeText(context, "Download failed: HTTP ${conn.responseCode}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = null
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            // Check if we need to request REQUEST_INSTALL_PACKAGES permission (Android 8.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Please allow this app to install unknown apps in settings, then try again.", Toast.LENGTH_LONG).show()
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun publishAppUpdate(
        versionCode: Int,
        downloadUrl: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isPublishingUpdate.value = true
            try {
                // 1. Publish Version Code
                val versionUrl = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/latest_version/$versionCode")
                val versionConn = (versionUrl.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Length", "0")
                }
                versionConn.connect()
                versionConn.responseCode
                versionConn.disconnect()

                // 2. Publish Download URL
                val encodedUrl = android.util.Base64.encodeToString(
                    downloadUrl.toByteArray(Charsets.UTF_8),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                )
                val dlUrl = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/update_download_url/$encodedUrl")
                val dlConn = (dlUrl.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Length", "0")
                }
                dlConn.connect()
                val responseCode = dlConn.responseCode
                dlConn.disconnect()

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Failed to publish download URL. Server returned: $responseCode")
                }

                // 3. Send ntfy trigger
                val ntfyUrl = URL("https://ntfy.sh/gemini_ota_updates_2026")
                val ntfyConn = (ntfyUrl.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                }
                ntfyConn.outputStream.use { out ->
                    out.write("update".toByteArray(Charsets.UTF_8))
                }
                ntfyConn.connect()
                ntfyConn.responseCode
                ntfyConn.disconnect()

                // Instantly update our local state
                preferencesManager.saveLatestVersion(versionCode)
                preferencesManager.saveUpdateDownloadUrl(downloadUrl)

                withContext(Dispatchers.Main) {
                    _isPublishingUpdate.value = false
                    _isUpdateAvailable.value = versionCode > currentVersionCode
                    onResult(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isPublishingUpdate.value = false
                    onResult(false, e.message)
                }
            }
        }
    }

    private var otaSseJob: Job? = null
    private fun startOtaNtfyListener() {
        otaSseJob?.cancel()
        otaSseJob = viewModelScope.launch(Dispatchers.IO) {
            val ntfyTopic = "gemini_ota_updates_2026"
            var backoffMs = 2000L
            while (isActive) {
                var connection: HttpURLConnection? = null
                var reader: java.io.BufferedReader? = null
                try {
                    val url = URL("https://ntfy.sh/$ntfyTopic/json")
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        readTimeout = 0
                        connectTimeout = 15000
                        requestMethod = "GET"
                    }
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        backoffMs = 2000L
                        reader = connection.inputStream.bufferedReader()
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.trim().isEmpty()) continue

                            try {
                                val jsonObj = org.json.JSONObject(line)
                                val event = jsonObj.optString("event")
                                if (event == "message") {
                                    checkForUpdates()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { reader?.close() } catch (_: Exception) {}
                    connection?.disconnect()
                }
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(60000L)
            }
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val username = myUsername.value
            if (username.isEmpty()) {
                withContext(Dispatchers.Main) { onResult(false, "No username registered") }
                return@launch
            }
            
            var connection: HttpURLConnection? = null
            try {
                // 1. Release username
                val url = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/user_${username.lowercase()}/deleted")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Length", "0")
                }
                connection.connect()
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    
                    // 2. Remove from global registry
                    val rawReg = getRemoteValue("all_registered_users")
                    if (rawReg.isNotEmpty()) {
                        try {
                            val regList = Json.decodeFromString<List<String>>(decodeBase64(rawReg)).toMutableList()
                            regList.remove(username)
                            val encodedReg = encodeBase64(Json.encodeToString(regList))
                            updateRemoteValue("all_registered_users", encodedReg)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // 3. Unfriend all friends on the server
                    val rawMyFriends = getRemoteValue("friends_${username.lowercase()}")
                    if (rawMyFriends.isNotEmpty()) {
                        try {
                            val myFriendsList = Json.decodeFromString<List<String>>(decodeBase64(rawMyFriends))
                            myFriendsList.forEach { friendName ->
                                val rawBFriends = getRemoteValue("friends_${friendName.lowercase()}")
                                if (rawBFriends.isNotEmpty()) {
                                    val bFriendsList = Json.decodeFromString<List<String>>(decodeBase64(rawBFriends)).toMutableList()
                                    bFriendsList.remove(username)
                                    updateRemoteValue("friends_${friendName.lowercase()}", encodeBase64(Json.encodeToString(bFriendsList)))
                                }
                                sendSseSignal(friendName, "friend_unfriend")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // 4. Delete own remote keys
                    updateRemoteValue("friends_${username.lowercase()}", encodeBase64("[]"))
                    updateRemoteValue("friend_requests_${username.lowercase()}", encodeBase64("[]"))
                    
                    // 5. Update local state
                    withContext(Dispatchers.Main) {
                        preferencesManager.saveMyUsername("")
                        preferencesManager.saveFriends(emptyList())
                        preferencesManager.saveChatMessages(emptyList())
                        preferencesManager.saveIncomingRequests(emptyList())
                        preferencesManager.saveSentRequests(emptyList())
                        onResult(true, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Server error: $code")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error: ${e.message}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun adminDeleteUser(targetUsername: String, onResult: (Boolean, String?) -> Unit) {
        val targetLower = targetUsername.trim().lowercase()
        if (targetLower.isEmpty()) {
            onResult(false, "Username cannot be empty")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // 1. Release username: set user_{targetLower} to "deleted"
                val url = URL("https://keyvalue.immanuel.co/api/KeyVal/UpdateValue/jsi08n3r/user_$targetLower/deleted")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Length", "0")
                }
                connection.connect()
                val code = connection.responseCode
                connection.disconnect()
                
                if (code == HttpURLConnection.HTTP_OK) {
                    // 2. Remove from global registered users list
                    val rawReg = getRemoteValue("all_registered_users")
                    if (rawReg.isNotEmpty()) {
                        try {
                            val regList = Json.decodeFromString<List<String>>(decodeBase64(rawReg)).toMutableList()
                            val iterator = regList.iterator()
                            while (iterator.hasNext()) {
                                if (iterator.next().equals(targetUsername, ignoreCase = true)) {
                                    iterator.remove()
                                }
                            }
                            val encodedReg = encodeBase64(Json.encodeToString(regList))
                            updateRemoteValue("all_registered_users", encodedReg)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // 3. Clean up friendships (remove target user from friends' lists)
                    val rawFriends = getRemoteValue("friends_$targetLower")
                    if (rawFriends.isNotEmpty()) {
                        try {
                            val friendsList = Json.decodeFromString<List<String>>(decodeBase64(rawFriends))
                            friendsList.forEach { friendName ->
                                val rawBFriends = getRemoteValue("friends_${friendName.lowercase()}")
                                if (rawBFriends.isNotEmpty()) {
                                    val bFriendsList = Json.decodeFromString<List<String>>(decodeBase64(rawBFriends)).toMutableList()
                                    bFriendsList.removeIf { it.equals(targetUsername, ignoreCase = true) }
                                    updateRemoteValue("friends_${friendName.lowercase()}", encodeBase64(Json.encodeToString(bFriendsList)))
                                }
                                sendSseSignal(friendName, "friend_unfriend")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // 4. Reset target user's remote files
                    updateRemoteValue("friends_$targetLower", encodeBase64("[]"))
                    updateRemoteValue("friend_requests_$targetLower", encodeBase64("[]"))
                    
                    // 5. Send force logout SSE signal
                    sendSseSignal(targetUsername, "force_logout")
                    
                    withContext(Dispatchers.Main) {
                        _registeredUsers.value = _registeredUsers.value.filter { !it.equals(targetUsername, ignoreCase = true) }
                        onResult(true, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Server returned error: $code")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Network error occurred")
                }
            }
        }
    }

    fun fetchAllRegisteredUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingRegisteredUsers.value = true
            try {
                val raw = getRemoteValue("all_registered_users")
                if (raw.isNotEmpty()) {
                    val list = Json.decodeFromString<List<String>>(decodeBase64(raw))
                    _registeredUsers.value = list
                } else {
                    _registeredUsers.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isFetchingRegisteredUsers.value = false
            }
        }
    }

    private var presenceJob: Job? = null
    private fun startPresenceHeartbeat(username: String) {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateRemoteValue("presence_${username.lowercase()}", System.currentTimeMillis().toString())
                delay(30000)
            }
        }
    }

    private var friendsPresenceJob: Job? = null
    private fun startFriendsPresenceCheck() {
        friendsPresenceJob?.cancel()
        friendsPresenceJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val myName = myUsername.value
                if (myName.isNotEmpty()) {
                    val localFriends = preferencesManager.friendsFlow.firstOrNull() ?: emptyList()
                    if (localFriends.isNotEmpty()) {
                        try {
                            val updated = coroutineScope {
                                localFriends.map { friend ->
                                    async {
                                        val rawPresence = getRemoteValue("presence_${friend.username.lowercase()}")
                                        val lastActiveTime = rawPresence.toLongOrNull() ?: 0L
                                        val isOnline = System.currentTimeMillis() - lastActiveTime < 60000L
                                        val statusStr = if (isOnline) "Online" else "Offline"
                                        friend.copy(status = statusStr)
                                    }
                                }.map { it.await() }
                            }
                            if (updated != localFriends) {
                                withContext(Dispatchers.Main) {
                                    preferencesManager.saveFriends(updated)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                delay(30000)
            }
        }
    }

    private var lastTypingSentTime = 0L
    fun sendTypingIndicator(friendUsername: String) {
        val now = System.currentTimeMillis()
        if (now - lastTypingSentTime < 2500) return
        lastTypingSentTime = now
        
        viewModelScope.launch(Dispatchers.IO) {
            val id = "typing_${java.util.UUID.randomUUID()}"
            val typingMsg = ChatMessage(
                id = id,
                friendUsername = friendUsername,
                sender = myUsername.value,
                text = "typing",
                timestampMs = now,
                fileAttachment = null
            )
            val ntfyTopic = "gemini_chat_2026_${friendUsername.lowercase()}"
            var connection: HttpURLConnection? = null
            try {
                val jsonPayload = Json.encodeToString(typingMsg)
                val url = URL("https://ntfy.sh/$ntfyTopic")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 10000
                    connectTimeout = 10000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "text/plain")
                }
                connection.outputStream.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }
                connection.connect()
                connection.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun updateMessageStatus(messageId: String, newStatus: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentList = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            val index = currentList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val msg = currentList[index]
                val shouldUpdate = when {
                    msg.status == "read" -> false
                    msg.status == "delivered" && newStatus == "delivered" -> false
                    else -> true
                }
                if (shouldUpdate) {
                    currentList[index] = msg.copy(status = newStatus)
                    preferencesManager.saveChatMessages(currentList)
                }
            }
        }
    }

    private fun sendReceipt(friendUsername: String, messageId: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val receiptMsg = ChatMessage(
                id = "${type}_${messageId}",
                friendUsername = friendUsername,
                sender = myUsername.value,
                text = type,
                timestampMs = System.currentTimeMillis(),
                fileAttachment = null,
                status = type
            )
            val ntfyTopic = "gemini_chat_2026_${friendUsername.lowercase()}"
            var connection: HttpURLConnection? = null
            try {
                val jsonPayload = Json.encodeToString(receiptMsg)
                val url = URL("https://ntfy.sh/$ntfyTopic")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 10000
                    connectTimeout = 10000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "text/plain")
                }
                connection.outputStream.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }
                connection.connect()
                connection.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun markMessagesAsRead(friendUsername: String) {
        viewModelScope.launch {
            val currentList = preferencesManager.chatMessagesFlow.firstOrNull()?.toMutableList() ?: mutableListOf()
            var changed = false
            currentList.forEachIndexed { index, msg ->
                if (msg.friendUsername.equals(friendUsername, ignoreCase = true) && 
                    !msg.sender.equals("me", ignoreCase = true) && 
                    msg.status != "read"
                ) {
                    currentList[index] = msg.copy(status = "read")
                    changed = true
                    sendReceipt(friendUsername, msg.id, "read")
                }
            }
            if (changed) {
                preferencesManager.saveChatMessages(currentList)
            }
        }
    }

    private fun showChatNotification(chatMsg: ChatMessage) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "chat_messages"
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, com.example.myapp.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatMsg.sender.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, "chat_messages")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(chatMsg.sender)
            .setContentText(chatMsg.text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        notificationManager.notify(chatMsg.sender.hashCode(), builder.build())
    }

    private fun showFriendNotification(title: String, text: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "friend_updates"
            val channel = NotificationChannel(
                channelId,
                "Friend Requests & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for friend requests and updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, com.example.myapp.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, "friend_updates")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        notificationManager.notify(title.hashCode(), builder.build())
    }

    fun saveShortcutPosition(label: String, x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentMap = preferencesManager.shortcutPositionsFlow.firstOrNull()?.toMutableMap() ?: mutableMapOf()
            currentMap[label] = "$x,$y"
            preferencesManager.saveShortcutPositions(currentMap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
        otaSseJob?.cancel()
        presenceJob?.cancel()
        friendsPresenceJob?.cancel()
        musicPlayerManager.release()
    }
}
