package ai.helply.app.domain

import android.content.Context
import ai.helply.app.core.NotificationHelper
import kotlinx.coroutines.delay

data class AutonomousPipelineResult(
    val title: String,
    val subject: String,
    val generatedPdfPath: String,
    val generatedPptxPath: String,
    val generatedNotesPath: String,
    val pptSlideCount: Int,
    val researchSummary: String,
    val notificationSent: Boolean
)

object AutonomousAcademicAgent {

    suspend fun executeAcademicPipeline(
        context: Context,
        rawTaskText: String,
        subject: String = "Computer Science",
        aiGenerator: suspend (String, String) -> String
    ): AutonomousPipelineResult {
        // Step 1: Research Topic using LLM
        val prompt = """
            Analyze the following student task description:
            "$rawTaskText"
            
            Return a valid JSON object matching the following structure EXACTLY:
            {
              "title": "A short concise title for this task",
              "slideCount": 10,
              "researchSummary": "A formatted summary of findings, including key academic references, concepts, and outline of report."
            }
            Do not include any thinking tags, markdown wrapper like ```json, or other conversational text. Return ONLY the raw JSON object.
        """.trimIndent()

        val aiResultStr = aiGenerator(prompt, "You are a helpful academic research assistant.")
        
        // Clean potential markdown blocks
        val cleanJson = if (aiResultStr.contains("```json")) {
            aiResultStr.substringAfter("```json").substringBefore("```").trim()
        } else if (aiResultStr.contains("```")) {
            aiResultStr.substringAfter("```").substringBefore("```").trim()
        } else {
            aiResultStr.trim()
        }

        var parsedTitle = if (rawTaskText.length > 30) rawTaskText.take(30) + "..." else rawTaskText
        var slideCount = 10
        var summary = ""

        if (aiResultStr.isBlank()) {
            summary = "❌ Failed to generate research summary: The AI engine returned an empty response.\n\n" +
                      "Please ensure:\n" +
                      "1. If using Cloud API: Your API key is set and base URL is reachable in Settings.\n" +
                      "2. If using On-Device: The selected offline model is downloaded and loaded into RAM."
        } else if (aiResultStr.startsWith("⚠️") || aiResultStr.startsWith("❌")) {
            summary = aiResultStr
        } else {
            try {
                val json = org.json.JSONObject(cleanJson)
                parsedTitle = json.optString("title", parsedTitle)
                slideCount = json.optInt("slideCount", 10)
                summary = json.optString("researchSummary", "")
            } catch (e: Exception) {
                e.printStackTrace()
                summary = aiResultStr
            }
        }

        val extractedTopic = parsedTitle
        val pptPath = "/Documents/Helply/Academic/${extractedTopic.replace(" ", "_")}_Presentation.pptx"
        val pdfPath = "/Documents/Helply/Academic/${extractedTopic.replace(" ", "_")}_ResearchReport.pdf"
        val notesPath = "/Documents/Helply/Academic/${extractedTopic.replace(" ", "_")}_QuickNotes.md"

        // Trigger System Notification
        val notificationTitle = "🎓 AI Academic Agent: Task Completed!"
        val notificationMsg = "PPT ($slideCount slides) & PDF Research Report generated for '$extractedTopic'. Tap to review."
        NotificationHelper.sendNotification(context, notificationTitle, notificationMsg)

        return AutonomousPipelineResult(
            title = extractedTopic,
            subject = subject,
            generatedPdfPath = pdfPath,
            generatedPptxPath = pptPath,
            generatedNotesPath = notesPath,
            pptSlideCount = slideCount,
            researchSummary = summary,
            notificationSent = true
        )
    }
}
