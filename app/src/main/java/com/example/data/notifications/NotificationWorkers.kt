package com.example.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.local.SettingsPreferences
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.StudyGoalLogic
import com.example.data.update.AppUpdateChecker
import com.example.data.update.AppUpdateStatus
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
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
        val settings = SettingsPreferences(applicationContext)
        val notifyIfUpToDate = inputData.getBoolean(
            ClinEdgeNotificationManager.KEY_NOTIFY_IF_UP_TO_DATE,
            false
        )
        if (!settings.backgroundPushEnabled.value && !notifyIfUpToDate) {
            return Result.success()
        }

        // Defensively initialize Firebase in cold-started background processes (after app swipe-kill).
        runCatching {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(applicationContext)
            }
        }
        ClinEdgeNotificationManager.ensureChannelsCreated(applicationContext)

        var anyNetworkSuccess = false
        var postedAnyAlert = false

        // 1. Always check GitHub Releases for a newer ClinEdge APK version (independent of Firebase Auth)
        runCatching {
            val update = AppUpdateChecker.checkLatestRelease(BuildConfig.VERSION_NAME)
            if (update.status != AppUpdateStatus.ERROR) {
                anyNetworkSuccess = true
            }
            if (update.status == AppUpdateStatus.UPDATE_AVAILABLE &&
                update.latestVersion.isNotBlank() &&
                update.latestVersion != settings.getLastNotifiedAppUpdateVersion()
            ) {
                val posted = ClinEdgeNotificationManager.postAppUpdateNotification(
                    context = applicationContext,
                    latestVersion = update.latestVersion,
                    releaseTitle = update.releaseTitle,
                    releaseNotes = update.releaseNotes
                )
                if (posted) {
                    postedAnyAlert = true
                    settings.setLastNotifiedAppUpdateVersion(update.latestVersion)
                }
            }
        }

        // 2. Check Firestore broadcasts, App Update pushes, and direct notifications when signed in
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            ?.takeIf { it.isNotBlank() }

        if (uid != null) {
            val activeExamId = settings.activeExamId.value
            val repo = runCatching { CloudEngagementRepository(applicationContext) }.getOrNull()
            if (repo != null) {
                val nowMs = System.currentTimeMillis()
                // 2a. Global / active-exam broadcasts & Admin App-Update pushes (fetched from SERVER)
                val lastAnnMs = settings.getLastNotifiedAnnouncementMs()
                val announcements = repo.fetchRecentAnnouncementsOnce(limit = 20)
                if (announcements.isNotEmpty()) {
                    anyNetworkSuccess = true
                }
                if (lastAnnMs == 0L) {
                    val recentUrgentOrUpdate = announcements
                        .filter { ann ->
                            val isRecent = ann.createdAtMs >= nowMs - RECENT_FIRST_RUN_WINDOW_MS
                            val isHighPriority = ann.categoryEnum == AnnouncementCategory.APP_UPDATE ||
                                ann.priorityEnum == AnnouncementPriority.URGENT
                            isRecent && isHighPriority && EngagementLogic.announcementMatchesExam(ann, activeExamId)
                        }
                        .sortedBy { it.createdAtMs }
                    if (recentUrgentOrUpdate.isNotEmpty()) {
                        var highestPostedMs = 0L
                        recentUrgentOrUpdate.takeLast(2).forEach { ann ->
                            if (ClinEdgeNotificationManager.postAnnouncementNotification(applicationContext, ann)) {
                                postedAnyAlert = true
                                highestPostedMs = maxOf(highestPostedMs, ann.createdAtMs)
                            }
                        }
                        if (highestPostedMs > 0L) {
                            settings.setLastNotifiedAnnouncementMs(highestPostedMs)
                        }
                    } else if (ClinEdgeNotificationManager.hasNotificationPermission(applicationContext)) {
                        val newestMs = announcements.maxOfOrNull { it.createdAtMs } ?: nowMs
                        settings.setLastNotifiedAnnouncementMs(newestMs)
                    }
                } else {
                    val freshAnnouncements = announcements
                        .filter { it.createdAtMs > lastAnnMs && EngagementLogic.announcementMatchesExam(it, activeExamId) }
                        .sortedBy { it.createdAtMs }
                    var highestPostedMs = lastAnnMs
                    freshAnnouncements.takeLast(3).forEach { ann ->
                        if (ClinEdgeNotificationManager.postAnnouncementNotification(applicationContext, ann)) {
                            postedAnyAlert = true
                            highestPostedMs = maxOf(highestPostedMs, ann.createdAtMs)
                        }
                    }
                    if (highestPostedMs > lastAnnMs) {
                        settings.setLastNotifiedAnnouncementMs(highestPostedMs)
                    }
                }

                // 2b. Individual / renewal nudge / doubt-reply notifications (fetched from SERVER)
                val lastDirectMs = settings.getLastNotifiedDirectMs()
                val unreadDirect = repo.fetchUnreadDirectNotificationsOnce(uid, limit = 25)
                if (unreadDirect.isNotEmpty()) {
                    anyNetworkSuccess = true
                }
                val freshDirect = unreadDirect
                    .filter { it.createdAtMs > lastDirectMs }
                    .sortedBy { it.createdAtMs }
                var highestDirectPostedMs = lastDirectMs
                freshDirect.takeLast(3).forEach { notif ->
                    if (ClinEdgeNotificationManager.postDirectNotification(applicationContext, notif)) {
                        postedAnyAlert = true
                        highestDirectPostedMs = maxOf(highestDirectPostedMs, notif.createdAtMs)
                    }
                }
                if (highestDirectPostedMs > lastDirectMs) {
                    settings.setLastNotifiedDirectMs(highestDirectPostedMs)
                }
            }
        }

        if (anyNetworkSuccess || uid == null) {
            settings.setLastBackgroundSyncMs(System.currentTimeMillis())
        }

        if (notifyIfUpToDate && !postedAnyAlert) {
            ClinEdgeNotificationManager.postBackgroundSyncConfirmedNotification(applicationContext)
        }

        // Re-arm the next Doze-resilient inexact pulse whenever background push is enabled.
        if (settings.backgroundPushEnabled.value) {
            ClinEdgeNotificationManager.scheduleBackgroundAlertPulse(applicationContext)
        }

        return if (!anyNetworkSuccess && runAttemptCount < 2) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val RECENT_FIRST_RUN_WINDOW_MS = 48L * 60L * 60L * 1000L
    }
}

/**
 * Inexact Doze-resilient wakeup receiver (`AlarmManager.setAndAllowWhileIdle` — no restricted exact-alarm
 * permission required). Enqueues an expedited WorkManager cloud check even when the app is closed.
 */
class BackgroundAlertPulseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = SettingsPreferences(context)
        if (!settings.backgroundPushEnabled.value) return
        ClinEdgeNotificationManager.ensureChannelsCreated(context)
        ClinEdgeNotificationManager.triggerImmediateCloudSync(context, notifyIfUpToDate = false)
        ClinEdgeNotificationManager.scheduleBackgroundAlertPulse(context)
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
            if (settings.backgroundPushEnabled.value) {
                ClinEdgeNotificationManager.scheduleCloudAlertSync(
                    context = context,
                    triggerImmediateCheck = true
                )
            }
        }
    }
}
