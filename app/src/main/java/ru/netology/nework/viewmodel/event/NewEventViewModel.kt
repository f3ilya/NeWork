package ru.netology.nework.viewmodel.event

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.EventType
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.error.AppError
import ru.netology.nework.error.ErrorType
import ru.netology.nework.error.toErrorType
import ru.netology.nework.fragment.event.NewEventFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.event.EventRepository
import ru.netology.nework.repository.user.UserRepository
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val empty = Event(
    id = 0,
    authorId = 0,
    author = "",
    authorJob = null,
    authorAvatar = null,
    content = "",
    datetime = "",
    published = "",
    coordinates = null,
    type = EventType.ONLINE,
    likeOwnerIds = emptyList(),
    likedByMe = false,
    speakerIds = emptyList(),
    participantsIds = emptyList(),
    participatedByMe = false,
    attachment = null,
    link = null,
    users = emptyMap(),
    ownedByMe = false
)

private val noMedia = MediaModel()

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _eventCreated = MutableSharedFlow<Unit>()
    val eventCreated: SharedFlow<Unit> = _eventCreated.asSharedFlow()

    private val _media = MutableStateFlow(MediaModel())
    val media: StateFlow<MediaModel> = _media.asStateFlow()

    private val _editedEvent = MutableStateFlow(empty)
    val editedEvent: StateFlow<Event> = _editedEvent.asStateFlow()

    val args = NewEventFragmentArgs.fromSavedStateHandle(savedStateHandle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val speakers: StateFlow<List<UserResponse>> = _editedEvent.map { it.speakerIds }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            userRepository.getUsersByIds(ids)
                .catch { exception -> handleError(exception) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        val eventId = args.eventId
        if (eventId == 0L) {
            _editedEvent.value = empty
        } else {
            viewModelScope.launch {
                eventRepository.getEventById(eventId)
                    .catch { exception -> handleError(exception) }
                    .collect { event ->
                        _editedEvent.value = event
                        event.attachment?.let {
                            changeMedia(it.url.toUri(), null, it.type)
                        }
                    }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun save() {
        val previousState = _state.getAndUpdate { state ->
            state.copy(
                loading = true,
                refreshing = false,
                error = false,
                errorType = null
            )
        }
        if (previousState.loading) return
        _editedEvent.value.let { event ->
            viewModelScope.launch {
                runCatching {
                    val mediaState = _media.value
                    val timePublished = event.published.ifEmpty { Clock.System.now().toString() }
                    val updatedEvent = if (mediaState.file == null) {
                        event.copy(published = timePublished)
                    } else {
                        val attachmentType = mediaState.type ?: throw AppError.UnknownError()
                        val media = eventRepository.saveMedia(mediaState.file)
                        event.copy(
                            published = timePublished,
                            attachment = Attachment(url = media.url, type = attachmentType)
                        )
                    }
                    eventRepository.saveEvent(updatedEvent)
                }.onSuccess {
                    resetState()
                    clearEdited()
                    _eventCreated.emit(Unit)
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception

                    handleError(exception)
                }
            }
        }
    }

    fun changeType(type: EventType) {
        _editedEvent.update { event ->
            event.copy(type = type)
        }
    }

    fun changeDatetime(datetime: String) {
        _editedEvent.update { event ->
            event.copy(datetime = datetime)
        }
    }

    fun changeContent(content: String) {
        _editedEvent.update { event ->
            event.copy(content = content.trim())
        }
    }

    fun changeMedia(uri: Uri, file: File?, type: AttachmentType?) {
        _media.update { model ->
            model.copy(uri = uri, file = file, type = type)
        }
    }

    fun changeMap(lat: Double, long: Double) {
        _editedEvent.update { event ->
            event.copy(coordinates = Coordinates(lat = lat, long = long))
        }
    }

    fun changeSpeakers(ids: List<Long>) {
        _editedEvent.update { event ->
            event.copy(speakerIds = ids)
        }
    }

    fun clearMedia() {
        _media.value = noMedia
        _editedEvent.update { event ->
            event.copy(attachment = null)
        }
    }

    fun clearMap() {
        _editedEvent.update { event ->
            event.copy(coordinates = null)
        }
    }

    fun clearSpeakers() {
        _editedEvent.update { event ->
            event.copy(speakerIds = emptyList())
        }
    }

    fun clearEdited() {
        _editedEvent.value = empty
        _media.value = noMedia
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val errorType = exception.toErrorType { code ->
            when (code) {
                403 -> ErrorType.AUTH_REQUIRED
                415 -> ErrorType.INVALID_FILE
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