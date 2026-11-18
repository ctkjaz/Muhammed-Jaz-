package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.PdfWorksheetWriter
import com.example.data.local.AppDatabase
import com.example.data.model.DifficultyFilter
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.parser.BulkQuestionParser
import com.example.data.repository.NursingPrepRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RrbNursingLogicTest {

    @Test
    fun `builtin questions asset contains 601 valid MCQs across difficulties`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = context.assets.open("builtin_questions.json").bufferedReader().use { it.readText() }
        val parsed = BulkQuestionParser.parseJsonBatch(json, markAsCustom = false)

        assertEquals(601, parsed.size)
        parsed.forEach { q ->
            assertTrue("Question text blank for ${q.id}", q.questionText.isNotBlank())
            assertEquals("Options count != 4 for ${q.id}", 4, q.options.size)
            assertTrue("Correct index out of range for ${q.id}", q.correctIndex in 0..3)
        }
        assertTrue(parsed.any { it.difficulty == QuestionDifficulty.EASY })
        assertTrue(parsed.any { it.difficulty == QuestionDifficulty.MEDIUM })
        assertTrue(parsed.any { it.difficulty == QuestionDifficulty.HARD })
    }

    @Test
    fun `pdf text parser parses questions options answer difficulty and explanation`() {
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
    }

    @Test
    fun `backup json preserves custom question difficulty`() {
        val customQ = Question(
            id = "C_100",
            category = "nur",
            shiftYear = "",
            subject = "Pharm",
            difficulty = QuestionDifficulty.HARD,
            questionText = "Which receptor relaxes bladder detrusor?",
            options = listOf("α1", "β1", "β2", "β3"),
            correctIndex = 3,
            explanation = "Mirabegron stimulates β3 receptors.",
            isCustom = true
        )
        val json = BulkQuestionParser.buildBackupJson(
            listOf(QuestionItem(customQ, QuestionProgress("C_100", isBookmarked = true)))
        )
        val restored = BulkQuestionParser.parseBackupJson(json)
        assertEquals(1, restored.customQuestions.size)
        assertEquals(QuestionDifficulty.HARD, restored.customQuestions.first().difficulty)
        assertEquals(true, restored.progressList.first().isBookmarked)
    }

    @Test
    fun `repository computes subject category stats and lowest accuracy ranking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = NursingPrepRepository(context, db.questionDao())

        val q1 = Question(
            id = "1",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            difficulty = QuestionDifficulty.HARD,
            questionText = "Q1",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 0,
            explanation = "E1"
        )
        val q2 = Question(
            id = "2",
            category = "pyq",
            shiftYear = "2025-S1",
            subject = "Pharm",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Q2",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 1,
            explanation = "E2"
        )
        val items = listOf(
            QuestionItem(q1, QuestionProgress("1", attemptsCount = 4, correctCount = 1, wrongCount = 3, lastOutcome = "wrong")),
            QuestionItem(q2, QuestionProgress("2", attemptsCount = 2, correctCount = 2, wrongCount = 0, lastOutcome = "right"))
        )

        val subjStats = repo.computeSubjectStats(items)
        val msn = subjStats.first { it.code == "MSN" }
        assertEquals(25, msn.accuracyPercent)

        val catStats = repo.computeCategoryStats(items)
        val pyq = catStats.first { it.key == "pyq" }
        assertEquals(100, pyq.accuracyPercent)

        val lowest = repo.findLowestAccuracyQuestions(items)
        assertEquals(1, lowest.size)
        assertEquals("1", lowest.first().id)

        val hardOnly = items.filter { it.question.difficulty == DifficultyFilter.HARD.difficulty }
        assertEquals(1, hardOnly.size)
        assertEquals("1", hardOnly.first().id)
    }

    @Test
    fun `pdf worksheet writer filters scope builds filename and generates valid PDF`() {
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

        val filename = PdfWorksheetWriter.buildSuggestedPdfFileName(
            sourceKey = "BOOKMARKS",
            difficultyFilter = DifficultyFilter.HARD,
            questionCount = bookmarkedHard.size,
            inlineAnswers = false
        )
        assertEquals("rrb-nursing-bookmarks-hard-1q-mock.pdf", filename)

        val mistakesEasy = PdfWorksheetWriter.resolveWorksheetQuestions(
            sourceKey = "MISTAKES",
            all = items,
            lowestAccuracy = emptyList(),
            difficultyFilter = DifficultyFilter.EASY,
            limit = 0
        )
        assertEquals(1, mistakesEasy.size)
        assertEquals("11", mistakesEasy.first().id)

        val studyFileName = PdfWorksheetWriter.buildSuggestedPdfFileName(
            sourceKey = "MSN",
            difficultyFilter = DifficultyFilter.ALL,
            questionCount = 30,
            inlineAnswers = true
        )
        assertEquals("rrb-nursing-msn-all-30q-study.pdf", studyFileName)

        assertEquals(8, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = false, includeAnswerKey = true))
        assertEquals(5, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = false, includeAnswerKey = false))
        assertEquals(8, PdfWorksheetWriter.estimatePageCount(30, inlineAnswers = true, includeAnswerKey = false))
        assertEquals("Bookmarked Revision", PdfWorksheetWriter.sourceDisplayName("BOOKMARKS"))
    }
}
