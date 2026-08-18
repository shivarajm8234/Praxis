package ai.helply.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "academic_memory")
data class AcademicMemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // Project, Certificate, Skill, Workshop, Exam, etc.
    val title: String,
    val description: String,
    val source: String, // Manual, GitHub, Email, Assignment
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val confidenceScore: Float = 0.95f,
    val verifiedStatus: Boolean = true,
    val tagsJson: String = "[]",
    val evidencePath: String? = null
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val title: String,
    val requirements: String,
    val deadline: Long,
    val priority: String, // High, Medium, Low
    val deliverablesJson: String = "[]",
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED
    val reportPath: String? = null
)

@Entity(tableName = "placement_companies")
data class PlacementCompanyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyName: String,
    val role: String,
    val jobDescription: String,
    val requiredSkillsJson: String = "[]",
    val estimatedAtsScore: Int = 85,
    val missingSkillsJson: String = "[]",
    val status: String = "APPLIED" // ANALYZED, APPLIED, INTERVIEWING
)

@Entity(tableName = "resume_versions")
data class ResumeVersionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val versionName: String,
    val targetCompany: String,
    val targetRole: String,
    val atsScore: Int,
    val contentMarkdown: String,
    val pdfPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio_projects")
data class PortfolioProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoryId: String,
    val title: String,
    val description: String,
    val repoUrl: String? = null,
    val demoUrl: String? = null,
    val imageUrlsJson: String = "[]",
    val isPublished: Boolean = true
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val examDate: Long,
    val venue: String,
    val priority: String = "CRITICAL",
    val focusModeScheduled: Boolean = true
)

@Entity(tableName = "emails")
data class EmailEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val subject: String,
    val snippet: String,
    val category: String, // EXAMINATION, ASSIGNMENT, PLACEMENT, etc.
    val priority: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = true
)
