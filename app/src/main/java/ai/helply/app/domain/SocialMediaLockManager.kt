package ai.helply.app.domain

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
    val examEndMillis: Long = 0L,
    val daysRemaining: Int = 0,
    val lockedApps: List<LockedAppInfo> = emptyList()  // populated by SocialMediaLockManager.getDefaultLockedApps()
)

object SocialMediaLockManager {

    /**
     * Returns the default list of apps to lock during exams.
     * This list is used as a fallback — the actual enforcement uses
     * [AIAppClassifierEngine.scanOnlyBlockedApps()] against the real device.
     */
    fun getDefaultLockedApps(): List<LockedAppInfo> = listOf(
        LockedAppInfo("com.instagram.android",            "Instagram",        "Social Media",           "📸", true,  false, "High-distraction social app"),
        LockedAppInfo("com.google.android.youtube",       "YouTube",          "Video & Entertainment",  "🔴", true,  false, "High-usage video streaming app"),
        LockedAppInfo("com.twitter.android",              "X (Twitter)",      "Social Media",           "🐦", true,  false, "High-distraction social app"),
        LockedAppInfo("com.reddit.frontpage",             "Reddit",           "Social & Forums",        "🤖", true,  false, "High-usage forum app"),
        LockedAppInfo("com.snapchat.android",             "Snapchat",         "Social Media",           "👻", true,  false, "High-distraction social app"),
        LockedAppInfo("com.facebook.katana",              "Facebook",         "Social Media",           "📘", true,  false, "High-distraction social app"),
        LockedAppInfo("com.facebook.orca",                "Messenger",        "Social Media",           "💬", true,  false, "Facebook Messenger"),
        LockedAppInfo("com.zhiliaoapp.musically",         "TikTok",           "Video & Entertainment",  "🎵", true,  false, "Short-video distraction app"),
        LockedAppInfo("com.netflix.mediaclient",          "Netflix",          "Video & Entertainment",  "🎬", true,  false, "High-usage streaming app"),
        LockedAppInfo("in.startv.hotstar",                "JioCinema/Hotstar","Video & Entertainment",  "📺", true,  false, "Streaming app"),
        LockedAppInfo("com.spotify.music",                "Spotify",          "Music & Entertainment",  "🎶", true,  false, "Music streaming — study playlists only via other means"),
        LockedAppInfo("com.discord.android",              "Discord",          "Social & Gaming",        "🟣", true,  false, "Social gaming chat"),
        LockedAppInfo("com.whatsapp",                     "WhatsApp",         "Messaging",              "💚", true,  false, "Messaging — use only for exam group coordination"),

        // ─── ALWAYS EXEMPT ───────────────────────────────────────────────────
        LockedAppInfo("com.phonepe.app",                  "PhonePe",          "Financial Payment",      "💳", false, true,  "EXEMPT: Essential Payment App"),
        LockedAppInfo("com.google.android.apps.walletnfcrel","Google Pay",    "Financial Payment",      "💳", false, true,  "EXEMPT: Essential Payment App"),
        LockedAppInfo("net.one97.paytm",                  "Paytm",            "Financial Payment",      "💳", false, true,  "EXEMPT: Essential Payment App"),
        LockedAppInfo("com.openai.chatgpt",               "ChatGPT",          "AI Assistant",           "🧠", false, true,  "EXEMPT: AI Research & Study Tool"),
        LockedAppInfo("ai.helply.app",                    "Helply OS",        "AI Student OS",          "🎓", false, true,  "EXEMPT: Primary Study Operating System")
    )
}
