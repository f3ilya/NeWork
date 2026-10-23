package ru.netology.nework.viewmodel.post

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
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.AppModelState
import ru.netology.nework.model.MapModel
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.post.PostRepository
import java.io.File
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val empty = Post(
    id = 0,
    authorId = 0,
    author = "",
    authorJob = null,
    authorAvatar = null,
    content = "",
    published = "",
    coordinates = null,
    link = null,
    mentionIds = emptyList(),
    mentionedMe = false,
    likeOwnerIds = emptyList(),
    likedByMe = false,
    attachment = null,
    users = emptyMap(),
    ownedByMe = false,
)

private val noMedia = MediaModel()
private val noMap = MapModel()

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: PostRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    val edited = MutableStateFlow(empty)

    private val _postCreated = MutableSharedFlow<Unit>()
    val postCreated: SharedFlow<Unit> = _postCreated.asSharedFlow()

    private val _media = MutableStateFlow(MediaModel())
    val media: StateFlow<MediaModel> = _media.asStateFlow()

    private val _map = MutableStateFlow(MapModel())
    val map: StateFlow<MapModel> = _map.asStateFlow()

    @OptIn(ExperimentalTime::class)
    fun save() {
        edited.value.let { post ->
            _state.value = AppModelState(loading = true)
            viewModelScope.launch {
                runCatching {
                    val mediaState = _media.value
                    val mapState = _map.value
                    val coordinates = if (mapState.lat != null && mapState.long != null) {
                        Coordinates(mapState.lat, mapState.long)
                    } else null
                    val timePublished = Clock.System.now().toString()
                    if (mediaState == noMedia || mediaState.file == null) {
                        repository.savePost(
                            post.copy(
                                published = timePublished,
                                coordinates = coordinates
                            )
                        )
                    } else {
                        val attachmentType = mediaState.type ?: throw UnknownError()
                        val media = repository.saveMedia(mediaState.file)
                        val updatedPost = post.copy(
                            published = timePublished,
                            coordinates = coordinates,
                            attachment = Attachment(url = media.url, type = attachmentType)
                        )
                        repository.savePost(updatedPost)
                    }
                }.onSuccess {
                    resetState()
                    clearEdited()
                    _postCreated.emit(Unit)
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception

                    handleError(exception)
                }
            }
        }
    }

    fun changeContent(content: String) {
        edited.value = edited.value.copy(content = content.trim())
    }

    fun changeMedia(uri: Uri, file: File, type: AttachmentType) {
        _media.value = MediaModel(uri = uri, file = file, type = type)
    }

    fun changeMap(lat: Double, long: Double) {
        _map.value = MapModel(lat = lat, long = long)
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun clearMedia() {
        _media.value = noMedia
    }

    fun clearMap() {
        _map.value = noMap
    }

    private fun clearEdited() {
        edited.value = empty
        _media.value = noMedia
        _map.value = noMap
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}