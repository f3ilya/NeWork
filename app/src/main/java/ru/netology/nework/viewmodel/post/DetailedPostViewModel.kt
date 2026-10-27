package ru.netology.nework.viewmodel.post

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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.fragment.post.DetailedPostFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.post.PostRepository
import javax.inject.Inject

@HiltViewModel
class DetailedPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PostRepository,
    appAuth: AppAuth
) : ViewModel() {
    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _detailedPost = MutableStateFlow<Post?>(null)
    val detailedPost: StateFlow<Post?> = _detailedPost.asStateFlow()

    private val args = DetailedPostFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            repository.getPostById(args.postId)
                .combine(appAuth.authStateFlow) { post, authState ->
                    post.copy(ownedByMe = post.authorId == authState.id)
                }
                .onStart { _state.value = AppModelState(loading = true) }
                .onEach { resetState() }
                .catch { exception -> handleError(exception) }
                .collect { post -> _detailedPost.value = post }
        }
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

    fun removePost(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                runCatching {
                    repository.removePost(id)
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception

                    handleError(exception)
                }
            }
        }
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}