package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
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
    private val viewModel: PostsViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter
    private var isManualScrollNeeded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExoPlayer()
        setupAdapter()
        setupListeners()
        observeViewModel()
        observeAdapter()
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
                TODO("Not yet implemented")
            }
        })

        binding.list.adapter = adapter.withLoadStateFooter(
            footer = PagingLoadStateAdapter { adapter.retry() }
        )
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.manualRefresh()
            adapter.refresh()
            isManualScrollNeeded = true
        }

        binding.fab.setOnClickListener {
            if (viewModel.authState.value.token.isNullOrEmpty()) {
                authorizationIsRequired {
                    findNavController().navigate(R.id.action_postsFragment_to_authenticationFragment)
                }
            } else {
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
            }
        }

        binding.retryButton.setOnClickListener {
            adapter.retry()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.data.collectLatest(adapter::submitData) }

                launch { viewModel.authState.collectLatest { adapter.refresh() } }

                launch {
                    adapter.loadStateFlow.collectLatest { states ->
                        binding.apply {
                            val isRefreshing = states.refresh is LoadState.Loading
                            swipeRefresh.isRefreshing = isRefreshing

                            val refreshError = states.mediator?.refresh as? LoadState.Error
                                ?: states.source.refresh as? LoadState.Error
                            val isListEmpty = adapter.itemCount == 0
                            val showErrorPlaceholder = refreshError != null && isListEmpty

                            errorPlaceholder.isVisible = showErrorPlaceholder
                            list.isVisible = !showErrorPlaceholder

                            if (showErrorPlaceholder) {
                                viewModel.handleError(refreshError.error)
                            } else if (states.refresh is LoadState.NotLoading && !isListEmpty) {
                                viewModel.resetState()
                            }

                            if (viewModel.isInitialLoad && !isListEmpty) {
                                viewModel.isInitialLoad = false
                                viewModel.manualRefresh()
                            }

                            progress.isVisible =
                                isRefreshing && isListEmpty && !showErrorPlaceholder
                        }
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.progress.isVisible = state.loading && adapter.itemCount == 0

                        if (state.error && state.errorMessage != null) {
                            error(state.errorMessage)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun observeAdapter() {
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && isManualScrollNeeded) {
                    isManualScrollNeeded = false
                    binding.list.post {
                        binding.list.smoothScrollToPosition(0)
                    }
                }
            }
        })
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