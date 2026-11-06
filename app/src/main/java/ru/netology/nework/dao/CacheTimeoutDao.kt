package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.netology.nework.entity.CacheTimeoutEntity

@Dao
interface CacheTimeoutDao {
    @Upsert
    suspend fun saveLastUpdated(timeout: CacheTimeoutEntity)

    @Query("SELECT lastUpdated FROM CacheTimeoutEntity WHERE type = 'POSTS'")
    suspend fun getPostsLastUpdated(): Long?

    @Query("SELECT lastUpdated FROM CacheTimeoutEntity WHERE type = 'EVENTS'")
    suspend fun getEventsLastUpdated(): Long?
}