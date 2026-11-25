package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.ClinicalMnemonicsCatalog
import com.example.data.model.ExamReadinessEngine
import com.example.data.model.MnemonicCategory
import com.example.data.model.MockHistory
import com.example.data.model.ReadinessBand
import com.example.data.model.SrsCardState
import com.example.data.model.SrsStage
import com.example.data.model.SubjectGroup
import com.example.data.model.SubjectStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V28FeaturesLogicTest {

    private fun sampleSubjects(): List<SubjectStat> = listOf(
        SubjectStat(
            code = "MSN",
            name = "Medical-Surgical Nursing",
            group = SubjectGroup.NURSING,
            totalQuestions = 100,
            attemptedQuestions = 80,
            totalAttempts = 100,
            correctAttempts = 85,
            wrongAttempts = 15,
            masteredQuestions = 60,
            accentColor = Color.Gray
        ),
        SubjectStat(
            code = "OBG",
            name = "OBG Nursing",
            group = SubjectGroup.NURSING,
            totalQuestions = 80,
            attemptedQuestions = 40,
            totalAttempts = 50,
            correctAttempts = 25,
            wrongAttempts = 25,
            masteredQuestions = 15,
            accentColor = Color.Gray
        )
    )

    @Test
    fun examReadiness_zeroActivity_returnsNeedsFocusAndOnboardingAction() {
        val summary = ExamReadinessEngine.compute(
            totalQuestions = 500,
            attemptedUnique = 0,
            totalAttempts = 0,
            accuracyPercent = 0,
            subjectStats = emptyList(),
            mockHistory = emptyList(),
            srsCards = emptyMap(),
            currentStreakDays = 0,
            dailyGoalMetToday = false,
            nowMs = 1_700_000_000_000L
        )
        assertEquals(0, summary.overallScore)
        assertEquals(ReadinessBand.NEEDS_FOCUS, summary.band)
        assertFalse(summary.hasEnoughData)
        assertEquals(5, summary.pillars.size)
        assertEquals(100, summary.pillars.sumOf { it.weightPercent })
        assertTrue(summary.nextBestAction.contains("25Q"))
    }

    @Test
    fun examReadiness_highPerformanceAndMasteredSrs_returnsExamReady() {
        val now = 1_700_000_000_000L
        val futureReview = now + 10L * 24L * 60L * 60L * 1000L
        val mocks = listOf(
            MockHistory(1, "Mock 1", 100, 86, 10, 4, 82.7, 90, now - 3000),
            MockHistory(2, "Mock 2", 100, 88, 8, 4, 85.3, 92, now - 2000),
            MockHistory(3, "Mock 3", 100, 85, 11, 4, 81.3, 88, now - 1000)
        )
        val srsCards = mapOf(
            "Q1" to SrsCardState("Q1", stage = SrsStage.MASTERED, nextReviewAtMs = futureReview, lastReviewedAtMs = now),
            "Q2" to SrsCardState("Q2", stage = SrsStage.MASTERED, nextReviewAtMs = futureReview, lastReviewedAtMs = now),
            "Q3" to SrsCardState("Q3", stage = SrsStage.BOX_14D, nextReviewAtMs = futureReview, lastReviewedAtMs = now)
        )

        val summary = ExamReadinessEngine.compute(
            totalQuestions = 400,
            attemptedUnique = 320,
            totalAttempts = 400,
            accuracyPercent = 88,
            subjectStats = sampleSubjects(),
            mockHistory = mocks,
            srsCards = srsCards,
            currentStreakDays = 9,
            dailyGoalMetToday = true,
            nowMs = now
        )

        assertTrue("Expected Exam Ready score >= 80, got ${summary.overallScore}", summary.overallScore >= 80)
        assertEquals(ReadinessBand.EXAM_READY, summary.band)
        assertTrue(summary.hasEnoughData)
        assertEquals("OBG", summary.weakestSubject?.subjectCode)
        val srsPillar = summary.pillars.first { it.id == "srs" }
        assertTrue("Mastered SRS cards should score >= 90%, got ${srsPillar.scorePercent}", srsPillar.scorePercent >= 90)
    }

    @Test
    fun examReadiness_dueSrsCardsLowerMistakeMasteryAndPrioritizeRevision() {
        val now = 1_700_000_000_000L
        val overdue = now - 86_400_000L
        val dueCards = (1..6).associate { idx ->
            "Q$idx" to SrsCardState("Q$idx", stage = SrsStage.BOX_1D, nextReviewAtMs = overdue, lastReviewedAtMs = overdue)
        }

        val summary = ExamReadinessEngine.compute(
            totalQuestions = 300,
            attemptedUnique = 60,
            totalAttempts = 70,
            accuracyPercent = 72,
            subjectStats = sampleSubjects(),
            mockHistory = emptyList(),
            srsCards = dueCards,
            currentStreakDays = 2,
            dailyGoalMetToday = false,
            nowMs = now
        )

        val srsPillar = summary.pillars.first { it.id == "srs" }
        assertEquals(0, srsPillar.scorePercent)
        assertTrue(summary.nextBestAction.contains("6 due cards"))
    }

    @Test
    fun clinicalMnemonicsCatalog_has24ValidUniqueEntriesAndSearchWorks() {
        val all = ClinicalMnemonicsCatalog.ALL_MNEMONICS
        assertEquals(24, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)

        all.forEach { m ->
            assertTrue("Acronym blank for ${m.id}", m.acronym.isNotBlank())
            assertTrue("Title blank for ${m.id}", m.title.isNotBlank())
            assertTrue("Steps empty for ${m.id}", m.steps.size >= 3)
            assertTrue("Exam trap too short for ${m.id}", m.examTrap.length >= 25)
            assertTrue("Exam tags empty for ${m.id}", m.examTags.isNotEmpty())
        }

        // Search by acronym & clinical keyword
        val ripes = ClinicalMnemonicsCatalog.search("RIPES")
        assertEquals(1, ripes.size)
        assertEquals("MN_RIPES", ripes.first().id)

        val bmw = ClinicalMnemonicsCatalog.search("gloves")
        assertTrue(bmw.any { it.id == "MN_BMW" })

        // Filter by category
        val obgPeds = ClinicalMnemonicsCatalog.search("", MnemonicCategory.OBG_PEDS)
        assertTrue(obgPeds.isNotEmpty())
        assertTrue(obgPeds.all { it.category == MnemonicCategory.OBG_PEDS })
        assertNotNull(obgPeds.firstOrNull { it.id == "MN_VEALCHOP" })
    }
}
