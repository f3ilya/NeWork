package ru.netology.nework.repository.user

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.UserResponse

interface UserRepository {
    fun getUsersAll(): Flow<List<UserResponse>>
    fun getUsersByIds(ids: List<Long>): Flow<List<UserResponse>>
    suspend fun refresh()
}