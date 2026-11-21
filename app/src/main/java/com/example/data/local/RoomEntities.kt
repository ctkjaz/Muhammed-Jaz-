package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.data.model.Exam
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamSection
import com.example.data.model.MockHistory
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Profession
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.Topic
import com.example.data.model.composeShiftYear
import com.example.data.model.parseShiftYear
import org.json.JSONArray

@Entity(tableName = "professions")
data class ProfessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ProfessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["professionId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["professionId"])]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val professionId: String,
    val name: String,
    val shortName: String,
    val description: String,
    val defaultMockQuestions: Int,
    val defaultMockMinutes: Int,
    val positiveMarks: Double,
    val negativeMarks: Double,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(
    tableName = "exam_sections",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ExamSectionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val name: String,
    val weightagePercent: Int,
    val sortOrder: Int
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val code: String,
    val professionId: String?,
    val name: String,
    val accentHex: String,
    val sortOrder: Int
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["code"],
            childColumns = ["subjectCode"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["subjectCode"])]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectCode: String,
    val name: String,
    val sortOrder: Int
)

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
    val sortOrder: Int = 0,
    val sourceStatus: String = Question.SOURCE_UNVERIFIED,
    val sourceNote: String = "",
    @ColumnInfo(defaultValue = "")
    val topicId: String = "",
    @ColumnInfo(defaultValue = "practice")
    val sourceType: String = QuestionSourceType.PRACTICE.dbValue,
    @ColumnInfo(defaultValue = "")
    val examYear: String = "",
    @ColumnInfo(defaultValue = "")
    val examShift: String = ""
)

@Entity(
    tableName = "exam_question_links",
    primaryKeys = ["examId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ExamSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["examId", "sectionId"]),
        Index(value = ["questionId"]),
        Index(value = ["sectionId"])
    ]
)
data class ExamQuestionLinkEntity(
    val examId: String,
    val questionId: String,
    val sectionId: String,
    val sortOrder: Int,
    val isPyqForExam: Boolean,
    val examYear: String,
    val examShift: String
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
    val timestamp: Long,
    @ColumnInfo(defaultValue = "rrb_nursing")
    val examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
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
    val (fallbackYear, fallbackShift) = parseShiftYear(question.shiftYear)
    val resolvedYear = question.examYear.ifBlank { fallbackYear }
    val resolvedShift = question.examShift.ifBlank { fallbackShift }
    val resolvedShiftYear = question.shiftYear.ifBlank { composeShiftYear(resolvedYear, resolvedShift) }
    val resolvedSourceType = QuestionSourceType.infer(
        category = question.category,
        shiftYear = resolvedShiftYear,
        sourceStatus = question.sourceStatus,
        sourceNote = question.sourceNote,
        rawSourceType = question.sourceType
    )
    val resolvedTopicId = question.topicId.ifBlank {
        PlatformCatalogDefaults.defaultTopicIdFor(question.subject)
    }

    val q = Question(
        id = question.id,
        category = question.category,
        shiftYear = resolvedShiftYear,
        subject = question.subject,
        difficulty = QuestionDifficulty.fromRaw(question.difficulty),
        questionText = question.questionText,
        options = QuestionConverters.jsonToList(question.optionsJson),
        correctIndex = question.correctIndex,
        explanation = question.explanation,
        isCustom = question.isCustom,
        sourceStatus = question.sourceStatus,
        sourceNote = question.sourceNote,
        topicId = resolvedTopicId,
        sourceType = resolvedSourceType,
        examYear = resolvedYear,
        examShift = resolvedShift
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

fun Question.toEntity(sortOrder: Int = 0): QuestionEntity {
    val (parsedYear, parsedShift) = parseShiftYear(shiftYear)
    val finalYear = examYear.ifBlank { parsedYear }
    val finalShift = examShift.ifBlank { parsedShift }
    val finalShiftYear = shiftYear.ifBlank { composeShiftYear(finalYear, finalShift) }
    val finalSourceType = QuestionSourceType.infer(
        category = category,
        shiftYear = finalShiftYear,
        sourceStatus = sourceStatus,
        sourceNote = sourceNote,
        rawSourceType = sourceType.dbValue
    )
    return QuestionEntity(
        id = id,
        category = category,
        shiftYear = finalShiftYear,
        subject = subject,
        difficulty = difficulty.label,
        questionText = questionText,
        optionsJson = QuestionConverters.listToJson(options),
        correctIndex = correctIndex,
        explanation = explanation,
        isCustom = isCustom,
        sortOrder = sortOrder,
        sourceStatus = sourceStatus,
        sourceNote = sourceNote,
        topicId = topicId.ifBlank { PlatformCatalogDefaults.defaultTopicIdFor(subject) },
        sourceType = finalSourceType.dbValue,
        examYear = finalYear,
        examShift = finalShift
    )
}

fun MockHistoryEntity.toDomain(): MockHistory = MockHistory(
    id = id,
    title = title,
    totalQuestions = totalQuestions,
    correctCount = correctCount,
    wrongCount = wrongCount,
    skippedCount = skippedCount,
    netScore = netScore,
    accuracyPercent = accuracyPercent,
    timestamp = timestamp,
    examId = examId
)

fun ProfessionEntity.toDomain(): Profession = Profession(
    id = id,
    name = name,
    description = description,
    sortOrder = sortOrder,
    isActive = isActive
)

fun Profession.toEntity(): ProfessionEntity = ProfessionEntity(
    id = id,
    name = name,
    description = description,
    sortOrder = sortOrder,
    isActive = isActive
)

fun ExamEntity.toDomain(): Exam = Exam(
    id = id,
    professionId = professionId,
    name = name,
    shortName = shortName,
    description = description,
    defaultMockQuestions = defaultMockQuestions,
    defaultMockMinutes = defaultMockMinutes,
    positiveMarks = positiveMarks,
    negativeMarks = negativeMarks,
    sortOrder = sortOrder,
    isActive = isActive
)

fun Exam.toEntity(): ExamEntity = ExamEntity(
    id = id,
    professionId = professionId,
    name = name,
    shortName = shortName,
    description = description,
    defaultMockQuestions = defaultMockQuestions,
    defaultMockMinutes = defaultMockMinutes,
    positiveMarks = positiveMarks,
    negativeMarks = negativeMarks,
    sortOrder = sortOrder,
    isActive = isActive
)

fun ExamSectionEntity.toDomain(): ExamSection = ExamSection(
    id = id,
    examId = examId,
    name = name,
    weightagePercent = weightagePercent,
    sortOrder = sortOrder
)

fun ExamSection.toEntity(): ExamSectionEntity = ExamSectionEntity(
    id = id,
    examId = examId,
    name = name,
    weightagePercent = weightagePercent,
    sortOrder = sortOrder
)

fun TopicEntity.toDomain(): Topic = Topic(
    id = id,
    subjectCode = subjectCode,
    name = name,
    sortOrder = sortOrder
)

fun Topic.toEntity(): TopicEntity = TopicEntity(
    id = id,
    subjectCode = subjectCode,
    name = name,
    sortOrder = sortOrder
)

fun ExamQuestionLinkEntity.toDomain(): ExamQuestionLink = ExamQuestionLink(
    examId = examId,
    questionId = questionId,
    sectionId = sectionId,
    sortOrder = sortOrder,
    isPyqForExam = isPyqForExam,
    examYear = examYear,
    examShift = examShift
)

fun ExamQuestionLink.toEntity(): ExamQuestionLinkEntity = ExamQuestionLinkEntity(
    examId = examId,
    questionId = questionId,
    sectionId = sectionId,
    sortOrder = sortOrder,
    isPyqForExam = isPyqForExam,
    examYear = examYear,
    examShift = examShift
)
