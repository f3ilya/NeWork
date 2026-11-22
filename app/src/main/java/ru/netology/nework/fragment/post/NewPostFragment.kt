package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.error.AppError
import ru.netology.nework.extensions.bindUserSection
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.registerImagePickerLauncher
import ru.netology.nework.extensions.registerMediaFilePickerLauncher
import ru.netology.nework.extensions.renderPlayback
import ru.netology.nework.extensions.setupMediaListeners
import ru.netology.nework.extensions.showMediaSelectionDialog
import ru.netology.nework.extensions.showPhotoSelectionDialog
import ru.netology.nework.extensions.uriToFile
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.model.MediaModel
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.post.NewPostViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post), OnSaveActionListener {
    private val binding by viewBinding(FragmentNewPostBinding::bind)
    private val viewModel: NewPostViewModel by viewModels()
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
    private var currentMediaAttachmentType: AttachmentType? = null
    private var isUserSeeking = false

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.args.postId != 0L) {
            postponeEnterTransition(150, TimeUnit.MILLISECONDS)
        }
        setupYandexMap()
        setupExoPlayer()
        setupListeners()
        setupObservers()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
    }

    private fun setupListeners() {
        with(binding) {
            btnPhoto.setOnClickListener {
                hideKeyboard()
                showPhotoSelectionDialog(launcher = pickPhotoLauncher)
            }

            btnMedia.setOnClickListener {
                hideKeyboard()
                showMediaSelectionDialog(
                    launcher = pickMediaLauncher,
                    onAudio = { currentMediaAttachmentType = AttachmentType.AUDIO },
                    onVideo = { currentMediaAttachmentType = AttachmentType.VIDEO })
            }

            btnLocation.setOnClickListener {
                hideKeyboard()
                findNavController().navigate(R.id.action_newPostFragment_to_mapFragment)
            }

            btnUsers.setOnClickListener {
                hideKeyboard()
                val action =
                    NewPostFragmentDirections.actionNewPostFragmentToDetailedUsersListFragment(
                        fragmentMode = FragmentMode.SELECTED
                    )
                findNavController().navigate(action)
            }

            removeAttachment.setOnClickListener {
                playbackManager.stopAndReset()
                viewModel.clearMedia()
            }

            removeMap.setOnClickListener {
                viewModel.clearMap()
            }

            removeMentioned.setOnClickListener {
                viewModel.clearMentioned()
            }

            edit.doAfterTextChanged { text ->
                viewModel.changeContent(text.toString())
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.postCreated.collectLatest { findNavController().navigateUp() } }

                launch {
                    playbackManager.playbackState.collectLatest { state ->
                        renderPlaybackState(state)
                    }
                }

                launch { viewModel.media.collectLatest { bindMedia(it) } }

                launch { viewModel.mentioned.collectLatest { bindMentioned(it) } }

                launch {
                    viewModel.editedPost.collectLatest { post ->
                        bindMap(post.coordinates)
                        if (post.content != binding.edit.text.toString()) {
                            post.content.let(binding.edit::setText)
                        }
                    }
                }

                launch {
                    findNavController().currentBackStackEntryFlow.collectLatest { backStackEntry ->
                        val handle = backStackEntry.savedStateHandle
                        val lat = handle.remove<Double>("lat")
                        val long = handle.remove<Double>("long")
                        if (lat != null && long != null) {
                            viewModel.changeMap(lat, long)
                        }

                        handle.remove<LongArray>("users")?.let {
                            viewModel.changeMentioned(it.toList())
                        }
                    }
                }

                launch {
                    playbackManager.errorFlow.collectLatest { error ->
                        viewModel.handleError(error)
                    }
                }

                val postId = viewModel.args.postId
                if (postId != 0L) {
                    launch {
                        combine(
                            viewModel.editedPost,
                            viewModel.media,
                            viewModel.mentioned
                        ) { post, _, _ ->
                            post.id == postId
                        }.first { isLoaded -> isLoaded }
                        binding.root.doOnPreDraw {
                            startPostponedEnterTransition()
                        }
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.loadingOverlay.isVisible = state.loading
                        state.errorType?.let { errorType ->
                            Snackbar.make(
                                binding.root,
                                errorType.messageRes,
                                Snackbar.LENGTH_LONG
                            ).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun bindMentioned(users: List<UserResponse>) {
        with(binding) {
            mentioned.isVisible = users.isNotEmpty()
            mentionedContainer.bindUserSection(
                mode = FragmentMode.MENTIONED,
                users = users,
                onAvatarClick = { userId ->
                    val action = NewPostFragmentDirections
                        .actionNewPostFragmentToDetailedUserFragment(userId)
                    findNavController().navigate(action)
                },
                onMoreClick = { mode, userIds ->
                    val action = NewPostFragmentDirections
                        .actionNewPostFragmentToDetailedUsersListFragment(mode, userIds)
                    findNavController().navigate(action)
                }
            )
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
            attachment.isVisible = mediaModel.type != null
            attachmentContainer.imageAttachment.isVisible = mediaModel.type == AttachmentType.IMAGE
            attachmentContainer.audioAttachment.isVisible = mediaModel.type == AttachmentType.AUDIO
            attachmentContainer.videoContainer.isVisible = mediaModel.type == AttachmentType.VIDEO
            attachmentContainer.videoAttachment.player = null
            if (mediaModel.type == AttachmentType.IMAGE) {
                attachmentContainer.imageAttachment.load(mediaModel.uri.toString())
            }
            attachmentContainer.setupMediaListeners(
                onVideoClick = {
                    playbackManager.togglePlay(
                        id = 0L,
                        url = mediaModel.uri.toString(),
                        isVideo = true
                    )
                },
                onAudioClick = {
                    playbackManager.togglePlay(
                        id = 0L,
                        url = mediaModel.uri.toString(),
                        isVideo = false
                    )
                },
                onSeekStart = { isUserSeeking = true },
                onSeekStop = { progress ->
                    isUserSeeking = false
                    playbackManager.seekTo(id = 0L, position = progress)
                }
            )
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val attachmentType = viewModel.media.value.type ?: return
        binding.attachmentContainer.renderPlayback(
            type = attachmentType,
            isActive = state.currentId != null,
            state = state,
            exoPlayer = playbackManager.exoPlayer,
            isUserSeeking = isUserSeeking
        )
    }

    private fun isValid(post: Post): Boolean {
        if (post.content.isBlank() && viewModel.media.value.type == null) {
            viewModel.handleError(AppError.EmptyContentOrAttachmentError())
            return false
        }
        return true
    }

    override fun onSaveAction() {
        hideKeyboard()
        if (isValid(viewModel.editedPost.value)) {
            viewModel.save()
        }
    }
}