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
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.OnPostInteractionListener
import ru.netology.nework.adapter.PagingLoadStateAdapter
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.authorizationIsRequired
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.util.autoCleared
import ru.netology.nework.viewmodel.post.PostsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment(R.layout.fragment_posts) {
    @Inject
    lateinit var playbackManager: MediaPlaybackManager
    private val binding by viewBinding(FragmentPostsBinding::bind)
    private val viewModel: PostsViewModel by viewModels()
    private var adapter by autoCleared<PostsAdapter>()

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
        adapter = PostsAdapter(playbackManager, object : OnPostInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
            }

            override fun onShare(post: Post) {
                requireContext().shareIntent(post.content)
            }

            override fun onAudio(post: Post) {
                val url = post.attachment?.url ?: return
                playbackManager.togglePlay(id = post.id, url = url, isVideo = false)
            }

            override fun onVideo(post: Post) {
                val url = post.attachment?.url ?: return
                playbackManager.togglePlay(id = post.id, url = url, isVideo = true)
            }

            override fun onSeek(post: Post, progress: Long) {
                playbackManager.seekTo(id = post.id, position = progress)
            }

            override fun onPost(post: Post) {
                val action = PostsFragmentDirections.actionPostsFragmentToDetailedPostFragment(
                    postId = post.id,
                    postContent = post.content,
                )
                findNavController().navigate(action)
            }

            override fun onRemove(post: Post) {
                viewModel.removePost(post.id)
            }

            override fun onEdit(post: Post) {
                val action = PostsFragmentDirections.actionPostsFragmentToNewPostFragment(post.id)
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
                        findNavController().navigate(R.id.action_postsFragment_to_authenticationFragment)
                    }
                } else {
                    findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
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