package ru.netology.nework.media

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class PlaybackState(
    val currentPostId: Long? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val isVideo: Boolean = false
)

class MediaPlaybackManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    var exoPlayer: ExoPlayer? = null
        private set

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _errorFlow = MutableSharedFlow<PlaybackException>(extraBufferCapacity = 1)
    val errorFlow: SharedFlow<PlaybackException> = _errorFlow.asSharedFlow()

    private var lifecycleOwner: LifecycleOwner? = null
    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                exoPlayer?.seekTo(0)
                exoPlayer?.pause()
            }
            updateState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            manageProgressJob(isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorFlow.tryEmit(error)
            stopAndReset()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        lifecycleOwner = owner
        exoPlayer = ExoPlayer.Builder(context).build().apply { addListener(listener) }
    }

    private fun updateState() {
        val player = exoPlayer ?: return
        if (_playbackState.value.currentPostId == null) return
        _playbackState.update { state ->
            state.copy(
                isPlaying = player.isPlaying,
                duration = player.duration.coerceAtLeast(0L),
                progress = player.currentPosition.coerceAtLeast(0L)
            )
        }
    }

    private fun manageProgressJob(isPlaying: Boolean) {
        progressJob?.cancel()
        if (isPlaying) {
            progressJob = lifecycleOwner?.lifecycleScope?.launch {
                while (isActive) {
                    updateState()
                    delay(200.milliseconds)
                }
            }
        }
    }

    fun togglePlay(postId: Long, url: String, isVideo: Boolean = false) {
        val player = exoPlayer ?: return
        if (_playbackState.value.currentPostId == postId) {
            if (!isVideo) {
                if (player.isPlaying) player.pause() else player.play()
            }
            return
        }

        stopAndReset()
        _playbackState.value = PlaybackState(
            currentPostId = postId,
            isVideo = isVideo
        )
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun seekTo(postId: Long, position: Long) {
        val player = exoPlayer ?: return
        if (_playbackState.value.currentPostId == postId) {
            player.seekTo(position)
            updateState()
        }
    }

    fun stopAndReset() {
        progressJob?.cancel()
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            clearVideoSurface()
        }
        _playbackState.value = PlaybackState()
    }

    override fun onStop(owner: LifecycleOwner) {
        exoPlayer?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopAndReset()
        exoPlayer?.apply {
            removeListener(listener)
            release()
        }
        exoPlayer = null
        lifecycleOwner = null
    }
}