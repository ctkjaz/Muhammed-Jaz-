package com.example.data.subscription

import androidx.annotation.Keep
import com.example.data.model.QuestionDifficulty
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.net.URLEncoder
import java.util.Locale

enum class PaymentRequestStatus(val dbValue: String, val label: String) {
    PENDING("pending", "Pending Verification"),
    APPROVED("approved", "Approved & Active"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromDb(raw: String?): PaymentRequestStatus =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class CloudPaymentRequest(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val planTier: String = PlanTier.SINGLE_EXAM.dbValue,
    val unlockedExamIds: List<String> = emptyList(),
    val tenureDays: Long = 30L,
    val amountInr: Long = 0L,
    val utrNumber: String = "",
    val payerName: String = "",
    val status: String = PaymentRequestStatus.PENDING.dbValue,
    val adminNote: String = "",
    val reviewedByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: PaymentRequestStatus
        get() = PaymentRequestStatus.fromDb(status)

    val parsedTier: PlanTier
        get() = PlanTier.fromDb(planTier)

    val planSummaryLabel: String
        get() {
            val tenureLabel = PlanTenure.fromDays(tenureDays.toInt()).label
            return if (parsedTier == PlanTier.ALL_EXAM) {
                "All-Exam Prep ($tenureLabel)"
            } else {
                val examNames = unlockedExamIds.map { EntitlementEvaluator.shortExamLabel(it) }
                    .distinct()
                    .ifEmpty { listOf("Single Exam") }
                "${examNames.joinToString(" + ")} Pro ($tenureLabel)"
            }
        }
}

enum class QuestionReportReason(val dbValue: String, val label: String) {
    WRONG_KEY("wrong_key", "Wrong Answer Key"),
    TYPO("typo", "Typo / Formatting"),
    OUTDATED("outdated", "Outdated Guideline"),
    WRONG_MAPPING("wrong_mapping", "Wrong Subject / Exam"),
    OTHER("other", "Other Clinical Issue");

    companion object {
        fun fromDb(raw: String?): QuestionReportReason =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: OTHER
    }
}

enum class QuestionReportStatus(val dbValue: String, val label: String) {
    OPEN("open", "Open"),
    RESOLVED("resolved", "Resolved"),
    DISMISSED("dismissed", "Dismissed");

    companion object {
        fun fromDb(raw: String?): QuestionReportStatus =
            entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class CloudQuestionReport(
    @DocumentId val id: String = "",
    val reportId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val examId: String = "",
    val subject: String = "",
    val questionId: String = "",
    val questionPreview: String = "",
    val reason: String = QuestionReportReason.WRONG_KEY.dbValue,
    val note: String = "",
    val status: String = QuestionReportStatus.OPEN.dbValue,
    val adminResolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedReason: QuestionReportReason
        get() = QuestionReportReason.fromDb(reason)

    val parsedStatus: QuestionReportStatus
        get() = QuestionReportStatus.fromDb(status)
}

/**
 * Editable draft of a cloud OTA pack question loaded by Owner Admin from a Question Report.
 */
data class OtaQuestionEditDraft(
    val packId: String,
    val docId: String,
    val localQuestionId: String,
    val examId: String,
    val subject: String,
    val difficulty: QuestionDifficulty,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val imageUrl: String = "",
    val reportId: String? = null
)

object UpiPaymentLogic {
    private val UTR_CLEAN_REGEX = Regex("[^A-Z0-9\\-]")

    fun sanitizeUtr(raw: String): String =
        raw.trim().uppercase(Locale.US).replace(UTR_CLEAN_REGEX, "").take(32)

    fun isValidUtr(raw: String): Boolean {
        val clean = sanitizeUtr(raw)
        return clean.length in 6..32
    }

    fun isValidUpiId(raw: String): Boolean {
        val clean = raw.trim()
        if (clean.length !in 5..80) return false
        val atIdx = clean.indexOf('@')
        return atIdx > 0 && atIdx < clean.lastIndex && !clean.contains(' ')
    }

    fun buildUpiPayUri(
        upiId: String,
        payeeName: String,
        amountInr: Int,
        transactionNote: String
    ): String {
        val pa = encode(upiId.trim())
        val pn = encode(payeeName.trim().ifBlank { "ClinEdge Nursing Prep" }.take(60))
        val tn = encode(transactionNote.trim().take(60))
        val am = amountInr.coerceAtLeast(1).toString()
        return "upi://pay?pa=$pa&pn=$pn&am=$am&cu=INR&tn=$tn"
    }

    /**
     * Parses local OTA question IDs formatted as "OTA_<packId>_<docId>" using known pack IDs.
     */
    fun parseOtaQuestionCoordinates(
        questionId: String,
        knownPackIds: Collection<String>
    ): Pair<String, String>? {
        if (!questionId.startsWith("OTA_")) return null
        val remainder = questionId.removePrefix("OTA_")
        val matchedPack = knownPackIds
            .sortedByDescending { it.length }
            .firstOrNull { remainder.startsWith("${it}_") }
            ?: return null
        val docId = remainder.removePrefix("${matchedPack}_").trim()
        if (docId.isBlank()) return null
        return matchedPack to docId
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
