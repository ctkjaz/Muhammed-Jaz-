package com.example

import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamStage
import com.example.data.model.MockSelectionLogic
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectPracticeLogic
import com.example.data.parser.BulkQuestionParser
import com.example.data.parser.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuestionBankSeparationTest {

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

    /**
     * Minimal pure-JVM JSON array parser mirroring BulkQuestionParser.parseJsonBatch
     * so asset integrity tests run without Robolectric or network access.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseAssetQuestions(assetName: String): List<Question> {
        val parsed = MiniJsonParser(readAssetFile(assetName)).parseValue() as List<Map<String, Any?>>
        return parsed.map { obj ->
            val id = obj["id"]?.toString().orEmpty()
            val category = obj["c"]?.toString()?.ifBlank { "nur" } ?: "nur"
            val shiftYear = obj["y"]?.toString()?.trim().orEmpty()
            val subject = obj["s"]?.toString()?.ifBlank { "MSN" } ?: "MSN"
            val diff = QuestionDifficulty.fromRaw(obj["d"]?.toString())
            val qText = obj["q"]?.toString()?.trim().orEmpty()
            val opts = (obj["o"] as? List<*>)?.map { it?.toString().orEmpty() }.orEmpty()
            val ans = (obj["a"] as? Number)?.toInt() ?: 0
            val exp = obj["e"]?.toString().orEmpty()
            val rawNote = obj["sourceNote"]?.toString()?.trim().orEmpty()
            val rawSourceType = obj["sourceType"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val isBuiltInPredicted = category == "mock" ||
                rawSourceType.equals(QuestionSourceType.PREDICTED_MOCK.dbValue, ignoreCase = true)
            val resolvedNote = if (isBuiltInPredicted) rawNote else ""
            val resolvedSourceType = QuestionSourceType.infer(
                category = category,
                shiftYear = shiftYear,
                sourceStatus = Question.SOURCE_UNVERIFIED,
                sourceNote = resolvedNote,
                rawSourceType = if (isBuiltInPredicted) rawSourceType else null
            )
            Question(
                id = id,
                category = category,
                shiftYear = shiftYear,
                subject = subject,
                difficulty = diff,
                questionText = qText,
                options = opts,
                correctIndex = ans,
                explanation = exp,
                isCustom = false,
                sourceStatus = Question.SOURCE_UNVERIFIED,
                sourceNote = resolvedNote,
                sourceType = resolvedSourceType
            )
        }
    }

    @Test
    fun subjectPracticeBank_hasExactly500ValidNonPyqNonMockQuestionsForAll16Subjects() {
        val parsed = parseAssetQuestions("subject_practice_bank.json")
        assertEquals(8000, parsed.size)
        assertEquals(8000, parsed.map { it.id }.toSet().size)

        val countsBySubject = parsed.groupingBy { it.subject }.eachCount()
        assertEquals(16, countsBySubject.size)
        SubjectCatalog.ALL_SUBJS.forEach { meta ->
            assertEquals("Expected 500 questions for ${meta.code}", 500, countsBySubject[meta.code])
        }

        parsed.forEach { q ->
            assertFalse("Subject practice question ${q.id} must not be PYQ-tagged", q.isPyqTagged)
            assertFalse("Subject practice question ${q.id} must not be Predicted Mock", q.isPredictedMock)
            assertEquals("Options count != 4 for ${q.id}", 4, q.options.size)
            assertEquals("Duplicate options in ${q.id}", 4, q.options.map { it.trim().lowercase() }.toSet().size)
            assertTrue("Invalid answer index in ${q.id}", q.correctIndex in 0..3)
            assertTrue("Rationale too short for ${q.id}", q.explanation.trim().length >= 20)
        }
    }

    @Test
    fun predictedMockBank_has500ValidPredictedMocksWithMainsClinicalCasesAndZeroPyqs() {
        val parsed = parseAssetQuestions("predicted_mock_bank.json")
        assertEquals(500, parsed.size)
        assertEquals(500, parsed.map { it.id }.toSet().size)

        var mainsCount = 0
        parsed.forEach { q ->
            assertTrue("${q.id} must be isPredictedMock", q.isPredictedMock)
            assertEquals("${q.id} sourceType must be PREDICTED_MOCK", QuestionSourceType.PREDICTED_MOCK, q.sourceType)
            assertFalse("${q.id} must not be PYQ-tagged", q.isPyqTagged)
            assertTrue("${q.id} must carry trend sourceNote", q.sourceNote.isNotBlank())
            assertEquals("Options count != 4 for ${q.id}", 4, q.options.size)
            assertEquals("Duplicate options in ${q.id}", 4, q.options.map { it.trim().lowercase() }.toSet().size)
            assertTrue("Invalid answer index in ${q.id}", q.correctIndex in 0..3)
            assertTrue("Rationale too short for ${q.id}", q.explanation.trim().length >= 20)
            if (PlatformCatalogDefaults.stageForNorcetQuestion(q) == ExamStage.MAINS) {
                mainsCount++
            }
        }
        assertTrue("Expected >= 160 NORCET Mains predicted clinical vignettes, found $mainsCount", mainsCount >= 160)
    }

    @Test
    fun allFiveAssets_have10203UniqueIdsAndZeroStemOverlapWithNew8500Questions() {
        val baseline = parseAssetQuestions("builtin_questions.json") +
            parseAssetQuestions("norcet_questions.json") +
            parseAssetQuestions("pgimer_questions.json")
        val practice = parseAssetQuestions("subject_practice_bank.json")
        val predicted = parseAssetQuestions("predicted_mock_bank.json")

        val all = baseline + practice + predicted
        assertEquals(10203, all.size)
        assertEquals("All 10,203 IDs across 5 assets must be unique", 10203, all.map { it.id }.toSet().size)

        val baselineStems = baseline.map { SubjectPracticeLogic.normalizeStem(it.questionText) }.toSet()
        val newQuestions = practice + predicted
        val newStems = newQuestions.map { SubjectPracticeLogic.normalizeStem(it.questionText) }
        assertEquals("All 8,500 new questions must have unique stems", 8500, newStems.toSet().size)
        val overlap = newStems.count { it in baselineStems }
        assertEquals("New 8,500 questions must have zero stem overlap with baseline PYQs", 0, overlap)
    }

    @Test
    fun mockSelection_strictlyExcludesPyqsPrioritizesPredictedMocksAndRespectsBlueprints() {
        val baseline = parseAssetQuestions("builtin_questions.json")
        val practice = parseAssetQuestions("subject_practice_bank.json")
        val predicted = parseAssetQuestions("predicted_mock_bank.json")
        val combinedItems = (baseline + practice + predicted).map { it.asItem() }

        // 1. RRB 100Q (70 Nursing / 30 Non-Nursing), 0 PYQs, 100% Predicted Mock
        val rrbMock = MockSelectionLogic.selectStandardMock(
            pool = combinedItems,
            examId = PlatformCatalogDefaults.EXAM_RRB_NURSING,
            totalQuestions = 100,
            shuffle = false
        )
        assertEquals(100, rrbMock.size)
        assertTrue("RRB Mock must contain ZERO PYQs", rrbMock.none { it.question.isPyqTagged })
        assertEquals(100, rrbMock.count { it.question.isPredictedMock })
        assertEquals(70, rrbMock.count { SubjectCatalog.isNursing(it.question) })
        assertEquals(30, rrbMock.count { !SubjectCatalog.isNursing(it.question) })

        // 2. AIIMS NORCET Prelims 100Q (80 Nursing / 20 Non-Nursing), 0 PYQs
        val norcetPrelimsPool = PlatformCatalogDefaults.filterQuestionsForExamStage(
            examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            stage = ExamStage.PRELIMS,
            questions = combinedItems.filter { PlatformCatalogDefaults.isNorcetSharedSubject(it.question.subject) }
        )
        val norcetPrelimsMock = MockSelectionLogic.selectStandardMock(
            pool = norcetPrelimsPool,
            examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            totalQuestions = 100,
            shuffle = false
        )
        assertEquals(100, norcetPrelimsMock.size)
        assertTrue("NORCET Prelims Mock must contain ZERO PYQs", norcetPrelimsMock.none { it.question.isPyqTagged })
        assertEquals(100, norcetPrelimsMock.count { it.question.isPredictedMock })
        assertEquals(80, norcetPrelimsMock.count { SubjectCatalog.isNursing(it.question) })
        assertEquals(20, norcetPrelimsMock.count { !SubjectCatalog.isNursing(it.question) })

        // 3. AIIMS NORCET Mains 160Q (100% Clinical Nursing Mains), 0 PYQs, 100% Predicted Mock Mains
        val norcetMainsMock = MockSelectionLogic.selectNorcetMainsMock(
            allQuestions = combinedItems.filter { PlatformCatalogDefaults.isNorcetSharedSubject(it.question.subject) },
            totalQuestions = 160,
            shuffle = false
        )
        assertEquals(160, norcetMainsMock.size)
        assertTrue("NORCET Mains Mock must contain ZERO PYQs", norcetMainsMock.none { it.question.isPyqTagged })
        assertEquals(160, norcetMainsMock.count { it.question.isPredictedMock })
        assertTrue("NORCET Mains Mock must be 100% Nursing", norcetMainsMock.all { SubjectCatalog.isNursing(it.question) })

        // 4. PGIMER 100Q (100% Core Nursing), 0 PYQs
        val pgimerPool = combinedItems.filter { PlatformCatalogDefaults.isPgimerSharedSubject(it.question.subject) }
        val pgimerMock = MockSelectionLogic.selectStandardMock(
            pool = pgimerPool,
            examId = PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
            totalQuestions = 100,
            shuffle = false
        )
        assertEquals(100, pgimerMock.size)
        assertTrue("PGIMER Mock must contain ZERO PYQs", pgimerMock.none { it.question.isPyqTagged })
        assertEquals(100, pgimerMock.count { it.question.isPredictedMock })
        assertTrue("PGIMER Mock must be 100% Core Nursing", pgimerMock.all { SubjectCatalog.isNursing(it.question) })
    }

    @Test
    fun subjectPracticeFilter_strictlyExcludesPyqsAndPredictedMocks() {
        val pyq = Question(
            id = "PYQ_1", category = "pyq", shiftYear = "2025-S1", subject = "MSN",
            questionText = "PYQ question?", options = listOf("A", "B", "C", "D"), correctIndex = 0, explanation = "E"
        ).asItem()
        val mock = Question(
            id = "PM_1", category = "mock", shiftYear = "", subject = "MSN",
            sourceType = QuestionSourceType.PREDICTED_MOCK,
            questionText = "Predicted mock question?", options = listOf("A", "B", "C", "D"), correctIndex = 1, explanation = "E"
        ).asItem()
        val practice = Question(
            id = "SP_1", category = "nur", shiftYear = "", subject = "MSN",
            sourceType = QuestionSourceType.PRACTICE,
            questionText = "Sample practice question?", options = listOf("A", "B", "C", "D"), correctIndex = 2, explanation = "E"
        ).asItem()

        val collected = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = listOf(pyq, mock, practice),
            allLinks = emptyList(),
            subjectCode = "MSN",
            examIdFilter = null,
            difficultyFilter = DifficultyFilter.ALL,
            excludePyqAndMock = true
        )
        assertEquals(listOf("SP_1"), collected.map { it.id })
    }

    @Test
    fun improvedPdfParser_handlesStandaloneOptionLinesUnnumberedQDotWrappedOptionsAndAnswerKey() {
        val messyPdfText = """
            Glucose is added to oral rehydration / anti-diarrhoeal solution because:
            A) It tastes sweet
            B) It increases absorption of amino
            acids
            C) It increases absorption of
            sodium
            D) All of above
            Ans: C

            Q. Case finding in RNTCP is based on:
            a)

            Sputum smear microscopy

            b)
            Chest X-ray alone

            c)

            Recent and past memory loss

            d)
            Global memory loss
            Answer: (a)
            Rationale: RNTCP/NTEP prioritizes sputum microscopy and CBNAAT for active case finding.

            3. Which electrolyte imbalance causes tall peaked T waves on ECG?
            (A) Hypokalemia  (B) Hyperkalemia  (C) Hyponatremia  (D) Hypercalcemia

            Answer Key
            3. B
        """.trimIndent()

        val parsed = BulkQuestionParser.parseRawPdfText(
            rawText = messyPdfText,
            category = "pyq",
            subject = "CHN",
            shiftYear = "2026-S1",
            defaultDifficulty = QuestionDifficulty.MEDIUM
        )

        assertEquals("Should parse all 3 real-world PDF MCQs", 3, parsed.size)

        // Q1: Implicit unnumbered stem + multi-line wrapped options B & C
        assertTrue(parsed[0].questionText.contains("anti-diarrhoeal solution"))
        assertEquals("It increases absorption of amino acids", parsed[0].options[1])
        assertEquals("It increases absorption of sodium", parsed[0].options[2])
        assertEquals(2, parsed[0].correctIndex)

        // Q2: Unnumbered "Q." + standalone a)/b)/c)/d) option markers with blank lines
        assertEquals("Case finding in RNTCP is based on:", parsed[1].questionText)
        assertEquals("Sputum smear microscopy", parsed[1].options[0])
        assertEquals("Recent and past memory loss", parsed[1].options[2])
        assertEquals("Global memory loss", parsed[1].options[3])
        assertEquals(0, parsed[1].correctIndex)
        assertTrue(parsed[1].explanation.contains("CBNAAT"))

        // Q3: Inline options split + trailing Answer Key lookup
        assertTrue(parsed[2].questionText.contains("tall peaked T waves"))
        assertEquals(listOf("Hypokalemia", "Hyperkalemia", "Hyponatremia", "Hypercalcemia"), parsed[2].options)
        assertEquals(1, parsed[2].correctIndex)
    }

    @Test
    fun improvedPdfExtractor_preservesWordsWithETAndKeepsHorizontalTdOptionLabelsOnSameLine() {
        val stream = """
            BT
            /F1 12 Tf
            72 720 Td
            (Q.1 CATHETER care in DIABETES mellitus and FETAL monitoring:) Tj
            0 -18 Td
            (c\)) Tj
            18 0 Td
            (Recent and past memory loss) Tj
            0 -18 Td
            (Ans: C) Tj
            ET
        """.trimIndent()
        val fakePdf = "%PDF-1.4\n1 0 obj\n<< /Length ${stream.length} >>\nstream\n$stream\nendstream\nendobj\n%%EOF"
        val res = PdfTextExtractor.extractFromBytes(fakePdf.toByteArray(Charsets.ISO_8859_1))

        assertEquals("pdf", res.fileType)
        assertFalse(res.isLikelyScannedPdf)
        assertTrue("Words containing ET must not truncate text", res.text.contains("DIABETES mellitus and FETAL monitoring"))
        assertTrue("Horizontal 18 0 Td must keep option label and body on same line", res.text.contains("c) Recent and past memory loss"))
    }

    private class MiniJsonParser(private val s: String) {
        private var i = 0

        fun parseValue(): Any? {
            skipWs()
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { i += 4; true }
                'f' -> { i += 5; false }
                'n' -> { i += 4; null }
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            i++ // '{'
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (s[i] == '}') { i++; return map }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                i++ // ':'
                map[key] = parseValue()
                skipWs()
                if (s[i] == ',') { i++ } else { i++; break }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            i++ // '['
            val list = ArrayList<Any?>()
            skipWs()
            if (s[i] == ']') { i++; return list }
            while (true) {
                list.add(parseValue())
                skipWs()
                if (s[i] == ',') { i++ } else { i++; break }
            }
            return list
        }

        private fun parseString(): String {
            i++ // '"'
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    when (val esc = s[i++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val code = s.substring(i, i + 4).toInt(16)
                            sb.append(code.toChar())
                            i += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNumber(): Number {
            val start = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++
            val raw = s.substring(start, i)
            return raw.toIntOrNull() ?: raw.toDouble()
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}
