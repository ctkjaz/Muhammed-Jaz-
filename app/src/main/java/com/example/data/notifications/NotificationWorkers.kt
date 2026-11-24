package com.example.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.SettingsPreferences
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.StudyGoalLogic
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsPreferences(applicationContext)
        if (!settings.dailyReminderEnabled.value) {
            return Result.success()
        }

        val todayKey = StudyGoalLogic.todayDateKey()
        val yesterdayKey = StudyGoalLogic.yesterdayDateKey()
        val activeExam = PlatformCatalogDefaults.examById(settings.activeExamId.value)
        val (title, body) = EngagementLogic.buildDailyReminderCopy(
            goalState = settings.studyGoalState.value,
            todayKey = todayKey,
            yesterdayKey = yesterdayKey,
            smartNudge = settings.dailyReminderSmartNudge.value,
            activeExamShortName = activeExam?.shortName ?: "Nursing"
        )

        ClinEdgeNotificationManager.postDailyReminderNotification(
            context = applicationContext,
            title = title,
            body = body
        )

        // Chain tomorrow's daily reminder cleanly without requiring restricted exact alarms.
        ClinEdgeNotificationManager.scheduleDailyReminder(
            context = applicationContext,
            hour = settings.dailyReminderHour.value,
            minute = settings.dailyReminderMinute.value
        )
        return Result.success()
    }
}

class CloudNotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return Result.success()

        val settings = SettingsPreferences(applicationContext)
        val activeExamId = settings.activeExamId.value
        val repo = runCatching { CloudEngagementRepository(applicationContext) }.getOrNull()
            ?: return Result.success()

        // 1. Check for new global / active-exam broadcasts since last notification watermark
        val lastAnnMs = settings.getLastNotifiedAnnouncementMs()
        val announcements = repo.fetchRecentAnnouncementsOnce(limit = 15)
        if (lastAnnMs == 0L) {
            val newestMs = announcements.maxOfOrNull { it.createdAtMs } ?: System.currentTimeMillis()
            settings.setLastNotifiedAnnouncementMs(newestMs)
        } else {
            val freshAnnouncements = announcements
                .filter { it.createdAtMs > lastAnnMs && EngagementLogic.announcementMatchesExam(it, activeExamId) }
                .sortedBy { it.createdAtMs }
            freshAnnouncements.takeLast(3).forEach { ann ->
                ClinEdgeNotificationManager.postAnnouncementNotification(applicationContext, ann)
            }
            freshAnnouncements.maxOfOrNull { it.createdAtMs }?.let {
                settings.setLastNotifiedAnnouncementMs(it)
            }
        }

        // 2. Check for new individual / doubt-reply notifications
        val lastDirectMs = settings.getLastNotifiedDirectMs()
        val unreadDirect = repo.fetchUnreadDirectNotificationsOnce(uid, limit = 20)
        val freshDirect = unreadDirect
            .filter { it.createdAtMs > lastDirectMs }
            .sortedBy { it.createdAtMs }
        freshDirect.takeLast(3).forEach { notif ->
            ClinEdgeNotificationManager.postDirectNotification(applicationContext, notif)
        }
        freshDirect.maxOfOrNull { it.createdAtMs }?.let {
            settings.setLastNotifiedDirectMs(it)
        }

        return Result.success()
    }
}

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            val settings = SettingsPreferences(context)
            ClinEdgeNotificationManager.ensureChannelsCreated(context)
            if (settings.dailyReminderEnabled.value) {
                ClinEdgeNotificationManager.scheduleDailyReminder(
                    context = context,
                    hour = settings.dailyReminderHour.value,
                    minute = settings.dailyReminderMinute.value
                )
            }
            ClinEdgeNotificationManager.scheduleCloudAlertSync(context)
        }
    }
}
