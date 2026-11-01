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
    suspend fun insert(userEntity: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: List<UserEntity>)

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getUsersAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity WHERE id IN (:ids)")
    fun getUsersByIds(ids: List<Long>): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity WHERE id = :id")
    fun getUsersById(id: Long): Flow<UserEntity?>

    @Query("SELECT id FROM UserEntity WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<Long>): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM UserEntity WHERE id = :id)")
    suspend fun isUserExists(id: Long): Boolean
}