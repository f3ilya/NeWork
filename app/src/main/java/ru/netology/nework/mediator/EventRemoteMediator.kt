package ru.netology.nework.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.event.EventDao
import ru.netology.nework.dao.event.EventRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.event.EventEntity
import ru.netology.nework.entity.event.EventRemoteKeyEntity
import ru.netology.nework.entity.event.toEntity
import ru.netology.nework.extensions.getOrThrow
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class EventRemoteMediator @Inject constructor(
    private val appDb: AppDb,
    private val eventDao: EventDao,
    private val eventRemoteKeyDao: EventRemoteKeyDao,
    private val apiService: ApiService,
) : RemoteMediator<Int, EventEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EventEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    apiService.getLatestEvents(state.config.initialLoadSize)
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = eventRemoteKeyDao.getBeforeKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBeforeEvents(id, state.config.pageSize)
                }
            }

            val body = response.getOrThrow()

            if (body.isEmpty()) return MediatorResult.Success(endOfPaginationReached = true)

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        eventRemoteKeyDao.removeAll()
                        eventDao.removeAll()
                        eventRemoteKeyDao.upsert(
                            listOf(
                                EventRemoteKeyEntity(
                                    type = EventRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id
                                ),
                                EventRemoteKeyEntity(
                                    type = EventRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id
                                ),
                            )
                        )
                    }

                    LoadType.PREPEND -> {}

                    LoadType.APPEND -> {
                        eventRemoteKeyDao.upsert(
                            EventRemoteKeyEntity(
                                type = EventRemoteKeyEntity.KeyType.BEFORE,
                                id = body.last().id
                            )
                        )
                    }
                }
                eventDao.upsert(body.toEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception

            return MediatorResult.Error(exception)
        }
    }
}