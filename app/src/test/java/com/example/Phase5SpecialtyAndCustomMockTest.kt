package com.example

import com.example.data.model.ClinicalSpotterCatalog
import com.example.data.model.CustomMockBuilderLogic
import com.example.data.model.CustomMockConfig
import com.example.data.model.CustomMockSourceFilter
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SpecialtyTrack
import com.example.data.model.SpecialtyTrackLogic
import com.example.data.model.SrsStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5SpecialtyAndCustomMockTest {

    private fun makeItem(
        id: String,
        subject: String,
        stem: String,
        explanation: String = "Clinical rationale",
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        imageUrl: String = "",
        attempts: Int = 0,
        correct: Int = 0,
        lastOutcome: String? = null,
        isBookmarked: Boolean = false,
        sourceType: QuestionSourceType = QuestionSourceType.PRACTICE
    ): QuestionItem {
        val q = Question(
            id = id,
            category = "nur",
            shiftYear = if (sourceType == QuestionSourceType.OFFICIAL_PYQ) "NORCET 2023 Shift 1" else "",
            subject = subject,
            difficulty = difficulty,
            questionText = stem,
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctIndex = 0,
            explanation = explanation,
            isCustom = false,
            sourceType = sourceType,
            imageUrl = imageUrl
        )
        val p = QuestionProgress(
            questionId = id,
            isBookmarked = isBookmarked,
            attemptsCount = attempts,
            correctCount = correct,
            wrongCount = (attempts - correct).coerceAtLeast(0),
            lastOutcome = lastOutcome
        )
        return QuestionItem(question = q, progress = p)
    }

    @Test
    fun specialtyTracks_zeroDuplicationProjection_mapsAllFourSpecialtiesAndRapidDrill() {
        val spotterItems = ClinicalSpotterCatalog.builtinSpotterQuestions().map { q ->
            QuestionItem(question = q, progress = QuestionProgress(questionId = q.id))
        }
        val extraItems = listOf(
            makeItem(
                id = "CARD_1",
                subject = "MSN",
                stem = "A patient with STEMI shows ST elevation in leads II, III, aVF. Which coronary artery is occluded?",
                attempts = 1,
                correct = 0,
                lastOutcome = "wrong"
            ),
            makeItem(
                id = "CARD_2",
                subject = "PHARM",
                stem = "Before administering digoxin, the nurse checks apical pulse for 1 full minute.",
                attempts = 1,
                correct = 1,
                lastOutcome = "right"
            ),
            makeItem(
                id = "ICU_1",
                subject = "FON",
                stem = "ABG shows pH 7.26, PaCO2 58 mmHg, HCO3 25 mEq/L in a COPD patient on ventilator.",
                attempts = 1,
                correct = 0,
                lastOutcome = "wrong"
            ),
            makeItem(
                id = "OBG_1",
                subject = "OBG",
                stem = "Loading dose of Magnesium sulfate (Pritchard regimen) for eclampsia prophylaxis.",
                attempts = 1,
                correct = 1,
                lastOutcome = "right"
            ),
            makeItem(
                id = "PEDS_1",
                subject = "PEDS",
                stem = "APGAR score at 1 minute for a newborn in NICU with acrocyanosis and heart rate 130/min.",
                attempts = 0,
                correct = 0,
                lastOutcome = null
            )
        )
        val pool = spotterItems + extraItems

        val projections = SpecialtyTrackLogic.projectAllTracks(pool)
        assertEquals(4, projections.size)

        val cardio = projections.first { it.track == SpecialtyTrack.CARDIOLOGY }
        val icu = projections.first { it.track == SpecialtyTrack.CRITICAL_CARE }
        val obg = projections.first { it.track == SpecialtyTrack.OBSTETRICS }
        val peds = projections.first { it.track == SpecialtyTrack.PEDIATRICS }

        assertTrue("Cardiology should project ECG & cardiac MCQs", cardio.totalCount >= 5)
        assertTrue("Cardiology should include IBQs", cardio.ibqQuestions.isNotEmpty())
        assertTrue("Cardiology weak list should include missed CARD_1", cardio.weakQuestions.any { it.id == "CARD_1" })
        assertEquals(50, cardio.accuracyPercent)

        assertTrue("Critical Care should project ICU/ABG/airway/ventilator MCQs", icu.totalCount >= 5)
        assertTrue("Critical Care should include IBQs", icu.ibqQuestions.isNotEmpty())
        assertTrue("Critical Care weak list should include ICU_1", icu.weakQuestions.any { it.id == "ICU_1" })

        assertTrue("Obstetrics should project OBG + CTG/Partograph IBQs", obg.totalCount >= 3)
        assertTrue("Obstetrics should include CTG/Partograph IBQs", obg.ibqQuestions.size >= 2)

        assertTrue("Pediatrics should project Pediatrics MCQs", peds.totalCount >= 1)

        val rapidDrill = cardio.buildRapidRevisionDrill(targetCount = 10)
        assertTrue(rapidDrill.isNotEmpty())
        assertTrue(rapidDrill.size <= 10)
        assertTrue("Weak item should be prioritized into rapid revision drill", rapidDrill.any { it.id == "CARD_1" })
    }

    @Test
    fun manualSrsQueueing_singleAndBulkEnqueue_makesBookmarkedItemsImmediatelyDue() {
        val now = 1_700_000_000_000L
        val q1 = makeItem(id = "Q_BM_1", subject = "MSN", stem = "Cardiac glycoside toxicity", isBookmarked = true)
        val q2 = makeItem(id = "Q_BM_2", subject = "OBG", stem = "Postpartum hemorrhage uterotonic", isBookmarked = true)
        val q3 = makeItem(id = "Q_BM_3", subject = "FON", stem = "Central venous pressure normal range", isBookmarked = true)

        val afterSingle = SpacedRepetitionLogic.enqueueManualReview(emptyMap(), q1.id, nowMs = now)
        val card1 = afterSingle[q1.id]!!
        assertEquals(SrsStage.BOX_1D, card1.stage)
        assertTrue(SpacedRepetitionLogic.isDue(card1, nowMs = now))

        val (afterBulk, newlyQueued) = SpacedRepetitionLogic.enqueueBulkManualReview(
            existing = afterSingle,
            questionIds = listOf(q1.id, q2.id, q3.id),
            nowMs = now
        )
        assertEquals(2, newlyQueued)
        assertEquals(3, afterBulk.size)

        val summary = SpacedRepetitionLogic.buildSummary(
            questions = listOf(q1, q2, q3),
            storedCards = afterBulk,
            nowMs = now
        )
        assertEquals(3, summary.dueEntries.size)

        val (_, secondRunAdded) = SpacedRepetitionLogic.enqueueBulkManualReview(
            existing = afterBulk,
            questionIds = listOf(q1.id, q2.id, q3.id),
            nowMs = now
        )
        assertEquals(0, secondRunAdded)
    }

    @Test
    fun customMockBuilder_filtersBySubjectSourceIbqAndBuildsConfiguredMock() {
        val pool = (1..20).map { i ->
            makeItem(
                id = "MSN_$i",
                subject = "MSN",
                stem = "MSN clinical question #$i",
                difficulty = if (i <= 6) QuestionDifficulty.HARD else QuestionDifficulty.MEDIUM,
                attempts = if (i <= 5) 1 else 0,
                correct = if (i <= 2) 1 else 0,
                lastOutcome = when {
                    i <= 2 -> "right"
                    i <= 5 -> "wrong"
                    else -> null
                }
            )
        } + (1..6).map { i ->
            makeItem(
                id = "OBG_IBQ_$i",
                subject = "OBG",
                stem = "Identify fetal monitoring finding #$i",
                imageUrl = "diagram://ctg_late_decel"
            )
        } + listOf(
            makeItem(
                id = "PYQ_SKIP_1",
                subject = "MSN",
                stem = "Tagged PYQ should stay excluded from CBT Custom Mock",
                sourceType = QuestionSourceType.OFFICIAL_PYQ
            )
        )

        // 1. Exclude IBQs + filter to MSN + Fresh Unseen + Untimed + No negative marking
        val freshNoIbqConfig = CustomMockConfig(
            questionCount = 15,
            selectedSubjectCodes = setOf("MSN"),
            sourceFilter = CustomMockSourceFilter.FRESH_UNSEEN,
            includeIbqs = false,
            timedMode = false,
            durationMinutes = 15,
            enableNegativeMarking = false,
            negativeMarkPenalty = 0.33
        )
        assertEquals(0.0, freshNoIbqConfig.effectiveNegativeMarks, 0.0001)

        val filteredFresh = CustomMockBuilderLogic.filterAvailablePool(pool, freshNoIbqConfig)
        assertEquals(15, filteredFresh.size)
        assertFalse(filteredFresh.any { it.question.isPyqTagged })
        assertFalse(filteredFresh.any { it.question.isImageBased })

        val validationFull = CustomMockBuilderLogic.validate(pool, freshNoIbqConfig)
        assertTrue(validationFull.canStart)
        assertEquals(15, validationFull.availableCount)
        assertEquals(15, validationFull.effectiveCount)

        // 2. Mistakes & Weak filter with partial fill (only 3 mistakes in MSN, requested 15)
        val mistakesConfig = CustomMockConfig(
            questionCount = 15,
            selectedSubjectCodes = setOf("MSN"),
            sourceFilter = CustomMockSourceFilter.MISTAKES_AND_WEAK,
            includeIbqs = true,
            timedMode = true,
            durationMinutes = 15,
            enableNegativeMarking = true,
            negativeMarkPenalty = 0.33
        )
        val validationPartial = CustomMockBuilderLogic.validate(pool, mistakesConfig)
        assertTrue(validationPartial.canStart)
        assertEquals(3, validationPartial.availableCount)
        assertEquals(3, validationPartial.effectiveCount)
        assertEquals(0.33, mistakesConfig.effectiveNegativeMarks, 0.0001)

        // 3. Include IBQs across all subjects -> interleaves visual IBQs cleanly & excludes PYQs
        val mixedConfig = CustomMockConfig(
            questionCount = 20,
            selectedSubjectCodes = emptySet(),
            sourceFilter = CustomMockSourceFilter.ALL_BANK,
            includeIbqs = true,
            timedMode = true,
            durationMinutes = 18,
            enableNegativeMarking = true,
            negativeMarkPenalty = 0.33
        )
        val built = CustomMockBuilderLogic.buildMockSessionQuestions(pool, mixedConfig)
        assertEquals(20, built.size)
        assertTrue("Custom mock with IBQs enabled should include visual IBQs", built.any { it.question.isImageBased })
        assertFalse("PYQs must stay out of CBT mocks", built.any { it.question.isPyqTagged })

        val title = CustomMockBuilderLogic.buildTitle(mixedConfig, actualCount = built.size)
        assertTrue(title.contains("20Q"))
        assertTrue(title.contains("Custom"))
    }
}
