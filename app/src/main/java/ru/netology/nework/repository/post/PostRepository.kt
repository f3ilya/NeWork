package ru.netology.nework.repository.post

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserResponse
import java.io.File

interface PostRepository {
    val data: Flow<PagingData<Post>>
    fun setManualRefresh(isManual: Boolean)
    fun getUsersByIds(ids: List<Long>): Flow<List<UserResponse>>
    suspend fun likePost(id: Long, likeByMe: Boolean)
    suspend fun getPostById(id: Long): Flow<Post>
    suspend fun savePost(post: Post)
    suspend fun saveMedia(file: File): Media
    suspend fun removePost(id: Long)
}