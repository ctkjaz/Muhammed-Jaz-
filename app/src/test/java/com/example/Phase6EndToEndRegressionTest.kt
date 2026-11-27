package com.example

import com.example.data.model.AbgCompensationStatus
import com.example.data.model.AbgInput
import com.example.data.model.AbgInterpreterLogic
import com.example.data.model.AbgPracticeCatalog
import com.example.data.model.AbgPrimaryDisorder
import com.example.data.model.ClinicalSpotterCatalog
import com.example.data.model.CustomMockBuilderLogic
import com.example.data.model.CustomMockConfig
import com.example.data.model.CustomMockSourceFilter
import com.example.data.model.DailyGoalValidation
import com.example.data.model.EcgStudioCatalog
import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.IbqCategory
import com.example.data.model.IbqHubLogic
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionOfTheDayCatalog
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SpecialtyTrackLogic
import com.example.data.model.SrsStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 End-to-End Cross-Module Regression Suite verifying coherence across Phases 0–5:
 * - Phase 1: Custom daily goals (5–200), deterministic QOTD, SRS mastery
 * - Phase 2: Anti-farming XP, level progression, badges, streak shield
 * - Phase 3: ABG Interpreter + 12 cases, ECG Studio 14 calibrated strips
 * - Phase 4: 25 offline visual IBQs + auto-interleaving into general drills
 * - Phase 5: 4 zero-duplication Specialty Tracks, manual SRS queueing, Custom Mock Builder
 */
class Phase6EndToEndRegressionTest {

    private fun synthItem(
        id: String,
        subject: String,
        stem: String,
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        sourceType: QuestionSourceType = QuestionSourceType.PRACTICE,
        attempts: Int = 0,
        correct: Int = 0,
        lastOutcome: String? = null,
        isBookmarked: Boolean = false
    ): QuestionItem {
        val q = Question(
            id = id,
            category = "nur",
            shiftYear = if (sourceType == QuestionSourceType.OFFICIAL_PYQ) "AIIMS NORCET 2024" else "",
            subject = subject,
            difficulty = difficulty,
            questionText = stem,
            options = listOf("A", "B", "C", "D"),
            correctIndex = 0,
            explanation = "High-yield clinical rationale for $id.",
            isCustom = false,
            sourceType = sourceType,
            imageUrl = ""
        )
        return QuestionItem(
            question = q,
            progress = QuestionProgress(
                questionId = id,
                isBookmarked = isBookmarked,
                attemptsCount = attempts,
                correctCount = correct,
                wrongCount = (attempts - correct).coerceAtLeast(0),
                lastOutcome = lastOutcome
            )
        )
    }

    @Test
    fun phase1And2_dailyGoalQotdAndAntiFarmingGamificationStayCoherent() {
        // Phase 1: Custom daily goal validation 5..200
        assertNull(DailyGoalValidation.parseCustomTarget("4"))
        assertEquals(65, DailyGoalValidation.parseCustomTarget("65"))
        assertEquals(200, DailyGoalValidation.parseCustomTarget("200"))
        assertNull(DailyGoalValidation.parseCustomTarget("201"))

        // Phase 1: Deterministic QOTD per date
        val qotdA1 = QuestionOfTheDayCatalog.questionForDate("2026-11-27")
        val qotdA2 = QuestionOfTheDayCatalog.questionForDate("2026-11-27")
        val qotdB = QuestionOfTheDayCatalog.questionForDate("2026-11-28")
        assertEquals(qotdA1.id, qotdA2.id)
        assertNotEquals(qotdA1.id, qotdB.id)

        // Phase 2: Anti-farming XP (repeat same question ID on same day awards 0 XP)
        val first = GamificationLogic.recordQuestionAnswer(
            current = GamificationState(),
            questionId = "Q_REGRESSION_1",
            isCorrect = true,
            lifetimeAnsweredCount = 1,
            longestStreakDays = 1,
            todayKey = "2026-11-27",
            weekKey = "2026-W48"
        )
        assertTrue(first.xpAwarded > 0)

        val repeatSameDay = GamificationLogic.recordQuestionAnswer(
            current = first.updatedState,
            questionId = "Q_REGRESSION_1",
            isCorrect = true,
            lifetimeAnsweredCount = 2,
            longestStreakDays = 1,
            todayKey = "2026-11-27",
            weekKey = "2026-W48"
        )
        assertEquals("Re-answering the same question on the same day must not farm XP", 0, repeatSameDay.xpAwarded)
        assertEquals(first.updatedState.totalXp, repeatSameDay.updatedState.totalXp)
    }

    @Test
    fun phase3_abgAndEcgClinicalStudiosAreCompleteAndAccurate() {
        // ABG: Classic DKA with appropriate Winter's compensation & elevated Anion Gap
        val dka = AbgInterpreterLogic.interpret(
            AbgInput(ph = 7.22, paco2 = 23.0, hco3 = 10.0, pao2 = 96.0, sodium = 138.0, chloride = 102.0)
        )
        assertEquals(AbgPrimaryDisorder.METABOLIC_ACIDOSIS, dka.primaryDisorder)
        assertEquals(AbgCompensationStatus.PARTIALLY_COMPENSATED, dka.compensationStatus)
        assertNotNull(dka.anionGap)
        assertTrue(dka.anionGap!!.isElevated)
        assertEquals(12, AbgPracticeCatalog.ALL_CASES.size)

        // ECG Studio: 14 calibrated strips & blinded practice deck
        assertEquals(14, EcgStudioCatalog.ALL_CASES.size)
        val practiceDeck = EcgStudioCatalog.buildPracticeDeck()
        assertEquals(14, practiceDeck.size)
        practiceDeck.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.correctIndex in q.options.indices)
            assertEquals(q.caseItem.title, q.options[q.correctIndex])
        }
    }

    @Test
    fun phase4And5_ibqsSpecialtyTracksSrsAndCustomMocksWorkTogetherEndToEnd() {
        val spotters = ClinicalSpotterCatalog.builtinSpotterQuestions().map { q ->
            QuestionItem(question = q, progress = QuestionProgress(questionId = q.id))
        }
        assertEquals(25, spotters.size)

        val textPool = (1..30).map { i ->
            val subj = when (i % 4) {
                0 -> "MSN"
                1 -> "OBG"
                2 -> "PEDS"
                else -> "FON"
            }
            val stem = when (subj) {
                "MSN" -> "Acute STEMI & hyperkalemia ECG management case #$i"
                "OBG" -> "Intrapartum labor CTG & PPH uterotonic case #$i"
                "PEDS" -> "NICU newborn APGAR & pediatric dosing case #$i"
                else -> "ICU ventilator ABG acidosis airway protocol #$i"
            }
            synthItem(
                id = "TXT_$i",
                subject = subj,
                stem = stem,
                difficulty = if (i % 3 == 0) QuestionDifficulty.HARD else QuestionDifficulty.MEDIUM,
                attempts = if (i <= 4) 1 else 0,
                correct = if (i <= 2) 1 else 0,
                lastOutcome = when {
                    i <= 2 -> "right"
                    i <= 4 -> "wrong"
                    else -> null
                },
                isBookmarked = i in 5..8
            )
        }
        val pyqItem = synthItem(
            id = "PYQ_ONLY_1",
            subject = "MSN",
            stem = "Official PYQ item that must stay out of CBT mocks",
            sourceType = QuestionSourceType.OFFICIAL_PYQ
        )
        val unifiedBank = spotters + textPool + pyqItem

        // Phase 4: Auto-interleaving IBQs into general 25Q mix
        val mixed25 = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = textPool,
            ibqPool = spotters,
            targetCount = 25
        )
        assertEquals(25, mixed25.size)
        assertTrue(mixed25.any { it.question.isImageBased })

        // Phase 4: IBQ Hub categorization covers all 5 categories
        val ibqSummary = IbqHubLogic.buildSummary(spotters)
        assertEquals(25, ibqSummary.totalCount)
        assertTrue(ibqSummary.categoryStats.first { it.category == IbqCategory.ECG }.totalCount >= 4)
        assertTrue(ibqSummary.categoryStats.first { it.category == IbqCategory.OBG_CTG }.totalCount >= 3)

        // Phase 5: All 4 Specialty Tracks project non-empty unified pools + rapid revision drills
        val tracks = SpecialtyTrackLogic.projectAllTracks(unifiedBank)
        assertEquals(4, tracks.size)
        tracks.forEach { proj ->
            assertTrue("Track ${proj.track.title} must project questions", proj.totalCount >= 5)
            val rapid = proj.buildRapidRevisionDrill(10)
            assertTrue("Rapid drill for ${proj.track.title} should be 1..10 items", rapid.size in 1..10)
            assertEquals("Zero duplicates in rapid drill", rapid.size, rapid.map { it.id }.distinct().size)
        }

        // Phase 5: Manual SRS queueing of bookmarked MCQs + 1-tap mastery graduation
        val now = 1_700_500_000_000L
        val bookmarkedIds = unifiedBank.filter { it.isBookmarked }.map { it.id }
        val (queuedCards, added) = SpacedRepetitionLogic.enqueueBulkManualReview(emptyMap(), bookmarkedIds, nowMs = now)
        assertEquals(4, added)
        val masteredMap = SpacedRepetitionLogic.markMastered(queuedCards, bookmarkedIds.first(), nowMs = now)
        assertEquals(SrsStage.MASTERED, masteredMap[bookmarkedIds.first()]?.stage)
        val srsSummary = SpacedRepetitionLogic.buildSummary(unifiedBank, masteredMap, nowMs = now)
        // 2 missed questions (TXT_3, TXT_4) auto-surfaced as due + 3 remaining queued bookmarks = 5 due
        assertEquals(5, srsSummary.dueEntries.size)
        assertEquals(1, srsSummary.masteredEntries.size)

        // Phase 5: Custom Mock Builder respects PYQ exclusion, untimed mode & negative marking toggle
        val customCfg = CustomMockConfig(
            questionCount = 25,
            selectedSubjectCodes = emptySet(),
            sourceFilter = CustomMockSourceFilter.ALL_BANK,
            includeIbqs = true,
            timedMode = false,
            durationMinutes = 25,
            enableNegativeMarking = false,
            negativeMarkPenalty = 0.33
        )
        val builtMock = CustomMockBuilderLogic.buildMockSessionQuestions(unifiedBank, customCfg)
        assertEquals(25, builtMock.size)
        assertFalse("PYQ must never leak into Custom CBT Mock", builtMock.any { it.id == pyqItem.id })
        assertTrue("IBQs enabled must include visual IBQs", builtMock.any { it.question.isImageBased })
        assertEquals(0.0, customCfg.effectiveNegativeMarks, 0.0001)
    }
}
