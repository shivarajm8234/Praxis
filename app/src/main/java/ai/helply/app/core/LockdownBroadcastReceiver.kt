package ai.helply.app.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives system broadcasts for:
 * - [Intent.ACTION_BOOT_COMPLETED]: Restores active lock state after device reboot
 * - [LockdownScheduler.ACTION_LOCK_START]: Activates the exam lockdown
 * - [LockdownScheduler.ACTION_LOCK_END]:   Releases the exam lockdown
 */
@AndroidEntryPoint
class LockdownBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var lockdownScheduler: LockdownScheduler
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var emailMonitorManager: EmailMonitorManager

    override fun onReceive(context: Context, intent: Intent) {
        lockdownScheduler.setNotificationHelper(notificationHelper)

        val examId    = intent.getStringExtra(LockdownScheduler.EXTRA_EXAM_ID) ?: ""
        val examTitle = intent.getStringExtra(LockdownScheduler.EXTRA_EXAM_TITLE) ?: "Examinations"

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                android.util.Log.d("LockdownReceiver", "Boot completed — restoring lock state")
                CoroutineScope(Dispatchers.IO).launch {
                    val lockState = lockdownScheduler.restoreLockStateIfActive()
                    if (lockState.isLockActive) {
                        android.util.Log.d("LockdownReceiver", "Lock restored: ${lockState.examTitle}")
                        notificationHelper.sendExamLockActivatedNotification(
                            examTitle = lockState.examTitle,
                            startDate = formatMs(lockState.examDateMillis),
                            endDate   = formatMs(lockState.examEndMillis)
                        )
                    }
                    // Restart email monitoring after reboot if account is connected
                    emailMonitorManager.startMonitoring()
                }
            }

            LockdownScheduler.ACTION_LOCK_START -> {
                android.util.Log.d("LockdownReceiver", "LOCK_START alarm fired for: $examTitle")
                CoroutineScope(Dispatchers.IO).launch {
                    val exam = lockdownScheduler.restoreLockStateIfActive()
                    lockdownScheduler.activateLockNow(
                        examId    = examId,
                        examTitle = examTitle,
                        examStartMs = exam.examDateMillis,
                        examEndMs   = exam.examEndMillis
                    )
                }
            }

            LockdownScheduler.ACTION_LOCK_END -> {
                android.util.Log.d("LockdownReceiver", "LOCK_END alarm fired for: $examTitle")
                CoroutineScope(Dispatchers.IO).launch {
                    lockdownScheduler.releaseLockdown(examId)
                }
            }
        }
    }

    private fun formatMs(ms: Long): String {
        if (ms == 0L) return "N/A"
        return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(ms))
    }
}
