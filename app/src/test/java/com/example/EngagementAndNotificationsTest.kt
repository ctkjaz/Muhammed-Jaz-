package com.example

import com.example.data.model.DailyStudyGoalState
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDirectNotification
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.DoubtStatus
import com.example.data.notifications.EngagementLogic
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class EngagementAndNotificationsTest {

    @Test
    fun `formatReminderTime formats midnight noon and evening cleanly`() {
        assertEquals("12:05 AM", EngagementLogic.formatReminderTime(0, 5))
        assertEquals("7:00 AM", EngagementLogic.formatReminderTime(7, 0))
        assertEquals("12:00 PM", EngagementLogic.formatReminderTime(12, 0))
        assertEquals("1:30 PM", EngagementLogic.formatReminderTime(13, 30))
        assertEquals("8:30 PM", EngagementLogic.formatReminderTime(20, 30))
        assertEquals("11:59 PM", EngagementLogic.formatReminderTime(23, 59))
    }

    @Test
    fun `calculateNextDailyTriggerDelayMs schedules same day if target time is ahead`() {
        val tz = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MAY, 10, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayMs = EngagementLogic.calculateNextDailyTriggerDelayMs(
            nowMs = cal.timeInMillis,
            hour = 20,
            minute = 30,
            timeZone = tz
        )
        // 10h 30m = 37,800,000 ms
        assertEquals(10L * 3600_000L + 30L * 60_000L, delayMs)
    }

    @Test
    fun `calculateNextDailyTriggerDelayMs rolls over to tomorrow if time already passed`() {
        val tz = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MAY, 10, 21, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayMs = EngagementLogic.calculateNextDailyTriggerDelayMs(
            nowMs = cal.timeInMillis,
            hour = 20,
            minute = 0,
            timeZone = tz
        )
        // 23 hours later
        assertEquals(23L * 3600_000L, delayMs)
    }

    @Test
    fun `buildDailyReminderCopy adapts to smart nudge streak and goal progress`() {
        val today = "2026-05-10"
        val yesterday = "2026-05-09"

        // Smart nudge OFF -> standard copy
        val standard = EngagementLogic.buildDailyReminderCopy(
            goalState = DailyStudyGoalState(dailyGoalQuestions = 30),
            todayKey = today,
            yesterdayKey = yesterday,
            smartNudge = false,
            activeExamShortName = "AIIMS NORCET"
        )
        assertTrue(standard.first.contains("AIIMS NORCET"))

        // Active streak at risk -> streak protection copy
        val streakAtRisk = EngagementLogic.buildDailyReminderCopy(
            goalState = DailyStudyGoalState(
                dailyGoalQuestions = 30,
                todayDateKey = today,
                todayAnsweredCount = 12,
                currentStreakDays = 6,
                lastGoalMetDateKey = yesterday
            ),
            todayKey = today,
            yesterdayKey = yesterday,
            smartNudge = true,
            activeExamShortName = "RRB Nursing"
        )
        assertTrue(streakAtRisk.first.contains("6-day streak"))
        assertTrue(streakAtRisk.second.contains("18 questions"))

        // Goal already completed today -> celebratory revision copy
        val goalDone = EngagementLogic.buildDailyReminderCopy(
            goalState = DailyStudyGoalState(
                dailyGoalQuestions = 20,
                todayDateKey = today,
                todayAnsweredCount = 24,
                currentStreakDays = 4,
                lastGoalMetDateKey = today
            ),
            todayKey = today,
            yesterdayKey = yesterday,
            smartNudge = true,
            activeExamShortName = "ESIC"
        )
        assertTrue(goalDone.first.contains("Goal crushed"))
        assertTrue(goalDone.second.contains("4-day streak"))
    }

    @Test
    fun `announcementMatchesExam respects all and specific exam ids`() {
        val allAnn = CloudAnnouncement(announcementId = "a1", targetExamId = "all")
        val norcetAnn = CloudAnnouncement(announcementId = "a2", targetExamId = "aiims_norcet")

        assertTrue(EngagementLogic.announcementMatchesExam(allAnn, "rrb_nursing"))
        assertTrue(EngagementLogic.announcementMatchesExam(norcetAnn, "aiims_norcet"))
        assertFalse(EngagementLogic.announcementMatchesExam(norcetAnn, "rrb_nursing"))
    }

    @Test
    fun `computeUnreadCounts tallies matching announcements direct alerts and unread doubt replies`() {
        val t1 = Timestamp(Date(1_000_000L))
        val t2 = Timestamp(Date(2_000_000L))
        val announcements = listOf(
            CloudAnnouncement(announcementId = "a1", targetExamId = "all", createdAt = t1),
            CloudAnnouncement(announcementId = "a2", targetExamId = "rrb_nursing", createdAt = t2),
            CloudAnnouncement(announcementId = "a3", targetExamId = "aiims_norcet", createdAt = t2)
        )
        val directs = listOf(
            CloudDirectNotification(notificationId = "n1", read = false),
            CloudDirectNotification(notificationId = "n2", read = true),
            CloudDirectNotification(notificationId = "n3", read = false)
        )
        val doubts = listOf(
            CloudDoubt(doubtId = "d1", status = DoubtStatus.ANSWERED.dbValue, adminReply = "Check dosage", studentReadReply = false),
            CloudDoubt(doubtId = "d2", status = DoubtStatus.ANSWERED.dbValue, adminReply = "Correct", studentReadReply = true),
            CloudDoubt(doubtId = "d3", status = DoubtStatus.OPEN.dbValue, adminReply = "", studentReadReply = true)
        )

        val summary = EngagementLogic.computeUnreadCounts(
            announcements = announcements,
            directNotifications = directs,
            doubts = doubts,
            activeExamId = "rrb_nursing",
            lastSeenAnnouncementMs = 1_500_000L
        )
        assertEquals(1, summary.unreadAnnouncements) // Only a2 matches rrb_nursing AND > watermark
        assertEquals(2, summary.unreadDirectNotifications)
        assertEquals(1, summary.unreadDoubtReplies)
        assertEquals(3, summary.totalBellUnread)
    }

    @Test
    fun `validation helpers enforce bounds and reject unsafe url schemes`() {
        assertNotNull(EngagementLogic.validateDoubtText("hi"))
        assertNull(EngagementLogic.validateDoubtText("Why is atropine contraindicated in glaucoma?"))
        assertNotNull(EngagementLogic.validateAdminReply("   "))
        assertNull(EngagementLogic.validateAdminReply("Because it increases intraocular pressure via mydriasis."))

        assertNull(EngagementLogic.sanitizeSafeHttpUrl("javascript:alert(1)"))
        assertNull(EngagementLogic.sanitizeSafeHttpUrl("file:///etc/passwd"))
        assertEquals("https://aiimsexams.ac.in", EngagementLogic.sanitizeSafeHttpUrl("  https://aiimsexams.ac.in  "))

        assertNotNull(EngagementLogic.validateNotificationFields("A", "Valid body", ""))
        assertNotNull(EngagementLogic.validateNotificationFields("Valid Title", "Valid body", "javascript:evil()"))
        assertNull(EngagementLogic.validateNotificationFields("NORCET Alert", "Registration opens tomorrow.", "https://aiimsexams.ac.in"))
    }
}
