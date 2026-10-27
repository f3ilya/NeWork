package ru.netology.nework.dao.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.user.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: List<UserEntity>)

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getUsersAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity WHERE id IN (:ids)")
    fun getUsersByIds(ids: List<Long>): Flow<List<UserEntity>>
}