package ai.helply.app.data.db

import androidx.room.*
import ai.helply.app.data.entities.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examStartDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    /** Returns the earliest active lock exam that hasn't ended yet. */
    @Query("SELECT * FROM exams WHERE isLockActive = 1 AND examEndDate >= :nowMs ORDER BY examStartDate ASC LIMIT 1")
    suspend fun getActiveLockExam(nowMs: Long): ExamEntity?

    @Query("UPDATE exams SET isLockActive = 0 WHERE id = :id")
    suspend fun deactivateLock(id: String)

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?
}
