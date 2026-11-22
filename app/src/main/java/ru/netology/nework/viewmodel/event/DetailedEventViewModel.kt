package ru.netology.nework.viewmodel.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Event
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.fragment.event.DetailedEventFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.event.EventRepository
import javax.inject.Inject

@HiltViewModel
class DetailedEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appAuth: AppAuth,
    private val repository: EventRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _detailedEvent = MutableStateFlow<Event?>(null)
    val detailedEvent: StateFlow<Event?> = _detailedEvent.asStateFlow()

    private val args = DetailedEventFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            repository.getEventById(args.eventId)
                .combine(appAuth.authStateFlow) { event, authState ->
                    event.copy(ownedByMe = event.authorId == authState.id)
                }
                .catch { exception -> handleError(exception) }
                .collect { event -> _detailedEvent.value = event }
        }
    }

    fun removeEvent(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching {
                    repository.removeEvent(id)
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception

                    handleError(exception)
                }
            }
        }
    }

    fun likeEvent(id: Long, likedByMe: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.likeEvent(id, likedByMe)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun participantEvent(id: Long, participatedByMe: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.participantEvent(id, participatedByMe)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { code ->
            when (code) {
                403 -> ErrorType.AUTH_REQUIRED
                404 -> ErrorType.EVENT_NOT_FOUND
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

    fun resetState() {
        _state.value = AppModelState()
    }
}