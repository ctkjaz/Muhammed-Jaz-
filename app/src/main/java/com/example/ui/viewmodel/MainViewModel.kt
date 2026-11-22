package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.BackupFolderSummary
import com.example.data.backup.BackupStorageManager
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
import com.example.data.update.AppUpdateChecker
import com.example.data.update.AppUpdateStatus
import com.example.data.update.AppUpdateUiState
import com.example.data.model.AccountSessionLogic
import com.example.data.model.CategoryStat
import com.example.data.model.DailyStudyGoalState
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
import com.example.data.model.SessionCheckResult
import com.example.data.model.StudyGoalLogic
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
    CBT("CBT"),
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
    val reviewFilter: CbtReviewFilter = CbtReviewFilter.ALL,
    val sectionSize: Int = 0,
    val sectionDurationSeconds: Int = 0,
    val sectionLabels: List<String> = emptyList(),
    val currentSectionIndex: Int = 0,
    val sectionRemainingSeconds: Int = 0,
    val showSectionAdvanceConfirm: Boolean = false,
    val sectionAutoAdvanceNotice: String? = null
) {
    val hasSectionLock: Boolean
        get() = sectionSize > 0 && sectionDurationSeconds > 0 && questions.size > sectionSize

    val totalSections: Int
        get() = if (!hasSectionLock) 1 else ((questions.size + sectionSize - 1) / sectionSize)

    val activeSectionRange: IntRange
        get() {
            if (!hasSectionLock) return questions.indices
            val start = (currentSectionIndex * sectionSize).coerceIn(0, questions.lastIndex.coerceAtLeast(0))
            val end = ((currentSectionIndex + 1) * sectionSize - 1).coerceAtMost(questions.lastIndex.coerceAtLeast(0))
            return start..end
        }

    val currentSectionLabel: String
        get() = sectionLabels.getOrNull(currentSectionIndex)
            ?: "Section ${currentSectionIndex + 1} of $totalSections"

    val isLastSection: Boolean
        get() = !hasSectionLock || currentSectionIndex >= totalSections - 1

    fun isIndexInActiveSection(index: Int): Boolean =
        !hasSectionLock || index in activeSectionRange
}

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
    accountPhotoUrl: String? = null,
    private val backupStorage: BackupStorageManager? = null
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
    val phoneNumber: StateFlow<String> = settings.phoneNumber
    val profileAvatarPath: StateFlow<String> = settings.profileAvatarPath
    val useTimeLimit: StateFlow<Boolean> = settings.useTimeLimit
    val secondsPerQuestion: StateFlow<Int> = settings.secondsPerQuestion
    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
    val activeExamId: StateFlow<String> = settings.activeExamId
    val onboardingCompleted: StateFlow<Boolean> = settings.onboardingCompleted
    val studyGoalState: StateFlow<DailyStudyGoalState> = settings.studyGoalState

    val currentDeviceName: String = AccountSessionLogic.buildDeviceLabel()

    private val _forcedSignOutEvent = MutableStateFlow<String?>(null)
    val forcedSignOutEvent: StateFlow<String?> = _forcedSignOutEvent.asStateFlow()

    private val _cloudSyncState = MutableStateFlow(CloudSyncUiState())
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private val _backupFolderSummary = MutableStateFlow(BackupFolderSummary())
    val backupFolderSummary: StateFlow<BackupFolderSummary> = _backupFolderSummary.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _appUpdateState = MutableStateFlow(
        AppUpdateUiState(currentVersion = BuildConfig.VERSION_NAME)
    )
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

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
    private var localSessionId: String = ""
    private var sessionGuardArmed = false
    private var pendingSessionClaimRetry = false

    init {
        viewModelScope.launch {
            val previousUid = settings.getLastSignedInUid()
            if (previousUid.isNotBlank() && previousUid != currentUserId) {
                repository.clearUserSpecificLocalDataForAccountSwitch()
                settings.resetGoalStateForAccountSwitch()
                settings.clearLocalSession()
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
                val allowed = initializeSingleDeviceSession()
                if (allowed && !isSigningOut) {
                    performBidirectionalSync(showFeedbackToast = false)
                    startRealtimeCloudObservers()
                }
            }
            refreshBackupFolderSummary()
            checkForAppUpdates(userInitiated = false)
        }
    }

    private suspend fun initializeSingleDeviceSession(): Boolean {
        val isFreshSignIn = settings.consumeFreshSignInPending()
        val storedUid = settings.getLocalSessionUid()
        val storedSessionId = settings.getLocalSessionId()

        val mustClaim = isFreshSignIn || storedUid != currentUserId || storedSessionId.isBlank()
        if (mustClaim) {
            val newId = AccountSessionLogic.generateSessionId()
            localSessionId = newId
            settings.saveLocalSession(currentUserId, newId)
            val claimResult = claimCurrentDeviceSessionOnCloud(newId)
            if (claimResult.isSuccess) {
                sessionGuardArmed = true
                pendingSessionClaimRetry = false
            } else {
                pendingSessionClaimRetry = true
            }
            return true
        }

        // Cold restart on an already signed-in device: verify against live Firestore server
        localSessionId = storedSessionId
        val serverProfileResult = cloudSync.fetchProfileFromServer(currentUserId)
        val serverProfile = serverProfileResult.getOrNull()
        if (serverProfileResult.isSuccess) {
            when (val verdict = AccountSessionLogic.evaluateRemoteSession(
                localSessionId = localSessionId,
                remoteSessionId = serverProfile?.activeSessionId,
                remoteDeviceName = serverProfile?.activeDeviceName
            )) {
                is SessionCheckResult.Revoked -> {
                    triggerForcedSignOut(verdict.activeDeviceName)
                    return false
                }
                SessionCheckResult.ClaimNeeded -> {
                    val claimResult = claimCurrentDeviceSessionOnCloud(localSessionId)
                    pendingSessionClaimRetry = claimResult.isFailure
                    sessionGuardArmed = claimResult.isSuccess
                }
                SessionCheckResult.Valid -> {
                    sessionGuardArmed = true
                    pendingSessionClaimRetry = false
                }
            }
        } else {
            // Offline cold start: allow local study, arm guard so reconnection immediately enforces lock
            sessionGuardArmed = true
        }
        return true
    }

    fun verifyActiveSessionOnResume() {
        if (currentUserId.isBlank() || isSigningOut) return
        viewModelScope.launch {
            if (pendingSessionClaimRetry && localSessionId.isNotBlank()) {
                val retry = claimCurrentDeviceSessionOnCloud(localSessionId)
                if (retry.isSuccess) {
                    pendingSessionClaimRetry = false
                    sessionGuardArmed = true
                }
                return@launch
            }
            if (!sessionGuardArmed || localSessionId.isBlank()) return@launch

            val serverProfileResult = cloudSync.fetchProfileFromServer(currentUserId)
            if (serverProfileResult.isSuccess && !isSigningOut) {
                val remote = serverProfileResult.getOrNull()
                when (val verdict = AccountSessionLogic.evaluateRemoteSession(
                    localSessionId = localSessionId,
                    remoteSessionId = remote?.activeSessionId,
                    remoteDeviceName = remote?.activeDeviceName
                )) {
                    is SessionCheckResult.Revoked -> triggerForcedSignOut(verdict.activeDeviceName)
                    SessionCheckResult.ClaimNeeded -> claimCurrentDeviceSessionOnCloud(localSessionId)
                    SessionCheckResult.Valid -> Unit
                }
            }
        }
    }

    private suspend fun claimCurrentDeviceSessionOnCloud(sessionId: String): Result<Unit> {
        val goal = settings.studyGoalState.value
        return cloudSync.claimActiveSession(
            userId = currentUserId,
            sessionId = sessionId,
            deviceName = currentDeviceName,
            displayName = settings.profileName.value,
            role = settings.profileRole.value,
            targetYear = settings.targetYear.value,
            focusSubject = settings.focusSubject.value,
            phoneNumber = settings.phoneNumber.value,
            activeExamId = settings.activeExamId.value,
            themePreference = settings.themePreference.value.name,
            negativeMarking = settings.negativeMarking.value,
            useTimeLimit = settings.useTimeLimit.value,
            secondsPerQuestion = settings.secondsPerQuestion.value,
            keepScreenOn = settings.keepScreenOn.value,
            onboardingCompleted = settings.onboardingCompleted.value,
            dailyGoalQuestions = goal.dailyGoalQuestions,
            currentStreakDays = goal.currentStreakDays,
            longestStreakDays = goal.longestStreakDays,
            lastGoalMetDate = goal.lastGoalMetDate,
            todayStudyDate = goal.todayStudyDate,
            todayAnsweredCount = goal.todayAnsweredCount
        )
    }

    private fun triggerForcedSignOut(otherDeviceName: String) {
        if (isSigningOut) return
        val notice = AccountSessionLogic.buildForcedSignOutMessage(otherDeviceName)
        settings.setForcedSignOutNotice(notice)
        detachCloudSyncForSignOut(isForcedByOtherDevice = true)
        _forcedSignOutEvent.value = notice
    }

    fun syncNow() {
        if (currentUserId.isBlank() || isSigningOut) return
        viewModelScope.launch {
            if (pendingSessionClaimRetry && localSessionId.isNotBlank()) {
                val retry = claimCurrentDeviceSessionOnCloud(localSessionId)
                if (retry.isSuccess) {
                    pendingSessionClaimRetry = false
                    sessionGuardArmed = true
                }
            }
            performBidirectionalSync(showFeedbackToast = true)
        }
    }

    fun detachCloudSyncForSignOut(isForcedByOtherDevice: Boolean = false) {
        isSigningOut = true
        sessionGuardArmed = false
        stopActiveTimer()
        profilePushJob?.cancel()
        profilePushJob = null
        syncListenerJobs.forEach { it.cancel() }
        syncListenerJobs = emptyList()
        settings.clearLocalSession()
        if (!isForcedByOtherDevice) {
            settings.markManualSignOut()
            settings.setForcedSignOutNotice(null)
        }
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
                        if (sessionGuardArmed && localSessionId.isNotBlank()) {
                            val verdict = AccountSessionLogic.evaluateRemoteSession(
                                localSessionId = localSessionId,
                                remoteSessionId = remoteProfile.activeSessionId,
                                remoteDeviceName = remoteProfile.activeDeviceName
                            )
                            if (verdict is SessionCheckResult.Revoked) {
                                triggerForcedSignOut(verdict.activeDeviceName)
                                return@collect
                            }
                        }
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
            val resolvedOnboarding = when {
                remote.onboardingCompleted == true -> true
                settings.onboardingCompleted.value -> true
                else -> remote.onboardingCompleted
            }
            val hasRemoteGoalData = remote.dailyGoalQuestions != null ||
                remote.currentStreakDays > 0L ||
                remote.longestStreakDays > 0L ||
                remote.todayAnsweredCount > 0L ||
                remote.lastGoalMetDate.isNotBlank()
            val mergedGoal = if (hasRemoteGoalData) {
                val remoteGoal = DailyStudyGoalState(
                    dailyGoalQuestions = (remote.dailyGoalQuestions?.toInt()
                        ?: settings.studyGoalState.value.dailyGoalQuestions)
                        .coerceIn(StudyGoalLogic.MIN_DAILY_GOAL, StudyGoalLogic.MAX_DAILY_GOAL),
                    todayStudyDate = StudyGoalLogic.sanitizeDateKey(remote.todayStudyDate),
                    todayAnsweredCount = remote.todayAnsweredCount.toInt().coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                    currentStreakDays = remote.currentStreakDays.toInt().coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                    longestStreakDays = remote.longestStreakDays.toInt().coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                    lastGoalMetDate = StudyGoalLogic.sanitizeDateKey(remote.lastGoalMetDate)
                )
                StudyGoalLogic.mergeCloudGoalState(
                    local = settings.studyGoalState.value,
                    remote = remoteGoal
                )
            } else {
                settings.studyGoalState.value
            }

            val resolvedPhone = remote.phoneNumber.takeIf { it.isNotBlank() }
                ?: settings.phoneNumber.value

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
                keepScreenOn = remote.keepScreenOn,
                onboardingCompleted = resolvedOnboarding,
                mergedGoalState = mergedGoal,
                phoneNumber = resolvedPhone
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
        val goal = settings.studyGoalState.value
        val result = cloudSync.upsertProfile(
            userId = currentUserId,
            displayName = settings.profileName.value,
            role = settings.profileRole.value,
            targetYear = settings.targetYear.value,
            focusSubject = settings.focusSubject.value,
            phoneNumber = settings.phoneNumber.value,
            activeExamId = settings.activeExamId.value,
            themePreference = settings.themePreference.value.name,
            negativeMarking = settings.negativeMarking.value,
            useTimeLimit = settings.useTimeLimit.value,
            secondsPerQuestion = settings.secondsPerQuestion.value,
            keepScreenOn = settings.keepScreenOn.value,
            onboardingCompleted = settings.onboardingCompleted.value,
            dailyGoalQuestions = goal.dailyGoalQuestions,
            currentStreakDays = goal.currentStreakDays,
            longestStreakDays = goal.longestStreakDays,
            lastGoalMetDate = goal.lastGoalMetDate,
            todayStudyDate = goal.todayStudyDate,
            todayAnsweredCount = goal.todayAnsweredCount,
            initialActiveSessionId = localSessionId,
            initialActiveDeviceName = currentDeviceName
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

    fun setPhoneNumber(phone: String) {
        settings.setPhoneNumber(phone)
        scheduleProfileCloudPush()
    }

    fun onProfileAvatarSaved(filePath: String) {
        settings.setProfileAvatarPath(filePath)
        showMessage("Profile photo updated!")
    }

    fun onProfileAvatarRemoved() {
        settings.setProfileAvatarPath("")
        showMessage("Profile photo removed.")
    }

    // ------------------------------------------------------------------------
    // IN-APP UPDATES (GITHUB RELEASES)
    // ------------------------------------------------------------------------
    fun checkForAppUpdates(userInitiated: Boolean) {
        if (_appUpdateState.value.status == AppUpdateStatus.CHECKING) return
        val currentVersion = BuildConfig.VERSION_NAME
        _appUpdateState.update {
            it.copy(
                status = AppUpdateStatus.CHECKING,
                currentVersion = currentVersion,
                statusMessage = "Checking for updates..."
            )
        }
        viewModelScope.launch {
            val result = AppUpdateChecker.checkLatestRelease(currentVersion)
            _appUpdateState.value = if (userInitiated) {
                result
            } else {
                // Silent background check: only pop dialog if a newer version is actually available
                result.copy(
                    showDialog = result.status == AppUpdateStatus.UPDATE_AVAILABLE,
                    statusMessage = if (result.status == AppUpdateStatus.ERROR) {
                        "Tap to check for new versions"
                    } else {
                        result.statusMessage
                    },
                    status = if (result.status == AppUpdateStatus.ERROR) AppUpdateStatus.IDLE else result.status
                )
            }
            if (userInitiated) {
                when (result.status) {
                    AppUpdateStatus.UPDATE_AVAILABLE -> showMessage("ClinEdge v${result.latestVersion} is available!")
                    AppUpdateStatus.UP_TO_DATE -> showMessage("You're on the latest version (v$currentVersion).")
                    AppUpdateStatus.ERROR -> showMessage(result.statusMessage)
                    else -> Unit
                }
            }
        }
    }

    fun openUpdateDialog() {
        if (_appUpdateState.value.status == AppUpdateStatus.UPDATE_AVAILABLE) {
            _appUpdateState.update { it.copy(showDialog = true) }
        }
    }

    fun dismissUpdateDialog() {
        _appUpdateState.update { it.copy(showDialog = false) }
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

    // ------------------------------------------------------------------------
    // ONBOARDING & DAILY STUDY GOALS
    // ------------------------------------------------------------------------
    fun completeOnboarding(
        examId: String,
        name: String,
        role: String,
        targetYear: String,
        focusSubject: String,
        phoneNumber: String,
        dailyGoalQuestions: Int
    ) {
        settings.setActiveExamId(examId)
        if (name.isNotBlank()) {
            settings.setProfileName(name)
        }
        settings.setProfileRole(role)
        settings.setTargetYear(targetYear)
        settings.setFocusSubject(focusSubject)
        settings.setPhoneNumber(AccountSessionLogic.sanitizePhoneNumber(phoneNumber))
        val goalResult = StudyGoalLogic.updateDailyGoalTarget(
            current = settings.studyGoalState.value,
            newTarget = dailyGoalQuestions
        )
        settings.setStudyGoalState(goalResult.updatedState)
        settings.setOnboardingCompleted(true)
        scheduleProfileCloudPush()
        val examLabel = mainState.value.availableExams.firstOrNull { it.id == examId }?.shortName
            ?: PlatformCatalogDefaults.DEFAULT_EXAMS.firstOrNull { it.id == examId }?.shortName
            ?: "ClinEdge"
        showMessage("All set for $examLabel • Daily goal: ${goalResult.updatedState.dailyGoalQuestions} Qs")
    }

    fun skipOnboarding() {
        settings.setOnboardingCompleted(true)
        scheduleProfileCloudPush()
    }

    fun reopenOnboarding() {
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        settings.setOnboardingCompleted(false)
    }

    fun setDailyGoalQuestions(target: Int) {
        val result = StudyGoalLogic.updateDailyGoalTarget(
            current = settings.studyGoalState.value,
            newTarget = target
        )
        settings.setStudyGoalState(result.updatedState)
        scheduleProfileCloudPush()
        if (result.justMetDailyGoal) {
            val days = result.updatedState.currentStreakDays
            showMessage("Daily goal complete! $days-day study streak!")
        }
    }

    fun startDailyGoalDrill() {
        val all = mainState.value.questions
        if (all.isEmpty()) {
            showMessage("Loading question bank, please wait a moment.")
            return
        }
        val goal = settings.studyGoalState.value
        val remaining = goal.remainingQuestions()
        val targetCount = if (remaining > 0) remaining else goal.dailyGoalQuestions
        val focus = settings.focusSubject.value
        val focusPool = if (focus.isNotBlank()) {
            all.filter { it.question.subject == focus }
        } else {
            emptyList()
        }
        val primaryPool = if (focusPool.size >= targetCount) focusPool else all
        // Prioritize mistakes and fresh unattempted questions first
        val mistakesAndFresh = primaryPool.filter { it.lastOutcome == "wrong" || it.attemptsCount == 0 }.shuffled()
        val remainder = primaryPool.filter { it.lastOutcome != "wrong" && it.attemptsCount > 0 }.shuffled()
        val picked = (mistakesAndFresh + remainder).take(targetCount)
        val drillTitle = if (remaining > 0) {
            "Daily Goal Drill (${picked.size}Q remaining)"
        } else {
            "Bonus Daily Drill (${picked.size}Q)"
        }
        startPractice(items = picked, title = drillTitle)
    }

    private fun recordDailyStudyProgress(answeredDelta: Int) {
        if (answeredDelta <= 0) return
        val result = StudyGoalLogic.recordStudyActivity(
            current = settings.studyGoalState.value,
            answeredDelta = answeredDelta
        )
        settings.setStudyGoalState(result.updatedState)
        scheduleProfileCloudPush()
        if (result.justMetDailyGoal) {
            val days = result.updatedState.currentStreakDays
            showMessage("Daily goal complete! $days-day study streak!")
        }
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

        recordDailyStudyProgress(answeredDelta = 1)

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
        val activeExamId = settings.activeExamId.value
        val isNorcetFullPrelims = isOfficialFullMock &&
            PlatformCatalogDefaults.isNorcetExam(activeExamId) &&
            totalQuestions == 100
        val nursingPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExamId)
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
            isOfficialFullMock = isOfficialFullMock,
            sectionSize = if (isNorcetFullPrelims && selected.size >= 100) 20 else 0,
            sectionDurationMinutes = if (isNorcetFullPrelims && selected.size >= 100) 18 else 0,
            sectionLabels = if (isNorcetFullPrelims && selected.size >= 100) {
                listOf(
                    "Section 1 • Nursing I (20Q)",
                    "Section 2 • Nursing II (20Q)",
                    "Section 3 • Nursing III (20Q)",
                    "Section 4 • Nursing IV (20Q)",
                    "Section 5 • GK & Aptitude (20Q)"
                )
            } else {
                emptyList()
            }
        )
    }

    fun startNorcetMainsMock() {
        val all = mainState.value.questions
        if (all.isEmpty()) return
        val nursingPool = all.filter { SubjectCatalog.isNursing(it.question) }
        // Prioritize dedicated NORCET clinical scenarios & Hard nursing questions for authentic Stage II Mains
        val dedicatedNorcetNur = nursingPool.filter { it.id.startsWith("N_") }.shuffled()
        val hardClinicalExtra = nursingPool.filter {
            !it.id.startsWith("N_") && it.question.difficulty == QuestionDifficulty.HARD
        }.shuffled()
        val remainingNursing = nursingPool.filter { q ->
            !q.id.startsWith("N_") && q.question.difficulty != QuestionDifficulty.HARD
        }.shuffled()
        val selected = (dedicatedNorcetNur + hardClinicalExtra + remainingNursing).take(160)
        val useSectionLock = selected.size >= 160
        startCbtCustom(
            items = selected,
            title = "NORCET Stage II Mains (160Q Clinical CBT)",
            durationMinutes = 180,
            isOfficialFullMock = true,
            sectionSize = if (useSectionLock) 40 else 0,
            sectionDurationMinutes = if (useSectionLock) 45 else 0,
            sectionLabels = if (useSectionLock) {
                listOf(
                    "Section 1 • Clinical Nursing I (40Q)",
                    "Section 2 • Clinical Nursing II (40Q)",
                    "Section 3 • Clinical Nursing III (40Q)",
                    "Section 4 • Clinical Nursing IV (40Q)"
                )
            } else {
                emptyList()
            }
        )
    }

    fun startCbtCustom(
        items: List<QuestionItem>,
        title: String,
        durationMinutes: Int? = null,
        isOfficialFullMock: Boolean = false,
        sectionSize: Int = 0,
        sectionDurationMinutes: Int = 0,
        sectionLabels: List<String> = emptyList()
    ) {
        if (items.isEmpty()) {
            showMessage("No questions available for this CBT test.")
            return
        }
        stopActiveTimer()
        _practiceSession.value = null
        val activeExam = mainState.value.activeExam
        val useSectionLock = sectionSize > 0 && sectionDurationMinutes > 0 && items.size > sectionSize
        val hasCountdown = isOfficialFullMock || useSectionLock || settings.useTimeLimit.value
        val sectionDurationSec = if (useSectionLock) sectionDurationMinutes * 60 else 0
        val totalSections = if (useSectionLock) ((items.size + sectionSize - 1) / sectionSize) else 1
        val totalSec = if (hasCountdown) {
            if (useSectionLock) {
                totalSections * sectionDurationSec
            } else {
                val mins = durationMinutes ?: if (isOfficialFullMock) {
                    activeExam?.defaultMockMinutes ?: 90
                } else {
                    SettingsPreferences.calculateSuggestedMinutes(
                        questionCount = items.size,
                        secondsPerQuestion = settings.secondsPerQuestion.value
                    ).coerceAtLeast(1)
                }
                mins * 60
            }
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
            remainingSeconds = totalSec,
            sectionSize = if (useSectionLock) sectionSize else 0,
            sectionDurationSeconds = sectionDurationSec,
            sectionLabels = if (useSectionLock) sectionLabels else emptyList(),
            currentSectionIndex = 0,
            sectionRemainingSeconds = sectionDurationSec
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

                if (current.hasSectionLock) {
                    val nextSectionSec = (current.sectionRemainingSeconds - 1).coerceAtLeast(0)
                    val nextTotalSec = (current.remainingSeconds - 1).coerceAtLeast(0)
                    if (nextSectionSec == 0) {
                        if (!current.isLastSection) {
                            advanceCbtSectionLocked(
                                session = current.copy(remainingSeconds = nextTotalSec),
                                triggeredByTimeout = true
                            )
                        } else {
                            _cbtSession.update {
                                it?.copy(
                                    remainingSeconds = 0,
                                    sectionRemainingSeconds = 0,
                                    showSectionAdvanceConfirm = false,
                                    showTimeoutDialog = true
                                )
                            }
                            break
                        }
                    } else {
                        _cbtSession.update {
                            it?.copy(
                                remainingSeconds = nextTotalSec,
                                sectionRemainingSeconds = nextSectionSec
                            )
                        }
                    }
                } else {
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
    }

    private fun advanceCbtSectionLocked(
        session: CbtSessionState,
        triggeredByTimeout: Boolean
    ) {
        if (!session.hasSectionLock || session.isLastSection) return
        val prevSectionNum = session.currentSectionIndex + 1
        val nextSectionIdx = session.currentSectionIndex + 1
        val nextStartIdx = (nextSectionIdx * session.sectionSize)
            .coerceIn(0, session.questions.lastIndex.coerceAtLeast(0))
        val updatedStatuses = if (session.paletteStatuses[nextStartIdx] == CbtPaletteStatus.NOT_VISITED) {
            session.paletteStatuses + (nextStartIdx to CbtPaletteStatus.NOT_ANSWERED)
        } else {
            session.paletteStatuses
        }
        val remainingSectionsCount = (session.totalSections - nextSectionIdx).coerceAtLeast(1)
        val syncedTotalRemaining = remainingSectionsCount * session.sectionDurationSeconds
        val notice = if (triggeredByTimeout) {
            "Section $prevSectionNum time ended and was locked. Now in Section ${nextSectionIdx + 1} of ${session.totalSections}."
        } else {
            "Section $prevSectionNum locked. Now in Section ${nextSectionIdx + 1} of ${session.totalSections}."
        }
        _cbtSession.value = session.copy(
            currentSectionIndex = nextSectionIdx,
            currentIndex = nextStartIdx,
            paletteStatuses = updatedStatuses,
            sectionRemainingSeconds = session.sectionDurationSeconds,
            remainingSeconds = syncedTotalRemaining,
            showSectionAdvanceConfirm = false,
            sectionAutoAdvanceNotice = notice
        )
    }

    fun requestCbtNextSection() {
        val sess = _cbtSession.value ?: return
        if (!sess.hasSectionLock || sess.isLastSection) return
        _cbtSession.value = sess.copy(showSectionAdvanceConfirm = true)
    }

    fun dismissCbtNextSection() {
        _cbtSession.update { it?.copy(showSectionAdvanceConfirm = false) }
    }

    fun confirmCbtNextSection() {
        val sess = _cbtSession.value ?: return
        if (!sess.hasSectionLock || sess.isLastSection) return
        val hasAns = sess.selectedAnswers.containsKey(sess.currentIndex)
        val curStatus = if (hasAns) CbtPaletteStatus.ANSWERED else CbtPaletteStatus.NOT_ANSWERED
        val synced = sess.copy(paletteStatuses = sess.paletteStatuses + (sess.currentIndex to curStatus))
        advanceCbtSectionLocked(synced, triggeredByTimeout = false)
    }

    fun dismissCbtSectionNotice() {
        _cbtSession.update { it?.copy(sectionAutoAdvanceNotice = null) }
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
                if (!it.isIndexInActiveSection(safeIdx)) {
                    return@let it
                }
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
        val sectionEnd = sess.activeSectionRange.last
        if (sess.currentIndex < sectionEnd) {
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
        } else if (sess.hasSectionLock && !sess.isLastSection) {
            _cbtSession.value = sess.copy(
                paletteStatuses = updatedStatuses,
                showSectionAdvanceConfirm = true
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
        val sectionEnd = sess.activeSectionRange.last
        if (sess.currentIndex < sectionEnd) {
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

        if (batchResults.isNotEmpty()) {
            recordDailyStudyProgress(answeredDelta = batchResults.size)
        }

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

    fun refreshBackupFolderSummary() {
        val storage = backupStorage ?: return
        viewModelScope.launch {
            _backupFolderSummary.value = storage.inspectActiveFolder(
                customTreeUri = settings.backupTreeUri.value,
                customLabel = settings.backupTreeLabel.value
            )
        }
    }

    fun onBackupFolderSelected(treeUri: Uri) {
        val storage = backupStorage ?: return
        viewModelScope.launch {
            val oldUri = settings.backupTreeUri.value
            if (oldUri.isNotBlank() && oldUri != treeUri.toString()) {
                storage.releaseTreePermission(oldUri)
            }
            val label = storage.persistTreePermission(treeUri)
            settings.setBackupFolder(treeUri.toString(), label)
            _backupFolderSummary.value = storage.inspectActiveFolder(
                customTreeUri = treeUri.toString(),
                customLabel = label
            )
            showMessage("Backup folder set to $label")
        }
    }

    fun resetBackupFolderToDefault() {
        val storage = backupStorage ?: return
        viewModelScope.launch {
            val oldUri = settings.backupTreeUri.value
            if (oldUri.isNotBlank()) {
                storage.releaseTreePermission(oldUri)
            }
            settings.clearBackupFolder()
            _backupFolderSummary.value = storage.inspectActiveFolder(
                customTreeUri = "",
                customLabel = ""
            )
            showMessage("Backup folder reset to default ClinEdgeBackups.")
        }
    }

    fun createBackupInActiveFolder() {
        val storage = backupStorage
        if (storage == null) {
            showMessage("Backup storage is not available right now.")
            return
        }
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            val content = buildBackupJson()
            val result = storage.saveBackupToActiveFolder(
                backupContent = content,
                customTreeUri = settings.backupTreeUri.value,
                customLabel = settings.backupTreeLabel.value
            )
            _backupFolderSummary.value = storage.inspectActiveFolder(
                customTreeUri = settings.backupTreeUri.value,
                customLabel = settings.backupTreeLabel.value
            )
            _isBackupBusy.value = false
            result.onSuccess { saved ->
                showMessage("Backup saved to ${saved.folderLabel}")
            }.onFailure { err ->
                showMessage("Backup failed: ${err.message}")
            }
        }
    }

    fun restoreLatestFromActiveFolder() {
        val storage = backupStorage
        if (storage == null) {
            showMessage("Backup storage is not available right now.")
            return
        }
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            val loaded = storage.readLatestBackupFromActiveFolder(
                customTreeUri = settings.backupTreeUri.value,
                customLabel = settings.backupTreeLabel.value
            )
            _isBackupBusy.value = false
            loaded.onSuccess { file ->
                restoreBackupFromText(file.content)
            }.onFailure { err ->
                showMessage(err.message ?: "No backup file found in folder.")
            }
        }
    }

    fun buildBackupJson(): String {
        val backupItems = allPlatformQuestions.value.ifEmpty { mainState.value.questions }
        val linksByQuestion = allPlatformLinks.value
            .groupBy { it.questionId }
            .mapValues { (_, links) -> links.map { it.examId }.distinct() }
        val goal = settings.studyGoalState.value
        val studentInfo = BulkQuestionParser.BackupStudentInfo(
            displayName = settings.profileName.value.ifBlank { signedInAccount.displayName },
            email = signedInAccount.email,
            role = settings.profileRole.value,
            targetYear = settings.targetYear.value,
            focusSubject = settings.focusSubject.value,
            phoneNumber = settings.phoneNumber.value,
            activeExamId = settings.activeExamId.value,
            dailyGoalQuestions = goal.dailyGoalQuestions,
            currentStreakDays = goal.currentStreakDays,
            longestStreakDays = goal.longestStreakDays,
            todayStudyDate = goal.todayStudyDate,
            todayAnsweredCount = goal.todayAnsweredCount,
            lastGoalMetDate = goal.lastGoalMetDate
        )
        val planInfo = BulkQuestionParser.BackupPlanInfo(
            planName = "ClinEdge Free",
            status = "Active",
            tier = "free",
            appVersion = BuildConfig.VERSION_NAME
        )
        return BulkQuestionParser.buildBackupJson(
            items = backupItems,
            questionExamLinks = linksByQuestion,
            activeExamId = settings.activeExamId.value,
            studentInfo = studentInfo,
            planInfo = planInfo
        )
    }

    fun restoreBackupFromText(jsonText: String) {
        viewModelScope.launch {
            runCatching {
                repository.restoreBackupWithDetails(jsonText, defaultExamId = settings.activeExamId.value)
            }.onSuccess { restored ->
                restored.studentInfo?.let { st ->
                    if (st.displayName.isNotBlank()) settings.setProfileName(st.displayName)
                    if (st.role.isNotBlank()) settings.setProfileRole(st.role)
                    if (st.targetYear.isNotBlank()) settings.setTargetYear(st.targetYear)
                    if (st.focusSubject.isNotBlank()) settings.setFocusSubject(st.focusSubject)
                    if (st.phoneNumber.isNotBlank()) {
                        settings.setPhoneNumber(AccountSessionLogic.sanitizePhoneNumber(st.phoneNumber))
                    }
                    val restoredGoal = DailyStudyGoalState(
                        dailyGoalQuestions = st.dailyGoalQuestions.coerceIn(
                            StudyGoalLogic.MIN_DAILY_GOAL,
                            StudyGoalLogic.MAX_DAILY_GOAL
                        ),
                        todayStudyDate = StudyGoalLogic.sanitizeDateKey(st.todayStudyDate),
                        todayAnsweredCount = st.todayAnsweredCount.coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                        currentStreakDays = st.currentStreakDays.coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                        longestStreakDays = st.longestStreakDays.coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                        lastGoalMetDate = StudyGoalLogic.sanitizeDateKey(st.lastGoalMetDate)
                    )
                    settings.setStudyGoalState(
                        StudyGoalLogic.mergeCloudGoalState(settings.studyGoalState.value, restoredGoal)
                    )
                }
                restored.activeExamId?.takeIf { it.isNotBlank() }?.let { examId ->
                    settings.setActiveExamId(examId)
                }
                _importerState.update { it.copy(isVisible = false) }
                if (currentUserId.isNotBlank() && !isSigningOut) {
                    scheduleProfileCloudPush()
                    performBidirectionalSync(showFeedbackToast = false)
                }
                val profileSummary = restored.studentInfo?.displayName?.takeIf { it.isNotBlank() }
                    ?.let { " • Profile: $it" }
                    .orEmpty()
                val planSummary = restored.planInfo?.planName?.takeIf { it.isNotBlank() }
                    ?.let { " ($it)" }
                    .orEmpty()
                showMessage(
                    "Backup restored (${restored.customQuestions.size} custom Qs, ${restored.progressList.size} progress items$profileSummary$planSummary)."
                )
            }.onFailure {
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
        val studentName = settings.profileName.value.trim()
            .ifBlank { signedInAccount.displayName.trim() }
        val options = PdfWorksheetOptions(
            title = "$sourceTitle Worksheet",
            subtitle = "Difficulty: $diffLabel  •  Format: $modeLabel",
            inlineAnswers = config.inlineAnswers,
            includeAnswerKey = config.includeAnswerKey,
            watermarkStudentName = studentName,
            watermarkPlanName = "ClinEdge Free",
            watermarkEmail = signedInAccount.email
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
