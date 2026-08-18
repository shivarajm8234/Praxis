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

    fun sampleCollegeEmails(): List<CollegeEmail> = listOf(
        CollegeEmail(
            id = "em_101",
            sender = "controller-exams@university.edu.in",
            subject = "URGENT CIRCULAR: End-Semester Theory Examinations Schedule 2024",
            snippet = "The End-Semester theory examinations for 7th Semester B.Tech will commence on Oct 28, 2024...",
            fullBody = "Dear Students,\n\nThis is an official circular regarding the upcoming End-Semester Theory Examinations.\n\nKey Details:\n- Start Date: Oct 28, 2024\n- End Date: Nov 10, 2024\n- Venue: Main Academic Block\n- Strict Attendance Rule: 75% mandatory.\n\nPlease prepare accordingly.",
            timestamp = "10:15 AM Today",
            category = EmailCategory.EXAM_CIRCULAR,
            priority = PriorityLevel.CRITICAL_RED,
            detectedExamDate = "Oct 28, 2024",
            examDateMillis = System.currentTimeMillis() + (5 * 86400000L) // 5 days from now
        ),
        CollegeEmail(
            id = "em_102",
            sender = "placements@university.edu.in",
            subject = "Placement Drive Notice: TechCorp & Google Software Engineer Roles",
            snippet = "TechCorp is visiting campus on Oct 30 for On-Campus hiring. Resume submission deadline is Oct 27...",
            fullBody = "Attention Batch of 2025,\n\nTechCorp and Google have confirmed on-campus placement drives for Software Engineer roles.\nCTC: 18 - 24 LPA.\nEligible Branches: CSE, ISE, ECE.\nSubmission Deadline: Oct 27, 2024.",
            timestamp = "Yesterday 4:30 PM",
            category = EmailCategory.PLACEMENT_DRIVE,
            priority = PriorityLevel.HIGH_ORANGE
        ),
        CollegeEmail(
            id = "em_103",
            sender = "hod-cse@university.edu.in",
            subject = "Submission Reminder: Machine Learning Lab Report & Slides",
            snippet = "All students must submit the ResNet-18 lab report and presentation slides before Oct 26...",
            fullBody = "Dear Students,\n\nPlease upload your Machine Learning Lab Report (PDF) and Presentation Slides (PPT) to the LMS portal before Oct 26, 11:59 PM.",
            timestamp = "2 Days Ago",
            category = EmailCategory.ASSIGNMENT_DUE,
            priority = PriorityLevel.HIGH_ORANGE
        )
    )

    fun analyzeEmails(emails: List<CollegeEmail>): ScanResult {
        var examFound = false
        var examTitle: String? = null
        var examDateStr: String? = null
        var daysCount = 5

        val examCirculars = emails.filter { it.category == EmailCategory.EXAM_CIRCULAR }
        if (examCirculars.isNotEmpty()) {
            val topExam = examCirculars.first()
            examFound = true
            examTitle = topExam.subject
            examDateStr = topExam.detectedExamDate ?: "Oct 28, 2024"
            daysCount = 5
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
