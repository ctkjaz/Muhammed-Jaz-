package com.example.data.model

import kotlin.math.roundToInt

enum class TopicWeaknessSeverity(val label: String) {
    CRITICAL("Critical"),
    NEEDS_WORK("Needs Work"),
    UNTOUCHED("Untouched"),
    STRONG("Strong")
}

data class WeakSubjectDiagnostic(
    val subjectCode: String,
    val subjectLabel: String,
    val attempted: Int,
    val correct: Int,
    val accuracyPercent: Int,
    val dueSrsCount: Int,
    val totalAvailable: Int,
    val severity: TopicWeaknessSeverity,
    val priorityScore: Double
)

data class RecoveryDayTask(
    val dayNumber: Int,
    val dayLabel: String,
    val subjectCode: String,
    val subjectLabel: String,
    val recommendedMcqs: Int,
    val srsReviewCount: Int,
    val focusReason: String
)

data class SmartStudyPlanSummary(
    val diagnostics: List<WeakSubjectDiagnostic>,
    val topWeakSubjects: List<WeakSubjectDiagnostic>,
    val sevenDayPlan: List<RecoveryDayTask>,
    val headlineReason: String
)

/**
 * Pure-JVM deterministic planner that diagnoses a student's weakest nursing subjects
 * from real question attempt telemetry + due Spaced Repetition (SRS) mistakes,
 * builds a tailored 7-day recovery schedule, and assembles weighted smart auto-drills.
 */
object SmartWeakTopicPlanner {

    fun buildPlan(
        questions: List<QuestionItem>,
        srsCards: Map<String, SrsCardState>,
        dailyGoalQuestions: Int = 30,
        nowMs: Long = System.currentTimeMillis()
    ): SmartStudyPlanSummary {
        if (questions.isEmpty()) {
            return SmartStudyPlanSummary(
                diagnostics = emptyList(),
                topWeakSubjects = emptyList(),
                sevenDayPlan = emptyList(),
                headlineReason = "Start practicing MCQs to unlock your personalized weak-subject plan."
            )
        }

        val bySubject = questions.groupBy { it.question.subject.trim().ifBlank { "MSN" } }
        val diagnostics = bySubject.map { (code, items) ->
            val attempted = items.count { it.attemptsCount > 0 }
            val totalAttempts = items.sumOf { it.attemptsCount }
            val totalCorrect = items.sumOf { it.correctCount }
            val accuracy = if (totalAttempts > 0) {
                ((totalCorrect * 100.0) / totalAttempts.toDouble()).roundToInt().coerceIn(0, 100)
            } else {
                0
            }
            val dueSrsCount = items.count { item ->
                val card = srsCards[item.id]
                card != null && card.nextReviewAtMs <= nowMs
            }
            val severity = when {
                attempted == 0 -> TopicWeaknessSeverity.UNTOUCHED
                accuracy < 55 -> TopicWeaknessSeverity.CRITICAL
                accuracy < 75 || dueSrsCount >= 3 -> TopicWeaknessSeverity.NEEDS_WORK
                else -> TopicWeaknessSeverity.STRONG
            }

            // Higher priority score = needs attention sooner
            val priorityScore = when (severity) {
                TopicWeaknessSeverity.CRITICAL -> 100.0 + (55 - accuracy) * 1.5 + dueSrsCount * 8.0
                TopicWeaknessSeverity.NEEDS_WORK -> 60.0 + (75 - accuracy) + dueSrsCount * 6.0
                TopicWeaknessSeverity.UNTOUCHED -> 45.0 + (items.size.coerceAtMost(50) * 0.2)
                TopicWeaknessSeverity.STRONG -> (100 - accuracy).toDouble() + dueSrsCount * 4.0
            }

            WeakSubjectDiagnostic(
                subjectCode = code,
                subjectLabel = formatSubjectLabel(code),
                attempted = attempted,
                correct = totalCorrect,
                accuracyPercent = accuracy,
                dueSrsCount = dueSrsCount,
                totalAvailable = items.size,
                severity = severity,
                priorityScore = priorityScore
            )
        }.sortedWith(
            compareByDescending<WeakSubjectDiagnostic> { it.priorityScore }
                .thenBy { it.subjectCode }
        )

        val topWeak = diagnostics.take(3)
        val safeGoal = dailyGoalQuestions.coerceIn(15, 100)
        val rotationPool = diagnostics.filter { it.severity != TopicWeaknessSeverity.STRONG }
            .ifEmpty { diagnostics }

        val sevenDayPlan = (1..7).map { dayIndex ->
            val target = rotationPool[(dayIndex - 1) % rotationPool.size]
            val srsSlice = target.dueSrsCount.coerceAtMost(10)
            val mcqTarget = when (target.severity) {
                TopicWeaknessSeverity.CRITICAL -> safeGoal.coerceAtLeast(25)
                TopicWeaknessSeverity.NEEDS_WORK -> safeGoal.coerceAtLeast(20)
                TopicWeaknessSeverity.UNTOUCHED -> (safeGoal * 0.75).roundToInt().coerceAtLeast(15)
                TopicWeaknessSeverity.STRONG -> (safeGoal * 0.65).roundToInt().coerceAtLeast(15)
            }
            val reason = when (target.severity) {
                TopicWeaknessSeverity.CRITICAL ->
                    "Low accuracy (${target.accuracyPercent}%) — rebuild core concepts"
                TopicWeaknessSeverity.NEEDS_WORK ->
                    if (target.dueSrsCount > 0) {
                        "${target.accuracyPercent}% accuracy • ${target.dueSrsCount} due mistakes"
                    } else {
                        "Push accuracy from ${target.accuracyPercent}% above 75%"
                    }
                TopicWeaknessSeverity.UNTOUCHED ->
                    "Not started yet (${target.totalAvailable} MCQs in bank)"
                TopicWeaknessSeverity.STRONG ->
                    "Retention check (${target.accuracyPercent}% accuracy)"
            }
            val dayLabel = when (dayIndex) {
                1 -> "Day 1 · Today"
                2 -> "Day 2 · Tomorrow"
                else -> "Day $dayIndex"
            }
            RecoveryDayTask(
                dayNumber = dayIndex,
                dayLabel = dayLabel,
                subjectCode = target.subjectCode,
                subjectLabel = target.subjectLabel,
                recommendedMcqs = mcqTarget,
                srsReviewCount = srsSlice,
                focusReason = reason
            )
        }

        val lead = topWeak.firstOrNull()
        val headline = when {
            lead == null -> "Build your accuracy baseline with a mixed drill."
            lead.severity == TopicWeaknessSeverity.CRITICAL ->
                "${lead.subjectLabel} is at ${lead.accuracyPercent}% — prioritized first in your auto-drill."
            lead.severity == TopicWeaknessSeverity.NEEDS_WORK ->
                "Targeting ${lead.subjectLabel} (${lead.accuracyPercent}%) and due mistakes to boost your rank."
            lead.severity == TopicWeaknessSeverity.UNTOUCHED ->
                "${lead.subjectLabel} is untouched — starting a starter diagnostic drill."
            else ->
                "All attempted subjects are strong (≥75%). Running high-yield retention mix."
        }

        return SmartStudyPlanSummary(
            diagnostics = diagnostics,
            topWeakSubjects = topWeak,
            sevenDayPlan = sevenDayPlan,
            headlineReason = headline
        )
    }

    /**
     * Selects a high-impact practice set weighted toward due SRS mistakes, previously missed questions,
     * and the student's lowest-accuracy subjects (or a specific [targetSubjectCode] from the 7-day plan).
     */
    fun selectWeightedAutoDrill(
        questions: List<QuestionItem>,
        srsCards: Map<String, SrsCardState>,
        targetCount: Int = 20,
        targetSubjectCode: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ): List<QuestionItem> {
        if (questions.isEmpty() || targetCount <= 0) return emptyList()

        val scopedPool = if (!targetSubjectCode.isNullOrBlank()) {
            questions.filter { it.question.subject.equals(targetSubjectCode.trim(), ignoreCase = true) }
                .ifEmpty { questions }
        } else {
            questions
        }

        val picked = LinkedHashMap<String, QuestionItem>()

        fun addFrom(candidates: List<QuestionItem>, limit: Int) {
            var added = 0
            for (item in candidates) {
                if (picked.size >= targetCount || added >= limit) break
                if (!picked.containsKey(item.id)) {
                    picked[item.id] = item
                    added++
                }
            }
        }

        // Priority 1: Due SRS cards in the scoped pool (up to 40% of drill)
        val dueSrs = scopedPool
            .filter { item ->
                val card = srsCards[item.id]
                card != null && card.nextReviewAtMs <= nowMs
            }
            .sortedBy { srsCards[it.id]?.nextReviewAtMs ?: Long.MAX_VALUE }
        addFrom(dueSrs, (targetCount * 0.4).roundToInt().coerceAtLeast(1))

        // Priority 2: Questions with wrong attempts / lowest accuracy
        val struggled = scopedPool
            .filter { it.attemptsCount > 0 && it.correctCount < it.attemptsCount }
            .sortedWith(
                compareBy<QuestionItem> {
                    it.correctCount.toDouble() / it.attemptsCount.coerceAtLeast(1).toDouble()
                }.thenByDescending { it.attemptsCount - it.correctCount }
            )
        addFrom(struggled, (targetCount * 0.35).roundToInt().coerceAtLeast(1))

        // Priority 3: Top weak subjects (50% #1 weak, 30% #2 weak, 20% #3 weak)
        if (targetSubjectCode.isNullOrBlank()) {
            val plan = buildPlan(scopedPool, srsCards, nowMs = nowMs)
            val weights = listOf(0.50, 0.30, 0.20)
            val remaining = (targetCount - picked.size).coerceAtLeast(0)
            plan.topWeakSubjects.forEachIndexed { idx, diag ->
                val quota = (remaining * weights.getOrElse(idx) { 0.2 }).roundToInt().coerceAtLeast(1)
                val subjectCandidates = scopedPool
                    .filter { it.question.subject.equals(diag.subjectCode, ignoreCase = true) }
                    .sortedWith(
                        compareBy<QuestionItem> { it.attemptsCount > 0 } // unattempted first
                            .thenBy { if (it.attemptsCount > 0) it.correctCount.toDouble() / it.attemptsCount else 0.0 }
                    )
                addFrom(subjectCandidates, quota)
            }
        }

        // Priority 4: Fill any remaining slots with fresh unattempted questions, then rest of pool
        val unattempted = scopedPool.filter { it.attemptsCount == 0 }
        addFrom(unattempted, targetCount - picked.size)
        addFrom(scopedPool, targetCount - picked.size)

        return picked.values.toList()
    }

    private fun formatSubjectLabel(code: String): String = when (code.trim().uppercase()) {
        "MSN" -> "Medical-Surgical"
        "OBG" -> "OBG & Midwifery"
        "PED", "PEDS", "PAED" -> "Pediatrics"
        "FON", "FUND" -> "Fundamentals"
        "CHN" -> "Community Health"
        "PHARM", "PHARMA" -> "Pharmacology"
        "PSY", "PSYCH" -> "Psychiatric Nursing"
        "ANAT", "AP" -> "Anatomy & Phys."
        "MICRO" -> "Microbiology"
        "NUTR" -> "Nutrition"
        "MGMT", "ADMIN" -> "Nursing Mgmt"
        "RES" -> "Nursing Research"
        "GK" -> "General Knowledge"
        "APT", "MATH" -> "Aptitude & Math"
        "SCI" -> "General Science"
        else -> code.trim().replace('_', ' ').ifBlank { "Nursing" }
    }
}
