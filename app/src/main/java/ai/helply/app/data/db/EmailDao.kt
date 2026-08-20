package ai.helply.app.data.db

import androidx.room.*
import ai.helply.app.data.entities.EmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY receivedAt DESC LIMIT 200")
    fun getAllEmails(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE category = 'EXAM_CIRCULAR' ORDER BY receivedAt DESC")
    fun getExamCirculars(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE priority = 'CRITICAL_RED' OR priority = 'HIGH_ORANGE' ORDER BY receivedAt DESC LIMIT 50")
    fun getHighPriorityEmails(): Flow<List<EmailEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmail(email: EmailEntity): Long

    /** Checks IMAP UID to prevent inserting duplicate messages. */
    @Query("SELECT * FROM emails WHERE imapUid = :uid LIMIT 1")
    suspend fun findByImapUid(uid: Long): EmailEntity?

    @Query("UPDATE emails SET isProcessed = 1, category = :cat, priority = :pri, aiSummary = :summary, detectedExamStartDate = :startMs, detectedExamEndDate = :endMs WHERE id = :id")
    suspend fun updateClassification(
        id: String,
        cat: String,
        pri: String,
        summary: String,
        startMs: Long?,
        endMs: Long?
    )

    /** Prunes emails older than 30 days to keep the DB lean. */
    @Query("DELETE FROM emails WHERE receivedAt < :cutoffMs")
    suspend fun deleteOldEmails(cutoffMs: Long)
}
