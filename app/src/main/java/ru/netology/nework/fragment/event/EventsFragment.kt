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
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.EventsAdapter
import ru.netology.nework.adapter.OnEventInteractionListener
import ru.netology.nework.adapter.PagingLoadStateAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.extensions.authorizationIsRequired
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.util.autoCleared
import ru.netology.nework.viewmodel.event.EventsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment(R.layout.fragment_events) {
    @Inject
    lateinit var playbackManager: MediaPlaybackManager
    private val binding by viewBinding(FragmentEventsBinding::bind)
    private val viewModel: EventsViewModel by viewModels()
    private var adapter by autoCleared<EventsAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExoPlayer()
        setupAdapter()
        setupListeners()
        setupObservers()
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
    }

    private fun setupAdapter() {
        adapter = EventsAdapter(playbackManager, object : OnEventInteractionListener {
            override fun onLike(event: Event) {
                viewModel.likeEvent(id = event.id, likedByMe = event.likedByMe)
            }

            override fun onParticipant(event: Event) {
                viewModel.participantEvent(id = event.id, participatedByMe = event.participatedByMe)
            }

            override fun onShare(event: Event) {
                requireContext().shareIntent(event.content)
            }

            override fun onAudio(event: Event) {
                val url = event.attachment?.url ?: return
                playbackManager.togglePlay(id = event.id, url = url, isVideo = false)
            }

            override fun onVideo(event: Event) {
                val url = event.attachment?.url ?: return
                playbackManager.togglePlay(id = event.id, url = url, isVideo = true)
            }

            override fun onSeek(event: Event, progress: Long) {
                playbackManager.seekTo(id = event.id, position = progress)
            }

            override fun onEvent(event: Event) {
                val action = EventsFragmentDirections.actionEventsFragmentToDetailedEventFragment(
                    eventId = event.id,
                    eventContent = event.content
                )
                findNavController().navigate(action)
            }

            override fun onRemove(event: Event) {
                viewModel.removeEvent(event.id)
            }

            override fun onEdit(event: Event) {
                val action = EventsFragmentDirections.actionEventsFragmentToNewEventFragment(
                    eventId = event.id
                )
                findNavController().navigate(action)
            }
        })

        binding.list.adapter = adapter.withLoadStateFooter(
            footer = PagingLoadStateAdapter { adapter.retry() }
        )
    }

    private fun setupListeners() {
        with(binding) {
            swipeRefresh.setOnRefreshListener(adapter::refresh)

            fab.setOnClickListener {
                if (viewModel.authState.value.token.isNullOrEmpty()) {
                    authorizationIsRequired {
                        findNavController().navigate(R.id.action_eventsFragment_to_authenticationFragment)
                    }
                } else {
                    findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
                }
            }

            placeholderContainer.retryButton.setOnClickListener {
                adapter.retry()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.data.collectLatest(adapter::submitData) }

                launch {
                    adapter.loadStateFlow.collectLatest { states ->
                        with(binding) {
                            swipeRefresh.isRefreshing = states.refresh is LoadState.Loading ||
                                    states.mediator?.refresh is LoadState.Loading

                            val refreshError = states.mediator?.refresh as? LoadState.Error
                                ?: states.source.refresh as? LoadState.Error
                            val isListEmpty = adapter.itemCount == 0
                            val showErrorPlaceholder = refreshError != null && isListEmpty
                            val isRefreshFinished = states.source.refresh is LoadState.NotLoading &&
                                    (states.mediator == null || states.mediator?.refresh is LoadState.NotLoading) &&
                                    states.append.endOfPaginationReached
                            val showEmptyPlaceholder =
                                isListEmpty && isRefreshFinished && !showErrorPlaceholder
                            val showProgress =
                                !isRefreshFinished && isListEmpty && !showErrorPlaceholder
                            val showPlaceholders =
                                showErrorPlaceholder || showEmptyPlaceholder || showProgress

                            list.isVisible = !showPlaceholders
                            placeholderContainer.scroll.isVisible = showPlaceholders
                            placeholderContainer.errorPlaceholder.isVisible = showErrorPlaceholder
                            placeholderContainer.emptyPlaceholder.isVisible = showEmptyPlaceholder
                            placeholderContainer.progress.isVisible = showProgress

                            if (refreshError != null) {
                                viewModel.handleError(refreshError.error)
                            }
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
}