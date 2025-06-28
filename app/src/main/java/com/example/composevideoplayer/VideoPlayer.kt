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
)

class VideoPlayer : ViewModel() {
    lateinit var player: ExoPlayer

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState
    var job: Job? = null

    fun setMediaItem(mediaItem: MediaItem, context: Context) {
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            addListener(object : Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    _playerState.update {
                        it.copy(
                            isPlaying = player.isPlaying,
                            duration = player.duration.toFloat(),
                            title = player.mediaMetadata.title.toString()
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
        _playerState.update { it.copy(ready = true) }
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun forward(time: Long) {
        player.seekTo(player.currentPosition + time)
    }

    fun reply(time: Long) {
        player.seekTo(player.currentPosition - time)
    }

    fun seekTo(time: Long) {
        player.seekTo(time)
    }

    fun dispose() {
        player.stop()
        player.release()
        _playerState.update { PlayerState() }
    }

}