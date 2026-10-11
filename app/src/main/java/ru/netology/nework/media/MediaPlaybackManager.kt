package ru.netology.nework.media

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.Runnable
import ru.netology.nework.error.AppError

class MediaPlaybackManager(
    context: Context,
    private val onErrorListener: (String) -> Unit,
) : DefaultLifecycleObserver {
    private var exoPlayer: ExoPlayer? = null
    private var currentSeekBar: SeekBar? = null
    private var currentPlayButton: MaterialButton? = null
    private var currentUrl: String? = null
    private var currentVideoPlayerView: PlayerView? = null
    private var currentVideoPlaceholder: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    currentSeekBar?.progress = player.currentPosition.toInt()
                    handler.postDelayed(this, 200)
                }
            }
        }
    }

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        currentSeekBar?.max = duration.toInt()
                        if (isPlaying) handler.post(updateProgressRunnable)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        currentPlayButton?.isChecked = true
                        handler.post(updateProgressRunnable)
                    } else {
                        currentPlayButton?.isChecked = false
                        handler.removeCallbacks(updateProgressRunnable)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val appError = AppError.from(error.cause ?: error)
                    onErrorListener(appError.code)
                    resetPlaybackState()
                }
            })
        }
    }

    fun playAudio(url: String, seekBar: SeekBar, playButton: MaterialButton) {
        val player = exoPlayer ?: return

        if (currentUrl == url) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }

        resetPlaybackState()
        player.clearVideoSurface()

        currentUrl = url
        currentSeekBar = seekBar
        currentPlayButton = playButton

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startPlayback(url)
    }

    fun playVideo(url: String, playerView: PlayerView, placeholder: View) {
        val player = exoPlayer ?: return

        if (currentUrl == url && playerView.player == player) return

        resetPlaybackState()

        currentUrl = url
        currentVideoPlayerView = playerView
        currentVideoPlaceholder = placeholder
        placeholder.isVisible = false
        playerView.isVisible = true
        playerView.player = player

        startPlayback(url)
    }

    private fun startPlayback(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    private fun resetPlaybackState() {
        currentPlayButton?.isChecked = false
        handler.removeCallbacks(updateProgressRunnable)
        currentSeekBar?.progress = 0
        currentVideoPlayerView?.isVisible = false
        currentVideoPlaceholder?.isVisible = true
        currentVideoPlaceholder = null
        currentSeekBar = null
        currentPlayButton = null
        currentVideoPlayerView?.player = null
        currentVideoPlayerView = null
    }

    override fun onStop(owner: LifecycleOwner) {
        exoPlayer?.pause()
        handler.removeCallbacks(updateProgressRunnable)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handler.removeCallbacks(updateProgressRunnable)
        exoPlayer?.release()
        exoPlayer = null
        resetPlaybackState()
    }
}