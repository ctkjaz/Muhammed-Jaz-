package com.example

import com.example.data.model.ClinicalImageRef
import com.example.data.model.ClinicalSpotterCatalog
import com.example.data.model.IbqCategory
import com.example.data.model.IbqHubLogic
import com.example.data.model.IbqStatusFilter
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4IbqHubLogicTest {

    private fun toItem(
        q: Question,
        attempts: Int = 0,
        correct: Int = 0,
        lastOutcome: String? = null
    ) = QuestionItem(
        question = q,
        progress = QuestionProgress(
            questionId = q.id,
            isBookmarked = false,
            attemptsCount = attempts,
            correctCount = correct,
            wrongCount = (attempts - correct).coerceAtLeast(0),
            lastOutcome = lastOutcome
        )
    )

    private fun textItem(id: String, subject: String = "MSN") = toItem(
        Question(
            id = id,
            category = "nur",
            shiftYear = "",
            subject = subject,
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "Clinical text MCQ $id",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 0,
            explanation = "Verified clinical explanation for $id with nursing rationale.",
            isCustom = false,
            sourceType = QuestionSourceType.PRACTICE,
            imageUrl = ""
        )
    )

    @Test
    fun builtinSpotterCatalog_expandsTo25OfflineVisualsCoveringAllFiveCategories() {
        val spotters = ClinicalSpotterCatalog.builtinSpotterQuestions()
        assertEquals(25, spotters.size)
        assertEquals(25, spotters.map { it.id }.distinct().size)

        val byCategory = spotters.groupBy { IbqHubLogic.categorize(it) }
        val expectedCategories = listOf(
            IbqCategory.ECG,
            IbqCategory.AIRWAY_OT,
            IbqCategory.OBG_CTG,
            IbqCategory.DEVICES_COLOR,
            IbqCategory.CLINICAL_IMAGING
        )
        expectedCategories.forEach { cat ->
            val list = byCategory[cat].orEmpty()
            assertTrue("Category ${cat.label} should contain at least 2 IBQs, got ${list.size}", list.size >= 2)
        }

        spotters.forEach { q ->
            val ref = ClinicalImageRef.parse(q.imageUrl)
            assertTrue("${q.id} should parse cleanly", ref is ClinicalImageRef.Diagram || ref is ClinicalImageRef.Drawable)
            assertTrue("${q.id} badge label should be non-blank", ClinicalSpotterCatalog.badgeLabelFor(ref).isNotBlank())
            assertTrue("${q.id} short spotter title should be non-blank", IbqHubLogic.shortSpotterTitle(q).isNotBlank())
        }
    }

    @Test
    fun ibqHubLogic_filtersByCategoryStatusAndSearch() {
        val spotters = ClinicalSpotterCatalog.builtinSpotterQuestions()
        val items = spotters.mapIndexed { idx, q ->
            when {
                idx % 3 == 0 -> toItem(q, attempts = 0, correct = 0, lastOutcome = null)
                idx % 3 == 1 -> toItem(q, attempts = 1, correct = 0, lastOutcome = "wrong")
                else -> toItem(q, attempts = 1, correct = 1, lastOutcome = "right")
            }
        }

        val obgAll = IbqHubLogic.filterQuestions(items, category = IbqCategory.OBG_CTG)
        assertTrue(obgAll.size >= 4) // Cusco's speculum + 2 CTGs + Partograph
        assertTrue(obgAll.all { IbqHubLogic.categorize(it.question) == IbqCategory.OBG_CTG })

        val mistakesOnly = IbqHubLogic.filterQuestions(items, statusFilter = IbqStatusFilter.MISTAKES)
        assertTrue(mistakesOnly.isNotEmpty())
        assertTrue(mistakesOnly.all { it.lastOutcome == "wrong" })

        val masteredOnly = IbqHubLogic.filterQuestions(items, statusFilter = IbqStatusFilter.MASTERED)
        assertTrue(masteredOnly.isNotEmpty())
        assertTrue(masteredOnly.all { it.lastOutcome == "right" })

        val unseenOnly = IbqHubLogic.filterQuestions(items, statusFilter = IbqStatusFilter.UNATTEMPTED)
        assertTrue(unseenOnly.isNotEmpty())
        assertTrue(unseenOnly.all { it.attemptsCount == 0 })

        val pneumoSearch = IbqHubLogic.filterQuestions(items, searchQuery = "pneumothorax")
        assertEquals(1, pneumoSearch.size)
        assertEquals("IBQ_019", pneumoSearch.first().id)

        val summary = IbqHubLogic.buildSummary(items)
        assertEquals(25, summary.totalCount)
        assertEquals(unseenOnly.size, summary.unseenCount)
        assertEquals(mistakesOnly.size, summary.mistakeCount)
        assertEquals(masteredOnly.size, summary.correctCount)
        assertEquals(IbqCategory.entries.size, summary.categoryStats.size)
    }

    @Test
    fun prioritizeForDrill_ordersMistakesThenUnseenThenMasteredWithoutDuplicates() {
        val spotters = ClinicalSpotterCatalog.builtinSpotterQuestions().take(6)
        val items = listOf(
            toItem(spotters[0], attempts = 2, correct = 2, lastOutcome = "right"),
            toItem(spotters[1], attempts = 0, correct = 0, lastOutcome = null),
            toItem(spotters[2], attempts = 1, correct = 0, lastOutcome = "wrong"),
            toItem(spotters[3], attempts = 1, correct = 1, lastOutcome = "right"),
            toItem(spotters[4], attempts = 2, correct = 0, lastOutcome = "wrong"),
            toItem(spotters[5], attempts = 0, correct = 0, lastOutcome = null)
        )

        val ordered = IbqHubLogic.prioritizeForDrill(items)
        assertEquals(6, ordered.size)
        assertEquals(6, ordered.map { it.id }.distinct().size)
        assertEquals("wrong", ordered[0].lastOutcome)
        assertEquals("wrong", ordered[1].lastOutcome)
        assertEquals(0, ordered[2].attemptsCount)
        assertEquals(0, ordered[3].attemptsCount)
        assertEquals("right", ordered[4].lastOutcome)
        assertEquals("right", ordered[5].lastOutcome)
    }

    @Test
    fun mixIbqsIntoGeneralPractice_blendsBalancedVisualShareWithoutDuplicates() {
        val textPool = (1..30).map { textItem("TXT_$it") }
        val ibqPool = ClinicalSpotterCatalog.builtinSpotterQuestions().take(10).mapIndexed { idx, q ->
            if (idx == 0) toItem(q, attempts = 1, correct = 0, lastOutcome = "wrong") else toItem(q)
        }

        val mixed = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = textPool,
            ibqPool = ibqPool,
            targetCount = 25,
            ibqSharePercent = 18,
            minIbqCount = 2,
            maxIbqCount = 5
        )

        assertEquals(25, mixed.size)
        assertEquals(25, mixed.map { it.id }.distinct().size)

        val ibqCount = mixed.count { it.question.isImageBased }
        assertTrue("Expected 4–5 IBQs in a 25Q mix, got $ibqCount", ibqCount in 4..5)

        // Ensure IBQs are interleaved rather than all clumped in the first slots
        val firstIbqIndex = mixed.indexOfFirst { it.question.isImageBased }
        val lastIbqIndex = mixed.indexOfLast { it.question.isImageBased }
        assertTrue("IBQs should be spread across session", lastIbqIndex - firstIbqIndex >= 8)

        // Mistake IBQ should be prioritized among chosen IBQs
        assertTrue("Wrong IBQ should be prioritized into mixed session", mixed.any { it.id == ibqPool[0].id })
    }

    @Test
    fun mixIbqsIntoGeneralPractice_handlesEmptyIbqsAndShortPoolsGracefully() {
        val textPool = (1..10).map { textItem("TXT_$it") }
        val noIbqs = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = textPool,
            ibqPool = emptyList(),
            targetCount = 8
        )
        assertEquals(8, noIbqs.size)
        assertFalse(noIbqs.any { it.question.isImageBased })

        // Backfill when text pool is tiny
        val tinyText = listOf(textItem("TXT_1"), textItem("TXT_2"))
        val ibqPool = ClinicalSpotterCatalog.builtinSpotterQuestions().take(6).map { toItem(it) }
        val backfilled = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = tinyText,
            ibqPool = ibqPool,
            targetCount = 6
        )
        assertEquals(6, backfilled.size)
        assertEquals(6, backfilled.map { it.id }.distinct().size)
    }

    @Test
    fun clinicalImageRefSanitizer_blocksUnsafeAndMalformedInputs() {
        assertEquals("", ClinicalImageRef.sanitize("javascript:alert(1)"))
        assertEquals("", ClinicalImageRef.sanitize("file:///etc/passwd"))
        assertEquals("", ClinicalImageRef.sanitize("content://media/external/1"))
        assertEquals("", ClinicalImageRef.sanitize("data:image/png;base64,AAA"))
        assertEquals("", ClinicalImageRef.sanitize("https://example.com/has space.png"))
        assertEquals("", ClinicalImageRef.sanitize("diagram://bad/../../path"))
        assertEquals("diagram://spotter_ctg_late_decel", ClinicalImageRef.sanitize("  diagram://SPOTTER_CTG_LATE_DECEL "))
        assertEquals("https://cdn.clinedge.in/ibq/cxr.webp", ClinicalImageRef.sanitize("https://cdn.clinedge.in/ibq/cxr.webp"))
    }
}
