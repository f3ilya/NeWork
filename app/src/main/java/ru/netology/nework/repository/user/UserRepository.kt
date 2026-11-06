package ru.netology.nework.repository.user

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserResponse

interface UserRepository {
    fun getWallPagingData(authorId: Long, isMyProfile: Boolean): Flow<PagingData<Post>>
    fun getUsersAll(): Flow<List<UserResponse>>
    fun getUsersByIds(ids: List<Long>): Flow<List<UserResponse>>
    fun getUserById(id: Long): Flow<UserResponse?>
    fun getJobsByOwnerId(ownerId: Long): Flow<List<Job>>
    suspend fun likePost(authorId: Long, postId: Long, likedByMe: Boolean, isMyProfile: Boolean)
    suspend fun checkAndRefreshUsers(ids: List<Long>)
    suspend fun checkAndRefreshUser(id: Long)
    suspend fun refreshJobs(ownerId: Long)
    suspend fun saveJob(job: Job)
    suspend fun removeJob(jobId: Long)
}