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
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.fragment.user.DetailedUserFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.user.UserRepository
import javax.inject.Inject

@HiltViewModel
class DetailedUserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
    auth: AppAuth
) : ViewModel() {
    private val args = DetailedUserFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val userId = args.userId
    val isMyProfile = userId == auth.authStateFlow.value.id

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    val user: StateFlow<UserResponse?> = repository.getUserById(userId)
        .catch { exception -> handleError(exception) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        refresh()
    }

    fun refresh() {
        _state.update { state ->
            state.copy(
                loading = true,
                refreshing = false,
                error = false,
                errorType = null
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.checkAndRefreshUser(userId)
            }.onSuccess {
                resetState()
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

    fun changeError(errorType: ErrorType) {
        _state.update { state ->
            state.copy(errorType = errorType)
        }
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { status ->
            when (status) {
                403 -> ErrorType.AUTH_REQUIRED
                404 -> ErrorType.POST_NOT_FOUND
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