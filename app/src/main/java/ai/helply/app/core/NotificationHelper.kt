package ai.helply.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ai.helply.app.domain.PriorityLevel
import ai.helply.app.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Priority-tiered notification system for Helply.
 *
 * Channels by priority:
 * ┌────────────────┬──────────────────────┬──────────────────────────────────────┐
 * │ Priority       │ Channel ID           │ Behavior                             │
 * ├────────────────┼──────────────────────┼──────────────────────────────────────┤
 * │ CRITICAL_RED   │ helply_critical      │ Heads-up popup + vibrate + sound     │
 * │ HIGH_ORANGE    │ helply_high          │ Heads-up popup + sound               │
 * │ MEDIUM_YELLOW  │ helply_medium        │ Status bar, no interrupt             │
 * │ LOW_GREEN      │ helply_low           │ Silent status bar                    │
 * │ Exam Lock      │ helply_lock_active   │ Persistent ongoing notification      │
 * │ Email Monitor  │ helply_monitor       │ Silent monitoring service badge      │
 * └────────────────┴──────────────────────┴──────────────────────────────────────┘
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_CRITICAL = "helply_critical"
        const val CHANNEL_HIGH     = "helply_high"
        const val CHANNEL_MEDIUM   = "helply_medium"
        const val CHANNEL_LOW      = "helply_low"
        const val CHANNEL_LOCK     = "helply_lock_active"
        const val CHANNEL_MONITOR  = "helply_monitor"

        const val NOTIF_ID_LOCK_ACTIVE  = 9001
        const val NOTIF_ID_LOCK_LIFTED  = 9002
        const val NOTIF_ID_MONITOR      = 9003
    }

    init {
        createAllChannels()
    }

    // ─── Channel Creation ─────────────────────────────────────────────────────

    private fun createAllChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            NotificationChannel(CHANNEL_CRITICAL, "Critical Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Exam circulars and critical academic deadlines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                enableLights(true)
                lightColor = 0xFFFF0000.toInt()
            },
            NotificationChannel(CHANNEL_HIGH, "High Priority Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Placement drives, assignment deadlines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400)
            },
            NotificationChannel(CHANNEL_MEDIUM, "Medium Priority", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Events, workshops, fee reminders"
            },
            NotificationChannel(CHANNEL_LOW, "General Announcements", NotificationManager.IMPORTANCE_LOW).apply {
                description = "College newsletters and general notices"
            },
            NotificationChannel(CHANNEL_LOCK, "Exam Lock Active", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Active exam focus lock status"
                enableVibration(false)
            },
            NotificationChannel(CHANNEL_MONITOR, "Email Monitor", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Background email monitoring service"
            }
        ).forEach { manager.createNotificationChannel(it) }
    }

    // ─── Email Priority Notification (HEADS-UP for CRITICAL + HIGH) ──────────

    /**
     * Sends a priority notification for a classified email.
     * CRITICAL_RED and HIGH_ORANGE use PRIORITY_MAX which triggers
     * the Android heads-up (peek) notification that pops over any foreground app.
     */
    fun sendEmailPriorityNotification(
        category: String,
        priority: PriorityLevel,
        subject: String,
        summary: String,
        sender: String,
        isExamCircular: Boolean
    ) {
        val (channelId, priorityFlag, emoji) = when (priority) {
            PriorityLevel.CRITICAL_RED  -> Triple(CHANNEL_CRITICAL, NotificationCompat.PRIORITY_MAX, "🔴")
            PriorityLevel.HIGH_ORANGE   -> Triple(CHANNEL_HIGH,     NotificationCompat.PRIORITY_HIGH,"🟠")
            PriorityLevel.MEDIUM_YELLOW -> Triple(CHANNEL_MEDIUM,   NotificationCompat.PRIORITY_DEFAULT, "🟡")
            PriorityLevel.LOW_GREEN     -> Triple(CHANNEL_LOW,      NotificationCompat.PRIORITY_LOW, "🟢")
        }

        val tapIntent = buildDeepLinkPendingIntent("email_intelligence")

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("$emoji $category")
            .setContentText(subject)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$emoji $subject")
                    .bigText(summary)
                    .setSummaryText("From: $sender")
            )
            .setPriority(priorityFlag)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)

        // Add vibration pattern for critical only
        if (priority == PriorityLevel.CRITICAL_RED) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
        }

        // Action buttons for exam circulars
        if (isExamCircular) {
            builder.addAction(
                android.R.drawable.ic_menu_agenda,
                "📅 View Schedule",
                buildDeepLinkPendingIntent("exam_schedule")
            )
            builder.addAction(
                android.R.drawable.ic_lock_idle_lock,
                "🔒 Activate Lock",
                buildDeepLinkPendingIntent("activate_lock")
            )
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // ─── Exam Lock Notifications ──────────────────────────────────────────────

    /** Persistent, non-dismissable notification shown during active lockdown. */
    fun sendExamLockActivatedNotification(examTitle: String, startDate: String, endDate: String) {
        val tapIntent = buildDeepLinkPendingIntent("email_intelligence")
        val notification = NotificationCompat.Builder(context, CHANNEL_LOCK)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("🔒 Exam Lock Active")
            .setContentText("$examTitle · Social apps blocked")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$examTitle\n$startDate → $endDate\n\nSocial media & entertainment apps are blocked. Stay focused! 📚")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)           // Cannot be dismissed by swipe
            .setAutoCancel(false)
            .setContentIntent(tapIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_LOCK_ACTIVE, notification)
    }

    /** One-shot notification when exams are over and lock is lifted. */
    fun sendExamLockLiftedNotification(examTitle: String) {
        // First cancel the ongoing lock notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_LOCK_ACTIVE)

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✅ Exam Lock Lifted!")
            .setContentText("$examTitle completed. Apps are now accessible.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$examTitle has ended.\nGreat job staying focused! Social apps are now accessible again. 🎉")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIF_ID_LOCK_LIFTED, notification)
    }

    // ─── Legacy Generic Notification ─────────────────────────────────────────

    /** Backward-compatible generic notification sender used by legacy code. */
    fun sendNotification(
        context: Context = this.context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        createAllChannels()
        val builder = NotificationCompat.Builder(context, CHANNEL_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private fun buildDeepLinkPendingIntent(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("nav_destination", destination)
        }
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
