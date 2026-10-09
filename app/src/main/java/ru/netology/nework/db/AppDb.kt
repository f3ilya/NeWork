package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dao.post.PostRemoteKeyDao
import ru.netology.nework.entity.Converters
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.post.PostRemoteKeyEntity

@Database(
    entities = [PostEntity::class, PostRemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
}