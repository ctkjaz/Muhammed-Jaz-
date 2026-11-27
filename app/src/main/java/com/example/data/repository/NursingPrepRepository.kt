package com.example.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.data.local.ExamQuestionLinkEntity
import com.example.data.local.MockHistoryEntity
import com.example.data.local.QuestionDao
import com.example.data.local.QuestionProgressEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.data.local.toDomain
import com.example.data.local.toDomainQuestion
import com.example.data.local.toEntity
import com.example.data.model.CategoryStat
import com.example.data.model.ClinicalSpotterCatalog
import com.example.data.model.Exam
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamSection
import com.example.data.model.Flashcard
import com.example.data.model.MockHistory
import com.example.data.model.PerformanceAggregationEngine
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Profession
import com.example.data.model.Question
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.QuestionSourceType
import com.example.data.model.StaticDedupCatalog
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectStat
import com.example.data.model.Topic
import com.example.data.model.composeShiftYear
import com.example.data.parser.BulkQuestionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.json.JSONArray

class NursingPrepRepository(
    private val context: Context,
    private val dao: QuestionDao,
    private val repoScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    val professions: Flow<List<Profession>> =
        dao.observeProfessions().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    val exams: Flow<List<Exam>> =
        dao.observeExams().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    val topics: Flow<List<Topic>> =
        dao.observeTopics().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    /**
     * Single shared SQLite progress observer across the entire app.
     * Conflates rapid burst updates so answering fast never queues redundant rebuilds.
     */
    private val allProgressMap: SharedFlow<Map<String, QuestionProgress>> =
        dao.observeAllProgress()
            .conflate()
            .map { progressList ->
                val map = HashMap<String, QuestionProgress>(progressList.size * 2 + 1)
                for (p in progressList) {
                    map[p.questionId] = p.toDomain()
                }
                map as Map<String, QuestionProgress>
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(
                scope = repoScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000L),
                replay = 1
            )

    /** Lightweight O(1) lookup set of bookmarked question IDs for active practice screens. */
    val bookmarkedQuestionIds: Flow<Set<String>> = allProgressMap
        .map { progressMap ->
            val ids = HashSet<String>()
            for ((id, progress) in progressMap) {
                if (progress.isBookmarked) ids.add(id)
            }
            ids as Set<String>
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    /**
     * Single shared parsed static Question catalog.
     * Parses SQLite rows + optionsJson ONCE on seed/import instead of per-collector.
     */
    private val parsedAllQuestions: SharedFlow<List<Question>> =
        dao.observeAllQuestionEntities()
            .conflate()
            .map { entities -> entities.map { it.toDomainQuestion() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(
                scope = repoScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000L),
                replay = 1
            )

    /**
     * Static stem-deduplicated catalog built ONCE whenever static questions change.
     * Eliminates 10,700+ StringBuilder stem normalizations on every single answer tap.
     */
    private val staticDedupCatalog: SharedFlow<StaticDedupCatalog> =
        parsedAllQuestions
            .map { questions -> PerformanceAggregationEngine.buildStaticDedupCatalog(questions) }
            .flowOn(Dispatchers.Default)
            .shareIn(
                scope = repoScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000L),
                replay = 1
            )

    val allQuestions: Flow<List<QuestionItem>> = run {
        var cachedPrevious: List<QuestionItem>? = null
        combine(parsedAllQuestions, allProgressMap) { questions, progressMap ->
            PerformanceAggregationEngine.overlayProgress(
                questions = questions,
                progressMap = progressMap,
                previousItems = cachedPrevious
            ).also { cachedPrevious = it }
        }.conflate().flowOn(Dispatchers.Default)
    }

    /** Deduplicated cross-exam question stream using cached static deduplication + O(1) instance reuse. */
    val dedupedAllQuestions: Flow<List<QuestionItem>> = run {
        var cachedPrevious: List<QuestionItem>? = null
        combine(staticDedupCatalog, allProgressMap) { catalog, progressMap ->
            PerformanceAggregationEngine.overlayProgress(
                questions = catalog.canonicalQuestions,
                progressMap = progressMap,
                previousItems = cachedPrevious,
                aliasIdsByCanonicalId = catalog.aliasIdsByCanonicalId
            ).also { cachedPrevious = it }
        }.conflate().flowOn(Dispatchers.Default)
    }

    val allExamLinks: SharedFlow<List<ExamQuestionLink>> =
        dao.observeAllExamLinks()
            .conflate()
            .map { list -> list.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(
                scope = repoScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000L),
                replay = 1
            )

    val mockHistory: Flow<List<MockHistory>> =
        dao.observeMockHistory().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    fun observeExamSections(examId: String): Flow<List<ExamSection>> =
        dao.observeExamSections(examId).map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    fun observeQuestionsForExam(examId: String): Flow<List<QuestionItem>> {
        // Project static questions for this exam ONCE per exam/seed change — never on progress writes.
        val projectedExamQuestions: Flow<List<Question>> = combine(
            dao.observeQuestionEntitiesForExam(examId).conflate(),
            dao.observeExamLinksForExam(examId).conflate(),
            parsedAllQuestions
        ) { entities, links, allParsed ->
            val linkMap = HashMap<String, ExamQuestionLinkEntity>(links.size * 2 + 1)
            for (link in links) {
                linkMap[link.questionId] = link
            }
            val result = ArrayList<Question>(entities.size + 4096)
            val seenIds = HashSet<String>(entities.size * 2 + 16)
            for (entity in entities) {
                val q = entity.toDomainQuestion()
                val link = linkMap[q.id]
                val projected = if (link != null) projectQuestionForExam(q, link) else q
                if (seenIds.add(projected.id)) {
                    result.add(projected)
                }
            }
            for (q in allParsed) {
                if (PlatformCatalogDefaults.shouldVirtuallyProjectQuestion(examId, q) &&
                    seenIds.add(q.id)
                ) {
                    result.add(q)
                }
            }
            result as List<Question>
        }.distinctUntilChanged().flowOn(Dispatchers.Default)

        var cachedPrevious: List<QuestionItem>? = null
        return combine(projectedExamQuestions, allProgressMap) { questions, progressMap ->
            PerformanceAggregationEngine.overlayProgress(
                questions = questions,
                progressMap = progressMap,
                previousItems = cachedPrevious
            ).also { cachedPrevious = it }
        }.conflate().flowOn(Dispatchers.Default)
    }

    private fun projectQuestionForExam(
        q: Question,
        link: ExamQuestionLinkEntity
    ): Question {
        return if (link.isPyqForExam) {
            val projectedShiftYear = composeShiftYear(link.examYear, link.examShift)
                .ifBlank { q.shiftYear }
            val projectedSourceType = if (q.sourceType == QuestionSourceType.OFFICIAL_PYQ || q.sourceType == QuestionSourceType.MEMORY_BASED) {
                q.sourceType
            } else {
                if (q.isVerifiedSource) QuestionSourceType.OFFICIAL_PYQ else QuestionSourceType.MEMORY_BASED
            }
            q.copy(
                category = "pyq",
                shiftYear = projectedShiftYear,
                examYear = link.examYear.ifBlank { q.examYear },
                examShift = link.examShift.ifBlank { q.examShift },
                sourceType = projectedSourceType
            )
        } else if (q.isPredictedMock) {
            q.copy(
                category = "mock",
                shiftYear = "",
                examYear = "",
                examShift = "",
                sourceType = QuestionSourceType.PREDICTED_MOCK
            )
        } else {
            val nonPyqCategory = when {
                SubjectCatalog.isNursingCode(q.subject) -> "nur"
                SubjectCatalog.isCaCode(q.subject) -> "ca"
                else -> "gen"
            }
            val nonPyqSourceType = if (SubjectCatalog.isCaCode(q.subject)) {
                QuestionSourceType.CURRENT_AFFAIRS
            } else {
                QuestionSourceType.PRACTICE
            }
            q.copy(
                category = nonPyqCategory,
                shiftYear = "",
                examYear = "",
                examShift = "",
                sourceType = nonPyqSourceType
            )
        }
    }

    fun observeMockHistoryForExam(examId: String): Flow<List<MockHistory>> =
        dao.observeMockHistoryForExam(examId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureCatalogSeeded() = withContext(Dispatchers.IO) {
        dao.upsertProfessions(PlatformCatalogDefaults.DEFAULT_PROFESSIONS.map { it.toEntity() })
        dao.upsertExams(PlatformCatalogDefaults.DEFAULT_EXAMS.map { it.toEntity() })
        dao.upsertExamSections(PlatformCatalogDefaults.DEFAULT_SECTIONS.map { it.toEntity() })

        val subjectEntities = SubjectCatalog.ALL_SUBJS.mapIndexed { idx, meta ->
            val profId = if (SubjectCatalog.isNursingCode(meta.code)) {
                PlatformCatalogDefaults.PROFESSION_NURSING
            } else {
                null
            }
            SubjectEntity(
                code = meta.code,
                professionId = profId,
                name = meta.name,
                accentHex = meta.accentHex,
                sortOrder = idx
            )
        }
        dao.upsertSubjects(subjectEntities)

        val defaultTopics = SubjectCatalog.ALL_SUBJS.map { meta ->
            TopicEntity(
                id = PlatformCatalogDefaults.defaultTopicIdFor(meta.code),
                subjectCode = meta.code,
                name = "General",
                sortOrder = 0
            )
        }
        dao.upsertTopics(defaultTopics)
    }

    suspend fun ensureSeeded(
        currentSeedVersion: Int = 0,
        onSeededVersionUpdated: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val builtInCount = dao.getBuiltInCount()
        val rrbLinkCount = dao.getLinkCountForExam(PlatformCatalogDefaults.EXAM_RRB_NURSING)
        // Fast warm-start path: skip redundant catalog write transactions when already seeded.
        if (currentSeedVersion >= CURRENT_SEED_VERSION && builtInCount > 0 && rrbLinkCount > 0) {
            return@withContext false
        }

        ensureCatalogSeeded()
        val needsQuestions = builtInCount == 0 || currentSeedVersion < CURRENT_SEED_VERSION
        val needsRrbLinks = rrbLinkCount == 0
        val needsNorcetLinks = dao.getLinkCountForExam(PlatformCatalogDefaults.EXAM_AIIMS_NORCET) == 0
        val needsPgimerLinks = dao.getLinkCountForExam(PlatformCatalogDefaults.EXAM_PGIMER_NURSING) == 0
        val needsAllIndiaPyqLinks = dao.getLinkCountForExam(PlatformCatalogDefaults.EXAM_ESIC_NURSING) == 0
        if (needsQuestions || needsRrbLinks || needsNorcetLinks || needsPgimerLinks || needsAllIndiaPyqLinks) {
            fun loadAssetQuestions(assetName: String): List<Question> = runCatching {
                val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
                BulkQuestionParser.parseJsonBatch(json, markAsCustom = false)
            }.getOrDefault(emptyList())

            fun loadAssetQuestionsWithExamIds(assetName: String): List<Pair<Question, String>> = runCatching {
                val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
                BulkQuestionParser.parseJsonBatchWithExamIds(json, markAsCustom = false)
            }.getOrDefault(emptyList())

            suspend fun insertQuestionBatch(questions: List<Question>, baseSortOrder: Int) {
                if (!needsQuestions || questions.isEmpty()) return
                questions.chunked(500).forEachIndexed { chunkIdx, chunk ->
                    val offset = baseSortOrder + chunkIdx * 500
                    dao.insertQuestions(chunk.mapIndexed { idx, q -> q.toEntity(sortOrder = offset + idx) })
                }
            }

            val rrbQuestions = loadAssetQuestions("builtin_questions.json")
            insertQuestionBatch(rrbQuestions, baseSortOrder = 0)

            val norcetQuestions = loadAssetQuestions("norcet_questions.json")
            insertQuestionBatch(norcetQuestions, baseSortOrder = 2000)

            val pgimerQuestions = loadAssetQuestions("pgimer_questions.json")
            insertQuestionBatch(pgimerQuestions, baseSortOrder = 4000)

            val subjectPracticeQuestions = loadAssetQuestions("subject_practice_bank.json")
            insertQuestionBatch(subjectPracticeQuestions, baseSortOrder = 10000)

            val predictedMockQuestions = loadAssetQuestions("predicted_mock_bank.json")
            insertQuestionBatch(predictedMockQuestions, baseSortOrder = 20000)

            val allIndiaPyqPairs = loadAssetQuestionsWithExamIds("all_india_pyqs.json")
            insertQuestionBatch(allIndiaPyqPairs.map { it.first }, baseSortOrder = 25000)

            val clinicalSpotterQuestions = ClinicalSpotterCatalog.builtinSpotterQuestions()
            insertQuestionBatch(clinicalSpotterQuestions, baseSortOrder = 29000)

            if (needsQuestions || needsRrbLinks) {
                val rrbBaseLinks = rrbQuestions.mapIndexed { index, q ->
                    ExamQuestionLinkEntity(
                        examId = PlatformCatalogDefaults.EXAM_RRB_NURSING,
                        questionId = q.id,
                        sectionId = PlatformCatalogDefaults.sectionIdForRrbSubject(q.subject),
                        sortOrder = index,
                        isPyqForExam = q.isPyqTagged,
                        examYear = q.examYear,
                        examShift = q.examShift
                    )
                }
                rrbBaseLinks.chunked(500).forEach { chunk ->
                    dao.upsertExamQuestionLinks(chunk)
                }
            }

            if (needsQuestions || needsNorcetLinks) {
                val dedicatedNorcetLinks = norcetQuestions.mapIndexed { index, q ->
                    ExamQuestionLinkEntity(
                        examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
                        questionId = q.id,
                        sectionId = PlatformCatalogDefaults.sectionIdForNorcetSubject(q.subject),
                        sortOrder = index,
                        isPyqForExam = q.isPyqTagged,
                        examYear = q.examYear,
                        examShift = q.examShift
                    )
                }
                val sharedNorcetLinks = rrbQuestions
                    .filter { PlatformCatalogDefaults.isNorcetSharedSubject(it.subject) }
                    .mapIndexed { index, q ->
                        ExamQuestionLinkEntity(
                            examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
                            questionId = q.id,
                            sectionId = PlatformCatalogDefaults.sectionIdForNorcetSubject(q.subject),
                            sortOrder = 3000 + index,
                            isPyqForExam = false,
                            examYear = "",
                            examShift = ""
                        )
                    }
                (dedicatedNorcetLinks + sharedNorcetLinks).chunked(500).forEach { chunk ->
                    dao.upsertExamQuestionLinks(chunk)
                }
            }

            if (needsQuestions || needsPgimerLinks) {
                val dedicatedPgimerLinks = pgimerQuestions.mapIndexed { index, q ->
                    ExamQuestionLinkEntity(
                        examId = PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
                        questionId = q.id,
                        sectionId = PlatformCatalogDefaults.sectionIdForPgimerSubject(q.subject),
                        sortOrder = index,
                        isPyqForExam = q.isPyqTagged,
                        examYear = q.examYear,
                        examShift = q.examShift
                    )
                }
                val sharedPgimerNursing = (norcetQuestions + rrbQuestions)
                    .filter { PlatformCatalogDefaults.isPgimerSharedSubject(it.subject) }
                    .distinctBy { it.id }
                    .mapIndexed { index, q ->
                        ExamQuestionLinkEntity(
                            examId = PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
                            questionId = q.id,
                            sectionId = PlatformCatalogDefaults.sectionIdForPgimerSubject(q.subject),
                            sortOrder = 5000 + index,
                            isPyqForExam = false,
                            examYear = "",
                            examShift = ""
                        )
                    }
                (dedicatedPgimerLinks + sharedPgimerNursing).chunked(500).forEach { chunk ->
                    dao.upsertExamQuestionLinks(chunk)
                }
            }

            if (needsQuestions || needsAllIndiaPyqLinks) {
                val allIndiaLinks = allIndiaPyqPairs.mapIndexedNotNull { index, (q, targetExamId) ->
                    val resolvedExamId = targetExamId.trim()
                    if (resolvedExamId.isEmpty()) return@mapIndexedNotNull null
                    ExamQuestionLinkEntity(
                        examId = resolvedExamId,
                        questionId = q.id,
                        sectionId = PlatformCatalogDefaults.sectionIdForExam(resolvedExamId, q.subject),
                        sortOrder = index,
                        isPyqForExam = true,
                        examYear = q.examYear,
                        examShift = q.examShift
                    )
                }
                allIndiaLinks.chunked(500).forEach { chunk ->
                    dao.upsertExamQuestionLinks(chunk)
                }
            }

            if (needsQuestions || needsRrbLinks || needsNorcetLinks || needsPgimerLinks) {
                val spotterLinks = buildList {
                    clinicalSpotterQuestions.forEachIndexed { idx, q ->
                        add(
                            ExamQuestionLinkEntity(
                                examId = PlatformCatalogDefaults.EXAM_RRB_NURSING,
                                questionId = q.id,
                                sectionId = PlatformCatalogDefaults.sectionIdForRrbSubject(q.subject),
                                sortOrder = 9000 + idx,
                                isPyqForExam = false,
                                examYear = "",
                                examShift = ""
                            )
                        )
                        add(
                            ExamQuestionLinkEntity(
                                examId = PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
                                questionId = q.id,
                                sectionId = PlatformCatalogDefaults.sectionIdForNorcetSubject(q.subject),
                                sortOrder = 9000 + idx,
                                isPyqForExam = false,
                                examYear = "",
                                examShift = ""
                            )
                        )
                        add(
                            ExamQuestionLinkEntity(
                                examId = PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
                                questionId = q.id,
                                sectionId = PlatformCatalogDefaults.sectionIdForPgimerSubject(q.subject),
                                sortOrder = 9000 + idx,
                                isPyqForExam = false,
                                examYear = "",
                                examShift = ""
                            )
                        )
                    }
                }
                spotterLinks.chunked(500).forEach { chunk ->
                    dao.upsertExamQuestionLinks(chunk)
                }
            }
            onSeededVersionUpdated(CURRENT_SEED_VERSION)
            return@withContext true
        }
        false
    }

    suspend fun linkQuestionToExam(
        questionId: String,
        examId: String,
        sectionId: String,
        sortOrder: Int = 0,
        isPyqForExam: Boolean = false,
        examYear: String = "",
        examShift: String = ""
    ) = withContext(Dispatchers.IO) {
        dao.upsertExamQuestionLinks(
            listOf(
                ExamQuestionLinkEntity(
                    examId = examId,
                    questionId = questionId,
                    sectionId = sectionId,
                    sortOrder = sortOrder,
                    isPyqForExam = isPyqForExam,
                    examYear = examYear,
                    examShift = examShift
                )
            )
        )
    }

    suspend fun ensureTopicExists(subjectCode: String, topicName: String): String = withContext(Dispatchers.IO) {
        val cleanSubject = subjectCode.trim().ifBlank { "MSN" }
        val topicId = BulkQuestionParser.slugTopicId(cleanSubject, topicName)
        val displayName = topicName.trim().ifBlank { "General" }
        dao.upsertSubjects(
            listOf(
                SubjectEntity(
                    code = cleanSubject,
                    professionId = if (SubjectCatalog.isNursingCode(cleanSubject)) {
                        PlatformCatalogDefaults.PROFESSION_NURSING
                    } else null,
                    name = SubjectCatalog.displayNameFor(cleanSubject),
                    accentHex = SubjectCatalog.ALL_SUBJS.firstOrNull { it.code == cleanSubject }?.accentHex ?: "#726D70",
                    sortOrder = SubjectCatalog.ALL_SUBJS.indexOfFirst { it.code == cleanSubject }.takeIf { it >= 0 } ?: 99
                )
            )
        )
        dao.upsertTopics(
            listOf(
                TopicEntity(
                    id = topicId,
                    subjectCode = cleanSubject,
                    name = displayName,
                    sortOrder = if (displayName == "General") 0 else 10
                )
            )
        )
        topicId
    }

    companion object {
        const val CURRENT_SEED_VERSION = 12
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

    suspend fun recordAttempt(questionId: String, isCorrect: Boolean): QuestionProgressEntity = withContext(Dispatchers.IO) {
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
        updated
    }

    suspend fun recordBatchAttempts(results: List<Pair<String, Boolean>>): List<QuestionProgressEntity> = withContext(Dispatchers.IO) {
        if (results.isEmpty()) return@withContext emptyList()
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
        updates
    }

    suspend fun toggleBookmark(questionId: String): QuestionProgressEntity = withContext(Dispatchers.IO) {
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
        updated
    }

    suspend fun saveCustomQuestions(
        questions: List<Question>,
        examIds: List<String> = listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING),
        customExamMap: Map<String, List<String>> = emptyMap(),
        topicLabelOverrides: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext
        ensureCatalogSeeded()
        val startOrder = dao.getTotalCount() + 1000
        val entities = questions.mapIndexed { idx, q ->
            val explicitTopicLabel = topicLabelOverrides[q.topicId]
            if (explicitTopicLabel != null) {
                ensureTopicExists(q.subject, explicitTopicLabel)
            }
            q.copy(isCustom = true).toEntity(sortOrder = startOrder + idx)
        }
        dao.insertQuestions(entities)

        val validTargetExamIds = examIds.ifEmpty { listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING) }
        val links = buildList {
            questions.forEachIndexed { idx, q ->
                val targets = customExamMap[q.id].orEmpty().ifEmpty { validTargetExamIds }
                targets.distinct().forEach { targetExamId ->
                    val resolvedExamId = targetExamId.ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING }
                    add(
                        ExamQuestionLinkEntity(
                            examId = resolvedExamId,
                            questionId = q.id,
                            sectionId = PlatformCatalogDefaults.sectionIdForExam(resolvedExamId, q.subject),
                            sortOrder = startOrder + idx,
                            isPyqForExam = q.isPyqTagged,
                            examYear = q.examYear,
                            examShift = q.examShift
                        )
                    )
                }
            }
        }
        if (links.isNotEmpty()) {
            dao.upsertExamQuestionLinks(links)
        }
    }

    /**
     * Persists Admin-published OTA question packs into Room SQLite as official bank entries
     * (isCustom = false) so they survive custom clear, work offline, and appear natively
     * across Dashboard, Subjects, Practice, and CBT Mocks.
     */
    suspend fun saveOtaPackQuestions(
        examId: String,
        questions: List<Question>
    ) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext
        ensureCatalogSeeded()
        val resolvedExamId = examId.trim().ifBlank { PlatformCatalogDefaults.EXAM_AIIMS_NORCET }
        val baseSort = 7000
        val entities = questions.mapIndexed { idx, q ->
            q.copy(isCustom = false).toEntity(sortOrder = baseSort + idx)
        }
        dao.insertQuestions(entities)

        val links = questions.mapIndexed { idx, q ->
            ExamQuestionLinkEntity(
                examId = resolvedExamId,
                questionId = q.id,
                sectionId = PlatformCatalogDefaults.sectionIdForExam(resolvedExamId, q.subject),
                sortOrder = baseSort + idx,
                isPyqForExam = q.isPyqTagged,
                examYear = q.examYear,
                examShift = q.examShift
            )
        }
        dao.upsertExamQuestionLinks(links)
    }

    suspend fun clearCustomQuestions() = withContext(Dispatchers.IO) {
        dao.deleteCustomQuestionLinks()
        dao.deleteCustomQuestions()
    }

    suspend fun restoreBackupWithDetails(
        jsonText: String,
        defaultExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING,
        allowCustomQuestions: Boolean = true
    ): BulkQuestionParser.RestoredBackup = withContext(Dispatchers.IO) {
        val restored = BulkQuestionParser.parseBackupJson(jsonText)
        if (allowCustomQuestions && restored.customQuestions.isNotEmpty()) {
            saveCustomQuestions(
                questions = restored.customQuestions,
                examIds = listOf(defaultExamId),
                customExamMap = restored.customQuestionExamIds
            )
        }
        if (restored.progressList.isNotEmpty()) {
            dao.upsertProgressList(restored.progressList)
        }
        restored
    }

    suspend fun restoreBackup(
        jsonText: String,
        defaultExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
    ): Int = restoreBackupWithDetails(jsonText, defaultExamId).customQuestions.size

    suspend fun saveMockHistory(
        title: String,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        skippedCount: Int,
        netScore: Double,
        accuracyPercent: Int,
        examId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
    ): MockHistoryEntity = withContext(Dispatchers.IO) {
        val entity = MockHistoryEntity(
            title = title,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            wrongCount = wrongCount,
            skippedCount = skippedCount,
            netScore = netScore,
            accuracyPercent = accuracyPercent,
            timestamp = System.currentTimeMillis(),
            examId = examId
        )
        dao.insertMockHistory(entity)
        entity
    }

    suspend fun clearStatistics(): List<QuestionProgressEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allProgress = dao.getAllProgressSnapshot()
        val resetBookmarksKept = allProgress.map {
            it.copy(
                attemptsCount = 0,
                correctCount = 0,
                wrongCount = 0,
                lastOutcome = null,
                updatedAt = now
            )
        }
        dao.upsertProgressList(resetBookmarksKept)
        resetBookmarksKept
    }

    suspend fun getAllProgressSnapshot(): List<QuestionProgressEntity> = withContext(Dispatchers.IO) {
        dao.getAllProgressSnapshot()
    }

    suspend fun upsertProgressList(items: List<QuestionProgressEntity>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            dao.upsertProgressList(items)
        }
    }

    suspend fun getAllMockHistorySnapshot(): List<MockHistoryEntity> = withContext(Dispatchers.IO) {
        dao.getAllMockHistorySnapshot()
    }

    suspend fun insertMockHistoryIfMissing(entity: MockHistoryEntity) = withContext(Dispatchers.IO) {
        val existing = dao.getAllMockHistorySnapshot()
        val duplicate = existing.any {
            it.timestamp == entity.timestamp && it.examId == entity.examId && it.title == entity.title
        }
        if (!duplicate) {
            dao.insertMockHistory(entity)
        }
    }

    suspend fun getCustomQuestionsWithExamIds(): Pair<List<Question>, Map<String, List<String>>> =
        withContext(Dispatchers.IO) {
            val customItems = dao.getCustomQuestionsSnapshot().map { it.toDomain().question }
            val examMap = customItems.associate { q ->
                val links = dao.getLinksForQuestion(q.id).map { it.examId }.distinct()
                q.id to links.ifEmpty { listOf(PlatformCatalogDefaults.EXAM_RRB_NURSING) }
            }
            customItems to examMap
        }

    suspend fun clearUserSpecificLocalDataForAccountSwitch() = withContext(Dispatchers.IO) {
        dao.clearAllProgress()
        dao.clearMockHistory()
        dao.deleteCustomQuestionLinks()
        dao.deleteCustomQuestions()
    }

    fun computeSubjectStats(items: List<QuestionItem>): List<SubjectStat> {
        val grouped = items.groupBy { it.question.subject }
        return SubjectCatalog.ALL_SUBJS.mapNotNull { meta ->
            val list = grouped[meta.code] ?: return@mapNotNull null
            if (list.isEmpty()) return@mapNotNull null
            var attempted = 0
            var totalAttempts = 0
            var correct = 0
            var wrong = 0
            var mastered = 0
            for (item in list) {
                val p = item.progress
                if (p.attemptsCount > 0) attempted++
                totalAttempts += p.attemptsCount
                correct += p.correctCount
                wrong += p.wrongCount
                if (p.lastOutcome == "right") mastered++
            }
            SubjectStat(
                code = meta.code,
                name = meta.name,
                group = meta.group,
                totalQuestions = list.size,
                attemptedQuestions = attempted,
                totalAttempts = totalAttempts,
                correctAttempts = correct,
                wrongAttempts = wrong,
                masteredQuestions = mastered,
                accentColor = meta.accentColor
            )
        }
    }

    fun computeCategoryStats(
        items: List<QuestionItem>,
        activeExamId: String = PlatformCatalogDefaults.EXAM_RRB_NURSING
    ): List<CategoryStat> {
        val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExamId)
        val isPureNursing = PlatformCatalogDefaults.examIsPureNursing(activeExamId)
        val examShort = PlatformCatalogDefaults.examById(activeExamId)?.shortName ?: "Exam"
        val nurPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExamId)
        val nonPct = 100 - nurPct
        data class CatDef(val key: String, val label: String, val desc: String, val color: Color, val filter: (QuestionItem) -> Boolean)
        val defs = when {
            isPureNursing -> listOf(
                CatDef("pyq", "$examShort PYQs", "Paper-wise memory-based recall sets", Color(0xFFA67942)) {
                    it.question.isPyqTagged
                },
                CatDef("nur", "Core Nursing (100%)", "All 9 clinical & foundational nursing subjects", Color(0xFF8E4456)) {
                    SubjectCatalog.isNursing(it.question)
                }
            )
            isNorcet -> listOf(
                CatDef("pyq", "NORCET Papers (5–10)", "Paper-wise Prelims & Mains recall sets", Color(0xFFA67942)) {
                    it.question.isPyqTagged
                },
                CatDef("nur", "Nursing Core (80%)", "All 9 clinical nursing subjects & Mains scenarios", Color(0xFF8E4456)) {
                    SubjectCatalog.isNursing(it.question)
                },
                CatDef("ca", "GK & Health Programs", "National awareness, health programs & static GK", Color(0xFFB78B79)) {
                    SubjectCatalog.isCa(it.question)
                },
                CatDef("gen", "Aptitude & Science (20%)", "Quantitative dosage, reasoning & general science", Color(0xFF527D86)) {
                    SubjectCatalog.isGen(it.question)
                }
            )
            activeExamId == PlatformCatalogDefaults.EXAM_RRB_NURSING -> listOf(
                CatDef("pyq", "RRB Previous Papers (2015–2026)", "Shift-wise exam sets", Color(0xFFA67942)) {
                    it.question.isPyqTagged
                },
                CatDef("nur", "Nursing (70%)", "All 9 clinical nursing subjects", Color(0xFF8E4456)) {
                    SubjectCatalog.isNursing(it.question)
                },
                CatDef("ca", "CA & Static GK", "Current affairs, railways & static GK", Color(0xFFB78B79)) {
                    SubjectCatalog.isCa(it.question)
                },
                CatDef("gen", "Sci / Math / Reas (30%)", "General science, arithmetic & reasoning", Color(0xFF527D86)) {
                    SubjectCatalog.isGen(it.question)
                }
            )
            else -> listOf(
                CatDef("pyq", "$examShort Papers", "Paper-wise memory-based recall sets", Color(0xFFA67942)) {
                    it.question.isPyqTagged
                },
                CatDef("nur", "Nursing Core ($nurPct%)", "All 9 clinical & foundational nursing subjects", Color(0xFF8E4456)) {
                    SubjectCatalog.isNursing(it.question)
                },
                CatDef("ca", "GK & Health Programs", "Current affairs, health schemes & static GK", Color(0xFFB78B79)) {
                    SubjectCatalog.isCa(it.question)
                },
                CatDef("gen", "Aptitude & Science ($nonPct%)", "General science, dosage math & reasoning", Color(0xFF527D86)) {
                    SubjectCatalog.isGen(it.question)
                }
            )
        }
        return defs.map { d ->
            var total = 0
            var attempted = 0
            var totalAttempts = 0
            var correct = 0
            var wrong = 0
            for (item in items) {
                if (d.filter(item)) {
                    total++
                    val p = item.progress
                    if (p.attemptsCount > 0) attempted++
                    totalAttempts += p.attemptsCount
                    correct += p.correctCount
                    wrong += p.wrongCount
                }
            }
            CategoryStat(
                key = d.key,
                label = d.label,
                description = d.desc,
                totalQuestions = total,
                attemptedQuestions = attempted,
                totalAttempts = totalAttempts,
                correctAttempts = correct,
                wrongAttempts = wrong,
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
