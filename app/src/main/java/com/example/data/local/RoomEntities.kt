package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.data.model.MockHistory
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import org.json.JSONArray

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val category: String,
    val shiftYear: String,
    val subject: String,
    val difficulty: String = "Medium",
    val questionText: String,
    val optionsJson: String,
    val correctIndex: Int,
    val explanation: String,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "question_progress")
data class QuestionProgressEntity(
    @PrimaryKey val questionId: String,
    val isBookmarked: Boolean = false,
    val attemptsCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastOutcome: String? = null,
    val updatedAt: Long = 0L
)

@Entity(tableName = "mock_history")
data class MockHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val netScore: Double,
    val accuracyPercent: Int,
    val timestamp: Long
)

data class QuestionWithProgressRelation(
    @Embedded val question: QuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val progress: QuestionProgressEntity?
)

object QuestionConverters {
    @TypeConverter
    fun listToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun jsonToList(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { idx -> arr.optString(idx, "") }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

fun QuestionWithProgressRelation.toDomain(): QuestionItem {
    val q = Question(
        id = question.id,
        category = question.category,
        shiftYear = question.shiftYear,
        subject = question.subject,
        difficulty = QuestionDifficulty.fromRaw(question.difficulty),
        questionText = question.questionText,
        options = QuestionConverters.jsonToList(question.optionsJson),
        correctIndex = question.correctIndex,
        explanation = question.explanation,
        isCustom = question.isCustom
    )
    val p = progress?.let {
        QuestionProgress(
            questionId = it.questionId,
            isBookmarked = it.isBookmarked,
            attemptsCount = it.attemptsCount,
            correctCount = it.correctCount,
            wrongCount = it.wrongCount,
            lastOutcome = it.lastOutcome,
            updatedAt = it.updatedAt
        )
    } ?: QuestionProgress(questionId = question.id)
    return QuestionItem(question = q, progress = p)
}

fun Question.toEntity(sortOrder: Int = 0): QuestionEntity = QuestionEntity(
    id = id,
    category = category,
    shiftYear = shiftYear,
    subject = subject,
    difficulty = difficulty.label,
    questionText = questionText,
    optionsJson = QuestionConverters.listToJson(options),
    correctIndex = correctIndex,
    explanation = explanation,
    isCustom = isCustom,
    sortOrder = sortOrder
)

fun MockHistoryEntity.toDomain(): MockHistory = MockHistory(
    id = id,
    title = title,
    totalQuestions = totalQuestions,
    correctCount = correctCount,
    wrongCount = wrongCount,
    skippedCount = skippedCount,
    netScore = netScore,
    accuracyPercent = accuracyPercent,
    timestamp = timestamp
)
