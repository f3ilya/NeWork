package ru.netology.nework.repository.post

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.post.toEntity
import ru.netology.nework.error.AppError
import ru.netology.nework.extensions.getOrThrow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: ApiService,
    mediator: PostRemoteMediator,
) : PostRepository {
    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = true, initialLoadSize = 10),
        remoteMediator = mediator,
        pagingSourceFactory = postDao::pagingSource,
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    override suspend fun getAll() {
        runCatching {
            val response = apiService.getAll()
            val body = response.getOrThrow()
            postDao.insert(body.toEntity())
        }.onFailure { exception ->
            if (exception is CancellationException) throw exception

            throw AppError.from(exception)
        }
    }

    override suspend fun likePost(id: Long, likeByMe: Boolean) {
        runCatching {
            val post = if (likeByMe) {
                apiService.dislikePost(id).getOrThrow()
            } else {
                apiService.likePost(id).getOrThrow()
            }
            postDao.insert(PostEntity.fromDto(post))
        }.onFailure { exception ->
            if (exception is CancellationException) throw exception

            throw AppError.from(exception)
        }
    }
}