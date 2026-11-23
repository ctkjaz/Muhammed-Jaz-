package com.example.data.parser

import com.example.data.local.QuestionProgressEntity
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionSourceType
import com.example.data.model.composeShiftYear
import com.example.data.model.parseShiftYear
import org.json.JSONArray
import org.json.JSONObject

object BulkQuestionParser {
    // Explicit question headers:
    //  - Numbered with Q prefix: Q.1, Q1., Q 1), Ques. 1, Question 1:
    //  - Unnumbered with Q prefix: Q., Q:, Ques., Question:  (fixes "Q. Case finding in RNTCP...")
    //  - Bare numbered: 1., 1), (1), [1]
    private val qExplicitPrefixRegex = Regex(
        """^(?:Q(?:ues(?:tion)?)?\.?\s*(\d+)?\s*[.):\-]?)\s+(.{4,})""",
        RegexOption.IGNORE_CASE
    )
    private val qExplicitHeaderOnlyRegex = Regex(
        """^(?:Q(?:ues(?:tion)?)?\.?\s*(\d+)?\s*[.):\-]?)$""",
        RegexOption.IGNORE_CASE
    )
    private val qBareNumberRegex = Regex(
        """^(?:\(?(\d{1,4})[.)]|\[(\d{1,4})])\s+(.{4,})"""
    )

    // Option markers:
    //  - Lettered: A), a), (A), (a), [A], [a], A., a., Option A: — with text OR standalone on its own line!
    //  - Numbered: 1), 1., (1), [1], Ans 1. — with text OR standalone on its own line!
    private val optLetterWithBodyRegex = Regex(
        """^(?:Option\s+)?(?:\(?([A-Da-d])[.)]|\[([A-Da-d])]|([A-Da-d])\s*[-:])\s+(.+)""",
        RegexOption.IGNORE_CASE
    )
    private val optLetterStandaloneRegex = Regex(
        """^(?:Option\s+)?(?:\(?([A-Da-d])[.)]|\[([A-Da-d])]|([A-Da-d])\s*[-:])$""",
        RegexOption.IGNORE_CASE
    )
    private val optNumberWithBodyRegex = Regex(
        """^(?:\(?([1-4])[.)]|\[([1-4])]|Ans\s*([1-4])\.)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )
    private val optNumberStandaloneRegex = Regex(
        """^(?:\(?([1-4])[.)]|\[([1-4])])$"""
    )

    // Flexible Answer line:
    //  Ans: C, Ans. (c), Ans - C, Answer: Option C, Answer (3), Correct Answer: [B],
    //  Right Answer: D, Key: A, ✓ C, Ans: C) It increases...
    private val ansRegex = Regex(
        """^(?:(?:Correct|Right)\s*(?:Option|Answer)|Ans(?:wer)?|Key|✓|✔|✅)\s*[:.\-–]?\s*(?:Option\s*)?[\[(]?\s*([1-4A-Da-d])\s*[\]).:\-–]?(?:\s+(.*))?$""",
        RegexOption.IGNORE_CASE
    )
    private val expRegex = Regex(
        """^(?:Exp(?:lanation)?|Rationale|Sol(?:ution)?|Hint|Key\s*Point|Note)\s*[:.\-–]\s*(.+)""",
        RegexOption.IGNORE_CASE
    )
    private val diffRegex = Regex("""^(?:Diff(?:iculty)?|Level)\s*[:.\-]\s*(Easy|Medium|Hard)""", RegexOption.IGNORE_CASE)
    private val sourceNoteRegex = Regex("""^(?:Source(?:\s*Note)?|Ref(?:erence)?)\s*[:.\-]\s*(.+)""", RegexOption.IGNORE_CASE)
    private val sourceStatusRegex = Regex("""^(?:Source\s*Status|Verification)\s*[:.\-]\s*(verified|unverified)""", RegexOption.IGNORE_CASE)
    private val topicRegex = Regex("""^(?:Topic)\s*[:.\-]\s*(.+)""", RegexOption.IGNORE_CASE)

    // Trailing Answer Key header & entries (e.g. "Answer Key" followed by "1. C  2. A  3-D")
    private val answerKeyHeaderRegex = Regex("""^(?:Answer\s*Key|Answers|Official\s*Key|Key)\s*[:.\-]?$""", RegexOption.IGNORE_CASE)
    private val answerKeyPairRegex = Regex("""(?:Q\.?\s*)?(\d{1,4})\s*[.):\-–]\s*[\[(]?([1-4A-Da-d])[\])]?(?=\s|$|,|;)""", RegexOption.IGNORE_CASE)

    // Inline option splitter when 2+ options share a line (e.g. "(a) One  (b) Two  (c) Three  (d) Four")
    private val inlineOptionSplitRegex = Regex("""\s+(?=(?:\([A-Da-d]\)|\[[A-Da-d]]|[A-Da-d]\))\s+)""")

    // Inline answer marker inside an option (e.g. "✓", "(Correct)", "[Ans]")
    private val inlineCorrectMarkerRegex = Regex("""(?:\s*(?:✓|✔|✅|\(correct\)|\[correct]|\(ans\)|\[ans]))+$""", RegexOption.IGNORE_CASE)

    // Standalone PDF page footer noise
    private val pageNoiseRegex = Regex("""^(?:Page\s+\d+(?:\s*(?:of|/)\s*\d+)?|-\s*\d+\s*-|\d+\s*/\s*\d+)$""", RegexOption.IGNORE_CASE)

    private enum class ParseSection { NONE, STEM, OPTION, EXPLANATION }

    private data class DraftQuestion(
        val explicitNumber: Int?,
        val questionText: String,
        val options: List<String>,
        val correctIndex: Int,
        val hasExplicitAnswer: Boolean,
        val explanation: String,
        val difficulty: QuestionDifficulty,
        val sourceNote: String,
        val sourceStatus: String?,
        val topicName: String
    )

    fun resolveSourceStatus(
        category: String,
        rawSourceStatus: String?,
        sourceNote: String
    ): String {
        val normalizedStatus = rawSourceStatus?.trim()?.lowercase()
        val trimmedNote = sourceNote.trim()
        if (normalizedStatus != Question.SOURCE_VERIFIED) {
            return Question.SOURCE_UNVERIFIED
        }
        return if (trimmedNote.isNotEmpty()) {
            Question.SOURCE_VERIFIED
        } else {
            Question.SOURCE_UNVERIFIED
        }
    }

    fun slugTopicId(subjectCode: String, rawTopicName: String): String {
        val trimmed = rawTopicName.trim()
        if (trimmed.isEmpty()) return PlatformCatalogDefaults.defaultTopicIdFor(subjectCode)
        val subjSlug = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val topicSlug = trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        if (topicSlug.isEmpty() || topicSlug == "general") {
            return PlatformCatalogDefaults.defaultTopicIdFor(subjectCode)
        }
        return "${subjSlug}_${topicSlug}"
    }

    private fun tokenToOptionIndex(rawToken: String): Int {
        val tok = rawToken.trim().uppercase()
        return when (tok) {
            "1", "2", "3", "4" -> tok.toInt() - 1
            else -> (tok.firstOrNull()?.minus('A') ?: 0).coerceIn(0, 3)
        }
    }

    private fun preprocessLines(rawText: String): List<String> {
        val normalized = rawText
            .replace("\uFEFF", "")
            .replace("\u00AD", "")
            .replace("\u00A0", " ")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val rawLines = normalized.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val expanded = ArrayList<String>(rawLines.size)
        for (line in rawLines) {
            if (pageNoiseRegex.matches(line)) continue
            // Split inline multi-option rows (e.g. "A) Foo   B) Bar   C) Baz   D) Qux")
            val startsWithLetterOpt = optLetterWithBodyRegex.containsMatchIn(line)
            val parts = if (startsWithLetterOpt) inlineOptionSplitRegex.split(line) else listOf(line)
            if (parts.size >= 2) {
                parts.map { it.trim() }.filter { it.isNotEmpty() }.forEach { expanded.add(it) }
            } else {
                expanded.add(line)
            }
        }
        return expanded
    }

    private fun extractTrailingAnswerKey(lines: List<String>): Pair<List<String>, Map<Int, Int>> {
        val keyHeaderIdx = lines.indexOfLast { answerKeyHeaderRegex.matches(it) }
        if (keyHeaderIdx < 0 || keyHeaderIdx >= lines.lastIndex) {
            return lines to emptyMap()
        }
        val keyMap = LinkedHashMap<Int, Int>()
        for (idx in (keyHeaderIdx + 1) until lines.size) {
            for (m in answerKeyPairRegex.findAll(lines[idx])) {
                val qNum = m.groupValues[1].toIntOrNull() ?: continue
                val optIdx = tokenToOptionIndex(m.groupValues[2])
                keyMap[qNum] = optIdx
            }
        }
        return if (keyMap.isNotEmpty()) {
            lines.subList(0, keyHeaderIdx) to keyMap
        } else {
            lines to emptyMap()
        }
    }

    private fun matchLetterOption(line: String): Pair<Int, String>? {
        val withBody = optLetterWithBodyRegex.find(line)
        if (withBody != null) {
            val tok = withBody.groupValues[1].ifEmpty {
                withBody.groupValues[2].ifEmpty { withBody.groupValues[3] }
            }
            return tokenToOptionIndex(tok) to withBody.groupValues[4].trim()
        }
        val standalone = optLetterStandaloneRegex.find(line)
        if (standalone != null) {
            val tok = standalone.groupValues[1].ifEmpty {
                standalone.groupValues[2].ifEmpty { standalone.groupValues[3] }
            }
            return tokenToOptionIndex(tok) to ""
        }
        return null
    }

    private fun matchNumberOption(line: String): Pair<Int, String>? {
        val withBody = optNumberWithBodyRegex.find(line)
        if (withBody != null) {
            val tok = withBody.groupValues[1].ifEmpty {
                withBody.groupValues[2].ifEmpty { withBody.groupValues[3] }
            }
            return tokenToOptionIndex(tok) to withBody.groupValues[4].trim()
        }
        val standalone = optNumberStandaloneRegex.find(line)
        if (standalone != null) {
            val tok = standalone.groupValues[1].ifEmpty { standalone.groupValues[2] }
            return tokenToOptionIndex(tok) to ""
        }
        return null
    }

    private fun looksLikeOptionStartWithin(lines: List<String>, fromIdx: Int, window: Int = 4): Boolean {
        val end = minOf(lines.size, fromIdx + window)
        for (k in fromIdx until end) {
            val l = lines[k]
            // Stop lookahead if an answer, explanation, or explicit new question appears first
            if (ansRegex.containsMatchIn(l) ||
                expRegex.containsMatchIn(l) ||
                qExplicitPrefixRegex.containsMatchIn(l) ||
                qExplicitHeaderOnlyRegex.matches(l) ||
                qBareNumberRegex.containsMatchIn(l)
            ) {
                return false
            }
            val letter = matchLetterOption(l)
            if (letter != null && letter.first == 0) return true
            val num = matchNumberOption(l)
            if (num != null && num.first == 0) return true
        }
        return false
    }

    fun parseRawPdfText(
        rawText: String,
        category: String,
        subject: String,
        shiftYear: String,
        defaultDifficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
        defaultSourceNote: String = "",
        markAsVerified: Boolean = false,
        defaultSourceType: QuestionSourceType? = null,
        defaultTopicId: String = ""
    ): List<Question> {
        val allLines = preprocessLines(rawText)
        if (allLines.isEmpty()) return emptyList()
        val (lines, trailingAnswerKey) = extractTrailingAnswerKey(allLines)

        val drafts = mutableListOf<DraftQuestion>()
        var hasCurrent = false
        var section = ParseSection.NONE
        var curExplicitNum: Int? = null
        var lastQuestionNum: Int? = null
        var curQuestionText = ""
        var curOptions = mutableListOf<String>()
        var curUsesLetterOptions = false
        var curAnswerIdx = 0
        var hasExplicitAnswer = false
        var curExplanation = ""
        var curDifficulty = defaultDifficulty
        var curSourceNote = ""
        var curSourceStatus: String? = null
        var curTopicName = ""
        val pendingStemLines = mutableListOf<String>()

        fun resetCurrent(num: Int?, stem: String) {
            hasCurrent = true
            section = ParseSection.STEM
            curExplicitNum = num
            if (num != null) lastQuestionNum = num
            curQuestionText = stem.trim()
            curOptions = mutableListOf()
            curUsesLetterOptions = false
            curAnswerIdx = 0
            hasExplicitAnswer = false
            curExplanation = ""
            curDifficulty = defaultDifficulty
            curSourceNote = ""
            curSourceStatus = null
            curTopicName = ""
            pendingStemLines.clear()
        }

        fun pushCurrent() {
            val nonBlankOptions = curOptions.map { it.trim() }.filter { it.isNotEmpty() }
            if (hasCurrent && curQuestionText.isNotBlank() && nonBlankOptions.size >= 2) {
                val padded = nonBlankOptions.toMutableList()
                while (padded.size < 4) padded.add("None of these")
                drafts.add(
                    DraftQuestion(
                        explicitNumber = curExplicitNum,
                        questionText = curQuestionText.trim(),
                        options = padded.take(4),
                        correctIndex = curAnswerIdx.coerceIn(0, 3),
                        hasExplicitAnswer = hasExplicitAnswer,
                        explanation = curExplanation.trim().ifBlank { "Imported from PDF text." },
                        difficulty = curDifficulty,
                        sourceNote = curSourceNote,
                        sourceStatus = curSourceStatus,
                        topicName = curTopicName
                    )
                )
            }
            hasCurrent = false
            section = ParseSection.NONE
            curQuestionText = ""
            curOptions = mutableListOf()
        }

        fun addOption(optIdx: Int, rawBody: String) {
            // If we didn't have an explicit Q header yet, promote accumulated pending stem lines!
            if (!hasCurrent) {
                val stem = pendingStemLines.joinToString(" ").trim()
                if (stem.isBlank()) return
                resetCurrent(num = null, stem = stem)
            }
            var body = rawBody.trim()
            var markedCorrect = false
            if (body.startsWith("*") && body.length > 2) {
                markedCorrect = true
                body = body.removePrefix("*").trim()
            }
            if (inlineCorrectMarkerRegex.containsMatchIn(body)) {
                markedCorrect = true
                body = inlineCorrectMarkerRegex.replace(body, "").trim()
            }
            if (markedCorrect) {
                curAnswerIdx = optIdx.coerceIn(0, 3)
                hasExplicitAnswer = true
            }
            curOptions.add(body)
            section = ParseSection.OPTION
        }

        for (i in lines.indices) {
            val line = lines[i]

            // 1. Metadata & Answer tags
            val diffMatch = diffRegex.find(line)
            if (diffMatch != null && hasCurrent) {
                curDifficulty = QuestionDifficulty.fromRaw(diffMatch.groupValues[1])
                continue
            }
            val sourceStatusMatch = sourceStatusRegex.find(line)
            if (sourceStatusMatch != null && hasCurrent) {
                curSourceStatus = sourceStatusMatch.groupValues[1].trim().lowercase()
                continue
            }
            val sourceNoteMatch = sourceNoteRegex.find(line)
            if (sourceNoteMatch != null && hasCurrent) {
                curSourceNote = sourceNoteMatch.groupValues[1].trim()
                continue
            }
            val topicMatch = topicRegex.find(line)
            if (topicMatch != null && hasCurrent) {
                curTopicName = topicMatch.groupValues[1].trim()
                continue
            }
            val expMatch = expRegex.find(line)
            if (expMatch != null && hasCurrent) {
                val expBody = expMatch.groupValues[1].trim()
                curExplanation = if (curExplanation.isBlank()) expBody else "$curExplanation $expBody"
                section = ParseSection.EXPLANATION
                continue
            }
            val ansMatch = ansRegex.find(line)
            if (ansMatch != null && hasCurrent && curOptions.isNotEmpty()) {
                curAnswerIdx = tokenToOptionIndex(ansMatch.groupValues[1])
                hasExplicitAnswer = true
                val trailing = ansMatch.groupValues[2].trim()
                if (trailing.isNotEmpty()) {
                    val trailingExp = expRegex.find(trailing)
                    if (trailingExp != null) {
                        val expBody = trailingExp.groupValues[1].trim()
                        curExplanation = if (curExplanation.isBlank()) expBody else "$curExplanation $expBody"
                        section = ParseSection.EXPLANATION
                    }
                }
                continue
            }

            // 2. Explicit Q-prefixed question header ("Q.1 ...", "Q. ...", "Question: ...", "Q.")
            val qPrefixMatch = qExplicitPrefixRegex.find(line)
            if (qPrefixMatch != null) {
                pushCurrent()
                val num = qPrefixMatch.groupValues[1].toIntOrNull()
                resetCurrent(num = num, stem = qPrefixMatch.groupValues[2])
                continue
            }
            val qHeaderOnlyMatch = qExplicitHeaderOnlyRegex.find(line)
            if (qHeaderOnlyMatch != null) {
                pushCurrent()
                val num = qHeaderOnlyMatch.groupValues[1].toIntOrNull()
                resetCurrent(num = num, stem = "")
                continue
            }

            // 3. Lettered option (A/B/C/D, standalone or with text)
            val letterOpt = matchLetterOption(line)
            if (letterOpt != null) {
                val (optIdx, optBody) = letterOpt
                if (hasCurrent && curOptions.size >= 4 && optIdx == 0) {
                    // Edge case: new option A while previous question already has 4 options
                    pushCurrent()
                }
                if (hasCurrent || pendingStemLines.isNotEmpty()) {
                    if (curOptions.size < 4) {
                        curUsesLetterOptions = true
                        addOption(optIdx, optBody)
                        continue
                    }
                }
            }

            // 4. Disambiguate bare numbered lines: "1. ..." could be a Question OR a numbered Option!
            val numOpt = matchNumberOption(line)
            val bareQ = qBareNumberRegex.find(line)
            if (numOpt != null || bareQ != null) {
                val bareNum = bareQ?.let {
                    it.groupValues[1].ifEmpty { it.groupValues[2] }.toIntOrNull()
                }
                val isSequentialNumberedOption = !curUsesLetterOptions &&
                    numOpt != null &&
                    bareNum != null &&
                    bareNum == curOptions.size + 1
                val isLikelyNextQuestion = when {
                    !hasCurrent -> pendingStemLines.isEmpty() || (bareNum != null && bareNum > 4)
                    curOptions.size >= 4 -> true
                    hasExplicitAnswer -> true
                    bareNum != null && bareNum > 4 -> true
                    isSequentialNumberedOption -> false
                    curOptions.size >= 2 && bareNum != null && lastQuestionNum != null && bareNum == lastQuestionNum + 1 -> true
                    // If we are inside an active question with 0 options and the number is 1, it's Option 1!
                    else -> false
                }

                if (isLikelyNextQuestion && bareQ != null) {
                    pushCurrent()
                    resetCurrent(num = bareNum, stem = bareQ.groupValues[3])
                    continue
                }
                if (numOpt != null && (hasCurrent || pendingStemLines.isNotEmpty()) && curOptions.size < 4) {
                    addOption(numOpt.first, numOpt.second)
                    continue
                }
            }

            // 5. Continuation / implicit unnumbered question detection
            if (hasCurrent) {
                when (section) {
                    ParseSection.STEM -> {
                        curQuestionText = if (curQuestionText.isBlank()) line else "$curQuestionText $line"
                    }
                    ParseSection.OPTION -> {
                        val awaitingStandaloneOptionBody = curOptions.isNotEmpty() && curOptions.last().isBlank()
                        val startsNewImplicitQuestion = !awaitingStandaloneOptionBody && (
                            (curOptions.size >= 2 && hasExplicitAnswer) ||
                                (curOptions.size >= 4 && looksLikeOptionStartWithin(lines, i + 1, window = 4))
                            )
                        if (startsNewImplicitQuestion) {
                            pushCurrent()
                            pendingStemLines.add(line)
                        } else if (curOptions.isNotEmpty()) {
                            // Append wrapped multi-line option text (or fill blank body from standalone "c)"/"d)" line)
                            val lastIdx = curOptions.lastIndex
                            val existing = curOptions[lastIdx]
                            curOptions[lastIdx] = if (existing.isBlank()) line else "$existing $line"
                        }
                    }
                    ParseSection.EXPLANATION -> {
                        val startsNewImplicitQuestion = looksLikeOptionStartWithin(lines, i + 1, window = 4)
                        if (startsNewImplicitQuestion) {
                            pushCurrent()
                            pendingStemLines.add(line)
                        } else {
                            curExplanation = if (curExplanation.isBlank()) line else "$curExplanation $line"
                        }
                    }
                    ParseSection.NONE -> {
                        pendingStemLines.add(line)
                    }
                }
            } else {
                pendingStemLines.add(line)
            }
        }
        pushCurrent()

        val baseTs = System.currentTimeMillis()
        val cleanShiftYear = shiftYear.trim()
        val (parsedYear, parsedShift) = parseShiftYear(cleanShiftYear)

        return drafts.mapIndexed { idx, draft ->
            val resolvedAnswer = if (draft.hasExplicitAnswer) {
                draft.correctIndex
            } else {
                val keyLookup = draft.explicitNumber?.let { trailingAnswerKey[it] }
                    ?: trailingAnswerKey[idx + 1]
                keyLookup ?: draft.correctIndex
            }
            val effectiveNote = draft.sourceNote.ifBlank { defaultSourceNote }.trim()
            val requestedStatus = draft.sourceStatus
                ?: if (markAsVerified) Question.SOURCE_VERIFIED else Question.SOURCE_UNVERIFIED
            val resolvedStatus = resolveSourceStatus(
                category = category,
                rawSourceStatus = requestedStatus,
                sourceNote = effectiveNote
            )
            val resolvedSourceType = QuestionSourceType.infer(
                category = category,
                shiftYear = cleanShiftYear,
                sourceStatus = resolvedStatus,
                sourceNote = effectiveNote,
                rawSourceType = defaultSourceType?.dbValue
            )
            val resolvedTopicId = when {
                draft.topicName.isNotBlank() -> slugTopicId(subject, draft.topicName)
                defaultTopicId.isNotBlank() -> defaultTopicId
                else -> PlatformCatalogDefaults.defaultTopicIdFor(subject)
            }
            Question(
                id = "C_${baseTs}_$idx",
                category = category,
                shiftYear = cleanShiftYear,
                subject = subject,
                difficulty = draft.difficulty,
                questionText = draft.questionText,
                options = draft.options,
                correctIndex = resolvedAnswer.coerceIn(0, 3),
                explanation = draft.explanation,
                isCustom = true,
                sourceStatus = resolvedStatus,
                sourceNote = effectiveNote,
                topicId = resolvedTopicId,
                sourceType = resolvedSourceType,
                examYear = parsedYear,
                examShift = parsedShift
            )
        }
    }

    fun parseJsonBatch(jsonText: String, markAsCustom: Boolean = true): List<Question> =
        parseJsonBatchWithExamIds(jsonText, markAsCustom).map { it.first }

    fun parseJsonBatchWithExamIds(
        jsonText: String,
        markAsCustom: Boolean = true
    ): List<Pair<Question, String>> {
        val trimmed = jsonText.trim().removePrefix("\uFEFF")
        val arr = if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            root.optJSONArray("questions")
                ?: root.optJSONArray("customQB")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("data")
                ?: JSONArray()
        } else {
            JSONArray(trimmed)
        }
        val baseTs = System.currentTimeMillis()
        val list = mutableListOf<Pair<Question, String>>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val qText = obj.optString("q", "").trim()
            val optsArr = obj.optJSONArray("o") ?: continue
            if (qText.isEmpty() || optsArr.length() < 2) continue
            val opts = MutableList(optsArr.length()) { idx -> optsArr.optString(idx, "") }
            while (opts.size < 4) opts.add("None of these")
            val rawId = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() }
            val rawDiff = obj.optString("d", "").ifBlank { obj.optString("difficulty", "") }
            val category = obj.optString("c", "nur").ifBlank { "nur" }
            val subject = obj.optString("s", "MSN").ifBlank { "MSN" }
            val rawExamId = obj.optString("examId", "").trim()
            val rawShiftYear = obj.optString("y", "").trim()
            val rawExamYear = obj.optString("examYear", "").trim()
            val rawExamShift = obj.optString("examShift", "").trim()
            val (splitYear, splitShift) = parseShiftYear(rawShiftYear)
            val resolvedYear = rawExamYear.ifBlank { splitYear }
            val resolvedShift = rawExamShift.ifBlank { splitShift }
            val resolvedShiftYear = rawShiftYear.ifBlank { composeShiftYear(resolvedYear, resolvedShift) }

            val rawNote = obj.optString("sourceNote", "").ifBlank { obj.optString("sn", "") }.trim()
            val rawStatus = obj.optString("sourceStatus", "").ifBlank { obj.optString("ss", "") }.trim()
            val rawSourceType = obj.optString("sourceType", "").trim().takeIf { it.isNotBlank() }
            val isBuiltInPredictedMock = category == "mock" ||
                rawSourceType.equals(QuestionSourceType.PREDICTED_MOCK.dbValue, ignoreCase = true)

            val resolvedNote = if (markAsCustom || isBuiltInPredictedMock) rawNote else ""
            val resolvedStatus = if (markAsCustom) {
                resolveSourceStatus(
                    category = category,
                    rawSourceStatus = rawStatus,
                    sourceNote = resolvedNote
                )
            } else {
                Question.SOURCE_UNVERIFIED
            }

            val resolvedSourceType = QuestionSourceType.infer(
                category = category,
                shiftYear = resolvedShiftYear,
                sourceStatus = resolvedStatus,
                sourceNote = resolvedNote,
                rawSourceType = if (markAsCustom || isBuiltInPredictedMock) rawSourceType else null
            )

            val rawTopicId = obj.optString("topicId", "").trim()
            val rawTopicName = obj.optString("topic", "").trim()
            val resolvedTopicId = when {
                rawTopicId.isNotBlank() -> rawTopicId
                rawTopicName.isNotBlank() -> slugTopicId(subject, rawTopicName)
                else -> PlatformCatalogDefaults.defaultTopicIdFor(subject)
            }

            val question = Question(
                id = rawId ?: "J_${baseTs}_$i",
                category = category,
                shiftYear = resolvedShiftYear,
                subject = subject,
                difficulty = QuestionDifficulty.fromRaw(rawDiff),
                questionText = qText,
                options = opts.take(4),
                correctIndex = obj.optInt("a", 0).coerceIn(0, 3),
                explanation = obj.optString("e", ""),
                isCustom = markAsCustom,
                sourceStatus = resolvedStatus,
                sourceNote = resolvedNote,
                topicId = resolvedTopicId,
                sourceType = resolvedSourceType,
                examYear = resolvedYear,
                examShift = resolvedShift
            )
            list.add(question to rawExamId)
        }
        return list
    }

    data class BackupStudentInfo(
        val displayName: String = "",
        val email: String = "",
        val role: String = "",
        val targetYear: String = "",
        val focusSubject: String = "",
        val phoneNumber: String = "",
        val activeExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING,
        val dailyGoalQuestions: Int = 25,
        val currentStreakDays: Int = 0,
        val longestStreakDays: Int = 0,
        val todayStudyDate: String = "",
        val todayAnsweredCount: Int = 0,
        val lastGoalMetDate: String = ""
    )

    data class BackupPlanInfo(
        val planName: String = "ClinEdge Free",
        val status: String = "Active",
        val tier: String = "free",
        val appVersion: String = ""
    )

    fun buildBackupJson(
        items: List<QuestionItem>,
        questionExamLinks: Map<String, List<String>> = emptyMap(),
        activeExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING,
        studentInfo: BackupStudentInfo? = null,
        planInfo: BackupPlanInfo = BackupPlanInfo()
    ): String {
        val root = JSONObject()
        val customArr = JSONArray()
        val bookmarksArr = JSONArray()
        val progressArr = JSONArray()

        items.forEach { item ->
            val q = item.question
            if (q.isCustom) {
                val examIds = questionExamLinks[q.id].orEmpty().ifEmpty { listOf(activeExamId) }
                customArr.put(
                    JSONObject().apply {
                        put("id", q.id)
                        put("c", q.category)
                        put("y", q.effectiveShiftYear)
                        put("s", q.subject)
                        put("d", q.difficulty.label)
                        put("q", q.questionText)
                        put("o", JSONArray(q.options))
                        put("a", q.correctIndex)
                        put("e", q.explanation)
                        put("sourceStatus", q.sourceStatus)
                        put("sourceNote", q.sourceNote)
                        put("topicId", q.topicId)
                        put("sourceType", q.sourceType.dbValue)
                        put("examYear", q.examYear)
                        put("examShift", q.examShift)
                        put("examIds", JSONArray(examIds))
                    }
                )
            }
            if (item.isBookmarked) {
                bookmarksArr.put(q.id)
            }
            if (item.attemptsCount > 0 || item.lastOutcome != null) {
                progressArr.put(
                    JSONObject().apply {
                        put("questionId", q.id)
                        put("isBookmarked", item.isBookmarked)
                        put("attemptsCount", item.attemptsCount)
                        put("correctCount", item.correctCount)
                        put("wrongCount", item.wrongCount)
                        put("lastOutcome", item.lastOutcome)
                        put("updatedAt", item.progress.updatedAt)
                    }
                )
            }
        }

        root.put("version", 4)
        root.put("app", "ClinEdge")
        root.put("activeExamId", activeExamId)
        root.put("exportedAt", System.currentTimeMillis())

        val resolvedStudent = studentInfo ?: BackupStudentInfo(activeExamId = activeExamId)
        root.put(
            "student",
            JSONObject().apply {
                put("displayName", resolvedStudent.displayName)
                put("email", resolvedStudent.email)
                put("role", resolvedStudent.role)
                put("targetYear", resolvedStudent.targetYear)
                put("focusSubject", resolvedStudent.focusSubject)
                put("phoneNumber", resolvedStudent.phoneNumber)
                put("activeExamId", resolvedStudent.activeExamId.ifBlank { activeExamId })
                put("dailyGoalQuestions", resolvedStudent.dailyGoalQuestions)
                put("currentStreakDays", resolvedStudent.currentStreakDays)
                put("longestStreakDays", resolvedStudent.longestStreakDays)
                put("todayStudyDate", resolvedStudent.todayStudyDate)
                put("todayAnsweredCount", resolvedStudent.todayAnsweredCount)
                put("lastGoalMetDate", resolvedStudent.lastGoalMetDate)
            }
        )

        root.put(
            "plan",
            JSONObject().apply {
                put("planName", planInfo.planName)
                put("status", planInfo.status)
                put("tier", planInfo.tier)
                put("appVersion", planInfo.appVersion)
            }
        )

        root.put("customQB", customArr)
        root.put("bookmarks", bookmarksArr)
        root.put("progress", progressArr)
        return root.toString(2)
    }

    data class RestoredBackup(
        val customQuestions: List<Question>,
        val progressList: List<QuestionProgressEntity>,
        val customQuestionExamIds: Map<String, List<String>> = emptyMap(),
        val activeExamId: String? = null,
        val studentInfo: BackupStudentInfo? = null,
        val planInfo: BackupPlanInfo? = null
    )

    fun parseBackupJson(jsonText: String): RestoredBackup {
        val trimmed = jsonText.trim()
        if (trimmed.startsWith("[")) {
            return RestoredBackup(parseJsonBatch(trimmed, markAsCustom = true), emptyList())
        }
        val root = JSONObject(trimmed)
        val customArr = root.optJSONArray("customQB")
        val customQuestions = if (customArr != null) {
            parseJsonBatch(customArr.toString(), markAsCustom = true)
        } else {
            emptyList()
        }

        val customExamMap = mutableMapOf<String, List<String>>()
        if (customArr != null) {
            for (i in 0 until customArr.length()) {
                val obj = customArr.optJSONObject(i) ?: continue
                val id = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() } ?: continue
                val examsArr = obj.optJSONArray("examIds") ?: continue
                val examIds = buildList {
                    for (j in 0 until examsArr.length()) {
                        examsArr.optString(j).trim().takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
                if (examIds.isNotEmpty()) {
                    customExamMap[id] = examIds
                }
            }
        }

        val bookmarkSet = mutableSetOf<String>()
        val bmArr = root.optJSONArray("bookmarks")
        if (bmArr != null) {
            for (i in 0 until bmArr.length()) {
                bmArr.optString(i).takeIf { it.isNotBlank() }?.let { bookmarkSet.add(it) }
            }
        }

        val progressMap = mutableMapOf<String, QuestionProgressEntity>()
        val progArr = root.optJSONArray("progress")
        if (progArr != null) {
            for (i in 0 until progArr.length()) {
                val o = progArr.optJSONObject(i) ?: continue
                val qId = o.optString("questionId", "").takeIf { it.isNotBlank() } ?: continue
                progressMap[qId] = QuestionProgressEntity(
                    questionId = qId,
                    isBookmarked = o.optBoolean("isBookmarked", qId in bookmarkSet),
                    attemptsCount = o.optInt("attemptsCount", 0),
                    correctCount = o.optInt("correctCount", 0),
                    wrongCount = o.optInt("wrongCount", 0),
                    lastOutcome = o.optString("lastOutcome").takeIf { it == "right" || it == "wrong" },
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        }
        bookmarkSet.forEach { qId ->
            if (qId !in progressMap) {
                progressMap[qId] = QuestionProgressEntity(
                    questionId = qId,
                    isBookmarked = true,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        val parsedActiveExamId = root.optString("activeExamId", "").trim().takeIf { it.isNotBlank() }

        val studentObj = root.optJSONObject("student")
        val parsedStudent = if (studentObj != null) {
            BackupStudentInfo(
                displayName = studentObj.optString("displayName", "").trim(),
                email = studentObj.optString("email", "").trim(),
                role = studentObj.optString("role", "").trim(),
                targetYear = studentObj.optString("targetYear", "").trim(),
                focusSubject = studentObj.optString("focusSubject", "").trim(),
                phoneNumber = studentObj.optString("phoneNumber", "").trim(),
                activeExamId = studentObj.optString("activeExamId", parsedActiveExamId ?: PlatformCatalogDefaults.EXAM_RRB_NURSING).trim(),
                dailyGoalQuestions = studentObj.optInt("dailyGoalQuestions", 25),
                currentStreakDays = studentObj.optInt("currentStreakDays", 0),
                longestStreakDays = studentObj.optInt("longestStreakDays", 0),
                todayStudyDate = studentObj.optString("todayStudyDate", "").trim(),
                todayAnsweredCount = studentObj.optInt("todayAnsweredCount", 0),
                lastGoalMetDate = studentObj.optString("lastGoalMetDate", "").trim()
            )
        } else {
            null
        }

        val planObj = root.optJSONObject("plan")
        val parsedPlan = if (planObj != null) {
            BackupPlanInfo(
                planName = planObj.optString("planName", "ClinEdge Free").trim().ifBlank { "ClinEdge Free" },
                status = planObj.optString("status", "Active").trim().ifBlank { "Active" },
                tier = planObj.optString("tier", "free").trim().ifBlank { "free" },
                appVersion = planObj.optString("appVersion", "").trim()
            )
        } else {
            null
        }

        return RestoredBackup(
            customQuestions = customQuestions,
            progressList = progressMap.values.toList(),
            customQuestionExamIds = customExamMap,
            activeExamId = parsedActiveExamId ?: parsedStudent?.activeExamId?.takeIf { it.isNotBlank() },
            studentInfo = parsedStudent,
            planInfo = parsedPlan
        )
    }
}
