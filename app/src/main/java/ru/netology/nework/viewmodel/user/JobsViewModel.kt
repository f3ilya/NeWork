package ru.netology.nework.viewmodel.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Job
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.fragment.user.JobsFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.user.UserRepository
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
) : ViewModel() {
    private val args = JobsFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val userId = args.userId
    val isMyProfile = args.isMyProfile
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    val jobs: StateFlow<List<Job>> = repository.getJobsByOwnerId(userId)
        .catch { exception -> handleError(exception) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        refresh()
    }

    fun refresh(isRefreshing: Boolean = false) {
        _state.update { state ->
            state.copy(
                loading = !isRefreshing,
                refreshing = isRefreshing,
                error = false,
                errorType = null
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.refreshJobs(userId)
            }.onSuccess {
                resetState()
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun removeJob(jobId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.removeJob(jobId)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun clearErrorMessage() {
        _state.update { state ->
            state.copy(errorType = null)
        }
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { code ->
            when (code) {
                403 -> ErrorType.AUTH_REQUIRED
                else -> ErrorType.UNKNOWN
            }
        }
        _state.update { state ->
            state.copy(
                loading = false,
                refreshing = false,
                error = true,
                errorType = errorType
            )
        }
    }
}