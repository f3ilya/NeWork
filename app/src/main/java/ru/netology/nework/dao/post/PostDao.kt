package ru.netology.nework.dao.post

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.post.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity WHERE id = :id")
    fun getPostById(id: Long): Flow<PostEntity>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun pagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM PostEntity WHERE authorId = :authorId ORDER BY id DESC")
    fun pagingSourceByAuthorId(authorId: Long): PagingSource<Int, PostEntity>

    @Upsert
    suspend fun insert(postEntity: PostEntity)

    @Upsert
    suspend fun insert(postsEntity: List<PostEntity>)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removePost(id: Long)

    @Query("DELETE FROM PostEntity")
    suspend fun removeAll()

    @Query("DELETE FROM PostEntity WHERE authorId = :authorId")
    suspend fun removeByAuthorId(authorId: Long)
}