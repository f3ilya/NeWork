package ru.netology.nework.dao.event

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.event.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM EventEntity WHERE id = :id")
    fun getEventById(id: Long): Flow<EventEntity>

    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun pagingSource(): PagingSource<Int, EventEntity>

    @Upsert
    suspend fun upsert(eventEntity: EventEntity)

    @Upsert
    suspend fun upsert(eventsEntity: List<EventEntity>)

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun removeEvent(id: Long)

    @Query("DELETE FROM EventEntity")
    suspend fun removeAll()
}