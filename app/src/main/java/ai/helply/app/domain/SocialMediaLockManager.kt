package ai.helply.app.domain

import androidx.compose.runtime.mutableStateListOf

data class LockedAppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val iconEmoji: String,
    val isLocked: Boolean
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
            LockedAppInfo("com.instagram.android", "Instagram", "Social Media", "📸", true),
            LockedAppInfo("com.google.android.youtube", "YouTube", "Entertainment", "🔴", true),
            LockedAppInfo("com.twitter.android", "X (Twitter)", "Social Media", "🐦", true),
            LockedAppInfo("com.reddit.frontpage", "Reddit", "Social & Forums", "🤖", true),
            LockedAppInfo("com.snapchat.android", "Snapchat", "Messaging & Social", "👻", true),
            LockedAppInfo("com.whatsapp", "WhatsApp (Distraction Channels)", "Messaging", "💬", false)
        )
    }
}
