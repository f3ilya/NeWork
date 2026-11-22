package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
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
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.bindAttachment
import ru.netology.nework.extensions.bindUserSection
import ru.netology.nework.extensions.formatToLocalDateTime
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.renderPlayback
import ru.netology.nework.extensions.setupMediaListeners
import ru.netology.nework.extensions.showOptionsMenu
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.util.DateFormatPattern
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.viewmodel.post.DetailedPostViewModel
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
    private var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition(100, TimeUnit.MILLISECONDS)
        setupYandexMap()
        setupExoPlayer()
        setupObservers()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
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
            headerContainer.avatar.load(post.authorAvatar, true)
            headerContainer.author.text = post.author
            headerContainer.publishedOrJob.text = if (post.authorJob.isNullOrBlank()) {
                getString(R.string.looking_for_a_job)
            } else post.authorJob
            content.isVisible = post.content.isNotBlank()
            content.text = post.content
            published.text = post.published.formatToLocalDateTime(DateFormatPattern.DATE_TIME)
            attachmentContainer.bindAttachment(post.attachment)
            attachmentContainer.setupMediaListeners(
                onVideoClick = {
                    playbackManager.togglePlay(
                        id = post.id,
                        url = post.attachment?.url ?: return@setupMediaListeners,
                        isVideo = true
                    )
                },
                onAudioClick = {
                    playbackManager.togglePlay(
                        id = post.id,
                        url = post.attachment?.url ?: return@setupMediaListeners,
                        isVideo = false
                    )
                },
                onSeekStart = { isUserSeeking = true },
                onSeekStop = { progress ->
                    isUserSeeking = false
                    playbackManager.seekTo(id = post.id, position = progress)
                }
            )

            likersContainer.actionButton.setOnClickListener {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
                likersContainer.actionButton.isChecked = post.likedByMe
            }

            likersContainer.bindUserSection(
                mode = FragmentMode.LIKERS,
                userIds = post.likeOwnerIds,
                usersMap = post.users,
                isChecked = post.likedByMe,
                onAvatarClick = ::openUserProfile,
                onMoreClick = ::openUsersList
            )

            mentionedContainer.bindUserSection(
                mode = FragmentMode.MENTIONED,
                userIds = post.mentionIds,
                usersMap = post.users,
                onAvatarClick = ::openUserProfile,
                onMoreClick = ::openUsersList
            )

            headerContainer.menu.isVisible = post.ownedByMe
            headerContainer.menu.setOnClickListener { view ->
                view.showOptionsMenu(
                    onEdit = {
                        val action = DetailedPostFragmentDirections
                            .actionDetailedPostFragmentToNewPostFragment(post.id)
                        findNavController().navigate(action)
                    },
                    onRemove = {
                        viewModel.removePost(post.id)
                        findNavController().navigateUp()
                    }
                )
            }
            startPostponedEnterTransition()
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val attachmentType = viewModel.detailedPost.value?.attachment?.type ?: return
        binding.attachmentContainer.renderPlayback(
            type = attachmentType,
            isActive = state.currentId != null,
            state = state,
            exoPlayer = playbackManager.exoPlayer,
            isUserSeeking = isUserSeeking
        )
    }

    private fun openUserProfile(userId: Long) {
        val action = DetailedPostFragmentDirections
            .actionDetailedPostFragmentToDetailedUserFragment(userId)
        findNavController().navigate(action)
    }

    private fun openUsersList(mode: FragmentMode, userIds: LongArray) {
        val action = DetailedPostFragmentDirections
            .actionDetailedPostFragmentToDetailedUsersListFragment(mode, userIds)
        findNavController().navigate(action)
    }
}