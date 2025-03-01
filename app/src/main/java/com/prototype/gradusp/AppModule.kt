package com.prototype.gradusp

import android.content.Context
import dagger.Module
import dagger.Provides
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.repository.EventRepository
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideEventRepository(): EventRepository {
        return EventRepository()
    }
}