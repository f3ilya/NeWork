package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
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
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.registerImagePickerLauncher
import ru.netology.nework.extensions.registerMediaFilePickerLauncher
import ru.netology.nework.extensions.renderAvatarStack
import ru.netology.nework.extensions.showMediaSelectionDialog
import ru.netology.nework.extensions.showPhotoSelectionDialog
import ru.netology.nework.extensions.uriToFile
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.model.AvatarModel
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
    private var currentMediaAttachmentType: AttachmentType? = null

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager

    private var isUserSeeking = false

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

            attachmentContainer.audioSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, p1: Int, p2: Boolean) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    seekBar?.let {
                        playbackManager.seekTo(
                            postId = 0L,
                            position = it.progress.toLong()
                        )
                    }
                }
            })
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

                launch { viewModel.users.collectLatest { bindMentioned(it) } }

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
                            viewModel.users
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
            if (users.isNotEmpty()) {
                mentioned.isVisible = true
                mentionedContainer.mentionedIcon.text = users.size.toString()
                val mentionModels = users.map { user ->
                    AvatarModel(
                        userId = user.id,
                        avatarUrl = user.avatar
                    )
                }
                mentionedContainer.avatarMentionedStack.renderAvatarStack(
                    models = mentionModels,
                    moreButton = mentionedContainer.moreMentioned,
                    onAvatarClickListener = { userId ->
                        val action =
                            NewPostFragmentDirections.actionNewPostFragmentToDetailedUserFragment(
                                userId
                            )
                        findNavController().navigate(action)
                    },
                    onMoreClickListener = {
                        val action =
                            NewPostFragmentDirections.actionNewPostFragmentToDetailedUsersListFragment(
                                fragmentMode = FragmentMode.MENTIONED,
                                userIds = users.map { it.id }.toLongArray()
                            )
                        findNavController().navigate(action)
                    }
                )
            } else mentioned.isVisible = false
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
            when (mediaModel.type) {
                AttachmentType.IMAGE -> {
                    attachmentContainer.imageAttachment.load(mediaModel.uri.toString())
                }

                AttachmentType.VIDEO -> {
                    attachmentContainer.videoContainer.setOnClickListener {
                        playbackManager.togglePlay(
                            postId = 0L,
                            url = mediaModel.uri.toString(),
                            isVideo = true
                        )
                    }
                }

                AttachmentType.AUDIO -> {
                    attachmentContainer.audioPlayPause.setOnClickListener {
                        playbackManager.togglePlay(
                            postId = 0L,
                            url = mediaModel.uri.toString(),
                            isVideo = false
                        )
                    }
                }

                null -> Unit
            }
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val attachmentType = viewModel.media.value.type ?: return
        val isActive = state.currentPostId != null
        with(binding.attachmentContainer) {
            when (attachmentType) {
                AttachmentType.VIDEO -> {
                    videoPlaceholder.isVisible = !isActive
                    videoAttachment.isVisible = isActive
                    videoAttachment.player = if (isActive) playbackManager.exoPlayer else null
                }

                AttachmentType.AUDIO -> {
                    audioPlayPause.isChecked = isActive && state.isPlaying
                    audioSeekBar.max = if (isActive) state.duration.toInt() else 0
                    if (!isUserSeeking) {
                        audioSeekBar.progress = if (isActive) state.progress.toInt() else 0
                    }
                }

                else -> Unit
            }
        }
    }

    override fun onSaveAction() {
        hideKeyboard()
        viewModel.save()
    }
}