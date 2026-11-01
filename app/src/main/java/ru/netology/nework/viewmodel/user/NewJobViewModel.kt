package ru.netology.nework.viewmodel.user

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.user.UserRepository
import javax.inject.Inject

private val empty = Job(
    id = 0,
    name = "",
    position = "",
    start = "",
    finish = null,
    link = null,
    ownerId = null,
    ownedByMe = true,
)

@HiltViewModel
class NewJobViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _edited = MutableStateFlow(empty)
    val edited: StateFlow<Job> = _edited.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    fun saveJob() {
        if (_state.value.loading) return
        _edited.value.let { job ->
            _state.value = AppModelState(loading = true)
            viewModelScope.launch {
                runCatching {
                    repository.saveJob(job)
                }.onSuccess {
                    resetState()
                    clearEdited()
                    _saveSuccess.emit(Unit)
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception

                    handleError(exception)
                }
            }
        }
    }

    fun changeEdited(name: String, position: String, link: String?) {
        _edited.update { job ->
            job.copy(
                name = name,
                position = position,
                link = link
            )
        }
    }

    fun changeFinish(finish: String?) {
        _edited.update { job ->
            job.copy(finish = finish)

        }
    }

    fun changeStart(start: String) {
        _edited.update { job ->
            job.copy(start = start)
        }
    }

    fun clearEdited() {
        _edited.value = empty
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}