package ru.netology.nework.fragment

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
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
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentAuthenticationBinding
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.viewmodel.AuthViewModel

@AndroidEntryPoint
class AuthenticationFragment : Fragment(R.layout.fragment_authentication) {
    private val binding by viewBinding(FragmentAuthenticationBinding::bind)
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        with(binding)
        {
            etLogin.doAfterTextChanged { binding.tilLogin.error = null }
            etPassword.doAfterTextChanged { binding.tilPassword.error = null }

            btnLogin.setOnClickListener {
                if (validateFields()) {
                    val login = etLogin.text.toString()
                    val pass = etPassword.text.toString()
                    viewModel.authentication(login, pass)
                }
            }

            btnRegister.setOnClickListener {
                findNavController().navigate(
                    R.id.action_authenticationFragment_to_registrationFragment
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authSuccess.collectLatest {
                    hideKeyboard()
                    val startDestinationId = findNavController().graph.startDestinationId
                    findNavController().popBackStack(startDestinationId, false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    if (state.error && state.errorMessage != null) {
                        val messageResId = when (state.errorMessage) {
                            "error_network" -> R.string.error_network
                            "400" -> R.string.incorrect_login_or_password
                            "404" -> R.string.the_user_is_not_registered
                            else -> R.string.error_unknown
                        }
                        Snackbar.make(
                            binding.root,
                            getString(messageResId),
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        with(binding)
        {
            if (etLogin.text.isNullOrBlank()) {
                tilLogin.error = getString(R.string.login_cannot_be_empty)
                isValid = false
            }

            if (etPassword.text.isNullOrBlank()) {
                tilPassword.error = getString(R.string.password_cannot_be_empty)
                isValid = false
            }
        }

        return isValid
    }
}