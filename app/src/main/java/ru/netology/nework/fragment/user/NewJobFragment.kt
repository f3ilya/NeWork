package ru.netology.nework.fragment.user

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.user.NewJobViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class NewJobFragment : Fragment(R.layout.fragment_new_job), OnSaveActionListener {
    private val viewModel: NewJobViewModel by viewModels()
    private val binding by viewBinding(FragmentNewJobBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        with(binding) {
            etName.doAfterTextChanged { tilName.error = null }
            etPosition.doAfterTextChanged { tilPosition.error = null }
            etLink.doAfterTextChanged { tilLink.error = null }

            chipStart.setOnClickListener {
                showDatePickerDialog(R.string.start_date_job) { timestamp ->
                    val start = Instant.ofEpochMilli(timestamp).toString()
                    viewModel.changeStart(start)
                    dateError.isVisible = false
                }
            }

            chipFinish.setOnClickListener {
                showDatePickerDialog(R.string.end_date_job) { timestamp ->
                    val finish = Instant.ofEpochMilli(timestamp).toString()
                    viewModel.changeFinish(finish)
                    dateError.isVisible = false
                }
            }

            chipStart.setOnCloseIconClickListener {
                viewModel.changeStart("")
            }

            chipFinish.setOnCloseIconClickListener {
                viewModel.changeFinish(null)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.edited.collectLatest { bindDate(it) } }

                launch { viewModel.saveSuccess.collectLatest { findNavController().navigateUp() } }

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

    private fun bindDate(job: Job) {
        with(binding) {
            val start = job.start.takeIf { it.isNotEmpty() }?.let {
                Instant.parse(it)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
            }
            val finish = job.finish.takeIf { !it.isNullOrBlank() }?.let {
                Instant.parse(it)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
            }
            chipStart.text = start ?: getString(R.string.start_date_job)
            chipStart.isCloseIconVisible = !start.isNullOrBlank()
            chipFinish.text = when {
                !finish.isNullOrBlank() -> finish
                !start.isNullOrBlank() -> getString(R.string.present)
                else -> getString(R.string.end_date_job)
            }
            chipFinish.isCloseIconVisible = !finish.isNullOrBlank()
        }
    }

    private fun showDatePickerDialog(titleResId: Int, onDateSelected: (Long) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(titleResId)
            .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
            .setTheme(R.style.ThemeOverlay_App_DatePicker_Modal)
            .build()

        datePicker.addOnPositiveButtonClickListener { timestamp ->
            if (timestamp != null) onDateSelected(timestamp)
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun validateFields(): Boolean {
        var isValid = true

        with(binding) {
            if (etName.text.isNullOrBlank()) {
                tilName.error = getString(R.string.name_cannot_be_empty_job)
                isValid = false
            }

            if (etPosition.text.isNullOrBlank()) {
                tilPosition.error = getString(R.string.position_cannot_be_empty_job)
                isValid = false
            }

            if (!etLink.text.isNullOrBlank()
                && !Patterns.WEB_URL.matcher(etLink.text.toString()).matches()
            ) {
                tilLink.error = getString(R.string.incorrect_link_format_job)
                isValid = false
            }

            val start = viewModel.edited.value.start
            val finish = viewModel.edited.value.finish
            val errorText = when {
                start.isEmpty() -> getString(R.string.start_date_cannot_be_empty)
                !finish.isNullOrBlank() && Instant.parse(finish)
                    .isBefore(Instant.parse(start)) -> {
                    getString(R.string.start_date_cannot_be_after_end_date)
                }

                else -> null
            }
            dateError.text = errorText
            dateError.isVisible = errorText != null
            if (errorText != null) isValid = false
        }

        return isValid
    }

    override fun onSaveAction() {
        with(binding) {
            if (validateFields()) {
                viewModel.changeEdited(
                    name = etName.text.toString().trim(),
                    position = etPosition.text.toString().trim(),
                    link = etLink.text.toString().trim()
                )
                viewModel.saveJob()
            }
        }
    }

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "403" -> R.string.authorization_is_required_for_this_action
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}