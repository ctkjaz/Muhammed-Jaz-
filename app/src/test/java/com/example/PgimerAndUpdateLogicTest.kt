package com.example

import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.SubjectCatalog
import com.example.data.update.AppUpdateVersionLogic
import com.example.ui.viewmodel.CbtSessionState
import com.example.ui.viewmodel.PracticeSessionState
import com.example.ui.viewmodel.SubjectHubSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgimerAndUpdateLogicTest {

    @Test
    fun `pgimer exam blueprint matches official 100Q 100m 100-percent nursing and -0_25 marking`() {
        val pgimer = PlatformCatalogDefaults.DEFAULT_EXAMS.firstOrNull {
            it.id == PlatformCatalogDefaults.EXAM_PGIMER_NURSING
        }
        assertNotNull("PGIMER exam must be registered in DEFAULT_EXAMS", pgimer)
        assertEquals(100, pgimer!!.defaultMockQuestions)
        assertEquals(100, pgimer.defaultMockMinutes)
        assertEquals(1.0, pgimer.positiveMarks, 0.001)
        assertEquals(0.25, pgimer.negativeMarks, 0.001)

        assertEquals(100, PlatformCatalogDefaults.nursingWeightagePercent(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))
        assertEquals(0.25, PlatformCatalogDefaults.negativeMarksForExam(PlatformCatalogDefaults.EXAM_PGIMER_NURSING), 0.001)
        assertEquals(0.33, PlatformCatalogDefaults.negativeMarksForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET), 0.001)
        assertEquals(0.33, PlatformCatalogDefaults.negativeMarksForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING), 0.001)

        assertTrue(PlatformCatalogDefaults.isPgimerSharedSubject("MSN"))
        assertTrue(PlatformCatalogDefaults.isPgimerSharedSubject("OBG"))
        assertFalse(PlatformCatalogDefaults.isPgimerSharedSubject("Railways"))
        assertFalse(PlatformCatalogDefaults.isPgimerSharedSubject("Math"))

        assertEquals("Nursing 100%", SubjectHubSection.NURSING.labelFor(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))
        assertEquals("PGIMER PYQs", SubjectHubSection.PYQ.labelFor(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))

        listOf("PGIMER-2025", "PGIMER-2024", "PGIMER-2023", "PGIMER-2022", "PGIMER-2020").forEach { shift ->
            assertTrue("Missing shift display name for $shift", SubjectCatalog.SHIFT_NAMES.containsKey(shift))
        }
    }

    @Test
    fun `session states preserve PGIMER -0_25 negative marking and update version comparison detects v1_8 over v1_6`() {
        val cbt = CbtSessionState(
            title = "PGIMER Official 100Q CBT Mock",
            questions = emptyList(),
            negativeMarks = 0.25
        )
        val practice = PracticeSessionState(
            title = "PGIMER Clinical Drill",
            questions = emptyList(),
            negativeMarks = 0.25
        )
        assertEquals(0.25, cbt.negativeMarks, 0.001)
        assertEquals(0.25, practice.negativeMarks, 0.001)

        // Verify v1.8 release is detected as newer than v1.6 and v1.7
        assertTrue(AppUpdateVersionLogic.isNewerVersion("v1.8", "1.6"))
        assertTrue(AppUpdateVersionLogic.isNewerVersion("v1.8", "1.7"))
        assertFalse(AppUpdateVersionLogic.isNewerVersion("v1.8", "1.8"))
        assertFalse(AppUpdateVersionLogic.isNewerVersion("v1.7", "1.8"))
        assertTrue(AppUpdateVersionLogic.GITHUB_ALL_RELEASES_API_URL.contains("/releases"))
    }
}
