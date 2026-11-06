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
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.PagingLoadStateAdapter
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.authorizationIsRequired
import ru.netology.nework.extensions.shareIntent
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.viewmodel.post.PostsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment(R.layout.fragment_posts) {
    @Inject
    lateinit var playbackManager: MediaPlaybackManager
    private val binding by viewBinding(FragmentPostsBinding::bind)
    private val viewModel: PostsViewModel by viewModels()
    private lateinit var adapter: PostsAdapter

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
        adapter = PostsAdapter(playbackManager, object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
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
            swipeRefresh.setOnRefreshListener {
                adapter.refresh()
            }

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

    private fun observeViewModel() {
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

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_playback" -> R.string.error_playback
            "error_file_too_large" -> R.string.error_file_too_large
            "error_file_access" -> R.string.error_file_access
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "403" -> R.string.authorization_is_required_for_this_action
            "404" -> R.string.post_not_found
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}