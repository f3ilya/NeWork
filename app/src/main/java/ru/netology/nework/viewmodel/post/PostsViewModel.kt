package ru.netology.nework.viewmodel.post

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
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.post.PostRepository
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
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

    var isInitialLoad = true

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

    fun manualRefresh() {
        repository.setManualRefresh(true)
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
}