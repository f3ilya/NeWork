package ru.netology.nework.dao.user

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.netology.nework.entity.user.WallRemoteKeyEntity

@Dao
interface WallRemoteKeyDao {
    @Query("SELECT id FROM WallRemoteKeyEntity WHERE type = 'BEFORE' AND authorId = :authorId")
    suspend fun getBeforeKeyByAuthorId(authorId: Long): Long?

    @Upsert
    suspend fun insert(remoteKeyEntity: List<WallRemoteKeyEntity>)

    @Upsert
    suspend fun insert(remoteKeyEntity: WallRemoteKeyEntity)

    @Query("DELETE FROM WallRemoteKeyEntity WHERE authorId = :authorId")
    suspend fun removeByAuthorId(authorId: Long)
}