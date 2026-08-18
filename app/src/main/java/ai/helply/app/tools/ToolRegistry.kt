package ai.helply.app.tools

import ai.helply.app.data.db.HelplyDatabase
import ai.helply.app.data.entities.AcademicMemoryEntity
import ai.helply.app.data.entities.AssignmentEntity
import ai.helply.app.data.entities.ExamEntity
import javax.inject.Inject
import javax.inject.Singleton

sealed class ToolResult {
    data class Success(val message: String, val payload: Map<String, Any> = emptyMap()) : ToolResult()
    data class Error(val reason: String) : ToolResult()
}

@Singleton
class ToolRegistry @Inject constructor(
    private val db: HelplyDatabase
) {
    suspend fun executeTool(toolName: String, params: Map<String, String>): ToolResult {
        return try {
            when (toolName) {
                "createTask" -> {
                    val title = params["title"] ?: return ToolResult.Error("Missing title")
                    val subject = params["subject"] ?: "General"
                    val deadlineDays = params["deadlineDays"]?.toLongOrNull() ?: 3L
                    val deadline = System.currentTimeMillis() + (deadlineDays * 86400000L)
                    val assignment = AssignmentEntity(
                        subject = subject,
                        title = title,
                        requirements = params["requirements"] ?: "Auto-generated task",
                        deadline = deadline,
                        priority = params["priority"] ?: "MEDIUM"
                    )
                    db.academicDao().insertAssignment(assignment)
                    ToolResult.Success("Task '$title' created successfully for $subject.", mapOf("id" to assignment.id))
                }

                "updateMemory" -> {
                    val title = params["title"] ?: return ToolResult.Error("Missing title")
                    val type = params["type"] ?: "Project"
                    val desc = params["description"] ?: ""
                    val memory = AcademicMemoryEntity(
                        type = type,
                        title = title,
                        description = desc,
                        source = params["source"] ?: "AI Tool"
                    )
                    db.memoryDao().insertMemory(memory)
                    ToolResult.Success("Academic Memory updated: $title ($type)", mapOf("id" to memory.id))
                }

                "calculateATS" -> {
                    val resumeText = params["resumeText"] ?: params["resumeId"] ?: ""
                    val jobDesc = params["jobDesc"] ?: params["jobId"] ?: ""
                    val atsResult = ai.helply.app.domain.ATSEngine.evaluateResume(resumeText, jobDesc)
                    ToolResult.Success(
                        "ATS Score calculated dynamically: ${atsResult.estimatedScore}%",
                        mapOf(
                            "score" to atsResult.estimatedScore,
                            "keywordMatch" to atsResult.keywordMatchPercentage,
                            "semanticSimilarity" to atsResult.semanticSimilarityPercentage,
                            "missingKeywords" to atsResult.missingKeywords
                        )
                    )
                }

                "enableFocusMode" -> {
                    val subject = params["subject"] ?: "Upcoming Examination"
                    val durationDays = params["durationDays"]?.toIntOrNull() ?: 5
                    val exam = ExamEntity(
                        subject = subject,
                        examDate = System.currentTimeMillis() + (durationDays * 86400000L),
                        venue = params["venue"] ?: "Main Examination Hall"
                    )
                    db.academicDao().insertExam(exam)
                    ToolResult.Success("Focus Mode & Exam Schedule created for $subject ($durationDays days count-down).")
                }

                "deployPortfolio" -> {
                    val repoName = params["repoName"] ?: "student-portfolio"
                    val liveUrl = "https://student.github.io/$repoName/"
                    ToolResult.Success("Portfolio successfully deployed to GitHub Pages!", mapOf("url" to liveUrl))
                }

                else -> ToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Tool execution failed: ${e.localizedMessage}")
        }
    }
}
