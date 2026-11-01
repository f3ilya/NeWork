package ru.netology.nework.repository.user

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.user.JobDao
import ru.netology.nework.dao.user.UserDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.entity.user.JobEntity
import ru.netology.nework.entity.user.UserEntity
import ru.netology.nework.entity.user.toDto
import ru.netology.nework.entity.user.toEntity
import ru.netology.nework.extensions.getOrThrow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val jobDao: JobDao,
    private val appAuth: AppAuth,
    private val appDb: AppDb,
) : UserRepository {
    override fun getUsersAll(): Flow<List<UserResponse>> =
        userDao.getUsersAll().map { it.toDto() }

    override fun getUsersByIds(ids: List<Long>): Flow<List<UserResponse>> =
        userDao.getUsersByIds(ids).map { it.toDto() }

    override fun getUserById(id: Long): Flow<UserResponse?> =
        userDao.getUsersById(id).map { it?.toDto() }

    override fun getJobsByOwnerId(ownerId: Long): Flow<List<Job>> =
        jobDao.getJobsByOwnerId(ownerId).combine(appAuth.authStateFlow) { jobs, authState ->
            jobs.map { it.toDto().copy(ownedByMe = ownerId == authState.id) }
        }

    override suspend fun checkAndRefreshUsers(ids: List<Long>) {
        if (ids.isEmpty() || ids.size != userDao.getExistingIds(ids).size) {
            val response = apiService.getAllUsers()
            val users = response.getOrThrow()
            userDao.insert(users.toEntity())
        }
    }

    override suspend fun checkAndRefreshUser(id: Long) {
        if (!userDao.isUserExists(id)) {
            val response = apiService.getUser(id)
            val user = response.getOrThrow()
            userDao.insert(UserEntity.fromDto(user))
        }
    }

    override suspend fun refreshJobs(ownerId: Long) {
        val myId = appAuth.authStateFlow.value.id
        val response = if (ownerId == myId) {
            apiService.getMyJobs()
        } else {
            apiService.getUserJobs(ownerId)
        }
        val body = response.getOrThrow()

        appDb.withTransaction {
            jobDao.removeJobsByOwnerId(ownerId)
            jobDao.insert(body.toEntity(ownerId))
        }
    }

    override suspend fun saveJob(job: Job) {
        val myId = appAuth.authStateFlow.value.id
        val response = apiService.saveJob(job)
        val body = response.getOrThrow()
        jobDao.insert(JobEntity.fromDto(body, myId))
    }

    override suspend fun removeJob(jobId: Long) {
        apiService.removeJob(jobId).getOrThrow()
        jobDao.removeJobById(jobId)
    }
}