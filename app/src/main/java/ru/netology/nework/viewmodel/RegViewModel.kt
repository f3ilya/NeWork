package ru.netology.nework.viewmodel

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
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
import ru.netology.nework.model.AppModelState
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.repository.auth.AuthRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RegViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val noPhoto = PhotoModel()

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _regSuccess = MutableSharedFlow<Unit>()
    val regSuccess: SharedFlow<Unit> = _regSuccess.asSharedFlow()

    private val _photo = MutableStateFlow(noPhoto)
    val photo: StateFlow<PhotoModel> = _photo.asStateFlow()

    fun registration(login: String, pass: String, name: String) {
        if (_state.value.loading) return

        _state.value = AppModelState(loading = true)
        viewModelScope.launch {
            runCatching {
                val file = if (_photo.value == noPhoto) null else _photo.value.file
                repository.registration(login, pass, name, file)
            }.onSuccess {
                _state.value = AppModelState()
                _regSuccess.emit(Unit)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                val error = AppError.from(exception)
                _state.value = AppModelState(
                    error = true,
                    errorMessage = error.code
                )
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun clearPhoto() {
        _photo.value = noPhoto
    }

    fun resetState() {
        _state.value = AppModelState()
    }
}