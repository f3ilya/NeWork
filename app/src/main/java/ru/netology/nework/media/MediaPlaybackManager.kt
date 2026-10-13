package ru.netology.nework.media

import android.content.Context
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@ActivityScoped
class MediaPlaybackManager @Inject constructor(
    @ApplicationContext context: Context
) : DefaultLifecycleObserver {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    var currentPlayingPostId: Long? = null
        private set
    private var currentSeekBar: SeekBar? = null
    private var currentPlayButton: MaterialButton? = null
    private var currentVideoPlayerView: PlayerView? = null
    private var currentVideoPlaceholder: View? = null
    private var onErrorListener: ((String) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) currentSeekBar?.max = exoPlayer.duration.toInt()
                progressUpdate(exoPlayer.isPlaying)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                currentPlayButton?.isChecked = isPlaying
                progressUpdate(isPlaying)
            }
            override fun onPlayerError(error: PlaybackException) {
                onErrorListener?.invoke(AppError.from(error.cause ?: error).code)
                stopAndReset()
            }
        })
    }

    private fun progressUpdate(isPlaying: Boolean) {
        progressJob?.cancel()
        if (isPlaying) {
            progressJob = scope.launch {
                while (isActive) {
                    currentSeekBar?.progress = exoPlayer.currentPosition.toInt()
                    delay(200.milliseconds)
                }
            }
        }
    }

    private fun setupSeekBarListener(seekBar: SeekBar) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) exoPlayer.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun prepareMedia(postId: Long, url: String, isVideo: Boolean = false) {
        if (currentPlayingPostId == postId) {
            if (!isVideo) {
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    currentSeekBar?.progress = 0
                }
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
            return
        }
        stopAndReset()
        currentPlayingPostId = postId
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setOnErrorListener(listener: (String) -> Unit) { this.onErrorListener = listener }


    fun playAudio(postId: Long, url: String, seekBar: SeekBar, playButton: MaterialButton) {
        val sameTrack = currentPlayingPostId == postId
        prepareMedia(postId, url)
        if (sameTrack) return

        currentSeekBar = seekBar
        currentPlayButton = playButton
        exoPlayer.clearVideoSurface()
        setupSeekBarListener(seekBar)
    }

    fun rebindAudio(seekBar: SeekBar, playButton: MaterialButton) {
        currentSeekBar = seekBar
        currentPlayButton = playButton
        currentPlayButton?.isChecked = exoPlayer.isPlaying
        currentSeekBar?.max = exoPlayer.duration.toInt()
        currentSeekBar?.progress = exoPlayer.currentPosition.toInt()
        setupSeekBarListener(seekBar)
        progressUpdate(exoPlayer.isPlaying)
    }

    fun playVideo(postId: Long, url: String, playerView: PlayerView, placeholder: View) {
        val sameVideo = currentPlayingPostId == postId
        prepareMedia(postId, url, true)
        if (sameVideo) return

        rebindVideo(playerView, placeholder)
    }

    fun rebindVideo(playerView: PlayerView, placeholder: View) {
        currentVideoPlayerView = playerView
        currentVideoPlaceholder = placeholder
        placeholder.isVisible = false
        playerView.isVisible = true
        playerView.player = null
        playerView.player = exoPlayer
    }

    fun stopAndReset() {
        progressJob?.cancel()
        currentPlayButton?.isChecked = false
        currentSeekBar?.progress = 0
        currentVideoPlaceholder?.isVisible = true
        currentVideoPlayerView?.isVisible = false
        currentVideoPlayerView?.player = null
        currentPlayingPostId = null
        currentVideoPlaceholder = null
        currentVideoPlayerView = null
        currentSeekBar = null
        currentPlayButton = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.clearVideoSurface()
    }

    override fun onStop(owner: LifecycleOwner) {
        exoPlayer.pause()
        progressJob?.cancel()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopAndReset()
    }
}