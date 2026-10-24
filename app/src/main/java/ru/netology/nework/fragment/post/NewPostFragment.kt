package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.registerImagePickerLauncher
import ru.netology.nework.extensions.registerMediaFilePickerLauncher
import ru.netology.nework.extensions.showMediaSelectionDialog
import ru.netology.nework.extensions.showPhotoSelectionDialog
import ru.netology.nework.extensions.uriToFile
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.model.MediaModel
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.post.NewPostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post), OnSaveActionListener {
    private val binding by viewBinding(FragmentNewPostBinding::bind)
    private val viewModel: NewPostViewModel by viewModels()
    private var currentMediaAttachmentType: AttachmentType? = null

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager

    private val pickPhotoLauncher = registerImagePickerLauncher(
        onSuccess = { uri, file ->
            playbackManager.stopAndReset()
            viewModel.clearMedia()
            viewModel.changeMedia(uri, file, AttachmentType.IMAGE)
        },
        onError = { error ->
            viewModel.handleError(error)
        }
    )

    private val pickMediaLauncher = registerMediaFilePickerLauncher(
        onSuccess = { uri ->
            runCatching {
                val file = requireContext().uriToFile(uri)
                playbackManager.stopAndReset()
                viewModel.clearMedia()
                viewModel.changeMedia(uri, file, currentMediaAttachmentType)
            }.onFailure { exception ->
                viewModel.handleError(exception)
            }
        },
        onError = { error ->
            viewModel.handleError(error)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupYandexMap()
        setupExoPlayer()
        setupListeners()
        observeViewModel()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
        playbackManager.setOnErrorListener { error ->
            viewModel.handleError(error)
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnPhoto.setOnClickListener {
                showPhotoSelectionDialog(launcher = pickPhotoLauncher)
            }

            btnMedia.setOnClickListener {
                showMediaSelectionDialog(
                    launcher = pickMediaLauncher,
                    onAudio = { currentMediaAttachmentType = AttachmentType.AUDIO },
                    onVideo = { currentMediaAttachmentType = AttachmentType.VIDEO })
            }

            btnLocation.setOnClickListener {
                findNavController().navigate(R.id.action_newPostFragment_to_mapFragment)
            }

            removeAttachment.setOnClickListener {
                playbackManager.stopAndReset()
                viewModel.clearMedia()
            }

            removeMap.setOnClickListener {
                viewModel.clearMap()
            }

            edit.doAfterTextChanged { text ->
                viewModel.changeContent(text.toString())
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.postCreated.collectLatest { findNavController().navigateUp() } }

                launch { viewModel.media.collectLatest { bindMedia(it) } }

                launch {
                    viewModel.editedPost.collectLatest { post ->
                        bindMap(post.coordinates)
                        post.attachment?.let {
                            viewModel.changeMedia(it.url.toUri(), null, it.type)
                        }
                        if (post.content != binding.edit.text.toString()) {
                            post.content.let(binding.edit::setText)
                        }
                    }
                }

                launch {
                    findNavController().currentBackStackEntryFlow.collectLatest { backStackEntry ->
                        val handle = backStackEntry.savedStateHandle
                        val lat = handle.getStateFlow<Double?>("lat", null)
                        val long = handle.getStateFlow<Double?>("long", null)

                        combine(lat, long) { lat, long ->
                            if (lat != null && long != null) Pair(lat, long) else null
                        }.collectLatest { coordinates ->
                            coordinates?.let { (lat, long) ->
                                viewModel.changeMap(lat, long)
                                handle["lat"] = null
                                handle["long"] = null
                            }
                        }
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        if (state.error && state.errorMessage != null) {
                            error(state.errorMessage)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun bindMap(coordinates: Coordinates?) {
        with(binding) {
            mapContainer.isVisible = false
            if (coordinates != null) {
                val lat = coordinates.lat
                val long = coordinates.long
                mapContainer.isVisible = true
                yandexMapManager.showPoint(lat, long)
                yandexMapManager.disableAllGestures()
            }
        }
    }

    private fun bindMedia(mediaModel: MediaModel) {
        with(binding) {
            attachment.isVisible = false
            attachmentContainer.imageAttachment.isVisible = false
            attachmentContainer.audioAttachment.isVisible = false
            attachmentContainer.videoContainer.isVisible = false
            when (mediaModel.type) {
                AttachmentType.IMAGE -> {
                    attachment.isVisible = true
                    attachmentContainer.imageAttachment.isVisible = true
                    attachmentContainer.imageAttachment.load(mediaModel.uri.toString())
                }

                AttachmentType.VIDEO -> {
                    attachment.isVisible = true
                    attachmentContainer.videoContainer.isVisible = true
                    attachmentContainer.videoContainer.setOnClickListener {
                        playbackManager.playVideo(
                            postId = 0L,
                            url = mediaModel.uri.toString(),
                            playerView = attachmentContainer.videoAttachment,
                            placeholder = attachmentContainer.videoPlaceholder
                        )
                    }
                }

                AttachmentType.AUDIO -> {
                    attachment.isVisible = true
                    attachmentContainer.audioAttachment.isVisible = true
                    attachmentContainer.audioPlayPause.setOnClickListener {
                        playbackManager.playAudio(
                            postId = 1L,
                            url = mediaModel.uri.toString(),
                            seekBar = attachmentContainer.audioSeekBar,
                            playButton = attachmentContainer.audioPlayPause
                        )
                    }
                }

                null -> {
                    attachment.isVisible = false
                    attachmentContainer.imageAttachment.isVisible = false
                    attachmentContainer.audioAttachment.isVisible = false
                    attachmentContainer.videoContainer.isVisible = false
                }
            }
        }
    }

    override fun onSaveAction() {
        hideKeyboard()
        viewModel.save()
    }

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_playback" -> R.string.error_playback
            "error_file_too_large" -> R.string.error_file_too_large
            "error_file_access" -> R.string.error_file_access
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "403" -> R.string.authorization_is_required_for_this_action
            "415" -> R.string.incorrect_attachment_format
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}