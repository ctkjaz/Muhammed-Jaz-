package com.example.data.model

import kotlin.math.roundToInt

enum class ReadinessTier(val label: String, val guidance: String) {
    EXAM_READY(
        "Exam Ready",
        "Maintain accuracy with timed full mocks and SRS mistake maintenance."
    ),
    STRONG_TRACK(
        "Strong Track",
        "Push weak subjects above 75% accuracy and close overdue SRS cards."
    ),
    BUILDING_MOMENTUM(
        "Building Momentum",
        "Cover remaining subjects and complete at least 2 full-length CBT mocks."
    ),
    GETTING_STARTED(
        "Getting Started",
        "Complete your daily target drills across core nursing subjects to calibrate readiness."
    )
}

data class ExamReadinessSummary(
    val overallScore: Int,         // 0..100
    val accuracyScore: Int,        // 0..100 (40% weight)
    val coverageScore: Int,        // 0..100 (25% weight)
    val mockConsistencyScore: Int, // 0..100 (20% weight)
    val mistakeMasteryScore: Int,  // 0..100 (15% weight)
    val subjectsPractised: Int,
    val totalSubjects: Int,
    val mocksCompleted: Int,
    val tier: ReadinessTier,
    val nextAction: String
)

/**
 * Pure-JVM Exam Readiness Index calculator derived strictly from live student telemetry.
 */
object ExamReadinessLogic {

    fun compute(
        overallAccuracyPercent: Int,
        attemptedUniqueQuestions: Int,
        subjectStats: List<SubjectStat>,
        mockHistory: List<MockHistory>,
        srsCards: Map<String, SrsCardState>,
        nowMs: Long = System.currentTimeMillis()
    ): ExamReadinessSummary {
        val totalSubjs = subjectStats.count { it.totalQuestions > 0 }.coerceAtLeast(1)
        val practisedSubjs = subjectStats.count { it.attemptedQuestions > 0 }

        // 1. Accuracy (40% weight) — dampened lightly if fewer than 20 questions attempted so 1/1 != 100% ready
        val sampleConfidence = (attemptedUniqueQuestions.coerceAtMost(25) / 25.0)
        val accuracyScore = (overallAccuracyPercent.coerceIn(0, 100) * sampleConfidence).roundToInt()

        // 2. Syllabus Coverage (25% weight) — breadth across subjects + meaningful volume
        val subjectBreadth = (practisedSubjs.toDouble() / totalSubjs.toDouble()).coerceIn(0.0, 1.0)
        val volumeFactor = (attemptedUniqueQuestions.coerceAtMost(150) / 150.0).coerceIn(0.0, 1.0)
        val coverageScore = ((subjectBreadth * 0.65 + volumeFactor * 0.35) * 100.0).roundToInt().coerceIn(0, 100)

        // 3. Mock Consistency (20% weight) — number of mocks completed (up to 4) + recent mock accuracy
        val mocksCompleted = mockHistory.size
        val mockConsistencyScore = if (mocksCompleted == 0) {
            0
        } else {
            val recentAvgAcc = mockHistory.take(3).map { it.accuracyPercent.coerceIn(0, 100) }.average()
            val mockCountScore = (mocksCompleted.coerceAtMost(4) / 4.0) * 100.0
            (mockCountScore * 0.45 + recentAvgAcc * 0.55).roundToInt().coerceIn(0, 100)
        }

        // 4. Mistake Mastery (15% weight) — SRS cards not overdue or mastered
        val mistakeMasteryScore = when {
            attemptedUniqueQuestions == 0 -> 0
            srsCards.isEmpty() -> if (overallAccuracyPercent >= 75) 85 else 50
            else -> {
                val masteredOrNotDue = srsCards.values.count { card ->
                    card.stage >= 2 || card.nextReviewAtMs > nowMs
                }
                ((masteredOrNotDue.toDouble() / srsCards.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            }
        }

        val weighted = (
            accuracyScore * 0.40 +
                coverageScore * 0.25 +
                mockConsistencyScore * 0.20 +
                mistakeMasteryScore * 0.15
            ).roundToInt().coerceIn(0, 100)

        val tier = when {
            weighted >= 80 && attemptedUniqueQuestions >= 50 -> ReadinessTier.EXAM_READY
            weighted >= 60 -> ReadinessTier.STRONG_TRACK
            weighted >= 30 -> ReadinessTier.BUILDING_MOMENTUM
            else -> ReadinessTier.GETTING_STARTED
        }

        val dueSrsCount = srsCards.values.count { it.nextReviewAtMs <= nowMs }
        val nextAction = when {
            attemptedUniqueQuestions < 20 -> "Solve ${20 - attemptedUniqueQuestions} more MCQs to calibrate your Readiness Index."
            dueSrsCount >= 5 -> "Clear $dueSrsCount overdue SRS Mistake Notebook cards to lift Mistake Mastery."
            mocksCompleted == 0 -> "Attempt your first timed CBT Mock to unlock Mock Consistency points."
            practisedSubjs < totalSubjs -> "Explore ${totalSubjs - practisedSubjs} untouched subject(s) to boost Syllabus Coverage."
            overallAccuracyPercent < 75 -> "Run a Smart Weak-Topic Drill to lift overall accuracy above 75%."
            else -> "Great consistency! Take a full timed mock every 2–3 days to stay sharp."
        }

        return ExamReadinessSummary(
            overallScore = weighted,
            accuracyScore = accuracyScore,
            coverageScore = coverageScore,
            mockConsistencyScore = mockConsistencyScore,
            mistakeMasteryScore = mistakeMasteryScore,
            subjectsPractised = practisedSubjs,
            totalSubjects = totalSubjs,
            mocksCompleted = mocksCompleted,
            tier = tier,
            nextAction = nextAction
        )
    }
}

/**
 * Pure-JVM persistence & sanitization helper for per-question personal study notes.
 * Encoded as a compact newline-safe delimited string so it works cleanly in pure JVM unit tests
 * without requiring Android's `org.json` stub.
 */
object QuestionNotesLogic {
    const val MAX_NOTE_LENGTH = 400
    const val MAX_STORED_NOTES = 500
    private const val ENTRY_SEP = "\u001E"
    private const val KV_SEP = "\u001F"

    fun sanitizeNote(raw: String): String =
        raw.replace(ENTRY_SEP, " ")
            .replace(KV_SEP, " ")
            .trim()
            .take(MAX_NOTE_LENGTH)

    fun upsertNote(
        existing: Map<String, String>,
        questionId: String,
        rawNote: String
    ): Map<String, String> {
        val cleanId = questionId.trim()
        if (cleanId.isEmpty()) return existing
        val cleanNote = sanitizeNote(rawNote)
        val mutable = LinkedHashMap(existing)
        if (cleanNote.isEmpty()) {
            mutable.remove(cleanId)
        } else {
            // Move to end for recency preservation and enforce bounded storage
            mutable.remove(cleanId)
            mutable[cleanId] = cleanNote
            while (mutable.size > MAX_STORED_NOTES) {
                val oldestKey = mutable.keys.firstOrNull() ?: break
                mutable.remove(oldestKey)
            }
        }
        return mutable
    }

    fun serialize(notes: Map<String, String>): String {
        if (notes.isEmpty()) return ""
        return notes.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString(ENTRY_SEP) { (id, note) ->
                "${id.trim()}$KV_SEP${sanitizeNote(note)}"
            }
    }

    fun deserialize(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        raw.split(ENTRY_SEP).forEach { entry ->
            val sepIdx = entry.indexOf(KV_SEP)
            if (sepIdx > 0 && sepIdx < entry.lastIndex) {
                val id = entry.substring(0, sepIdx).trim()
                val note = sanitizeNote(entry.substring(sepIdx + 1))
                if (id.isNotEmpty() && note.isNotEmpty()) {
                    result[id] = note
                }
            }
        }
        return result
    }
}
