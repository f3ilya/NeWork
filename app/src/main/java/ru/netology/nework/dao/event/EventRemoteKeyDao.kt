package ru.netology.nework.dao.event

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.netology.nework.entity.event.EventRemoteKeyEntity

@Dao
interface EventRemoteKeyDao {
    @Query("SELECT id FROM EventRemoteKeyEntity WHERE type = 'BEFORE'")
    suspend fun getBeforeKey(): Long?

    @Upsert
    suspend fun upsert(key: EventRemoteKeyEntity)

    @Upsert
    suspend fun upsert(keys: List<EventRemoteKeyEntity>)

    @Query("DELETE FROM EventRemoteKeyEntity")
    suspend fun removeAll()
}