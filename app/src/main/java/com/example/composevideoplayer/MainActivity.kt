package com.example.composevideoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.compose.PlayerSurface
import com.example.composevideoplayer.ui.theme.ComposeVideoPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeVideoPlayerTheme {
//                AndroidPlayer()
                ExoPlayerSurface()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSurface() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val videoPlayer: VideoPlayer = viewModel()
    val playerState by videoPlayer.playerState.collectAsState()

    LaunchedEffect(context) {
        if (!playerState.ready) {
            val mediaItem =
                MediaItem.Builder().setUri("file:///android_asset/BigBuckBunny.mp4")
                    .setMediaMetadata(MediaMetadata.Builder().setTitle("Big Buck Bunny").build())
                    .build()
            videoPlayer.setMediaItem(mediaItem, context)
        } else {
            videoPlayer.reinitializePlayer(context)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    videoPlayer.reinitializePlayer(context)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    if (!lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        videoPlayer.dispose()
                    }
                }
                else -> {}
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
        }
    }

    if (playerState.ready && videoPlayer.playerOrNull != null)
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerSurface(
                player = videoPlayer.player,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(playerState.title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = playerState.currentPosition,
                    valueRange = 0f..if (playerState.duration < 0f) 0f else playerState.duration,
                    onValueChange = {
                        videoPlayer.seekTo(it.toLong())
                    })
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        FloatingActionButton(onClick = {
                            videoPlayer.reply(30000)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.reply_30),
                                contentDescription = null
                            )
                        }
                        FloatingActionButton(onClick = {
                            videoPlayer.toggle()
                        }) {
                            Icon(
                                painter = painterResource(if (playerState.isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = null
                            )
                        }
                        FloatingActionButton(onClick = {
                            videoPlayer.forward(30000)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.forward_30),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

}

@OptIn(UnstableApi::class)
@Composable
fun AndroidPlayer(videoPlayer: VideoPlayer = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playerState by videoPlayer.playerState.collectAsState()

    LaunchedEffect(Unit) {
        val mediaItem =
            MediaItem.Builder().setUri("file:///android_asset/BigBuckBunny.mp4")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Big Buck Bunny").build())
                .build()
        videoPlayer.setMediaItem(mediaItem, context)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            videoPlayer.dispose()
        }
    }

    if (playerState.ready)
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                modifier = Modifier
                    .padding(horizontal = 100.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                factory = {
                    PlayerView(context).apply {
                        player = videoPlayer.player
                        useController = false
                    }
                })
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(playerState.title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = playerState.currentPosition,
                    valueRange = 0f..if (playerState.duration < 0f) 0f else playerState.duration,
                    onValueChange = {
                        videoPlayer.seekTo(it.toLong())
                    })
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        FloatingActionButton(onClick = {
                            videoPlayer.reply(30000)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.reply_30),
                                contentDescription = null
                            )
                        }
                        FloatingActionButton(onClick = {
                            videoPlayer.toggle()
                        }) {
                            Icon(
                                painter = painterResource(if (playerState.isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = null
                            )
                        }
                        FloatingActionButton(onClick = {
                            videoPlayer.forward(30000)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.forward_30),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
}