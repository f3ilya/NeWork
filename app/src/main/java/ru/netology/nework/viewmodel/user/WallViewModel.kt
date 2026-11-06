package ru.netology.nework.viewmodel.user

import androidx.lifecycle.SavedStateHandle
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
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import ru.netology.nework.fragment.user.WallFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.user.UserRepository
import javax.inject.Inject

@HiltViewModel
class WallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository,
    appAuth: AppAuth
) : ViewModel() {
    private val args = WallFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val userId = args.userId
    val isMyProfile = args.isMyProfile

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Post>> = appAuth.authStateFlow
        .flatMapLatest { authState ->
            val myId = authState.id
            repository.getWallPagingData(userId, isMyProfile).map { pagingData ->
                pagingData.map { post ->
                    post.copy(likedByMe = myId in post.likeOwnerIds)
                }
            }
        }.cachedIn(viewModelScope)

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    fun likePost(postId: Long, likedByMe: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.likePost(userId, postId, likedByMe, isMyProfile)
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