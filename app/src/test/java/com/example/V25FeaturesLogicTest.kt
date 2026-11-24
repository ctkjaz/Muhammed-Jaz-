package com.example

import com.example.data.grandtest.AdminAnalyticsCalculator
import com.example.data.grandtest.CloudGrandTest
import com.example.data.grandtest.CloudGrandTestEntry
import com.example.data.grandtest.GrandTestLeaderboardLogic
import com.example.data.grandtest.GrandTestStatus
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.DoubtStatus
import com.example.data.subscription.AdminUserDirectoryRow
import com.example.data.subscription.CloudAccessCode
import com.example.data.subscription.CloudPaymentRequest
import com.example.data.subscription.CloudQuestionPack
import com.example.data.subscription.CloudQuestionReport
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.CloudUserEntitlement
import com.example.data.subscription.PaymentRequestStatus
import com.example.data.subscription.PlanTier
import com.example.data.subscription.QuestionReportStatus
import com.example.data.subscription.SubscriptionStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class V25FeaturesLogicTest {

    @Test
    fun grandTestMasterSwitchDefaultsToDormantFalse() {
        val defaultConfig = CloudSubscriptionConfig()
        assertFalse(
            "Weekly All-India Grand Test must default to dormant (false) until Admin enables it",
            defaultConfig.grandTestEnabled
        )
    }

    @Test
    fun leaderboardRanksByNetScoreAccuracyAndTimeWithNtaPercentile() {
        val test = CloudGrandTest(
            id = "gt_1",
            testId = "gt_1",
            title = "All-India NORCET Grand Test #1",
            examId = "aiims_norcet",
            weekLabel = "Week 1",
            questionCount = 50,
            durationMinutes = 45,
            status = GrandTestStatus.LIVE.dbValue
        )
        val entries = listOf(
            // Tie on netScore=40.0, lower accuracy -> rank 2
            CloudGrandTestEntry(
                testId = "gt_1", userId = "u_b", studentName = "Bhanu",
                netScore = 40.0, correctCount = 42, wrongCount = 6, skippedCount = 2,
                totalQuestions = 50, accuracyPercent = 88, timeTakenSeconds = 1800
            ),
            // Tie on netScore=40.0, higher accuracy -> rank 1
            CloudGrandTestEntry(
                testId = "gt_1", userId = "u_a", studentName = "Ananya",
                netScore = 40.0, correctCount = 41, wrongCount = 3, skippedCount = 6,
                totalQuestions = 50, accuracyPercent = 93, timeTakenSeconds = 2000
            ),
            // Lower net score, faster time -> rank 3
            CloudGrandTestEntry(
                testId = "gt_1", userId = "u_c", studentName = "Charu",
                netScore = 30.0, correctCount = 32, wrongCount = 6, skippedCount = 12,
                totalQuestions = 50, accuracyPercent = 84, timeTakenSeconds = 1500
            ),
            // Same netScore=30.0, same accuracy=84, slower time -> rank 4
            CloudGrandTestEntry(
                testId = "gt_1", userId = "u_d", studentName = "Deepa",
                netScore = 30.0, correctCount = 32, wrongCount = 6, skippedCount = 12,
                totalQuestions = 50, accuracyPercent = 84, timeTakenSeconds = 1900
            )
        )

        val summary = GrandTestLeaderboardLogic.buildSummary(test, entries, currentUserId = "u_c")
        assertEquals(4, summary.totalParticipants)
        assertEquals(40.0, summary.highestNetScore, 0.01)
        assertEquals(35.0, summary.averageNetScore, 0.01)

        assertEquals(listOf("u_a", "u_b", "u_c", "u_d"), summary.rankedEntries.map { it.entry.userId })
        assertEquals(listOf(1, 2, 3, 4), summary.rankedEntries.map { it.rank })
        // NTA percentile: both 40.0 scorers beat/tie 4/4 = 100.0; both 30.0 scorers beat/tie 2/4 = 50.0
        assertEquals(100.0, summary.rankedEntries[0].percentile, 0.01)
        assertEquals(100.0, summary.rankedEntries[1].percentile, 0.01)
        assertEquals(50.0, summary.rankedEntries[2].percentile, 0.01)

        val myRank = summary.currentUserRank
        assertNotNull(myRank)
        assertEquals(3, myRank!!.rank)
        assertTrue(myRank.isCurrentUser)
    }

    @Test
    fun leaderboardHandlesSingleParticipantAndEmptyListGracefully() {
        val test = CloudGrandTest(id = "gt_2", testId = "gt_2", title = "Mini Grand Test")

        val empty = GrandTestLeaderboardLogic.buildSummary(test, emptyList(), "u_1")
        assertEquals(0, empty.totalParticipants)
        assertNull(empty.currentUserRank)

        val solo = GrandTestLeaderboardLogic.buildSummary(
            test = test,
            entries = listOf(
                CloudGrandTestEntry(
                    testId = "gt_2", userId = "u_1", studentName = "Solo Aspirant",
                    netScore = 25.0, correctCount = 25, wrongCount = 0, skippedCount = 0,
                    totalQuestions = 25, accuracyPercent = 100, timeTakenSeconds = 600
                )
            ),
            currentUserId = "u_1"
        )
        assertEquals(1, solo.totalParticipants)
        assertEquals(1, solo.currentUserRank?.rank)
        assertEquals(100.0, solo.currentUserRank?.percentile ?: 0.0, 0.01)
    }

    @Test
    fun adminAnalyticsCalculatesTiersConversionRevenueExamPopularityAndSla() {
        val nowMs = 1_750_000_000_000L
        val futureTs = Timestamp(Date(nowMs + 10L * 86_400_000L))
        val pastTs = Timestamp(Date(nowMs - 2L * 86_400_000L))

        val users = listOf(
            // All-Exam Pro (converted from trial)
            AdminUserDirectoryRow(
                uid = "u1", displayName = "User 1", email = "u1@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Pixel", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u1", planTier = PlanTier.ALL_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue, expiresAt = futureTs, trialUsed = true
                ),
                profile = null
            ),
            // Single-Exam Pro (direct paid)
            AdminUserDirectoryRow(
                uid = "u2", displayName = "User 2", email = "u2@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Samsung", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u2", planTier = PlanTier.SINGLE_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue, expiresAt = futureTs, trialUsed = false
                ),
                profile = null
            ),
            // Active 3-Day Trial
            AdminUserDirectoryRow(
                uid = "u3", displayName = "User 3", email = "u3@test.com", phoneNumber = "",
                activeExamId = "rrb_nursing", activeDeviceName = "OnePlus", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u3", planTier = PlanTier.TRIAL.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue, expiresAt = futureTs, trialUsed = true
                ),
                profile = null
            ),
            // Expired Single-Exam
            AdminUserDirectoryRow(
                uid = "u4", displayName = "User 4", email = "u4@test.com", phoneNumber = "",
                activeExamId = "pgimer_nursing", activeDeviceName = "Xiaomi", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u4", planTier = PlanTier.SINGLE_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue, expiresAt = pastTs, trialUsed = false
                ),
                profile = null
            ),
            // Free user
            AdminUserDirectoryRow(
                uid = "u5", displayName = "User 5", email = "u5@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Vivo", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u5", planTier = PlanTier.FREE.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue
                ),
                profile = null
            )
        )

        val payments = listOf(
            CloudPaymentRequest(requestId = "p1", amountInr = 499, status = PaymentRequestStatus.APPROVED.dbValue),
            CloudPaymentRequest(requestId = "p2", amountInr = 249, status = PaymentRequestStatus.APPROVED.dbValue),
            CloudPaymentRequest(requestId = "p3", amountInr = 199, status = PaymentRequestStatus.PENDING.dbValue),
            CloudPaymentRequest(requestId = "p4", amountInr = 99, status = PaymentRequestStatus.REJECTED.dbValue)
        )
        val codes = listOf(
            CloudAccessCode(code = "CE-1", status = "active"),
            CloudAccessCode(code = "CE-2", status = "redeemed")
        )
        val reports = listOf(
            CloudQuestionReport(reportId = "r1", status = QuestionReportStatus.OPEN.dbValue),
            CloudQuestionReport(reportId = "r2", status = QuestionReportStatus.RESOLVED.dbValue)
        )
        val packs = listOf(
            CloudQuestionPack(packId = "pk1", status = "published", questionCount = 120),
            CloudQuestionPack(packId = "pk2", status = "draft", questionCount = 50)
        )
        val doubts = listOf(
            CloudDoubt(doubtId = "d1", status = DoubtStatus.OPEN.dbValue),
            CloudDoubt(doubtId = "d2", status = DoubtStatus.ANSWERED.dbValue),
            CloudDoubt(doubtId = "d3", status = DoubtStatus.CLOSED.dbValue),
            CloudDoubt(doubtId = "d4", status = DoubtStatus.ANSWERED.dbValue)
        )
        val announcements = listOf(CloudAnnouncement(announcementId = "a1"))
        val grandTests = listOf(
            CloudGrandTest(testId = "g1", status = GrandTestStatus.LIVE.dbValue),
            CloudGrandTest(testId = "g2", status = GrandTestStatus.DRAFT.dbValue)
        )

        val snapshot = AdminAnalyticsCalculator.compute(
            users = users,
            paymentRequests = payments,
            accessCodes = codes,
            questionReports = reports,
            questionPacks = packs,
            doubts = doubts,
            announcements = announcements,
            grandTests = grandTests,
            grandTestEnabled = false,
            nowMs = nowMs
        )

        assertEquals(5, snapshot.totalUsers)
        assertEquals(2, snapshot.activePaidUsers)
        assertEquals(1, snapshot.allExamPaidUsers)
        assertEquals(1, snapshot.singleExamPaidUsers)
        assertEquals(1, snapshot.activeTrialUsers)
        assertEquals(1, snapshot.freeUsers)
        assertEquals(1, snapshot.expiredOrSuspendedUsers)
        // 1 converted out of 2 trial-ever users = 50%
        assertEquals(50, snapshot.trialToPaidConversionPercent)
        // 2 active paid out of 5 users = 40%
        assertEquals(40, snapshot.overallPaidPercent)
        assertEquals(748L, snapshot.approvedRevenueInr)
        assertEquals(2, snapshot.approvedPaymentsCount)
        assertEquals(199L, snapshot.pendingRevenueInr)
        assertEquals(1, snapshot.pendingPaymentsCount)
        assertEquals(1, snapshot.rejectedPaymentsCount)
        assertEquals(75, snapshot.doubtResolutionPercent)
        assertEquals(1, snapshot.openQuestionReports)
        assertEquals(1, snapshot.resolvedQuestionReports)
        assertEquals(1, snapshot.activeAccessCodes)
        assertEquals(1, snapshot.redeemedAccessCodes)
        assertEquals(1, snapshot.liveQuestionPacks)
        assertEquals(120L, snapshot.totalOtaMcqs)
        assertEquals(2, snapshot.totalGrandTests)
        assertEquals(1, snapshot.liveGrandTests)
        assertFalse(snapshot.grandTestEnabled)
        assertEquals("aiims_norcet", snapshot.topExams.first().examId)
        assertEquals(3, snapshot.topExams.first().userCount)
        assertTrue(snapshot.expiringIn7Days.isEmpty())
    }

    @Test
    fun adminAnalyticsDetectsExpiringWithin7DaysCohortInAscendingOrder() {
        val nowMs = 1_750_000_000_000L
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L

        val users = listOf(
            // Active Single-Exam expiring in exactly 7 days -> INCLUDED (3rd, 7d left)
            AdminUserDirectoryRow(
                uid = "u_7d", displayName = "Seven Day", email = "seven@test.com", phoneNumber = "",
                activeExamId = "rrb_nursing", activeDeviceName = "Pixel", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_7d", planTier = PlanTier.SINGLE_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue,
                    expiresAt = Timestamp(Date(nowMs + 7L * dayMs))
                ),
                profile = null
            ),
            // Active All-Exam expiring in 18 hours -> INCLUDED (1st, 1d left)
            AdminUserDirectoryRow(
                uid = "u_18h", displayName = "Urgent Pro", email = "urgent@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Pixel", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_18h", planTier = PlanTier.ALL_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue,
                    expiresAt = Timestamp(Date(nowMs + 18L * hourMs))
                ),
                profile = null
            ),
            // Active Trial expiring in 2.5 days -> INCLUDED (2nd, 3d left, isTrial = true)
            AdminUserDirectoryRow(
                uid = "u_trial", displayName = "Trial Nurse", email = "trial@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "OnePlus", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_trial", planTier = PlanTier.TRIAL.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue,
                    expiresAt = Timestamp(Date(nowMs + 2L * dayMs + 12L * hourMs))
                ),
                profile = null
            ),
            // Active All-Exam expiring in 8 days -> EXCLUDED (> 7d)
            AdminUserDirectoryRow(
                uid = "u_8d", displayName = "Safe Pro", email = "safe@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Samsung", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_8d", planTier = PlanTier.ALL_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue,
                    expiresAt = Timestamp(Date(nowMs + 8L * dayMs))
                ),
                profile = null
            ),
            // Suspended user expiring in 2 days -> EXCLUDED
            AdminUserDirectoryRow(
                uid = "u_susp", displayName = "Suspended", email = "susp@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Xiaomi", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_susp", planTier = PlanTier.ALL_EXAM.dbValue,
                    status = SubscriptionStatus.SUSPENDED.dbValue,
                    expiresAt = Timestamp(Date(nowMs + 2L * dayMs))
                ),
                profile = null
            ),
            // Already expired 1 hour ago -> EXCLUDED
            AdminUserDirectoryRow(
                uid = "u_exp", displayName = "Expired", email = "exp@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Vivo", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_exp", planTier = PlanTier.SINGLE_EXAM.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue,
                    expiresAt = Timestamp(Date(nowMs - hourMs))
                ),
                profile = null
            ),
            // Free user -> EXCLUDED
            AdminUserDirectoryRow(
                uid = "u_free", displayName = "Free", email = "free@test.com", phoneNumber = "",
                activeExamId = "aiims_norcet", activeDeviceName = "Realme", lastDeviceHash = "",
                entitlement = CloudUserEntitlement(
                    userId = "u_free", planTier = PlanTier.FREE.dbValue,
                    status = SubscriptionStatus.ACTIVE.dbValue
                ),
                profile = null
            )
        )

        val snapshot = AdminAnalyticsCalculator.compute(
            users = users,
            paymentRequests = emptyList(),
            accessCodes = emptyList(),
            questionReports = emptyList(),
            questionPacks = emptyList(),
            doubts = emptyList(),
            announcements = emptyList(),
            grandTests = emptyList(),
            grandTestEnabled = false,
            nowMs = nowMs
        )

        assertEquals(listOf("u_18h", "u_trial", "u_7d"), snapshot.expiringIn7Days.map { it.uid })
        assertEquals(listOf(1, 3, 7), snapshot.expiringIn7Days.map { it.daysLeft })
        assertEquals(listOf("All-Exam Pro", "3-Day Trial", "Single Exam Pro"), snapshot.expiringIn7Days.map { it.planLabel })
        assertEquals(listOf(false, true, false), snapshot.expiringIn7Days.map { it.isTrial })
    }
}
