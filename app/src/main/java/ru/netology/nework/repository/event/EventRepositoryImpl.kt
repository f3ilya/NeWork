package ru.netology.nework.repository.event

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
import ru.netology.nework.dao.event.EventDao
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import ru.netology.nework.entity.event.EventEntity
import ru.netology.nework.entity.event.toDto
import ru.netology.nework.entity.event.toEntity
import ru.netology.nework.extensions.getOrThrow
import ru.netology.nework.mediator.EventRemoteMediator
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val apiService: ApiService,
    private val mediator: EventRemoteMediator
) : EventRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun getEventPagingData(): Flow<PagingData<Event>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = true, initialLoadSize = 10),
        remoteMediator = mediator,
        pagingSourceFactory = eventDao::pagingSource
    ).flow.map { pagingData ->
        pagingData.map(EventEntity::toDto)
    }

    override suspend fun getEventById(id: Long): Flow<Event> =
        eventDao.getEventById(id).map(EventEntity::toDto)

    override suspend fun likeEvent(id: Long, likedByMe: Boolean) {
        val response = if (likedByMe) {
            apiService.dislikeEvent(id)
        } else {
            apiService.likeEvent(id)
        }
        response.getOrThrow().let { event ->
            eventDao.upsert(event.toEntity())
        }
    }

    override suspend fun participantEvent(id: Long, participatedByMe: Boolean) {
        val response = if (participatedByMe) {
            apiService.nonParticipantEvent(id)
        } else {
            apiService.participantEvent(id)
        }
        response.getOrThrow().let { event ->
            eventDao.upsert(event.toEntity())
        }
    }

    override suspend fun saveEvent(event: Event) {
        val response = apiService.saveEvent(event)
        response.getOrThrow().let { event ->
            eventDao.upsert(event.toEntity())
        }
    }

    override suspend fun saveMedia(file: File): Media {
        val media = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody()
        )
        val response = apiService.saveMedia(media)
        return response.getOrThrow()
    }

    override suspend fun removeEvent(id: Long) {
        apiService.removeEvent(id)
        eventDao.removeEvent(id)
    }
}