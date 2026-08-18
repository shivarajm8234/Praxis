package ai.helply.app.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ai.helply.app.domain.AIAppClassifierEngine
import ai.helply.app.ui.AppLockOverlayActivity

object AppLockEnforcer {

    // Track last blocked package + time to prevent rapid re-launches of the overlay
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L
    private const val BLOCK_COOLDOWN_MS = 3000L // 3 second cooldown

    fun getForegroundPackageName(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 3000L

        val usageEvents = usm.queryEvents(startTime, endTime) ?: return null
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        // Use ACTIVITY_RESUMED on API 29+ (MOVE_TO_FOREGROUND is deprecated)
        val targetEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == targetEventType) {
                currentForegroundPackage = event.packageName
            }
        }
        return currentForegroundPackage
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun enforceLockIfBlocked(
        context: Context,
        isLockActive: Boolean,
        manuallyLockedPackages: Set<String> = emptySet()
    ): Boolean {
        if (!AppLockPermissionManager.hasUsageStatsPermission(context)) return false

        val fgPackage = getForegroundPackageName(context) ?: return false

        // Never block Helply OS or its lock overlay itself!
        if (fgPackage == context.packageName) return false

        val isManuallyLocked = manuallyLockedPackages.contains(fgPackage)
        val appLabel = getAppLabel(context, fgPackage)
        val classified = AIAppClassifierEngine.classifyApp(fgPackage, appLabel)
        val isExamBlocked = isLockActive && classified.isBlockedDuringExams

        if (isManuallyLocked || isExamBlocked) {
            // Cooldown: avoid rapid re-launching of overlay while user is viewing it
            val now = System.currentTimeMillis()
            if (fgPackage == lastBlockedPackage && (now - lastBlockedTimestamp) < BLOCK_COOLDOWN_MS) {
                return true // Already blocked recently, skip re-launch
            }
            lastBlockedPackage = fgPackage
            lastBlockedTimestamp = now

            val reason = if (isExamBlocked) {
                "Helply AI detected an active Exam Circular (End-Semester Exams starting in 5 days). Access to high-distraction social & streaming apps is locked until exams end."
            } else {
                "This application was manually locked by you in Helply Student OS settings to maintain uninterrupted study focus."
            }

            // Launch AppLockOverlayActivity with proper app name and reasoning
            val overlayIntent = Intent(context, AppLockOverlayActivity::class.java).apply {
                putExtra("BLOCKED_APP_NAME", classified.appName)
                putExtra("BLOCKED_PACKAGE_NAME", fgPackage)
                putExtra("LOCK_REASON", reason)
                putExtra("ICON_EMOJI", classified.iconEmoji)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(overlayIntent)
            return true
        }

        // User navigated away from blocked app — reset cooldown tracking
        if (fgPackage != lastBlockedPackage) {
            lastBlockedPackage = null
            lastBlockedTimestamp = 0L
        }
        return false
    }
}
