package com.example

import com.example.data.model.ClinicalImageRef
import com.example.data.model.ClinicalSpotterCatalog
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionSourceType
import com.example.data.parser.BulkQuestionParser
import com.example.data.subscription.CloudPackQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V27FeaturesLogicTest {

    @Test
    fun builtinSpotterCatalog_contains15ValidMedicallyCompleteImageQuestions() {
        val spotters = ClinicalSpotterCatalog.builtinSpotterQuestions()
        assertTrue("Expected at least 25 built-in clinical spotters", spotters.size >= 25)
        assertEquals(spotters.size, spotters.map { it.id }.distinct().size)

        spotters.forEach { q ->
            assertTrue("Spotter ${q.id} should be marked image-based", q.isImageBased)
            assertEquals("Spotter ${q.id} must have 4 options", 4, q.options.size)
            assertTrue("Spotter ${q.id} correctIndex out of bounds", q.correctIndex in 0..3)
            assertTrue("Spotter ${q.id} explanation missing", q.explanation.length >= 40)
            assertTrue("Spotter ${q.id} should belong to nursing core", q.category == "nur")
            assertEquals(QuestionSourceType.PRACTICE, q.sourceType)

            val parsed = ClinicalImageRef.parse(q.imageUrl)
            assertTrue(
                "Spotter ${q.id} must parse to Diagram or Drawable, got $parsed",
                parsed is ClinicalImageRef.Diagram || parsed is ClinicalImageRef.Drawable
            )
            assertTrue(
                "Badge label should not be blank for ${q.id}",
                ClinicalSpotterCatalog.badgeLabelFor(parsed).isNotBlank()
            )
            // Verify virtual projection works across all other nursing exams (e.g. ESIC, JIPMER, DSSSB)
            assertTrue(
                "Spotter ${q.id} should virtually project to ESIC Nursing",
                PlatformCatalogDefaults.shouldVirtuallyProjectQuestion(
                    PlatformCatalogDefaults.EXAM_ESIC_NURSING,
                    q
                )
            )
        }
    }

    @Test
    fun clinicalImageRef_sanitizesAllowedSchemesAndRejectsUnsafeSchemes() {
        assertEquals(
            ClinicalImageRef.Diagram("ecg_vtach"),
            ClinicalImageRef.parse("diagram://ecg_vtach")
        )
        assertEquals(
            ClinicalImageRef.Drawable("laryngoscope"),
            ClinicalImageRef.parse("  drawable://LARYNGOSCOPE  ")
        )
        assertEquals(
            ClinicalImageRef.RemoteUrl("https://cdn.clinedge.in/spotters/ecg1.png"),
            ClinicalImageRef.parse("https://cdn.clinedge.in/spotters/ecg1.png")
        )

        // Unsafe or malformed schemes must be rejected cleanly
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("javascript:alert(1)"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("file:///etc/passwd"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("content://media/external/1"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("data:image/png;base64,AAA"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("diagram://../bad_path"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse("https://bad url with spaces.com/a.png"))
        assertEquals(ClinicalImageRef.None, ClinicalImageRef.parse(""))
    }

    @Test
    fun bulkQuestionParser_parsesImageLineInPlainTextAndRejectsUnsafeSchemes() {
        val rawText = """
            Q.1 Identify the sterile instrument shown above.
            Image: drawable://surgical_tray
            A) Cheatle forceps
            B) Sims speculum
            C) Ambu bag
            D) Laryngoscope
            Ans: A
            Exp: Cheatle forceps are used to transfer sterile instruments with tips pointing downward.

            Q.2 Which cannula gauge is ISO green?
            Image: javascript:alert(1)
            A) 18G
            B) 20G
            C) 22G
            D) 24G
            Ans: A
            Exp: 18G peripheral IV cannula has an ISO green hub.
        """.trimIndent()

        val textQuestions = BulkQuestionParser.parseRawPdfText(
            rawText = rawText,
            category = "nur",
            subject = "FON",
            shiftYear = ""
        )
        assertEquals(2, textQuestions.size)
        assertEquals("drawable://surgical_tray", textQuestions[0].imageUrl)
        assertTrue(textQuestions[0].isImageBased)
        assertEquals("", textQuestions[1].imageUrl)
        assertFalse(textQuestions[1].isImageBased)
    }

    @Test
    fun cloudPackQuestion_preservesSanitizedImageUrlWhenMappedToDomain() {
        val cloud = CloudPackQuestion(
            id = "Q_1",
            packId = "norcet_ibq_pack",
            subject = "MSN",
            difficulty = QuestionDifficulty.HARD.label,
            questionText = "Interpret this lead V2-V4 ECG finding.",
            options = listOf("Anterior STEMI", "Normal sinus rhythm", "Hypocalcemia", "Sinus pause"),
            correctIndex = 0L,
            explanation = "Convex ST-elevation in V2-V4 indicates anterior wall MI.",
            imageUrl = "https://storage.googleapis.com/clinedge/stemi_v2.png"
        )
        val domain: Question = cloud.toDomainQuestion()!!
        assertTrue(domain.isImageBased)
        assertEquals("https://storage.googleapis.com/clinedge/stemi_v2.png", domain.imageUrl)
    }
}
