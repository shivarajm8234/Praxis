package ai.helply.app.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Calendar

data class ClassifiedApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isBlockedDuringExams: Boolean,
    val reasoning: String,
    val iconEmoji: String,
    val usageTimeMs: Long = 0L,
    val usageTimeFormatted: String = "0m today"
)

enum class AppCategory {
    SOCIAL_MEDIA_BLOCKED,
    HIGH_USAGE_ENTERTAINMENT_BLOCKED,
    PAYMENT_APP_ALLOWED,
    AI_APP_ALLOWED,
    STUDY_PRODUCTIVITY_ALLOWED,
    SYSTEM_ESSENTIAL_ALLOWED
}

object AIAppClassifierEngine {

    private val paymentKeywords = listOf("pay", "upi", "wallet", "phonepe", "paytm", "gpay", "bhim", "bank", "hdfc", "sbi", "icici", "axis")
    private val aiKeywords = listOf("chatgpt", "openai", "claude", "gemini", "perplexity", "copilot", "helply", "anthropic")
    private val socialKeywords = listOf("instagram", "snapchat", "twitter", "facebook", "tiktok", "reddit", "pinterest", "threads", "tumblr", "whatsapp")
    private val entertainmentKeywords = listOf("youtube", "netflix", "primevideo", "hotstar", "twitch", "pubg", "bgmi", "candycrush", "roblox", "vlc")

    fun classifyApp(packageName: String, appName: String, usageMs: Long = 0L): ClassifiedApp {
        val pName = packageName.lowercase()
        val aName = appName.lowercase()
        val formattedUsage = formatUsageTime(usageMs)

        // 1. Check AI Apps (STRICT EXEMPTION - NEVER BLOCK)
        if (aiKeywords.any { pName.contains(it) || aName.contains(it) }) {
            return ClassifiedApp(
                packageName = packageName,
                appName = appName,
                category = AppCategory.AI_APP_ALLOWED,
                isBlockedDuringExams = false,
                reasoning = "AI Assistant App (Allowed for research & study)",
                iconEmoji = "🧠",
                usageTimeMs = usageMs,
                usageTimeFormatted = formattedUsage
            )
        }

        // 2. Check Payment Apps (STRICT EXEMPTION - NEVER BLOCK)
        if (paymentKeywords.any { pName.contains(it) || aName.contains(it) }) {
            return ClassifiedApp(
                packageName = packageName,
                appName = appName,
                category = AppCategory.PAYMENT_APP_ALLOWED,
                isBlockedDuringExams = false,
                reasoning = "Financial & Payment App (Allowed for essential transactions)",
                iconEmoji = "💳",
                usageTimeMs = usageMs,
                usageTimeFormatted = formattedUsage
            )
        }

        // 3. Check Social Media Apps (STRICT BLOCK)
        if (socialKeywords.any { pName.contains(it) || aName.contains(it) }) {
            return ClassifiedApp(
                packageName = packageName,
                appName = appName,
                category = AppCategory.SOCIAL_MEDIA_BLOCKED,
                isBlockedDuringExams = true,
                reasoning = "High Distraction Social Media (Locked 5 days before exam)",
                iconEmoji = "📸",
                usageTimeMs = usageMs,
                usageTimeFormatted = formattedUsage
            )
        }

        // 4. Check YouTube & High Usage Entertainment Apps (STRICT BLOCK)
        if (entertainmentKeywords.any { pName.contains(it) || aName.contains(it) }) {
            return ClassifiedApp(
                packageName = packageName,
                appName = appName,
                category = AppCategory.HIGH_USAGE_ENTERTAINMENT_BLOCKED,
                isBlockedDuringExams = true,
                reasoning = "High Usage Video / Entertainment (Locked 5 days before exam)",
                iconEmoji = "🔴",
                usageTimeMs = usageMs,
                usageTimeFormatted = formattedUsage
            )
        }

        // 5. Default Study & System Apps (ALLOWED)
        return ClassifiedApp(
            packageName = packageName,
            appName = appName,
            category = AppCategory.STUDY_PRODUCTIVITY_ALLOWED,
            isBlockedDuringExams = false,
            reasoning = "Study / Productivity / System Tool (Allowed)",
            iconEmoji = "📚",
            usageTimeMs = usageMs,
            usageTimeFormatted = formattedUsage
        )
    }

    private fun formatUsageTime(usageMs: Long): String {
        if (usageMs <= 0) return "15m today"
        val totalMinutes = usageMs / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return if (hours > 0) {
            "${hours}h ${mins}m today"
        } else {
            "${mins}m today"
        }
    }

    /**
     * Scans ONLY BLOCKED installed applications along with their screen usage time today.
     */
    fun scanOnlyBlockedApps(context: Context): List<ClassifiedApp> {
        val list = mutableListOf<ClassifiedApp>()
        val pm = context.packageManager

        // Query today's UsageStats
        val usageMap = mutableMapOf<String, Long>()
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usm != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()
                val statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                if (statsList != null) {
                    for (stat in statsList) {
                        usageMap[stat.packageName] = (usageMap[stat.packageName] ?: 0L) + stat.totalTimeInForeground
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)

                if (!isSystemApp || launchIntent != null) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName

                    if (packageName != "android" && !packageName.startsWith("com.android.internal")) {
                        val usageMs = usageMap[packageName] ?: 0L
                        val classified = classifyApp(packageName, appName, usageMs)

                        // ONLY INCLUDE BLOCKED APPS!
                        if (classified.isBlockedDuringExams) {
                            list.add(classified)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort by usage time (highest usage first)
        return list.sortedByDescending { it.usageTimeMs }
    }
}
