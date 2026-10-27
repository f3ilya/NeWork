package ru.netology.nework.fragment.user

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.model.UserModel
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.user.UsersViewModel

@AndroidEntryPoint
class UsersFragment : Fragment(R.layout.fragment_users), OnSaveActionListener {
    private val binding by viewBinding(FragmentUsersBinding::bind)
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        observeViewModel()
    }

    private fun setupAdapter() {
        val isSelectedMode = viewModel.mode == FragmentMode.SELECTED
        adapter = UsersAdapter(isSelectedMode) { id ->
            if (isSelectedMode) {
                viewModel.toggleUserSelection(id)
            } else {
                TODO()
            }
        }
        binding.list.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(viewModel.users, viewModel.selectedUserIds) { users, selectedId ->
                        users.map { userResponse ->
                            UserModel(
                                user = userResponse,
                                isSelected = selectedId.contains(userResponse.id)
                            )
                        }
                    }.collectLatest { models -> adapter.submitList(models) }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.list.isVisible = !state.loading

                        if (state.error && state.errorMessage != null) {
                            error(state.errorMessage)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    override fun onSaveAction() {
        val selectedIds = viewModel.selectedUserIds.value.toLongArray()
        val navController = findNavController()
        val prevFragment = navController.previousBackStackEntry ?: return
        prevFragment.savedStateHandle["users"] = selectedIds
        navController.navigateUp()
    }

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "404" -> R.string.user_not_found
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}