package com.example.data.model

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ClinEdge v2.8.0 — Exam Readiness Engine.
 *
 * Computes a transparent, 5-pillar composite readiness score (0..100) from:
 *  1. Question Accuracy (30% weight, confidence-adjusted by sample size)
 *  2. Syllabus Coverage (25% weight, unique questions + subject breadth)
 *  3. CBT Mock Performance (20% weight, recent full-length mock averages)
 *  4. Mistake Mastery / SRS (15% weight, Leitner box progression via [SrsStage])
 *  5. Study Consistency (10% weight, active daily streak & today's goal)
 */
enum class ReadinessBand(
    val label: String,
    val guidance: String
) {
    EXAM_READY(
        label = "Exam Ready",
        guidance = "Strong accuracy, breadth, and CBT discipline — keep sharpening speed with timed PYQs and SRS drills."
    ),
    ON_TRACK(
        label = "On Track",
        guidance = "Solid trajectory — push your weakest subject above 75% and complete regular full-length CBT mocks."
    ),
    BUILDING(
        label = "Building Momentum",
        guidance = "Good early progress — expand subject coverage and clear due mistakes in your Spaced Repetition Notebook."
    ),
    NEEDS_FOCUS(
        label = "Getting Started",
        guidance = "Complete a 25Q daily drill and one baseline CBT mock to unlock an accurate readiness forecast."
    )
}

data class ReadinessPillar(
    val id: String,
    val label: String,
    val scorePercent: Int,
    val weightPercent: Int,
    val detail: String
)

data class WeakSubjectAction(
    val subjectCode: String,
    val subjectName: String,
    val accuracyPercent: Int,
    val attemptedQuestions: Int,
    val totalQuestions: Int
)

data class ExamReadinessSummary(
    val overallScore: Int,
    val band: ReadinessBand,
    val pillars: List<ReadinessPillar>,
    val weakestSubject: WeakSubjectAction?,
    val nextBestAction: String,
    val estimatedRankTier: String,
    val hasEnoughData: Boolean
)

object ExamReadinessEngine {

    private const val ACCURACY_WEIGHT = 30
    private const val COVERAGE_WEIGHT = 25
    private const val MOCK_WEIGHT = 20
    private const val SRS_WEIGHT = 15
    private const val CONSISTENCY_WEIGHT = 10

    private const val ACCURACY_CONFIDENCE_FLOOR = 40.0
    private const val COVERAGE_TARGET_QUESTIONS = 300

    fun compute(
        totalQuestions: Int,
        attemptedUnique: Int,
        totalAttempts: Int,
        accuracyPercent: Int,
        subjectStats: List<SubjectStat>,
        mockHistory: List<MockHistory>,
        srsCards: Map<String, SrsCardState>,
        currentStreakDays: Int,
        dailyGoalMetToday: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): ExamReadinessSummary {
        val safeTotalQ = totalQuestions.coerceAtLeast(0)
        val safeAttemptedUnique = attemptedUnique.coerceIn(0, safeTotalQ.coerceAtLeast(attemptedUnique.coerceAtLeast(0)))
        val safeAttempts = totalAttempts.coerceAtLeast(0)
        val safeAccuracy = accuracyPercent.coerceIn(0, 100)
        val safeStreak = currentStreakDays.coerceAtLeast(0)

        // 1. Accuracy Pillar (30%): confidence-weighted so 2/2 correct doesn't inflate readiness
        val accuracyConfidence = min(1.0, safeAttempts / ACCURACY_CONFIDENCE_FLOOR)
        val accuracyScore = (safeAccuracy * accuracyConfidence).roundToInt().coerceIn(0, 100)
        val accuracyDetail = if (safeAttempts == 0) {
            "No questions attempted yet"
        } else if (safeAttempts < ACCURACY_CONFIDENCE_FLOOR.toInt()) {
            "$safeAccuracy% across $safeAttempts attempts (building sample)"
        } else {
            "$safeAccuracy% lifetime accuracy across $safeAttempts attempts"
        }

        // 2. Syllabus Coverage Pillar (25%): 70% unique-question volume + 30% subject breadth
        val coverageTarget = if (safeTotalQ in 1 until COVERAGE_TARGET_QUESTIONS) {
            safeTotalQ
        } else {
            COVERAGE_TARGET_QUESTIONS
        }
        val volumeRatio = if (coverageTarget > 0) {
            min(1.0, safeAttemptedUnique.toDouble() / coverageTarget.toDouble())
        } else {
            0.0
        }
        val activeSubjects = subjectStats.filter { it.totalQuestions > 0 }
        val touchedSubjects = activeSubjects.count { it.attemptedQuestions > 0 }
        val breadthRatio = if (activeSubjects.isNotEmpty()) {
            touchedSubjects.toDouble() / activeSubjects.size.toDouble()
        } else {
            volumeRatio
        }
        val coverageScore = ((volumeRatio * 0.70 + breadthRatio * 0.30) * 100.0).roundToInt().coerceIn(0, 100)
        val coverageDetail = if (activeSubjects.isNotEmpty()) {
            "$safeAttemptedUnique Qs solved · $touchedSubjects/${activeSubjects.size} subjects touched"
        } else {
            "$safeAttemptedUnique unique questions solved"
        }

        // 3. CBT Mock Performance Pillar (20%): latest up to 5 mocks, scaled smoothly for 1..3+ mocks
        val recentMocks = mockHistory.sortedByDescending { it.timestamp }.take(5)
        val mockScore = if (recentMocks.isEmpty()) {
            0
        } else {
            val avgMockAcc = recentMocks.map { it.accuracyPercent.coerceIn(0, 100) }.average()
            val mockVolumeFactor = when (recentMocks.size) {
                1 -> 0.70
                2 -> 0.85
                else -> 1.0
            }
            (avgMockAcc * mockVolumeFactor).roundToInt().coerceIn(0, 100)
        }
        val mockDetail = if (recentMocks.isEmpty()) {
            "Complete a full-length CBT mock"
        } else {
            val avg = recentMocks.map { it.accuracyPercent.coerceIn(0, 100) }.average().roundToInt()
            "${recentMocks.size} recent mock${if (recentMocks.size == 1) "" else "s"} · $avg% avg"
        }

        // 4. Mistake Mastery / SRS Pillar (15%): strictly uses SrsStage enum (BOX_1D..MASTERED)
        val srsScore: Int
        val srsDetail: String
        if (srsCards.isEmpty()) {
            // No mistakes logged yet: neutral baseline proportional to practice volume
            srsScore = if (safeAttempts >= 20 && safeAccuracy >= 75) 80 else if (safeAttempts > 0) 50 else 0
            srsDetail = if (safeAttempts > 0) "No pending SRS mistakes" else "Mistake cards appear after drills"
        } else {
            val cards = srsCards.values
            val masteredCount = cards.count { it.stage == SrsStage.MASTERED }
            val advancedCount = cards.count {
                it.stage == SrsStage.BOX_7D || it.stage == SrsStage.BOX_14D || it.stage == SrsStage.MASTERED
            }
            val dueNowCount = cards.count { SpacedRepetitionLogic.isDue(it, nowMs) }
            val maxBoxIndex = SrsStage.MASTERED.boxIndex.coerceAtLeast(1).toDouble()
            val avgBoxProgress = cards.map { it.stage.boxIndex.toDouble() / maxBoxIndex }.average()
            val duePenalty = (dueNowCount.toDouble() / cards.size.toDouble()) * 25.0
            srsScore = ((avgBoxProgress * 100.0) - duePenalty).roundToInt().coerceIn(0, 100)
            srsDetail = "$masteredCount/${cards.size} mastered · $advancedCount in 7d+ · $dueNowCount due"
        }

        // 5. Study Consistency Pillar (10%): 7-day streak + daily goal bonus
        val streakRatio = min(1.0, safeStreak / 7.0)
        val goalBonus = if (dailyGoalMetToday) 20.0 else 0.0
        val consistencyScore = ((streakRatio * 80.0) + goalBonus).roundToInt().coerceIn(0, 100)
        val consistencyDetail = when {
            safeStreak > 0 && dailyGoalMetToday -> "${safeStreak}d streak · Today's goal met"
            safeStreak > 0 -> "${safeStreak}d active streak"
            dailyGoalMetToday -> "Today's study goal complete"
            else -> "Hit your daily goal to start a streak"
        }

        val pillars = listOf(
            ReadinessPillar(
                id = "accuracy",
                label = "Accuracy",
                scorePercent = accuracyScore,
                weightPercent = ACCURACY_WEIGHT,
                detail = accuracyDetail
            ),
            ReadinessPillar(
                id = "coverage",
                label = "Syllabus Coverage",
                scorePercent = coverageScore,
                weightPercent = COVERAGE_WEIGHT,
                detail = coverageDetail
            ),
            ReadinessPillar(
                id = "mocks",
                label = "CBT Mocks",
                scorePercent = mockScore,
                weightPercent = MOCK_WEIGHT,
                detail = mockDetail
            ),
            ReadinessPillar(
                id = "srs",
                label = "Mistake Mastery",
                scorePercent = srsScore,
                weightPercent = SRS_WEIGHT,
                detail = srsDetail
            ),
            ReadinessPillar(
                id = "consistency",
                label = "Consistency",
                scorePercent = consistencyScore,
                weightPercent = CONSISTENCY_WEIGHT,
                detail = consistencyDetail
            )
        )

        val weightedSum = pillars.sumOf { it.scorePercent * it.weightPercent }
        val totalWeight = pillars.sumOf { it.weightPercent }.coerceAtLeast(1)
        val overallScore = (weightedSum.toDouble() / totalWeight.toDouble()).roundToInt().coerceIn(0, 100)

        val band = when {
            overallScore >= 80 -> ReadinessBand.EXAM_READY
            overallScore >= 60 -> ReadinessBand.ON_TRACK
            overallScore >= 35 -> ReadinessBand.BUILDING
            else -> ReadinessBand.NEEDS_FOCUS
        }

        val estimatedRankTier = when {
            overallScore >= 85 -> "Top 5% Selection Zone"
            overallScore >= 72 -> "Top 15% Merit Contender"
            overallScore >= 55 -> "Top 35% Cutoff Watch"
            overallScore >= 35 -> "Foundation Builder"
            else -> "Baseline Needed"
        }

        // Identify weakest actionable subject (prefer attempted subjects with lowest accuracy, else largest untouched nursing subject)
        val attemptedSubjects = activeSubjects.filter { it.totalAttempts > 0 }
        val weakestStat = attemptedSubjects
            .minWithOrNull(compareBy<SubjectStat> { it.accuracyPercent }.thenByDescending { it.wrongAttempts })
            ?: activeSubjects.maxByOrNull { it.totalQuestions }

        val weakestSubject = weakestStat?.let {
            WeakSubjectAction(
                subjectCode = it.code,
                subjectName = it.name,
                accuracyPercent = it.accuracyPercent,
                attemptedQuestions = it.attemptedQuestions,
                totalQuestions = it.totalQuestions
            )
        }

        val dueSrsCount = srsCards.values.count { SpacedRepetitionLogic.isDue(it, nowMs) }
        val nextBestAction = when {
            safeAttempts < 10 -> "Solve your first 25Q daily drill to calibrate your readiness score."
            dueSrsCount >= 5 -> "Clear $dueSrsCount due cards in your Mistake Notebook to boost retention."
            recentMocks.isEmpty() -> "Attempt 1 full-length CBT mock test to benchmark exam speed."
            weakestSubject != null && weakestSubject.attemptedQuestions > 0 && weakestSubject.accuracyPercent < 65 ->
                "Drill ${weakestSubject.subjectName} (${weakestSubject.accuracyPercent}% accuracy) to lift your score."
            !dailyGoalMetToday -> "Finish today's daily study goal to protect your consistency pillar."
            else -> "Take a timed PYQ or Image-Based Spotter drill to lock in selection-zone speed."
        }

        return ExamReadinessSummary(
            overallScore = overallScore,
            band = band,
            pillars = pillars,
            weakestSubject = weakestSubject,
            nextBestAction = nextBestAction,
            estimatedRankTier = estimatedRankTier,
            hasEnoughData = safeAttempts >= 10
        )
    }
}
