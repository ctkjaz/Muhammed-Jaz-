package com.example.data.grandtest

import androidx.annotation.Keep
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.DoubtStatus
import com.example.data.subscription.AdminUserDirectoryRow
import com.example.data.subscription.CloudAccessCode
import com.example.data.subscription.CloudPaymentRequest
import com.example.data.subscription.CloudQuestionPack
import com.example.data.subscription.CloudQuestionReport
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PaymentRequestStatus
import com.example.data.subscription.PlanTier
import com.example.data.subscription.QuestionReportStatus
import com.example.data.subscription.SubscriptionStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class GrandTestStatus(val dbValue: String, val label: String) {
    DRAFT("draft", "Draft"),
    LIVE("live", "Live"),
    CLOSED("closed", "Closed");

    companion object {
        fun fromDb(raw: String?): GrandTestStatus =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: DRAFT
    }
}

@Keep
@IgnoreExtraProperties
data class CloudGrandTest(
    @DocumentId val id: String = "",
    val testId: String = "",
    val title: String = "",
    val examId: String = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
    val weekLabel: String = "",
    val questionCount: Long = 50L,
    val durationMinutes: Long = 45L,
    val negativeMarks: Double = 0.33,
    val status: String = GrandTestStatus.DRAFT.dbValue,
    val startsAtMs: Long = 0L,
    val endsAtMs: Long = 0L,
    val createdByEmail: String = CloudSubscriptionConfig.OWNER_ADMIN_EMAIL,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: GrandTestStatus
        get() = GrandTestStatus.fromDb(status)

    val effectiveTestId: String
        get() = testId.ifBlank { id }
}

@Keep
@IgnoreExtraProperties
data class CloudGrandTestEntry(
    @DocumentId val id: String = "",
    val testId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val examId: String = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
    val netScore: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val skippedCount: Long = 0L,
    val totalQuestions: Long = 0L,
    val accuracyPercent: Long = 0L,
    val timeTakenSeconds: Long = 0L,
    @ServerTimestamp val submittedAt: Timestamp? = null
)

data class RankedLeaderboardEntry(
    val rank: Int,
    val percentile: Double,
    val entry: CloudGrandTestEntry,
    val isCurrentUser: Boolean
)

data class GrandTestLeaderboardSummary(
    val test: CloudGrandTest,
    val totalParticipants: Int,
    val averageNetScore: Double,
    val highestNetScore: Double,
    val rankedEntries: List<RankedLeaderboardEntry>,
    val currentUserRank: RankedLeaderboardEntry?
)

/**
 * Pure-JVM deterministic All-India Rank (AIR) and NTA/AIIMS percentile engine.
 *
 * Ranking order:
 *   1. Higher netScore
 *   2. Higher accuracyPercent
 *   3. Faster timeTakenSeconds (lower is better)
 *   4. Lexicographic userId for total stability
 *
 * NTA/AIIMS percentile formula:
 *   percentile = (count of candidates with score <= candidate's score / totalParticipants) * 100.0
 *   Rounded to 1 decimal place. For a single participant, percentile is 100.0.
 */
object GrandTestLeaderboardLogic {

    fun buildSummary(
        test: CloudGrandTest,
        entries: List<CloudGrandTestEntry>,
        currentUserId: String?
    ): GrandTestLeaderboardSummary {
        if (entries.isEmpty()) {
            return GrandTestLeaderboardSummary(
                test = test,
                totalParticipants = 0,
                averageNetScore = 0.0,
                highestNetScore = 0.0,
                rankedEntries = emptyList(),
                currentUserRank = null
            )
        }

        val sorted = entries.sortedWith(
            compareByDescending<CloudGrandTestEntry> { it.netScore }
                .thenByDescending { it.accuracyPercent }
                .thenBy { it.timeTakenSeconds }
                .thenBy { it.userId }
        )

        val total = sorted.size
        val averageNet = (sorted.sumOf { it.netScore } / total.toDouble()).roundTo1Decimal()
        val highestNet = sorted.first().netScore.roundTo1Decimal()

        var previousRank = 1
        val ranked = sorted.mapIndexed { index, entry ->
            val rank = if (index == 0) {
                1
            } else {
                val prev = sorted[index - 1]
                val isExactTie = prev.netScore == entry.netScore &&
                    prev.accuracyPercent == entry.accuracyPercent &&
                    prev.timeTakenSeconds == entry.timeTakenSeconds
                if (isExactTie) previousRank else index + 1
            }
            previousRank = rank

            // NTA/AIIMS formula: candidates with netScore <= this candidate's netScore
            val countAtOrBelow = sorted.count { it.netScore <= entry.netScore }
            val percentile = ((countAtOrBelow.toDouble() / total.toDouble()) * 100.0)
                .coerceIn(0.0, 100.0)
                .roundTo1Decimal()

            RankedLeaderboardEntry(
                rank = rank,
                percentile = percentile,
                entry = entry,
                isCurrentUser = !currentUserId.isNullOrBlank() && entry.userId == currentUserId
            )
        }

        return GrandTestLeaderboardSummary(
            test = test,
            totalParticipants = total,
            averageNetScore = averageNet,
            highestNetScore = highestNet,
            rankedEntries = ranked,
            currentUserRank = ranked.firstOrNull { it.isCurrentUser }
        )
    }

    private fun Double.roundTo1Decimal(): Double = (this * 10.0).roundToInt() / 10.0
}

// ============================================================================
// OWNER ADMIN LIVE ANALYTICS ENGINE (Pure-JVM, computed from real Firestore data)
// ============================================================================

data class ExamPopularityMetric(
    val examId: String,
    val label: String,
    val userCount: Int,
    val sharePercent: Int
)

data class TierCountMetric(
    val label: String,
    val count: Int,
    val sharePercent: Int
)

data class ExpiringSubscriberAlert(
    val uid: String,
    val displayName: String,
    val email: String,
    val planLabel: String,
    val isTrial: Boolean,
    val daysLeft: Int,
    val expiresAtMs: Long
)

data class AdminAnalyticsSnapshot(
    val totalUsers: Int,
    val activePaidUsers: Int,
    val singleExamPaidUsers: Int,
    val allExamPaidUsers: Int,
    val activeTrialUsers: Int,
    val freeUsers: Int,
    val expiredOrSuspendedUsers: Int,
    val trialToPaidConversionPercent: Int,
    val overallPaidPercent: Int,
    val approvedRevenueInr: Long,
    val approvedPaymentsCount: Int,
    val pendingRevenueInr: Long,
    val pendingPaymentsCount: Int,
    val rejectedPaymentsCount: Int,
    val totalDoubts: Int,
    val openDoubts: Int,
    val resolvedDoubts: Int,
    val doubtResolutionPercent: Int,
    val openQuestionReports: Int,
    val resolvedQuestionReports: Int,
    val activeAccessCodes: Int,
    val redeemedAccessCodes: Int,
    val liveQuestionPacks: Int,
    val totalOtaMcqs: Long,
    val totalAnnouncements: Int,
    val totalGrandTests: Int,
    val liveGrandTests: Int,
    val grandTestEnabled: Boolean,
    val tierDistribution: List<TierCountMetric>,
    val topExams: List<ExamPopularityMetric>,
    val expiringIn7Days: List<ExpiringSubscriberAlert>
)

object AdminAnalyticsCalculator {
    private const val DAY_MS = 86_400_000L
    private const val SEVEN_DAYS_MS = 7L * DAY_MS

    fun compute(
        users: List<AdminUserDirectoryRow>,
        paymentRequests: List<CloudPaymentRequest>,
        accessCodes: List<CloudAccessCode>,
        questionReports: List<CloudQuestionReport>,
        questionPacks: List<CloudQuestionPack>,
        doubts: List<CloudDoubt>,
        announcements: List<CloudAnnouncement>,
        grandTests: List<CloudGrandTest>,
        grandTestEnabled: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): AdminAnalyticsSnapshot {
        var singlePaid = 0
        var allPaid = 0
        var activeTrials = 0
        var freeUsers = 0
        var expiredOrSuspended = 0
        var trialEverUsedCount = 0
        var trialConvertedToPaid = 0

        val examCounter = mutableMapOf<String, Int>()
        val expiringAlerts = mutableListOf<ExpiringSubscriberAlert>()

        for (row in users) {
            val examKey = row.activeExamId.ifBlank { PlatformCatalogDefaults.EXAM_AIIMS_NORCET }
            examCounter[examKey] = (examCounter[examKey] ?: 0) + 1

            val ent = row.entitlement
            val rawTier = PlanTier.fromDb(ent?.planTier)
            val rawStatus = SubscriptionStatus.fromDb(ent?.status)
            val expiresAtMs = ent?.expiresAt?.toDate()?.time
            val isSuspended = rawStatus == SubscriptionStatus.SUSPENDED
            val isExpired = rawTier != PlanTier.FREE &&
                (rawStatus == SubscriptionStatus.EXPIRED || (expiresAtMs != null && nowMs >= expiresAtMs))

            val isPaidTier = rawTier == PlanTier.SINGLE_EXAM || rawTier == PlanTier.ALL_EXAM
            if (ent?.trialUsed == true) {
                trialEverUsedCount++
                if (isPaidTier && !isSuspended && !isExpired) {
                    trialConvertedToPaid++
                }
            }

            if (!isSuspended && !isExpired && expiresAtMs != null &&
                (rawTier == PlanTier.ALL_EXAM || rawTier == PlanTier.SINGLE_EXAM || rawTier == PlanTier.TRIAL)
            ) {
                val remainingMs = expiresAtMs - nowMs
                if (remainingMs in 1L..SEVEN_DAYS_MS) {
                    val daysLeft = ceil(remainingMs.toDouble() / DAY_MS.toDouble())
                        .toInt()
                        .coerceIn(1, 7)
                    val planLabel = when (rawTier) {
                        PlanTier.ALL_EXAM -> "All-Exam Pro"
                        PlanTier.SINGLE_EXAM -> "Single Exam Pro"
                        PlanTier.TRIAL -> "3-Day Trial"
                        PlanTier.FREE -> "Free"
                    }
                    expiringAlerts += ExpiringSubscriberAlert(
                        uid = row.uid,
                        displayName = row.displayName.ifBlank { "Unnamed Student" },
                        email = row.email.ifBlank { row.uid },
                        planLabel = planLabel,
                        isTrial = rawTier == PlanTier.TRIAL,
                        daysLeft = daysLeft,
                        expiresAtMs = expiresAtMs
                    )
                }
            }

            when {
                isSuspended || isExpired -> expiredOrSuspended++
                rawTier == PlanTier.ALL_EXAM -> allPaid++
                rawTier == PlanTier.SINGLE_EXAM -> singlePaid++
                rawTier == PlanTier.TRIAL -> activeTrials++
                else -> freeUsers++
            }
        }

        val totalUsers = users.size
        val activePaid = singlePaid + allPaid

        // Trial-to-paid conversion: if trial users exist, measure paid among trial-takers (or overall paid funnel)
        val trialToPaidPct = when {
            trialEverUsedCount > 0 -> ((trialConvertedToPaid * 100.0) / trialEverUsedCount.toDouble()).roundToInt().coerceIn(0, 100)
            totalUsers > 0 -> ((activePaid * 100.0) / totalUsers.toDouble()).roundToInt().coerceIn(0, 100)
            else -> 0
        }
        val overallPaidPct = if (totalUsers > 0) {
            ((activePaid * 100.0) / totalUsers.toDouble()).roundToInt().coerceIn(0, 100)
        } else 0

        val approvedPayments = paymentRequests.filter { it.parsedStatus == PaymentRequestStatus.APPROVED }
        val pendingPayments = paymentRequests.filter { it.parsedStatus == PaymentRequestStatus.PENDING }
        val rejectedPayments = paymentRequests.filter { it.parsedStatus == PaymentRequestStatus.REJECTED }

        val approvedRevenue = approvedPayments.sumOf { it.amountInr.coerceAtLeast(0L) }
        val pendingRevenue = pendingPayments.sumOf { it.amountInr.coerceAtLeast(0L) }

        val openDoubts = doubts.count { it.statusEnum == DoubtStatus.OPEN }
        val resolvedDoubts = doubts.size - openDoubts
        val doubtResolutionPct = if (doubts.isNotEmpty()) {
            ((resolvedDoubts * 100.0) / doubts.size.toDouble()).roundToInt().coerceIn(0, 100)
        } else 100

        val openReports = questionReports.count { it.parsedStatus == QuestionReportStatus.OPEN }
        val resolvedReports = questionReports.size - openReports

        val activeCodes = accessCodes.count { it.status.equals("active", ignoreCase = true) }
        val redeemedCodes = accessCodes.count { it.status.equals("redeemed", ignoreCase = true) }

        val livePacks = questionPacks.filter { it.status.equals("published", ignoreCase = true) }
        val totalOtaMcqs = livePacks.sumOf { it.questionCount.coerceAtLeast(0L) }

        val liveGrandTests = grandTests.count { it.parsedStatus == GrandTestStatus.LIVE }

        val safeDenom = totalUsers.coerceAtLeast(1).toDouble()
        val tierDistribution = listOf(
            TierCountMetric("All-Exam Pro", allPaid, ((allPaid * 100.0) / safeDenom).roundToInt()),
            TierCountMetric("Single Exam Pro", singlePaid, ((singlePaid * 100.0) / safeDenom).roundToInt()),
            TierCountMetric("3-Day Trial", activeTrials, ((activeTrials * 100.0) / safeDenom).roundToInt()),
            TierCountMetric("Free Tier", freeUsers, ((freeUsers * 100.0) / safeDenom).roundToInt()),
            TierCountMetric("Expired / Suspended", expiredOrSuspended, ((expiredOrSuspended * 100.0) / safeDenom).roundToInt())
        )

        val topExams = examCounter.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { (examId, count) ->
                ExamPopularityMetric(
                    examId = examId,
                    label = EntitlementEvaluator.shortExamLabel(examId),
                    userCount = count,
                    sharePercent = ((count * 100.0) / safeDenom).roundToInt().coerceIn(0, 100)
                )
            }

        return AdminAnalyticsSnapshot(
            totalUsers = totalUsers,
            activePaidUsers = activePaid,
            singleExamPaidUsers = singlePaid,
            allExamPaidUsers = allPaid,
            activeTrialUsers = activeTrials,
            freeUsers = freeUsers,
            expiredOrSuspendedUsers = expiredOrSuspended,
            trialToPaidConversionPercent = trialToPaidPct,
            overallPaidPercent = overallPaidPct,
            approvedRevenueInr = approvedRevenue,
            approvedPaymentsCount = approvedPayments.size,
            pendingRevenueInr = pendingRevenue,
            pendingPaymentsCount = pendingPayments.size,
            rejectedPaymentsCount = rejectedPayments.size,
            totalDoubts = doubts.size,
            openDoubts = openDoubts,
            resolvedDoubts = resolvedDoubts,
            doubtResolutionPercent = doubtResolutionPct,
            openQuestionReports = openReports,
            resolvedQuestionReports = resolvedReports,
            activeAccessCodes = activeCodes,
            redeemedAccessCodes = redeemedCodes,
            liveQuestionPacks = livePacks.size,
            totalOtaMcqs = totalOtaMcqs,
            totalAnnouncements = announcements.size,
            totalGrandTests = grandTests.size,
            liveGrandTests = liveGrandTests,
            grandTestEnabled = grandTestEnabled,
            tierDistribution = tierDistribution,
            topExams = topExams,
            expiringIn7Days = expiringAlerts.sortedWith(
                compareBy<ExpiringSubscriberAlert> { it.expiresAtMs }.thenBy { it.uid }
            )
        )
    }
}
