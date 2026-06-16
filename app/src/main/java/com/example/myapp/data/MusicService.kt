package com.example.myapp.data

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.example.myapp.MainActivity

class MusicService : Service() {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    
    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    var onTrackComplete: (() -> Unit)? = null

    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 2002
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun play(track: MusicTrack) {
        stopPlayback()
        _currentTrack.value = track
        
        try {
            val file = File(track.filePath)
            mediaPlayer = MediaPlayer().apply {
                if (!file.exists()) {
                    setDataSource(applicationContext, Uri.parse(track.filePath))
                } else {
                    setDataSource(file.absolutePath)
                }
                prepare()
                isLooping = _isRepeatEnabled.value
                start()
            }
            
            _isPlaying.value = true
            _durationMs.value = mediaPlayer?.duration ?: 0
            
            mediaPlayer?.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0
                stopPositionUpdate()
                onTrackComplete?.invoke()
            }
            
            startPositionUpdate()
            showNotification(track, true)
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopPositionUpdate()
                _currentTrack.value?.let { showNotification(it, false) }
            }
        }
    }

    fun resume() {
        val track = _currentTrack.value ?: return
        mediaPlayer?.let { player ->
            player.start()
            _isPlaying.value = true
            startPositionUpdate()
            showNotification(track, true)
        } ?: play(track)
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun setRepeatEnabled(enabled: Boolean) {
        _isRepeatEnabled.value = enabled
        mediaPlayer?.isLooping = enabled
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _currentTrack.value = null
        _isPlaying.value = false
        stopPositionUpdate()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startPositionUpdate() {
        stopPositionUpdate()
        positionJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdate() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification channel for offline music player"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(track: MusicTrack, isPlaying: Boolean) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateActiveTrack(track: MusicTrack) {
        if (_currentTrack.value?.id == track.id) {
            _currentTrack.value = track
            showNotification(track, _isPlaying.value)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlayback()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        scope.cancel()
    }
}
