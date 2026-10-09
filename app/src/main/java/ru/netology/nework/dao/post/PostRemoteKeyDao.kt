package ru.netology.nework.dao.post

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.post.PostRemoteKeyEntity

@Dao
interface PostRemoteKeyDao {
    @Query("SELECT COUNT(*) == 0 FROM PostRemoteKeyEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT id FROM PostRemoteKeyEntity WHERE type = 'AFTER'")
    suspend fun getAfterKey(): Long?

    @Query("SELECT id FROM PostRemoteKeyEntity WHERE type = 'BEFORE'")
    suspend fun getBeforeKey(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: PostRemoteKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keys: List<PostRemoteKeyEntity>)

    @Query("DELETE FROM PostRemoteKeyEntity")
    suspend fun removeAll()
}