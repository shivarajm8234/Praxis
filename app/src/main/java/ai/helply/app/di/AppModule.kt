package ai.helply.app.di

import android.content.Context
import ai.helply.app.ai.CloudApiEngine
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.ai.ModelDownloadManager
import ai.helply.app.ai.ModelRepository
import ai.helply.app.core.EmailMonitorManager
import ai.helply.app.core.LockdownScheduler
import ai.helply.app.core.NotificationHelper
import ai.helply.app.data.db.EmailDao
import ai.helply.app.data.db.ExamDao
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
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Room Database ────────────────────────────────────────────────────────

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

    @Provides @Singleton fun provideMemoryDao(db: HelplyDatabase): MemoryDao = db.memoryDao()
    @Provides @Singleton fun provideAcademicDao(db: HelplyDatabase): AcademicDao = db.academicDao()
    @Provides @Singleton fun providePlacementDao(db: HelplyDatabase): PlacementDao = db.placementDao()
    @Provides @Singleton fun provideEmailDao(db: HelplyDatabase): EmailDao = db.emailDao()
    @Provides @Singleton fun provideExamDao(db: HelplyDatabase): ExamDao = db.examDao()

    // ─── Networking ──────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // ─── Lockdown & Monitoring ────────────────────────────────────────────────

    @Provides @Singleton
    fun provideLockdownScheduler(
        @ApplicationContext context: Context,
        examDao: ExamDao
    ): LockdownScheduler = LockdownScheduler(context, examDao)

    @Provides @Singleton
    fun provideEmailMonitorManager(@ApplicationContext context: Context): EmailMonitorManager =
        EmailMonitorManager(context)

    @Provides @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    // ─── AI Infrastructure ────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideModelRepository(@ApplicationContext context: Context): ModelRepository =
        ModelRepository(context)

    @Provides @Singleton
    fun provideModelDownloadManager(
        @ApplicationContext context: Context,
        modelRepository: ModelRepository
    ): ModelDownloadManager = ModelDownloadManager(context, modelRepository)

    @Provides @Singleton
    fun provideGemmaEngineManager(
        @ApplicationContext context: Context,
        modelRepository: ModelRepository
    ): GemmaEngineManager = GemmaEngineManager(context, modelRepository)

    @Provides @Singleton
    fun provideCloudApiEngine(@ApplicationContext context: Context): CloudApiEngine =
        CloudApiEngine(context)

    @Provides @Singleton
    fun provideToolRegistry(
        db: HelplyDatabase,
        @ApplicationContext context: Context,
        cloudApiEngine: CloudApiEngine,
        gemmaEngine: GemmaEngineManager
    ): ToolRegistry = ToolRegistry(db, context, cloudApiEngine, gemmaEngine)
}
