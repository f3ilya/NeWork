package ru.netology.nework.repository.user

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dao.user.JobDao
import ru.netology.nework.dao.user.UserDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.user.JobEntity
import ru.netology.nework.entity.user.UserEntity
import ru.netology.nework.entity.user.toDto
import ru.netology.nework.entity.user.toEntity
import ru.netology.nework.extensions.getOrThrow
import ru.netology.nework.mediator.WallRemoteMediator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val jobDao: JobDao,
    private val appAuth: AppAuth,
    private val appDb: AppDb,
    private val postDao: PostDao,
    private val wallRemoteMediator: WallRemoteMediator.Factory
) : UserRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun getWallPagingData(authorId: Long, isMyProfile: Boolean): Flow<PagingData<Post>> =
        Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = true, initialLoadSize = 10),
            remoteMediator = wallRemoteMediator.create(authorId, isMyProfile),
            pagingSourceFactory = { postDao.pagingSourceByAuthorId(authorId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDto() }
        }

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

    override suspend fun likePost(
        authorId: Long,
        postId: Long,
        likedByMe: Boolean,
        isMyProfile: Boolean
    ) {
        val response = when {
            isMyProfile && likedByMe -> apiService.dislikeMyWall(postId)
            isMyProfile && !likedByMe -> apiService.likeMyWall(postId)
            !isMyProfile && likedByMe -> apiService.dislikeAuthorWall(authorId, postId)
            else -> apiService.likeAuthorWall(authorId, postId)
        }
        response.getOrThrow().let { post ->
            postDao.insert(PostEntity.fromDto(post))
        }
    }

    override suspend fun checkAndRefreshUsers(ids: List<Long>) {
        if (ids.isEmpty() || ids.size != userDao.getExistingIds(ids).size) {
            val response = apiService.getAllUsers()
            response.getOrThrow().let { users ->
                userDao.insert(users.toEntity())
            }
        }
    }

    override suspend fun checkAndRefreshUser(id: Long) {
        if (!userDao.isUserExists(id)) {
            val response = apiService.getUser(id)
            response.getOrThrow().let { user ->
                userDao.insert(UserEntity.fromDto(user))
            }
        }
    }

    override suspend fun refreshJobs(ownerId: Long) {
        val myId = appAuth.authStateFlow.value.id
        val response = if (ownerId == myId) {
            apiService.getMyJobs()
        } else {
            apiService.getUserJobs(ownerId)
        }
        response.getOrThrow().let { jobs ->
            appDb.withTransaction {
                jobDao.removeJobsByOwnerId(ownerId)
                jobDao.insert(jobs.toEntity(ownerId))
            }
        }
    }

    override suspend fun saveJob(job: Job) {
        val myId = appAuth.authStateFlow.value.id
        val response = apiService.saveJob(job)
        response.getOrThrow().let { job ->
            jobDao.insert(JobEntity.fromDto(job, myId))
        }
    }

    override suspend fun removeJob(jobId: Long) {
        apiService.removeJob(jobId).getOrThrow()
        jobDao.removeJobById(jobId)
    }
}