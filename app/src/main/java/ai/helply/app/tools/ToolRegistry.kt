package ai.helply.app.tools

import ai.helply.app.data.db.HelplyDatabase
import ai.helply.app.data.entities.AcademicMemoryEntity
import ai.helply.app.data.entities.AssignmentEntity
import ai.helply.app.data.entities.ExamEntity
import android.content.Context
import ai.helply.app.domain.GitHubAppManager
import ai.helply.app.ai.CloudApiEngine
import ai.helply.app.ai.GemmaEngineManager
import javax.inject.Inject
import javax.inject.Singleton

sealed class ToolResult {
    data class Success(val message: String, val payload: Map<String, Any> = emptyMap()) : ToolResult()
    data class Error(val reason: String) : ToolResult()
}

@Singleton
class ToolRegistry @Inject constructor(
    private val db: HelplyDatabase,
    private val context: Context,
    private val cloudApiEngine: CloudApiEngine,
    private val gemmaEngine: GemmaEngineManager
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
                    val atsResult = ai.helply.app.domain.ATSEngine.evaluateResume(resumeText, jobDesc) { p, s ->
                        generateAICompletion(p, s)
                    }
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

                "githubCreateRepo" -> {
                    val repoName = params["repoName"] ?: return ToolResult.Error("Missing repoName")
                    val desc = params["description"] ?: "Created via Helply OS Agent"
                    val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
                    val token = prefs.getString("github_access_token", null)
                    if (token.isNullOrBlank()) {
                        return ToolResult.Error("GitHub account not connected. Please connect in Profile tab.")
                    }
                    val success = GitHubAppManager.createRepository(token, repoName, desc)
                    if (success) {
                        ToolResult.Success("GitHub Repository '$repoName' created successfully!")
                    } else {
                        ToolResult.Error("Failed to create repository. Check your connection/permissions.")
                    }
                }

                "githubWriteFile" -> {
                    val repoName = params["repoName"] ?: return ToolResult.Error("Missing repoName")
                    val path = params["path"] ?: return ToolResult.Error("Missing path")
                    val content = params["content"] ?: return ToolResult.Error("Missing content")
                    val commitMsg = params["commitMessage"] ?: "Updated file via Helply OS Agent"
                    
                    val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
                    val token = prefs.getString("github_access_token", null)
                    val owner = prefs.getString("github_username", null)
                    
                    if (token.isNullOrBlank() || owner.isNullOrBlank()) {
                        return ToolResult.Error("GitHub account not connected. Please connect in Profile tab.")
                    }
                    val success = GitHubAppManager.createOrUpdateFile(token, owner, repoName, path, content, commitMsg)
                    if (success) {
                        ToolResult.Success("Successfully committed changes to '$path' in repository '$repoName'.")
                    } else {
                        ToolResult.Error("Failed to write/update file in '$repoName'.")
                    }
                }

                else -> ToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Tool execution failed: ${e.localizedMessage}")
        }
    }

    private suspend fun generateAICompletion(prompt: String, systemPrompt: String): String {
        val prefs = context.getSharedPreferences("helply_cloud_api", Context.MODE_PRIVATE)
        val modeStr = prefs.getString("inference_mode", "ON_DEVICE")
        val isCloud = modeStr == "CLOUD_API"

        return if (isCloud) {
            cloudApiEngine.generateResponse(prompt, systemPrompt)
        } else {
            val selectedModelId = prefs.getString("selected_chat_model_id", ai.helply.app.ai.ModelRegistry.GEMMA_4B_IT.id) ?: ai.helply.app.ai.ModelRegistry.GEMMA_4B_IT.id
            gemmaEngine.generateResponse(prompt, systemPrompt, selectedModelId)
        }
    }
}
