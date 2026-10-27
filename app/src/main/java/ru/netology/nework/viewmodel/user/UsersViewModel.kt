package ru.netology.nework.viewmodel.user

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.error.AppError
import ru.netology.nework.fragment.user.UsersFragmentArgs
import ru.netology.nework.model.AppModelState
import ru.netology.nework.repository.user.UserRepository
import ru.netology.nework.util.FragmentMode
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserRepository
) : ViewModel() {
    private val args = UsersFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val mode: FragmentMode = args.fragmentMode
    private val userIds: List<Long> = args.userIds?.toList() ?: emptyList()

    private val _state = MutableStateFlow(AppModelState())
    val state: StateFlow<AppModelState> = _state.asStateFlow()

    private val _selectedUserIds = MutableStateFlow(emptySet<Long>())
    val selectedUserIds: StateFlow<Set<Long>> = _selectedUserIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: StateFlow<List<UserResponse>> = flowOf(mode)
        .flatMapLatest { mode ->
            if (mode == FragmentMode.USERS || mode == FragmentMode.SELECTED) {
                repository.getUsersAll()
            } else {
                repository.getUsersByIds(userIds)
            }
        }
        .onStart { _state.value = AppModelState(loading = true) }
        .onEach { resetState() }
        .catch { exception -> handleError(exception) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0, 0),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            runCatching {
                repository.refresh()
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception

                handleError(exception)
            }
        }
    }

    fun toggleUserSelection(id: Long) {
        val currentSet = _selectedUserIds.value
        _selectedUserIds.value = if (currentSet.contains(id)) currentSet - id else currentSet + id
    }

    fun resetState() {
        _state.value = AppModelState()
    }

    fun handleError(exception: Throwable) {
        val error = AppError.from(exception)
        _state.value = AppModelState(error = true, errorMessage = error.code)
    }
}