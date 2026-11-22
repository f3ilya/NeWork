package ru.netology.nework.fragment.event

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
import ru.netology.nework.databinding.FragmentDetailedEventBinding
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
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
import ru.netology.nework.viewmodel.event.DetailedEventViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class DetailedEventFragment : Fragment(R.layout.fragment_detailed_event) {
    private val binding by viewBinding(FragmentDetailedEventBinding::bind)
    private val viewModel: DetailedEventViewModel by viewModels()

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager
    private var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition(100, TimeUnit.MILLISECONDS)
        setupExoPlayer()
        setupYandexMap()
        setupObservers()
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
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
                    viewModel.detailedEvent.collectLatest { event ->
                        event?.let {
                            bindMap(event.coordinates)
                            bindEvent(event)
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

    private fun bindEvent(event: Event) {
        with(binding) {
            headerContainer.avatar.load(event.authorAvatar, true)
            headerContainer.author.text = event.author
            headerContainer.publishedOrJob.text =
                event.authorJob ?: getString(R.string.looking_for_a_job)
            content.isVisible = event.content.isNotBlank()
            content.text = event.content
            type.text = event.type.toString()
            datetime.text = event.datetime.formatToLocalDateTime(DateFormatPattern.DATE_TIME)
            attachmentContainer.bindAttachment(event.attachment)
            attachmentContainer.setupMediaListeners(
                onVideoClick = {
                    playbackManager.togglePlay(
                        id = event.id,
                        url = event.attachment?.url ?: return@setupMediaListeners,
                        isVideo = true
                    )
                },
                onAudioClick = {
                    playbackManager.togglePlay(
                        id = event.id,
                        url = event.attachment?.url ?: return@setupMediaListeners,
                        isVideo = false
                    )
                },
                onSeekStart = { isUserSeeking = true },
                onSeekStop = { progress ->
                    isUserSeeking = false
                    playbackManager.seekTo(id = event.id, position = progress)
                }
            )
            likersContainer.actionButton.setOnClickListener {
                viewModel.likeEvent(id = event.id, likedByMe = event.likedByMe)
                likersContainer.actionButton.isChecked = event.likedByMe
            }

            participantsContainer.actionButton.setOnClickListener {
                viewModel.participantEvent(id = event.id, participatedByMe = event.participatedByMe)
                participantsContainer.actionButton.isChecked = event.participatedByMe
            }

            speakersContainer.bindUserSection(
                mode = FragmentMode.SPEAKERS,
                userIds = event.speakerIds,
                usersMap = event.users,
                onAvatarClick = ::openUserProfile,
                onMoreClick = ::openUsersList
            )

            likersContainer.bindUserSection(
                mode = FragmentMode.LIKERS,
                userIds = event.likeOwnerIds,
                usersMap = event.users,
                isChecked = event.likedByMe,
                onAvatarClick = ::openUserProfile,
                onMoreClick = ::openUsersList
            )

            participantsContainer.bindUserSection(
                mode = FragmentMode.PARTICIPANTS,
                userIds = event.participantsIds,
                usersMap = event.users,
                isChecked = event.participatedByMe,
                onAvatarClick = ::openUserProfile,
                onMoreClick = ::openUsersList
            )

            headerContainer.menu.isVisible = event.ownedByMe
            headerContainer.menu.setOnClickListener { view ->
                view.showOptionsMenu(
                    onEdit = {
                        val action = DetailedEventFragmentDirections
                            .actionDetailedEventFragmentToNewEventFragment(event.id)
                        findNavController().navigate(action)
                    },
                    onRemove = {
                        viewModel.removeEvent(event.id)
                        findNavController().navigateUp()
                    }
                )
            }
            startPostponedEnterTransition()
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val attachmentType = viewModel.detailedEvent.value?.attachment?.type ?: return
        binding.attachmentContainer.renderPlayback(
            type = attachmentType,
            isActive = state.currentId != null,
            state = state,
            exoPlayer = playbackManager.exoPlayer,
            isUserSeeking = isUserSeeking
        )
    }

    private fun openUserProfile(userId: Long) {
        val action = DetailedEventFragmentDirections
            .actionDetailedEventFragmentToDetailedUserFragment(userId)
        findNavController().navigate(action)
    }

    private fun openUsersList(mode: FragmentMode, userIds: LongArray) {
        val action = DetailedEventFragmentDirections
            .actionDetailedEventFragmentToDetailedUsersListFragment(mode, userIds)
        findNavController().navigate(action)
    }
}