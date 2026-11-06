package ru.netology.nework.fragment.user

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.PagingLoadStateAdapter
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.viewmodel.user.DetailedUserViewModel
import ru.netology.nework.viewmodel.user.WallViewModel
import javax.inject.Inject

@AndroidEntryPoint
class WallFragment : Fragment(R.layout.fragment_posts) {
    companion object {
        fun newInstance(userId: Long, isMyProfile: Boolean) = WallFragment().apply {
            arguments = WallFragmentArgs(userId, isMyProfile).toBundle()
        }
    }

    private val binding by viewBinding(FragmentPostsBinding::bind)
    private val viewModel: WallViewModel by viewModels()
    private val parentViewModel: DetailedUserViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: PostsAdapter

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExoPlayer()
        setupAdapter()
        setupListeners()
        observeViewModel()
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
        playbackManager.setOnErrorListener { error ->
            viewModel.handleError(error)
        }
    }

    private fun setupAdapter() {
        binding.fab.isVisible = false
        val paddingInPx = resources.getDimensionPixelSize(R.dimen.common_spacing)
        binding.root.updatePadding(top = paddingInPx)
        adapter = PostsAdapter(playbackManager, object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likePost(post.id, post.likedByMe)
            }

            override fun onShare(post: Post) {
                requireContext().shareIntent(post.content)
            }

            override fun onAudio(post: Post, binding: CardPostBinding) {
                playbackManager.playAudio(
                    postId = post.id,
                    url = post.attachment?.url ?: "",
                    seekBar = binding.attachmentContainer.audioSeekBar,
                    playButton = binding.attachmentContainer.audioPlayPause,
                )
            }

            override fun onVideo(post: Post, binding: CardPostBinding) {
                playbackManager.playVideo(
                    postId = post.id,
                    url = post.attachment?.url ?: "",
                    playerView = binding.attachmentContainer.videoAttachment,
                    placeholder = binding.attachmentContainer.videoPlaceholder,
                )
            }

            override fun onPost(post: Post) {}

            override fun onRemove(post: Post) {}

            override fun onEdit(post: Post) {}
        })

        binding.list.adapter = adapter.withLoadStateFooter(
            footer = PagingLoadStateAdapter { adapter.retry() }
        )
    }

    private fun setupListeners() {
        with(binding) {
            swipeRefresh.setOnRefreshListener(adapter::refresh)

            retryButton.setOnClickListener { adapter.retry() }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.data.collectLatest(adapter::submitData) }

                launch {
                    adapter.loadStateFlow.collectLatest { states ->
                        with(binding) {
                            val isRefreshing = states.refresh is LoadState.Loading
                            swipeRefresh.isRefreshing = isRefreshing

                            val refreshError = states.mediator?.refresh as? LoadState.Error
                                ?: states.source.refresh as? LoadState.Error
                            val isListEmpty = adapter.itemCount == 0
                            val showErrorPlaceholder = refreshError != null && isListEmpty
                            val showEmptyPlaceholder =
                                isListEmpty && !isRefreshing && !showErrorPlaceholder

                            errorPlaceholder.isVisible = showErrorPlaceholder
                            emptyPlaceholder.isVisible = showEmptyPlaceholder
                            list.isVisible = !showErrorPlaceholder && !showEmptyPlaceholder
                            progress.isVisible =
                                isRefreshing && isListEmpty && !showErrorPlaceholder

                            if (showErrorPlaceholder) {
                                viewModel.handleError(refreshError.error)
                            }

                            if (viewModel.isInitialLoad && !isListEmpty) {
                                viewModel.isInitialLoad = false
                                list.scrollToPosition(0)
                            }
                        }
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.progress.isVisible = state.loading && adapter.itemCount == 0

                        if (state.error && state.errorMessage != null) {
                            parentViewModel.changeError(state.errorMessage)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }
}