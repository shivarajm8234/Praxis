package ai.helply.app.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import ai.helply.app.domain.AIAppClassifierEngine

object AppLockEnforcer {

    fun getForegroundPackageName(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 3000L

        val usageEvents = usm.queryEvents(startTime, endTime) ?: return null
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentForegroundPackage = event.packageName
            }
        }
        return currentForegroundPackage
    }

    fun enforceLockIfBlocked(context: Context, isLockActive: Boolean): Boolean {
        if (!isLockActive) return false
        if (!AppLockPermissionManager.hasUsageStatsPermission(context)) return false

        val fgPackage = getForegroundPackageName(context) ?: return false

        // Don't block Helply OS itself!
        if (fgPackage == context.packageName) return false

        val classified = AIAppClassifierEngine.classifyApp(fgPackage, fgPackage)
        if (classified.isBlockedDuringExams) {
            // Block app! Send user to Home screen launch intent
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            return true
        }
        return false
    }
}
