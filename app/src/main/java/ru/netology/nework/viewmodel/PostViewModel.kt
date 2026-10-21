package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
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
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    appAuth: AppAuth
) : ViewModel() {
    private val cached: Flow<PagingData<Post>> = repository.data
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Post>> = appAuth.authStateFlow
        .flatMapLatest { (myId) ->
            cached.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }
        .cachedIn(viewModelScope)

    val authState: StateFlow<AuthState> = appAuth.authStateFlow
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    val edited = MutableStateFlow(empty)

    private val _postCreated = MutableSharedFlow<Unit>()
    val postCreated: SharedFlow<Unit> = _postCreated.asSharedFlow()

    private val _media = MutableStateFlow(MediaModel())
    val media: StateFlow<MediaModel> = _media.asStateFlow()

    private val currentPostId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedPost: StateFlow<Post?> = currentPostId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            repository.getPostById(id)
                .onStart { _state.value = AppModelState(loading = true) }
                .onEach { resetState() }
                .catch { exception -> handleError(exception) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(0, 0),
        initialValue = null
    )

    var isInitialLoad = true

    @OptIn(ExperimentalTime::class)
    fun save() {
        edited.value.let { post ->
            _state.value = AppModelState(loading = true)
            viewModelScope.launch {
                runCatching {
                    val mediaState = _media.value
                    val timePublished = Clock.System.now().toString()
                    if (mediaState == noMedia || mediaState.file == null) {
                        repository.savePost(post.copy(published = timePublished))
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

    fun removePost(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.removePost(id)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun changeContent(content: String) {
        edited.value = edited.value.copy(content = content.trim())
    }

    fun changeMedia(uri: Uri, file: File, type: AttachmentType) {
        _media.value = MediaModel(uri = uri, file = file, type = type)
    }

    fun manualRefresh() {
        repository.setManualRefresh(true)
    }

    fun setDetailedPostId(id: Long) {
        currentPostId.value = id
    }

    fun likePost(id: Long, likeByMe: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.likePost(id, likeByMe)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun clearMedia() {
        _media.value = noMedia
    }

    private fun clearEdited() {
        edited.value = empty
        _media.value = noMedia
    }
}