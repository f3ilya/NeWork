package ru.netology.nework.fragment.user

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
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

        setupAdapter()
        observeViewModel()
    }

    private fun setupAdapter() {
        adapter = JobsAdapter(viewModel.isMyProfile) { jobId ->
            viewModel.removeJob(jobId)
        }
        binding.list.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.jobs.collectLatest { adapter.submitList(it) } }

                launch {
                    viewModel.state.collectLatest { state ->
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