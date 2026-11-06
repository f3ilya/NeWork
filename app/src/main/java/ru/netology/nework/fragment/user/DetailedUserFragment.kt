package ru.netology.nework.fragment.user

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.activity.AppActivity
import ru.netology.nework.adapter.DetailedUserAdapter
import ru.netology.nework.databinding.FragmentDetailedUserBinding
import ru.netology.nework.extensions.load
import ru.netology.nework.viewmodel.user.DetailedUserViewModel

@AndroidEntryPoint
class DetailedUserFragment : Fragment(R.layout.fragment_detailed_user) {
    private val binding by viewBinding(FragmentDetailedUserBinding::bind)
    private val viewModel: DetailedUserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupListeners()
        observeViewModel()
    }

    private fun setupAdapter() {
        with(binding) {
            viewPager.adapter = DetailedUserAdapter(
                this@DetailedUserFragment,
                viewModel.userId,
                viewModel.isMyProfile
            )
            TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
                tab.text = if (pos == 0) getString(R.string.wall) else getString(R.string.jobs)
            }.attach()
        }
    }

    private fun setupListeners() {
        with(binding) {
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 1 && viewModel.isMyProfile) fab.show() else fab.hide()
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    if (!viewModel.isMyProfile) return

                    when (position) {
                        0 -> {
                            binding.fab.apply {
                                scaleX = positionOffset
                                scaleY = positionOffset
                                alpha = positionOffset
                                isClickable = positionOffset > 0.5f
                            }
                        }

                        1 -> {
                            binding.fab.apply {
                                scaleX = 1f
                                scaleY = 1f
                                alpha = 1f
                                isClickable = true
                            }
                        }
                    }
                }
            })

            fab.setOnClickListener {
                findNavController().navigate(R.id.action_detailedUserFragment_to_newJobFragment)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collectLatest { user ->
                        user?.let {
                            binding.avatar.load(url = user.avatar)
                            (activity as? AppActivity)?.setToolbarUserTitle(
                                title = user.name + " / " + user.login,
                                isMyProfile = viewModel.isMyProfile
                            )
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