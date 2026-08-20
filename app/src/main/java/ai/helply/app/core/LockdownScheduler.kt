package ai.helply.app.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ai.helply.app.data.db.ExamDao
import ai.helply.app.data.entities.ExamEntity
import ai.helply.app.domain.ExamLockState
import ai.helply.app.domain.SocialMediaLockManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and manages the exam lockdown lifecycle using [AlarmManager].
 *
 * Lock state is persisted in [EncryptedSharedPreferences] so it survives:
 * - App process kill
 * - Device reboot (restored via [LockdownBroadcastReceiver])
 *
 * Two alarms are registered per exam:
 * 1. LOCK_START: Fires at (examStartDate - 5 days) → activates lockdown
 * 2. LOCK_END:   Fires at (examEndDate + 1 day at midnight) → releases lockdown
 */
@Singleton
class LockdownScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val examDao: ExamDao
) {
    companion object {
        const val ACTION_LOCK_START = "ai.helply.LOCK_START"
        const val ACTION_LOCK_END   = "ai.helply.LOCK_END"
        const val EXTRA_EXAM_ID     = "exam_id"
        const val EXTRA_EXAM_TITLE  = "exam_title"

        private const val KEY_IS_LOCK_ACTIVE    = "lock_is_active"
        private const val KEY_EXAM_ID           = "lock_exam_id"
        private const val KEY_EXAM_TITLE        = "lock_exam_title"
        private const val KEY_EXAM_START_MS     = "lock_exam_start_ms"
        private const val KEY_EXAM_END_MS       = "lock_exam_end_ms"
        private const val KEY_LOCKDOWN_START_MS = "lock_lockdown_start_ms"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "helply_lockdown_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    // ─── Schedule ────────────────────────────────────────────────────────────

    /**
     * Schedules LOCK_START and LOCK_END alarms for the given exam.
     * If the lockdown start time is already in the past, activates the lock immediately.
     */
    fun scheduleLockdown(exam: ExamEntity) {
        persistLockState(
            examId       = exam.id,
            examTitle    = exam.subject,
            examStartMs  = exam.examStartDate,
            examEndMs    = exam.examEndDate,
            lockStartMs  = exam.lockdownStartDate,
            isActive     = false  // will flip to true when alarm fires or immediately below
        )

        val now = System.currentTimeMillis()

        // Schedule LOCK_START alarm
        if (exam.lockdownStartDate > now) {
            scheduleAlarm(ACTION_LOCK_START, exam.lockdownStartDate, exam.id, exam.subject)
            android.util.Log.d("LockdownScheduler", "Lock START scheduled for: ${formatDate(exam.lockdownStartDate)}")
        } else {
            // Lock start already passed — activate immediately
            android.util.Log.d("LockdownScheduler", "Lock start in past — activating immediately")
            activateLockNow(exam.id, exam.subject, exam.examStartDate, exam.examEndDate)
        }

        // Schedule LOCK_END alarm: examEndDate + 1 day at midnight
        val endMs = exam.examEndDate + 86_400_000L
        scheduleAlarm(ACTION_LOCK_END, endMs, exam.id, exam.subject)
        android.util.Log.d("LockdownScheduler", "Lock END scheduled for: ${formatDate(endMs)}")
    }

    fun activateLockNow(examId: String, examTitle: String, examStartMs: Long, examEndMs: Long) {
        prefs.edit().putBoolean(KEY_IS_LOCK_ACTIVE, true).apply()
        notificationHelper?.sendExamLockActivatedNotification(
            examTitle  = examTitle,
            startDate  = formatDate(examStartMs),
            endDate    = formatDate(examEndMs)
        )
    }

    suspend fun releaseLockdown(examId: String) {
        prefs.edit().putBoolean(KEY_IS_LOCK_ACTIVE, false).apply()
        withContext(Dispatchers.IO) {
            examDao.deactivateLock(examId)
        }
        val title = prefs.getString(KEY_EXAM_TITLE, "Examinations") ?: "Examinations"
        notificationHelper?.sendExamLockLiftedNotification(title)
        android.util.Log.d("LockdownScheduler", "Lockdown released for exam: $examId")
    }

    // ─── State Restoration ────────────────────────────────────────────────────

    /**
     * Called on app start and after device reboot to restore persisted lock state.
     * Returns the current [ExamLockState] regardless of whether lock is active.
     */
    suspend fun restoreLockStateIfActive(): ExamLockState {
        val isActive = prefs.getBoolean(KEY_IS_LOCK_ACTIVE, false)
        val examTitle = prefs.getString(KEY_EXAM_TITLE, "") ?: ""
        val examStartMs = prefs.getLong(KEY_EXAM_START_MS, 0L)
        val examEndMs = prefs.getLong(KEY_EXAM_END_MS, 0L)

        if (!isActive) return ExamLockState()

        // Auto-release if exam has ended
        val now = System.currentTimeMillis()
        if (examEndMs > 0 && now > examEndMs + 86_400_000L) {
            val examId = prefs.getString(KEY_EXAM_ID, "") ?: ""
            releaseLockdown(examId)
            return ExamLockState()
        }

        val daysRemaining = if (examStartMs > now) {
            ((examStartMs - now) / 86_400_000L).toInt().coerceAtLeast(0)
        } else 0

        // Rebuild locked apps list from device
        val lockedApps = SocialMediaLockManager.getDefaultLockedApps()

        return ExamLockState(
            isLockActive   = true,
            examTitle      = examTitle,
            examDateMillis = examStartMs,
            examEndMillis  = examEndMs,
            daysRemaining  = daysRemaining,
            lockedApps     = lockedApps
        )
    }

    fun getPersistedLockState(): ExamLockState {
        val isActive = prefs.getBoolean(KEY_IS_LOCK_ACTIVE, false)
        if (!isActive) return ExamLockState()
        return ExamLockState(
            isLockActive   = true,
            examTitle      = prefs.getString(KEY_EXAM_TITLE, "") ?: "",
            examDateMillis = prefs.getLong(KEY_EXAM_START_MS, 0L),
            examEndMillis  = prefs.getLong(KEY_EXAM_END_MS, 0L),
            daysRemaining  = 0,
            lockedApps     = SocialMediaLockManager.getDefaultLockedApps()
        )
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private fun scheduleAlarm(action: String, triggerAtMs: Long, examId: String, examTitle: String) {
        val intent = Intent(context, LockdownBroadcastReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_EXAM_ID, examId)
            putExtra(EXTRA_EXAM_TITLE, examTitle)
        }
        val requestCode = (examId + action).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setExactAndAllowWhileIdle works even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    private fun persistLockState(
        examId: String, examTitle: String,
        examStartMs: Long, examEndMs: Long,
        lockStartMs: Long, isActive: Boolean
    ) {
        prefs.edit()
            .putString(KEY_EXAM_ID, examId)
            .putString(KEY_EXAM_TITLE, examTitle)
            .putLong(KEY_EXAM_START_MS, examStartMs)
            .putLong(KEY_EXAM_END_MS, examEndMs)
            .putLong(KEY_LOCKDOWN_START_MS, lockStartMs)
            .putBoolean(KEY_IS_LOCK_ACTIVE, isActive)
            .apply()
    }

    private fun formatDate(ms: Long): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(ms))

    // Injected lazily to avoid circular DI dependency
    private var notificationHelper: NotificationHelper? = null
    fun setNotificationHelper(helper: NotificationHelper) { notificationHelper = helper }
}
