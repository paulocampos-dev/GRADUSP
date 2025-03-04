package com.prototype.gradusp

import android.content.Context
import dagger.Module
import dagger.Provides
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.repository.EventRepository
import com.prototype.gradusp.data.repository.GradesRepository
import com.prototype.gradusp.data.repository.UspDataRepository
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
    fun provideEventRepository(
        @ApplicationContext context: Context
    ): EventRepository {
        return EventRepository(context)
    }

    @Provides
    @Singleton
    fun provideUspDataRepository(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository
    ): UspDataRepository {
        return UspDataRepository(context, userPreferencesRepository)
    }

    @Provides
    @Singleton
    fun provideGradesRepository(
        @ApplicationContext context: Context
    ): GradesRepository {
        return GradesRepository(context)
    }
}