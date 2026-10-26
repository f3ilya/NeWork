package ru.netology.nework.dao.user

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.db.AppDb

@InstallIn(SingletonComponent::class)
@Module
object UserDaoModule {
    @Provides
    fun provideUserDao(db: AppDb): UserDao = db.userDao()
}