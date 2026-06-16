package com.example.myapp.data

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(private val context: Context) {
    private var musicService: MusicService? = null
    private var isBound = false

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

    private var pendingPlayTrack: MusicTrack? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            val srv = binder.getService()
            musicService = srv
            isBound = true

            // Set track completion callback
            srv.onTrackComplete = {
                onTrackComplete?.invoke()
            }

            // Sync states
            scope.launch {
                srv.currentTrack.collect { _currentTrack.value = it }
            }
            scope.launch {
                srv.isPlaying.collect { _isPlaying.value = it }
            }
            scope.launch {
                srv.currentPositionMs.collect { _currentPositionMs.value = it }
            }
            scope.launch {
                srv.durationMs.collect { _durationMs.value = it }
            }
            scope.launch {
                srv.isRepeatEnabled.collect { _isRepeatEnabled.value = it }
            }

            // Play pending track if any
            pendingPlayTrack?.let {
                srv.play(it)
                pendingPlayTrack = null
            }
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    init {
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun play(track: MusicTrack) {
        val intent = Intent(context, MusicService::class.java)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            context.startService(intent)
        }
        
        val service = musicService
        if (service != null) {
            service.play(track)
        } else {
            pendingPlayTrack = track
        }
    }

    fun pause() {
        musicService?.pause()
    }

    fun resume() {
        musicService?.resume()
    }

    fun seekTo(positionMs: Int) {
        musicService?.seekTo(positionMs)
    }

    fun setRepeatEnabled(enabled: Boolean) {
        musicService?.setRepeatEnabled(enabled)
    }

    fun skipNext(tracks: List<MusicTrack>) {
        val track = _currentTrack.value ?: return
        if (tracks.isEmpty()) return
        val currentIndex = tracks.indexOfFirst { it.id == track.id }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % tracks.size
            play(tracks[nextIndex])
        } else {
            // Fallback for standalone/received track: play the first track in the library
            play(tracks.first())
        }
    }

    fun skipPrevious(tracks: List<MusicTrack>) {
        val track = _currentTrack.value ?: return
        if (tracks.isEmpty()) return
        val currentIndex = tracks.indexOfFirst { it.id == track.id }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
            play(tracks[prevIndex])
        } else {
            // Fallback for standalone/received track: play the first track in the library
            play(tracks.first())
        }
    }

    fun stopPlayback() {
        musicService?.stopPlayback()
    }

    fun updateActiveTrack(track: MusicTrack) {
        if (_currentTrack.value?.id == track.id) {
            _currentTrack.value = track
            musicService?.updateActiveTrack(track)
        }
    }

    fun release() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        musicService = null
        scope.cancel()
    }
}
