package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
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
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentDetailedPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.renderAvatarStack
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.model.AvatarModel
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.viewmodel.post.DetailedPostViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class DetailedPostFragment : Fragment(R.layout.fragment_detailed_post) {
    private val binding by viewBinding(FragmentDetailedPostBinding::bind)
    private val viewModel: DetailedPostViewModel by viewModels()

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager

    private var currentPostId: Long = 0L
    private var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition(100, TimeUnit.MILLISECONDS)
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
            attachmentContainer.audioSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}


                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    seekBar?.let {
                        playbackManager.seekTo(
                            postId = currentPostId,
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
                launch {
                    playbackManager.playbackState.collectLatest { state ->
                        renderPlaybackState(state)
                    }
                }

                launch {
                    viewModel.detailedPost.collectLatest { post ->
                        post?.let {
                            currentPostId = post.id
                            bindPost(post)
                            bindMap(post.coordinates)
                        }
                    }
                }

                launch {
                    playbackManager.errorFlow.collectLatest { error ->
                        viewModel.handleError(error)
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.postContentContainer.isVisible = !state.loading
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

    private fun bindMap(coordinates: Coordinates?) {
        with(binding) {
            map.isVisible = false
            if (coordinates != null) {
                val lat = coordinates.lat
                val long = coordinates.long
                map.isVisible = true
                yandexMapManager.showPoint(lat, long)
                yandexMapManager.disableAllGestures()

            }
        }
    }

    private fun bindPost(post: Post) {
        with(binding) {
            if (!post.authorAvatar.isNullOrBlank()) {
                authorAvatar.load(post.authorAvatar, true)
            } else {
                authorAvatar.setImageResource(R.drawable.ic_avatar_placeholder_48)
            }
            authorName.text = post.author
            authorJob.text = if (post.authorJob.isNullOrBlank()) {
                getString(R.string.looking_for_a_job)
            } else post.authorJob
            content.isVisible = if (post.content.isNotBlank() || post.content.isNotEmpty()) {
                content.text = post.content
                true
            } else false
            published.text = Instant.parse(post.published)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            like.isChecked = post.likedByMe
            like.text = post.likeOwnerIds.size.toString()

            val type = post.attachment?.type
            attachmentContainer.imageAttachment.isVisible = type == AttachmentType.IMAGE
            attachmentContainer.videoContainer.isVisible = type == AttachmentType.VIDEO
            attachmentContainer.audioAttachment.isVisible = type == AttachmentType.AUDIO
            when (post.attachment?.type) {
                AttachmentType.IMAGE -> {
                    attachmentContainer.imageAttachment.load(post.attachment.url)
                }

                AttachmentType.VIDEO -> {
                    attachmentContainer.videoContainer.setOnClickListener {
                        playbackManager.togglePlay(
                            postId = post.id,
                            url = post.attachment.url,
                            isVideo = true
                        )
                    }
                }

                AttachmentType.AUDIO -> {
                    attachmentContainer.audioPlayPause.setOnClickListener {
                        playbackManager.togglePlay(
                            postId = post.id,
                            url = post.attachment.url,
                            isVideo = false
                        )
                    }
                }

                null -> Unit
            }

            like.setOnClickListener {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
                like.isChecked = post.likedByMe
            }

            val likeModels = post.likeOwnerIds.map { userId ->
                AvatarModel(
                    userId = userId,
                    avatarUrl = post.users[userId.toString()]?.avatar
                )
            }

            avatarLikersStack.renderAvatarStack(
                models = likeModels,
                moreButton = moreLikes,
                onAvatarClickListener = { userId ->
                    val action =
                        DetailedPostFragmentDirections.actionDetailedPostFragmentToDetailedUserFragment(
                            userId
                        )
                    findNavController().navigate(action)
                },
                onMoreClickListener = {
                    val action =
                        DetailedPostFragmentDirections.actionDetailedPostFragmentToDetailedUsersListFragment(
                            fragmentMode = FragmentMode.LIKERS,
                            userIds = post.likeOwnerIds.toLongArray()
                        )
                    findNavController().navigate(action)
                }
            )

            if (post.mentionIds.isNotEmpty()) {
                mentionedContainer.root.isVisible = true
                mentionedContainer.mentionedIcon.text = post.mentionIds.size.toString()
                val mentionModels = post.mentionIds.map { userId ->
                    AvatarModel(
                        userId = userId,
                        avatarUrl = post.users[userId.toString()]?.avatar
                    )
                }
                mentionedContainer.avatarMentionedStack.renderAvatarStack(
                    models = mentionModels,
                    moreButton = mentionedContainer.moreMentioned,
                    onAvatarClickListener = { userId ->
                        val action =
                            DetailedPostFragmentDirections.actionDetailedPostFragmentToDetailedUserFragment(
                                userId
                            )
                        findNavController().navigate(action)
                    },
                    onMoreClickListener = {
                        val action =
                            DetailedPostFragmentDirections.actionDetailedPostFragmentToDetailedUsersListFragment(
                                fragmentMode = FragmentMode.MENTIONED,
                                userIds = post.mentionIds.toLongArray()
                            )
                        findNavController().navigate(action)
                    }
                )
            } else mentionedContainer.root.isVisible = false

            menu.isVisible = post.ownedByMe
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                viewModel.removePost(post.id)
                                findNavController().navigateUp()
                                true
                            }

                            R.id.edit -> {
                                val action =
                                    DetailedPostFragmentDirections.actionDetailedPostFragmentToNewPostFragment(
                                        post.id
                                    )
                                findNavController().navigate(action)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
            startPostponedEnterTransition()
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val attachmentType = viewModel.detailedPost.value?.attachment?.type ?: return
        val isActive = (state.currentPostId == currentPostId)
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
}