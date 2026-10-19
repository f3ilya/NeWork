package ru.netology.nework.repository.post

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Post

interface PostRepository {
    val data: Flow<PagingData<Post>>
    fun setManualRefresh(isManual: Boolean)
    suspend fun getAll()
    suspend fun likePost(id: Long, likeByMe: Boolean)
    suspend fun getPostById(id: Long): Flow<Post?>
}