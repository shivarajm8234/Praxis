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

    fun analyzeEmails(emails: List<CollegeEmail>): ScanResult {
        var examFound = false
        var examTitle: String? = null
        var examDateStr: String? = null
        var daysCount = 0

        val examCirculars = emails.filter { it.category == EmailCategory.EXAM_CIRCULAR }
        if (examCirculars.isNotEmpty()) {
            val topExam = examCirculars.first()
            examFound = true
            examTitle = topExam.subject
            examDateStr = topExam.detectedExamDate ?: "Upcoming Exam"

            if (topExam.examDateMillis != null) {
                val diffMs = topExam.examDateMillis - System.currentTimeMillis()
                daysCount = (diffMs / 86400000L).toInt().coerceAtLeast(1)
            } else {
                daysCount = 5
            }
        }

        return ScanResult(
            processedCount = emails.size,
            examCircularsFound = examCirculars.size,
            activeLockdownTriggered = examFound,
            examTitle = examTitle,
            examDate = examDateStr,
            daysUntilExam = daysCount
        )
    }
}
