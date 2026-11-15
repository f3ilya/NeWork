package ru.netology.nework.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.auth.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _authSuccess = MutableSharedFlow<Unit>()
    val authSuccess: SharedFlow<Unit> = _authSuccess.asSharedFlow()

    fun authentication(login: String, pass: String) {
        val previousState = _state.getAndUpdate { state ->
            state.copy(
                loading = true,
                refreshing = false,
                error = false,
                errorType = null
            )
        }
        if (previousState.loading) return
        viewModelScope.launch {
            runCatching {
                repository.authentication(login, pass)
            }.onSuccess {
                resetState()
                _authSuccess.emit(Unit)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { code ->
            when (code) {
                400 -> ErrorType.INVALID_PASSWORD
                404 -> ErrorType.USER_NOT_REGISTRATION
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