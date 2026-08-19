package ai.helply.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class with Hilt + WorkManager configuration.
 *
 * Implementing [Configuration.Provider] here ensures WorkManager uses the
 * [HiltWorkerFactory] so @HiltWorker + @AssistedInject is resolved correctly
 * in [ai.helply.app.core.EmailPollWorker].
 */
@HiltAndroidApp
class HelplyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
