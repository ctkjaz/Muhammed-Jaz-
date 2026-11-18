package com.example.data.repository

import android.content.Context
import com.example.data.local.MockHistoryEntity
import com.example.data.local.QuestionDao
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.CategoryStat
import com.example.data.model.Flashcard
import com.example.data.model.MockHistory
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectStat
import com.example.data.parser.BulkQuestionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import androidx.compose.ui.graphics.Color

class NursingPrepRepository(
    private val context: Context,
    private val dao: QuestionDao
) {
    val allQuestions: Flow<List<QuestionItem>> =
        dao.observeAllQuestionsWithProgress().map { list -> list.map { it.toDomain() } }

    val mockHistory: Flow<List<MockHistory>> =
        dao.observeMockHistory().map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeeded(
        currentSeedVersion: Int = 0,
        onSeededVersionUpdated: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (dao.getBuiltInCount() == 0 || currentSeedVersion < CURRENT_SEED_VERSION) {
            val json = context.assets.open("builtin_questions.json").bufferedReader().use { it.readText() }
            val questions = BulkQuestionParser.parseJsonBatch(json, markAsCustom = false)
            val entities = questions.mapIndexed { index, q -> q.toEntity(sortOrder = index) }
            dao.insertQuestions(entities)
            onSeededVersionUpdated(CURRENT_SEED_VERSION)
        }
    }

    companion object {
        const val CURRENT_SEED_VERSION = 3
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

    suspend fun saveCustomQuestions(questions: List<Question>) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext
        val startOrder = dao.getTotalCount() + 1000
        val entities = questions.mapIndexed { idx, q ->
            q.copy(isCustom = true).toEntity(sortOrder = startOrder + idx)
        }
        dao.insertQuestions(entities)
    }

    suspend fun clearCustomQuestions() = withContext(Dispatchers.IO) {
        dao.deleteCustomQuestions()
    }

    suspend fun restoreBackup(jsonText: String): Int = withContext(Dispatchers.IO) {
        val restored = BulkQuestionParser.parseBackupJson(jsonText)
        if (restored.customQuestions.isNotEmpty()) {
            saveCustomQuestions(restored.customQuestions)
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
        accuracyPercent: Int
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
                timestamp = System.currentTimeMillis()
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
            CatDef("pyq", "PYQs (2015–2026)", "Tagged previous year shift papers", Color(0xFFD97706)) {
                it.question.category == "pyq" || it.question.shiftYear.isNotBlank()
            },
            CatDef("nur", "Nursing (70%)", "All 9 clinical nursing subjects", Color(0xFFE11D48)) {
                SubjectCatalog.isNursing(it.question)
            },
            CatDef("ca", "CA & Static GK", "2025–26 current affairs, railways & polity", Color(0xFF2563EB)) {
                SubjectCatalog.isCa(it.question)
            },
            CatDef("gen", "Sci / Math / Reas (30%)", "General science, arithmetic & reasoning", Color(0xFF0891B2)) {
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
