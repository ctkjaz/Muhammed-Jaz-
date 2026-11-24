package com.example

import com.example.data.subscription.CloudPaymentRequest
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.PaymentRequestStatus
import com.example.data.subscription.PlanTier
import com.example.data.subscription.QuestionReportReason
import com.example.data.subscription.QuestionReportStatus
import com.example.data.subscription.UpiPaymentLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for ClinEdge v2.3 core business logic:
 * - UPI UTR sanitization, validation, and deep-link URI construction
 * - OTA cloud question coordinate resolution for Admin live editing
 * - Payment request & Question report enum/model parsing
 */
class V23FeaturesLogicTest {

    @Test
    fun `sanitizeUtr strips whitespace invalid symbols uppercases and caps length`() {
        assertEquals("4123-45678901", UpiPaymentLogic.sanitizeUtr(" 4123-4567 8901 "))
        assertEquals("UPIREF2026-ABC", UpiPaymentLogic.sanitizeUtr("upi_ref#2026-abc"))
        assertEquals(32, UpiPaymentLogic.sanitizeUtr("A".repeat(45)).length)
    }

    @Test
    fun `isValidUtr enforces 6 to 32 alphanumeric or hyphen characters`() {
        assertFalse(UpiPaymentLogic.isValidUtr("12345"))
        assertTrue(UpiPaymentLogic.isValidUtr("412345678901"))
        assertTrue(UpiPaymentLogic.isValidUtr("T2601011234567890"))
        assertFalse(UpiPaymentLogic.isValidUtr("   "))
    }

    @Test
    fun `isValidUpiId validates standard VPA format`() {
        assertTrue(UpiPaymentLogic.isValidUpiId("clinedge@okaxis"))
        assertTrue(UpiPaymentLogic.isValidUpiId("nursing.prep_01@ybl"))
        assertFalse(UpiPaymentLogic.isValidUpiId("clinedge"))
        assertFalse(UpiPaymentLogic.isValidUpiId("bad vpa@upi"))
        assertFalse(UpiPaymentLogic.isValidUpiId("@okicici"))
    }

    @Test
    fun `buildUpiPayUri builds encoded deep link with INR currency`() {
        val uri = UpiPaymentLogic.buildUpiPayUri(
            upiId = "clinedge@okaxis",
            payeeName = "ClinEdge Nursing Prep",
            amountInr = 299,
            transactionNote = "All-Exam 1M Pro"
        )
        assertTrue(uri.startsWith("upi://pay?"))
        assertTrue(uri.contains("pa=clinedge%40okaxis"))
        assertTrue(uri.contains("pn=ClinEdge%20Nursing%20Prep"))
        assertTrue(uri.contains("am=299"))
        assertTrue(uri.contains("cu=INR"))
    }

    @Test
    fun `parseOtaQuestionCoordinates matches longest known packId and rejects bundled ids`() {
        val knownPacks = listOf("pack_norcet", "pack_norcet_2026_s1")
        val resolved = UpiPaymentLogic.parseOtaQuestionCoordinates(
            questionId = "OTA_pack_norcet_2026_s1_q042",
            knownPackIds = knownPacks
        )
        assertEquals("pack_norcet_2026_s1" to "q042", resolved)

        assertNull(UpiPaymentLogic.parseOtaQuestionCoordinates("NORCET-MSN-001", knownPacks))
        assertNull(UpiPaymentLogic.parseOtaQuestionCoordinates("OTA_unknownpack_q007", knownPacks))
        assertNull(UpiPaymentLogic.parseOtaQuestionCoordinates("OTA_pack_norcet_", knownPacks))
    }

    @Test
    fun `payment request status and summary labels parse safely`() {
        assertEquals(PaymentRequestStatus.PENDING, PaymentRequestStatus.fromDb("pending"))
        assertEquals(PaymentRequestStatus.APPROVED, PaymentRequestStatus.fromDb("approved"))
        assertEquals(PaymentRequestStatus.REJECTED, PaymentRequestStatus.fromDb("rejected"))
        assertEquals(PaymentRequestStatus.PENDING, PaymentRequestStatus.fromDb("unknown"))

        val allExamReq = CloudPaymentRequest(
            planTier = PlanTier.ALL_EXAM.dbValue,
            unlockedExamIds = listOf("all"),
            tenureDays = 90
        )
        assertTrue(allExamReq.planSummaryLabel.startsWith("All-Exam Prep"))

        val singleExamReq = CloudPaymentRequest(
            planTier = PlanTier.SINGLE_EXAM.dbValue,
            unlockedExamIds = listOf("aiims_norcet"),
            tenureDays = 30
        )
        assertTrue(singleExamReq.planSummaryLabel.contains("Pro"))
    }

    @Test
    fun `question report enums parse cleanly and subscription config has UPI defaults`() {
        assertEquals(QuestionReportReason.WRONG_KEY, QuestionReportReason.fromDb("wrong_key"))
        assertEquals(QuestionReportReason.TYPO, QuestionReportReason.fromDb("typo"))
        assertEquals(QuestionReportReason.OUTDATED, QuestionReportReason.fromDb("outdated"))
        assertEquals(QuestionReportReason.WRONG_MAPPING, QuestionReportReason.fromDb("wrong_mapping"))
        assertEquals(QuestionReportReason.OTHER, QuestionReportReason.fromDb("unexpected"))

        assertEquals(QuestionReportStatus.OPEN, QuestionReportStatus.fromDb("open"))
        assertEquals(QuestionReportStatus.RESOLVED, QuestionReportStatus.fromDb("resolved"))
        assertEquals(QuestionReportStatus.DISMISSED, QuestionReportStatus.fromDb("dismissed"))

        val defaultConfig = CloudSubscriptionConfig()
        assertEquals("", defaultConfig.upiId)
        assertEquals("ClinEdge Nursing Prep", defaultConfig.upiPayeeName)
    }
}
