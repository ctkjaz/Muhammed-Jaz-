package com.example.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.data.local.ExamQuestionLinkEntity
import com.example.data.local.MockHistoryEntity
import com.example.data.local.QuestionDao
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.CategoryStat
import com.example.data.model.Exam
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamSection
import com.example.data.model.Flashcard
import com.example.data.model.MockHistory
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Profession
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectStat
import com.example.data.model.Topic
import com.example.data.parser.BulkQuestionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class NursingPrepRepository(
    private val context: Context,
    private val dao: QuestionDao
) {
    val professions: Flow<List<Profession>> =
        dao.observeProfessions().map { list -> list.map { it.toDomain() } }

    val exams: Flow<List<Exam>> =
        dao.observeExams().map { list -> list.map { it.toDomain() } }

    val topics: Flow<List<Topic>> =
        dao.observeTopics().map { list -> list.map { it.toDomain() } }

    val allQuestions: Flow<List<QuestionItem>> =
        dao.observeAllQuestionsWithProgress().map { list -> list.map { it.toDomain() } }

    val allExamLinks: Flow<List<ExamQuestionLink>> =
        dao.observeAllExamLinks().map { list -> list.map { it.toDomain() } }

    val mockHistory: Flow<List<MockHistory>> =
        dao.observeMockHistory().map { list -> list.map { it.toDomain() } }

    fun observeExamSections(examId: String): Flow<List<ExamSection>> =
        dao.observeExamSections(examId).map { list -> list.map { it.toDomain() } }

    fun observeQuestionsForExam(examId: String): Flow<List<QuestionItem>> =
        dao.observeQuestionsForExam(examId).map { list -> list.map { it.toDomain() } }

    fun observeMockHistoryForExam(examId: String): Flow<List<MockHistory>> =
        dao.observeMockHistoryForExam(examId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureCatalogSeeded() = withContext(Dispatchers.IO) {
        dao.upsertProfessions(PlatformCatalogDefaults.DEFAULT_PROFESSIONS.map { it.toEntity() })
        dao.upsertExams(PlatformCatalogDefaults.DEFAULT_EXAMS.map { it.toEntity() })
        dao.upsertExamSections(PlatformCatalogDefaults.DEFAULT_SECTIONS.map { it.toEntity() })

        val subjectEntities = SubjectCatalog.ALL_SUBJS.mapIndexed { idx, meta ->
            val profId = if (SubjectCatalog.isNursingCode(meta.code)) {
                PlatformCatalogDefaults.PROFESSION_NURSING
            } else {
                null
            }
            SubjectEntity(
                code = meta.code,
                professionId = profId,
                name = meta.name,
                accentHex = meta.accentHex,
                sortOrder = idx
            )
        }
        dao.upsertSubjects(subjectEntities)

        val defaultTopics = SubjectCatalog.ALL_SUBJS.map { meta ->
            TopicEntity(
                id = PlatformCatalogDefaults.defaultTopicIdFor(meta.code),
                subjectCode = meta.code,
                name = "General",
                sortOrder = 0
            )
        }
        dao.upsertTopics(defaultTopics)
    }

    suspend fun ensureSeeded(
        currentSeedVersion: Int = 0,
        onSeededVersionUpdated: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        ensureCatalogSeeded()
        val needsQuestions = dao.getBuiltInCount() == 0 || currentSeedVersion < CURRENT_SEED_VERSION
        val needsRrbLinks = dao.getLinkCountForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING) == 0
        if (needsQuestions || needsRrbLinks) {
            val json = context.assets.open("builtin_questions.json").bufferedReader().use { it.readText() }
            val questions = BulkQuestionParser.parseJsonBatch(json, markAsCustom = false)
            val entities = questions.mapIndexed { index, q -> q.toEntity(sortOrder = index) }
            if (needsQuestions) {
                dao.insertQuestions(entities)
            }
            val rrbLinks = questions.mapIndexed { index, q ->
                ExamQuestionLinkEntity(
                    examId = PlatformCatalogDefaults.EXAM_RRB_NURSING,
                    questionId = q.id,
                    sectionId = PlatformCatalogDefaults.sectionIdForRrbSubject(q.subject),
                    sortOrder = index,
                    isPyqForExam = q.isPyqTagged,
                    examYear = q.examYear,
                    examShift = q.examShift
                )
            }
            dao.upsertExamQuestionLinks(rrbLinks)
            onSeededVersionUpdated(CURRENT_SEED_VERSION)
        }
    }

    suspend fun linkQuestionToExam(
        questionId: String,
        examId: String,
        sectionId: String,
        sortOrder: Int = 0,
        isPyqForExam: Boolean = false,
        examYear: String = "",
        examShift: String = ""
    ) = withContext(Dispatchers.IO) {
        dao.upsertExamQuestionLinks(
            listOf(
                ExamQuestionLinkEntity(
                    examId = examId,
                    questionId = questionId,
                    sectionId = sectionId,
                    sortOrder = sortOrder,
                    isPyqForExam = isPyqForExam,
                    examYear = examYear,
                    examShift = examShift
                )
            )
        )
    }

    suspend fun ensureTopicExists(subjectCode: String, topicName: String): String = withContext(Dispatchers.IO) {
        val cleanSubject = subjectCode.trim().ifBlank { "MSN" }
        val topicId = BulkQuestionParser.slugTopicId(cleanSubject, topicName)
        val displayName = topicName.trim().ifBlank { "General" }
        dao.upsertSubjects(
            listOf(
                SubjectEntity(
                    code = cleanSubject,
                    professionId = if (SubjectCatalog.isNursingCode(cleanSubject)) {
                        PlatformCatalogDefaults.PROFESSION_NURSING
                    } else null,
                    name = SubjectCatalog.displayNameFor(cleanSubject),
                    accentHex = SubjectCatalog.ALL_SUBJS.firstOrNull { it.code == cleanSubject }?.accentHex ?: "#726D70",
                    sortOrder = SubjectCatalog.ALL_SUBJS.indexOfFirst { it.code == cleanSubject }.takeIf { it >= 0 } ?: 99
                )
            )
        )
        dao.upsertTopics(
            listOf(
                TopicEntity(
                    id = topicId,
                    subjectCode = cleanSubject,
                    name = displayName,
                    sortOrder = if (displayName == "General") 0 else 10
                )
            )
        )
        topicId
    }

    companion object {
        const val CURRENT_SEED_VERSION = 5
    }

    suspend fun loadFlashcards(): List<Flashcard> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.assets.open("ca_flashcards.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                Flashcard(
                    id = obj.optInt("id", i + 1),
                    title = obj.optString("t", ""),
                    tag = obj.optString("tag", ""),
                    body = obj.optString("b", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun recordAttempt(questionId: String, isCorrect: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getProgressForQuestion(questionId)
        val now = System.currentTimeMillis()
        val updated = if (existing == null) {
            QuestionProgressEntity(
                questionId = questionId,
                isBookmarked = false,
                attemptsCount = 1,
                correctCount = if (isCorrect) 1 else 0,
                wrongCount = if (isCorrect) 0 else 1,
                lastOutcome = if (isCorrect) "right" else "wrong",
                updatedAt = now
            )
        } else {
            existing.copy(
                attemptsCount = existing.attemptsCount + 1,
                correctCount = existing.correctCount + if (isCorrect) 1 else 0,
                wrongCount = existing.wrongCount + if (isCorrect) 0 else 1,
                lastOutcome = if (isCorrect) "right" else "wrong",
                updatedAt = now
            )
        }
        dao.upsertProgress(updated)
    }

    suspend fun recordBatchAttempts(results: List<Pair<String, Boolean>>) = withContext(Dispatchers.IO) {
        if (results.isEmpty()) return@withContext
        val existingMap = dao.getAllProgressSnapshot().associateBy { it.questionId }.toMutableMap()
        val now = System.currentTimeMillis()
        val updates = results.map { (qId, isCorrect) ->
            val prev = existingMap[qId]
            val updated = if (prev == null) {
                QuestionProgressEntity(
                    questionId = qId,
                    isBookmarked = false,
                    attemptsCount = 1,
                    correctCount = if (isCorrect) 1 else 0,
                    wrongCount = if (isCorrect) 0 else 1,
                    lastOutcome = if (isCorrect) "right" else "wrong",
                    updatedAt = now
                )
            } else {
                prev.copy(
                    attemptsCount = prev.attemptsCount + 1,
                    correctCount = prev.correctCount + if (isCorrect) 1 else 0,
                    wrongCount = prev.wrongCount + if (isCorrect) 0 else 1,
                    lastOutcome = if (isCorrect) "right" else "wrong",
                    updatedAt = now
                )
            }
            existingMap[qId] = updated
            updated
        }
        dao.upsertProgressList(updates)
    }

    suspend fun toggleBookmark(questionId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getProgressForQuestion(questionId)
        val now = System.currentTimeMillis()
        val updated = existing?.copy(
            isBookmarked = !existing.isBookmarked,
            updatedAt = now
        ) ?: QuestionProgressEntity(
            questionId = questionId,
            isBookmarked = true,
            updatedAt = now
        )
        dao.upsertProgress(updated)
    }

    suspend fun saveCustomQuestions(
        questions: List<Question>,
        examIds: List<String> = listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING),
        customExamMap: Map<String, List<String>> = emptyMap(),
        topicLabelOverrides: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext
        ensureCatalogSeeded()
        val startOrder = dao.getTotalCount() + 1000
        val entities = questions.mapIndexed { idx, q ->
            val explicitTopicLabel = topicLabelOverrides[q.topicId]
            if (explicitTopicLabel != null) {
                ensureTopicExists(q.subject, explicitTopicLabel)
            }
            q.copy(isCustom = true).toEntity(sortOrder = startOrder + idx)
        }
        dao.insertQuestions(entities)

        val validTargetExamIds = examIds.ifEmpty { listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING) }
        val links = buildList {
            questions.forEachIndexed { idx, q ->
                val targets = customExamMap[q.id].orEmpty().ifEmpty { validTargetExamIds }
                targets.distinct().forEach { targetExamId ->
                    val resolvedExamId = targetExamId.ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }
                    add(
                        ExamQuestionLinkEntity(
                            examId = resolvedExamId,
                            questionId = q.id,
                            sectionId = PlatformCatalogDefaults.sectionIdForRrbSubject(q.subject),
                            sortOrder = startOrder + idx,
                            isPyqForExam = q.isPyqTagged,
                            examYear = q.examYear,
                            examShift = q.examShift
                        )
                    )
                }
            }
        }
        if (links.isNotEmpty()) {
            dao.upsertExamQuestionLinks(links)
        }
    }

    suspend fun clearCustomQuestions() = withContext(Dispatchers.IO) {
        dao.deleteCustomQuestionLinks()
        dao.deleteCustomQuestions()
    }

    suspend fun restoreBackup(
        jsonText: String,
        defaultExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
    ): Int = withContext(Dispatchers.IO) {
        val restored = BulkQuestionParser.parseBackupJson(jsonText)
        if (restored.customQuestions.isNotEmpty()) {
            saveCustomQuestions(
                questions = restored.customQuestions,
                examIds = listOf(defaultExamId),
                customExamMap = restored.customQuestionExamIds
            )
        }
        if (restored.progressList.isNotEmpty()) {
            dao.upsertProgressList(restored.progressList)
        }
        restored.customQuestions.size
    }

    suspend fun saveMockHistory(
        title: String,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int,
        netScore: Double,
        accuracyPercent: Int,
        examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
    ) = withContext(Dispatchers.IO) {
        dao.insertMockHistory(
            MockHistoryEntity(
                title = title,
                totalQuestions = totalQuestions,
                correctCount = correctCount,
                wrongCount = wrongCount,
                skippedCount = skippedCount,
                netScore = netScore,
                accuracyPercent = accuracyPercent,
                timestamp = System.currentTimeMillis(),
                examId = examId
            )
        )
    }

    suspend fun clearStatistics() = withContext(Dispatchers.IO) {
        val allProgress = dao.getAllProgressSnapshot()
        val resetBookmarksKept = allProgress.map {
            it.copy(attemptsCount = 0, correctCount = 0, wrongCount = 0, lastOutcome = null)
        }
        dao.upsertProgressList(resetBookmarksKept)
    }

    fun computeSubjectStats(items: List<QuestionItem>): List<SubjectStat> {
        val grouped = items.groupBy { it.question.subject }
        return SubjectCatalog.ALL_SUBJS.mapNotNull { meta ->
            val list = grouped[meta.code].orEmpty()
            if (list.isEmpty()) return@mapNotNull null
            SubjectStat(
                code = meta.code,
                name = meta.name,
                group = meta.group,
                totalQuestions = list.size,
                attemptedQuestions = list.count { it.attemptsCount > 0 },
                totalAttempts = list.sumOf { it.attemptsCount },
                correctAttempts = list.sumOf { it.correctCount },
                wrongAttempts = list.sumOf { it.wrongCount },
                masteredQuestions = list.count { it.lastOutcome == "right" },
                accentColor = meta.accentColor
            )
        }
    }

    fun computeCategoryStats(items: List<QuestionItem>): List<CategoryStat> {
        data class CatDef(val key: String, val label: String, val desc: String, val color: Color, val filter: (QuestionItem) -> Boolean)
        val defs = listOf(
            CatDef("pyq", "PYQ-Tagged (2015–2026)", "Shift-tagged questions (unverified unless matched to official keys)", Color(0xFFA67942)) {
                it.question.isPyqTagged
            },
            CatDef("nur", "Nursing (70%)", "All 9 clinical nursing subjects", Color(0xFF8E4456)) {
                SubjectCatalog.isNursing(it.question)
            },
            CatDef("ca", "CA & Static GK", "2025–26 current affairs, railways & polity", Color(0xFFB78B79)) {
                SubjectCatalog.isCa(it.question)
            },
            CatDef("gen", "Sci / Math / Reas (30%)", "General science, arithmetic & reasoning", Color(0xFF527D86)) {
                SubjectCatalog.isGen(it.question)
            }
        )
        return defs.map { d ->
            val list = items.filter(d.filter)
            CategoryStat(
                key = d.key,
                label = d.label,
                description = d.desc,
                totalQuestions = list.size,
                attemptedQuestions = list.count { it.attemptsCount > 0 },
                totalAttempts = list.sumOf { it.attemptsCount },
                correctAttempts = list.sumOf { it.correctCount },
                wrongAttempts = list.sumOf { it.wrongCount },
                accentColor = d.color
            )
        }
    }

    fun findLowestAccuracyQuestions(items: List<QuestionItem>): List<QuestionItem> {
        return items
            .filter { it.attemptsCount > 0 && (it.accuracyPercent < 100 || it.wrongCount > 0) }
            .sortedWith(
                compareBy<QuestionItem> { it.accuracyPercent }
                    .thenByDescending { it.wrongCount }
                    .thenByDescending { it.attemptsCount }
            )
    }
}
