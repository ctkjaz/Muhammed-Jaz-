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

enum class QuestionSourceType(val dbValue: String, val label: String) {
    OFFICIAL_PYQ("official_pyq", "Official PYQ"),
    MEMORY_BASED("memory_based", "Memory-Based PYQ"),
    PRACTICE("practice", "Practice"),
    CURRENT_AFFAIRS("current_affairs", "Current Affairs");

    companion object {
        fun fromDbValue(raw: String?): QuestionSourceType? = when (raw?.trim()?.lowercase()) {
            "official_pyq" -> OFFICIAL_PYQ
            "memory_based", "reconstructed" -> MEMORY_BASED
            "practice" -> PRACTICE
            "current_affairs", "ca" -> CURRENT_AFFAIRS
            else -> null
        }

        fun infer(
            category: String,
            shiftYear: String,
            sourceStatus: String,
            sourceNote: String,
            rawSourceType: String? = null
        ): QuestionSourceType {
            val isVerified = sourceStatus == Question.SOURCE_VERIFIED && sourceNote.isNotBlank()
            val explicit = fromDbValue(rawSourceType)
            if (explicit != null) {
                return if (explicit == OFFICIAL_PYQ && !isVerified) MEMORY_BASED else explicit
            }
            return when {
                category == "pyq" || shiftYear.isNotBlank() ->
                    if (isVerified) OFFICIAL_PYQ else MEMORY_BASED
                category == "ca" -> CURRENT_AFFAIRS
                else -> PRACTICE
            }
        }
    }
}

fun parseShiftYear(shiftYear: String): Pair<String, String> {
    val trimmed = shiftYear.trim()
    if (trimmed.isEmpty()) return "" to ""
    val dash = trimmed.indexOf('-')
    return if (dash > 0) {
        trimmed.substring(0, dash).trim() to trimmed.substring(dash + 1).trim()
    } else {
        trimmed to ""
    }
}

fun composeShiftYear(examYear: String, examShift: String): String {
    val y = examYear.trim()
    val s = examShift.trim()
    return when {
        y.isNotEmpty() && s.isNotEmpty() -> "$y-$s"
        y.isNotEmpty() -> y
        else -> ""
    }
}

data class Profession(
    val id: String,
    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class Exam(
    val id: String,
    val professionId: String,
    val name: String,
    val shortName: String,
    val description: String = "",
    val defaultMockQuestions: Int = 100,
    val defaultMockMinutes: Int = 90,
    val positiveMarks: Double = 1.0,
    val negativeMarks: Double = 0.33,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class ExamSection(
    val id: String,
    val examId: String,
    val name: String,
    val weightagePercent: Int = 0,
    val sortOrder: Int = 0
)

data class Topic(
    val id: String,
    val subjectCode: String,
    val name: String,
    val sortOrder: Int = 0
)

data class ExamQuestionLink(
    val examId: String,
    val questionId: String,
    val sectionId: String,
    val sortOrder: Int = 0,
    val isPyqForExam: Boolean = false,
    val examYear: String = "",
    val examShift: String = ""
)

object PlatformCatalogDefaults {
    const val PROFESSION_NURSING = "nursing"
    const val EXAM_RRB_NURSING = "rrb_nursing"

    const val SECTION_RRB_NURSING_CORE = "rrb_nursing_core"
    const val SECTION_RRB_CA_GK = "rrb_nursing_ca_gk"
    const val SECTION_RRB_NON_TECH = "rrb_nursing_nontech"

    val DEFAULT_PROFESSIONS = listOf(
        Profession(
            id = PROFESSION_NURSING,
            name = "Nursing",
            description = "Central & State Nursing Officer, Staff Nurse, and Nursing Superintendent exams",
            sortOrder = 0,
            isActive = true
        )
    )

    val DEFAULT_EXAMS = listOf(
        Exam(
            id = EXAM_RRB_NURSING,
            professionId = PROFESSION_NURSING,
            name = "RRB Nursing Superintendent & Staff Nurse",
            shortName = "RRB Nursing",
            description = "100-question CBT pattern (70% Nursing, 30% Non-Tech & CA/GK) • 90 min • +1 / -0.33",
            defaultMockQuestions = 100,
            defaultMockMinutes = 90,
            positiveMarks = 1.0,
            negativeMarks = 0.33,
            sortOrder = 0,
            isActive = true
        )
    )

    val DEFAULT_SECTIONS = listOf(
        ExamSection(
            id = SECTION_RRB_NURSING_CORE,
            examId = EXAM_RRB_NURSING,
            name = "Nursing Core (70%)",
            weightagePercent = 70,
            sortOrder = 0
        ),
        ExamSection(
            id = SECTION_RRB_CA_GK,
            examId = EXAM_RRB_NURSING,
            name = "Current Affairs & Static GK",
            weightagePercent = 10,
            sortOrder = 1
        ),
        ExamSection(
            id = SECTION_RRB_NON_TECH,
            examId = EXAM_RRB_NURSING,
            name = "Science, Arithmetic & Reasoning (30%)",
            weightagePercent = 20,
            sortOrder = 2
        )
    )

    fun defaultTopicIdFor(subjectCode: String): String {
        val clean = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return if (clean.isEmpty()) "general" else "${clean}_general"
    }

    fun sectionIdForRrbSubject(subjectCode: String): String = when {
        SubjectCatalog.isNursingCode(subjectCode) -> SECTION_RRB_NURSING_CORE
        SubjectCatalog.isCaCode(subjectCode) -> SECTION_RRB_CA_GK
        else -> SECTION_RRB_NON_TECH
    }
}

data class Question(
    val id: String,
    val category: String,      // Legacy: "pyq" | "nur" | "ca" | "gen"
    val shiftYear: String,     // Legacy combined tag: e.g. "2025-S1", "2015", ""
    val subject: String,       // Matches subjects.code: e.g. "MSN", "Pharm", "Science"
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val isCustom: Boolean = false,
    val sourceStatus: String = SOURCE_UNVERIFIED, // "unverified" | "verified"
    val sourceNote: String = "",
    val topicId: String = PlatformCatalogDefaults.defaultTopicIdFor(subject),
    val sourceType: QuestionSourceType = QuestionSourceType.infer(category, shiftYear, sourceStatus, sourceNote),
    val examYear: String = parseShiftYear(shiftYear).first,
    val examShift: String = parseShiftYear(shiftYear).second
) {
    val isPyqTagged: Boolean
        get() = sourceType == QuestionSourceType.OFFICIAL_PYQ ||
            sourceType == QuestionSourceType.MEMORY_BASED ||
            category == "pyq" ||
            shiftYear.isNotBlank() ||
            examYear.isNotBlank()

    val isVerifiedSource: Boolean
        get() = sourceStatus == SOURCE_VERIFIED && sourceNote.isNotBlank()

    val effectiveShiftYear: String
        get() = shiftYear.ifBlank { composeShiftYear(examYear, examShift) }

    fun pyqBadgeText(): String? {
        if (!isPyqTagged) return null
        val sy = effectiveShiftYear
        return if (isVerifiedSource) {
            if (sy.isNotBlank()) "Verified PYQ • $sy" else "Verified PYQ"
        } else {
            "Unverified PYQ tag"
        }
    }

    fun sourceTypeBadgeText(): String {
        val sy = effectiveShiftYear
        return when (sourceType) {
            QuestionSourceType.OFFICIAL_PYQ ->
                if (isVerifiedSource && sy.isNotBlank()) "Official PYQ • $sy"
                else if (isVerifiedSource) "Official PYQ"
                else "Memory-Based PYQ"
            QuestionSourceType.MEMORY_BASED -> "Memory-Based PYQ"
            QuestionSourceType.CURRENT_AFFAIRS -> "Current Affairs"
            QuestionSourceType.PRACTICE -> "Practice"
        }
    }

    companion object {
        const val SOURCE_UNVERIFIED = "unverified"
        const val SOURCE_VERIFIED = "verified"
    }
}

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
    val timestamp: Long,
    val examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
)

data class SubjectMeta(
    val code: String,
    val name: String,
    val accentColor: Color,
    val group: SubjectGroup,
    val accentHex: String = "#726D70"
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
        SubjectMeta("MSN", "Medical-Surgical", Color(0xFF8E4456), SubjectGroup.NURSING, "#8E4456"),
        SubjectMeta("Pharm", "Pharmacology", Color(0xFF756091), SubjectGroup.NURSING, "#756091"),
        SubjectMeta("OBG", "OBG & Midwifery", Color(0xFFB78B79), SubjectGroup.NURSING, "#B78B79"),
        SubjectMeta("Peds", "Child Health (Peds)", Color(0xFFA67942), SubjectGroup.NURSING, "#A67942"),
        SubjectMeta("CHN", "Community Health", Color(0xFF478271), SubjectGroup.NURSING, "#478271"),
        SubjectMeta("Psych", "Mental Health", Color(0xFF666B8A), SubjectGroup.NURSING, "#666B8A"),
        SubjectMeta("FON", "Fundamentals (FON)", Color(0xFF537B8A), SubjectGroup.NURSING, "#537B8A"),
        SubjectMeta("Anat", "Anatomy & Physio", Color(0xFF5B8078), SubjectGroup.NURSING, "#5B8078"),
        SubjectMeta("Admin", "Admin & Research", Color(0xFF726D70), SubjectGroup.NURSING, "#726D70")
    )

    val CA_SUBJS = listOf(
        SubjectMeta("CA-2026", "2025–26 National & Global", Color(0xFF7D5360), SubjectGroup.CA_GK, "#7D5360"),
        SubjectMeta("Railways", "Indian Railways & Budget", Color(0xFF6A637D), SubjectGroup.CA_GK, "#6A637D"),
        SubjectMeta("Health-Prog", "Health Drives & UIP", Color(0xFF4E8570), SubjectGroup.CA_GK, "#4E8570"),
        SubjectMeta("Static-GK", "Static GK & Polity", Color(0xFF9F7A4D), SubjectGroup.CA_GK, "#9F7A4D")
    )

    val GEN_SUBJS = listOf(
        SubjectMeta("Science", "General Science (NCERT)", Color(0xFF527D86), SubjectGroup.NON_TECH, "#527D86"),
        SubjectMeta("Math", "Arithmetic & Dosage", Color(0xFFA96D54), SubjectGroup.NON_TECH, "#A96D54"),
        SubjectMeta("Reasoning", "Logical Reasoning", Color(0xFF7E6189), SubjectGroup.NON_TECH, "#7E6189")
    )

    val ALL_SUBJS = NURSING_SUBJS + CA_SUBJS + GEN_SUBJS

    private val nursingCodes = NURSING_SUBJS.map { it.code }.toSet()
    private val caCodes = CA_SUBJS.map { it.code }.toSet()
    private val genCodes = GEN_SUBJS.map { it.code }.toSet()

    fun isNursingCode(code: String): Boolean = code in nursingCodes
    fun isCaCode(code: String): Boolean = code in caCodes
    fun isGenCode(code: String): Boolean = code in genCodes

    fun isNursing(q: Question): Boolean = q.subject in nursingCodes
    fun isCa(q: Question): Boolean = q.subject in caCodes
    fun isGen(q: Question): Boolean = q.subject in genCodes

    fun displayNameFor(code: String): String =
        ALL_SUBJS.firstOrNull { it.code == code }?.name ?: code

    val SHIFT_NAMES = mapOf(
        "2025-S1" to "2025 Nursing Supt. — Shift 1",
        "2025-S2" to "2025 Nursing Supt. — Shift 2",
        "2025-S3" to "2025 Nursing Supt. — Shift 3",
        "2026-S1" to "2026 RRB Staff Nurse — Shift 1",
        "2026-S2" to "2026 RRB Staff Nurse — Shift 2",
        "2026-S3" to "2026 RRB Staff Nurse — Shift 3",
        "2019-S1" to "2019 RRB Staff Nurse — Shift 1",
        "2019-S2" to "2019 RRB Staff Nurse — Shift 2",
        "2019-S3" to "2019 RRB Staff Nurse — Shift 3",
        "2015" to "2015 RRB Staff Nurse PYQ"
    )
}
