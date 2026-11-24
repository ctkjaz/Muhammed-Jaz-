package com.example.data.notifications

import androidx.annotation.Keep
import com.example.data.model.DailyStudyGoalState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class AnnouncementCategory(val dbValue: String, val label: String) {
    EXAM_ALERT("exam_alert", "Exam Alert"),
    VACANCY("vacancy", "Vacancy"),
    NEWS("news", "News"),
    STUDY_TIP("study_tip", "Study Tip"),
    APP_UPDATE("app_update", "App Update");

    companion object {
        fun fromRaw(raw: String?): AnnouncementCategory =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: NEWS
    }
}

enum class AnnouncementPriority(val dbValue: String, val label: String) {
    NORMAL("normal", "Normal"),
    URGENT("urgent", "Urgent");

    companion object {
        fun fromRaw(raw: String?): AnnouncementPriority =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: NORMAL
    }
}

enum class DirectNotificationType(val dbValue: String, val label: String) {
    CUSTOM_ADMIN("custom_admin", "Personal Message"),
    DOUBT_REPLY("doubt_reply", "Doubt Reply"),
    ACCOUNT_NOTICE("account_notice", "Account Notice");

    companion object {
        fun fromRaw(raw: String?): DirectNotificationType =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: CUSTOM_ADMIN
    }
}

enum class DoubtStatus(val dbValue: String, val label: String) {
    OPEN("open", "Open"),
    ANSWERED("answered", "Answered"),
    CLOSED("closed", "Closed");

    companion object {
        fun fromRaw(raw: String?): DoubtStatus =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class CloudAnnouncement(
    @DocumentId val docId: String = "",
    val announcementId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = AnnouncementCategory.NEWS.dbValue,
    val targetExamId: String = "all",
    val priority: String = AnnouncementPriority.NORMAL.dbValue,
    val actionUrl: String = "",
    val createdByEmail: String = "",
    val createdAt: Timestamp? = null
) {
    val categoryEnum: AnnouncementCategory get() = AnnouncementCategory.fromRaw(category)
    val priorityEnum: AnnouncementPriority get() = AnnouncementPriority.fromRaw(priority)
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    /**
     * Stable non-empty identifier used for dismissing urgent home banners and Compose list keys,
     * even if a legacy Firestore announcement document omitted the `announcementId` field.
     */
    val effectiveBannerKey: String
        get() = announcementId.trim()
            .ifBlank { docId.trim() }
            .ifBlank { "${title.trim()}|$createdAtMs" }
}

@Keep
@IgnoreExtraProperties
data class CloudDirectNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = DirectNotificationType.CUSTOM_ADMIN.dbValue,
    val referenceId: String = "",
    val actionUrl: String = "",
    val read: Boolean = false,
    val createdByEmail: String = "",
    val createdAt: Timestamp? = null
) {
    val typeEnum: DirectNotificationType get() = DirectNotificationType.fromRaw(type)
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class CloudDoubt(
    val doubtId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val examId: String = "rrb_nursing",
    val subject: String = "MSN",
    val questionId: String = "",
    val questionPreview: String = "",
    val doubtText: String = "",
    val status: String = DoubtStatus.OPEN.dbValue,
    val adminReply: String = "",
    val repliedBy: String = "",
    val repliedAt: Timestamp? = null,
    val studentReadReply: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val statusEnum: DoubtStatus get() = DoubtStatus.fromRaw(status)
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: createdAtMs
    val repliedAtMs: Long get() = repliedAt?.toDate()?.time ?: 0L
    val hasUnreadReply: Boolean get() = adminReply.isNotBlank() && !studentReadReply
}

data class DailyReminderSettingsState(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
    val smartNudge: Boolean = true
)

data class EngagementUnreadSummary(
    val unreadAnnouncements: Int = 0,
    val unreadDirectNotifications: Int = 0,
    val unreadDoubtReplies: Int = 0
) {
    val totalBellUnread: Int get() = unreadAnnouncements + unreadDirectNotifications
}

data class DoubtAskContext(
    val examId: String,
    val subject: String,
    val questionId: String = "",
    val questionPreview: String = ""
)

object EngagementLogic {
    const val MIN_TITLE_LENGTH = 2
    const val MAX_TITLE_LENGTH = 120
    const val MIN_BODY_LENGTH = 2
    const val MAX_BODY_LENGTH = 2000
    const val MAX_URL_LENGTH = 300
    const val MIN_DOUBT_LENGTH = 3
    const val MAX_DOUBT_LENGTH = 1500
    const val MAX_REPLY_LENGTH = 2500
    const val MAX_PREVIEW_LENGTH = 240

    fun formatReminderTime(hour: Int, minute: Int): String {
        val safeH = hour.coerceIn(0, 23)
        val safeM = minute.coerceIn(0, 59)
        val period = if (safeH < 12) "AM" else "PM"
        val displayHour = when {
            safeH == 0 -> 12
            safeH > 12 -> safeH - 12
            else -> safeH
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, safeM, period)
    }

    /**
     * Computes the delay in milliseconds from [nowMs] until the next occurrence of [hour]:[minute]
     * in [timeZone]. If the target time today has already passed (or is within 30 seconds), schedules
     * for the same time tomorrow.
     */
    fun calculateNextDailyTriggerDelayMs(
        nowMs: Long,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val safeH = hour.coerceIn(0, 23)
        val safeM = minute.coerceIn(0, 59)
        val target = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, safeH)
            set(Calendar.MINUTE, safeM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= nowMs + 30_000L) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - nowMs).coerceAtLeast(60_000L)
    }

    /**
     * Generates personalized title + body copy for the daily study reminder based on today's
     * daily goal progress and active streak.
     */
    fun buildDailyReminderCopy(
        goalState: DailyStudyGoalState,
        todayKey: String,
        yesterdayKey: String,
        smartNudge: Boolean,
        activeExamShortName: String
    ): Pair<String, String> {
        val examLabel = activeExamShortName.ifBlank { "Nursing" }
        if (!smartNudge) {
            return "Time for your $examLabel practice" to
                "Sharpen your clinical accuracy with a quick MCQ drill on ClinEdge."
        }
        val answeredToday = goalState.effectiveTodayCount(todayKey)
        val remaining = goalState.remainingQuestions(todayKey)
        val streak = goalState.effectiveCurrentStreak(todayKey, yesterdayKey)
        val goalMet = goalState.isGoalMetToday(todayKey)

        return when {
            goalMet -> {
                val streakLine = if (streak > 1) "$streak-day streak locked in! " else "Daily goal complete! "
                "Goal crushed today" to
                    "${streakLine}Optional 5-minute revision of bookmarks or mistakes for $examLabel?"
            }
            streak > 0 && remaining > 0 -> {
                "Protect your $streak-day streak" to
                    "Only $remaining ${if (remaining == 1) "question" else "questions"} left to hit today's $examLabel target ($answeredToday/${goalState.dailyGoalQuestions})."
            }
            answeredToday > 0 && remaining > 0 -> {
                "Almost at today's goal" to
                    "You've solved $answeredToday MCQs today — finish $remaining more to start your study streak!"
            }
            else -> {
                "Daily $examLabel study reminder" to
                    "Target: ${goalState.dailyGoalQuestions} MCQs today. Start a quick 10-minute clinical session now."
            }
        }
    }

    fun announcementMatchesExam(announcement: CloudAnnouncement, activeExamId: String): Boolean {
        val target = announcement.targetExamId.trim()
        return target.isBlank() || target.equals("all", ignoreCase = true) || target == activeExamId
    }

    private val SEMVER_REGEX = Regex("""(?i)\bv?(\d+)\.(\d+)(?:\.(\d+))?\b""")

    /**
     * Returns true if an APP_UPDATE announcement refers to a version already installed on this device
     * (e.g., announcement says "ClinEdge v2.5.1 Update Available" while installed version is "2.8.0").
     */
    fun isAppUpdateAnnouncementAlreadyInstalled(
        announcement: CloudAnnouncement,
        installedVersionName: String
    ): Boolean {
        if (announcement.categoryEnum != AnnouncementCategory.APP_UPDATE) return false
        val installed = parseSemverTuple(installedVersionName) ?: return false
        val mentioned = parseSemverTuple(announcement.title)
            ?: parseSemverTuple(announcement.body)
            ?: return false
        return compareSemver(installed, mentioned) >= 0
    }

    private fun parseSemverTuple(text: String): Triple<Int, Int, Int>? {
        val m = SEMVER_REGEX.find(text) ?: return null
        val major = m.groupValues[1].toIntOrNull() ?: return null
        val minor = m.groupValues[2].toIntOrNull() ?: return null
        val patch = m.groupValues[3].toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    private fun compareSemver(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
        if (a.first != b.first) return a.first.compareTo(b.first)
        if (a.second != b.second) return a.second.compareTo(b.second)
        return a.third.compareTo(b.third)
    }

    /**
     * Selects the top active URGENT banner for the dashboard, respecting:
     * 1. All permanently dismissed banner keys (Set<String> — prevents ping-pong when 2+ urgent alerts exist).
     * 2. Active exam targeting.
     * 3. Auto-suppressing stale APP_UPDATE banners once the user is already on an equal/newer app version.
     */
    fun selectActiveUrgentBanner(
        announcements: List<CloudAnnouncement>,
        activeExamId: String,
        dismissedBannerKeys: Set<String>,
        installedVersionName: String = ""
    ): CloudAnnouncement? {
        return announcements.firstOrNull { ann ->
            if (ann.priorityEnum != AnnouncementPriority.URGENT) return@firstOrNull false
            if (!announcementMatchesExam(ann, activeExamId)) return@firstOrNull false
            val key = ann.effectiveBannerKey
            if (key in dismissedBannerKeys) return@firstOrNull false
            if (ann.announcementId.isNotBlank() && ann.announcementId.trim() in dismissedBannerKeys) {
                return@firstOrNull false
            }
            if (installedVersionName.isNotBlank() &&
                isAppUpdateAnnouncementAlreadyInstalled(ann, installedVersionName)
            ) {
                return@firstOrNull false
            }
            true
        }
    }

    fun computeUnreadCounts(
        announcements: List<CloudAnnouncement>,
        directNotifications: List<CloudDirectNotification>,
        doubts: List<CloudDoubt>,
        activeExamId: String,
        lastSeenAnnouncementMs: Long
    ): EngagementUnreadSummary {
        val unreadAnn = announcements.count { ann ->
            announcementMatchesExam(ann, activeExamId) && ann.createdAtMs > lastSeenAnnouncementMs
        }
        val unreadDirect = directNotifications.count { !it.read }
        val unreadDoubts = doubts.count { it.hasUnreadReply }
        return EngagementUnreadSummary(
            unreadAnnouncements = unreadAnn,
            unreadDirectNotifications = unreadDirect,
            unreadDoubtReplies = unreadDoubts
        )
    }

    fun validateDoubtText(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.length < MIN_DOUBT_LENGTH -> "Please describe your doubt (at least $MIN_DOUBT_LENGTH characters)."
            trimmed.length > MAX_DOUBT_LENGTH -> "Doubt is too long (max $MAX_DOUBT_LENGTH characters)."
            else -> null
        }
    }

    fun validateAdminReply(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> "Please enter a reply before sending."
            trimmed.length > MAX_REPLY_LENGTH -> "Reply exceeds max length ($MAX_REPLY_LENGTH characters)."
            else -> null
        }
    }

    fun validateNotificationFields(title: String, body: String, actionUrl: String): String? {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        val cleanUrl = actionUrl.trim()
        return when {
            cleanTitle.length < MIN_TITLE_LENGTH -> "Title must be at least $MIN_TITLE_LENGTH characters."
            cleanTitle.length > MAX_TITLE_LENGTH -> "Title cannot exceed $MAX_TITLE_LENGTH characters."
            cleanBody.length < MIN_BODY_LENGTH -> "Message body must be at least $MIN_BODY_LENGTH characters."
            cleanBody.length > MAX_BODY_LENGTH -> "Message body cannot exceed $MAX_BODY_LENGTH characters."
            cleanUrl.length > MAX_URL_LENGTH -> "Link URL cannot exceed $MAX_URL_LENGTH characters."
            cleanUrl.isNotEmpty() && sanitizeSafeHttpUrl(cleanUrl) == null -> "Link must start with https:// or http://."
            else -> null
        }
    }

    fun sanitizeSafeHttpUrl(raw: String): String? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val lower = clean.lowercase(Locale.US)
        return if (lower.startsWith("https://") || lower.startsWith("http://")) clean else null
    }
}
