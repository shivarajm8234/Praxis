package ai.helply.app.domain

import android.content.Context

data class LockedAppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val iconEmoji: String,
    val isLocked: Boolean,
    val isPaymentOrAI: Boolean = false,
    val reasoning: String = ""
)

data class ExamLockState(
    val isLockActive: Boolean = false,
    val examTitle: String = "",
    val examDateMillis: Long = 0L,
    val daysRemaining: Int = 0,
    val lockedApps: List<LockedAppInfo> = defaultApps()
) {
    companion object {
        fun defaultApps(): List<LockedAppInfo> = listOf(
            LockedAppInfo("com.instagram.android", "Instagram", "Social Media", "📸", true, false, "High Distraction Social App"),
            LockedAppInfo("com.google.android.youtube", "YouTube", "Video & Entertainment", "🔴", true, false, "High Usage Streaming App"),
            LockedAppInfo("com.twitter.android", "X (Twitter)", "Social Media", "🐦", true, false, "High Distraction Social App"),
            LockedAppInfo("com.reddit.frontpage", "Reddit", "Social & Forums", "🤖", true, false, "High Usage Forum App"),
            LockedAppInfo("com.snapchat.android", "Snapchat", "Social Media", "👻", true, false, "High Distraction Social App"),
            LockedAppInfo("com.phonepe.app", "PhonePe", "Financial Payment", "💳", false, true, "EXEMPT: Essential Payment App"),
            LockedAppInfo("com.google.android.apps.nfc.payment", "Google Pay (GPay)", "Financial Payment", "💳", false, true, "EXEMPT: Essential Payment App"),
            LockedAppInfo("com.openai.chatgpt", "ChatGPT", "AI Assistant", "🧠", false, true, "EXEMPT: AI Research Tool"),
            LockedAppInfo("ai.helply.app", "Helply OS", "AI Student OS", "🎓", false, true, "EXEMPT: Primary Study OS")
        )
    }
}
