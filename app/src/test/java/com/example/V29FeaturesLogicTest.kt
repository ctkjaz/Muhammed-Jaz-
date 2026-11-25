package com.example

import com.example.data.model.ExamReadinessSummary
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionNote
import com.example.data.model.QuestionNotesLogic
import com.example.data.model.QuestionProgress
import com.example.data.model.ReadinessBand
import com.example.data.model.ReadinessPillar
import com.example.data.model.ResultShareFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V29FeaturesLogicTest {

    private fun sampleItem(id: String, subject: String, stem: String): QuestionItem = QuestionItem(
        question = Question(
            id = id,
            category = "nur",
            shiftYear = "2025",
            subject = subject,
            questionText = stem,
            options = listOf("A", "B", "C", "D"),
            correctIndex = 0,
            explanation = "Sample explanation"
        ),
        progress = QuestionProgress(questionId = id)
    )

    @Test
    fun questionNotesLogic_upsertSanitizeAndRoundTripSerializeWithSpecialChars() {
        val initial = emptyMap<String, QuestionNote>()
        val rawNote = "  NORCET-6 Trap:\tCheck \"Left Lateral\" first!\n\n\n\\Atropine before 2-PAM  "
        val updated = QuestionNotesLogic.upsertNote(initial, "Q_101", rawNote, nowMs = 1_700_000_000_000L)

        assertEquals(1, updated.size)
        val saved = updated["Q_101"]!!
        assertFalse("Multiple blank lines should collapse", saved.text.contains("\n\n\n"))
        assertTrue(saved.text.startsWith("NORCET-6 Trap:"))
        assertTrue(saved.text.contains("\\Atropine"))

        val encoded = QuestionNotesLogic.serializeMap(updated)
        val decoded = QuestionNotesLogic.deserializeMap(encoded)
        assertEquals(1, decoded.size)
        assertEquals(saved.text, decoded["Q_101"]?.text)
        assertEquals(1_700_000_000_000L, decoded["Q_101"]?.updatedAtMs)
    }

    @Test
    fun questionNotesLogic_blankTextRemovesNoteAndEnforcesMaxLength() {
        val longText = "A".repeat(750)
        val map1 = QuestionNotesLogic.upsertNote(emptyMap(), "Q1", longText, nowMs = 100L)
        assertEquals(QuestionNotesLogic.MAX_NOTE_LENGTH, map1["Q1"]?.text?.length)

        val map2 = QuestionNotesLogic.upsertNote(map1, "Q1", "   \n  ", nowMs = 200L)
        assertTrue("Blank note should delete entry", map2.isEmpty())
    }

    @Test
    fun questionNotesLogic_searchOrdersByNewestAndMatchesStemSubjectOrNote() {
        val q1 = sampleItem("Q1", "OBG", "Management of late decelerations in FHR?")
        val q2 = sampleItem("Q2", "Pharm", "Antidote for organophosphate poisoning?")
        val notes = mapOf(
            "Q1" to QuestionNote("Q1", "Use LION bundle", updatedAtMs = 1000L),
            "Q2" to QuestionNote("Q2", "Atropine first then 2-PAM", updatedAtMs = 2000L)
        )

        val all = QuestionNotesLogic.searchNotedQuestions(listOf(q1, q2), notes)
        assertEquals(listOf("Q2", "Q1"), all.map { it.first.id })

        val byNote = QuestionNotesLogic.searchNotedQuestions(listOf(q1, q2), notes, "LION")
        assertEquals(listOf("Q1"), byNote.map { it.first.id })

        val bySubject = QuestionNotesLogic.searchNotedQuestions(listOf(q1, q2), notes, "pharm")
        assertEquals(listOf("Q2"), bySubject.map { it.first.id })
    }

    @Test
    fun resultShareFormatter_formatsCbtScoreCardAndReadinessReportCleanly() {
        val cbtText = ResultShareFormatter.formatCbtResultShare(
            title = "AIIMS NORCET Full Mock #1",
            netScore = 78.67,
            totalQuestions = 100,
            right = 82,
            wrong = 10,
            skipped = 8,
            accuracyPercent = 89,
            negativeMarks = 0.33,
            topSubjectLine = "OBG (95%)"
        )
        assertTrue(cbtText.contains("AIIMS NORCET Full Mock #1"))
        assertTrue(cbtText.contains("78.67 / 100"))
        assertTrue(cbtText.contains("Accuracy: 89%"))
        assertTrue(cbtText.contains("OBG (95%)"))

        val readiness = ExamReadinessSummary(
            overallScore = 82,
            band = ReadinessBand.EXAM_READY,
            pillars = listOf(
                ReadinessPillar("accuracy", "Accuracy", 88, 30, "88%"),
                ReadinessPillar("coverage", "Syllabus Coverage", 76, 25, "76%")
            ),
            weakestSubject = null,
            nextBestAction = "Take a timed PYQ drill.",
            estimatedRankTier = "Top 15% Merit Contender",
            hasEnoughData = true
        )
        val report = ResultShareFormatter.formatReadinessShare("RRB Nursing", readiness, streakDays = 6)
        assertTrue(report.contains("RRB Nursing"))
        assertTrue(report.contains("82% (Exam Ready)"))
        assertTrue(report.contains("6 days"))
        assertTrue(report.contains("- Accuracy: 88%"))
    }
}
