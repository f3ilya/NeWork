@file:Suppress("unused")

package ru.netology.nework.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.repository.auth.AuthRepository
import ru.netology.nework.repository.auth.AuthRepositoryImpl
import ru.netology.nework.repository.event.EventRepository
import ru.netology.nework.repository.event.EventRepositoryImpl
import ru.netology.nework.repository.post.PostRepository
import ru.netology.nework.repository.post.PostRepositoryImpl
import ru.netology.nework.repository.user.UserRepository
import ru.netology.nework.repository.user.UserRepositoryImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}