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
    private val qRegex = Regex("""^(?:Q\.?\s*\d+[.):]?|\d+[.)])\s+(.{8,})""", RegexOption.IGNORE_CASE)
    private val optRegex = Regex("""^(?:\(?([1-4A-Da-d])[.)]|Ans\s*([1-4])\.)\s+(.+)""")
    private val ansRegex = Regex("""^(?:Ans(?:wer)?|Correct\s*(?:Option|Answer))\s*[:-]\s*([1-4A-Da-d])""", RegexOption.IGNORE_CASE)
    private val expRegex = Regex("""^(?:Exp(?:lanation)?|Rationale|Sol(?:ution)?)\s*[:-]\s*(.+)""", RegexOption.IGNORE_CASE)
    private val diffRegex = Regex("""^(?:Diff(?:iculty)?|Level)\s*[:-]\s*(Easy|Medium|Hard)""", RegexOption.IGNORE_CASE)
    private val sourceNoteRegex = Regex("""^(?:Source(?:\s*Note)?|Ref(?:erence)?)\s*[:-]\s*(.+)""", RegexOption.IGNORE_CASE)
    private val sourceStatusRegex = Regex("""^(?:Source\s*Status|Verification)\s*[:-]\s*(verified|unverified)""", RegexOption.IGNORE_CASE)
    private val topicRegex = Regex("""^(?:Topic)\s*[:-]\s*(.+)""", RegexOption.IGNORE_CASE)

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
        var curSourceNote = ""
        var curSourceStatus: String? = null
        var curTopicName = ""
        var hasCurrent = false
        val baseTs = System.currentTimeMillis()
        val cleanShiftYear = shiftYear.trim()
        val (parsedYear, parsedShift) = parseShiftYear(cleanShiftYear)

        fun pushCurrent() {
            if (hasCurrent && curQuestionText.isNotBlank() && curOptions.size >= 2) {
                val padded = curOptions.toMutableList()
                while (padded.size < 4) padded.add("None of these")
                val safeAns = curAnswerIdx.coerceIn(0, 3)
                val effectiveNote = curSourceNote.ifBlank { defaultSourceNote }.trim()
                val requestedStatus = curSourceStatus
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
                    curTopicName.isNotBlank() -> slugTopicId(subject, curTopicName)
                    defaultTopicId.isNotBlank() -> defaultTopicId
                    else -> PlatformCatalogDefaults.defaultTopicIdFor(subject)
                }
                parsed.add(
                    Question(
                        id = "C_${baseTs}_${parsed.size}",
                        category = category,
                        shiftYear = cleanShiftYear,
                        subject = subject,
                        difficulty = curDifficulty,
                        questionText = curQuestionText.trim(),
                        options = padded.take(4),
                        correctIndex = safeAns,
                        explanation = curExplanation.trim(),
                        isCustom = true,
                        sourceStatus = resolvedStatus,
                        sourceNote = effectiveNote,
                        topicId = resolvedTopicId,
                        sourceType = resolvedSourceType,
                        examYear = parsedYear,
                        examShift = parsedShift
                    )
                )
            }
        }

        for (line in lines) {
            val ansMatch = ansRegex.find(line)
            val expMatch = expRegex.find(line)
            val diffMatch = diffRegex.find(line)
            val sourceStatusMatch = sourceStatusRegex.find(line)
            val sourceNoteMatch = sourceNoteRegex.find(line)
            val topicMatch = topicRegex.find(line)
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
                sourceStatusMatch != null && hasCurrent -> {
                    curSourceStatus = sourceStatusMatch.groupValues[1].trim().lowercase()
                }
                sourceNoteMatch != null && hasCurrent -> {
                    curSourceNote = sourceNoteMatch.groupValues[1].trim()
                }
                topicMatch != null && hasCurrent -> {
                    curTopicName = topicMatch.groupValues[1].trim()
                }
                qMatch != null && (!hasCurrent || line.startsWith("Q", ignoreCase = true)) -> {
                    pushCurrent()
                    hasCurrent = true
                    curQuestionText = qMatch.groupValues[1].trim()
                    curOptions = mutableListOf()
                    curAnswerIdx = 0
                    curDifficulty = defaultDifficulty
                    curExplanation = "Imported from PDF text."
                    curSourceNote = ""
                    curSourceStatus = null
                    curTopicName = ""
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
                    curSourceNote = ""
                    curSourceStatus = null
                    curTopicName = ""
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
            val category = obj.optString("c", "nur").ifBlank { "nur" }
            val subject = obj.optString("s", "MSN").ifBlank { "MSN" }
            val rawShiftYear = obj.optString("y", "").trim()
            val rawExamYear = obj.optString("examYear", "").trim()
            val rawExamShift = obj.optString("examShift", "").trim()
            val (splitYear, splitShift) = parseShiftYear(rawShiftYear)
            val resolvedYear = rawExamYear.ifBlank { splitYear }
            val resolvedShift = rawExamShift.ifBlank { splitShift }
            val resolvedShiftYear = rawShiftYear.ifBlank { composeShiftYear(resolvedYear, resolvedShift) }

            val rawNote = obj.optString("sourceNote", "").ifBlank { obj.optString("sn", "") }.trim()
            val rawStatus = obj.optString("sourceStatus", "").ifBlank { obj.optString("ss", "") }.trim()
            val resolvedNote = if (markAsCustom) rawNote else ""
            val resolvedStatus = if (markAsCustom) {
                resolveSourceStatus(
                    category = category,
                    rawSourceStatus = rawStatus,
                    sourceNote = resolvedNote
                )
            } else {
                Question.SOURCE_UNVERIFIED
            }

            val rawSourceType = obj.optString("sourceType", "").trim().takeIf { it.isNotBlank() }
            val resolvedSourceType = QuestionSourceType.infer(
                category = category,
                shiftYear = resolvedShiftYear,
                sourceStatus = resolvedStatus,
                sourceNote = resolvedNote,
                rawSourceType = if (markAsCustom) rawSourceType else null
            )

            val rawTopicId = obj.optString("topicId", "").trim()
            val rawTopicName = obj.optString("topic", "").trim()
            val resolvedTopicId = when {
                rawTopicId.isNotBlank() -> rawTopicId
                rawTopicName.isNotBlank() -> slugTopicId(subject, rawTopicName)
                else -> PlatformCatalogDefaults.defaultTopicIdFor(subject)
            }

            list.add(
                Question(
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
            )
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
