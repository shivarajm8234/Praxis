package ai.helply.app.core

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.helply.app.BuildConfig
import ai.helply.app.ai.CloudApiEngine
import ai.helply.app.data.db.EmailDao
import ai.helply.app.data.db.ExamDao
import ai.helply.app.data.entities.EmailEntity
import ai.helply.app.data.entities.ExamEntity
import ai.helply.app.data.remote.GmailApiClient
import ai.helply.app.data.remote.GmailTokenStore
import ai.helply.app.domain.EmailCategory
import ai.helply.app.domain.EmailScannerEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager background worker that polls Gmail every 15 minutes via OAuth.
 *
 * Pipeline per cycle:
 * 1. Check Gmail OAuth connection — abort if no refresh token
 * 2. Fetch new UNREAD messages since last poll via Gmail REST API
 * 3. Stage 1 heuristic pre-filter — skip obvious spam/promos
 * 4. Stage 2 AI classification — category, priority, 2-sentence summary
 * 5. For EXAM_CIRCULAR: Stage 2b exam date extraction
 * 6. Persist EmailEntity + ExamEntity to Room DB
 * 7. Schedule lockdown via [LockdownScheduler]
 * 8. Send priority-tiered notification via [NotificationHelper]
 * 9. Update last-poll timestamp in [GmailTokenStore]
 */
@HiltWorker
class EmailPollWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val gmailApiClient: GmailApiClient,
    private val tokenStore: GmailTokenStore,
    private val emailDao: EmailDao,
    private val examDao: ExamDao,
    private val lockdownScheduler: LockdownScheduler,
    private val notificationHelper: NotificationHelper,
    private val cloudApiEngine: CloudApiEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!tokenStore.isConnected()) {
            android.util.Log.d("EmailPollWorker", "No Gmail account connected — skipping poll")
            return Result.success()
        }

        val lastPoll = tokenStore.getLastPollMs()
        android.util.Log.d("EmailPollWorker", "Polling Gmail since ${java.util.Date(lastPoll)}")

        val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID

        val rawEmails = try {
            gmailApiClient.fetchNewEmails(lastPoll, clientId)
        } catch (e: Exception) {
            android.util.Log.e("EmailPollWorker", "Gmail fetch failed", e)
            return Result.retry()  // Retry with exponential backoff
        }

        android.util.Log.d("EmailPollWorker", "Fetched ${rawEmails.size} raw emails")

        for (raw in rawEmails) {
            try {
                // Dedup: skip emails already in the DB by message ID hash
                if (raw.imapUid != 0L && emailDao.findByImapUid(raw.imapUid) != null) {
                    continue
                }

                // Stage 1: Heuristic pre-filter (instant, no AI cost)
                if (!EmailScannerEngine.isAcademicCandidate(raw.subject, raw.sender, raw.body.take(200))) {
                    android.util.Log.d("EmailPollWorker", "Skipped (non-academic): ${raw.subject}")
                    continue
                }

                // Stage 2: AI Classification
                val result = EmailScannerEngine.classifyEmail(raw) { prompt, sys ->
                    cloudApiEngine.generateResponse(prompt, sys)
                }

                // Persist email to Room
                val entity = EmailEntity(
                    imapUid = raw.imapUid,
                    sender = "${raw.senderName} <${raw.sender}>",
                    subject = raw.subject,
                    snippet = raw.body.take(120).replace('\n', ' '),
                    fullBody = raw.body,
                    category = result.category.name,
                    priority = result.priority.name,
                    aiSummary = result.aiSummary,
                    receivedAt = raw.receivedAt,
                    isProcessed = true
                )
                val rowId = emailDao.insertEmail(entity)
                val emailId = if (rowId > 0) entity.id else continue

                // Stage 2b: Exam Date Extraction — only for EXAM_CIRCULAR
                if (result.category == EmailCategory.EXAM_CIRCULAR) {
                    android.util.Log.d("EmailPollWorker", "Exam circular detected: ${raw.subject}")

                    val dateResult = EmailScannerEngine.extractExamDetails(raw.body) { prompt, sys ->
                        cloudApiEngine.generateResponse(prompt, sys)
                    }

                    if (dateResult.examStartDateMillis != null) {
                        val lockdownStart = dateResult.examStartDateMillis - (5 * 86_400_000L)
                        val exam = ExamEntity(
                            subject = dateResult.examTitle ?: raw.subject,
                            examStartDate = dateResult.examStartDateMillis,
                            examEndDate = dateResult.examEndDateMillis ?: dateResult.examStartDateMillis,
                            lockdownStartDate = lockdownStart,
                            venue = dateResult.venue ?: "",
                            circularEmailId = emailId,
                            isLockActive = true
                        )
                        examDao.insertExam(exam)
                        lockdownScheduler.scheduleLockdown(exam)
                        android.util.Log.d("EmailPollWorker", "Exam saved + lockdown scheduled: ${exam.subject}")
                    }
                }

                // Send priority notification
                notificationHelper.sendEmailPriorityNotification(
                    category = result.category.displayName,
                    priority = result.priority,
                    subject = raw.subject,
                    summary = result.aiSummary,
                    sender = raw.sender,
                    isExamCircular = result.category == EmailCategory.EXAM_CIRCULAR
                )

                android.util.Log.d("EmailPollWorker",
                    "Processed: [${result.priority.name}] ${result.category.name} — ${raw.subject}")

            } catch (e: Exception) {
                // Per-email error isolation — don't fail the whole batch
                android.util.Log.e("EmailPollWorker", "Error processing email: ${raw.subject}", e)
            }
        }

        android.util.Log.d("EmailPollWorker", "Poll cycle complete. Next in ~15 min.")
        return Result.success()
    }
}
