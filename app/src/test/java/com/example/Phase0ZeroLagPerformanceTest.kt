package com.example

import com.example.data.model.PerformanceAggregationEngine
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.SessionTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM regression and performance guardrails for Phase 0 Zero-Lag Overhaul (v3.5.0).
 * Verifies one-time static stem deduplication, O(1) reference-reusing progress overlays,
 * single-pass O(N) summary aggregation at 12,500+ MCQ scale, and isolated SessionTimerState.
 */
class Phase0ZeroLagPerformanceTest {

    private fun buildSyntheticQuestion(index: Int): Question {
        val subjects = listOf("MSN", "Pharm", "OBG", "Peds", "CHN", "Psych", "FON", "Anat", "Math", "Reasoning")
        val subj = subjects[index % subjects.size]
        val isPyq = index % 7 == 0
        val isMock = !isPyq && index % 11 == 0
        val isIbq = index % 250 == 0
        val diff = when (index % 3) {
            0 -> QuestionDifficulty.EASY
            1 -> QuestionDifficulty.MEDIUM
            else -> QuestionDifficulty.HARD
        }
        return Question(
            id = "Q_PERF_$index",
            category = if (isPyq) "pyq" else if (isMock) "mock" else "nur",
            shiftYear = if (isPyq) "2024-S1" else "",
            subject = subj,
            difficulty = diff,
            questionText = "Clinical stem number $index for $subj nursing officer assessment",
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctIndex = index % 4,
            explanation = "Clinical rationale for question $index.",
            isCustom = index == 9999,
            sourceType = when {
                isPyq -> QuestionSourceType.MEMORY_BASED
                isMock -> QuestionSourceType.PREDICTED_MOCK
                else -> QuestionSourceType.PRACTICE
            },
            imageUrl = if (isIbq) "ibq_ecg_stemi" else ""
        )
    }

    @Test
    fun `static deduplication removes normalized stem duplicates and tracks alias IDs`() {
        val q1 = buildSyntheticQuestion(1).copy(
            id = "Q_ORIG",
            subject = "MSN",
            questionText = "  Which   electrolyte imbalance causes peaked T waves? "
        )
        val q2 = buildSyntheticQuestion(2).copy(
            id = "Q_DUP",
            subject = "MSN",
            questionText = "which electrolyte imbalance causes peaked t waves?"
        )
        val q3 = buildSyntheticQuestion(3).copy(
            id = "Q_OTHER_SUBJ",
            subject = "Pharm",
            questionText = "which electrolyte imbalance causes peaked t waves?"
        )

        val catalog = PerformanceAggregationEngine.buildStaticDedupCatalog(listOf(q1, q2, q3))
        assertEquals(2, catalog.canonicalQuestions.size)
        assertEquals("Q_ORIG", catalog.canonicalQuestions[0].id)
        assertEquals("Q_OTHER_SUBJ", catalog.canonicalQuestions[1].id)
        assertEquals(listOf("Q_DUP"), catalog.aliasIdsByCanonicalId["Q_ORIG"])

        // Progress logged on duplicate alias ID must surface on canonical item via O(1) alias merge
        val progressMap = mapOf(
            "Q_DUP" to QuestionProgress(
                questionId = "Q_DUP",
                isBookmarked = true,
                attemptsCount = 3,
                correctCount = 2,
                wrongCount = 1,
                lastOutcome = "right"
            )
        )
        val items = PerformanceAggregationEngine.overlayProgress(
            questions = catalog.canonicalQuestions,
            progressMap = progressMap,
            previousItems = null,
            aliasIdsByCanonicalId = catalog.aliasIdsByCanonicalId
        )
        assertEquals(2, items.size)
        assertTrue(items[0].isBookmarked)
        assertEquals(3, items[0].attemptsCount)
        assertEquals("Q_ORIG", items[0].progress.questionId)
    }

    @Test
    fun `overlayProgress reuses 12499 of 12500 QuestionItem references on single answer update`() {
        val total = 12_500
        val questions = List(total) { buildSyntheticQuestion(it) }
        val initialProgress = mapOf(
            "Q_PERF_0" to QuestionProgress("Q_PERF_0", attemptsCount = 1, correctCount = 1, lastOutcome = "right")
        )

        val pass1 = PerformanceAggregationEngine.overlayProgress(
            questions = questions,
            progressMap = initialProgress,
            previousItems = null
        )
        assertEquals(total, pass1.size)

        // Simulate answering 1 single question (Q_PERF_5000)
        val updatedProgress = initialProgress + (
            "Q_PERF_5000" to QuestionProgress(
                questionId = "Q_PERF_5000",
                isBookmarked = false,
                attemptsCount = 1,
                correctCount = 0,
                wrongCount = 1,
                lastOutcome = "wrong"
            )
        )

        val startNs = System.nanoTime()
        val pass2 = PerformanceAggregationEngine.overlayProgress(
            questions = questions,
            progressMap = updatedProgress,
            previousItems = pass1
        )
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        assertEquals(total, pass2.size)
        var reusedCount = 0
        for (i in 0 until total) {
            if (pass1[i] === pass2[i]) {
                reusedCount++
            }
        }
        assertEquals(total - 1, reusedCount)
        assertSame(pass1[0], pass2[0])
        assertNotSame(pass1[5000], pass2[5000])
        assertEquals("wrong", pass2[5000].lastOutcome)
        assertTrue("overlayProgress on 12,500 items should stay well under 200ms (was ${elapsedMs}ms)", elapsedMs < 200.0)
    }

    @Test
    fun `computePoolSummary computes exact single-pass metrics rapidly for 12500 questions`() {
        val total = 12_500
        val questions = List(total) { buildSyntheticQuestion(it) }
        val progressMap = HashMap<String, QuestionProgress>()
        for (i in 0 until 500) {
            progressMap["Q_PERF_$i"] = QuestionProgress(
                questionId = "Q_PERF_$i",
                isBookmarked = i % 2 == 0,
                attemptsCount = 2,
                correctCount = if (i % 3 == 0) 1 else 2,
                wrongCount = if (i % 3 == 0) 1 else 0,
                lastOutcome = if (i % 3 == 0) "wrong" else "right"
            )
        }
        val items = PerformanceAggregationEngine.overlayProgress(questions, progressMap)

        val startNs = System.nanoTime()
        val summary = PerformanceAggregationEngine.computePoolSummary(items, items)
        val mistakes = PerformanceAggregationEngine.extractStageMistakes(items)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        val expectedPyq = items.count { it.question.isPyqTagged }
        val expectedIbq = items.count { it.question.isImageBased }
        val expectedBookmarks = items.count { it.isBookmarked }
        val expectedWrongLatest = items.count { it.lastOutcome == "wrong" }

        assertEquals(total, summary.totalQuestions)
        assertEquals(500, summary.attemptedUnique)
        assertEquals(1000, summary.totalAttempts)
        assertEquals(expectedPyq, summary.pyqCount)
        assertEquals(expectedIbq, summary.imageBasedCount)
        assertEquals(expectedBookmarks, summary.bookmarkCount)
        assertEquals(expectedWrongLatest, summary.latestWrong)
        assertEquals(expectedWrongLatest, mistakes.size)
        assertEquals(1, summary.customCount)
        assertTrue("Single-pass summary for 12,500 items should run under 200ms (was ${elapsedMs}ms)", elapsedMs < 200.0)
    }

    @Test
    fun `SessionTimerState holds isolated countdown values cleanly`() {
        val initial = SessionTimerState(remainingSeconds = 5400, sectionRemainingSeconds = 1080)
        val next = initial.copy(
            remainingSeconds = initial.remainingSeconds - 1,
            sectionRemainingSeconds = initial.sectionRemainingSeconds - 1
        )
        assertEquals(5399, next.remainingSeconds)
        assertEquals(1079, next.sectionRemainingSeconds)
    }
}
