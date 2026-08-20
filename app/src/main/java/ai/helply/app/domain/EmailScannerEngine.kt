package ai.helply.app.domain

import ai.helply.app.data.remote.RawEmail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Domain Models ────────────────────────────────────────────────────────────

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

enum class EmailCategory(val displayName: String) {
    EXAM_CIRCULAR("Exam Circular"),
    ASSIGNMENT_DUE("Assignment Due"),
    PLACEMENT_DRIVE("Placement Drive"),
    FEE_NOTICE("Fee Notice"),
    EVENT_INVITE("Event / Workshop"),
    GENERAL_ANNOUNCEMENT("General Announcement")
}

enum class PriorityLevel(val displayName: String) {
    CRITICAL_RED("Critical"),
    HIGH_ORANGE("High"),
    MEDIUM_YELLOW("Medium"),
    LOW_GREEN("Low")
}

data class ScanResult(
    val processedCount: Int,
    val examCircularsFound: Int,
    val activeLockdownTriggered: Boolean,
    val examTitle: String?,
    val examDate: String?,
    val daysUntilExam: Int
)

data class EmailClassificationResult(
    val emailId: String,
    val category: EmailCategory,
    val priority: PriorityLevel,
    val aiSummary: String,
    val examTitle: String? = null,
    val examStartDateMillis: Long? = null,
    val examEndDateMillis: Long? = null,
    val examSubjects: List<ExamSubject> = emptyList()
)

data class ExamSubject(
    val subject: String,
    val paperDateMillis: Long,
    val paperTime: String? = null
)

data class ExamDateExtractionResult(
    val examTitle: String?,
    val examStartDateMillis: Long?,
    val examEndDateMillis: Long?,
    val venue: String?,
    val subjects: List<ExamSubject>
)

// ─── Email Scanner Engine ─────────────────────────────────────────────────────

object EmailScannerEngine {

    // ─── Stage 1: Fast Heuristic Pre-filter (no AI) ──────────────────────────

    private val ACADEMIC_KEYWORDS = listOf(
        "exam", "circular", "examination", "timetable", "schedule", "hall ticket",
        "submission", "assignment", "placement", "internship", "announcement",
        "result", "attendance", "marks", "internal", "university", "semester",
        "college", "department", "academic", "syllabus", "notice", "re-exam",
        "retest", "practical", "viva", "project", "report", "deadline",
        "backlog", "arrear", "supplementary", "fee", "scholarship", "hostel"
    )

    private val SPAM_REJECT = listOf(
        "sale", "discount", "offer", "promo", "unsubscribe", "buy now",
        "limited time", "congratulations you won", "click here to claim",
        "free gift", "earn money", "work from home", "casino", "lottery",
        "weight loss", "make money fast", "risk free", "act now"
    )

    /**
     * Fast heuristic check — no AI needed. Returns true if the email is
     * a plausible academic candidate worth sending to AI for classification.
     */
    fun isAcademicCandidate(subject: String, sender: String, snippet: String): Boolean {
        val text = "$subject $sender $snippet".lowercase()
        val hasHardSpam = SPAM_REJECT.count { text.contains(it) } >= 2
        if (hasHardSpam) return false
        val hasAcademic = ACADEMIC_KEYWORDS.any { text.contains(it) }
        // Also accept if sender looks institutional (contains edu, ac.in, college)
        val isInstitutionalSender = sender.lowercase().let {
            it.contains(".edu") || it.contains("ac.in") || it.contains("college") ||
            it.contains("university") || it.contains("institute") || it.contains("dept")
        }
        return hasAcademic || isInstitutionalSender
    }

    // ─── Stage 2: AI Classification ──────────────────────────────────────────

    /**
     * Classifies a single raw email using the AI engine.
     * Returns [EmailClassificationResult] with category, priority, and AI summary.
     */
    suspend fun classifyEmail(
        raw: RawEmail,
        aiGenerator: suspend (prompt: String, system: String) -> String
    ): EmailClassificationResult {
        val bodyExcerpt = raw.body.take(800).trim()
        val prompt = """
            You are a college academic communications classifier for Indian engineering colleges.

            Analyze this email:
            Subject: ${raw.subject}
            Sender: ${raw.sender}
            Body excerpt: $bodyExcerpt

            Return ONLY this JSON object, no markdown, no explanation:
            {
              "category": "<EXAM_CIRCULAR|ASSIGNMENT_DUE|PLACEMENT_DRIVE|FEE_NOTICE|EVENT_INVITE|GENERAL_ANNOUNCEMENT>",
              "priority": "<CRITICAL_RED|HIGH_ORANGE|MEDIUM_YELLOW|LOW_GREEN>",
              "summary": "<Two concise sentences. First: what this email is about. Second: what action the student needs to take, if any.>"
            }

            Priority assignment rules:
            - CRITICAL_RED: Exam circular, hall ticket, urgent deadline within 5 days, result published
            - HIGH_ORANGE: Placement drive registration, assignment submission deadline 6-14 days away
            - MEDIUM_YELLOW: Events, workshops, fee reminders more than 14 days away
            - LOW_GREEN: General college announcements, newsletters, FYI notices
        """.trimIndent()

        val system = "You are a JSON-only academic email classifier. Output raw JSON with no markdown wrappers."
        val rawResponse = aiGenerator(prompt, system)
        val cleanJson = extractJson(rawResponse)

        return try {
            val json = org.json.JSONObject(cleanJson)
            val catStr = json.optString("category", "GENERAL_ANNOUNCEMENT")
            val priStr = json.optString("priority", "LOW_GREEN")
            val summary = json.optString("summary", "")

            val category = try { EmailCategory.valueOf(catStr) } catch (_: Exception) { EmailCategory.GENERAL_ANNOUNCEMENT }
            val priority = try { PriorityLevel.valueOf(priStr) } catch (_: Exception) { PriorityLevel.LOW_GREEN }

            EmailClassificationResult(
                emailId = raw.imapUid.toString(),
                category = category,
                priority = priority,
                aiSummary = summary
            )
        } catch (e: Exception) {
            // Graceful fallback — never crash the pipeline
            EmailClassificationResult(
                emailId = raw.imapUid.toString(),
                category = EmailCategory.GENERAL_ANNOUNCEMENT,
                priority = PriorityLevel.LOW_GREEN,
                aiSummary = raw.subject
            )
        }
    }

    // ─── Stage 2b: Exam Date Extractor ───────────────────────────────────────

    /**
     * Called only when [classifyEmail] returns [EmailCategory.EXAM_CIRCULAR].
     * Extracts structured exam schedule from the circular text.
     */
    suspend fun extractExamDetails(
        emailBody: String,
        aiGenerator: suspend (prompt: String, system: String) -> String
    ): ExamDateExtractionResult {
        val bodyExcerpt = emailBody.take(2500).trim()
        val prompt = """
            Extract the examination schedule from this college circular text.

            Circular text:
            $bodyExcerpt

            Return ONLY this JSON, no markdown:
            {
              "examTitle": "<Official exam series name, e.g. 'End-Semester Examinations August 2026'>",
              "examStartDate": "<YYYY-MM-DD or null if not explicitly stated>",
              "examEndDate": "<YYYY-MM-DD or null if not explicitly stated>",
              "venue": "<Exam hall or location, or null>",
              "subjects": [
                { "name": "<Subject name>", "date": "<YYYY-MM-DD>", "time": "<HH:mm or null>" }
              ]
            }

            Rules:
            - Do NOT guess or estimate dates. If a date is not explicitly written, return null.
            - If only one date is mentioned, set examStartDate = examEndDate = that date.
            - Format all dates as YYYY-MM-DD (ISO 8601).
            - subjects array can be empty [] if individual paper dates are not listed.
        """.trimIndent()

        val system = "You are a JSON-only exam schedule extractor. Output raw JSON only, no extra text."
        val rawResponse = aiGenerator(prompt, system)
        val cleanJson = extractJson(rawResponse)

        return try {
            val json = org.json.JSONObject(cleanJson)
            val examTitle = json.optString("examTitle").takeIf { it.isNotBlank() }
            val venue = json.optString("venue").takeIf { it.isNotBlank() }
            val startDateStr = json.optString("examStartDate").takeIf { it != "null" && it.isNotBlank() }
            val endDateStr = json.optString("examEndDate").takeIf { it != "null" && it.isNotBlank() }

            val startMs = parseDateToMillis(startDateStr)
            val endMs = parseDateToMillis(endDateStr) ?: startMs

            val subjectsArray = json.optJSONArray("subjects")
            val subjects = mutableListOf<ExamSubject>()
            if (subjectsArray != null) {
                for (i in 0 until subjectsArray.length()) {
                    val obj = subjectsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val date = obj.optString("date").takeIf { it != "null" && it.isNotBlank() }
                    val time = obj.optString("time").takeIf { it != "null" && it.isNotBlank() }
                    val dateMs = parseDateToMillis(date) ?: continue
                    subjects.add(ExamSubject(name, dateMs, time))
                }
            }

            ExamDateExtractionResult(
                examTitle = examTitle,
                examStartDateMillis = startMs,
                examEndDateMillis = endMs,
                venue = venue,
                subjects = subjects
            )
        } catch (e: Exception) {
            ExamDateExtractionResult(null, null, null, null, emptyList())
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Strips markdown code fences and whitespace from AI response. */
    private fun extractJson(raw: String): String {
        return when {
            raw.contains("```json") -> raw.substringAfter("```json").substringBefore("```").trim()
            raw.contains("```")     -> raw.substringAfter("```").substringBefore("```").trim()
            else                    -> raw.trim()
        }
    }

    /** Parses YYYY-MM-DD string to epoch millis. Returns null on parse failure. */
    private fun parseDateToMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank() || dateStr == "null") return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            // Try alternate common formats found in Indian college circulars
            val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "MMM dd, yyyy", "dd MMM yyyy", "MMMM dd, yyyy")
            for (fmt in formats) {
                try {
                    return SimpleDateFormat(fmt, Locale.US).parse(dateStr)?.time
                } catch (_: Exception) { /* try next */ }
            }
            null
        }
    }

    fun sampleCollegeEmails(): List<CollegeEmail> = emptyList()

    // Keep legacy method signature for backward compat with HelplyViewModel
    suspend fun analyzeEmails(
        emails: List<CollegeEmail>,
        aiGenerator: suspend (String, String) -> String
    ): Pair<List<CollegeEmail>, ScanResult> {
        if (emails.isEmpty()) return Pair(emptyList(), ScanResult(0, 0, false, null, null, 0))

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

            Classify each email into: EXAM_CIRCULAR, ASSIGNMENT_DUE, PLACEMENT_DRIVE, FEE_NOTICE, EVENT_INVITE, GENERAL_ANNOUNCEMENT.
            Determine priority: CRITICAL_RED, HIGH_ORANGE, MEDIUM_YELLOW, LOW_GREEN.
            If EXAM_CIRCULAR, detect exam title and date (format: "YYYY-MM-DD").

            Return ONLY valid JSON:
            {
              "emailClassifications": [
                { "id": "email_id", "category": "EXAM_CIRCULAR", "priority": "CRITICAL_RED", "detectedExamDate": "2026-08-25", "summary": "Two sentence summary." }
              ],
              "lockdownTriggered": true,
              "examTitle": "Exam Title",
              "examDate": "2026-08-25",
              "daysUntilExam": 5
            }
            Output raw JSON only.
        """.trimIndent()

        val aiResultStr = aiGenerator(prompt, "You are a helpful college communications intelligence agent.")
        val cleanJson = extractJson(aiResultStr)

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
                    val category = try { EmailCategory.valueOf(catStr) } catch (_: Exception) { EmailCategory.GENERAL_ANNOUNCEMENT }
                    val priority = try { PriorityLevel.valueOf(priStr) } catch (_: Exception) { PriorityLevel.LOW_GREEN }
                    classificationsMap[id] = Triple(category, priority, examDate)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        val updatedEmails = emails.map { email ->
            val classification = classificationsMap[email.id]
            if (classification != null) {
                val dateMillis = parseDateToMillis(classification.third)
                email.copy(
                    category = classification.first,
                    priority = classification.second,
                    detectedExamDate = classification.third,
                    examDateMillis = dateMillis
                )
            } else email
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
