package ai.helply.app.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class ClassifiedApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isBlockedDuringExams: Boolean,
    val reasoning: String,
    val iconEmoji: String
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

    fun classifyApp(packageName: String, appName: String): ClassifiedApp {
        val pName = packageName.lowercase()
        val aName = appName.lowercase()

        // 1. Check AI Apps (STRICT EXEMPTION - NEVER BLOCK)
        if (aiKeywords.any { pName.contains(it) || aName.contains(it) }) {
            return ClassifiedApp(
                packageName = packageName,
                appName = appName,
                category = AppCategory.AI_APP_ALLOWED,
                isBlockedDuringExams = false,
                reasoning = "AI Assistant App (Allowed for research & study)",
                iconEmoji = "🧠"
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
                iconEmoji = "💳"
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
                iconEmoji = "📸"
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
                iconEmoji = "🔴"
            )
        }

        // 5. Default Study & System Apps (ALLOWED)
        return ClassifiedApp(
            packageName = packageName,
            appName = appName,
            category = AppCategory.STUDY_PRODUCTIVITY_ALLOWED,
            isBlockedDuringExams = false,
            reasoning = "Study / Productivity / System Tool (Allowed)",
            iconEmoji = "📚"
        )
    }

    /**
     * Scans REAL installed applications live from the device package manager.
     */
    fun scanInstalledApps(context: Context): List<ClassifiedApp> {
        val list = mutableListOf<ClassifiedApp>()
        val pm = context.packageManager

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                // Skip internal system core services unless launchable
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)

                if (!isSystemApp || launchIntent != null) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName

                    // Avoid duplicate OS core apps that aren't relevant
                    if (packageName != "android" && !packageName.startsWith("com.android.internal")) {
                        list.add(classifyApp(packageName, appName))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort: Blocked apps first, then Allowed apps
        return list.sortedByDescending { it.isBlockedDuringExams }
    }
}
