package ru.netology.nework.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
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

    @OptIn(UnstableApi::class)
    fun authentication(login: String, pass: String) {
        if (_state.value.loading) return

        _state.value = AppModelState(loading = true)
        viewModelScope.launch {
            runCatching {
                repository.authentication(login, pass)
            }.onSuccess {
                _state.value = AppModelState()
                _authSuccess.emit(Unit)
            }.onFailure { exception ->
                exception.message?.let { Log.d("MyTagViewModel", it) }
                if (exception is CancellationException) throw exception

                val error = AppError.from(exception)
                _state.value = AppModelState(
                    error = true,
                    errorMessage = error.code
                )
            }
        }
    }

    fun resetState() {
        _state.value = AppModelState()
    }
}