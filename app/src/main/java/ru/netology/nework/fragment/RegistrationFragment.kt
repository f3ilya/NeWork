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
import ru.netology.nework.databinding.FragmentRegistrationBinding
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.registerImagePickerLauncher
import ru.netology.nework.extensions.showPhotoSelectionDialog
import ru.netology.nework.viewmodel.RegViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {
    private val binding by viewBinding(FragmentRegistrationBinding::bind)
    private val viewModel: RegViewModel by viewModels()

    private val pickPhotoLauncher = registerImagePickerLauncher(
        onSuccess = { uri, file ->
            viewModel.changePhoto(uri, file)
        },
        onError = { error ->
            viewModel.handleError(error)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        with(binding) {
            etName.doAfterTextChanged { tilName.error = null }
            etLogin.doAfterTextChanged { tilLogin.error = null }
            etPassword.doAfterTextChanged { tilPassword.error = null }
            etConfirmPassword.doAfterTextChanged { tilConfirmPassword.error = null }

            btnBackToLogin.setOnClickListener {
                findNavController().navigate(
                    R.id.action_registrationFragment_to_authenticationFragment
                )
            }

            btnCreateAccount.setOnClickListener {
                if (validateFields()) {
                    hideKeyboard()
                    val name = etName.text.toString()
                    val login = etLogin.text.toString()
                    val pass = etPassword.text.toString()

                    viewModel.registration(login, pass, name)
                }
            }

            ivAvatar.setOnClickListener {
                showPhotoSelectionDialog(
                    launcher = pickPhotoLauncher,
                    isPhotoAttached = false,
                    onRemovePhoto = { viewModel.clearPhoto() },
                    title = R.string.choose_a_profile_picture
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.photo.collectLatest { photoModel ->
                        if (photoModel.uri == null) {
                            binding.ivAvatar.setImageResource(R.drawable.ic_camera_48)
                        } else {
                            binding.ivAvatar.load(photoModel.uri.toString(), true)
                        }
                    }
                }

                launch {
                    viewModel.regSuccess.collectLatest {
                        val startDestinationId = findNavController().graph.startDestinationId
                        findNavController().popBackStack(startDestinationId, false)
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

    private fun validateFields(): Boolean {
        var isValid = true

        with(binding)
        {
            if (etName.text.isNullOrBlank()) {
                tilName.error = getString(R.string.name_cannot_be_empty)
                isValid = false
            }

            if (etLogin.text.isNullOrBlank()) {
                tilLogin.error = getString(R.string.login_cannot_be_empty)
                isValid = false
            }

            if (etPassword.text.isNullOrBlank()) {
                tilPassword.error = getString(R.string.password_cannot_be_empty)
                isValid = false
            }

            if (etConfirmPassword.text.isNullOrBlank()) {
                tilConfirmPassword.error = getString(R.string.password_cannot_be_empty)
                isValid = false
            }

            if (etPassword.text.toString() != etConfirmPassword.text.toString()) {
                tilConfirmPassword.error = getString(R.string.passwords_don_t_match)
                isValid = false
            }
        }

        return isValid
    }

    private fun error(error: String) {
        val messageResId = when (error) {
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