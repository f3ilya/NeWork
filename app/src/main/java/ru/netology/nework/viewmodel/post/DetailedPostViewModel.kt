package ru.netology.nework.viewmodel.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.post.PostRepository
import javax.inject.Inject

@HiltViewModel
class DetailedPostViewModel @Inject constructor(
    private val repository: PostRepository,
    appAuth: AppAuth
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()
    private val currentPostId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedPost: StateFlow<Post?> = currentPostId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            repository.getPostById(id)
                .combine(appAuth.authStateFlow) { post, authState ->
                    val myId = authState.id
                    post?.copy(ownedByMe = post.authorId == myId)
                }
                .onStart { _state.value = AppModelState(loading = true) }
                .onEach { resetState() }
                .catch { exception -> handleError(exception) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(0, 0),
        initialValue = null
    )

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

    fun setDetailedPostId(id: Long) {
        currentPostId.value = id
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}