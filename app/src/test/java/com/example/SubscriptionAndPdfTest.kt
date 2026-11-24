package com.example

import com.example.data.model.PlatformCatalogDefaults
import com.example.data.parser.PdfTextExtractor
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.CloudUserEntitlement
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PlanTenure
import com.example.data.subscription.PlanTier
import com.example.data.subscription.SubscriptionStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.zip.Deflater

class SubscriptionAndPdfTest {

    private val baseNowMs = 1_780_000_000_000L // fixed deterministic epoch ms
    private val defaultConfig = CloudSubscriptionConfig(
        freeDailyQuestionLimit = 15L,
        trialDays = 3L,
        supportEmail = "jazsaleem2004@gmail.com",
        supportWhatsAppNumber = "919876543210"
    )

    @Test
    fun freeUser_has15DailyLimit_lockedPyqs_andOneSampleMock() {
        val state = EntitlementEvaluator.evaluate(
            entitlement = null,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 5,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-ABCD1234",
            deviceHash = "hash_abc",
            isAdmin = false
        )

        assertEquals(PlanTier.FREE, state.effectiveTier)
        assertFalse(state.isExpired)
        assertFalse(state.isSuspended)
        assertFalse(state.hasPremiumForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertFalse(state.hasAllExamAccess)
        assertEquals(10, state.remainingFreeQuestionsToday)
        assertTrue(state.canStartSampleMock())
        assertEquals("ClinEdge Free", state.planDisplayName)
    }

    @Test
    fun singleExamPro_unlocksOnlyThatExam_andLocksOthers() {
        val futureExpiry = Timestamp(Date(baseNowMs + 25L * 86_400_000L))
        val ent = CloudUserEntitlement(
            userId = "u1",
            planTier = PlanTier.SINGLE_EXAM.dbValue,
            unlockedExamIds = listOf(PlatformCatalogDefaults.EXAM_AIIMS_NORCET),
            status = SubscriptionStatus.ACTIVE.dbValue,
            expiresAt = futureExpiry
        )

        val state = EntitlementEvaluator.evaluate(
            entitlement = ent,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 15,
            freeSampleMockUsed = true,
            deviceShortCode = "CE-11112222",
            deviceHash = "hash1",
            isAdmin = false
        )

        assertEquals(PlanTier.SINGLE_EXAM, state.effectiveTier)
        assertTrue(state.hasPremiumForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertFalse(state.hasPremiumForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING))
        assertFalse(state.hasPremiumForExam(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))
        assertFalse(state.hasAllExamAccess)
        assertEquals(25, state.daysRemaining)
    }

    @Test
    fun allExamPro_andThreeDayTrial_unlockEveryExam() {
        val futureExpiry = Timestamp(Date(baseNowMs + 3L * 86_400_000L))
        val trialEnt = CloudUserEntitlement(
            userId = "u2",
            planTier = PlanTier.TRIAL.dbValue,
            unlockedExamIds = listOf("all"),
            status = SubscriptionStatus.ACTIVE.dbValue,
            expiresAt = futureExpiry,
            trialUsed = true
        )

        val trialState = EntitlementEvaluator.evaluate(
            entitlement = trialEnt,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-33334444",
            deviceHash = "hash2",
            isAdmin = false
        )

        assertEquals(PlanTier.TRIAL, trialState.effectiveTier)
        assertTrue(trialState.hasAllExamAccess)
        assertTrue(trialState.hasPremiumForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING))
        assertTrue(trialState.hasPremiumForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertTrue(trialState.hasPremiumForExam(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))
        assertTrue(trialState.trialUsed)
    }

    @Test
    fun expiredOrSuspendedPremium_autoRevertsToFreeTier() {
        val pastExpiry = Timestamp(Date(baseNowMs - 5_000L))
        val expiredEnt = CloudUserEntitlement(
            userId = "u3",
            planTier = PlanTier.ALL_EXAM.dbValue,
            unlockedExamIds = listOf("all"),
            status = SubscriptionStatus.ACTIVE.dbValue,
            expiresAt = pastExpiry
        )

        val expiredState = EntitlementEvaluator.evaluate(
            entitlement = expiredEnt,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-55556666",
            deviceHash = "hash3",
            isAdmin = false
        )
        assertEquals(PlanTier.FREE, expiredState.effectiveTier)
        assertTrue(expiredState.isExpired)
        assertFalse(expiredState.hasAllExamAccess)

        val suspendedEnt = CloudUserEntitlement(
            userId = "u4",
            planTier = PlanTier.ALL_EXAM.dbValue,
            unlockedExamIds = listOf("all"),
            status = SubscriptionStatus.SUSPENDED.dbValue,
            expiresAt = Timestamp(Date(baseNowMs + 60L * 86_400_000L))
        )
        val suspendedState = EntitlementEvaluator.evaluate(
            entitlement = suspendedEnt,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-77778888",
            deviceHash = "hash4",
            isAdmin = false
        )
        assertEquals(PlanTier.FREE, suspendedState.effectiveTier)
        assertTrue(suspendedState.isSuspended)
        assertFalse(suspendedState.hasAllExamAccess)
    }

    @Test
    fun ownerAdminEmail_getsUnlimitedAllExamAccess() {
        assertTrue(EntitlementEvaluator.isOwnerAdminEmail("jazsaleem2004@gmail.com"))
        assertTrue(EntitlementEvaluator.isOwnerAdminEmail("  JAZSALEEM2004@GMAIL.COM  "))
        assertFalse(EntitlementEvaluator.isOwnerAdminEmail("student@gmail.com"))

        val state = EntitlementEvaluator.evaluate(
            entitlement = null,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 100,
            freeSampleMockUsed = true,
            deviceShortCode = "CE-OWNER",
            deviceHash = "hash_owner",
            isAdmin = true
        )
        assertTrue(state.isAdmin)
        assertEquals(PlanTier.ALL_EXAM, state.effectiveTier)
        assertTrue(state.hasAllExamAccess)
    }

    @Test
    fun computeNewExpiryMs_stacksRemainingDaysOnActiveRenewal() {
        val remainingDaysMs = 10L * 86_400_000L
        val activeEnt = CloudUserEntitlement(
            userId = "u5",
            planTier = PlanTier.SINGLE_EXAM.dbValue,
            unlockedExamIds = listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING),
            status = SubscriptionStatus.ACTIVE.dbValue,
            expiresAt = Timestamp(Date(baseNowMs + remainingDaysMs))
        )

        val stackedMs = EntitlementEvaluator.computeNewExpiryMs(activeEnt, 30, baseNowMs)
        val expectedStacked = baseNowMs + remainingDaysMs + 30L * 86_400_000L - 60_000L
        assertEquals(expectedStacked, stackedMs)

        // Merged exam list when redeeming another single-exam code while active
        val mergedExams = EntitlementEvaluator.resolveUnlockedExamsForRedemption(
            existing = activeEnt,
            codeTier = PlanTier.SINGLE_EXAM,
            codeExamIds = listOf(PlatformCatalogDefaults.EXAM_AIIMS_NORCET),
            serverNowMs = baseNowMs
        )
        assertEquals(
            listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING, PlatformCatalogDefaults.EXAM_AIIMS_NORCET),
            mergedExams
        )
    }

    @Test
    fun accessCodeGeneration_maskEmail_andSupportUrlsWork() {
        val code = EntitlementEvaluator.generateAccessCode(
            tier = PlanTier.ALL_EXAM,
            examId = null,
            tenure = PlanTenure.THREE_MONTHS
        )
        assertTrue(code.startsWith("CE-ALL-3M-"))
        assertEquals(19, code.length)

        assertEquals("s***@gmail.com", EntitlementEvaluator.maskEmail("student@gmail.com"))
        assertEquals("***", EntitlementEvaluator.maskEmail("invalid"))

        val body = EntitlementEvaluator.buildSupportMessage(
            reason = "Unlock Pro",
            uid = "uid_12345",
            email = "student@gmail.com",
            planLabel = "ClinEdge Free",
            shortDeviceCode = "CE-AABBCCDD",
            deviceName = "Pixel 9"
        )
        assertTrue(body.contains("uid_12345"))
        assertTrue(body.contains("CE-AABBCCDD"))

        val waUrl = EntitlementEvaluator.buildWhatsAppUrl("+91 98765-43210", body)
        assertTrue(waUrl.startsWith("https://wa.me/919876543210?text="))
    }

    @Test
    fun pdfTextExtractor_handlesPlainText_uncompressedPdf_andFlateCompressedPdf() {
        val plainRes = PdfTextExtractor.extractFromBytes(
            "1. Normal serum potassium level is?\nA) 3.5-5.0 mEq/L\nAnswer: A".toByteArray()
        )
        assertEquals("text", plainRes.fileType)
        assertFalse(plainRes.isLikelyScannedPdf)
        assertTrue(plainRes.text.contains("Normal serum potassium"))

        val pdfStreamContent = "BT /F1 12 Tf 72 720 Td (1. Which artery is palpated in infants during CPR?) Tj 0 -16 Td (A\\) Brachial artery B\\) Carotid artery Answer: A) Tj ET"
        val uncompressedPdf = "%PDF-1.4\n1 0 obj\n<< /Length ${pdfStreamContent.length} >>\nstream\n$pdfStreamContent\nendstream\nendobj\n%%EOF"
        val uncompressedRes = PdfTextExtractor.extractFromBytes(uncompressedPdf.toByteArray(Charsets.ISO_8859_1))
        assertEquals("pdf", uncompressedRes.fileType)
        assertFalse(uncompressedRes.isLikelyScannedPdf)
        assertTrue(uncompressedRes.text.contains("Brachial artery"))

        // FlateDecode compressed stream
        val compressed = deflateBytes(pdfStreamContent.toByteArray(Charsets.ISO_8859_1))
        val out = ByteArrayOutputStream()
        out.write("%PDF-1.4\n1 0 obj\n<< /Filter /FlateDecode /Length ${compressed.size} >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
        out.write(compressed)
        out.write("\nendstream\nendobj\n%%EOF".toByteArray(Charsets.ISO_8859_1))

        val compressedRes = PdfTextExtractor.extractFromBytes(out.toByteArray())
        assertEquals("pdf", compressedRes.fileType)
        assertFalse(compressedRes.isLikelyScannedPdf)
        assertTrue(compressedRes.text.contains("Which artery is palpated"))
    }

    @Test
    fun singleExamLock_blocksOtherExamsAndAllExams_butAllowsUnlockedAndHigherTiers() {
        val singleExamEnt = CloudUserEntitlement(
            userId = "u_single",
            planTier = PlanTier.SINGLE_EXAM.dbValue,
            unlockedExamIds = listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING),
            status = SubscriptionStatus.ACTIVE.dbValue,
            expiresAt = Timestamp(Date(baseNowMs + 30L * 86_400_000L))
        )
        val singleState = EntitlementEvaluator.evaluate(
            entitlement = singleExamEnt,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-SINGLE",
            deviceHash = "hash_single",
            isAdmin = false
        )
        assertTrue(singleState.isSingleExamLocked)
        assertEquals(PlatformCatalogDefaults.EXAM_RRB_NURSING, singleState.primaryUnlockedExamId)
        assertFalse(singleState.isExamLockedForSwitch(PlatformCatalogDefaults.EXAM_RRB_NURSING))
        assertTrue(singleState.isExamLockedForSwitch(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertTrue(singleState.isExamLockedForSwitch(null))
        assertTrue(singleState.isExamLockedForSwitch(""))

        // All-Exam tier is never single-exam locked
        val allExamState = EntitlementEvaluator.evaluate(
            entitlement = singleExamEnt.copy(
                planTier = PlanTier.ALL_EXAM.dbValue,
                unlockedExamIds = listOf("all")
            ),
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-ALL",
            deviceHash = "hash_all",
            isAdmin = false
        )
        assertFalse(allExamState.isSingleExamLocked)
        assertFalse(allExamState.isExamLockedForSwitch(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertFalse(allExamState.isExamLockedForSwitch(null))

        // Owner Admin is never single-exam locked
        val adminState = EntitlementEvaluator.evaluate(
            entitlement = singleExamEnt,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-ADMIN",
            deviceHash = "hash_admin",
            isAdmin = true
        )
        assertFalse(adminState.isSingleExamLocked)
        assertFalse(adminState.isExamLockedForSwitch(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertFalse(adminState.isExamLockedForSwitch(null))

        // Free tier users can still browse exam tracks (subject to daily free limit / paywall)
        val freeState = EntitlementEvaluator.evaluate(
            entitlement = null,
            config = defaultConfig,
            serverNowMs = baseNowMs,
            dailyFreeUsed = 0,
            freeSampleMockUsed = false,
            deviceShortCode = "CE-FREE",
            deviceHash = "hash_free",
            isAdmin = false
        )
        assertFalse(freeState.isSingleExamLocked)
        assertFalse(freeState.isExamLockedForSwitch(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
    }

    private fun deflateBytes(input: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val buffer = ByteArray(1024)
        val out = ByteArrayOutputStream()
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            if (count <= 0) break
            out.write(buffer, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }
}
