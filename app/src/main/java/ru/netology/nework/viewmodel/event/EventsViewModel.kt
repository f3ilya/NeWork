package ru.netology.nework.viewmodel.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Event
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.event.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository,
    appAuth: AppAuth
) : ViewModel() {
    private val cachedEvents: Flow<PagingData<Event>> = repository
        .getEventPagingData()
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Event>> = appAuth.authStateFlow
        .flatMapLatest { authState ->
            val myId = authState.id
            cachedEvents.map { pagingData ->
                pagingData.map { event ->
                    event.copy(
                        ownedByMe = event.authorId == myId,
                        likedByMe = myId in event.likeOwnerIds,
                        participatedByMe = myId in event.participantsIds
                    )
                }
            }
        }.cachedIn(viewModelScope)

    val authState: StateFlow<AuthState> = appAuth.authStateFlow

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    fun removeEvent(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.removeEvent(id)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
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