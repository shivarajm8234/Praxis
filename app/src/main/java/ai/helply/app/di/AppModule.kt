package ai.helply.app.di

import android.content.Context
import ai.helply.app.ai.CloudApiEngine
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.ai.ModelDownloadManager
import ai.helply.app.ai.ModelRepository
import ai.helply.app.data.db.HelplyDatabase
import ai.helply.app.data.db.MemoryDao
import ai.helply.app.data.db.AcademicDao
import ai.helply.app.data.db.PlacementDao
import ai.helply.app.tools.ToolRegistry
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHelplyDatabase(@ApplicationContext context: Context): HelplyDatabase {
        return Room.databaseBuilder(
            context,
            HelplyDatabase::class.java,
            "helply_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(db: HelplyDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideAcademicDao(db: HelplyDatabase): AcademicDao = db.academicDao()

    @Provides
    @Singleton
    fun providePlacementDao(db: HelplyDatabase): PlacementDao = db.placementDao()

    @Provides
    @Singleton
    fun provideModelRepository(@ApplicationContext context: Context): ModelRepository {
        return ModelRepository(context)
    }

    @Provides
    @Singleton
    fun provideModelDownloadManager(
        @ApplicationContext context: Context,
        modelRepository: ModelRepository
    ): ModelDownloadManager {
        return ModelDownloadManager(context, modelRepository)
    }

    @Provides
    @Singleton
    fun provideGemmaEngineManager(
        @ApplicationContext context: Context,
        modelRepository: ModelRepository
    ): GemmaEngineManager {
        return GemmaEngineManager(context, modelRepository)
    }

    @Provides
    @Singleton
    fun provideCloudApiEngine(@ApplicationContext context: Context): CloudApiEngine {
        return CloudApiEngine(context)
    }

    @Provides
    @Singleton
    fun provideToolRegistry(
        db: HelplyDatabase,
        @ApplicationContext context: Context,
        cloudApiEngine: CloudApiEngine,
        gemmaEngine: GemmaEngineManager
    ): ToolRegistry {
        return ToolRegistry(db, context, cloudApiEngine, gemmaEngine)
    }
}

