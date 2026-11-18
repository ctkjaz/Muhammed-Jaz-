package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class QuestionDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    companion object {
        fun fromRaw(raw: String?): QuestionDifficulty = when (raw?.trim()?.lowercase()) {
            "easy" -> EASY
            "hard" -> HARD
            else -> MEDIUM
        }
    }
}

enum class DifficultyFilter(val label: String, val difficulty: QuestionDifficulty?) {
    ALL("All", null),
    EASY("Easy", QuestionDifficulty.EASY),
    MEDIUM("Medium", QuestionDifficulty.MEDIUM),
    HARD("Hard", QuestionDifficulty.HARD)
}

data class Question(
    val id: String,
    val category: String,      // "pyq" | "nur" | "ca" | "gen"
    val shiftYear: String,     // e.g. "2025-S1", "2026", ""
    val subject: String,       // e.g. "MSN", "Pharm", "Science", "CA-2026"
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val isCustom: Boolean = false
)

data class QuestionProgress(
    val questionId: String,
    val isBookmarked: Boolean = false,
    val attemptsCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastOutcome: String? = null, // "right" | "wrong" | null
    val updatedAt: Long = 0L
) {
    val accuracyPercent: Int
        get() = if (attemptsCount > 0) ((correctCount.toFloat() / attemptsCount) * 100f).toInt() else 0
}

data class QuestionItem(
    val question: Question,
    val progress: QuestionProgress
) {
    val id: String get() = question.id
    val isBookmarked: Boolean get() = progress.isBookmarked
    val lastOutcome: String? get() = progress.lastOutcome
    val attemptsCount: Int get() = progress.attemptsCount
    val correctCount: Int get() = progress.correctCount
    val wrongCount: Int get() = progress.wrongCount
    val accuracyPercent: Int get() = progress.accuracyPercent
}

data class Flashcard(
    val id: Int,
    val title: String,
    val tag: String,
    val body: String
)

data class MockHistory(
    val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val netScore: Double,
    val accuracyPercent: Int,
    val timestamp: Long
)

data class SubjectMeta(
    val code: String,
    val name: String,
    val accentColor: Color,
    val group: SubjectGroup
)

enum class SubjectGroup(val label: String) {
    NURSING("Nursing (70%)"),
    CA_GK("Current Affairs & GK"),
    NON_TECH("Sci / Math / Reasoning (30%)")
}

data class SubjectStat(
    val code: String,
    val name: String,
    val group: SubjectGroup,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val wrongAttempts: Int,
    val masteredQuestions: Int,
    val accentColor: Color
) {
    val accuracyPercent: Int
        get() = if (totalAttempts > 0) ((correctAttempts.toFloat() / totalAttempts) * 100f).toInt() else 0
}

data class CategoryStat(
    val key: String,
    val label: String,
    val description: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val wrongAttempts: Int,
    val accentColor: Color
) {
    val accuracyPercent: Int
        get() = if (totalAttempts > 0) ((correctAttempts.toFloat() / totalAttempts) * 100f).toInt() else 0
}

object SubjectCatalog {
    val NURSING_SUBJS = listOf(
        SubjectMeta("MSN", "Medical-Surgical", Color(0xFFE11D48), SubjectGroup.NURSING),
        SubjectMeta("Pharm", "Pharmacology", Color(0xFF7C3AED), SubjectGroup.NURSING),
        SubjectMeta("OBG", "OBG & Midwifery", Color(0xFFDB2777), SubjectGroup.NURSING),
        SubjectMeta("Peds", "Child Health (Peds)", Color(0xFFD97706), SubjectGroup.NURSING),
        SubjectMeta("CHN", "Community Health", Color(0xFF059669), SubjectGroup.NURSING),
        SubjectMeta("Psych", "Mental Health", Color(0xFF4F46E5), SubjectGroup.NURSING),
        SubjectMeta("FON", "Fundamentals (FON)", Color(0xFF0284C7), SubjectGroup.NURSING),
        SubjectMeta("Anat", "Anatomy & Physio", Color(0xFF0D9488), SubjectGroup.NURSING),
        SubjectMeta("Admin", "Admin & Research", Color(0xFF475569), SubjectGroup.NURSING)
    )

    val CA_SUBJS = listOf(
        SubjectMeta("CA-2026", "2025–26 National & Global", Color(0xFF2563EB), SubjectGroup.CA_GK),
        SubjectMeta("Railways", "Indian Railways & Budget", Color(0xFF4338CA), SubjectGroup.CA_GK),
        SubjectMeta("Health-Prog", "Health Drives & UIP", Color(0xFF059669), SubjectGroup.CA_GK),
        SubjectMeta("Static-GK", "Static GK & Polity", Color(0xFFD97706), SubjectGroup.CA_GK)
    )

    val GEN_SUBJS = listOf(
        SubjectMeta("Science", "General Science (NCERT)", Color(0xFF0891B2), SubjectGroup.NON_TECH),
        SubjectMeta("Math", "Arithmetic & Dosage", Color(0xFFEA580C), SubjectGroup.NON_TECH),
        SubjectMeta("Reasoning", "Logical Reasoning", Color(0xFF9333EA), SubjectGroup.NON_TECH)
    )

    val ALL_SUBJS = NURSING_SUBJS + CA_SUBJS + GEN_SUBJS

    private val nursingCodes = NURSING_SUBJS.map { it.code }.toSet()
    private val caCodes = CA_SUBJS.map { it.code }.toSet()
    private val genCodes = GEN_SUBJS.map { it.code }.toSet()

    fun isNursing(q: Question): Boolean = q.subject in nursingCodes
    fun isCa(q: Question): Boolean = q.subject in caCodes
    fun isGen(q: Question): Boolean = q.subject in genCodes

    fun displayNameFor(code: String): String =
        ALL_SUBJS.firstOrNull { it.code == code }?.name ?: code

    val SHIFT_NAMES = mapOf(
        "2025-S1" to "2025 Nursing Supt. — Shift 1",
        "2025-S2" to "2025 Nursing Supt. — Shift 2",
        "2025-S3" to "2025 Nursing Supt. — Shift 3",
        "2026-S1" to "2026 Memory-Based Set",
        "2019-S1" to "2019 RRB Staff Nurse — Shift 1",
        "2019-S2" to "2019 RRB Staff Nurse — Shift 2",
        "2015" to "2015 RRB Staff Nurse PYQ"
    )
}
