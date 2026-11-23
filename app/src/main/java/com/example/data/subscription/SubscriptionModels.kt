package com.example.data.subscription

import androidx.annotation.Keep
import com.example.data.cloud.CloudUserProfile
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionSourceType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.net.URLEncoder
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

enum class PlanTier(val dbValue: String, val displayLabel: String) {
    FREE("free", "Free Plan"),
    TRIAL("trial", "3-Day Free Trial"),
    SINGLE_EXAM("single_exam", "Single Exam Pro"),
    ALL_EXAM("all_exam", "All-Exam Nursing Prep");

    companion object {
        fun fromDb(raw: String?): PlanTier =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: FREE
    }
}

enum class PlanTenure(val days: Int, val shortCode: String, val label: String) {
    ONE_MONTH(30, "1M", "1 Month"),
    THREE_MONTHS(90, "3M", "3 Months"),
    SIX_MONTHS(180, "6M", "6 Months"),
    ONE_YEAR(365, "1Y", "1 Year");

    companion object {
        fun fromDays(days: Int): PlanTenure =
            entries.firstOrNull { it.days == days } ?: ONE_MONTH
    }
}

enum class SubscriptionStatus(val dbValue: String) {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    EXPIRED("expired");

    companion object {
        fun fromDb(raw: String?): SubscriptionStatus =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: ACTIVE
    }
}

@Keep
@IgnoreExtraProperties
data class CloudSubscriptionConfig(
    val singleExam1M: Long = 99L,
    val singleExam3M: Long = 249L,
    val singleExam6M: Long = 449L,
    val singleExam1Y: Long = 799L,
    val allExam1M: Long = 199L,
    val allExam3M: Long = 499L,
    val allExam6M: Long = 899L,
    val allExam1Y: Long = 1499L,
    val freeDailyQuestionLimit: Long = 15L,
    val trialDays: Long = 3L,
    val supportWhatsAppNumber: String = "+919876543210",
    val supportEmail: String = OWNER_ADMIN_EMAIL,
    val catalogVersion: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun priceFor(tier: PlanTier, tenure: PlanTenure): Int {
        return when (tier) {
            PlanTier.ALL_EXAM -> when (tenure) {
                PlanTenure.ONE_MONTH -> allExam1M.toInt()
                PlanTenure.THREE_MONTHS -> allExam3M.toInt()
                PlanTenure.SIX_MONTHS -> allExam6M.toInt()
                PlanTenure.ONE_YEAR -> allExam1Y.toInt()
            }
            else -> when (tenure) {
                PlanTenure.ONE_MONTH -> singleExam1M.toInt()
                PlanTenure.THREE_MONTHS -> singleExam3M.toInt()
                PlanTenure.SIX_MONTHS -> singleExam6M.toInt()
                PlanTenure.ONE_YEAR -> singleExam1Y.toInt()
            }
        }
    }

    companion object {
        const val OWNER_ADMIN_EMAIL = "jazsaleem2004@gmail.com"
        const val DEFAULT_CONFIG_DOC_ID = "subscriptionConfig"
    }
}

@Keep
@IgnoreExtraProperties
data class CloudUserEntitlement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val planTier: String = PlanTier.FREE.dbValue,
    val unlockedExamIds: List<String> = emptyList(),
    val status: String = SubscriptionStatus.ACTIVE.dbValue,
    val startsAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val trialUsed: Boolean = false,
    val trialDeviceHash: String = "",
    val lastCodeId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAccessCode(
    @DocumentId val id: String = "",
    val code: String = "",
    val planTier: String = PlanTier.ALL_EXAM.dbValue,
    val unlockedExamIds: List<String> = listOf("all"),
    val tenureDays: Long = 30L,
    val status: String = "active",
    val buyerNote: String = "",
    val createdByEmail: String = CloudSubscriptionConfig.OWNER_ADMIN_EMAIL,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val redeemedByUid: String? = null,
    val redeemedByEmail: String? = null,
    val redeemedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudDeviceTrialLock(
    @DocumentId val id: String = "",
    val deviceHash: String = "",
    val shortDeviceCode: String = "",
    val ownerUid: String = "",
    val maskedEmail: String = "",
    val deviceName: String = "",
    @ServerTimestamp val claimedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudQuestionPack(
    @DocumentId val id: String = "",
    val packId: String = "",
    val title: String = "",
    val examId: String = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
    val stage: String = "all",
    val subject: String = "",
    val questionCount: Long = 0L,
    @get:PropertyName("isPremiumOnly")
    @PropertyName("isPremiumOnly")
    val isPremiumOnly: Boolean = true,
    val status: String = "published",
    val version: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudPackQuestion(
    @DocumentId val id: String = "",
    val packId: String = "",
    val packStatus: String = "published",
    val examId: String = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
    val category: String = "nur",
    val shiftYear: String = "",
    val subject: String = "MSN",
    val difficulty: String = QuestionDifficulty.MEDIUM.label,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val sourceStatus: String = Question.SOURCE_VERIFIED,
    val sourceNote: String = "",
    val topicId: String = "",
    val sourceType: String = QuestionSourceType.PRACTICE.dbValue,
    val examYear: String = "",
    val examShift: String = "",
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toDomainQuestion(): Question? {
        val cleanText = questionText.trim()
        if (cleanText.isBlank() || options.size < 2) return null
        val safeCorrect = correctIndex.toInt().coerceIn(0, options.lastIndex)
        val cleanId = "OTA_${packId}_${id.ifBlank { sortOrder.toString() }}"
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(96)
        return Question(
            id = cleanId,
            category = category.ifBlank { "nur" },
            shiftYear = shiftYear,
            subject = subject.ifBlank { "MSN" },
            difficulty = QuestionDifficulty.fromRaw(difficulty),
            questionText = cleanText,
            options = options,
            correctIndex = safeCorrect,
            explanation = explanation,
            isCustom = false,
            sourceStatus = if (sourceStatus == Question.SOURCE_VERIFIED) Question.SOURCE_VERIFIED else Question.SOURCE_UNVERIFIED,
            sourceNote = sourceNote,
            topicId = topicId.ifBlank { PlatformCatalogDefaults.defaultTopicIdFor(subject.ifBlank { "MSN" }) },
            sourceType = QuestionSourceType.fromDbValue(sourceType) ?: QuestionSourceType.PRACTICE,
            examYear = examYear,
            examShift = examShift
        )
    }
}

data class AdminUserDirectoryRow(
    val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val activeExamId: String,
    val activeDeviceName: String,
    val lastDeviceHash: String,
    val entitlement: CloudUserEntitlement?,
    val profile: CloudUserProfile?
)

data class PaywallPromptState(
    val title: String,
    val message: String,
    val lockedFeatureTag: String,
    val targetExamId: String? = null
)

data class DeviceLockConflictState(
    val shortDeviceCode: String,
    val maskedOwnerEmail: String,
    val deviceName: String
)

data class SubscriptionUiState(
    val isAdmin: Boolean = false,
    val rawTier: PlanTier = PlanTier.FREE,
    val effectiveTier: PlanTier = PlanTier.FREE,
    val rawStatus: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val isExpired: Boolean = false,
    val isSuspended: Boolean = false,
    val unlockedExamIds: Set<String> = emptySet(),
    val expiresAtMs: Long? = null,
    val daysRemaining: Int = 0,
    val trialUsed: Boolean = false,
    val deviceShortCode: String = "",
    val deviceHash: String = "",
    val dailyFreeLimit: Int = 15,
    val dailyFreeUsed: Int = 0,
    val freeSampleMockUsed: Boolean = false,
    val config: CloudSubscriptionConfig = CloudSubscriptionConfig(),
    val isLoaded: Boolean = false,
    val planDisplayName: String = "ClinEdge Free",
    val expirySummaryLabel: String = "15 Qs/day • 1 sample mock"
) {
    val hasAnyActivePaidOrTrial: Boolean
        get() = effectiveTier == PlanTier.ALL_EXAM ||
            effectiveTier == PlanTier.TRIAL ||
            effectiveTier == PlanTier.SINGLE_EXAM

    val hasAllExamAccess: Boolean
        get() = isAdmin ||
            effectiveTier == PlanTier.ALL_EXAM ||
            effectiveTier == PlanTier.TRIAL ||
            "all" in unlockedExamIds

    val remainingFreeQuestionsToday: Int
        get() = (dailyFreeLimit - dailyFreeUsed).coerceAtLeast(0)

    fun hasPremiumForExam(examId: String): Boolean {
        if (isAdmin) return true
        if (hasAllExamAccess) return true
        return effectiveTier == PlanTier.SINGLE_EXAM && examId in unlockedExamIds
    }

    fun canAccessPyq(examId: String): Boolean = hasPremiumForExam(examId)

    fun canAccessCrossExamPractice(): Boolean = hasAllExamAccess

    fun canExportPdf(examId: String): Boolean = hasPremiumForExam(examId)

    fun canStartFullCbtMock(examId: String): Boolean = hasPremiumForExam(examId)

    fun canStartSampleMock(): Boolean = !freeSampleMockUsed

    fun canAnswerPracticeQuestion(examId: String): Boolean {
        if (hasPremiumForExam(examId)) return true
        return dailyFreeUsed < dailyFreeLimit
    }
}

sealed interface TrialClaimResult {
    data class Success(val expiresAtMs: Long) : TrialClaimResult
    data class DeviceAlreadyLocked(
        val shortDeviceCode: String,
        val maskedEmail: String,
        val deviceName: String
    ) : TrialClaimResult
    data object TrialAlreadyUsedByAccount : TrialClaimResult
    data class Error(val message: String) : TrialClaimResult
}

sealed interface CodeRedeemResult {
    data class Success(val planLabel: String, val expiresAtMs: Long) : CodeRedeemResult
    data class InvalidOrUsed(val message: String) : CodeRedeemResult
    data class Error(val message: String) : CodeRedeemResult
}

object EntitlementEvaluator {
    const val MILLIS_PER_DAY = 86_400_000L
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()

    fun isOwnerAdminEmail(email: String?): Boolean =
        email?.trim().equals(CloudSubscriptionConfig.OWNER_ADMIN_EMAIL, ignoreCase = true)

    fun evaluate(
        entitlement: CloudUserEntitlement?,
        config: CloudSubscriptionConfig,
        serverNowMs: Long,
        dailyFreeUsed: Int,
        freeSampleMockUsed: Boolean,
        deviceShortCode: String,
        deviceHash: String,
        isAdmin: Boolean
    ): SubscriptionUiState {
        val dailyLimit = config.freeDailyQuestionLimit.toInt().coerceIn(1, 200)
        if (entitlement == null) {
            return SubscriptionUiState(
                isAdmin = isAdmin,
                rawTier = PlanTier.FREE,
                effectiveTier = if (isAdmin) PlanTier.ALL_EXAM else PlanTier.FREE,
                rawStatus = SubscriptionStatus.ACTIVE,
                isExpired = false,
                isSuspended = false,
                unlockedExamIds = if (isAdmin) setOf("all") else emptySet(),
                expiresAtMs = null,
                daysRemaining = 0,
                trialUsed = false,
                deviceShortCode = deviceShortCode,
                deviceHash = deviceHash,
                dailyFreeLimit = dailyLimit,
                dailyFreeUsed = dailyFreeUsed.coerceAtLeast(0),
                freeSampleMockUsed = freeSampleMockUsed,
                config = config,
                isLoaded = true,
                planDisplayName = if (isAdmin) "Owner Admin (All-Exam)" else "ClinEdge Free",
                expirySummaryLabel = if (isAdmin) "Unlimited owner access" else "$dailyLimit Qs/day • 1 sample mock"
            )
        }

        val rawTier = PlanTier.fromDb(entitlement.planTier)
        val rawStatus = SubscriptionStatus.fromDb(entitlement.status)
        val expiresAtMs = entitlement.expiresAt?.toDate()?.time

        val isSuspended = rawStatus == SubscriptionStatus.SUSPENDED
        val isTimeExpired = rawTier != PlanTier.FREE &&
            (rawStatus == SubscriptionStatus.EXPIRED || (expiresAtMs != null && serverNowMs >= expiresAtMs))

        val effectiveTier = when {
            isAdmin -> PlanTier.ALL_EXAM
            isSuspended || isTimeExpired -> PlanTier.FREE
            else -> rawTier
        }

        val effectiveExams = when {
            isAdmin -> setOf("all")
            effectiveTier == PlanTier.ALL_EXAM || effectiveTier == PlanTier.TRIAL -> setOf("all")
            effectiveTier == PlanTier.SINGLE_EXAM -> entitlement.unlockedExamIds.filter { it.isNotBlank() }.toSet()
            else -> emptySet()
        }

        val daysRemaining = if (expiresAtMs != null && serverNowMs < expiresAtMs && !isSuspended) {
            ceil((expiresAtMs - serverNowMs).toDouble() / MILLIS_PER_DAY.toDouble()).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val planLabel = when {
            isAdmin -> "Owner Admin (All-Exam)"
            isSuspended -> "Plan Suspended (Free Tier)"
            isTimeExpired -> "Plan Expired (Free Tier)"
            effectiveTier == PlanTier.ALL_EXAM -> "All-Exam Nursing Prep"
            effectiveTier == PlanTier.TRIAL -> "3-Day Free Trial"
            effectiveTier == PlanTier.SINGLE_EXAM -> {
                val names = effectiveExams.map { shortExamLabel(it) }.distinct()
                if (names.isEmpty()) "Single Exam Pro" else "${names.joinToString(" + ")} Pro"
            }
            else -> "ClinEdge Free"
        }

        val expiryLabel = when {
            isAdmin -> "Unlimited owner access"
            isSuspended -> "Suspended by admin • Tap Support to reactivate"
            isTimeExpired && expiresAtMs != null ->
                "Expired on ${formatShortDate(expiresAtMs)} • Renew to resume"
            effectiveTier == PlanTier.FREE ->
                "$dailyLimit Qs/day • ${if (freeSampleMockUsed) "Sample mock used" else "1 sample mock left"}"
            expiresAtMs != null ->
                "$daysRemaining day${if (daysRemaining == 1) "" else "s"} left • Ends ${formatShortDate(expiresAtMs)}"
            else -> "Active"
        }

        return SubscriptionUiState(
            isAdmin = isAdmin,
            rawTier = rawTier,
            effectiveTier = effectiveTier,
            rawStatus = rawStatus,
            isExpired = isTimeExpired,
            isSuspended = isSuspended,
            unlockedExamIds = effectiveExams,
            expiresAtMs = expiresAtMs,
            daysRemaining = daysRemaining,
            trialUsed = entitlement.trialUsed,
            deviceShortCode = deviceShortCode,
            deviceHash = deviceHash,
            dailyFreeLimit = dailyLimit,
            dailyFreeUsed = dailyFreeUsed.coerceAtLeast(0),
            freeSampleMockUsed = freeSampleMockUsed,
            config = config,
            isLoaded = true,
            planDisplayName = planLabel,
            expirySummaryLabel = expiryLabel
        )
    }

    /**
     * Computes stacked renewal expiry timestamp in epoch ms:
     * If user already has an active unexpired paid/trial plan, adds tenureDays on top of current expiresAt;
     * otherwise starts fresh from serverNowMs. Subtracts 60s safety margin so rules clock tolerance always passes.
     */
    fun computeNewExpiryMs(
        existing: CloudUserEntitlement?,
        tenureDays: Int,
        serverNowMs: Long
    ): Long {
        val currentExpiry = existing?.expiresAt?.toDate()?.time
        val isActiveUnexpired = existing != null &&
            existing.status == SubscriptionStatus.ACTIVE.dbValue &&
            currentExpiry != null &&
            currentExpiry > serverNowMs
        val base = if (isActiveUnexpired) currentExpiry!! else serverNowMs
        return base + (tenureDays.coerceIn(1, 365).toLong() * MILLIS_PER_DAY) - 60_000L
    }

    /**
     * Merges unlocked exams when redeeming a single-exam code on an active single-exam plan,
     * or returns ["all"] for all-exam codes.
     */
    fun resolveUnlockedExamsForRedemption(
        existing: CloudUserEntitlement?,
        codeTier: PlanTier,
        codeExamIds: List<String>,
        serverNowMs: Long
    ): List<String> {
        if (codeTier == PlanTier.ALL_EXAM) return listOf("all")
        val cleanCodeExams = codeExamIds.filter { it.isNotBlank() }.distinct().ifEmpty {
            listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING)
        }
        val currentExpiry = existing?.expiresAt?.toDate()?.time
        val canMergeExistingSingle = existing != null &&
            existing.planTier == PlanTier.SINGLE_EXAM.dbValue &&
            existing.status == SubscriptionStatus.ACTIVE.dbValue &&
            currentExpiry != null &&
            currentExpiry > serverNowMs
        return if (canMergeExistingSingle) {
            (existing!!.unlockedExamIds + cleanCodeExams).filter { it.isNotBlank() }.distinct().take(5)
        } else {
            cleanCodeExams.take(5)
        }
    }

    fun generateAccessCode(tier: PlanTier, examId: String?, tenure: PlanTenure): String {
        val tag = when {
            tier == PlanTier.ALL_EXAM -> "ALL"
            examId == PlatformCatalogDefaults.EXAM_AIIMS_NORCET -> "NOR"
            examId == PlatformCatalogDefaults.EXAM_PGIMER_NURSING -> "PGI"
            examId == PlatformCatalogDefaults.EXAM_RRB_NURSING -> "RRB"
            !examId.isNullOrBlank() -> examId.substringBefore('_').uppercase(Locale.US).take(3).padEnd(3, 'X')
            else -> "RRB"
        }
        val part1 = randomCodeSegment(4)
        val part2 = randomCodeSegment(4)
        return "CE-$tag-${tenure.shortCode}-$part1-$part2"
    }

    private fun randomCodeSegment(len: Int): String {
        val sb = StringBuilder(len)
        repeat(len) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
        }
        return sb.toString()
    }

    fun normalizeCodeInput(raw: String): String =
        raw.trim().uppercase(Locale.US).replace(Regex("\\s+"), "")

    fun maskEmail(email: String): String {
        val clean = email.trim()
        val at = clean.indexOf('@')
        if (at <= 1) return "***"
        val prefix = clean.substring(0, 1)
        val domain = clean.substring(at)
        return "${prefix}***$domain"
    }

    fun shortExamLabel(examId: String): String {
        if (examId == "all") return "All 29 Exams"
        PlatformCatalogDefaults.examById(examId)?.let { return it.shortName }
        return examId.uppercase(Locale.US)
    }

    fun formatShortDate(epochMs: Long): String {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun buildSupportMessage(
        reason: String,
        uid: String,
        email: String,
        planLabel: String,
        shortDeviceCode: String,
        deviceName: String,
        requestedPlanSummary: String? = null
    ): String {
        return buildString {
            appendLine("Hello ClinEdge Support,")
            appendLine()
            appendLine("Reason: $reason")
            if (!requestedPlanSummary.isNullOrBlank()) {
                appendLine("Selected Plan: $requestedPlanSummary")
            }
            appendLine("Account UID: ${uid.ifBlank { "Not signed in" }}")
            appendLine("Email: ${email.ifBlank { "Not signed in" }}")
            appendLine("Current Plan: $planLabel")
            appendLine("Device Lock ID: ${shortDeviceCode.ifBlank { "N/A" }}")
            appendLine("Device: ${deviceName.ifBlank { "Android" }}")
        }.trim()
    }

    fun buildWhatsAppUrl(phoneNumber: String, message: String): String {
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        val encoded = urlEncode(message)
        return "https://wa.me/$digitsOnly?text=$encoded"
    }

    fun buildMailtoUri(supportEmail: String, subject: String, body: String): String {
        val cleanEmail = supportEmail.trim().ifBlank { CloudSubscriptionConfig.OWNER_ADMIN_EMAIL }
        return "mailto:$cleanEmail?subject=${urlEncode(subject)}&body=${urlEncode(body)}"
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
