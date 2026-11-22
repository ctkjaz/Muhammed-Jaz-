package com.example

import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.SubjectCatalog
import com.example.ui.viewmodel.CbtSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NorcetSectionalCbtTest {

    private fun fakeItems(count: Int): List<QuestionItem> = List(count) { i ->
        QuestionItem(
            question = Question(
                id = "N_TEST_$i",
                category = "nur",
                shiftYear = "",
                subject = "MSN",
                difficulty = QuestionDifficulty.HARD,
                questionText = "Clinical vignette #$i",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0,
                explanation = "Rationale #$i"
            ),
            progress = QuestionProgress("N_TEST_$i")
        )
    }

    @Test
    fun `norcet stage 1 prelims enforces 5 locked sections of 20 questions each`() {
        val prelims = CbtSessionState(
            title = "NORCET Stage I Prelims (100Q)",
            questions = fakeItems(100),
            sectionSize = 20,
            sectionDurationSeconds = 18 * 60,
            sectionLabels = listOf("Sec 1", "Sec 2", "Sec 3", "Sec 4", "Sec 5"),
            currentSectionIndex = 0,
            sectionRemainingSeconds = 18 * 60,
            totalSeconds = 90 * 60,
            remainingSeconds = 90 * 60
        )

        assertTrue(prelims.hasSectionLock)
        assertEquals(5, prelims.totalSections)
        assertEquals(0..19, prelims.activeSectionRange)
        assertTrue(prelims.isIndexInActiveSection(0))
        assertTrue(prelims.isIndexInActiveSection(19))
        assertFalse(prelims.isIndexInActiveSection(20))
        assertFalse(prelims.isLastSection)
        assertEquals("Sec 1", prelims.currentSectionLabel)

        val finalSection = prelims.copy(currentSectionIndex = 4, currentIndex = 80)
        assertEquals(80..99, finalSection.activeSectionRange)
        assertFalse(finalSection.isIndexInActiveSection(79))
        assertTrue(finalSection.isIndexInActiveSection(80))
        assertTrue(finalSection.isIndexInActiveSection(99))
        assertTrue(finalSection.isLastSection)
    }

    @Test
    fun `norcet stage 2 mains enforces 4 locked sections of 40 questions each`() {
        val mains = CbtSessionState(
            title = "NORCET Stage II Mains (160Q Clinical CBT)",
            questions = fakeItems(160),
            sectionSize = 40,
            sectionDurationSeconds = 45 * 60,
            currentSectionIndex = 1,
            sectionRemainingSeconds = 45 * 60,
            totalSeconds = 180 * 60,
            remainingSeconds = 135 * 60
        )

        assertTrue(mains.hasSectionLock)
        assertEquals(4, mains.totalSections)
        assertEquals(40..79, mains.activeSectionRange)
        assertFalse(mains.isIndexInActiveSection(39))
        assertTrue(mains.isIndexInActiveSection(40))
        assertTrue(mains.isIndexInActiveSection(79))
        assertFalse(mains.isIndexInActiveSection(80))
        assertFalse(mains.isLastSection)
    }

    @Test
    fun `standard rrb mock without section lock allows free navigation across all questions`() {
        val standard = CbtSessionState(
            title = "Full Official 100Q CBT Mock",
            questions = fakeItems(100),
            totalSeconds = 90 * 60,
            remainingSeconds = 90 * 60
        )

        assertFalse(standard.hasSectionLock)
        assertEquals(1, standard.totalSections)
        assertEquals(0..99, standard.activeSectionRange)
        assertTrue(standard.isIndexInActiveSection(0))
        assertTrue(standard.isIndexInActiveSection(99))
        assertTrue(standard.isLastSection)
    }

    @Test
    fun `norcet catalog metadata includes norcet 10 shift and 80 percent nursing weightage`() {
        assertEquals(80, PlatformCatalogDefaults.nursingWeightagePercent(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertEquals(70, PlatformCatalogDefaults.nursingWeightagePercent(PlatformCatalogDefaults.EXAM_RRB_NURSING))
        assertEquals("AIIMS NORCET-10 (Prelims & Mains)", SubjectCatalog.SHIFT_NAMES["NORCET-10"])
    }
}
