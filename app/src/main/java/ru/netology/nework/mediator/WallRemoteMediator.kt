package ru.netology.nework.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dao.user.WallRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.post.toEntity
import ru.netology.nework.entity.user.WallRemoteKeyEntity
import ru.netology.nework.extensions.getOrThrow

@OptIn(ExperimentalPagingApi::class)
class WallRemoteMediator @AssistedInject constructor(
    private val appDb: AppDb,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    private val postDao: PostDao,
    private val apiService: ApiService,
    @Assisted private val authorId: Long,
    @Assisted private val isMyProfile: Boolean,
) : RemoteMediator<Int, PostEntity>() {
    @AssistedFactory
    interface Factory {
        fun create(authorId: Long, isMyProfile: Boolean): WallRemoteMediator
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (isMyProfile) {
                        apiService.getLatestMyWall(state.config.initialLoadSize)
                    } else {
                        apiService.getLatestAuthorWall(authorId, state.config.initialLoadSize)
                    }
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = wallRemoteKeyDao.getBeforeKeyByAuthorId(authorId)
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    if (isMyProfile) {
                        apiService.getBeforeMyWall(id, state.config.pageSize)
                    } else {
                        apiService.getBeforeAuthorWall(authorId, id, state.config.pageSize)
                    }
                }
            }
            val body = response.getOrThrow()
            if (body.isEmpty()) return MediatorResult.Success(endOfPaginationReached = true)

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        wallRemoteKeyDao.removeByAuthorId(authorId)
                        postDao.removeByAuthorId(authorId)
                        wallRemoteKeyDao.insert(
                            listOf(
                                WallRemoteKeyEntity(
                                    type = WallRemoteKeyEntity.KeyType.AFTER,
                                    authorId = authorId,
                                    id = body.first().id
                                ),
                                WallRemoteKeyEntity(
                                    type = WallRemoteKeyEntity.KeyType.BEFORE,
                                    authorId = authorId,
                                    id = body.last().id
                                )
                            )
                        )
                    }

                    LoadType.PREPEND -> {}

                    LoadType.APPEND -> {
                        wallRemoteKeyDao.insert(
                            WallRemoteKeyEntity(
                                type = WallRemoteKeyEntity.KeyType.BEFORE,
                                authorId = authorId,
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