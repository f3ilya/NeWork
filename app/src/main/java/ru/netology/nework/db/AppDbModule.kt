package ru.netology.nework.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppDbModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDb {
        return Room.databaseBuilder(context, AppDb::class.java, "app.db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
}