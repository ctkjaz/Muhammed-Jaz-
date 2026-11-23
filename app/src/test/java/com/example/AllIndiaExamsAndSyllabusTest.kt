package com.example

import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamCategory
import com.example.data.model.ExamQuestionLink
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectPracticeLogic
import com.example.data.model.SyllabusCatalog
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PlanTenure
import com.example.data.subscription.PlanTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AllIndiaExamsAndSyllabusTest {

    private fun readAssetFile(name: String): String {
        val candidates = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Asset file not found: $name (cwd=${File(".").absolutePath})")
        return file.readText(Charsets.UTF_8)
    }

    private fun Question.asItem(): QuestionItem =
        QuestionItem(question = this, progress = QuestionProgress(questionId = id))

    @Suppress("UNCHECKED_CAST")
    private fun parseAssetObjects(assetName: String): List<Map<String, Any?>> {
        return TestJsonParser(readAssetFile(assetName)).parseValue() as List<Map<String, Any?>>
    }

    private fun mapToQuestion(obj: Map<String, Any?>): Question {
        val id = obj["id"]?.toString().orEmpty()
        val category = obj["c"]?.toString()?.ifBlank { "nur" } ?: "nur"
        val shiftYear = obj["y"]?.toString()?.trim().orEmpty()
        val subject = obj["s"]?.toString()?.ifBlank { "MSN" } ?: "MSN"
        val topicId = obj["topicId"]?.toString()?.trim()?.ifBlank {
            obj["t"]?.toString()?.trim().orEmpty()
        }?.ifBlank {
            PlatformCatalogDefaults.defaultTopicIdFor(subject)
        } ?: PlatformCatalogDefaults.defaultTopicIdFor(subject)
        val diff = QuestionDifficulty.fromRaw(obj["d"]?.toString())
        val qText = obj["q"]?.toString()?.trim().orEmpty()
        val opts = (obj["o"] as? List<*>)?.map { it?.toString().orEmpty() }.orEmpty()
        val ans = (obj["a"] as? Number)?.toInt() ?: 0
        val exp = obj["e"]?.toString().orEmpty()
        val rawNote = obj["sourceNote"]?.toString()?.trim().orEmpty()
        val rawSourceType = obj["sourceType"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val resolvedSourceType = QuestionSourceType.infer(
            category = category,
            shiftYear = shiftYear,
            sourceStatus = Question.SOURCE_UNVERIFIED,
            sourceNote = rawNote,
            rawSourceType = rawSourceType
        )
        return Question(
            id = id,
            category = category,
            shiftYear = shiftYear,
            subject = subject,
            topicId = topicId,
            difficulty = diff,
            questionText = qText,
            options = opts,
            correctIndex = ans,
            explanation = exp,
            isCustom = false,
            sourceStatus = Question.SOURCE_UNVERIFIED,
            sourceNote = rawNote,
            sourceType = resolvedSourceType
        )
    }

    @Test
    fun catalog_hasAll29IndianNursingExamsAcrossAll4Categories() {
        val exams = PlatformCatalogDefaults.DEFAULT_EXAMS
        assertEquals(29, exams.size)
        assertEquals(29, exams.map { it.id }.toSet().size)

        val byCategory = exams.groupBy { PlatformCatalogDefaults.examCategoryFor(it.id) }
        assertEquals(5, byCategory[ExamCategory.CENTRAL]?.size)
        assertEquals(12, byCategory[ExamCategory.AUTONOMOUS]?.size)
        assertEquals(9, byCategory[ExamCategory.STATE_CHO]?.size)
        assertEquals(3, byCategory[ExamCategory.DEFENCE_PSU]?.size)

        // Spot-check key user-requested exams
        val requiredIds = listOf(
            PlatformCatalogDefaults.EXAM_RRB_NURSING,
            PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
            PlatformCatalogDefaults.EXAM_ESIC_NURSING,
            PlatformCatalogDefaults.EXAM_DSSSB_NURSING,
            PlatformCatalogDefaults.EXAM_NEIGRIHMS_NURSING,
            PlatformCatalogDefaults.EXAM_GIMS_NOIDA_NURSING,
            PlatformCatalogDefaults.EXAM_JIPMER_NURSING,
            PlatformCatalogDefaults.EXAM_NIMHANS_NURSING,
            PlatformCatalogDefaults.EXAM_SGPGI_LUCKNOW_NURSING,
            PlatformCatalogDefaults.EXAM_KGMU_LUCKNOW_NURSING,
            PlatformCatalogDefaults.EXAM_NHM_CHO_NURSING,
            PlatformCatalogDefaults.EXAM_KERALA_PSC_NURSING,
            PlatformCatalogDefaults.EXAM_MNS_SSC_NURSING
        )
        requiredIds.forEach { examId ->
            val exam = PlatformCatalogDefaults.examById(examId)
            assertNotNull("Missing exam $examId", exam)
            assertTrue("Exam $examId must have positive defaultMockQuestions", exam!!.defaultMockQuestions > 0)
            assertTrue("Exam $examId must have positive defaultMockMinutes", exam.defaultMockMinutes > 0)
            assertTrue(
                "Exam $examId must have at least one section blueprint",
                PlatformCatalogDefaults.DEFAULT_SECTIONS.any { it.examId == examId }
            )
            assertTrue(
                "shortExamLabel should resolve cleanly for $examId",
                EntitlementEvaluator.shortExamLabel(examId).isNotBlank()
            )
        }

        // Access code generation for newly added single exams
        val neigrihmsCode = EntitlementEvaluator.generateAccessCode(
            tier = PlanTier.SINGLE_EXAM,
            examId = PlatformCatalogDefaults.EXAM_NEIGRIHMS_NURSING,
            tenure = PlanTenure.THREE_MONTHS
        )
        assertTrue("Expected CE- prefix in $neigrihmsCode", neigrihmsCode.startsWith("CE-"))
        assertEquals("All 29 Exams", EntitlementEvaluator.shortExamLabel("all"))
    }

    @Test
    fun allIndiaPyqsAsset_has520ValidPaperWisePyqsAcross26ExamsWithZeroOverlap() {
        val rawObjects = parseAssetObjects("all_india_pyqs.json")
        assertEquals(520, rawObjects.size)

        val examIds = mutableSetOf<String>()
        val shiftCodes = mutableSetOf<String>()
        val allIndiaIds = mutableSetOf<String>()
        val allIndiaStems = mutableSetOf<String>()

        rawObjects.forEach { obj ->
            val examId = obj["examId"]?.toString()?.trim().orEmpty()
            assertTrue("Missing or invalid examId: $examId", PlatformCatalogDefaults.examById(examId) != null)
            examIds.add(examId)

            val q = mapToQuestion(obj)
            assertTrue("ID blank", q.id.isNotBlank())
            assertTrue("Duplicate ID ${q.id}", allIndiaIds.add(q.id))
            val normStem = q.questionText.trim().lowercase()
            assertTrue("Duplicate stem inside all_india_pyqs.json: ${q.id}", allIndiaStems.add(normStem))
            assertTrue("${q.id} must be PYQ-tagged", q.isPyqTagged)
            assertEquals("${q.id} must be memory-based", QuestionSourceType.MEMORY_BASED, q.sourceType)
            assertTrue("${q.id} must have a shift/paper code", q.shiftYear.isNotBlank())
            shiftCodes.add(q.shiftYear)

            val formattedTitle = SubjectCatalog.formatPaperTitle(q.shiftYear)
            assertTrue("Paper title should be formatted for ${q.shiftYear}", formattedTitle.isNotBlank())
            assertFalse("Paper title should not stay raw code for ${q.shiftYear}", formattedTitle == q.shiftYear)

            assertEquals("Options count != 4 for ${q.id}", 4, q.options.size)
            assertEquals("Duplicate options in ${q.id}", 4, q.options.map { it.trim().lowercase() }.toSet().size)
            assertTrue("Invalid answer index for ${q.id}", q.correctIndex in 0..3)
            assertTrue("Rationale too short for ${q.id}", q.explanation.trim().length >= 30)
        }

        assertEquals(26, examIds.size)
        assertEquals(52, shiftCodes.size)

        // Verify zero ID or stem overlap against all 5 existing assets
        val baselineAssets = listOf(
            "builtin_questions.json",
            "norcet_questions.json",
            "pgimer_questions.json",
            "subject_practice_bank.json",
            "predicted_mock_bank.json"
        )
        baselineAssets.forEach { asset ->
            val baseObjs = parseAssetObjects(asset)
            baseObjs.forEach { obj ->
                val baseId = obj["id"]?.toString().orEmpty()
                val baseStem = obj["q"]?.toString()?.trim()?.lowercase().orEmpty()
                assertFalse("ID collision with $asset: $baseId", baseId in allIndiaIds)
                assertFalse("Stem collision with $asset: $baseId", baseStem in allIndiaStems)
            }
        }
    }

    @Test
    fun syllabusCatalog_classifiesAll16SubjectsIntoChaptersAndTopicsWithHighYieldFiltering() {
        SubjectCatalog.ALL_SUBJS.forEach { subj ->
            val chapters = SyllabusCatalog.chaptersForSubject(subj.code)
            assertTrue("Subject ${subj.code} must have at least 2 syllabus chapters", chapters.size >= 2)
            assertEquals(chapters.size, SyllabusCatalog.chapterCountForSubject(subj.code))
        }

        val practiceQuestions = parseAssetObjects("subject_practice_bank.json").map { mapToQuestion(it).asItem() }
        val bySubject = practiceQuestions.groupBy { it.question.subject }

        SubjectCatalog.ALL_SUBJS.forEach { subj ->
            val items = bySubject[subj.code].orEmpty()
            assertEquals("Expected 500 practice items for ${subj.code}", 500, items.size)
            val buckets = SyllabusCatalog.buildChaptersForSubject(
                subjectCode = subj.code,
                items = items,
                highYieldOnly = false
            )
            assertTrue("Expected non-empty chapter buckets for ${subj.code}", buckets.isNotEmpty())
            assertEquals(
                "All 500 questions in ${subj.code} must map cleanly into chapters without drops",
                500,
                buckets.sumOf { it.items.size }
            )
            buckets.forEach { bucket ->
                assertEquals(
                    "Topics inside ${bucket.meta.title} must sum to chapter total",
                    bucket.items.size,
                    bucket.topics.sumOf { it.items.size }
                )
            }

            val hyBuckets = SyllabusCatalog.buildChaptersForSubject(
                subjectCode = subj.code,
                items = items,
                highYieldOnly = true
            )
            val hyTotal = hyBuckets.sumOf { it.items.size }
            assertTrue("High-Yield filter should produce non-empty subset for ${subj.code}", hyTotal > 0)
            assertTrue("High-Yield subset should be <= full subject pool for ${subj.code}", hyTotal <= 500)
        }

        // Verify Predicted Mocks and PYQs are always tagged High-Yield
        val predictedItem = parseAssetObjects("predicted_mock_bank.json").first().let { mapToQuestion(it) }
        val pyqItem = parseAssetObjects("all_india_pyqs.json").first().let { mapToQuestion(it) }
        assertTrue("Predicted mocks must be marked High-Yield", SyllabusCatalog.isHighYield(predictedItem))
        assertTrue("PYQs must be marked High-Yield", SyllabusCatalog.isHighYield(pyqItem))
    }

    @Test
    fun virtualSharedProjection_populatesNewExamsRespectingPureNursingRules() {
        val msnPractice = Question(
            id = "PRAC_MSN_1",
            category = "nur",
            shiftYear = "",
            subject = "MSN",
            topicId = "msn_cardiology",
            difficulty = QuestionDifficulty.MEDIUM,
            questionText = "First-line drug for anaphylactic shock?",
            options = listOf("Epinephrine IM", "Atropine IV", "Dopamine", "Digoxin"),
            correctIndex = 0,
            explanation = "IM epinephrine is first-line.",
            sourceType = QuestionSourceType.PRACTICE
        ).asItem()

        val mathPractice = Question(
            id = "PRAC_MATH_1",
            category = "gen",
            shiftYear = "",
            subject = "Math",
            topicId = "math_percentage",
            difficulty = QuestionDifficulty.EASY,
            questionText = "What is 25% of 200?",
            options = listOf("25", "50", "75", "100"),
            correctIndex = 1,
            explanation = "25% of 200 equals 50.",
            sourceType = QuestionSourceType.PRACTICE
        ).asItem()

        val railwaysPractice = Question(
            id = "PRAC_RAIL_1",
            category = "ca",
            shiftYear = "",
            subject = "Railways",
            topicId = "railways_gk",
            difficulty = QuestionDifficulty.EASY,
            questionText = "Headquarters of Northern Railway?",
            options = listOf("New Delhi", "Mumbai", "Kolkata", "Chennai"),
            correctIndex = 0,
            explanation = "Northern Railway HQ is New Delhi.",
            sourceType = QuestionSourceType.CURRENT_AFFAIRS
        ).asItem()

        val pool = listOf(msnPractice, mathPractice, railwaysPractice)
        val links = emptyList<ExamQuestionLink>()

        // Pure nursing exam (NEIGRIHMS): receives MSN, excludes Math & Railways
        val neigrihmsMsn = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = pool,
            allLinks = links,
            subjectCode = "MSN",
            examIdFilter = PlatformCatalogDefaults.EXAM_NEIGRIHMS_NURSING,
            difficultyFilter = DifficultyFilter.ALL,
            includeVirtualShared = true
        )
        val neigrihmsMath = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = pool,
            allLinks = links,
            subjectCode = "Math",
            examIdFilter = PlatformCatalogDefaults.EXAM_NEIGRIHMS_NURSING,
            difficultyFilter = DifficultyFilter.ALL,
            includeVirtualShared = true
        )
        assertEquals(1, neigrihmsMsn.size)
        assertEquals(0, neigrihmsMath.size)

        // Mixed exam (GIMS Noida): receives MSN & Math, excludes Railways
        val gimsMath = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = pool,
            allLinks = links,
            subjectCode = "Math",
            examIdFilter = PlatformCatalogDefaults.EXAM_GIMS_NOIDA_NURSING,
            difficultyFilter = DifficultyFilter.ALL,
            includeVirtualShared = true
        )
        val gimsRailways = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = pool,
            allLinks = links,
            subjectCode = "Railways",
            examIdFilter = PlatformCatalogDefaults.EXAM_GIMS_NOIDA_NURSING,
            difficultyFilter = DifficultyFilter.ALL,
            includeVirtualShared = true
        )
        assertEquals(1, gimsMath.size)
        assertEquals(0, gimsRailways.size)
    }

    @Test
    fun virtualProjection_preservesExactBaselineCountsAndIsolatesDedicatedExamBanks() {
        val rrbRaw = parseAssetObjects("builtin_questions.json").map { mapToQuestion(it) }
        val norcetRaw = parseAssetObjects("norcet_questions.json").map { mapToQuestion(it) }
        val pgimerRaw = parseAssetObjects("pgimer_questions.json").map { mapToQuestion(it) }
        val practiceRaw = parseAssetObjects("subject_practice_bank.json").map { mapToQuestion(it) }
        val predictedRaw = parseAssetObjects("predicted_mock_bank.json").map { mapToQuestion(it) }
        val allIndiaPairs = parseAssetObjects("all_india_pyqs.json").map { obj ->
            mapToQuestion(obj) to obj["examId"]?.toString()?.trim().orEmpty()
        }

        val allQuestions = rrbRaw + norcetRaw + pgimerRaw + practiceRaw + predictedRaw + allIndiaPairs.map { it.first }

        fun projectNonPyq(q: Question): Question = q.copy(
            category = when {
                SubjectCatalog.isNursingCode(q.subject) -> "nur"
                SubjectCatalog.isCaCode(q.subject) -> "ca"
                else -> "gen"
            },
            shiftYear = "",
            examYear = "",
            examShift = "",
            sourceType = if (SubjectCatalog.isCaCode(q.subject)) {
                QuestionSourceType.CURRENT_AFFAIRS
            } else {
                QuestionSourceType.PRACTICE
            }
        )

        fun simulateObserveQuestionsForExam(examId: String): List<Question> {
            val explicit: List<Question> = when (examId) {
                PlatformCatalogDefaults.EXAM_RRB_NURSING -> rrbRaw
                PlatformCatalogDefaults.EXAM_AIIMS_NORCET -> {
                    norcetRaw + rrbRaw
                        .filter { PlatformCatalogDefaults.isNorcetSharedSubject(it.subject) }
                        .map { projectNonPyq(it) }
                }
                PlatformCatalogDefaults.EXAM_PGIMER_NURSING -> {
                    pgimerRaw + (norcetRaw + rrbRaw)
                        .filter { PlatformCatalogDefaults.isPgimerSharedSubject(it.subject) }
                        .distinctBy { it.id }
                        .map { projectNonPyq(it) }
                }
                else -> allIndiaPairs.filter { it.second == examId }.map { it.first }
            }
            val result = ArrayList<Question>(explicit.size + 4096)
            val seenIds = HashSet<String>(explicit.size * 2 + 16)
            for (q in explicit) {
                if (seenIds.add(q.id)) result.add(q)
            }
            for (q in allQuestions) {
                if (PlatformCatalogDefaults.shouldVirtuallyProjectQuestion(examId, q) &&
                    seenIds.add(q.id)
                ) {
                    result.add(q)
                }
            }
            return result
        }

        // 1. RRB Nursing: exact 1,223 baseline + 8,000 practice + 500 predicted mocks = 9,723 (no N_* / P_* leakage)
        val rrbProjected = simulateObserveQuestionsForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING)
        assertEquals(1223 + 8000 + 500, rrbProjected.size)
        assertEquals(500, rrbProjected.count { it.isPredictedMock })
        assertTrue(rrbProjected.none { it.id.startsWith("N_") || it.id.startsWith("P_") || it.id.startsWith("AIP_") })
        val rrbPyqShifts = rrbProjected.filter { it.isPyqTagged }.map { it.shiftYear }.toSet()
        assertTrue("2025-S1" in rrbPyqShifts)
        assertTrue(rrbPyqShifts.none { it.startsWith("NORCET") })

        // 2. AIIMS NORCET: exact 320 dedicated N_* + shared non-Railways > 9,000, zero Railways, zero P_* leakage
        val norcetProjected = simulateObserveQuestionsForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET)
        assertEquals(320, norcetProjected.count { it.id.startsWith("N_") })
        assertTrue(norcetProjected.size > 9000)
        assertTrue(norcetProjected.none { it.subject == "Railways" })
        assertTrue(norcetProjected.none { it.id.startsWith("P_") || it.id.startsWith("AIP_") })
        val norcetPyqShifts = norcetProjected.filter { it.isPyqTagged }.map { it.shiftYear }.toSet()
        assertEquals(
            setOf("NORCET-10", "NORCET-9", "NORCET-8", "NORCET-7", "NORCET-6", "NORCET-5"),
            norcetPyqShifts
        )

        // 3. PGIMER: 100% pure nursing, zero non-nursing subjects, zero AIP_* leakage
        val pgimerProjected = simulateObserveQuestionsForExam(PlatformCatalogDefaults.EXAM_PGIMER_NURSING)
        assertTrue(pgimerProjected.all { SubjectCatalog.isNursingCode(it.subject) })
        assertTrue(pgimerProjected.size > 6000)
        assertTrue(pgimerProjected.none { it.id.startsWith("AIP_") })

        // 4. Newly added pure-nursing exam (NEIGRIHMS) & mixed exam (ESIC) get rich banks + isolated PYQs
        val neigrihmsProjected = simulateObserveQuestionsForExam(PlatformCatalogDefaults.EXAM_NEIGRIHMS_NURSING)
        assertEquals(20, neigrihmsProjected.count { it.isPyqTagged })
        assertTrue(neigrihmsProjected.all { SubjectCatalog.isNursingCode(it.subject) })
        assertTrue(neigrihmsProjected.size > 5000)
        assertTrue(neigrihmsProjected.none { it.id.startsWith("N_") && it.questionText.contains("[Mains", ignoreCase = true) })

        val esicProjected = simulateObserveQuestionsForExam(PlatformCatalogDefaults.EXAM_ESIC_NURSING)
        assertEquals(20, esicProjected.count { it.isPyqTagged })
        assertTrue(esicProjected.none { it.subject == "Railways" })
        assertTrue(esicProjected.size > 7500)
    }
}

private class TestJsonParser(private val src: String) {
    private var idx = 0

    fun parseValue(): Any? {
        skipWs()
        if (idx >= src.length) return null
        return when (val ch = src[idx]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> { idx += 4; true }
            'f' -> { idx += 5; false }
            'n' -> { idx += 4; null }
            else -> if (ch == '-' || ch.isDigit()) parseNumber() else error("Unexpected '$ch' at $idx")
        }
    }

    private fun skipWs() {
        while (idx < src.length && src[idx].isWhitespace()) idx++
    }

    private fun parseObject(): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        idx++
        skipWs()
        if (idx < src.length && src[idx] == '}') { idx++; return map }
        while (idx < src.length) {
            skipWs()
            val key = parseString()
            skipWs()
            if (src[idx] == ':') idx++
            map[key] = parseValue()
            skipWs()
            when (src[idx]) {
                ',' -> idx++
                '}' -> { idx++; return map }
                else -> error("Expected ',' or '}' at $idx")
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        val list = ArrayList<Any?>()
        idx++
        skipWs()
        if (idx < src.length && src[idx] == ']') { idx++; return list }
        while (idx < src.length) {
            list.add(parseValue())
            skipWs()
            when (src[idx]) {
                ',' -> idx++
                ']' -> { idx++; return list }
                else -> error("Expected ',' or ']' at $idx")
            }
        }
        return list
    }

    private fun parseString(): String {
        idx++
        val sb = StringBuilder()
        while (idx < src.length) {
            val ch = src[idx++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    val esc = src[idx++]
                    when (esc) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = src.substring(idx, idx + 4)
                            sb.append(hex.toInt(16).toChar())
                            idx += 4
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun parseNumber(): Number {
        val start = idx
        if (src[idx] == '-') idx++
        while (idx < src.length && (src[idx].isDigit() || src[idx] == '.' || src[idx] == 'e' || src[idx] == 'E' || src[idx] == '+' || src[idx] == '-')) {
            idx++
        }
        val token = src.substring(start, idx)
        return if (token.contains('.') || token.contains('e', true)) token.toDouble() else token.toLong()
    }
}
