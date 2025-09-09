package com.prototype.gradusp

import android.content.Context
import com.prototype.gradusp.data.parser.DataTransformer
import com.prototype.gradusp.data.parser.EnhancedUspParser
import com.prototype.gradusp.data.parser.SearchService
import com.prototype.gradusp.utils.OfflineSupport
import dagger.Module
import dagger.Provides
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.repository.EventRepository
import com.prototype.gradusp.data.repository.GradesRepository
import com.prototype.gradusp.data.repository.UspDataRepository
import com.prototype.gradusp.data.repository.EnhancedUspDataRepository
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

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
    fun provideEnhancedUspParser(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): EnhancedUspParser {
        return EnhancedUspParser(context, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideDataTransformer(): DataTransformer {
        return DataTransformer()
    }

    @Provides
    @Singleton
    fun provideSearchService(
        enhancedUspParser: EnhancedUspParser,
        dataTransformer: DataTransformer
    ): SearchService {
        return SearchService(enhancedUspParser, dataTransformer)
    }

    @Provides
    @Singleton
    fun provideEnhancedUspDataRepository(
        @ApplicationContext context: Context,
        enhancedUspParser: EnhancedUspParser,
        searchService: SearchService,
        userPreferencesRepository: UserPreferencesRepository
    ): EnhancedUspDataRepository {
        return EnhancedUspDataRepository(context, enhancedUspParser, searchService, userPreferencesRepository)
    }

    @Provides
    @Singleton
    fun provideOfflineSupport(
        @ApplicationContext context: Context
    ): OfflineSupport {
        return OfflineSupport(context)
    }

    @Provides
    @Singleton
    fun provideGradesRepository(
        @ApplicationContext context: Context
    ): GradesRepository {
        return GradesRepository(context)
    }
}