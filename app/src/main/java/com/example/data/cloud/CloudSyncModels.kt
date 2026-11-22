package com.example.data.cloud

import androidx.annotation.Keep
import com.example.data.local.MockHistoryEntity
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionSourceType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val lastDeviceHash: String = "",
    val displayName: String = "",
    val role: String = "",
    val targetYear: String = SettingsPreferences.DEFAULT_TARGET_YEAR,
    val focusSubject: String = "",
    val activeExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING,
    val themePreference: String = ThemePreference.LIGHT.name,
    val negativeMarking: Boolean = true,
    val useTimeLimit: Boolean = true,
    val secondsPerQuestion: Long = SettingsPreferences.DEFAULT_SECONDS_PER_QUESTION.toLong(),
    val keepScreenOn: Boolean = false,
    val onboardingCompleted: Boolean? = null,
    val dailyGoalQuestions: Long? = null,
    val currentStreakDays: Long = 0L,
    val longestStreakDays: Long = 0L,
    val lastGoalMetDate: String = "",
    val todayStudyDate: String = "",
    val todayAnsweredCount: Long = 0L,
    val phoneNumber: String = "",
    val activeSessionId: String = "",
    val activeDeviceName: String = "",
    @ServerTimestamp val sessionUpdatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudQuestionProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val bookmarked: Boolean = false,
    val attemptsCount: Long = 0L,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val lastOutcome: String? = null,
    val clientUpdatedAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudMockHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING,
    val totalQuestions: Long = 1L,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val skippedCount: Long = 0L,
    val netScore: Double = 0.0,
    val accuracyPercent: Long = 0L,
    val completedAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudCustomQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "nur",
    val shiftYear: String = "",
    val subject: String = "MSN",
    val difficulty: String = QuestionDifficulty.MEDIUM.label,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val sourceStatus: String = Question.SOURCE_UNVERIFIED,
    val sourceNote: String = "",
    val topicId: String = "",
    val sourceType: String = QuestionSourceType.PRACTICE.dbValue,
    val examYear: String = "",
    val examShift: String = "",
    val examIds: List<String> = listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudAccountSnapshot(
    val profile: CloudUserProfile?,
    val progress: List<CloudQuestionProgress>,
    val mockHistory: List<CloudMockHistory>,
    val customQuestions: List<CloudCustomQuestion>
)

enum class SyncBadgeState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class CloudSyncUiState(
    val status: SyncBadgeState = SyncBadgeState.IDLE,
    val lastSyncedAtMs: Long = 0L,
    val detailMessage: String = "Cloud backup ready"
)

data class SignedInAccountInfo(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)

object CloudMergeLogic {
    private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

    fun isValidFirestoreId(raw: String): Boolean =
        raw.isNotEmpty() && raw.length <= 128 && VALID_ID_REGEX.matches(raw)

    fun sanitizeFirestoreId(raw: String, fallbackPrefix: String = "doc"): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val nonEmpty = if (cleaned.isBlank()) "${fallbackPrefix}_${System.currentTimeMillis()}" else cleaned
        return nonEmpty.take(128)
    }

    fun deterministicMockDocId(completedAtMs: Long, examId: String): String {
        val safeExam = sanitizeFirestoreId(examId, "exam").take(40)
        return sanitizeFirestoreId("mock_${completedAtMs.coerceAtLeast(0L)}_$safeExam", "mock")
    }

    fun parseThemeSafe(raw: String?): ThemePreference {
        return ThemePreference.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: ThemePreference.LIGHT
    }

    fun mergeProgress(
        local: QuestionProgressEntity?,
        cloud: CloudQuestionProgress
    ): QuestionProgressEntity {
        val cloudAttempts = cloud.attemptsCount.toInt().coerceIn(0, 100000)
        val cloudCorrect = cloud.correctCount.toInt().coerceIn(0, 100000)
        val cloudWrong = cloud.wrongCount.toInt().coerceIn(0, 100000)
        val cloudOutcome = cloud.lastOutcome?.takeIf { it == "right" || it == "wrong" }
        val qId = cloud.questionId.ifBlank { cloud.id }

        if (local == null) {
            return QuestionProgressEntity(
                questionId = qId,
                isBookmarked = cloud.bookmarked,
                attemptsCount = cloudAttempts,
                correctCount = cloudCorrect,
                wrongCount = cloudWrong,
                lastOutcome = cloudOutcome,
                updatedAt = cloud.clientUpdatedAtMs.coerceAtLeast(0L)
            )
        }

        val cloudIsNewer = cloud.clientUpdatedAtMs > local.updatedAt
        val localIsNewer = local.updatedAt > cloud.clientUpdatedAtMs
        return when {
            cloudIsNewer -> QuestionProgressEntity(
                questionId = qId,
                isBookmarked = cloud.bookmarked,
                attemptsCount = maxOf(local.attemptsCount, cloudAttempts),
                correctCount = maxOf(local.correctCount, cloudCorrect),
                wrongCount = maxOf(local.wrongCount, cloudWrong),
                lastOutcome = cloudOutcome ?: local.lastOutcome,
                updatedAt = cloud.clientUpdatedAtMs
            )
            localIsNewer -> local
            else -> QuestionProgressEntity(
                questionId = qId,
                isBookmarked = local.isBookmarked || cloud.bookmarked,
                attemptsCount = maxOf(local.attemptsCount, cloudAttempts),
                correctCount = maxOf(local.correctCount, cloudCorrect),
                wrongCount = maxOf(local.wrongCount, cloudWrong),
                lastOutcome = local.lastOutcome ?: cloudOutcome,
                updatedAt = maxOf(local.updatedAt, cloud.clientUpdatedAtMs)
            )
        }
    }

    fun shouldPushLocalProgress(
        local: QuestionProgressEntity,
        cloud: CloudQuestionProgress?
    ): Boolean {
        if (cloud == null) {
            return local.isBookmarked || local.attemptsCount > 0
        }
        return local.updatedAt > cloud.clientUpdatedAtMs ||
            local.attemptsCount > cloud.attemptsCount.toInt() ||
            (local.updatedAt == cloud.clientUpdatedAtMs && local.isBookmarked != cloud.bookmarked)
    }

    fun toDomainQuestion(cloud: CloudCustomQuestion): Question? {
        val qId = cloud.id.trim()
        if (qId.isBlank() || cloud.questionText.isBlank() || cloud.options.size < 2) return null
        val safeCorrect = cloud.correctIndex.toInt().coerceIn(0, cloud.options.lastIndex)
        val cleanStatus = if (cloud.sourceStatus == Question.SOURCE_VERIFIED && cloud.sourceNote.isNotBlank()) {
            Question.SOURCE_VERIFIED
        } else {
            Question.SOURCE_UNVERIFIED
        }
        return Question(
            id = qId,
            category = cloud.category.ifBlank { "nur" },
            shiftYear = cloud.shiftYear,
            subject = cloud.subject.ifBlank { "MSN" },
            difficulty = QuestionDifficulty.fromRaw(cloud.difficulty),
            questionText = cloud.questionText,
            options = cloud.options,
            correctIndex = safeCorrect,
            explanation = cloud.explanation,
            isCustom = true,
            sourceStatus = cleanStatus,
            sourceNote = if (cleanStatus == Question.SOURCE_VERIFIED) cloud.sourceNote else "",
            topicId = cloud.topicId.ifBlank { PlatformCatalogDefaults.defaultTopicIdFor(cloud.subject.ifBlank { "MSN" }) },
            sourceType = QuestionSourceType.fromDbValue(cloud.sourceType) ?: QuestionSourceType.PRACTICE,
            examYear = cloud.examYear,
            examShift = cloud.examShift
        )
    }

    fun toLocalMockHistory(cloud: CloudMockHistory): MockHistoryEntity = MockHistoryEntity(
        title = cloud.title.ifBlank { "CBT Mock" },
        totalQuestions = cloud.totalQuestions.toInt().coerceAtLeast(1),
        correctCount = cloud.correctCount.toInt().coerceAtLeast(0),
        wrongCount = cloud.wrongCount.toInt().coerceAtLeast(0),
        skippedCount = cloud.skippedCount.toInt().coerceAtLeast(0),
        netScore = cloud.netScore,
        accuracyPercent = cloud.accuracyPercent.toInt().coerceIn(0, 100),
        timestamp = cloud.completedAtMs.coerceAtLeast(0L),
        examId = cloud.examId.ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }
    )
}
