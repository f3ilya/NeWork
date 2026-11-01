package ru.netology.nework.dao.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.user.JobEntity

@Dao
interface JobDao {
    @Query("SELECT * FROM JobEntity WHERE ownerId = :ownerId")
    fun getJobsByOwnerId(ownerId: Long): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jobs: List<JobEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Query("DELETE FROM JobEntity WHERE ownerId = :ownerId")
    suspend fun removeJobsByOwnerId(ownerId: Long)

    @Query("DELETE FROM JobEntity WHERE id = :id")
    suspend fun removeJobById(id: Long)
}