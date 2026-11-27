package com.example

import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.UnfoldingClinicalCasesCatalog
import com.example.data.parser.BulkQuestionParser
import com.example.data.repository.NursingPrepRepository
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class PhaseAClinicalCasesAndNorcetPyqTest {

    private fun resolveAssetFile(fileName: String): File {
        val candidates = listOf(
            File("src/main/assets/$fileName"),
            File("app/src/main/assets/$fileName")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Asset file not found: $fileName (cwd=${File(".").absolutePath})")
    }

    private fun normalizeStem(stem: String): String =
        stem.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    @Test
    fun unfoldingClinicalCasesCatalog_has10CompleteClinicallyValidCases() {
        val cases = UnfoldingClinicalCasesCatalog.ALL_CASES
        assertEquals("Expected 10 NORCET Mains unfolding clinical cases", 10, cases.size)

        val uniqueCaseIds = cases.map { it.id }.toSet()
        assertEquals("All clinical case IDs must be unique", cases.size, uniqueCaseIds.size)

        val specialties = cases.map { it.specialty }.toSet()
        assertTrue("Expected ICU/MSN specialty coverage", specialties.any { it.contains("ICU", ignoreCase = true) || it.contains("MSN", ignoreCase = true) })
        assertTrue("Expected OBG specialty coverage", specialties.any { it.contains("OBG", ignoreCase = true) })
        assertTrue("Expected Peds/NICU specialty coverage", specialties.any { it.contains("Ped", ignoreCase = true) || it.contains("NICU", ignoreCase = true) })
        assertTrue("Expected Emergency specialty coverage", specialties.any { it.contains("Emergency", ignoreCase = true) })

        cases.forEach { c ->
            assertTrue("Case ${c.id} must have title", c.title.isNotBlank())
            assertTrue("Case ${c.id} must have patient profile", c.patientProfile.isNotBlank())
            assertTrue("Case ${c.id} must have at least 3 linked stages", c.stages.size >= 3)
            assertTrue("Case ${c.id} must have at least 3 debrief pearls", c.debriefPearls.size >= 3)

            c.stages.forEachIndexed { idx, stage ->
                assertEquals("Stage index ${idx + 1} mismatch in ${c.id}", idx + 1, stage.stageNumber)
                assertTrue("Stage clinicalUpdate blank in ${c.id}#${stage.stageNumber}", stage.clinicalUpdate.isNotBlank())
                assertTrue("Stage prompt blank in ${c.id}#${stage.stageNumber}", stage.prompt.isNotBlank())
                assertEquals("Stage must have 4 options in ${c.id}#${stage.stageNumber}", 4, stage.options.size)
                assertEquals(
                    "Stage options must be distinct in ${c.id}#${stage.stageNumber}",
                    4,
                    stage.options.map { it.trim().lowercase(Locale.ROOT) }.toSet().size
                )
                assertTrue(
                    "Stage correctIndex out of range in ${c.id}#${stage.stageNumber}",
                    stage.correctIndex in 0..3
                )
                assertTrue("Stage rationale blank in ${c.id}#${stage.stageNumber}", stage.rationale.isNotBlank())
                assertTrue("Stage criticalPearl blank in ${c.id}#${stage.stageNumber}", stage.criticalPearl.isNotBlank())
                assertTrue("Stage vitals HR blank in ${c.id}#${stage.stageNumber}", stage.vitals.hr.isNotBlank())
                assertTrue("Stage vitals BP blank in ${c.id}#${stage.stageNumber}", stage.vitals.bp.isNotBlank())
                assertTrue("Stage vitals SpO2 blank in ${c.id}#${stage.stageNumber}", stage.vitals.spo2.isNotBlank())
            }
        }
    }

    @Test
    fun unfoldingClinicalCases_scoringAntiFarmingXpAndSerializationWork() {
        assertEquals(100, UnfoldingClinicalCasesCatalog.computeCaseScorePercent(4, 4))
        assertEquals(75, UnfoldingClinicalCasesCatalog.computeCaseScorePercent(3, 4))
        assertEquals(0, UnfoldingClinicalCasesCatalog.computeCaseScorePercent(0, 0))

        // First completion awards XP: 40 base + 10 per correct stage
        val firstRunXp = UnfoldingClinicalCasesCatalog.xpForCaseCompletion(
            correctStages = 3,
            totalStages = 4,
            alreadyCompleted = false
        )
        assertEquals(70, firstRunXp)

        // Replay awards 0 XP (anti-farming protection)
        val replayXp = UnfoldingClinicalCasesCatalog.xpForCaseCompletion(
            correctStages = 4,
            totalStages = 4,
            alreadyCompleted = true
        )
        assertEquals(0, replayXp)

        // Serialization round-trip
        val ids = setOf("case_stemi_shock", "case_dka_transition", "case_preeclampsia_mgso4")
        val serialized = UnfoldingClinicalCasesCatalog.serializeCompletedCaseIds(ids)
        val restored = UnfoldingClinicalCasesCatalog.deserializeCompletedCaseIds(serialized)
        assertEquals(ids, restored)
        assertTrue(UnfoldingClinicalCasesCatalog.deserializeCompletedCaseIds(null).isEmpty())

        // GamificationLogic integration
        val initial = GamificationState(totalXp = 100, weeklyXp = 20)
        val result = GamificationLogic.recordClinicalCaseCompleted(
            current = initial,
            xpAwarded = firstRunXp,
            correctStages = 3,
            totalStages = 4,
            lifetimeAnsweredCount = 15,
            longestStreakDays = 3,
            weekKey = "2026-W48"
        )
        assertEquals(170, result.updatedState.totalXp)
        assertEquals(4, result.updatedState.weeklyQuestions)
        assertEquals(3, result.updatedState.weeklyCorrect)
        assertEquals(1, result.updatedState.completedSessionsCount)
    }

    @Test
    fun norcetPyqExpansionAsset_has300UniqueVerifiedQuestionsWithZeroDuplicatesAcrossBank() {
        val expansionFile = resolveAssetFile("norcet_pyq_expansion.json")
        val expansionJson = expansionFile.readText()
        val arr = JSONArray(expansionJson)
        assertEquals("norcet_pyq_expansion.json must contain exactly 300 MCQs", 300, arr.length())

        val expansionIds = mutableSetOf<String>()
        val expansionStems = mutableSetOf<String>()
        val yearCounts = mutableMapOf<String, Int>()
        val subjectsSeen = mutableSetOf<String>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")
            val category = obj.getString("c")
            val year = obj.getString("y")
            val subject = obj.getString("s")
            val sourceType = obj.getString("sourceType")
            val q = obj.getString("q")
            val opts = obj.getJSONArray("o")
            val ans = obj.getInt("a")
            val exp = obj.getString("e")

            assertTrue("ID $id must start with N_EXP_", id.startsWith("N_EXP_"))
            assertTrue("Duplicate ID inside expansion pack: $id", expansionIds.add(id))
            assertEquals("Category must be pyq for $id", "pyq", category)
            assertEquals("sourceType must be memory_based for $id", "memory_based", sourceType)
            assertTrue("Question stem too short for $id", q.trim().length >= 25)
            assertEquals("Must have exactly 4 options for $id", 4, opts.length())

            val distinctOpts = (0 until opts.length()).map { opts.getString(it).trim() }.filter { it.isNotEmpty() }.toSet()
            assertEquals("All 4 options must be distinct and non-empty for $id", 4, distinctOpts.size)
            assertTrue("Answer index out of range for $id", ans in 0..3)
            assertTrue("Rationale blank for $id", exp.trim().length >= 20)

            val norm = normalizeStem(q)
            assertTrue("Duplicate normalized stem inside expansion pack at $id", expansionStems.add(norm))

            yearCounts[year] = (yearCounts[year] ?: 0) + 1
            subjectsSeen.add(subject)

            // Verify section mapping resolves cleanly to NORCET Nursing or GK/Aptitude
            val sectionId = PlatformCatalogDefaults.sectionIdForNorcetSubject(subject)
            assertTrue(
                "Unexpected NORCET sectionId $sectionId for $id ($subject)",
                sectionId == PlatformCatalogDefaults.SECTION_NORCET_NURSING ||
                    sectionId == PlatformCatalogDefaults.SECTION_NORCET_GK_APTITUDE
            )
        }

        // Verify exact balanced coverage across NORCET-5 through NORCET-10 (50 each)
        listOf("NORCET-10", "NORCET-9", "NORCET-8", "NORCET-7", "NORCET-6", "NORCET-5").forEach { shift ->
            assertEquals("Expected 50 PYQs for $shift", 50, yearCounts[shift])
        }

        // Verify both Nursing & Prelims GK/Aptitude sections are represented
        assertTrue("Expected MSN coverage", "MSN" in subjectsSeen)
        assertTrue("Expected OBG coverage", "OBG" in subjectsSeen)
        assertTrue("Expected Peds coverage", "Peds" in subjectsSeen)
        assertTrue("Expected Pharm coverage", "Pharm" in subjectsSeen)
        assertTrue("Expected Prelims Math/Reasoning/GK coverage", "Math" in subjectsSeen && "Reasoning" in subjectsSeen && "Static-GK" in subjectsSeen)

        // Cross-check ZERO ID collisions and ZERO normalized stem collisions against all existing asset banks
        val existingAssetFiles = listOf(
            "builtin_questions.json",
            "norcet_questions.json",
            "pgimer_questions.json",
            "subject_practice_bank.json",
            "predicted_mock_bank.json",
            "all_india_pyqs.json"
        )
        existingAssetFiles.forEach { assetName ->
            val otherArr = JSONArray(resolveAssetFile(assetName).readText())
            for (i in 0 until otherArr.length()) {
                val obj = otherArr.getJSONObject(i)
                val otherId = obj.optString("id", "")
                val otherStem = normalizeStem(obj.optString("q", ""))
                assertFalse("ID collision with $assetName: $otherId", otherId in expansionIds)
                assertFalse("Stem duplicate with $assetName ($otherId)", otherStem in expansionStems)
            }
        }
    }

    @Test
    fun norcetPyqExpansion_parsesThroughBulkParserAsPyqTaggedAndRepositoryUsesSeedVersion13() {
        assertEquals(13, NursingPrepRepository.CURRENT_SEED_VERSION)

        val expansionJson = resolveAssetFile("norcet_pyq_expansion.json").readText()
        val parsed = BulkQuestionParser.parseJsonBatch(expansionJson)
        assertEquals(300, parsed.size)
        assertTrue("Every expansion item must be PYQ-tagged", parsed.all { it.isPyqTagged })
        assertTrue("Every expansion item must have non-blank examYear", parsed.all { it.examYear.isNotBlank() })
    }
}
