package com.example

import com.example.data.cloud.CloudCustomQuestion
import com.example.data.cloud.CloudMergeLogic
import com.example.data.cloud.CloudQuestionProgress
import com.example.data.export.PdfWorksheetWriter
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.AccountSessionLogic
import com.example.data.model.DailyStudyGoalState
import com.example.data.model.DifficultyFilter
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.SessionCheckResult
import com.example.data.model.StudyGoalLogic
import com.example.data.parser.BulkQuestionParser
import com.example.ui.components.formatApproxQuestionCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM mirror of all non-Android assertions in RrbNursingLogicTest
 * so regressions in parser, worksheet, cloud merge, study goals, and formatting
 * are caught locally without requiring Robolectric SDK downloads.
 */
class RrbPureLogicVerificationTest {

    @Test
    fun pdfTextParser_matchesRrbNursingLogicTestCases() {
        val raw = """
            Q.1 Which cranial nerve is responsible for vision?
            1. Olfactory
            2. Optic
            3. Oculomotor
            4. Trochlear
            Ans: 2
            Difficulty: Hard
            Explanation: CN II (Optic) transmits visual signals.
        """.trimIndent()

        val parsed = BulkQuestionParser.parseRawPdfText(
            rawText = raw,
            category = "pyq",
            subject = "Anat",
            shiftYear = "2025-S1",
            defaultDifficulty = QuestionDifficulty.EASY
        )

        assertEquals(1, parsed.size)
        val q = parsed.first()
        assertEquals("Which cranial nerve is responsible for vision?", q.questionText)
        assertEquals(listOf("Olfactory", "Optic", "Oculomotor", "Trochlear"), q.options)
        assertEquals(1, q.correctIndex)
        assertEquals(QuestionDifficulty.HARD, q.difficulty)
        assertEquals("CN II (Optic) transmits visual signals.", q.explanation)

        val rawPdf = """
            Q.1 Normal adult pulse range is?
            1. 40-50 bpm
            2. 60-100 bpm
            3. 110-130 bpm
            4. 130-150 bpm
            Ans: 2
        """.trimIndent()

        val pdfWithoutNote = BulkQuestionParser.parseRawPdfText(
            rawText = rawPdf,
            category = "pyq",
            subject = "FON",
            shiftYear = "2025-S2",
            defaultSourceNote = "",
            markAsVerified = true
        )
        assertEquals(1, pdfWithoutNote.size)
        assertEquals(Question.SOURCE_UNVERIFIED, pdfWithoutNote.first().sourceStatus)

        val pdfWithNote = BulkQuestionParser.parseRawPdfText(
            rawText = rawPdf,
            category = "pyq",
            subject = "FON",
            shiftYear = "2025-S2",
            defaultSourceNote = "Official RRB 2025 Shift 2 response sheet",
            markAsVerified = true
        )
        assertEquals(1, pdfWithNote.size)
        assertEquals(Question.SOURCE_VERIFIED, pdfWithNote.first().sourceStatus)
        assertEquals("Verified PYQ • 2025-S2", pdfWithNote.first().pyqBadgeText())
    }

    @Test
    fun pdfTextParser_handlesSequentialNumberedQuestionsWithNumberedOptions() {
        val raw = """
            Q.1 First question stem?
            1. Alpha option
            2. Beta option
            3. Gamma option
            4. Delta option
            Ans: 1

            Q.2 Second question stem?
            1. First choice
            2. Second choice
            3. Third choice
            4. Fourth choice
            Ans: 3
        """.trimIndent()

        val parsed = BulkQuestionParser.parseRawPdfText(
            rawText = raw,
            category = "nur",
            subject = "MSN",
            shiftYear = ""
        )
        assertEquals(2, parsed.size)
        assertEquals(listOf("First choice", "Second choice", "Third choice", "Fourth choice"), parsed[1].options)
        assertEquals(2, parsed[1].correctIndex)
    }

    @Test
    fun pdfWorksheetWriter_matchesRrbNursingLogicTestCases() {
        val q1 = Question(
            id = "10",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.HARD,
            questionText = "Which electrolyte imbalance triggers peaked T waves on ECG?",
            options = listOf("Hypokalemia", "Hyperkalemia", "Hyponatremia", "Hypercalcemia"),
            correctIndex = 1,
            explanation = "Hyperkalemia causes tall peaked T waves and widened QRS."
        )
        val q2 = Question(
            id = "11",
            category = "pyq",
            shiftYear = "2025-S2",
            subject = "Pharm",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Which drug is the antidote for heparin overdose?",
            options = listOf("Vitamin K", "Protamine sulfate", "Naloxone", "Flumazenil"),
            correctIndex = 1,
            explanation = "Protamine sulfate neutralizes unfractionated heparin."
        )
        val items = listOf(
            QuestionItem(q1, QuestionProgress("10", isBookmarked = true)),
            QuestionItem(q2, QuestionProgress("11", isBookmarked = false, lastOutcome = "wrong"))
        )

        val bookmarkedHard = PdfWorksheetWriter.resolveWorksheetQuestions(
            sourceKey = "BOOKMARKS",
            all = items,
            lowestAccuracy = emptyList(),
            difficultyFilter = DifficultyFilter.HARD,
            limit = 30
        )
        assertEquals(1, bookmarkedHard.size)
        assertEquals("10", bookmarkedHard.first().id)

        assertEquals(
            "rrb-nursing-bookmarks-hard-1q-mock.pdf",
            PdfWorksheetWriter.buildSuggestedPdfFileName("BOOKMARKS", DifficultyFilter.HARD, 1, false)
        )
        assertEquals(8, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = false, includeAnswerKey = true))
        assertEquals(5, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = false, includeAnswerKey = false))
        assertEquals(8, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = true, includeAnswerKey = false))
        assertEquals("Q1  •  MSN  •  Hard", PdfWorksheetWriter.formatQuestionTagLine(0, q1))
        assertEquals("Q2  •  Pharm  •  Easy  •  Unverified PYQ tag", PdfWorksheetWriter.formatQuestionTagLine(1, q2))
        val verifiedPyq = q2.copy(
            sourceStatus = Question.SOURCE_VERIFIED,
            sourceNote = "Official RRB 2025 Shift 2 key"
        )
        assertEquals(
            "Q2  •  Pharm  •  Easy  •  Verified PYQ • 2025-S2",
            PdfWorksheetWriter.formatQuestionTagLine(1, verifiedPyq)
        )
    }

    @Test
    fun helperLogic_approxCount_timer_cloudMerge_studyGoals_andSessionCheck() {
        assertEquals(27, SettingsPreferences.calculateSuggestedMinutes(30, 54))
        assertEquals("1,000+ Qs", formatApproxQuestionCount(1223))
        assertEquals("1,000+ questions", formatApproxQuestionCount(1223, verbose = true))
        assertEquals("2,000+ Qs", formatApproxQuestionCount(2450))
        assertEquals("500+ Qs", formatApproxQuestionCount(707))

        val localOlder = QuestionProgressEntity("N_12", false, 2, 1, 1, "wrong", 1000L)
        val cloudNewer = CloudQuestionProgress("N_12", "uid_1", "N_12", true, 4, 3, 1, "right", 2000L)
        val merged = CloudMergeLogic.mergeProgress(localOlder, cloudNewer)
        assertTrue(merged.isBookmarked)
        assertEquals(4, merged.attemptsCount)
        assertFalse(CloudMergeLogic.shouldPushLocalProgress(localOlder, cloudNewer))
        assertEquals(ThemePreference.LIGHT, CloudMergeLogic.parseThemeSafe("INVALID_THEME"))
        assertEquals(ThemePreference.DARK, CloudMergeLogic.parseThemeSafe("DARK"))

        val cloudQ = CloudCustomQuestion(
            id = "C_SYNC_1", userId = "uid_1", category = "pyq", shiftYear = "NORCET-9",
            subject = "MSN", difficulty = "Hard", questionText = "Which lead shows anterior MI?",
            options = listOf("V1-V4", "II, III, aVF", "aVR", "V5-V6"), correctIndex = 0,
            explanation = "Anterior wall is supplied by LAD (V1-V4).",
            sourceStatus = Question.SOURCE_UNVERIFIED, sourceNote = "",
            topicId = "msn_general", sourceType = "memory_based", examIds = listOf("aiims_norcet")
        )
        val domainQ = CloudMergeLogic.toDomainQuestion(cloudQ)
        assertEquals("C_SYNC_1", domainQ?.id)
        assertEquals(QuestionDifficulty.HARD, domainQ?.difficulty)

        val day1 = "2026-11-20"
        val day0 = "2026-11-19"
        val initial = DailyStudyGoalState(dailyGoalQuestions = 15)
        val met = StudyGoalLogic.recordStudyActivity(initial, answeredDelta = 15, todayKey = day1, yesterdayKey = day0)
        assertTrue(met.justMetDailyGoal)
        assertEquals(1, met.updatedState.currentStreakDays)

        assertEquals(
            SessionCheckResult.Valid,
            AccountSessionLogic.evaluateRemoteSession("sess-A", "sess-A", "Pixel 8")
        )
    }
}
