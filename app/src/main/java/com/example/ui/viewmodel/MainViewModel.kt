package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.CategoryStat
import com.example.data.model.DifficultyFilter
import com.example.data.model.Flashcard
import com.example.data.model.MockHistory
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectStat
import com.example.data.export.PdfWorksheetOptions
import com.example.data.export.PdfWorksheetWriter
import com.example.data.parser.BulkQuestionParser
import com.example.data.repository.NursingPrepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

enum class MainTab(val label: String) {
    DASHBOARD("Home"),
    SUBJECTS("Subjects"),
    CBT("CBT Mock"),
    STATS("Stats"),
    DECK("Deck")
}

enum class SubjectHubSection(val label: String) {
    NURSING("Nursing 70%"),
    PYQ("PYQs"),
    CA_GK("CA & GK"),
    NON_TECH("Sci/Math 30%")
}

enum class CbtPaletteStatus {
    NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED, ANSWERED_MARKED
}

enum class CbtReviewFilter(val label: String) {
    ALL("All"),
    WRONG("Wrong Only"),
    RIGHT("Correct"),
    SKIPPED("Skipped")
}

data class PracticeSessionState(
    val title: String,
    val questions: List<QuestionItem>,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, Int> = emptyMap(),
    val timedMode: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val showCompletionPrompt: Boolean = false,
    val showTimeoutDialog: Boolean = false,
    val isSubmittedSummary: Boolean = false,
    val reviewFilter: CbtReviewFilter = CbtReviewFilter.ALL
)

data class CbtSessionState(
    val title: String,
    val questions: List<QuestionItem>,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val paletteStatuses: Map<Int, CbtPaletteStatus> = emptyMap(),
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isSubmitted: Boolean = false,
    val showSubmitConfirm: Boolean = false,
    val showTimeoutDialog: Boolean = false,
    val reviewFilter: CbtReviewFilter = CbtReviewFilter.ALL
)

data class TimedPracticeConfigState(
    val isVisible: Boolean = false,
    val sourceKey: String = "MIXED",
    val questionCount: Int = 30,
    val durationMinutes: Int = 30,
    val difficultyFilter: DifficultyFilter = DifficultyFilter.ALL
)

data class ImporterUiState(
    val isVisible: Boolean = false,
    val selectedTab: Int = 0,
    val category: String = "pyq",
    val subject: String = "MSN",
    val shiftYear: String = "2025-S1",
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    val sourceNote: String = "",
    val markAsVerified: Boolean = false,
    val rawText: String = "",
    val jsonText: String = "",
    val previewQuestions: List<Question> = emptyList(),
    val hasPreviewed: Boolean = false
)

data class PdfExportConfigState(
    val isVisible: Boolean = false,
    val sourceKey: String = "MIXED",
    val difficultyFilter: DifficultyFilter = DifficultyFilter.ALL,
    val questionLimit: Int = 30,
    val inlineAnswers: Boolean = false,
    val includeAnswerKey: Boolean = true,
    val isExporting: Boolean = false
)

data class OverallStats(
    val totalQuestions: Int = 0,
    val attemptedUnique: Int = 0,
    val totalAttempts: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val accuracyPercent: Int = 0,
    val netPracticeScore: Double = 0.0,
    val bookmarkCount: Int = 0,
    val mistakeCount: Int = 0,
    val customCount: Int = 0
)

data class DashboardDerivedState(
    val questions: List<QuestionItem> = emptyList(),
    val overall: OverallStats = OverallStats(),
    val subjectStats: List<SubjectStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val lowestAccuracyQuestions: List<QuestionItem> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val mockHistory: List<MockHistory> = emptyList(),
    val isLoaded: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = NursingPrepRepository(application, db.questionDao())
    private val settings = SettingsPreferences(application)

    val themePreference: StateFlow<ThemePreference> = settings.themePreference
    val negativeMarking: StateFlow<Boolean> = settings.negativeMarking

    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    private val _isSeeded = MutableStateFlow(false)

    val mainState: StateFlow<DashboardDerivedState> = combine(
        repository.allQuestions,
        repository.mockHistory,
        _flashcards,
        _isSeeded
    ) { items, history, cards, seeded ->
        val attemptedUnique = items.count { it.attemptsCount > 0 }
        val totalAttempts = items.sumOf { it.attemptsCount }
        val totalCorrect = items.sumOf { it.correctCount }
        val totalWrong = items.sumOf { it.wrongCount }
        val acc = if (totalAttempts > 0) ((totalCorrect.toFloat() / totalAttempts) * 100f).toInt() else 0
        val latestRight = items.count { it.lastOutcome == "right" }
        val latestWrong = items.count { it.lastOutcome == "wrong" }
        val netScore = latestRight - (latestWrong * 0.33)
        DashboardDerivedState(
            questions = items,
            overall = OverallStats(
                totalQuestions = items.size,
                attemptedUnique = attemptedUnique,
                totalAttempts = totalAttempts,
                totalCorrect = totalCorrect,
                totalWrong = totalWrong,
                accuracyPercent = acc,
                netPracticeScore = netScore,
                bookmarkCount = items.count { it.isBookmarked },
                mistakeCount = items.count { it.lastOutcome == "wrong" },
                customCount = items.count { it.question.isCustom }
            ),
            subjectStats = repository.computeSubjectStats(items),
            categoryStats = repository.computeCategoryStats(items),
            lowestAccuracyQuestions = repository.findLowestAccuracyQuestions(items),
            flashcards = cards,
            mockHistory = history,
            isLoaded = seeded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardDerivedState()
    )

    private val _selectedTab = MutableStateFlow(MainTab.DASHBOARD)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _subjectHubSection = MutableStateFlow(SubjectHubSection.NURSING)
    val subjectHubSection: StateFlow<SubjectHubSection> = _subjectHubSection.asStateFlow()

    private val _difficultyFilter = MutableStateFlow(DifficultyFilter.ALL)
    val difficultyFilter: StateFlow<DifficultyFilter> = _difficultyFilter.asStateFlow()

    private val _practiceSession = MutableStateFlow<PracticeSessionState?>(null)
    val practiceSession: StateFlow<PracticeSessionState?> = _practiceSession.asStateFlow()

    private val _cbtSession = MutableStateFlow<CbtSessionState?>(null)
    val cbtSession: StateFlow<CbtSessionState?> = _cbtSession.asStateFlow()

    private val _timedConfig = MutableStateFlow(TimedPracticeConfigState())
    val timedConfig: StateFlow<TimedPracticeConfigState> = _timedConfig.asStateFlow()

    private val _importerState = MutableStateFlow(ImporterUiState())
    val importerState: StateFlow<ImporterUiState> = _importerState.asStateFlow()

    private val _pdfExportConfig = MutableStateFlow(PdfExportConfigState())
    val pdfExportConfig: StateFlow<PdfExportConfigState> = _pdfExportConfig.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded(settings.getSeededVersion()) { newVer ->
                settings.setSeededVersion(newVer)
            }
            _flashcards.value = repository.loadFlashcards()
            _isSeeded.value = true
        }
    }

    fun selectTab(tab: MainTab) {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
        _selectedTab.value = tab
    }

    fun selectSubjectSection(section: SubjectHubSection) {
        _subjectHubSection.value = section
    }

    fun selectDifficultyFilter(filter: DifficultyFilter) {
        _difficultyFilter.value = filter
    }

    fun openSubjectHub(section: SubjectHubSection) {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
        _subjectHubSection.value = section
        _selectedTab.value = MainTab.SUBJECTS
    }

    fun toggleTheme(currentlyDark: Boolean) {
        settings.cycleTheme(currentlyDark)
    }

    fun toggleNegativeMarking() {
        settings.setNegativeMarking(!settings.negativeMarking.value)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // ------------------------------------------------------------------------
    // PRACTICE & TIMED PRACTICE
    // ------------------------------------------------------------------------
    fun startPractice(
        items: List<QuestionItem>,
        title: String,
        timedMode: Boolean = false,
        durationMinutes: Int = 30
    ) {
        if (items.isEmpty()) {
            showMessage("No questions found in this selection yet.")
            return
        }
        stopActiveTimer()
        _cbtSession.value = null
        val totalSec = if (timedMode) durationMinutes.coerceAtLeast(1) * 60 else 0
        _practiceSession.value = PracticeSessionState(
            title = title,
            questions = items,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            timedMode = timedMode,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        if (timedMode) {
            startPracticeTimer()
        }
    }

    private fun startPracticeTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _practiceSession.value ?: break
                if (!current.timedMode || current.isSubmittedSummary || current.showTimeoutDialog) break
                val nextSec = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (nextSec == 0) {
                    _practiceSession.update {
                        it?.copy(remainingSeconds = 0, showTimeoutDialog = true)
                    }
                    break
                } else {
                    _practiceSession.update { it?.copy(remainingSeconds = nextSec) }
                }
            }
        }
    }

    fun answerPracticeQuestion(optionIndex: Int) {
        val sess = _practiceSession.value ?: return
        if (sess.isSubmittedSummary) return
        val qItem = sess.questions.getOrNull(sess.currentIndex) ?: return
        if (sess.selectedAnswers.containsKey(qItem.id)) return

        val updatedMap = sess.selectedAnswers + (qItem.id to optionIndex)
        val isCorrect = optionIndex == qItem.question.correctIndex
        val allAnswered = updatedMap.size == sess.questions.size
        _practiceSession.value = sess.copy(
            selectedAnswers = updatedMap,
            showCompletionPrompt = sess.timedMode && allAnswered
        )

        viewModelScope.launch {
            repository.recordAttempt(qItem.id, isCorrect)
        }
    }

    fun movePracticeIndex(newIndex: Int) {
        _practiceSession.update { sess ->
            sess?.copy(currentIndex = newIndex.coerceIn(0, sess.questions.lastIndex.coerceAtLeast(0)))
        }
    }

    fun requestPracticeSubmitPrompt() {
        _practiceSession.update { it?.copy(showCompletionPrompt = true) }
    }

    fun dismissPracticeSubmitPrompt() {
        _practiceSession.update { it?.copy(showCompletionPrompt = false) }
    }

    fun submitTimedPractice() {
        stopActiveTimer()
        _practiceSession.update { sess ->
            sess?.copy(
                showCompletionPrompt = false,
                showTimeoutDialog = false,
                isSubmittedSummary = true
            )
        }
    }

    fun setPracticeReviewFilter(filter: CbtReviewFilter) {
        _practiceSession.update { it?.copy(reviewFilter = filter) }
    }

    fun exitPracticeOrCbt() {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
    }

    // ------------------------------------------------------------------------
    // TIMED PRACTICE CONFIGURATOR
    // ------------------------------------------------------------------------
    fun openTimedPracticeConfig(
        defaultSource: String = "MIXED",
        defaultCount: Int = 30,
        defaultDifficulty: DifficultyFilter = _difficultyFilter.value
    ) {
        val defaultMins = when (defaultCount) {
            15 -> 15
            30 -> 30
            50 -> 45
            else -> 30
        }
        _timedConfig.value = TimedPracticeConfigState(
            isVisible = true,
            sourceKey = defaultSource,
            questionCount = defaultCount,
            durationMinutes = defaultMins,
            difficultyFilter = defaultDifficulty
        )
    }

    fun closeTimedPracticeConfig() {
        _timedConfig.update { it.copy(isVisible = false) }
    }

    fun updateTimedConfigSource(sourceKey: String) {
        _timedConfig.update { it.copy(sourceKey = sourceKey) }
    }

    fun updateTimedConfigDifficulty(filter: DifficultyFilter) {
        _timedConfig.update { it.copy(difficultyFilter = filter) }
    }

    fun updateTimedConfigCount(count: Int) {
        val suggestedMinutes = when (count) {
            15 -> 15
            30 -> 30
            50 -> 45
            else -> 60
        }
        _timedConfig.update { it.copy(questionCount = count, durationMinutes = suggestedMinutes) }
    }

    fun updateTimedConfigMinutes(minutes: Int) {
        _timedConfig.update { it.copy(durationMinutes = minutes.coerceIn(5, 120)) }
    }

    fun filteredQuestionsForSource(
        sourceKey: String,
        all: List<QuestionItem>,
        difficultyFilter: DifficultyFilter = DifficultyFilter.ALL
    ): List<QuestionItem> {
        val bySource = when (sourceKey) {
            "MIXED" -> all
            "PYQ" -> all.filter { it.question.category == "pyq" || it.question.shiftYear.isNotBlank() }
            "NURSING" -> all.filter { SubjectCatalog.isNursing(it.question) }
            "CA" -> all.filter { SubjectCatalog.isCa(it.question) }
            "GEN" -> all.filter { SubjectCatalog.isGen(it.question) }
            else -> all.filter { it.question.subject == sourceKey }
        }
        val targetDiff = difficultyFilter.difficulty ?: return bySource
        return bySource.filter { it.question.difficulty == targetDiff }
    }

    fun launchConfiguredTimedPractice() {
        val cfg = _timedConfig.value
        val pool = filteredQuestionsForSource(
            sourceKey = cfg.sourceKey,
            all = mainState.value.questions,
            difficultyFilter = cfg.difficultyFilter
        ).shuffled()
        if (pool.isEmpty()) {
            showMessage("No questions available for the selected subject and difficulty.")
            return
        }
        val picked = if (cfg.questionCount <= 0) pool else pool.take(cfg.questionCount)
        val sourceLabel = when (cfg.sourceKey) {
            "MIXED" -> "Mixed Set"
            "PYQ" -> "PYQs"
            "NURSING" -> "Nursing (70%)"
            "CA" -> "CA & Static GK"
            "GEN" -> "Sci/Math/Reas"
            else -> SubjectCatalog.displayNameFor(cfg.sourceKey)
        }
        val diffSuffix = cfg.difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
        _timedConfig.update { it.copy(isVisible = false) }
        startPractice(
            items = picked,
            title = "Timed: $sourceLabel$diffSuffix (${picked.size}Q)",
            timedMode = true,
            durationMinutes = cfg.durationMinutes
        )
    }

    // ------------------------------------------------------------------------
    // CBT MOCK SIMULATOR
    // ------------------------------------------------------------------------
    fun startCbtStandardMock(totalQuestions: Int, durationMinutes: Int, title: String) {
        val all = mainState.value.questions
        if (all.isEmpty()) return
        val nursingCount = (totalQuestions * 0.7).toInt()
        val nonNursingCount = totalQuestions - nursingCount
        val nurPool = all.filter { SubjectCatalog.isNursing(it.question) }.shuffled()
        val nonPool = all.filter { !SubjectCatalog.isNursing(it.question) }.shuffled()
        val selected = (nurPool.take(nursingCount) + nonPool.take(nonNursingCount)).toMutableList()
        if (selected.size < totalQuestions) {
            val usedIds = selected.map { it.id }.toSet()
            selected += all.filter { it.id !in usedIds }.shuffled().take(totalQuestions - selected.size)
        }
        startCbtCustom(selected, title, durationMinutes)
    }

    fun startCbtCustom(items: List<QuestionItem>, title: String, durationMinutes: Int? = null) {
        if (items.isEmpty()) {
            showMessage("No questions available for this CBT test.")
            return
        }
        stopActiveTimer()
        _practiceSession.value = null
        val mins = durationMinutes ?: maxOf(10, (items.size * 0.9).toInt())
        val initialStatuses = items.indices.associateWith { i ->
            if (i == 0) CbtPaletteStatus.NOT_ANSWERED else CbtPaletteStatus.NOT_VISITED
        }
        val totalSec = mins * 60
        _cbtSession.value = CbtSessionState(
            title = title,
            questions = items,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            paletteStatuses = initialStatuses,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        startCbtTimer()
    }

    private fun startCbtTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _cbtSession.value ?: break
                if (current.isSubmitted || current.showTimeoutDialog) break
                val nextSec = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (nextSec == 0) {
                    _cbtSession.update { it?.copy(remainingSeconds = 0, showTimeoutDialog = true) }
                    break
                } else {
                    _cbtSession.update { it?.copy(remainingSeconds = nextSec) }
                }
            }
        }
    }

    fun cbtSelectOption(optionIndex: Int) {
        _cbtSession.update { sess ->
            sess?.let {
                it.copy(selectedAnswers = it.selectedAnswers + (it.currentIndex to optionIndex))
            }
        }
    }

    fun cbtClearResponse() {
        _cbtSession.update { sess ->
            sess?.let {
                it.copy(
                    selectedAnswers = it.selectedAnswers - it.currentIndex,
                    paletteStatuses = it.paletteStatuses + (it.currentIndex to CbtPaletteStatus.NOT_ANSWERED)
                )
            }
        }
    }

    fun cbtMoveTo(index: Int) {
        _cbtSession.update { sess ->
            sess?.let {
                val safeIdx = index.coerceIn(0, it.questions.lastIndex.coerceAtLeast(0))
                val nextStatus = if (it.paletteStatuses[safeIdx] == CbtPaletteStatus.NOT_VISITED) {
                    it.paletteStatuses + (safeIdx to CbtPaletteStatus.NOT_ANSWERED)
                } else {
                    it.paletteStatuses
                }
                it.copy(currentIndex = safeIdx, paletteStatuses = nextStatus)
            }
        }
    }

    fun cbtSaveAndNext() {
        val sess = _cbtSession.value ?: return
        val hasAns = sess.selectedAnswers.containsKey(sess.currentIndex)
        val status = if (hasAns) CbtPaletteStatus.ANSWERED else CbtPaletteStatus.NOT_ANSWERED
        val updatedStatuses = sess.paletteStatuses + (sess.currentIndex to status)
        if (sess.currentIndex < sess.questions.lastIndex) {
            val nextIdx = sess.currentIndex + 1
            val finalStatuses = if (updatedStatuses[nextIdx] == CbtPaletteStatus.NOT_VISITED) {
                updatedStatuses + (nextIdx to CbtPaletteStatus.NOT_ANSWERED)
            } else {
                updatedStatuses
            }
            _cbtSession.value = sess.copy(
                currentIndex = nextIdx,
                paletteStatuses = finalStatuses
            )
        } else {
            _cbtSession.value = sess.copy(paletteStatuses = updatedStatuses, showSubmitConfirm = true)
        }
    }

    fun cbtMarkForReviewAndNext() {
        val sess = _cbtSession.value ?: return
        val hasAns = sess.selectedAnswers.containsKey(sess.currentIndex)
        val status = if (hasAns) CbtPaletteStatus.ANSWERED_MARKED else CbtPaletteStatus.MARKED
        val updatedStatuses = sess.paletteStatuses + (sess.currentIndex to status)
        if (sess.currentIndex < sess.questions.lastIndex) {
            val nextIdx = sess.currentIndex + 1
            val finalStatuses = if (updatedStatuses[nextIdx] == CbtPaletteStatus.NOT_VISITED) {
                updatedStatuses + (nextIdx to CbtPaletteStatus.NOT_ANSWERED)
            } else {
                updatedStatuses
            }
            _cbtSession.value = sess.copy(
                currentIndex = nextIdx,
                paletteStatuses = finalStatuses
            )
        } else {
            _cbtSession.value = sess.copy(paletteStatuses = updatedStatuses)
        }
    }

    fun requestCbtSubmitConfirm() {
        _cbtSession.update { it?.copy(showSubmitConfirm = true) }
    }

    fun dismissCbtSubmitConfirm() {
        _cbtSession.update { it?.copy(showSubmitConfirm = false) }
    }

    fun submitCbtExam() {
        val sess = _cbtSession.value ?: return
        if (sess.isSubmitted) return
        stopActiveTimer()

        var right = 0
        var wrong = 0
        var skipped = 0
        val batchResults = mutableListOf<Pair<String, Boolean>>()

        sess.questions.forEachIndexed { idx, qItem ->
            val chosen = sess.selectedAnswers[idx]
            if (chosen == null) {
                skipped++
            } else {
                val ok = chosen == qItem.question.correctIndex
                if (ok) right++ else wrong++
                batchResults.add(qItem.id to ok)
            }
        }

        val netScore = right - (wrong * 0.33)
        val attempted = right + wrong
        val acc = if (attempted > 0) ((right.toFloat() / attempted) * 100f).toInt() else 0

        _cbtSession.value = sess.copy(
            isSubmitted = true,
            showSubmitConfirm = false,
            showTimeoutDialog = false
        )

        viewModelScope.launch {
            repository.recordBatchAttempts(batchResults)
            repository.saveMockHistory(
                title = sess.title,
                totalQuestions = sess.questions.size,
                correctCount = right,
                wrongCount = wrong,
                skippedCount = skipped,
                netScore = netScore,
                accuracyPercent = acc
            )
        }
    }

    fun setCbtReviewFilter(filter: CbtReviewFilter) {
        _cbtSession.update { it?.copy(reviewFilter = filter) }
    }

    // ------------------------------------------------------------------------
    // BOOKMARKS & STATS MANAGEMENT
    // ------------------------------------------------------------------------
    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(questionId)
        }
    }

    fun clearAllStatistics() {
        viewModelScope.launch {
            repository.clearStatistics()
            showMessage("Attempt statistics cleared. Bookmarks preserved.")
        }
    }

    // ------------------------------------------------------------------------
    // BULK IMPORTER & BACKUP
    // ------------------------------------------------------------------------
    fun openImporter() {
        _importerState.update { it.copy(isVisible = true) }
    }

    fun closeImporter() {
        _importerState.update { it.copy(isVisible = false) }
    }

    fun updateImporter(transform: (ImporterUiState) -> ImporterUiState) {
        _importerState.update(transform)
    }

    fun previewPdfTextImport() {
        val st = _importerState.value
        val parsed = BulkQuestionParser.parseRawPdfText(
            rawText = st.rawText,
            category = st.category,
            subject = st.subject,
            shiftYear = st.shiftYear,
            defaultDifficulty = st.difficulty,
            defaultSourceNote = st.sourceNote,
            markAsVerified = st.markAsVerified
        )
        _importerState.update { it.copy(previewQuestions = parsed, hasPreviewed = true) }
    }

    fun commitPreviewImport() {
        val list = _importerState.value.previewQuestions
        if (list.isEmpty()) return
        viewModelScope.launch {
            repository.saveCustomQuestions(list)
            _importerState.update {
                it.copy(
                    isVisible = false,
                    rawText = "",
                    sourceNote = "",
                    markAsVerified = false,
                    previewQuestions = emptyList(),
                    hasPreviewed = false
                )
            }
            showMessage("Saved ${list.size} questions permanently offline!")
        }
    }

    fun importJsonBatch() {
        val raw = _importerState.value.jsonText
        val parsed = runCatching { BulkQuestionParser.parseJsonBatch(raw, markAsCustom = true) }
            .onFailure { showMessage("Invalid JSON format: ${it.message}") }
            .getOrNull() ?: return
        if (parsed.isEmpty()) {
            showMessage("No valid MCQs found in JSON array.")
            return
        }
        viewModelScope.launch {
            repository.saveCustomQuestions(parsed)
            _importerState.update { it.copy(isVisible = false, jsonText = "") }
            showMessage("Imported ${parsed.size} JSON questions!")
        }
    }

    fun buildBackupJson(): String = BulkQuestionParser.buildBackupJson(mainState.value.questions)

    fun restoreBackupFromText(jsonText: String) {
        viewModelScope.launch {
            runCatching { repository.restoreBackup(jsonText) }
                .onSuccess { count ->
                    _importerState.update { it.copy(isVisible = false) }
                    showMessage("Backup restored ($count custom questions + progress synced).")
                }
                .onFailure {
                    showMessage("Failed to restore backup: ${it.message}")
                }
        }
    }

    fun clearCustomQuestions() {
        viewModelScope.launch {
            repository.clearCustomQuestions()
            showMessage("Deleted all custom imported questions. Built-in MCQs remain safe.")
        }
    }

    // ------------------------------------------------------------------------
    // PRINTABLE PDF WORKSHEET EXPORT
    // ------------------------------------------------------------------------
    fun openPdfExport(
        defaultSource: String = "MIXED",
        defaultDifficulty: DifficultyFilter = _difficultyFilter.value,
        defaultLimit: Int = 30
    ) {
        _pdfExportConfig.value = PdfExportConfigState(
            isVisible = true,
            sourceKey = defaultSource,
            difficultyFilter = defaultDifficulty,
            questionLimit = defaultLimit
        )
    }

    fun closePdfExport() {
        _pdfExportConfig.update { it.copy(isVisible = false, isExporting = false) }
    }

    fun updatePdfExportConfig(transform: (PdfExportConfigState) -> PdfExportConfigState) {
        _pdfExportConfig.update(transform)
    }

    fun resolvedPdfWorksheetQuestions(
        config: PdfExportConfigState = _pdfExportConfig.value,
        state: DashboardDerivedState = mainState.value
    ): List<QuestionItem> = PdfWorksheetWriter.resolveWorksheetQuestions(
        sourceKey = config.sourceKey,
        all = state.questions,
        lowestAccuracy = state.lowestAccuracyQuestions,
        difficultyFilter = config.difficultyFilter,
        limit = config.questionLimit
    )

    fun suggestedPdfFileName(
        config: PdfExportConfigState = _pdfExportConfig.value,
        state: DashboardDerivedState = mainState.value
    ): String {
        val count = resolvedPdfWorksheetQuestions(config, state).size
        return PdfWorksheetWriter.buildSuggestedPdfFileName(
            sourceKey = config.sourceKey,
            difficultyFilter = config.difficultyFilter,
            questionCount = count,
            inlineAnswers = config.inlineAnswers
        )
    }

    fun exportWorksheetPdf(openStream: () -> OutputStream?) {
        val config = _pdfExportConfig.value
        val state = mainState.value
        val picked = resolvedPdfWorksheetQuestions(config, state)
        if (picked.isEmpty()) {
            showMessage("No questions match the selected PDF filter.")
            return
        }
        val sourceTitle = PdfWorksheetWriter.sourceDisplayName(config.sourceKey)
        val diffLabel = config.difficultyFilter.label
        val modeLabel = when {
            config.inlineAnswers -> "Study Revision Sheet (Answers & Rationales Inline)"
            config.includeAnswerKey -> "Mock Test Booklet (Answer Key Included)"
            else -> "Practice Question Sheet"
        }
        val options = PdfWorksheetOptions(
            title = "$sourceTitle Worksheet",
            subtitle = "Difficulty: $diffLabel  •  Format: $modeLabel",
            inlineAnswers = config.inlineAnswers,
            includeAnswerKey = config.includeAnswerKey
        )

        viewModelScope.launch {
            _pdfExportConfig.update { it.copy(isExporting = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = openStream() ?: error("Could not open destination PDF file.")
                    stream.use { out ->
                        PdfWorksheetWriter.writeWorksheetPdf(
                            questions = picked,
                            options = options,
                            outputStream = out
                        )
                    }
                }
            }
            result.onSuccess { pages ->
                _pdfExportConfig.update { it.copy(isVisible = false, isExporting = false) }
                showMessage("Exported ${picked.size} MCQs ($pages A4 pages) as PDF!")
            }.onFailure { err ->
                _pdfExportConfig.update { it.copy(isExporting = false) }
                showMessage("PDF export failed: ${err.message}")
            }
        }
    }

    private fun stopActiveTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopActiveTimer()
    }
}
