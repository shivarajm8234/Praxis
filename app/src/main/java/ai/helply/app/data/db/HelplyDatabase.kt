package ai.helply.app.data.db

import androidx.room.*
import ai.helply.app.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM academic_memory ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<AcademicMemoryEntity>>

    @Query("SELECT * FROM academic_memory WHERE type = :type ORDER BY createdAt DESC")
    fun getMemoriesByType(type: String): Flow<List<AcademicMemoryEntity>>

    @Query("SELECT * FROM academic_memory WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<AcademicMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AcademicMemoryEntity)

    @Query("DELETE FROM academic_memory WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM academic_memory")
    suspend fun deleteAllMemories()
}

@Dao
interface AcademicDao {
    @Query("SELECT * FROM assignments ORDER BY deadline ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)
}

@Dao
interface PlacementDao {
    @Query("SELECT * FROM placement_companies ORDER BY companyName ASC")
    fun getAllCompanies(): Flow<List<PlacementCompanyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: PlacementCompanyEntity)

    @Query("SELECT * FROM resume_versions ORDER BY createdAt DESC")
    fun getAllResumeVersions(): Flow<List<ResumeVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResumeVersion(version: ResumeVersionEntity)
}

@Database(
    entities = [
        AcademicMemoryEntity::class,
        AssignmentEntity::class,
        PlacementCompanyEntity::class,
        ResumeVersionEntity::class,
        PortfolioProjectEntity::class,
        ExamEntity::class,
        EmailEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HelplyDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun academicDao(): AcademicDao
    abstract fun placementDao(): PlacementDao
}
