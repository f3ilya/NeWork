package ru.netology.nework.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.create
import ru.netology.nework.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApiServiceModule {
    @Provides
    @Singleton
    fun provideApiService(auth: AppAuth): ApiService {
        return retrofit(okhttp(loggingInterceptors(), authInterceptors(auth)))
            .create()
    }
}