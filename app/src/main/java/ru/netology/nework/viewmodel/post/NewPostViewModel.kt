package ru.netology.nework.viewmodel.post

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.AppModelState
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

@HiltViewModel
class NewPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PostRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _postCreated = MutableSharedFlow<Unit>()
    val postCreated: SharedFlow<Unit> = _postCreated.asSharedFlow()

    private val _media = MutableStateFlow(MediaModel())
    val media: StateFlow<MediaModel> = _media.asStateFlow()

    private val _editedPost = MutableStateFlow(empty)
    val editedPost: StateFlow<Post> = _editedPost.asStateFlow()

    init {
        val postId = savedStateHandle.get<Long>("postId")
        if (postId == null) {
            _editedPost.value = empty
        } else {
            viewModelScope.launch {
                repository.getPostById(postId)
                    .onStart { _state.value = AppModelState(loading = true) }
                    .onEach { resetState() }
                    .catch { exception -> handleError(exception) }
                    .collect { post -> _editedPost.value = post }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun save() {
        _editedPost.value.let { post ->
            _state.value = AppModelState(loading = true)
            viewModelScope.launch {
                runCatching {
                    val mediaState = _media.value
                    val timePublished = post.published.ifEmpty { Clock.System.now().toString() }
                    if (mediaState.file == null) {
                        val updatedPost = post.copy(published = timePublished)
                        repository.savePost(updatedPost)
                    } else {
                        val attachmentType = mediaState.type ?: throw UnknownError()
                        val media = repository.saveMedia(mediaState.file)
                        val updatedPost = post.copy(
                            published = timePublished,
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
        _editedPost.update { post ->
            post.copy(content = content.trim())
        }
    }

    fun changeMedia(uri: Uri, file: File?, type: AttachmentType?) {
        _media.value = MediaModel(uri = uri, file = file, type = type)
    }

    fun changeMap(lat: Double, long: Double) {
        _editedPost.update { post ->
            post.copy(coordinates = Coordinates(lat = lat, long = long))
        }
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun clearMedia() {
        _media.value = noMedia
        _editedPost.update { post ->
            post.copy(attachment = null)
        }
    }

    fun clearMap() {
        _editedPost.update { post ->
            post.copy(coordinates = null)
        }
    }

    private fun clearEdited() {
        _editedPost.value = empty
        _media.value = noMedia
        clearMap()
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}