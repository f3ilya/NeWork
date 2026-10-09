package ru.netology.nework.fragment


import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentRegistrationBinding
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.showConfirmationDialog
import ru.netology.nework.viewmodel.RegViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {
    private val binding by viewBinding(FragmentRegistrationBinding::bind)
    private val viewModel: RegViewModel by viewModels()

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
                    val name = etName.text.toString()
                    val login = etLogin.text.toString()
                    val pass = etPassword.text.toString()

                    viewModel.registration(login, pass, name)
                }
            }

            val pickPhotoLauncher = setupPickPhotoLauncher()

            ivAvatar.setOnClickListener {
                requireContext().showConfirmationDialog(
                    title = getString(R.string.choose_a_profile_picture),
                    onConfirm = {
                        ImagePicker.with(requireActivity())
                            .crop()
                            .compress(2048)
                            .provider(ImageProvider.GALLERY)
                            .galleryMimeTypes(
                                arrayOf(
                                    "image/png",
                                    "image/jpeg",
                                )
                            ).createIntent(pickPhotoLauncher::launch)
                    },
                    positive = getString(R.string.description_select_photo),
                    negative = getString(R.string.description_take_photo),
                    onCancel = {
                        ImagePicker.with(requireActivity())
                            .crop()
                            .compress(2048)
                            .provider(ImageProvider.CAMERA)
                            .createIntent(pickPhotoLauncher::launch)
                    },
                    neutral = getString(R.string.remove),
                    onNeutral = { viewModel.clearPhoto() },
                    isNeed = viewModel.photo.value.file != null
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
                            binding.ivAvatar.setImageURI(photoModel.uri)
                        }
                    }
                }

                launch {
                    viewModel.regSuccess.collectLatest {
                        hideKeyboard()
                        val startDestinationId = findNavController().graph.startDestinationId
                        findNavController().popBackStack(startDestinationId, false)
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        if (state.error && state.errorMessage != null) {
                            val messageResId = when (state.errorMessage) {
                                "error_network" -> R.string.error_network
                                "403" -> R.string.the_user_is_already_registered
                                "415" -> R.string.incorrect_photo_format
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
    }

    private fun setupPickPhotoLauncher() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(it.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                Activity.RESULT_OK -> {
                    val uri: Uri? = it.data?.data
                    viewModel.changePhoto(uri, uri?.toFile())
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
}