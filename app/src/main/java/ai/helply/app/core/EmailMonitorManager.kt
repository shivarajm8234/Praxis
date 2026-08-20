package ai.helply.app.core

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for the WorkManager email polling job.
 *
 * - [startMonitoring]: Registers a 15-minute periodic poll (minimum Android interval)
 * - [stopMonitoring]: Cancels the recurring poll
 * - [triggerImmediatePoll]: Fires a one-shot poll right now (used by "Sync Now" button)
 */
@Singleton
class EmailMonitorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME_PERIODIC = "helply_email_poll_periodic"
        const val WORK_NAME_IMMEDIATE = "helply_email_poll_immediate"
    }

    fun startMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<EmailPollWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,  // Don't restart if already running
            periodicRequest
        )
        android.util.Log.d("EmailMonitorManager", "Periodic email poll started (15-min interval)")
    }

    fun stopMonitoring() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        android.util.Log.d("EmailMonitorManager", "Periodic email poll stopped")
    }

    fun triggerImmediatePoll() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<EmailPollWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_IMMEDIATE,
            ExistingWorkPolicy.REPLACE,  // Cancel previous immediate if still pending
            immediateRequest
        )
        android.util.Log.d("EmailMonitorManager", "Immediate email poll triggered")
    }

    fun isMonitoringActive(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME_PERIODIC)
            .get()
        return workInfos.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
    }
}
