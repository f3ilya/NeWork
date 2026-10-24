package ru.netology.nework.repository.post

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.extensions.getOrThrow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: ApiService,
    private val mediator: PostRemoteMediator,
) : PostRepository {
    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = true, initialLoadSize = 10),
        remoteMediator = mediator,
        pagingSourceFactory = postDao::pagingSource,
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    override fun setManualRefresh(isManual: Boolean) {
        mediator.isManualRefresh = true
    }

    override suspend fun likePost(id: Long, likeByMe: Boolean) {
        val post = if (likeByMe) {
            apiService.dislikePost(id).getOrThrow()
        } else {
            apiService.likePost(id).getOrThrow()
        }
        postDao.insert(PostEntity.fromDto(post))
    }

    override suspend fun getPostById(id: Long): Flow<Post> = postDao.getPostById(id).map { entity ->
        entity.toDto()
    }

    override suspend fun savePost(post: Post) {
        val response = apiService.savePost(post)
        val body = response.getOrThrow()
        postDao.insert(PostEntity.fromDto(body))
    }

    override suspend fun saveMedia(file: File): Media {
        val media = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody()
        )
        val response = apiService.saveMedia(media)
        return response.getOrThrow()
    }

    override suspend fun removePost(id: Long) {
        apiService.removePost(id).getOrThrow()
        postDao.removePost(id)
    }
}