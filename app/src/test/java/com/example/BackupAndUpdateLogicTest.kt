package com.example

import com.example.data.backup.BackupStorageLogic
import com.example.data.export.PdfWorksheetWriter
import com.example.data.update.AppUpdateVersionLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAndUpdateLogicTest {

    @Test
    fun backupStorageLogicFormatsLabelsAndFilenamesCleanly() {
        val name = BackupStorageLogic.buildBackupFileName(1700000000000L)
        assertTrue(name.startsWith("clinedge-backup-"))
        assertTrue(name.endsWith(".json"))
        assertTrue(BackupStorageLogic.isBackupFileName(name))
        assertTrue(BackupStorageLogic.isBackupFileName("backup.clinedge"))
        assertFalse(BackupStorageLogic.isBackupFileName("notes.pdf"))

        val defaultLabel = BackupStorageLogic.formatTreeUriDisplayLabel("")
        assertEquals(BackupStorageLogic.DEFAULT_FOLDER_LABEL, defaultLabel)

        val encodedSafUri =
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FClinEdgeBackups"
        assertEquals(
            "Documents/ClinEdgeBackups",
            BackupStorageLogic.formatTreeUriDisplayLabel(encodedSafUri)
        )
    }

    @Test
    fun pdfWatermarkHelpersPersonalizeForStudentAndPlan() {
        val (primary, secondary) = PdfWorksheetWriter.buildWatermarkLines(
            studentName = "Priya Menon",
            planName = "ClinEdge Free",
            email = "priya@example.com"
        )
        assertEquals("CLINEDGE", primary)
        assertEquals("Priya Menon  •  ClinEdge Free", secondary)
        assertEquals(
            "ClinEdge • Prepared for Priya Menon (ClinEdge Free) • Practice Only",
            PdfWorksheetWriter.buildFooterBranding("Priya Menon", "ClinEdge Free")
        )

        val (_, fallbackEmail) = PdfWorksheetWriter.buildWatermarkLines(
            studentName = "  ",
            planName = "ClinEdge Free",
            email = "student@example.com"
        )
        assertEquals("student@example.com  •  ClinEdge Free", fallbackEmail)

        val (_, fallbackGeneric) = PdfWorksheetWriter.buildWatermarkLines("", "", "")
        assertEquals("ClinEdge Free  •  Practice Material", fallbackGeneric)
    }

    @Test
    fun appUpdateVersionComparisonHandlesSemverAndPrefixes() {
        assertTrue(AppUpdateVersionLogic.isNewerVersion("1.7", "1.6"))
        assertTrue(AppUpdateVersionLogic.isNewerVersion("v1.6.1", "1.6"))
        assertTrue(AppUpdateVersionLogic.isNewerVersion("2.0.0-beta", "1.9.5"))
        assertFalse(AppUpdateVersionLogic.isNewerVersion("v1.6", "1.6"))
        assertFalse(AppUpdateVersionLogic.isNewerVersion("1.5.9", "1.6"))
        assertFalse(AppUpdateVersionLogic.isNewerVersion("", "1.6"))
        assertEquals(listOf(1, 6, 2), AppUpdateVersionLogic.normalizeVersionParts("v1.6.2-rc1"))
    }
}
