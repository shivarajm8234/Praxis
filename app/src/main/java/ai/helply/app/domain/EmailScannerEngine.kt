package ai.helply.app.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CollegeEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val fullBody: String,
    val timestamp: String,
    val category: EmailCategory,
    val priority: PriorityLevel,
    val detectedExamDate: String? = null,
    val examDateMillis: Long? = null,
    val isProcessed: Boolean = false
)

enum class EmailCategory {
    EXAM_CIRCULAR,
    ASSIGNMENT_DUE,
    PLACEMENT_DRIVE,
    GENERAL_ANNOUNCEMENT
}

enum class PriorityLevel {
    CRITICAL_RED,
    HIGH_ORANGE,
    MEDIUM_YELLOW,
    LOW_GREEN
}

data class ScanResult(
    val processedCount: Int,
    val examCircularsFound: Int,
    val activeLockdownTriggered: Boolean,
    val examTitle: String?,
    val examDate: String?,
    val daysUntilExam: Int
)

object EmailScannerEngine {

    fun sampleCollegeEmails(): List<CollegeEmail> = emptyList()

    suspend fun analyzeEmails(
        emails: List<CollegeEmail>,
        aiGenerator: suspend (String, String) -> String
    ): Pair<List<CollegeEmail>, ScanResult> {
        if (emails.isEmpty()) {
            return Pair(emptyList(), ScanResult(0, 0, false, null, null, 0))
        }

        val emailsJson = org.json.JSONArray().apply {
            emails.forEach { email ->
                put(org.json.JSONObject().apply {
                    put("id", email.id)
                    put("sender", email.sender)
                    put("subject", email.subject)
                    put("snippet", email.snippet)
                })
            }
        }

        val prompt = """
            Analyze these college emails and classify them.
            Emails JSON:
            $emailsJson

            Classify each email into: EXAM_CIRCULAR, ASSIGNMENT_DUE, PLACEMENT_DRIVE, GENERAL_ANNOUNCEMENT.
            Determine priority: CRITICAL_RED, HIGH_ORANGE, MEDIUM_YELLOW, LOW_GREEN.
            If an email is an EXAM_CIRCULAR, detect the exam title and date (if mentioned, format e.g., "Aug 25, 2026").

            Return a valid JSON object matching this structure EXACTLY:
            {
              "emailClassifications": [
                {
                  "id": "email_id",
                  "category": "EXAM_CIRCULAR",
                  "priority": "CRITICAL_RED",
                  "detectedExamDate": "exam_date_or_null"
                }
              ],
              "lockdownTriggered": true,
              "examTitle": "Exam Title",
              "examDate": "Exam Date",
              "daysUntilExam": 5
            }
            Do not include any markdown wrappers or thinking tags. Output raw JSON only.
        """.trimIndent()

        val aiResultStr = aiGenerator(prompt, "You are a helpful college communications intelligence agent.")

        val cleanJson = if (aiResultStr.contains("```json")) {
            aiResultStr.substringAfter("```json").substringBefore("```").trim()
        } else if (aiResultStr.contains("```")) {
            aiResultStr.substringAfter("```").substringBefore("```").trim()
        } else {
            aiResultStr.trim()
        }

        var examFound = false
        var examTitle: String? = null
        var examDateStr: String? = null
        var daysCount = 5
        val classificationsMap = mutableMapOf<String, Triple<EmailCategory, PriorityLevel, String?>>()

        try {
            val json = org.json.JSONObject(cleanJson)
            examFound = json.optBoolean("lockdownTriggered", false)
            examTitle = json.optString("examTitle", null)
            examDateStr = json.optString("examDate", null)
            daysCount = json.optInt("daysUntilExam", 5)

            val array = json.optJSONArray("emailClassifications")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val catStr = obj.optString("category", "GENERAL_ANNOUNCEMENT")
                    val priStr = obj.optString("priority", "LOW_GREEN")
                    val examDate = obj.optString("detectedExamDate", null)

                    val category = try { EmailCategory.valueOf(catStr) } catch(_: Exception) { EmailCategory.GENERAL_ANNOUNCEMENT }
                    val priority = try { PriorityLevel.valueOf(priStr) } catch(_: Exception) { PriorityLevel.LOW_GREEN }
                    classificationsMap[id] = Triple(category, priority, examDate)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val updatedEmails = emails.map { email ->
            val classification = classificationsMap[email.id]
            if (classification != null) {
                val dateMillis = classification.third?.let {
                    try {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        sdf.parse(it)?.time
                    } catch(_: Exception) {
                        System.currentTimeMillis() + (daysCount * 86400000L)
                    }
                }
                email.copy(
                    category = classification.first,
                    priority = classification.second,
                    detectedExamDate = classification.third,
                    examDateMillis = dateMillis
                )
            } else {
                email
            }
        }

        val examCircularsCount = updatedEmails.count { it.category == EmailCategory.EXAM_CIRCULAR }

        return Pair(
            updatedEmails,
            ScanResult(
                processedCount = emails.size,
                examCircularsFound = examCircularsCount,
                activeLockdownTriggered = examFound,
                examTitle = examTitle,
                examDate = examDateStr,
                daysUntilExam = daysCount
            )
        )
    }
}
