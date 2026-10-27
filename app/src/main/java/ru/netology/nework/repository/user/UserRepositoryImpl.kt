package ru.netology.nework.repository.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.user.UserDao
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.entity.user.toDto
import ru.netology.nework.entity.user.toEntity
import ru.netology.nework.extensions.getOrThrow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
) : UserRepository {
    override fun getUsersAll(): Flow<List<UserResponse>> =
        userDao.getUsersAll().map { it.toDto() }

    override fun getUsersByIds(ids: List<Long>): Flow<List<UserResponse>> =
        userDao.getUsersByIds(ids).map { it.toDto() }

    override suspend fun refresh() {
        val response = apiService.getAllUsers()
        val users = response.getOrThrow()
        userDao.insert(users.toEntity())
    }
}