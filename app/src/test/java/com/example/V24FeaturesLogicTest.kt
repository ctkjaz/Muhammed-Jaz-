package com.example

import com.example.data.model.ClinicalCalculatorLogic
import com.example.data.model.ExamCountdownLogic
import com.example.data.model.NormalLabValuesCatalog
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SrsStage
import com.example.data.parser.QuestionSubjectClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class V24FeaturesLogicTest {

    private fun testQuestion(
        id: String,
        questionText: String,
        subject: String = "MSN",
        options: List<String> = listOf("A", "B", "C", "D"),
        correctIndex: Int = 0,
        explanation: String = ""
    ): Question = Question(
        id = id,
        category = "pyq",
        shiftYear = "PYQ 2025",
        subject = subject,
        questionText = questionText,
        options = options,
        correctIndex = correctIndex,
        explanation = explanation
    )

    // ------------------------------------------------------------------------
    // 1. AUTO-PYQ SMART SUBJECT SCANNER & AUTO-DISTRIBUTOR
    // ------------------------------------------------------------------------

    @Test
    fun subjectClassifier_distributesMixedPyqAcrossNursingAndNonTechSubjects() {
        val mixedQuestions = listOf(
            testQuestion(
                id = "q_obg",
                questionText = "Which drug is the drug of choice for preventing seizures in severe preeclampsia and eclampsia?",
                options = listOf("Magnesium sulfate", "Oxytocin", "Phenytoin", "Diazepam"),
                correctIndex = 0,
                explanation = "MgSO4 is the first-line agent in preeclampsia during pregnancy and labor."
            ),
            testQuestion(
                id = "q_peds",
                questionText = "At what age does the anterior fontanelle normally close in a healthy infant?",
                options = listOf("2-3 months", "6-8 months", "12-18 months", "24-30 months"),
                correctIndex = 2,
                explanation = "Anterior fontanelle closes between 12 and 18 months as a pediatric developmental milestone."
            ),
            testQuestion(
                id = "q_pharm",
                questionText = "What is the specific antidote for Heparin overdose causing bleeding?",
                options = listOf("Vitamin K", "Protamine sulfate", "Naloxone", "Flumazenil"),
                correctIndex = 1,
                explanation = "Protamine sulfate is the pharmacological antidote to reverse unfractionated heparin toxicity."
            ),
            testQuestion(
                id = "q_psych",
                questionText = "A patient with schizophrenia exhibits waxy flexibility and mutism. Which subtype is this?",
                options = listOf("Paranoid", "Catatonic", "Hebephrenic", "Residual"),
                correctIndex = 1,
                explanation = "Catatonia and waxy flexibility are classic psychiatric findings in catatonic schizophrenia."
            ),
            testQuestion(
                id = "q_chn",
                questionText = "At which level of the Indian healthcare system is a Primary Health Centre (PHC) with Cold Chain ILR established?",
                options = listOf("30,000 population in plains", "5,000 population", "1,00,000 population", "1,000 population"),
                correctIndex = 0,
                explanation = "Under community health norms, a PHC covers 30,000 in plains and stores vaccines under Universal Immunization."
            ),
            testQuestion(
                id = "q_math",
                questionText = "Find the compound interest on Rs. 10,000 at 10% per annum for 2 years.",
                options = listOf("2000", "2100", "2200", "2500"),
                correctIndex = 1,
                explanation = "Compound interest percentage calculation yields Rs. 2100."
            ),
            testQuestion(
                id = "q_explicit",
                questionText = "Subject: Anat — Which cranial nerve supplies the lateral rectus muscle of the eye?",
                options = listOf("CN III", "CN IV", "CN VI", "CN VII"),
                correctIndex = 2,
                explanation = "Abducens nerve (CN VI) innervates lateral rectus."
            )
        )

        val distributed = QuestionSubjectClassifier.autoDistributeBySubject(mixedQuestions)
        val byId = distributed.associateBy { it.id }

        assertEquals("OBG", byId["q_obg"]?.subject)
        assertEquals("Peds", byId["q_peds"]?.subject)
        assertEquals("Pharm", byId["q_pharm"]?.subject)
        assertEquals("Psych", byId["q_psych"]?.subject)
        assertEquals("CHN", byId["q_chn"]?.subject)
        assertEquals("Math", byId["q_math"]?.subject)
        assertEquals("Anat", byId["q_explicit"]?.subject)
        assertTrue(distributed.all { it.topicId.isNotBlank() })

        val summary = QuestionSubjectClassifier.summarizeSubjectDistribution(distributed)
        assertEquals(7, summary.size)
        assertEquals(7, summary.sumOf { it.second })
    }

    // ------------------------------------------------------------------------
    // 2. SPACED REPETITION (SRS) MISTAKE NOTEBOOK
    // ------------------------------------------------------------------------

    @Test
    fun spacedRepetition_progressesAcrossBoxesAndResetsOnMistake() {
        val t0 = 1_700_000_000_000L
        val oneDayMs = 24L * 60L * 60L * 1000L

        // Wrong answer enters Box 1d and is due immediately
        var cards = SpacedRepetitionLogic.recordWrong(emptyMap(), "Q1", nowMs = t0)
        assertEquals(SrsStage.BOX_1D, cards["Q1"]?.stage)
        assertTrue(SpacedRepetitionLogic.isDue(cards["Q1"]!!, nowMs = t0))

        // Correct recall advances 1d -> 3d -> 7d -> 14d -> Mastered
        cards = SpacedRepetitionLogic.recordRight(cards, "Q1", nowMs = t0)
        assertEquals(SrsStage.BOX_3D, cards["Q1"]?.stage)
        assertFalse(SpacedRepetitionLogic.isDue(cards["Q1"]!!, nowMs = t0 + oneDayMs))
        assertTrue(SpacedRepetitionLogic.isDue(cards["Q1"]!!, nowMs = t0 + 3 * oneDayMs))

        cards = SpacedRepetitionLogic.recordRight(cards, "Q1", nowMs = t0 + 3 * oneDayMs)
        assertEquals(SrsStage.BOX_7D, cards["Q1"]?.stage)

        cards = SpacedRepetitionLogic.recordRight(cards, "Q1", nowMs = t0 + 10 * oneDayMs)
        assertEquals(SrsStage.BOX_14D, cards["Q1"]?.stage)

        cards = SpacedRepetitionLogic.recordRight(cards, "Q1", nowMs = t0 + 24 * oneDayMs)
        assertEquals(SrsStage.MASTERED, cards["Q1"]?.stage)
        assertFalse(SpacedRepetitionLogic.isDue(cards["Q1"]!!, nowMs = t0 + 100 * oneDayMs))

        // Another mistake resets immediately back to Box 1d
        cards = SpacedRepetitionLogic.recordWrong(cards, "Q1", nowMs = t0 + 30 * oneDayMs)
        assertEquals(SrsStage.BOX_1D, cards["Q1"]?.stage)
        assertEquals(0, cards["Q1"]?.consecutiveCorrect)
    }

    @Test
    fun spacedRepetition_buildSummaryAutoSeedsExistingMistakes() {
        val now = 1_700_000_000_000L
        val q1 = QuestionItem(
            question = testQuestion(id = "Q_WRONG", questionText = "Missed Q", subject = "MSN"),
            progress = QuestionProgress(questionId = "Q_WRONG", wrongCount = 1, lastOutcome = "wrong", updatedAt = now - 5_000L)
        )
        val q2 = QuestionItem(
            question = testQuestion(id = "Q_RIGHT", questionText = "Right Q", subject = "OBG"),
            progress = QuestionProgress(questionId = "Q_RIGHT", correctCount = 1, lastOutcome = "right", updatedAt = now)
        )

        val summary = SpacedRepetitionLogic.buildSummary(listOf(q1, q2), emptyMap(), nowMs = now)
        assertEquals(1, summary.allEntries.size)
        assertEquals(1, summary.dueEntries.size)
        assertEquals("Q_WRONG", summary.dueEntries.first().item.id)
    }

    // ------------------------------------------------------------------------
    // 3. TARGET EXAM COUNTDOWN & STUDY ROADMAP
    // ------------------------------------------------------------------------

    @Test
    fun examCountdown_computesPhasesAndDailyTarget() {
        val tz = TimeZone.getTimeZone("UTC")
        val nowMs = 1_760_000_000_000L
        val in10Days = ExamCountdownLogic.isoDateAfterDays(10, nowMs, tz)
        val in35Days = ExamCountdownLogic.isoDateAfterDays(35, nowMs, tz)
        val in80Days = ExamCountdownLogic.isoDateAfterDays(80, nowMs, tz)

        assertEquals(10, ExamCountdownLogic.daysUntil(in10Days, nowMs, tz))
        assertNull(ExamCountdownLogic.daysUntil("not-a-date", nowMs, tz))

        val sprint = ExamCountdownLogic.computeRoadmap(in10Days, totalBankCount = 1000, attemptedCount = 200, currentDailyGoal = 30, nowMs = nowMs, tz = tz)
        assertTrue(sprint.isConfigured)
        assertEquals(10, sprint.daysRemaining)
        assertTrue(sprint.phaseTitle.contains("Final Sprint"))
        assertEquals(80, sprint.recommendedDailyQs)

        val consolidation = ExamCountdownLogic.computeRoadmap(in35Days, totalBankCount = 1000, attemptedCount = 300, currentDailyGoal = 30, nowMs = nowMs, tz = tz)
        assertTrue(consolidation.phaseTitle.contains("Consolidation"))

        val foundation = ExamCountdownLogic.computeRoadmap(in80Days, totalBankCount = 1000, attemptedCount = 100, currentDailyGoal = 30, nowMs = nowMs, tz = tz)
        assertTrue(foundation.phaseTitle.contains("Foundation"))
    }

    // ------------------------------------------------------------------------
    // 4. NURSING CLINICAL CALCULATORS & NORMAL LAB VALUES CATALOG
    // ------------------------------------------------------------------------

    @Test
    fun clinicalCalculators_produceAccurateNursingResults() {
        // IV Drip: 1000 mL over 8h with 15 gtt/mL => 125 mL/hr, 31 gtt/min
        val iv = ClinicalCalculatorLogic.calculateIvDrip(1000.0, 8.0, 15)
        assertNotNull(iv)
        assertEquals(125.0, iv!!.mlPerHour, 0.01)
        assertEquals(31, iv.dropsPerMinute)
        assertNull(ClinicalCalculatorLogic.calculateIvDrip(0.0, 8.0, 15))

        // Pediatric Dose: 15 kg * 40 mg/kg/day / 3 doses => 600 mg/day, 200 mg/dose; Young's for 4yo, 500mg adult => 125 mg
        val peds = ClinicalCalculatorLogic.calculatePediatricDose(15.0, 40.0, 3, adultDoseMg = 500.0, childAgeYears = 4.0)
        assertNotNull(peds)
        assertEquals(600.0, peds!!.totalDailyMg, 0.01)
        assertEquals(200.0, peds.singleDoseMg, 0.01)
        assertEquals(125.0, peds.youngsRuleMg!!, 0.01)

        // Parkland Burn: 70 kg * 40% TBSA * 4 = 11200 mL total -> 5600 first 8h (700/hr) -> 5600 next 16h (350/hr)
        val burn = ClinicalCalculatorLogic.calculateParklandBurns(70.0, 40.0)
        assertNotNull(burn)
        assertEquals(11200.0, burn!!.total24hMl, 0.01)
        assertEquals(5600.0, burn.first8hMl, 0.01)
        assertEquals(700.0, burn.first8hMlPerHr, 0.01)
        assertEquals(350.0, burn.next16hMlPerHr, 0.01)

        // GCS <= 8 triggers airway alert
        val gcsSevere = ClinicalCalculatorLogic.evaluateGcs(2, 2, 4)
        assertEquals(8, gcsSevere.total)
        assertTrue(gcsSevere.airwayAlert)
        val gcsMild = ClinicalCalculatorLogic.evaluateGcs(4, 5, 6)
        assertEquals(15, gcsMild.total)
        assertFalse(gcsMild.airwayAlert)

        // APGAR & Bishop
        val apgar = ClinicalCalculatorLogic.evaluateApgar(2, 2, 1, 2, 2)
        assertEquals(9, apgar.total)
        assertTrue(apgar.interpretation.contains("Normal"))

        val bishop = ClinicalCalculatorLogic.evaluateBishop(2, 2, 1, 2, 2)
        assertEquals(9, bishop.total)
        assertTrue(bishop.readiness.contains("Favorable"))

        // Normal Lab Values Catalog completeness
        assertTrue(NormalLabValuesCatalog.ALL_VALUES.size >= 36)
        assertTrue(NormalLabValuesCatalog.ALL_VALUES.any { it.name.contains("Potassium") })
        assertTrue(NormalLabValuesCatalog.ALL_VALUES.any { it.name.contains("Fetal Heart Rate") })
    }
}
