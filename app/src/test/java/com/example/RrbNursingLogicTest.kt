package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RrbNursingLogicTest {

    @Test
    fun `builtin questions asset contains 1223 valid MCQs across difficulties`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = context.assets.open("builtin_questions.json").bufferedReader().use { it.readText() }
        val parsed = BulkQuestionParser.parseJsonBatch(json, markAsCustom = false)

        assertEquals(1223, parsed.size)
        assertEquals(1223, parsed.map { it.id }.toSet().size)
        assertTrue((1..601).all { id -> parsed.any { it.id == id.toString() } })
        assertEquals(0, parsed.count { it.sourceStatus == Question.SOURCE_VERIFIED })
        assertEquals(1223, parsed.count { it.sourceStatus == Question.SOURCE_UNVERIFIED })
        parsed.forEach { q ->
            assertTrue("Question text blank for ${q.id}", q.questionText.isNotBlank())
            assertEquals("Options count != 4 for ${q.id}", 4, q.options.size)
            assertTrue("Correct index out of range for ${q.id}", q.correctIndex in 0..3)
            assertTrue("Explanation blank for ${q.id}", q.explanation.isNotBlank())
            assertEquals("Built-in question must be unverified for ${q.id}", Question.SOURCE_UNVERIFIED, q.sourceStatus)
            assertEquals("Built-in sourceNote must be empty for ${q.id}", "", q.sourceNote)
            if (q.isPyqTagged) {
                assertEquals("Unverified PYQ tag", q.pyqBadgeText())
            }
        }
        assertEquals(683, parsed.count { it.category == "pyq" })
        assertEquals(707, parsed.count { it.isPyqTagged })
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
        assertEquals("PYQ-Tagged Questions", PdfWorksheetWriter.sourceDisplayName("PYQ"))

        assertEquals("Q1  •  MSN  •  Hard", PdfWorksheetWriter.formatQuestionTagLine(0, q1))
        assertEquals(
            "Q2  •  Pharm  •  Easy  •  Unverified PYQ tag",
            PdfWorksheetWriter.formatQuestionTagLine(1, q2)
        )
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
    fun `importer enforces non-empty sourceNote rule for verified PYQs and restores pre-1B backups cleanly`() {
        val jsonInput = """
            [
              {
                "id": "J_1",
                "c": "pyq",
                "y": "2025-S1",
                "s": "MSN",
                "d": "Medium",
                "q": "Verified PYQ with note",
                "o": ["A", "B", "C", "D"],
                "a": 0,
                "e": "Exp",
                "sourceStatus": "verified",
                "sourceNote": "Official RRB 2025 Shift 1 PDF"
              },
              {
                "id": "J_2",
                "c": "pyq",
                "y": "2025-S1",
                "s": "MSN",
                "d": "Medium",
                "q": "Verified flag without source note must downgrade",
                "o": ["A", "B", "C", "D"],
                "a": 1,
                "e": "Exp",
                "sourceStatus": "verified",
                "sourceNote": "   "
              },
              {
                "id": "J_3",
                "c": "nur",
                "y": "",
                "s": "Pharm",
                "d": "Easy",
                "q": "Non-PYQ default",
                "o": ["A", "B", "C", "D"],
                "a": 2,
                "e": "Exp"
              }
            ]
        """.trimIndent()

        val imported = BulkQuestionParser.parseJsonBatch(jsonInput, markAsCustom = true)
        assertEquals(3, imported.size)
        assertEquals(Question.SOURCE_VERIFIED, imported[0].sourceStatus)
        assertEquals("Official RRB 2025 Shift 1 PDF", imported[0].sourceNote)
        assertEquals("Verified PYQ • 2025-S1", imported[0].pyqBadgeText())

        assertEquals(Question.SOURCE_UNVERIFIED, imported[1].sourceStatus)
        assertEquals("", imported[1].sourceNote)
        assertEquals("Unverified PYQ tag", imported[1].pyqBadgeText())

        assertEquals(Question.SOURCE_UNVERIFIED, imported[2].sourceStatus)
        assertNull(imported[2].pyqBadgeText())

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
        assertEquals(Question.SOURCE_UNVERIFIED, pdfWithoutNote.first().sourceStatus)

        val pdfWithNote = BulkQuestionParser.parseRawPdfText(
            rawText = rawPdf,
            category = "pyq",
            subject = "FON",
            shiftYear = "2025-S2",
            defaultSourceNote = "Official RRB 2025 Shift 2 response sheet",
            markAsVerified = true
        )
        assertEquals(Question.SOURCE_VERIFIED, pdfWithNote.first().sourceStatus)
        assertEquals("Verified PYQ • 2025-S2", pdfWithNote.first().pyqBadgeText())

        // Pre-1B backup format (version 1, no sourceStatus or sourceNote keys)
        val pre1bBackup = """
            {
              "version": 1,
              "exportedAt": 1700000000000,
              "customQB": [
                {
                  "id": "C_OLD_1",
                  "c": "pyq",
                  "y": "2019-S1",
                  "s": "MSN",
                  "d": "Hard",
                  "q": "Legacy custom PYQ question",
                  "o": ["A", "B", "C", "D"],
                  "a": 0,
                  "e": "Legacy explanation"
                }
              ],
              "bookmarks": ["C_OLD_1", "1"],
              "progress": [
                {
                  "questionId": "1",
                  "isBookmarked": true,
                  "attemptsCount": 3,
                  "correctCount": 2,
                  "wrongCount": 1,
                  "lastOutcome": "right",
                  "updatedAt": 1700000000000
                }
              ]
            }
        """.trimIndent()

        val restoredOld = BulkQuestionParser.parseBackupJson(pre1bBackup)
        assertEquals(1, restoredOld.customQuestions.size)
        assertEquals(Question.SOURCE_UNVERIFIED, restoredOld.customQuestions.first().sourceStatus)
        assertEquals("", restoredOld.customQuestions.first().sourceNote)
        assertEquals(2, restoredOld.progressList.size)

        // Round-trip new backup preserves verified status and sourceNote
        val newBackup = BulkQuestionParser.buildBackupJson(
            listOf(QuestionItem(imported[0], QuestionProgress(imported[0].id, isBookmarked = true)))
        )
        val restoredNew = BulkQuestionParser.parseBackupJson(newBackup)
        assertEquals(1, restoredNew.customQuestions.size)
        assertEquals(Question.SOURCE_VERIFIED, restoredNew.customQuestions.first().sourceStatus)
        assertEquals("Official RRB 2025 Shift 1 PDF", restoredNew.customQuestions.first().sourceNote)
    }

    @Test
    fun `room migration 2 to 3 preserves existing questions progress bookmarks and mock history`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_2_3.db"
        context.deleteDatabase(dbName)

        // Create an authentic v2 SQLite database before opening with Room v3
        val v2Helper = object : SQLiteOpenHelper(context, dbName, null, 2) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `questions` (
                        `id` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `shiftYear` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `optionsJson` TEXT NOT NULL,
                        `correctIndex` INTEGER NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `question_progress` (
                        `questionId` TEXT NOT NULL,
                        `isBookmarked` INTEGER NOT NULL,
                        `attemptsCount` INTEGER NOT NULL,
                        `correctCount` INTEGER NOT NULL,
                        `wrongCount` INTEGER NOT NULL,
                        `lastOutcome` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`questionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mock_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correctCount` INTEGER NOT NULL,
                        `wrongCount` INTEGER NOT NULL,
                        `skippedCount` INTEGER NOT NULL,
                        `netScore` REAL NOT NULL,
                        `accuracyPercent` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `questions` VALUES (
                        '1', 'pyq', '2025-S1', 'MSN', 'Medium',
                        'Sample v2 question', '["A","B","C","D"]', 1,
                        'Sample rationale', 0, 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `question_progress` VALUES (
                        '1', 1, 5, 4, 1, 'right', 1700000000000
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `mock_history` (`title`,`totalQuestions`,`correctCount`,`wrongCount`,`skippedCount`,`netScore`,`accuracyPercent`,`timestamp`)
                    VALUES ('v2 Mock', 30, 24, 6, 0, 22.02, 80, 1700000000000)
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        v2Helper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val items = migratedDb.questionDao().observeAllQuestionsWithProgress().first()
            assertEquals(1, items.size)
            val row = items.first()
            assertEquals("1", row.question.id)
            assertEquals("Sample v2 question", row.question.questionText)
            assertEquals(Question.SOURCE_UNVERIFIED, row.question.sourceStatus)
            assertEquals("", row.question.sourceNote)
            assertEquals(true, row.progress?.isBookmarked)
            assertEquals(5, row.progress?.attemptsCount)
            assertEquals(4, row.progress?.correctCount)

            val history = migratedDb.questionDao().observeMockHistory().first()
            assertEquals(1, history.size)
            assertEquals("v2 Mock", history.first().title)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
