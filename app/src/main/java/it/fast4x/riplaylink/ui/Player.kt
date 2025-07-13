package it.fast4x.riplaylink.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplaylink.MainActivity
import it.fast4x.riplaylink.R
import it.fast4x.riplaylink.getLastYTVideoId
import it.fast4x.riplaylink.getLastYTVideoSeconds
import it.fast4x.riplaylink.service.CommandService
import it.fast4x.riplaylink.ui.customui.CustomDefaultPlayerUiController
import it.fast4x.riplaylink.utils.isLandscape
import it.fast4x.riplaylink.utils.lastVideoIdKey
import it.fast4x.riplaylink.utils.lastVideoSecondsKey
import it.fast4x.riplaylink.utils.rememberPreference
import kotlinx.coroutines.delay

@Composable
fun Player(
    innerPadding: PaddingValues
) {

    val context = LocalContext.current
    val inflatedView = LayoutInflater.from(context).inflate(R.layout.youtube_player, null, false)
    val onlinePlayerView: YouTubePlayerView = inflatedView as YouTubePlayerView
    val player = remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var enableBackgroundPlayback by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = isLandscape
    var mediaId by remember { mutableStateOf("Tmod0giDy0o") }
    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    val commandService = remember { CommandService(
        context as MainActivity,
        onCommandLoad = { id, position ->
            println("CommandService onCommandPlay $id $position")
            player.value?.loadVideo(id, position)
        },
        onCommandPlay = {
            println("CommandService onCommandPause")
            player.value?.play()
        },
        onCommandPause = {
            println("CommandService onCommandPause")
            player.value?.pause()
        }
    ) }

    LaunchedEffect(Unit) {
        commandService.start()
    }
    

    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.Center),
            visible = showControls && isLandscape,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Gray.copy(alpha = .4f), RoundedCornerShape(12.dp))
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
            ) {
                //TODO Implement controls
                //controlsContent(Modifier.padding(top = 20.dp))
            }
        }

        AndroidView(
            modifier = Modifier
                .background(Color.Transparent)
                .zIndex(0f),
            factory = {
                if (onlinePlayerView.parent != null) {
                    (onlinePlayerView.parent as ViewGroup).removeView(onlinePlayerView) // <- fix
                }
//                val iFramePlayerOptions = IFramePlayerOptions.Builder()
//                    .controls(1) // show/hide controls
//                    .rel(0) // related video at the end
//                    .ivLoadPolicy(0) // show/hide annotations
//                    .ccLoadPolicy(0) // show/hide captions
//                    // Play a playlist by id
//                    //.listType("playlist")
//                    //.list(PLAYLIST_ID)
//                    .build()

                // Disable default view controls to set custom view
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

                        /* Used to show custom player ui with uiController as listener
//                            val customPlayerUiController = CustomBasePlayerUiControllerAsListener(
//                                it,
//                                customPLayerUi,
//                                youTubePlayer,
//                                onlinePlayerView,
//                                onTap = {
//                                    showControls = !showControls
//                                }
//                            )
//                            youTubePlayer.addListener(customPlayerUiController)
*/

// Used to show default player ui with defaultPlayerUiController as custom view
                        val customUiController =
                            CustomDefaultPlayerUiController(
                                onlinePlayerView,
                                youTubePlayer,
                                onTap = {
                                    showControls = !showControls
                                }
                            )
                        customUiController.showUi(false) // disable all default controls and buttons
                        customUiController.showMenuButton(false)
                        customUiController.showVideoTitle(false)
                        customUiController.showPlayPauseButton(false)
                        customUiController.showDuration(false)
                        customUiController.showCurrentTime(false)
                        customUiController.showSeekBar(false)
                        customUiController.showBufferingProgress(false)
                        customUiController.showYouTubeButton(false)
                        customUiController.showFullscreenButton(false)
                        onlinePlayerView.setCustomPlayerUi(customUiController.rootView)

                        if (playerState.value == PlayerConstants.PlayerState.UNSTARTED
                            || playerState.value != PlayerConstants.PlayerState.BUFFERING
                        )
                            youTubePlayer.loadVideo(
                                mediaId,
                                if (mediaId == getLastYTVideoId()) getLastYTVideoSeconds() else 0f
                            )

                        //youTubePlayer.cueVideo(mediaId, 0f)


                    }

                    override fun onCurrentSecond(
                        youTubePlayer: YouTubePlayer,
                        second: Float
                    ) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond = second
                        lastYTVideoSeconds = second
                        lastYTVideoId = mediaId
                    }

                    override fun onVideoDuration(
                        youTubePlayer: YouTubePlayer,
                        duration: Float
                    ) {
                        super.onVideoDuration(youTubePlayer, duration)
                        currentDuration = duration
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        super.onStateChange(youTubePlayer, state)
//                        if (state == PlayerConstants.PlayerState.ENDED) {
//                            onVideoEnded()
//                        }
                        playerState.value = state

                    }

                    override fun onPlaybackQualityChange(
                        youTubePlayer: YouTubePlayer,
                        playbackQuality: PlayerConstants.PlaybackQuality
                    ) {
                        super.onPlaybackQualityChange(youTubePlayer, playbackQuality)
                        println("OnlinePlayer onPlaybackQualityChange $playbackQuality")
                    }


                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false

                    if (enableBackgroundPlayback)
                        enableBackgroundPlayback(true)
                    else
                        lifecycleOwner.lifecycle.addObserver(this)

                    initialize(listener, iFramePlayerOptions)
                }

            },
            update = {
                it.enableBackgroundPlayback(enableBackgroundPlayback)
                it.layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
        )
    }

}