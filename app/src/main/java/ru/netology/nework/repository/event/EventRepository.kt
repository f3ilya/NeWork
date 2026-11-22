package ru.netology.nework.repository.event

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import java.io.File

interface EventRepository {
    fun getEventPagingData(): Flow<PagingData<Event>>
    suspend fun getEventById(id: Long): Flow<Event>
    suspend fun likeEvent(id: Long, likedByMe: Boolean)
    suspend fun participantEvent(id: Long, participatedByMe: Boolean)
    suspend fun saveEvent(event: Event)
    suspend fun saveMedia(file: File): Media
    suspend fun removeEvent(id: Long)
}