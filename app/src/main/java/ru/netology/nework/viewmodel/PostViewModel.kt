package ru.netology.nework.viewmodel

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.post.PostRepository
import javax.inject.Inject

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

private val noPhoto = PhotoModel()

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

    private val _photo = MutableStateFlow(PhotoModel())
    val photo: StateFlow<PhotoModel> = _photo.asStateFlow()

    var isInitialLoad = true

//    fun save() {
//        edited.value.let { post ->
//            _state.value = PostModelState(loading = true)
//            viewModelScope.launch {
//                runCatching<Unit> {
//                    when (_photo.value) {
//                        noPhoto -> repository.save(post)
//                        else -> _photo.value.file?.let { file ->
//                            repository.saveWithAttachment(post, MediaUplod(file))
//                        }
//                    }
//                    _state.value = PostModelState()
//                    _postCreated.emit(Unit)
//                    clearEdited()
//                }.onFailure { exception ->
//                    if (exception is CancellationException) throw exception
//
//                    _state.value = PostModelState(error = true)
//                }
//            }
//        }
//    }

    fun likePost(id: Long, likeByMe: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.likePost(id, likeByMe)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                val error = AppError.from(exception)
                _state.value = AppModelState(error = true, errorMessage = error.code)
            }
        }
    }

    fun handlePagingError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }

    fun resetState() {
        _state.value = AppModelState()
    }
}