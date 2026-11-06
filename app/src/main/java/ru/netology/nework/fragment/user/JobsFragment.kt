package ru.netology.nework.fragment.user

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobsAdapter
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.viewmodel.user.DetailedUserViewModel
import ru.netology.nework.viewmodel.user.JobsViewModel

@AndroidEntryPoint
class JobsFragment : Fragment(R.layout.fragment_jobs) {
    companion object {
        fun newInstance(userId: Long, isMyProfile: Boolean) = JobsFragment().apply {
            arguments = JobsFragmentArgs(userId, isMyProfile).toBundle()
        }
    }

    private val binding by viewBinding(FragmentJobsBinding::bind)
    private val viewModel: JobsViewModel by viewModels()
    private val parentViewModel: DetailedUserViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: JobsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind()
        setupAdapter()
        setupListeners()
        observeViewModel()
    }

    private fun bind() {
        with(binding) {
            listOf(
                placeholderContainer.emptyPlaceholder,
                placeholderContainer.errorPlaceholder,
                placeholderContainer.progress
            ).forEach { placeholderView ->
                placeholderView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    verticalBias = 0.3f
                }
            }
            val textEmptyPlaceholder = if (viewModel.isMyProfile) {
                R.string.empty_profile_jobs_owner
            } else {
                R.string.empty_profile_jobs_guest
            }
            placeholderContainer.emptyPlaceholder.setText(textEmptyPlaceholder)
        }
    }

    private fun setupAdapter() {
        adapter = JobsAdapter(viewModel.isMyProfile) { jobId ->
            viewModel.removeJob(jobId)
        }
        binding.list.adapter = adapter
    }

    private fun setupListeners() {
        with(binding) {
            swipeRefresh.setOnRefreshListener {
                viewModel.refresh(true)
            }

            placeholderContainer.retryButton.setOnClickListener {
                viewModel.refresh()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.jobs, viewModel.state) { jobs, state ->
                    jobs to state
                }.collectLatest { (jobs, state) ->
                    adapter.submitList(jobs) {
                        with(binding) {
                            swipeRefresh.isRefreshing = state.refreshing
                            val isListEmpty = jobs.isEmpty()
                            val showErrorPlaceholder = state.error && isListEmpty
                            val showEmptyPlaceholder =
                                isListEmpty && !state.refreshing && !state.loading && !showErrorPlaceholder
                            val showProgress = state.loading && isListEmpty && !showErrorPlaceholder
                            val showPlaceholders =
                                showProgress || showErrorPlaceholder || showEmptyPlaceholder

                            placeholderContainer.scroll.isVisible = showPlaceholders
                            list.isVisible = !showPlaceholders
                            placeholderContainer.errorPlaceholder.isVisible = showErrorPlaceholder
                            placeholderContainer.emptyPlaceholder.isVisible = showEmptyPlaceholder
                            placeholderContainer.progress.isVisible = showProgress

                            if (state.error && state.errorMessage != null) {
                                parentViewModel.changeError(state.errorMessage)
                                viewModel.clearErrorMessage()
                            }
                        }
                    }
                }

            }
        }
    }
}