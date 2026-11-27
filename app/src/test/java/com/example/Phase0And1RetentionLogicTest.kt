package com.example

import com.example.data.model.DailyGoalValidation
import com.example.data.model.OptionRationaleAnalyzer
import com.example.data.model.QotdCompletionState
import com.example.data.model.Question
import com.example.data.model.QuestionOfTheDayCatalog
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SrsStage
import com.example.data.model.StartupLaunchMode
import com.example.data.model.StartupNursingPearls
import com.example.data.model.StartupProfiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase0And1RetentionLogicTest {

    @Test
    fun startupProfiler_recordsColdAndWarmPhaseTimingsCleanly() {
        var now = 1000L
        val profiler = StartupProfiler(clockMs = { now })
        profiler.mark("vm_init")
        now += 15L
        profiler.mark("seed_start")
        now += 40L
        profiler.mark("seed_done")
        profiler.setLaunchMode(StartupLaunchMode.WARM_CACHE)
        now += 10L
        profiler.mark("first_data_ready")

        val report = profiler.buildReport()
        assertEquals(StartupLaunchMode.WARM_CACHE, report.launchMode)
        assertEquals(40L, report.seedDurationMs)
        assertEquals(65L, report.totalToFirstDataMs)
        assertEquals(4, report.marks.size)
        assertTrue(report.formatLogLine().contains("mode=warm_cache"))
        assertTrue(report.formatLogLine().contains("seed=40ms"))

        assertEquals(StartupNursingPearls.ALL.first(), StartupNursingPearls.pearlForIndex(0))
        assertEquals(StartupNursingPearls.ALL.last(), StartupNursingPearls.pearlForIndex(-1))
    }

    @Test
    fun dailyGoalValidation_enforcesCustomBoundsAndPresets() {
        assertEquals(listOf(10, 20, 30, 50), DailyGoalValidation.PRESET_TARGETS)
        assertEquals(5, DailyGoalValidation.parseCustomTarget("5"))
        assertEquals(75, DailyGoalValidation.parseCustomTarget(" 75 "))
        assertEquals(200, DailyGoalValidation.parseCustomTarget("200"))
        assertNull(DailyGoalValidation.parseCustomTarget("4"))
        assertNull(DailyGoalValidation.parseCustomTarget("201"))
        assertNull(DailyGoalValidation.parseCustomTarget("abc"))
        assertNotNull(DailyGoalValidation.validationHint("3"))
        assertNotNull(DailyGoalValidation.validationHint("250"))
        assertNull(DailyGoalValidation.validationHint("25"))
    }

    @Test
    fun qotdCatalog_has14MedicallyCompleteQuestionsWithFullPerOptionRationalesAndDeterminism() {
        val all = QuestionOfTheDayCatalog.ALL_QUESTIONS
        assertEquals(14, all.size)
        assertEquals(14, all.map { it.id }.toSet().size)

        all.forEach { q ->
            assertEquals("Expected 4 options for ${q.id}", 4, q.options.size)
            assertTrue("Valid correctIndex for ${q.id}", q.correctIndex in 0..3)
            assertEquals("Expected 4 option rationales for ${q.id}", 4, q.optionRationales.size)
            assertEquals(setOf(0, 1, 2, 3), q.optionRationales.map { it.optionIndex }.toSet())
            assertTrue(q.conciseSummary.length >= 25)
            assertTrue(q.nursingPriority.length >= 20)
            assertTrue(q.examPearl.length >= 20)
            assertTrue(q.commonTrap.length >= 20)
            assertTrue(q.deeperLearning.length >= 30)
        }

        val dayA1 = QuestionOfTheDayCatalog.questionForDate("2026-11-27")
        val dayA2 = QuestionOfTheDayCatalog.questionForDate("2026-11-27")
        assertEquals(dayA1.id, dayA2.id)

        val state = QotdCompletionState(
            dateKey = "2026-11-27",
            questionId = dayA1.id,
            selectedIndex = dayA1.correctIndex,
            isCorrect = true
        )
        assertTrue(state.isAnsweredFor("2026-11-27", dayA1.id))
        assertFalse(state.isAnsweredFor("2026-11-28", dayA1.id))

        val encoded = QuestionOfTheDayCatalog.serializeCompletion(state)
        val decoded = QuestionOfTheDayCatalog.deserializeCompletion(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun optionRationaleAnalyzer_buildsStructuredCardsAndExtractsDistractorNotes() {
        val q = Question(
            id = "TEST_Q_1",
            category = "nur",
            shiftYear = "2025",
            subject = "OBG",
            questionText = "Which drug is DOC for Eclampsia?",
            options = listOf("Magnesium Sulfate", "Hydralazine", "Oxytocin", "Methylergometrine"),
            correctIndex = 0,
            explanation = "Magnesium Sulfate is the drug of choice for eclampsia. Hydralazine is used for severe hypertension whereas Methylergometrine is contraindicated in preeclampsia."
        )

        val details = OptionRationaleAnalyzer.buildForQuestion(q, selectedIndex = 1)
        assertEquals(4, details.size)
        assertTrue(details[0].isCorrect)
        assertFalse(details[0].isUserSelected)
        assertTrue(details[1].isUserSelected)
        assertTrue(details[1].explanation.contains("Hydralazine", ignoreCase = true))
        assertTrue(details[3].explanation.contains("contraindicated", ignoreCase = true))
    }

    @Test
    fun spacedRepetition_markMasteredPromotesCardOutOfDueQueue() {
        val now = 1_700_000_000_000L
        val afterWrong = SpacedRepetitionLogic.recordWrong(emptyMap(), "Q_MISS_1", nowMs = now)
        assertTrue(SpacedRepetitionLogic.isDue(afterWrong["Q_MISS_1"]!!, nowMs = now))

        val afterMastered = SpacedRepetitionLogic.markMastered(afterWrong, "Q_MISS_1", nowMs = now)
        val masteredCard = afterMastered["Q_MISS_1"]!!
        assertEquals(SrsStage.MASTERED, masteredCard.stage)
        assertFalse(SpacedRepetitionLogic.isDue(masteredCard, nowMs = now))
        assertTrue(masteredCard.consecutiveCorrect >= 4)
    }
}
