package ru.netology.nework.repository.post

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dao.post.PostRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.post.PostRemoteKeyEntity
import ru.netology.nework.entity.post.toEntity
import ru.netology.nework.extensions.getOrThrow
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator @Inject constructor(
    private val appDb: AppDb,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val postDao: PostDao,
    private val apiService: ApiService,
) : RemoteMediator<Int, PostEntity>() {
    var isManualRefresh: Boolean = false

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> { apiService.getLatestPosts(state.config.initialLoadSize) }

                LoadType.PREPEND -> { return MediatorResult.Success(endOfPaginationReached = true) }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.getBeforeKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBeforePosts(id, state.config.pageSize)
                }
            }

            val body = response.getOrThrow()

            if (body.isEmpty()) return MediatorResult.Success(endOfPaginationReached = true)

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (isManualRefresh){
                            postRemoteKeyDao.removeAll()
                            postDao.removeAll()
                            isManualRefresh = false
                        }
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id
                                ),
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id
                                ),
                            )
                        )
                    }

                    LoadType.PREPEND -> {}

                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                id = body.last().id
                            )
                        )
                    }
                }
                postDao.insert(body.toEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception

            return MediatorResult.Error(exception)
        }
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.SKIP_INITIAL_REFRESH
    }
}