package ru.netology.nework.fragment.event

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.BottomSheetEventBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.EventType
import ru.netology.nework.viewmodel.event.NewEventViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class EventBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_event) {
    private val parentViewModel: NewEventViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val binding by viewBinding(BottomSheetEventBinding::bind)
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private var datetime = ""

    companion object {
        const val TAG = "EVENT_BOTTOM_SHEET"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        with(binding) {
            typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedType = when (checkedId) {
                    R.id.radioOnline -> EventType.ONLINE
                    R.id.radioOffline -> EventType.OFFLINE
                    else -> return@setOnCheckedChangeListener
                }
                parentViewModel.changeType(selectedType)
            }

            etDate.setOnClickListener { showDatetimePickers() }
            tilDate.setOnClickListener { showDatetimePickers() }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentViewModel.editedEvent.collectLatest {
                    bindDatetime(it)
                    datetime = it.datetime
                }
            }
        }
    }

    private fun bindDatetime(event: Event) {
        with(binding) {
            if (event.datetime.isNotBlank()) {
                runCatching {
                    val formatted = Instant.parse(event.datetime)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                    etDate.setText(formatted)
                }
            }

            when (event.type) {
                EventType.ONLINE -> radioOnline.isChecked = true
                EventType.OFFLINE -> radioOffline.isChecked = true
            }
        }
    }

    private fun showDatetimePickers() {
        val currentInstant = datetime.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val todayUtc = MaterialDatePicker.todayInUtcMilliseconds()
        val initialDateMillis = currentInstant?.toEpochMilli()?.coerceAtLeast(todayUtc) ?: todayUtc
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(initialDateMillis)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { dateMillis ->
            val selectedDate = Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            showTimePicker(selectedDate, currentInstant)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(selectedDate: LocalDate, currentInstant: Instant?) {
        val initialTime = currentInstant?.atZone(ZoneId.systemDefault())?.toLocalTime()
            ?: LocalTime.now()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialTime.hour)
            .setMinute(initialTime.minute)
            .setTitleText(R.string.select_time)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedInstant = LocalDateTime.of(
                selectedDate,
                LocalTime.of(timePicker.hour, timePicker.minute)
            )
                .atZone(ZoneId.systemDefault())
                .toInstant()

            if (selectedInstant.isBefore(Instant.now())) {
                binding.tilDate.error = getString(R.string.error_past_event_date)
            } else {
                binding.tilDate.error = null
                binding.etDate.setText(
                    selectedInstant.atZone(ZoneId.systemDefault()).format(formatter)
                )
                parentViewModel.changeDatetime(selectedInstant.toString())
            }
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }
}