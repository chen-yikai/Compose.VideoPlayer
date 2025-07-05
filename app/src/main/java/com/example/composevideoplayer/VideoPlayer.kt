package com.example.composevideoplayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_PLAYER_ERROR
import androidx.media3.common.Player.Listener
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class PlayerState(
    var isPlaying: Boolean = false,
    var ready: Boolean = false,
    var title: String = "",
    var currentPosition: Float = 0f,
    var duration: Float = 0f,
    var mediaUri: String = "",
    var wasPlaying: Boolean = false, // Track if was playing before pause
)

class VideoPlayer : ViewModel() {
    private var _player: ExoPlayer? = null
    val player: ExoPlayer get() = _player!!
    val playerOrNull: ExoPlayer? get() = _player

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState
    private var job: Job? = null
    private var isPlayerInitialized = false
    
    // Saved state for configuration changes
    private var savedPosition: Long = 0L
    private var savedWasPlaying: Boolean = false
    private var savedMediaItem: MediaItem? = null

    fun setMediaItem(mediaItem: MediaItem, context: Context) {
        if (!isPlayerInitialized) {
            savedMediaItem = mediaItem
            _player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(mediaItem)
                prepare()
                
                // Restore saved state if available
                if (savedPosition > 0) {
                    seekTo(savedPosition)
                    playWhenReady = savedWasPlaying
                } else {
                    playWhenReady = true // Start playing on first load
                }
                
                addListener(object : Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
                        
                        // Save current state for configuration changes
                        savedPosition = player.currentPosition
                        savedWasPlaying = player.isPlaying
                        
                        _playerState.update {
                            it.copy(
                                isPlaying = player.isPlaying,
                                duration = player.duration.toFloat(),
                                title = player.mediaMetadata.title.toString(),
                                mediaUri = mediaItem.localConfiguration?.uri.toString(),
                                wasPlaying = player.isPlaying
                            )
                        }
                        
                        job?.cancel()
                        job = viewModelScope.launch {
                            while (true) {
                                _playerState.update {
                                    it.copy(currentPosition = player.currentPosition.toFloat())
                                }
                                delay(500L)
                            }
                        }
                    }
                })
            }
            isPlayerInitialized = true
            _playerState.update { it.copy(ready = true) }
        }
    }
    
    fun reinitializePlayer(context: Context) {
        if (savedMediaItem != null && _player == null) {
            isPlayerInitialized = false // Reset initialization flag
            setMediaItem(savedMediaItem!!, context)
        }
    }

    fun toggle() {
        _player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun forward(time: Long) {
        _player?.let {
            it.seekTo(it.currentPosition + time)
        }
    }

    fun reply(time: Long) {
        _player?.let {
            it.seekTo(it.currentPosition - time)
        }
    }

    fun seekTo(time: Long) {
        _player?.seekTo(time)
    }

    fun dispose() {
        job?.cancel()
        _player?.let {
            // Save current state before disposal
            savedPosition = it.currentPosition
            savedWasPlaying = it.isPlaying
            it.stop()
            it.release()
        }
        _player = null
        isPlayerInitialized = false
        // Don't reset PlayerState completely - keep saved values
        _playerState.update { 
            it.copy(ready = false) 
        }
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }

}
