package ru.netology.nework.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PagingLoadStateAdapter
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment(R.layout.fragment_posts) {
    @Inject
    lateinit var auth: AppAuth
    private val binding by viewBinding(FragmentPostsBinding::bind)
    private val viewModel: PostViewModel by activityViewModels()
    private lateinit var adapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
            }

            override fun onShare(post: Post) {
                TODO("Not yet implemented")
            }

            override fun onAudio(post: Post, isPlaying: Boolean) {
                TODO("Not yet implemented")
            }

            override fun onVideo(post: Post) {
                TODO("Not yet implemented")
            }

            override fun onPost(post: Post) {
                TODO("Not yet implemented")
            }
        })

        binding.list.adapter = adapter.withLoadStateFooter(
            footer = PagingLoadStateAdapter { adapter.retry() }
        )
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(adapter::refresh)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{ viewModel.data.collectLatest(adapter::submitData) }

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
                                viewModel.handlePagingError(refreshError.error)
                            } else if (states.refresh is LoadState.NotLoading && !isListEmpty) {
                                viewModel.resetState()
                            }

                            if (viewModel.isInitialLoad && !isListEmpty) {
                                viewModel.isInitialLoad = false
                                list.scrollToPosition(0)
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
                            val messageResId = when (state.errorMessage) {
                                "error_network" -> R.string.error_network
                                "error_db" -> R.string.error_db
                                else -> R.string.error_unknown
                            }

                            Snackbar.make(
                                binding.root,
                                getString(messageResId),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }

                        viewModel.resetState()
                    }
                }
            }
        }
    }
}