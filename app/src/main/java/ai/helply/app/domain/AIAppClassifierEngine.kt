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

    // Package keywords & exact packages
    private val paymentKeywords = listOf("pay", "upi", "wallet", "phonepe", "paytm", "gpay", "bhim", "bank", "hdfc", "sbi", "icici", "axis")
    private val aiKeywords = listOf("chatgpt", "openai", "claude", "gemini", "perplexity", "copilot", "helply", "anthropic")
    private val socialKeywords = listOf("instagram", "snapchat", "twitter", "facebook", "tiktok", "reddit", "pinterest", "threads", "tumblr")
    private val entertainmentKeywords = listOf("youtube", "netflix", "primevideo", "hotstar", "twitch", "pubg", "bgmi", "candycrush", "roblox")

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

    fun scanInstalledApps(context: Context): List<ClassifiedApp> {
        val list = mutableListOf<ClassifiedApp>()
        val pm = context.packageManager

        // Standard sample set if PM query returns basic packages
        val samplePackages = listOf(
            "com.instagram.android" to "Instagram",
            "com.google.android.youtube" to "YouTube",
            "com.snapchat.android" to "Snapchat",
            "com.twitter.android" to "X (Twitter)",
            "com.reddit.frontpage" to "Reddit",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "com.google.android.apps.nfc.payment" to "Google Pay (GPay)",
            "com.openai.chatgpt" to "ChatGPT",
            "ai.helply.app" to "Helply OS",
            "com.google.android.apps.docs" to "Google Drive",
            "com.netflix.mediaclient" to "Netflix"
        )

        samplePackages.forEach { (pkg, name) ->
            list.add(classifyApp(pkg, name))
        }

        return list
    }
}
