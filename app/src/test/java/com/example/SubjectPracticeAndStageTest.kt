package com.example

import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectPracticeLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectPracticeAndStageTest {

    private fun makeItem(
        id: String,
        category: String = "nur",
        shiftYear: String = "",
        subject: String = "MSN",
        difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        text: String = "Question stem for $id",
        attempts: Int = 0,
        correct: Int = 0,
        bookmarked: Boolean = false
    ): QuestionItem {
        val q = Question(
            id = id,
            category = category,
            shiftYear = shiftYear,
            subject = subject,
            difficulty = difficulty,
            questionText = text,
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctIndex = 0,
            explanation = "Clinical rationale."
        )
        return QuestionItem(
            question = q,
            progress = QuestionProgress(
                questionId = id,
                isBookmarked = bookmarked,
                attemptsCount = attempts,
                correctCount = correct,
                wrongCount = (attempts - correct).coerceAtLeast(0),
                lastOutcome = if (correct > 0) "right" else if (attempts > 0) "wrong" else null
            )
        )
    }

    @Test
    fun onlyAiimsNorcetSupportsPrelimsAndMainsStages() {
        assertTrue(PlatformCatalogDefaults.supportsStages(PlatformCatalogDefaults.EXAM_AIIMS_NORCET))
        assertFalse(PlatformCatalogDefaults.supportsStages(PlatformCatalogDefaults.EXAM_RRB_NURSING))
        assertFalse(PlatformCatalogDefaults.supportsStages(PlatformCatalogDefaults.EXAM_PGIMER_NURSING))
        assertFalse(PlatformCatalogDefaults.supportsStages(null))
    }

    @Test
    fun norcetPrelimsAndMainsPoolsAreStrictlyDisjointWithZeroCrossLeakage() {
        val pool = listOf(
            makeItem("N_10_1", category = "pyq", shiftYear = "NORCET-10", subject = "MSN", difficulty = QuestionDifficulty.HARD, text = "NORCET-10 Prelims recall nursing question"),
            makeItem("N_9_2", category = "pyq", shiftYear = "NORCET-9", subject = "Static-GK", difficulty = QuestionDifficulty.MEDIUM, text = "NORCET-9 Prelims GK question"),
            makeItem("N_CA_1", category = "ca", subject = "Health-Prog", difficulty = QuestionDifficulty.HARD, text = "National Health Programme question"),
            makeItem("N_GEN_1", category = "gen", subject = "Reasoning", difficulty = QuestionDifficulty.HARD, text = "Logical reasoning aptitude question"),
            makeItem("N_MAINS_1", category = "nur", subject = "MSN", difficulty = QuestionDifficulty.MEDIUM, text = "[Mains Clinical Case] 58-year-old ICU patient with sepsis"),
            makeItem("N_MAINS_2", category = "nur", subject = "Pharm", difficulty = QuestionDifficulty.HARD, text = "[Mains Calculation] Infuse 1000 mL over 8 hours"),
            makeItem("B_NUR_HARD", category = "nur", subject = "OBG", difficulty = QuestionDifficulty.HARD, text = "Priority nursing management in eclampsia"),
            makeItem("B_NUR_MED", category = "nur", subject = "FON", difficulty = QuestionDifficulty.MEDIUM, text = "Normal adult oral temperature range")
        )

        val prelims = PlatformCatalogDefaults.filterQuestionsForExamStage(
            PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            ExamStage.PRELIMS,
            pool
        )
        val mains = PlatformCatalogDefaults.filterQuestionsForExamStage(
            PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            ExamStage.MAINS,
            pool
        )

        // Strictly disjoint and complete partition
        val prelimsIds = prelims.map { it.id }.toSet()
        val mainsIds = mains.map { it.id }.toSet()
        assertTrue("Prelims and Mains must never overlap", prelimsIds.intersect(mainsIds).isEmpty())
        assertEquals(pool.size, prelims.size + mains.size)

        // Stage II Mains must be 100% Core Clinical Nursing
        assertTrue(
            "Mains must contain zero non-nursing questions",
            mains.all { SubjectCatalog.isNursing(it.question) }
        )
        assertTrue(mainsIds.containsAll(setOf("N_MAINS_1", "N_MAINS_2", "B_NUR_HARD")))

        // Stage I Prelims must never include [Mains ...] items, and must keep NORCET PYQs + non-nursing
        assertTrue(
            "Prelims must never contain [Mains] clinical cases",
            prelims.none { it.question.questionText.contains("[Mains", ignoreCase = true) }
        )
        assertTrue(prelimsIds.containsAll(setOf("N_10_1", "N_9_2", "N_CA_1", "N_GEN_1", "B_NUR_MED")))
    }

    @Test
    fun singleStageExamsDoNotFilterByStage() {
        val pool = listOf(
            makeItem("P_1", category = "pyq", shiftYear = "PGIMER-2025", subject = "MSN", difficulty = QuestionDifficulty.HARD),
            makeItem("P_2", category = "nur", subject = "Pharm", difficulty = QuestionDifficulty.EASY)
        )
        assertEquals(
            pool.size,
            PlatformCatalogDefaults.filterQuestionsForExamStage(
                PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
                ExamStage.MAINS,
                pool
            ).size
        )
        assertEquals(
            pool.size,
            PlatformCatalogDefaults.filterQuestionsForExamStage(
                PlatformCatalogDefaults.EXAM_RRB_NURSING,
                ExamStage.PRELIMS,
                pool
            ).size
        )
    }

    @Test
    fun crossExamSubjectPracticeAggregatesAcrossAllExamsAndDeduplicates() {
        val rrbMsn = makeItem("RRB_MSN_1", subject = "MSN", difficulty = QuestionDifficulty.MEDIUM, text = "Normal serum potassium level?")
        val norcetMsn = makeItem("N_MSN_1", subject = "MSN", difficulty = QuestionDifficulty.HARD, text = "First-line drug for anaphylactic shock?")
        val pgimerMsn = makeItem("PG_MSN_1", subject = "MSN", difficulty = QuestionDifficulty.EASY, text = "Parkland formula fluid calculation?")
        // Duplicate stem across exams with higher progress — should merge to 1 keeping higher progress
        val dupStem = makeItem(
            id = "PG_MSN_DUP",
            subject = "MSN",
            difficulty = QuestionDifficulty.MEDIUM,
            text = "  normal   serum POTASSIUM level? ",
            attempts = 3,
            correct = 2,
            bookmarked = true
        )
        val pharmItem = makeItem("RRB_PHARM_1", subject = "Pharm", difficulty = QuestionDifficulty.EASY, text = "Antidote for heparin?")

        val allQuestions = listOf(rrbMsn, norcetMsn, pgimerMsn, dupStem, pharmItem)
        val links = listOf(
            ExamQuestionLink(PlatformCatalogDefaults.EXAM_RRB_NURSING, rrbMsn.id, "sec_rrb"),
            ExamQuestionLink(PlatformCatalogDefaults.EXAM_AIIMS_NORCET, norcetMsn.id, "sec_norcet"),
            ExamQuestionLink(PlatformCatalogDefaults.EXAM_PGIMER_NURSING, pgimerMsn.id, "sec_pgimer"),
            ExamQuestionLink(PlatformCatalogDefaults.EXAM_PGIMER_NURSING, dupStem.id, "sec_pgimer"),
            ExamQuestionLink(PlatformCatalogDefaults.EXAM_RRB_NURSING, pharmItem.id, "sec_rrb")
        )

        // All Exams combined for MSN -> 3 unique questions (4 raw minus 1 normalized duplicate)
        val allMsn = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = allQuestions,
            allLinks = links,
            subjectCode = "MSN",
            examIdFilter = null,
            difficultyFilter = DifficultyFilter.ALL
        )
        assertEquals(3, allMsn.size)
        val mergedPotassium = allMsn.first { it.question.questionText.contains("potassium", ignoreCase = true) }
        assertTrue("Deduplication must preserve higher progress & bookmark", mergedPotassium.isBookmarked)
        assertEquals(3, mergedPotassium.attemptsCount)

        // Optional exam filter scopes strictly to that exam
        val norcetOnlyMsn = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = allQuestions,
            allLinks = links,
            subjectCode = "MSN",
            examIdFilter = PlatformCatalogDefaults.EXAM_AIIMS_NORCET
        )
        assertEquals(listOf("N_MSN_1"), norcetOnlyMsn.map { it.id })

        // Difficulty filter works cleanly on cross-exam pool
        val hardMsn = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = allQuestions,
            allLinks = links,
            subjectCode = "MSN",
            examIdFilter = null,
            difficultyFilter = DifficultyFilter.HARD
        )
        assertEquals(listOf("N_MSN_1"), hardMsn.map { it.id })

        // Cross-exam subject stats reflect accurate deduplicated totals
        val stats = SubjectPracticeLogic.computeCrossExamSubjectStats(allQuestions, links, examIdFilter = null)
        val msnStat = stats.first { it.code == "MSN" }
        val pharmStat = stats.first { it.code == "Pharm" }
        assertEquals(3, msnStat.totalQuestions)
        assertEquals(1, msnStat.attemptedQuestions)
        assertEquals(1, pharmStat.totalQuestions)
    }
}
