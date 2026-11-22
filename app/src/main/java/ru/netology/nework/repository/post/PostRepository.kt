package ru.netology.nework.repository.post

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import java.io.File

interface PostRepository {
    fun getPostPagingData(): Flow<PagingData<Post>>
    suspend fun getPostById(id: Long): Flow<Post>
    suspend fun likePost(id: Long, likedByMe: Boolean)
    suspend fun savePost(post: Post)
    suspend fun saveMedia(file: File): Media
    suspend fun removePost(id: Long)
}