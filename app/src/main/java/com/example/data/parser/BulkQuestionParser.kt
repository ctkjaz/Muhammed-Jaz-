package com.example.data.parser

import com.example.data.local.QuestionProgressEntity
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import org.json.JSONArray
import org.json.JSONObject

object BulkQuestionParser {
    private val qRegex = Regex("""^(?:Q\.?\s*\d+[.):]?|\d+[.)])\s+(.{8,})""", RegexOption.IGNORE_CASE)
    private val optRegex = Regex("""^(?:\(?([1-4A-Da-d])[.)]|Ans\s*([1-4])\.)\s+(.+)""")
    private val ansRegex = Regex("""^(?:Ans(?:wer)?|Correct\s*(?:Option|Answer))\s*[:-]\s*([1-4A-Da-d])""", RegexOption.IGNORE_CASE)
    private val expRegex = Regex("""^(?:Exp(?:lanation)?|Rationale|Sol(?:ution)?)\s*[:-]\s*(.+)""", RegexOption.IGNORE_CASE)
    private val diffRegex = Regex("""^(?:Diff(?:iculty)?|Level)\s*[:-]\s*(Easy|Medium|Hard)""", RegexOption.IGNORE_CASE)

    fun parseRawPdfText(
        rawText: String,
        category: String,
        subject: String,
        shiftYear: String,
        defaultDifficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM
    ): List<Question> {
        val lines = rawText.replace("\r", "")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsed = mutableListOf<Question>()
        var curQuestionText = ""
        var curOptions = mutableListOf<String>()
        var curAnswerIdx = 0
        var curExplanation = "Imported from PDF text."
        var curDifficulty = defaultDifficulty
        var hasCurrent = false
        val baseTs = System.currentTimeMillis()

        fun pushCurrent() {
            if (hasCurrent && curQuestionText.isNotBlank() && curOptions.size >= 2) {
                val padded = curOptions.toMutableList()
                while (padded.size < 4) padded.add("None of these")
                val safeAns = curAnswerIdx.coerceIn(0, 3)
                parsed.add(
                    Question(
                        id = "C_${baseTs}_${parsed.size}",
                        category = category,
                        shiftYear = shiftYear.trim(),
                        subject = subject,
                        difficulty = curDifficulty,
                        questionText = curQuestionText.trim(),
                        options = padded.take(4),
                        correctIndex = safeAns,
                        explanation = curExplanation.trim(),
                        isCustom = true
                    )
                )
            }
        }

        for (line in lines) {
            val ansMatch = ansRegex.find(line)
            val expMatch = expRegex.find(line)
            val diffMatch = diffRegex.find(line)
            val qMatch = qRegex.find(line)
            val optMatch = optRegex.find(line)

            when {
                ansMatch != null && hasCurrent -> {
                    val tok = ansMatch.groupValues[1].uppercase()
                    curAnswerIdx = when (tok) {
                        "1", "2", "3", "4" -> tok.toInt() - 1
                        else -> (tok.first() - 'A').coerceIn(0, 3)
                    }
                }
                expMatch != null && hasCurrent -> {
                    curExplanation = expMatch.groupValues[1].trim()
                }
                diffMatch != null && hasCurrent -> {
                    curDifficulty = QuestionDifficulty.fromRaw(diffMatch.groupValues[1])
                }
                qMatch != null && (!hasCurrent || line.startsWith("Q", ignoreCase = true)) -> {
                    pushCurrent()
                    hasCurrent = true
                    curQuestionText = qMatch.groupValues[1].trim()
                    curOptions = mutableListOf()
                    curAnswerIdx = 0
                    curDifficulty = defaultDifficulty
                    curExplanation = "Imported from PDF text."
                }
                optMatch != null && hasCurrent && curOptions.size < 4 -> {
                    curOptions.add(optMatch.groupValues[3].trim())
                }
                qMatch != null && curOptions.size >= 2 -> {
                    pushCurrent()
                    hasCurrent = true
                    curQuestionText = qMatch.groupValues[1].trim()
                    curOptions = mutableListOf()
                    curAnswerIdx = 0
                    curDifficulty = defaultDifficulty
                    curExplanation = "Imported from PDF text."
                }
                hasCurrent -> {
                    if (curOptions.isEmpty()) {
                        curQuestionText += " $line"
                    } else if (curExplanation != "Imported from PDF text.") {
                        curExplanation += " $line"
                    } else if (optMatch != null) {
                        curOptions.add(optMatch.groupValues[3].trim())
                    }
                }
            }
        }
        pushCurrent()
        return parsed
    }

    fun parseJsonBatch(jsonText: String, markAsCustom: Boolean = true): List<Question> {
        val arr = JSONArray(jsonText.trim())
        val baseTs = System.currentTimeMillis()
        val list = mutableListOf<Question>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val qText = obj.optString("q", "").trim()
            val optsArr = obj.optJSONArray("o") ?: continue
            if (qText.isEmpty() || optsArr.length() < 2) continue
            val opts = MutableList(optsArr.length()) { idx -> optsArr.optString(idx, "") }
            while (opts.size < 4) opts.add("None of these")
            val rawId = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() }
            val rawDiff = obj.optString("d", "").ifBlank { obj.optString("difficulty", "") }
            list.add(
                Question(
                    id = rawId ?: "J_${baseTs}_$i",
                    category = obj.optString("c", "nur").ifBlank { "nur" },
                    shiftYear = obj.optString("y", ""),
                    subject = obj.optString("s", "MSN").ifBlank { "MSN" },
                    difficulty = QuestionDifficulty.fromRaw(rawDiff),
                    questionText = qText,
                    options = opts.take(4),
                    correctIndex = obj.optInt("a", 0).coerceIn(0, 3),
                    explanation = obj.optString("e", ""),
                    isCustom = markAsCustom
                )
            )
        }
        return list
    }

    fun buildBackupJson(items: List<QuestionItem>): String {
        val root = JSONObject()
        val customArr = JSONArray()
        val bookmarksArr = JSONArray()
        val progressArr = JSONArray()

        items.forEach { item ->
            val q = item.question
            if (q.isCustom) {
                customArr.put(
                    JSONObject().apply {
                        put("id", q.id)
                        put("c", q.category)
                        put("y", q.shiftYear)
                        put("s", q.subject)
                        put("d", q.difficulty.label)
                        put("q", q.questionText)
                        put("o", JSONArray(q.options))
                        put("a", q.correctIndex)
                        put("e", q.explanation)
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

        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("customQB", customArr)
        root.put("bookmarks", bookmarksArr)
        root.put("progress", progressArr)
        return root.toString(2)
    }

    data class RestoredBackup(
        val customQuestions: List<Question>,
        val progressList: List<QuestionProgressEntity>
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

        return RestoredBackup(
            customQuestions = customQuestions,
            progressList = progressMap.values.toList()
        )
    }
}
