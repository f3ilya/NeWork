package ru.netology.nework.dao.post

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.netology.nework.entity.post.PostRemoteKeyEntity

@Dao
interface PostRemoteKeyDao {
    @Query("SELECT id FROM PostRemoteKeyEntity WHERE type = 'BEFORE'")
    suspend fun getBeforeKey(): Long?

    @Upsert
    suspend fun insert(key: PostRemoteKeyEntity)

    @Upsert
    suspend fun insert(keys: List<PostRemoteKeyEntity>)

    @Query("DELETE FROM PostRemoteKeyEntity")
    suspend fun removeAll()
}