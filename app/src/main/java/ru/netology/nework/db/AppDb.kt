package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nework.dao.post.PostDao
import ru.netology.nework.dao.post.PostRemoteKeyDao
import ru.netology.nework.dao.user.JobDao
import ru.netology.nework.dao.user.UserDao
import ru.netology.nework.dao.user.WallRemoteKeyDao
import ru.netology.nework.entity.Converters
import ru.netology.nework.entity.post.PostEntity
import ru.netology.nework.entity.post.PostRemoteKeyEntity
import ru.netology.nework.entity.user.JobEntity
import ru.netology.nework.entity.user.UserEntity
import ru.netology.nework.entity.user.WallRemoteKeyEntity

@Database(
    entities = [PostEntity::class, PostRemoteKeyEntity::class, UserEntity::class, JobEntity::class,
        WallRemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun postRemoteKeyDao(): PostRemoteKeyDao
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao
    abstract fun wallRemoteKeyDao(): WallRemoteKeyDao
}