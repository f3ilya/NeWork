package ru.netology.nework.viewmodel.post

import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.error.AppError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.fragment.post.NewPostFragmentArgs
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

    private val args = NewPostFragmentArgs.fromSavedStateHandle(savedStateHandle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: StateFlow<List<UserResponse>> = _editedPost.map { it.mentionIds }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getUsersByIds(_editedPost.value.mentionIds)
                    .onStart { _state.value = AppModelState(loading = true) }
                    .onEach { resetState() }
                    .catch { exception -> handleError(exception) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0, 0),
            initialValue = emptyList()
        )

    init {
        val postId = args.postId
        if (postId == 0L) {
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
        if (_state.value.loading) return
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

    fun changeMentioned(ids: List<Long>) {
        Log.d("MyDebugChangeMent", ids.toString())
        _editedPost.update { post ->
            post.copy(mentionIds = ids)
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

    fun clearMentioned() {
        _editedPost.update { post ->
            post.copy(mentionIds = emptyList())
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