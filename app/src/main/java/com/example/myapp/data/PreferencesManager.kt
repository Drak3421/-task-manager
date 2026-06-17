package com.example.myapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val WELCOME_NAME_KEY = stringPreferencesKey("welcome_name")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val BACKGROUND_URI_KEY = stringPreferencesKey("background_uri")
        val CLOCK_LAYOUT_KEY = stringPreferencesKey("clock_layout")
        private val TASKS_KEY = stringPreferencesKey("tasks_json")
        val MUSIC_TRACKS_KEY = stringPreferencesKey("music_tracks_json")
        val YOUTUBE_CHANNELS_KEY = stringPreferencesKey("youtube_channels_json")
        val NEWS_TOPICS_KEY = stringPreferencesKey("news_topics_json")
        val RECENT_VIDEOS_KEY = stringPreferencesKey("recent_videos_json")
        val RECENT_NEWS_KEY = stringPreferencesKey("recent_news_json")
        val DOWNLOADED_NEWS_KEY = stringPreferencesKey("downloaded_news_json")
        val LAST_HISTORY_RESET_KEY = longPreferencesKey("last_history_reset")
        val MY_USERNAME_KEY = stringPreferencesKey("my_username")
        val FRIENDS_KEY = stringPreferencesKey("friends_json")
        val CHAT_MESSAGES_KEY = stringPreferencesKey("chat_messages_json")
        val FAVORITE_WEBSITES_KEY = stringPreferencesKey("favorite_websites_json")
        val LATEST_VERSION_KEY = androidx.datastore.preferences.core.intPreferencesKey("latest_version")
        val UPDATE_DOWNLOAD_URL_KEY = stringPreferencesKey("update_download_url")
        val INCOMING_REQUESTS_KEY = stringPreferencesKey("incoming_requests_json")
        val SENT_REQUESTS_KEY = stringPreferencesKey("sent_requests_json")
        val LAST_EVENT_TIME_KEY = longPreferencesKey("last_event_time")
        val ACTIVE_APP_ICON_KEY = stringPreferencesKey("active_app_icon")
        val TASK_GROUPS_KEY = stringPreferencesKey("task_groups_json")
        val GROUP_TASKS_KEY = stringPreferencesKey("group_tasks_json")
    }

    val tasksFlow: Flow<List<Task>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[TASKS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveTasks(tasks: List<Task>) {
        context.dataStore.edit { preferences ->
            preferences[TASKS_KEY] = Json.encodeToString(tasks)
        }
    }

    val musicTracksFlow: Flow<List<MusicTrack>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[MUSIC_TRACKS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveMusicTracks(tracks: List<MusicTrack>) {
        context.dataStore.edit { preferences ->
            preferences[MUSIC_TRACKS_KEY] = Json.encodeToString(tracks)
        }
    }

    val welcomeNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WELCOME_NAME_KEY] ?: "User"
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: true // Apple clock uses dark mode by default
    }
    
    val backgroundUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BACKGROUND_URI_KEY]
    }

    val clockLayoutFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CLOCK_LAYOUT_KEY] ?: "digital"
    }

    suspend fun setClockLayout(layout: String) {
        context.dataStore.edit { preferences ->
            preferences[CLOCK_LAYOUT_KEY] = layout
        }
    }

    suspend fun saveWelcomeName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[WELCOME_NAME_KEY] = name
        }
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }
    
    suspend fun setBackgroundUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(BACKGROUND_URI_KEY)
            } else {
                preferences[BACKGROUND_URI_KEY] = uri
            }
        }
    }

    val youtubeChannelsFlow: Flow<List<YouTubeChannel>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[YOUTUBE_CHANNELS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveYouTubeChannels(channels: List<YouTubeChannel>) {
        context.dataStore.edit { preferences ->
            preferences[YOUTUBE_CHANNELS_KEY] = Json.encodeToString(channels)
        }
    }

    val newsTopicsFlow: Flow<List<NewsTopic>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[NEWS_TOPICS_KEY]
            if (json == null) {
                listOf(NewsTopic(id = "ai", query = "Artificial Intelligence"))
            } else {
                try {
                    Json.decodeFromString(json)
                } catch (e: Exception) {
                    listOf(NewsTopic(id = "ai", query = "Artificial Intelligence"))
                }
            }
        }

    suspend fun saveNewsTopics(topics: List<NewsTopic>) {
        context.dataStore.edit { preferences ->
            preferences[NEWS_TOPICS_KEY] = Json.encodeToString(topics)
        }
    }

    val recentVideosFlow: Flow<List<YouTubeVideo>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[RECENT_VIDEOS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveRecentVideos(videos: List<YouTubeVideo>) {
        context.dataStore.edit { preferences ->
            preferences[RECENT_VIDEOS_KEY] = Json.encodeToString(videos)
        }
    }

    val recentNewsFlow: Flow<List<AiNewsArticle>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[RECENT_NEWS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveRecentNews(news: List<AiNewsArticle>) {
        context.dataStore.edit { preferences ->
            preferences[RECENT_NEWS_KEY] = Json.encodeToString(news)
        }
    }

    val lastHistoryResetFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[LAST_HISTORY_RESET_KEY] ?: 0L
        }

    suspend fun saveLastHistoryResetTime(timeMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_HISTORY_RESET_KEY] = timeMs
        }
    }

    val downloadedNewsFlow: Flow<List<DownloadedNewsArticle>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[DOWNLOADED_NEWS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveDownloadedNews(articles: List<DownloadedNewsArticle>) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOADED_NEWS_KEY] = Json.encodeToString(articles)
        }
    }

    val myUsernameFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[MY_USERNAME_KEY] ?: ""
        }

    suspend fun saveMyUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[MY_USERNAME_KEY] = username
        }
    }

    val friendsFlow: Flow<List<Friend>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[FRIENDS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveFriends(friends: List<Friend>) {
        context.dataStore.edit { preferences ->
            preferences[FRIENDS_KEY] = Json.encodeToString(friends)
        }
    }

    val chatMessagesFlow: Flow<List<ChatMessage>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[CHAT_MESSAGES_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveChatMessages(messages: List<ChatMessage>) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_MESSAGES_KEY] = Json.encodeToString(messages)
        }
    }

    val favoriteWebsitesFlow: Flow<List<FavoriteWebsite>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[FAVORITE_WEBSITES_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveFavoriteWebsites(websites: List<FavoriteWebsite>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_WEBSITES_KEY] = Json.encodeToString(websites)
        }
    }

    val latestVersionFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LATEST_VERSION_KEY] ?: 1
    }

    suspend fun saveLatestVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[LATEST_VERSION_KEY] = version
        }
    }

    val updateDownloadUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_DOWNLOAD_URL_KEY] ?: ""
    }

    suspend fun saveUpdateDownloadUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_DOWNLOAD_URL_KEY] = url
        }
    }

    val incomingRequestsFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[INCOMING_REQUESTS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveIncomingRequests(requests: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[INCOMING_REQUESTS_KEY] = Json.encodeToString(requests)
        }
    }

    val sentRequestsFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[SENT_REQUESTS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveSentRequests(requests: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[SENT_REQUESTS_KEY] = Json.encodeToString(requests)
        }
    }

    val lastEventTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_EVENT_TIME_KEY] ?: 0L
    }

    suspend fun saveLastEventTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_EVENT_TIME_KEY] = time
        }
    }

    val taskGroupsFlow: Flow<List<TaskGroup>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[TASK_GROUPS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveTaskGroups(groups: List<TaskGroup>) {
        context.dataStore.edit { preferences ->
            preferences[TASK_GROUPS_KEY] = Json.encodeToString(groups)
        }
    }

    val groupTasksFlow: Flow<List<GroupTask>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val json = preferences[GROUP_TASKS_KEY] ?: "[]"
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveGroupTasks(tasks: List<GroupTask>) {
        context.dataStore.edit { preferences ->
            preferences[GROUP_TASKS_KEY] = Json.encodeToString(tasks)
        }
    }

    val activeAppIconFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[ACTIVE_APP_ICON_KEY] ?: "MainActivityDefault"
        }

    suspend fun saveActiveAppIcon(iconName: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_APP_ICON_KEY] = iconName
        }
    }
}
