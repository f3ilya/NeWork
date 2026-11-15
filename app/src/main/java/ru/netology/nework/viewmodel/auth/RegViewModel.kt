package ru.netology.nework.viewmodel.auth

import android.net.Uri
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
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.auth.AuthRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RegViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val noPhoto = MediaModel()

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _regSuccess = MutableSharedFlow<Unit>()
    val regSuccess: SharedFlow<Unit> = _regSuccess.asSharedFlow()

    private val _photo = MutableStateFlow(noPhoto)
    val photo: StateFlow<MediaModel> = _photo.asStateFlow()

    fun registration(login: String, pass: String, name: String) {
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
                val file = if (_photo.value == noPhoto) null else _photo.value.file
                repository.registration(login, pass, name, file)
            }.onSuccess {
                resetState()
                _regSuccess.emit(Unit)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.update { model ->
            model.copy(
                uri = uri,
                file = file,
                type = null
            )
        }
    }

    fun clearPhoto() {
        _photo.value = noPhoto
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { code ->
            when (code) {
                403 -> ErrorType.USER_ALREADY_REGISTERED
                404 -> ErrorType.INVALID_PHOTO
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