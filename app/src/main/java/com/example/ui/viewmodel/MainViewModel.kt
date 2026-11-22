package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudAccountSnapshot
import com.example.data.cloud.CloudCustomQuestion
import com.example.data.cloud.CloudMergeLogic
import com.example.data.cloud.CloudMockHistory
import com.example.data.cloud.CloudQuestionProgress
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.CloudSyncUiState
import com.example.data.cloud.CloudUserProfile
import com.example.data.cloud.SignedInAccountInfo
import com.example.data.cloud.SyncBadgeState
import com.example.data.cloud.extractFirestoreRootError
import com.example.data.cloud.isPermissionDeniedOrOffline
import com.example.data.export.PdfWorksheetOptions
import com.example.data.export.PdfWorksheetWriter
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.CategoryStat
import com.example.data.model.DifficultyFilter
import com.example.data.model.Exam
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamSection
import com.example.data.model.Flashcard
import com.example.data.model.MockHistory
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Profession
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionSourceType
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectStat
import com.example.data.model.Topic
import com.example.data.parser.BulkQuestionParser
import com.example.data.repository.NursingPrepRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    NON_TECH("Sci/Math 30%");

    fun labelFor(examId: String?): String = if (PlatformCatalogDefaults.isNorcetExam(examId)) {
        when (this) {
            NURSING -> "Nursing 80%"
            PYQ -> "NORCET PYQs"
            CA_GK -> "GK & Health"
            NON_TECH -> "Aptitude & Sci"
        }
    } else {
        label
    }
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
    val hasCountdown: Boolean = false,
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
    val hasCountdown: Boolean = true,
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
    val sourceType: QuestionSourceType = QuestionSourceType.MEMORY_BASED,
    val subject: String = "MSN",
    val topicName: String = "",
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
    val activeExam: Exam? = PlatformCatalogDefaults.DEFAULT_EXAMS.first(),
    val activeExamSections: List<ExamSection> = PlatformCatalogDefaults.DEFAULT_SECTIONS,
    val availableProfessions: List<Profession> = PlatformCatalogDefaults.DEFAULT_PROFESSIONS,
    val availableExams: List<Exam> = PlatformCatalogDefaults.DEFAULT_EXAMS,
    val examQuestionCounts: Map<String, Int> = emptyMap(),
    val topicsById: Map<String, Topic> = emptyMap(),
    val questions: List<QuestionItem> = emptyList(),
    val overall: OverallStats = OverallStats(),
    val subjectStats: List<SubjectStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val lowestAccuracyQuestions: List<QuestionItem> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val mockHistory: List<MockHistory> = emptyList(),
    val isLoaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: NursingPrepRepository,
    private val settings: SettingsPreferences,
    private val cloudSync: CloudSyncRepository,
    private val currentUserId: String,
    accountDisplayName: String = "",
    accountEmail: String = "",
    accountPhotoUrl: String? = null
) : ViewModel() {

    val signedInAccount: SignedInAccountInfo = SignedInAccountInfo(
        uid = currentUserId,
        displayName = accountDisplayName,
        email = accountEmail,
        photoUrl = accountPhotoUrl
    )

    val themePreference: StateFlow<ThemePreference> = settings.themePreference
    val negativeMarking: StateFlow<Boolean> = settings.negativeMarking
    val profileName: StateFlow<String> = settings.profileName
    val profileRole: StateFlow<String> = settings.profileRole
    val targetYear: StateFlow<String> = settings.targetYear
    val focusSubject: StateFlow<String> = settings.focusSubject
    val useTimeLimit: StateFlow<Boolean> = settings.useTimeLimit
    val secondsPerQuestion: StateFlow<Int> = settings.secondsPerQuestion
    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
    val activeExamId: StateFlow<String> = settings.activeExamId

    private val _cloudSyncState = MutableStateFlow(CloudSyncUiState())
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    private val _isSeeded = MutableStateFlow(false)
    private val _isExamPickerOpen = MutableStateFlow(false)
    val isExamPickerOpen: StateFlow<Boolean> = _isExamPickerOpen.asStateFlow()
    private val _isProfileOpen = MutableStateFlow(false)
    val isProfileOpen: StateFlow<Boolean> = _isProfileOpen.asStateFlow()

    private val allPlatformQuestions: StateFlow<List<QuestionItem>> = repository.allQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val allPlatformLinks: StateFlow<List<ExamQuestionLink>> = repository.allExamLinks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val activeExamQuestions = activeExamId.flatMapLatest { examId ->
        repository.observeQuestionsForExam(examId)
    }

    private val activeExamHistory = activeExamId.flatMapLatest { examId ->
        repository.observeMockHistoryForExam(examId)
    }

    private val activeExamSections = activeExamId.flatMapLatest { examId ->
        repository.observeExamSections(examId)
    }

    private data class CatalogBundle(
        val activeExamId: String,
        val professions: List<Profession>,
        val exams: List<Exam>,
        val sections: List<ExamSection>,
        val topics: List<Topic>,
        val links: List<ExamQuestionLink>
    )

    private val catalogBundle = combine(
        activeExamId,
        repository.professions,
        repository.exams,
        activeExamSections,
        repository.topics
    ) { examId, profs, exams, sections, topics ->
        CatalogBundle(
            activeExamId = examId,
            professions = profs,
            exams = exams,
            sections = sections,
            topics = topics,
            links = emptyList()
        )
    }.combine(allPlatformLinks) { bundle, links ->
        bundle.copy(links = links)
    }

    val mainState: StateFlow<DashboardDerivedState> = combine(
        activeExamQuestions,
        activeExamHistory,
        _flashcards,
        _isSeeded,
        catalogBundle
    ) { items, history, cards, seeded, catalog ->
        val attemptedUnique = items.count { it.attemptsCount > 0 }
        val totalAttempts = items.sumOf { it.attemptsCount }
        val totalCorrect = items.sumOf { it.correctCount }
        val totalWrong = items.sumOf { it.wrongCount }
        val acc = if (totalAttempts > 0) ((totalCorrect.toFloat() / totalAttempts) * 100f).toInt() else 0
        val latestRight = items.count { it.lastOutcome == "right" }
        val latestWrong = items.count { it.lastOutcome == "wrong" }
        val netScore = latestRight - (latestWrong * 0.33)
        val activeExam = catalog.exams.firstOrNull { it.id == catalog.activeExamId }
            ?: catalog.exams.firstOrNull()
            ?: PlatformCatalogDefaults.DEFAULT_EXAMS.first()
        val countsByExam = catalog.links.groupingBy { it.examId }.eachCount()
        DashboardDerivedState(
            activeExam = activeExam,
            activeExamSections = catalog.sections,
            availableProfessions = catalog.professions,
            availableExams = catalog.exams,
            examQuestionCounts = countsByExam,
            topicsById = catalog.topics.associateBy { it.id },
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
            categoryStats = repository.computeCategoryStats(items, activeExam.id),
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

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

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
    private var syncListenerJobs: List<Job> = emptyList()
    private var profilePushJob: Job? = null
    private var applyingCloudProfile = false
    private var isSigningOut = false

    init {
        viewModelScope.launch {
            val previousUid = settings.getLastSignedInUid()
            if (previousUid.isNotBlank() && previousUid != currentUserId) {
                repository.clearUserSpecificLocalDataForAccountSwitch()
            }
            settings.setLastSignedInUid(currentUserId)
            if (settings.profileName.value.isBlank() && signedInAccount.displayName.isNotBlank()) {
                settings.setProfileName(signedInAccount.displayName)
            }

            repository.ensureSeeded(settings.getSeededVersion()) { newVer ->
                settings.setSeededVersion(newVer)
            }
            _flashcards.value = repository.loadFlashcards()
            _isSeeded.value = true

            if (currentUserId.isNotBlank()) {
                performBidirectionalSync(showFeedbackToast = false)
                startRealtimeCloudObservers()
            }
        }
    }

    fun syncNow() {
        if (currentUserId.isBlank() || isSigningOut) return
        viewModelScope.launch {
            performBidirectionalSync(showFeedbackToast = true)
        }
    }

    fun detachCloudSyncForSignOut() {
        isSigningOut = true
        profilePushJob?.cancel()
        profilePushJob = null
        syncListenerJobs.forEach { it.cancel() }
        syncListenerJobs = emptyList()
    }

    private fun startRealtimeCloudObservers() {
        if (currentUserId.isBlank() || isSigningOut) return
        syncListenerJobs.forEach { it.cancel() }

        val profileJob = viewModelScope.launch {
            cloudSync.observeProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (!isSigningOut) {
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Profile sync stream interrupted", rootError)
                        recordSyncFailure(rootError)
                    }
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null && !isSigningOut) {
                        applyRemoteProfileToLocal(remoteProfile)
                        markSynced()
                    }
                }
        }

        val progressJob = viewModelScope.launch {
            cloudSync.observeProgress(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (!isSigningOut) {
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Progress sync stream interrupted", rootError)
                        recordSyncFailure(rootError)
                    }
                }
                .collect { remoteProgress ->
                    if (!isSigningOut && remoteProgress.isNotEmpty()) {
                        mergeCloudProgressIntoLocal(remoteProgress)
                        markSynced()
                    }
                }
        }

        val mockJob = viewModelScope.launch {
            cloudSync.observeMockHistory(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (!isSigningOut) {
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Mock history sync stream interrupted", rootError)
                        recordSyncFailure(rootError)
                    }
                }
                .collect { remoteMocks ->
                    if (!isSigningOut && remoteMocks.isNotEmpty()) {
                        mergeCloudMocksIntoLocal(remoteMocks)
                        markSynced()
                    }
                }
        }

        val customJob = viewModelScope.launch {
            cloudSync.observeCustomQuestions(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (!isSigningOut) {
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Custom questions sync stream interrupted", rootError)
                        recordSyncFailure(rootError)
                    }
                }
                .collect { remoteCustom ->
                    if (!isSigningOut && remoteCustom.isNotEmpty()) {
                        mergeCloudCustomQuestionsIntoLocal(remoteCustom)
                        markSynced()
                    }
                }
        }

        syncListenerJobs = listOf(profileJob, progressJob, mockJob, customJob)
    }

    private suspend fun performBidirectionalSync(showFeedbackToast: Boolean) {
        if (currentUserId.isBlank() || isSigningOut) return
        _cloudSyncState.update {
            it.copy(status = SyncBadgeState.SYNCING, detailMessage = "Syncing with your Google account...")
        }
        val snapshotResult = cloudSync.fetchAccountSnapshot(currentUserId)
        val snapshot = snapshotResult.getOrElse { err ->
            recordSyncFailure(err)
            if (showFeedbackToast) {
                showMessage("Offline or sync unavailable — local study data is safe.")
            }
            return
        }

        mergeSnapshotBidirectionally(snapshot)
        markSynced()
        if (showFeedbackToast) {
            showMessage("Synced with your Google account!")
        }
    }

    private suspend fun mergeSnapshotBidirectionally(snapshot: CloudAccountSnapshot) {
        // 1. Profile & Preferences
        if (snapshot.profile != null) {
            applyRemoteProfileToLocal(snapshot.profile)
        } else {
            pushCurrentProfileToCloud()
        }

        // 2. Question Progress & Bookmarks
        val localProgress = repository.getAllProgressSnapshot()
        val localMap = localProgress.associateBy { it.questionId }
        val cloudMap = snapshot.progress.associateBy { it.questionId.ifBlank { it.id } }

        mergeCloudProgressIntoLocal(snapshot.progress)

        val toPush = localProgress.filter { local ->
            CloudMergeLogic.shouldPushLocalProgress(local, cloudMap[local.questionId])
        }
        if (toPush.isNotEmpty()) {
            cloudSync.upsertProgressBatch(currentUserId, toPush).onFailure { recordSyncFailure(it) }
        }

        // 3. Mock History
        mergeCloudMocksIntoLocal(snapshot.mockHistory)
        val cloudMockKeys = snapshot.mockHistory.map { "${it.completedAtMs}_${it.examId}_${it.title}" }.toSet()
        val localMocks = repository.getAllMockHistorySnapshot()
        localMocks.forEach { localMock ->
            val key = "${localMock.timestamp}_${localMock.examId}_${localMock.title}"
            if (key !in cloudMockKeys) {
                cloudSync.upsertMockHistory(currentUserId, localMock).onFailure { recordSyncFailure(it) }
            }
        }

        // 4. Custom Questions
        mergeCloudCustomQuestionsIntoLocal(snapshot.customQuestions)
        val cloudCustomIds = snapshot.customQuestions.map { it.id }.toSet()
        val (localCustom, localExamMap) = repository.getCustomQuestionsWithExamIds()
        val missingInCloud = localCustom.filter { it.id !in cloudCustomIds }
        if (missingInCloud.isNotEmpty()) {
            cloudSync.upsertCustomQuestions(currentUserId, missingInCloud, localExamMap)
                .onFailure { recordSyncFailure(it) }
        }
    }

    private fun applyRemoteProfileToLocal(remote: CloudUserProfile) {
        applyingCloudProfile = true
        try {
            val resolvedName = remote.displayName.ifBlank { signedInAccount.displayName }
            settings.applySyncedProfile(
                displayName = resolvedName,
                role = remote.role,
                targetYear = remote.targetYear.ifBlank { SettingsPreferences.DEFAULT_TARGET_YEAR },
                focusSubject = remote.focusSubject,
                activeExamId = remote.activeExamId,
                themePreference = CloudMergeLogic.parseThemeSafe(remote.themePreference),
                negativeMarking = remote.negativeMarking,
                useTimeLimit = remote.useTimeLimit,
                secondsPerQuestion = remote.secondsPerQuestion.toInt(),
                keepScreenOn = remote.keepScreenOn
            )
        } finally {
            applyingCloudProfile = false
        }
    }

    private suspend fun mergeCloudProgressIntoLocal(remoteList: List<CloudQuestionProgress>) {
        if (remoteList.isEmpty()) return
        val localMap = repository.getAllProgressSnapshot().associateBy { it.questionId }
        val mergedUpdates = remoteList.mapNotNull { cloud ->
            val qId = cloud.questionId.ifBlank { cloud.id }
            if (qId.isBlank()) return@mapNotNull null
            val local = localMap[qId]
            val merged = CloudMergeLogic.mergeProgress(local, cloud)
            if (merged != local) merged else null
        }
        if (mergedUpdates.isNotEmpty()) {
            repository.upsertProgressList(mergedUpdates)
        }
    }

    private suspend fun mergeCloudMocksIntoLocal(remoteMocks: List<CloudMockHistory>) {
        remoteMocks.forEach { cloud ->
            repository.insertMockHistoryIfMissing(CloudMergeLogic.toLocalMockHistory(cloud))
        }
    }

    private suspend fun mergeCloudCustomQuestionsIntoLocal(remoteCustom: List<CloudCustomQuestion>) {
        if (remoteCustom.isEmpty()) return
        val domainQuestions = remoteCustom.mapNotNull { CloudMergeLogic.toDomainQuestion(it) }
        if (domainQuestions.isEmpty()) return
        val examMap = remoteCustom.associate { it.id to it.examIds }
        repository.saveCustomQuestions(
            questions = domainQuestions,
            examIds = listOf(settings.activeExamId.value),
            customExamMap = examMap
        )
    }

    private fun scheduleProfileCloudPush() {
        if (applyingCloudProfile || currentUserId.isBlank() || isSigningOut) return
        profilePushJob?.cancel()
        profilePushJob = viewModelScope.launch {
            delay(650L)
            pushCurrentProfileToCloud()
        }
    }

    private suspend fun pushCurrentProfileToCloud() {
        if (currentUserId.isBlank() || isSigningOut) return
        val result = cloudSync.upsertProfile(
            userId = currentUserId,
            displayName = settings.profileName.value,
            role = settings.profileRole.value,
            targetYear = settings.targetYear.value,
            focusSubject = settings.focusSubject.value,
            activeExamId = settings.activeExamId.value,
            themePreference = settings.themePreference.value.name,
            negativeMarking = settings.negativeMarking.value,
            useTimeLimit = settings.useTimeLimit.value,
            secondsPerQuestion = settings.secondsPerQuestion.value,
            keepScreenOn = settings.keepScreenOn.value
        )
        result.onSuccess { markSynced() }.onFailure { recordSyncFailure(it) }
    }

    private fun markSynced() {
        _cloudSyncState.value = CloudSyncUiState(
            status = SyncBadgeState.SYNCED,
            lastSyncedAtMs = System.currentTimeMillis(),
            detailMessage = "All study progress backed up to cloud"
        )
    }

    private fun recordSyncFailure(error: Throwable) {
        val root = extractFirestoreRootError(error)
        val offline = isPermissionDeniedOrOffline(root)
        _cloudSyncState.update { prev ->
            prev.copy(
                status = if (offline) SyncBadgeState.OFFLINE else SyncBadgeState.ERROR,
                detailMessage = if (offline) {
                    "Offline — saved locally, will sync when online"
                } else {
                    root.localizedMessage ?: "Cloud sync paused — local data safe"
                }
            )
        }
    }

    fun selectTab(tab: MainTab) {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        _selectedTab.value = tab
    }

    fun openSettings() {
        _isProfileOpen.value = false
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openProfile() {
        _isSettingsOpen.value = false
        _isProfileOpen.value = true
    }

    fun closeProfile() {
        _isProfileOpen.value = false
    }

    fun openExamPicker() {
        _isExamPickerOpen.value = true
    }

    fun closeExamPicker() {
        _isExamPickerOpen.value = false
    }

    fun selectActiveExam(examId: String) {
        val changed = settings.activeExamId.value != examId
        settings.setActiveExamId(examId)
        _isExamPickerOpen.value = false
        if (changed) {
            scheduleProfileCloudPush()
            stopActiveTimer()
            _practiceSession.value = null
            _cbtSession.value = null
            val targetName = mainState.value.availableExams.firstOrNull { it.id == examId }?.shortName
                ?: PlatformCatalogDefaults.DEFAULT_EXAMS.firstOrNull { it.id == examId }?.shortName
            if (targetName != null) {
                showMessage("Switched exam track to $targetName")
            }
        }
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
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        _subjectHubSection.value = section
        _selectedTab.value = MainTab.SUBJECTS
    }

    fun toggleTheme(currentlyDark: Boolean) {
        settings.cycleTheme(currentlyDark)
        scheduleProfileCloudPush()
    }

    fun setThemePreference(preference: ThemePreference) {
        settings.setThemePreference(preference)
        scheduleProfileCloudPush()
    }

    fun setProfileName(name: String) {
        settings.setProfileName(name)
        scheduleProfileCloudPush()
    }

    fun setProfileRole(role: String) {
        settings.setProfileRole(role)
        scheduleProfileCloudPush()
    }

    fun setTargetYear(year: String) {
        settings.setTargetYear(year)
        scheduleProfileCloudPush()
    }

    fun setFocusSubject(subjectCode: String) {
        settings.setFocusSubject(subjectCode)
        scheduleProfileCloudPush()
    }

    fun setUseTimeLimit(enabled: Boolean) {
        settings.setUseTimeLimit(enabled)
        scheduleProfileCloudPush()
    }

    fun setSecondsPerQuestion(seconds: Int) {
        settings.setSecondsPerQuestion(seconds)
        scheduleProfileCloudPush()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        settings.setKeepScreenOn(enabled)
        scheduleProfileCloudPush()
    }

    fun setNegativeMarking(enabled: Boolean) {
        settings.setNegativeMarking(enabled)
        scheduleProfileCloudPush()
    }

    fun toggleNegativeMarking() {
        settings.setNegativeMarking(!settings.negativeMarking.value)
        scheduleProfileCloudPush()
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
        durationMinutes: Int = 30,
        hasCountdown: Boolean = timedMode
    ) {
        if (items.isEmpty()) {
            showMessage("No questions found in this selection yet.")
            return
        }
        stopActiveTimer()
        _cbtSession.value = null
        val actualCountdown = timedMode && hasCountdown && durationMinutes > 0
        val totalSec = if (actualCountdown) durationMinutes.coerceAtLeast(1) * 60 else 0
        _practiceSession.value = PracticeSessionState(
            title = title,
            questions = items,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            timedMode = timedMode,
            hasCountdown = actualCountdown,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        if (actualCountdown) {
            startPracticeTimer()
        }
    }

    private fun startPracticeTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _practiceSession.value ?: break
                if (!current.timedMode || !current.hasCountdown || current.isSubmittedSummary || current.showTimeoutDialog) break
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
            val savedProgress = repository.recordAttempt(qItem.id, isCorrect)
            if (currentUserId.isNotBlank() && !isSigningOut) {
                cloudSync.upsertProgress(currentUserId, savedProgress)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
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
    private fun suggestedTimedPracticeMinutes(
        questionCount: Int,
        sourceKey: String,
        difficultyFilter: DifficultyFilter
    ): Int {
        val effectiveCount = if (questionCount <= 0) {
            filteredQuestionsForSource(sourceKey, mainState.value.questions, difficultyFilter).size
        } else {
            questionCount
        }
        val rawMinutes = SettingsPreferences.calculateSuggestedMinutes(
            questionCount = effectiveCount,
            secondsPerQuestion = settings.secondsPerQuestion.value
        )
        return if (rawMinutes <= 0) 5 else rawMinutes.coerceIn(5, 120)
    }

    fun openTimedPracticeConfig(
        defaultSource: String = "MIXED",
        defaultCount: Int = 30,
        defaultDifficulty: DifficultyFilter = _difficultyFilter.value
    ) {
        val defaultMins = suggestedTimedPracticeMinutes(defaultCount, defaultSource, defaultDifficulty)
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
        _timedConfig.update { cfg ->
            val updatedMins = if (cfg.questionCount <= 0) {
                suggestedTimedPracticeMinutes(cfg.questionCount, sourceKey, cfg.difficultyFilter)
            } else {
                cfg.durationMinutes
            }
            cfg.copy(sourceKey = sourceKey, durationMinutes = updatedMins)
        }
    }

    fun updateTimedConfigDifficulty(filter: DifficultyFilter) {
        _timedConfig.update { cfg ->
            val updatedMins = if (cfg.questionCount <= 0) {
                suggestedTimedPracticeMinutes(cfg.questionCount, cfg.sourceKey, filter)
            } else {
                cfg.durationMinutes
            }
            cfg.copy(difficultyFilter = filter, durationMinutes = updatedMins)
        }
    }

    fun updateTimedConfigCount(count: Int) {
        _timedConfig.update { cfg ->
            val suggestedMinutes = suggestedTimedPracticeMinutes(count, cfg.sourceKey, cfg.difficultyFilter)
            cfg.copy(questionCount = count, durationMinutes = suggestedMinutes)
        }
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
        val activeExamId = settings.activeExamId.value
        val nursingPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExamId)
        val sourceLabel = when (cfg.sourceKey) {
            "MIXED" -> "Mixed Set"
            "PYQ" -> if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "NORCET PYQs" else "PYQs"
            "NURSING" -> "Nursing ($nursingPct%)"
            "CA" -> if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "GK & Health" else "CA & Static GK"
            "GEN" -> if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "Aptitude & Science" else "Sci/Math/Reas"
            else -> SubjectCatalog.displayNameFor(cfg.sourceKey)
        }
        val diffSuffix = cfg.difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
        val useLimit = settings.useTimeLimit.value
        val titlePrefix = if (useLimit) "Timed" else "Drill"
        _timedConfig.update { it.copy(isVisible = false) }
        startPractice(
            items = picked,
            title = "$titlePrefix: $sourceLabel$diffSuffix (${picked.size}Q)",
            timedMode = true,
            durationMinutes = if (useLimit) cfg.durationMinutes else 0,
            hasCountdown = useLimit
        )
    }

    // ------------------------------------------------------------------------
    // CBT MOCK SIMULATOR
    // ------------------------------------------------------------------------
    fun startCbtStandardMock(
        totalQuestions: Int,
        durationMinutes: Int?,
        title: String,
        isOfficialFullMock: Boolean = false
    ) {
        val all = mainState.value.questions
        if (all.isEmpty()) return
        val nursingPct = PlatformCatalogDefaults.nursingWeightagePercent(settings.activeExamId.value)
        val nursingCount = (totalQuestions * nursingPct) / 100
        val nonNursingCount = totalQuestions - nursingCount
        val nurPool = all.filter { SubjectCatalog.isNursing(it.question) }.shuffled()
        val nonPool = all.filter { !SubjectCatalog.isNursing(it.question) }.shuffled()
        val selected = (nurPool.take(nursingCount) + nonPool.take(nonNursingCount)).toMutableList()
        if (selected.size < totalQuestions) {
            val usedIds = selected.map { it.id }.toSet()
            selected += all.filter { it.id !in usedIds }.shuffled().take(totalQuestions - selected.size)
        }
        startCbtCustom(
            items = selected,
            title = title,
            durationMinutes = durationMinutes,
            isOfficialFullMock = isOfficialFullMock
        )
    }

    fun startNorcetMainsMock() {
        val all = mainState.value.questions
        if (all.isEmpty()) return
        val nursingPool = all.filter { SubjectCatalog.isNursing(it.question) }
        // Prioritize clinical scenario & Hard nursing questions first for authentic NORCET Stage II Mains
        val mainsPriority = nursingPool.filter {
            it.id.startsWith("N_") || it.question.difficulty == QuestionDifficulty.HARD
        }.shuffled()
        val remainingNursing = nursingPool.filter { q -> mainsPriority.none { it.id == q.id } }.shuffled()
        val selected = (mainsPriority + remainingNursing).take(160)
        startCbtCustom(
            items = selected,
            title = "NORCET Stage II Mains (160Q Clinical CBT)",
            durationMinutes = 180,
            isOfficialFullMock = false
        )
    }

    fun startCbtCustom(
        items: List<QuestionItem>,
        title: String,
        durationMinutes: Int? = null,
        isOfficialFullMock: Boolean = false
    ) {
        if (items.isEmpty()) {
            showMessage("No questions available for this CBT test.")
            return
        }
        stopActiveTimer()
        _practiceSession.value = null
        val activeExam = mainState.value.activeExam
        val hasCountdown = isOfficialFullMock || settings.useTimeLimit.value
        val totalSec = if (hasCountdown) {
            val mins = if (isOfficialFullMock) {
                activeExam?.defaultMockMinutes ?: 90
            } else {
                durationMinutes ?: SettingsPreferences.calculateSuggestedMinutes(
                    questionCount = items.size,
                    secondsPerQuestion = settings.secondsPerQuestion.value
                ).coerceAtLeast(1)
            }
            mins * 60
        } else {
            0
        }
        val initialStatuses = items.indices.associateWith { i ->
            if (i == 0) CbtPaletteStatus.NOT_ANSWERED else CbtPaletteStatus.NOT_VISITED
        }
        _cbtSession.value = CbtSessionState(
            title = title,
            questions = items,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            paletteStatuses = initialStatuses,
            hasCountdown = hasCountdown,
            totalSeconds = totalSec,
            remainingSeconds = totalSec
        )
        if (hasCountdown) {
            startCbtTimer()
        }
    }

    private fun startCbtTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _cbtSession.value ?: break
                if (!current.hasCountdown || current.isSubmitted || current.showTimeoutDialog) break
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
            val savedProgress = repository.recordBatchAttempts(batchResults)
            val savedMock = repository.saveMockHistory(
                title = sess.title,
                totalQuestions = sess.questions.size,
                correctCount = right,
                wrongCount = wrong,
                skippedCount = skipped,
                netScore = netScore,
                accuracyPercent = acc,
                examId = settings.activeExamId.value
            )
            if (currentUserId.isNotBlank() && !isSigningOut) {
                cloudSync.upsertProgressBatch(currentUserId, savedProgress)
                    .onFailure { recordSyncFailure(it) }
                cloudSync.upsertMockHistory(currentUserId, savedMock)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
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
            val saved = repository.toggleBookmark(questionId)
            if (currentUserId.isNotBlank() && !isSigningOut) {
                cloudSync.upsertProgress(currentUserId, saved)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
        }
    }

    fun clearAllStatistics() {
        viewModelScope.launch {
            val resetList = repository.clearStatistics()
            if (currentUserId.isNotBlank() && !isSigningOut && resetList.isNotEmpty()) {
                cloudSync.upsertProgressBatch(currentUserId, resetList)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
            showMessage("Attempt statistics cleared. Bookmarks preserved.")
        }
    }

    // ------------------------------------------------------------------------
    // BULK IMPORTER & BACKUP
    // ------------------------------------------------------------------------
    fun openImporter() {
        val defaultShift = if (PlatformCatalogDefaults.isNorcetExam(settings.activeExamId.value)) {
            "NORCET-9"
        } else {
            "2025-S1"
        }
        _importerState.update {
            it.copy(
                isVisible = true,
                shiftYear = if (it.rawText.isBlank()) defaultShift else it.shiftYear
            )
        }
    }

    fun closeImporter() {
        _importerState.update { it.copy(isVisible = false) }
    }

    fun updateImporter(transform: (ImporterUiState) -> ImporterUiState) {
        _importerState.update(transform)
    }

    fun previewPdfTextImport() {
        val st = _importerState.value
        val effectiveTopicId = BulkQuestionParser.slugTopicId(st.subject, st.topicName)
        val parsed = BulkQuestionParser.parseRawPdfText(
            rawText = st.rawText,
            category = st.category,
            subject = st.subject,
            shiftYear = st.shiftYear,
            defaultDifficulty = st.difficulty,
            defaultSourceNote = st.sourceNote,
            markAsVerified = st.markAsVerified,
            defaultSourceType = st.sourceType,
            defaultTopicId = effectiveTopicId
        )
        _importerState.update { it.copy(previewQuestions = parsed, hasPreviewed = true) }
    }

    fun commitPreviewImport() {
        val st = _importerState.value
        val list = st.previewQuestions
        if (list.isEmpty()) return
        val topicLabelOverrides = if (st.topicName.isNotBlank()) {
            mapOf(BulkQuestionParser.slugTopicId(st.subject, st.topicName) to st.topicName.trim())
        } else {
            emptyMap()
        }
        viewModelScope.launch {
            val activeExam = settings.activeExamId.value
            repository.saveCustomQuestions(
                questions = list,
                examIds = listOf(activeExam),
                topicLabelOverrides = topicLabelOverrides
            )
            if (currentUserId.isNotBlank() && !isSigningOut) {
                val examMap = list.associate { it.id to listOf(activeExam) }
                cloudSync.upsertCustomQuestions(currentUserId, list, examMap)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
            _importerState.update {
                it.copy(
                    isVisible = false,
                    rawText = "",
                    topicName = "",
                    sourceNote = "",
                    markAsVerified = false,
                    previewQuestions = emptyList(),
                    hasPreviewed = false
                )
            }
            showMessage("Saved ${list.size} questions offline & synced to cloud!")
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
            val activeExam = settings.activeExamId.value
            repository.saveCustomQuestions(
                questions = parsed,
                examIds = listOf(activeExam)
            )
            if (currentUserId.isNotBlank() && !isSigningOut) {
                val examMap = parsed.associate { it.id to listOf(activeExam) }
                cloudSync.upsertCustomQuestions(currentUserId, parsed, examMap)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
            _importerState.update { it.copy(isVisible = false, jsonText = "") }
            showMessage("Imported ${parsed.size} JSON questions!")
        }
    }

    fun buildBackupJson(): String {
        val backupItems = allPlatformQuestions.value.ifEmpty { mainState.value.questions }
        val linksByQuestion = allPlatformLinks.value
            .groupBy { it.questionId }
            .mapValues { (_, links) -> links.map { it.examId }.distinct() }
        return BulkQuestionParser.buildBackupJson(
            items = backupItems,
            questionExamLinks = linksByQuestion,
            activeExamId = settings.activeExamId.value
        )
    }

    fun restoreBackupFromText(jsonText: String) {
        viewModelScope.launch {
            runCatching { repository.restoreBackup(jsonText, defaultExamId = settings.activeExamId.value) }
                .onSuccess { count ->
                    _importerState.update { it.copy(isVisible = false) }
                    if (currentUserId.isNotBlank() && !isSigningOut) {
                        performBidirectionalSync(showFeedbackToast = false)
                    }
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
            if (currentUserId.isNotBlank() && !isSigningOut) {
                cloudSync.deleteAllCustomQuestions(currentUserId)
                    .onSuccess { markSynced() }
                    .onFailure { recordSyncFailure(it) }
            }
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
        detachCloudSyncForSignOut()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
