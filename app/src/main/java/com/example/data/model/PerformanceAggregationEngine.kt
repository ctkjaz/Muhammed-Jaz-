package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * Isolated 1-second session countdown state.
 * Collected ONLY by compact header timer badges so ticking clocks never recompose
 * the full app shell, question cards, option lists, or CBT palette grids.
 */
@Immutable
data class SessionTimerState(
    val remainingSeconds: Int = 0,
    val sectionRemainingSeconds: Int = 0
)

/**
 * Pre-computed single-pass summary for an exam stage pool so Dashboard, Subjects,
 * and CBT screens read O(1) counts with zero main-thread collection scans.
 */
@Immutable
data class PrecomputedPoolSummary(
    val totalQuestions: Int = 0,
    val attemptedUnique: Int = 0,
    val totalAttempts: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val latestRight: Int = 0,
    val latestWrong: Int = 0,
    val bookmarkCount: Int = 0,
    val customCount: Int = 0,
    val pyqCount: Int = 0,
    val imageBasedCount: Int = 0,
    val cbtPredictedCount: Int = 0,
    val cbtPredictedNurCount: Int = 0,
    val cbtPracticeNurCount: Int = 0,
    val cbtPracticeNonCount: Int = 0,
    val cbtHardClinicalCount: Int = 0
)

/**
 * Cached result of deduplicating static [Question] entities once on catalog load.
 * Progress changes overlay onto [canonicalQuestions] in O(N) without ever
 * re-normalizing question stems or rebuilding hash maps of strings.
 */
data class StaticDedupCatalog(
    val canonicalQuestions: List<Question> = emptyList(),
    val aliasIdsByCanonicalId: Map<String, List<String>> = emptyMap(),
    val globalImageBasedCount: Int = 0
)

/**
 * High-performance pure-Kotlin aggregation helpers engineered for 12,000+ MCQ catalogs.
 */
object PerformanceAggregationEngine {

    /**
     * Deduplicates static [Question] rows by ID and normalized (subject, stem) ONCE
     * when the static question table changes (seed/import/delete), never on progress writes.
     */
    fun buildStaticDedupCatalog(questions: List<Question>): StaticDedupCatalog {
        if (questions.isEmpty()) return StaticDedupCatalog()

        val byId = LinkedHashMap<String, Question>(questions.size * 2 + 1)
        for (q in questions) {
            if (!byId.containsKey(q.id)) {
                byId[q.id] = q
            }
        }

        val canonicalByStem = LinkedHashMap<String, Question>(byId.size * 2 + 1)
        val aliasesByCanonical = HashMap<String, MutableList<String>>()
        var ibqCount = 0

        for (q in byId.values) {
            val stemKey = "${q.subject}|${SubjectPracticeLogic.normalizeStem(q.questionText)}"
            val existing = canonicalByStem[stemKey]
            if (existing == null) {
                canonicalByStem[stemKey] = q
                if (q.isImageBased) ibqCount++
            } else {
                aliasesByCanonical.getOrPut(existing.id) { ArrayList(2) }.add(q.id)
            }
        }

        return StaticDedupCatalog(
            canonicalQuestions = ArrayList(canonicalByStem.values),
            aliasIdsByCanonicalId = aliasesByCanonical,
            globalImageBasedCount = ibqCount
        )
    }

    /**
     * Overlays [progressMap] onto static [questions] while reusing identical [QuestionItem]
     * instances from [previousItems] whenever both the question reference and progress value
     * are unchanged. Answering 1 question out of 12,000 allocates ~1 new QuestionItem instead
     * of 12,000, slashing GC pressure and Compose diff overhead.
     */
    fun overlayProgress(
        questions: List<Question>,
        progressMap: Map<String, QuestionProgress>,
        previousItems: List<QuestionItem>? = null,
        aliasIdsByCanonicalId: Map<String, List<String>> = emptyMap()
    ): List<QuestionItem> {
        if (questions.isEmpty()) return emptyList()

        val canReuseByIndex = previousItems != null && previousItems.size == questions.size
        val result = ArrayList<QuestionItem>(questions.size)

        for (i in questions.indices) {
            val q = questions[i]
            val resolvedProgress = resolveBestProgress(
                canonicalId = q.id,
                progressMap = progressMap,
                aliasIds = aliasIdsByCanonicalId[q.id]
            )
            if (canReuseByIndex) {
                val prev = previousItems!![i]
                if (prev.question === q && prev.progress == resolvedProgress) {
                    result.add(prev)
                    continue
                }
            }
            result.add(QuestionItem(question = q, progress = resolvedProgress))
        }
        return result
    }

    private fun resolveBestProgress(
        canonicalId: String,
        progressMap: Map<String, QuestionProgress>,
        aliasIds: List<String>?
    ): QuestionProgress {
        var best = progressMap[canonicalId]
        if (!aliasIds.isNullOrEmpty()) {
            for (aliasId in aliasIds) {
                val candidate = progressMap[aliasId] ?: continue
                if (best == null ||
                    candidate.attemptsCount > best.attemptsCount ||
                    (!best.isBookmarked && candidate.isBookmarked)
                ) {
                    best = if (candidate.questionId == canonicalId) {
                        candidate
                    } else {
                        candidate.copy(questionId = canonicalId)
                    }
                }
            }
        }
        return best ?: QuestionProgress(questionId = canonicalId)
    }

    /**
     * Computes all Dashboard, Subjects Hub, and CBT Hub metrics in a SINGLE O(N) pass
     * on Dispatchers.Default — zero main-thread scans during Compose rendering.
     */
    fun computePoolSummary(
        stagePool: List<QuestionItem>,
        allExamItems: List<QuestionItem>
    ): PrecomputedPoolSummary {
        var attemptedUnique = 0
        var totalAttempts = 0
        var totalCorrect = 0
        var totalWrong = 0
        var latestRight = 0
        var latestWrong = 0
        var pyqCount = 0
        var imageBasedCount = 0
        var cbtPredictedCount = 0
        var cbtPredictedNurCount = 0
        var cbtPracticeNurCount = 0
        var cbtPracticeNonCount = 0
        var cbtHardClinicalCount = 0

        for (item in stagePool) {
            val p = item.progress
            if (p.attemptsCount > 0) attemptedUnique++
            totalAttempts += p.attemptsCount
            totalCorrect += p.correctCount
            totalWrong += p.wrongCount
            when (p.lastOutcome) {
                "right" -> latestRight++
                "wrong" -> latestWrong++
            }

            val q = item.question
            if (q.isImageBased) imageBasedCount++
            if (q.isPyqTagged) {
                pyqCount++
            } else {
                val isNur = SubjectCatalog.isNursing(q)
                if (q.isPredictedMock) {
                    cbtPredictedCount++
                    if (isNur) {
                        cbtPredictedNurCount++
                        if (q.difficulty == QuestionDifficulty.HARD ||
                            q.questionText.contains("[Mains", ignoreCase = true)
                        ) {
                            cbtHardClinicalCount++
                        }
                    }
                } else if (isNur) {
                    cbtPracticeNurCount++
                    if (q.difficulty == QuestionDifficulty.HARD ||
                        item.id.startsWith("P_") ||
                        q.questionText.contains("[Mains", ignoreCase = true)
                    ) {
                        cbtHardClinicalCount++
                    }
                } else {
                    cbtPracticeNonCount++
                }
            }
        }

        var bookmarkCount = 0
        var customCount = 0
        for (item in allExamItems) {
            if (item.isBookmarked) bookmarkCount++
            if (item.question.isCustom) customCount++
        }

        return PrecomputedPoolSummary(
            totalQuestions = stagePool.size,
            attemptedUnique = attemptedUnique,
            totalAttempts = totalAttempts,
            totalCorrect = totalCorrect,
            totalWrong = totalWrong,
            latestRight = latestRight,
            latestWrong = latestWrong,
            bookmarkCount = bookmarkCount,
            customCount = customCount,
            pyqCount = pyqCount,
            imageBasedCount = imageBasedCount,
            cbtPredictedCount = cbtPredictedCount,
            cbtPredictedNurCount = cbtPredictedNurCount,
            cbtPracticeNurCount = cbtPracticeNurCount,
            cbtPracticeNonCount = cbtPracticeNonCount,
            cbtHardClinicalCount = cbtHardClinicalCount
        )
    }

    /** Extracts mistakes in one linear pass off the UI thread. */
    fun extractStageMistakes(stagePool: List<QuestionItem>): List<QuestionItem> {
        val mistakes = ArrayList<QuestionItem>()
        for (item in stagePool) {
            if (item.lastOutcome == "wrong") {
                mistakes.add(item)
            }
        }
        return mistakes
    }
}
