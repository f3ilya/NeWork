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
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
//                    val id = postRemoteKeyDao.getAfterKey()
//                    if (id == null) {
//                        apiService.getLatestPosts(state.config.initialLoadSize)
//                    } else {
//                        apiService.getAfterPosts(id, state.config.pageSize)
//                    }
                    apiService.getLatestPosts(state.config.initialLoadSize)
                }

                LoadType.PREPEND -> {
//                    val id = postRemoteKeyDao.getAfterKey() ?: return MediatorResult.Success(
//                        endOfPaginationReached = false
//                    )
//                    apiService.getAfterPosts(id, state.config.pageSize)
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

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
                        postRemoteKeyDao.removeAll()
                        postDao.removeAll()
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
}