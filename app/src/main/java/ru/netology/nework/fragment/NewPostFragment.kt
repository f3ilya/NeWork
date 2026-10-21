package ru.netology.nework.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.extensions.hideKeyboard
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.registerImagePickerLauncher
import ru.netology.nework.extensions.showPhotoSelectionDialog
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post), OnSaveActionListener {
    private val binding by viewBinding(FragmentNewPostBinding::bind)
    private val viewModel: PostViewModel by activityViewModels()
    private val pickPhotoLauncher = registerImagePickerLauncher(
        onSuccess = { uri, file ->
            viewModel.changeMedia(uri, file, AttachmentType.IMAGE)
        },
        onError = { error ->
            viewModel.handleError(error)
        }
    )
    @Inject
    lateinit var yandexMapManager: YandexMapManager
//    private val locationPermissionLauncher = registerLocationPermissionLauncher {
//        yandexMapManager.enableUserLocation()
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupYandexMap()
        setupListeners()
        observeViewModel()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
//        checkAndRequestLocationPermission(locationPermissionLauncher) {
//            yandexMapManager.enableUserLocation()
//        }
    }

    private fun setupListeners() {
        with(binding) {
            btnPhoto.setOnClickListener {
                showPhotoSelectionDialog(launcher = pickPhotoLauncher)
            }

            removeAttachment.setOnClickListener {
                viewModel.clearMedia()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.postCreated.collectLatest { findNavController().navigateUp() } }

                launch {
                    viewModel.media.collectLatest { mediaModel ->
                        when (mediaModel.type) {
                            AttachmentType.IMAGE -> {
                                binding.attachment.isVisible = true
                                binding.attachmentContainer.imageAttachment.isVisible = true
                                binding.attachmentContainer.imageAttachment.load(mediaModel.uri.toString())
                            }
                            AttachmentType.VIDEO -> TODO()
                            AttachmentType.AUDIO -> TODO()
                            null -> {
                                binding.attachment.isVisible = false
                                binding.attachmentContainer.imageAttachment.isVisible = false
                                binding.attachmentContainer.audioAttachment.isVisible = false
                                binding.attachmentContainer.videoAttachment.isVisible = false
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

    override fun onSaveAction() {
        val content = binding.edit.text.toString()
        viewModel.changeContent(content)
        hideKeyboard()
        viewModel.save()
    }

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "403" -> R.string.authorization_is_required_for_this_action
            "415" -> R.string.incorrect_attachment_format
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}