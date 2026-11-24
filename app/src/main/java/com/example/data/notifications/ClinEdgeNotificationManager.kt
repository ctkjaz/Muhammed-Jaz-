package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

object ClinEdgeNotificationManager {
    const val CHANNEL_DAILY_REMINDER = "clinedge_daily_reminders"
    const val CHANNEL_EXAM_ALERTS = "clinedge_exam_alerts"
    const val CHANNEL_PERSONAL_DOUBTS = "clinedge_personal_doubts"
    const val CHANNEL_APP_UPDATES = "clinedge_app_updates"

    const val WORK_DAILY_REMINDER = "clinedge_daily_study_reminder_work"
    const val WORK_CLOUD_SYNC = "clinedge_cloud_alert_sync_work"

    const val EXTRA_OPEN_TARGET = "clinedge_open_target"
    const val EXTRA_DOUBT_ID = "clinedge_doubt_id"
    const val TARGET_DAILY_DRILL = "daily_drill"
    const val TARGET_NOTIFICATIONS = "notifications"
    const val TARGET_DOUBTS = "doubts"
    const val TARGET_APP_UPDATE = "app_update"

    private const val NOTIF_ID_DAILY_REMINDER = 41001
    private const val NOTIF_ID_ANNOUNCEMENT_BASE = 42000
    private const val NOTIF_ID_DIRECT_BASE = 43000
    private const val NOTIF_ID_APP_UPDATE = 44001

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily practice reminders with streak protection and goal progress nudges."
            },
            NotificationChannel(
                CHANNEL_EXAM_ALERTS,
                "Exam News & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live Indian nursing exam vacancies, admit cards, dates, and official news."
            },
            NotificationChannel(
                CHANNEL_PERSONAL_DOUBTS,
                "Personal & Doubt Replies",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct faculty replies to your Doubt Box questions and personal notices."
            },
            NotificationChannel(
                CHANNEL_APP_UPDATES,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts whenever a new version of ClinEdge is released with new features or question banks."
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val delayMs = EngagementLogic.calculateNextDailyTriggerDelayMs(
            nowMs = System.currentTimeMillis(),
            hour = hour,
            minute = minute
        )
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_DAILY_REMINDER,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_DAILY_REMINDER)
    }

    fun scheduleCloudAlertSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CloudNotificationSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_CLOUD_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun postDailyReminderNotification(
        context: Context,
        title: String,
        body: String
    ): Boolean {
        return postTrayNotification(
            context = context,
            channelId = CHANNEL_DAILY_REMINDER,
            notificationId = NOTIF_ID_DAILY_REMINDER,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            openTarget = TARGET_DAILY_DRILL
        )
    }

    fun postAnnouncementNotification(
        context: Context,
        announcement: CloudAnnouncement
    ): Boolean {
        val isAppUpdate = announcement.categoryEnum == AnnouncementCategory.APP_UPDATE
        val prefix = if (announcement.priorityEnum == AnnouncementPriority.URGENT && !isAppUpdate) "URGENT • " else ""
        val categoryTag = announcement.categoryEnum.label
        val notifId = NOTIF_ID_ANNOUNCEMENT_BASE + (announcement.announcementId.hashCode() and 0x0FFF)
        return postTrayNotification(
            context = context,
            channelId = if (isAppUpdate) CHANNEL_APP_UPDATES else CHANNEL_EXAM_ALERTS,
            notificationId = notifId,
            title = "$prefix${announcement.title}",
            body = announcement.body,
            subText = categoryTag,
            priority = if (announcement.priorityEnum == AnnouncementPriority.URGENT || isAppUpdate) {
                NotificationCompat.PRIORITY_HIGH
            } else {
                NotificationCompat.PRIORITY_DEFAULT
            },
            openTarget = if (isAppUpdate) TARGET_APP_UPDATE else TARGET_NOTIFICATIONS
        )
    }

    fun postAppUpdateNotification(
        context: Context,
        latestVersion: String,
        releaseTitle: String,
        releaseNotes: String
    ): Boolean {
        val cleanVer = latestVersion.removePrefix("v").removePrefix("V").trim()
        val title = releaseTitle.ifBlank { "ClinEdge v$cleanVer Available" }
        val body = releaseNotes.ifBlank {
            "A new version of ClinEdge (v$cleanVer) is ready to download. Tap to update now."
        }
        return postTrayNotification(
            context = context,
            channelId = CHANNEL_APP_UPDATES,
            notificationId = NOTIF_ID_APP_UPDATE,
            title = title,
            body = body,
            subText = "App Update",
            priority = NotificationCompat.PRIORITY_HIGH,
            openTarget = TARGET_APP_UPDATE
        )
    }

    fun postDirectNotification(
        context: Context,
        notification: CloudDirectNotification
    ): Boolean {
        val isDoubtReply = notification.typeEnum == DirectNotificationType.DOUBT_REPLY
        val notifId = NOTIF_ID_DIRECT_BASE + (notification.notificationId.hashCode() and 0x0FFF)
        return postTrayNotification(
            context = context,
            channelId = CHANNEL_PERSONAL_DOUBTS,
            notificationId = notifId,
            title = notification.title,
            body = notification.body,
            subText = notification.typeEnum.label,
            priority = NotificationCompat.PRIORITY_HIGH,
            openTarget = if (isDoubtReply) TARGET_DOUBTS else TARGET_NOTIFICATIONS,
            doubtId = if (isDoubtReply) notification.referenceId else ""
        )
    }

    private fun postTrayNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        subText: String? = null,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        openTarget: String = TARGET_NOTIFICATIONS,
        doubtId: String = ""
    ): Boolean {
        val appContext = context.applicationContext
        ensureChannelsCreated(appContext)
        if (!hasNotificationPermission(appContext)) return false

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TARGET, openTarget)
            if (doubtId.isNotBlank()) {
                putExtra(EXTRA_DOUBT_ID, doubtId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!subText.isNullOrBlank()) {
            builder.setSubText(subText)
        }

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
            true
        }.getOrDefault(false)
    }
}
