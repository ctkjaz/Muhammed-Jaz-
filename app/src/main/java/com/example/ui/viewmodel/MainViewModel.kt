package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ai.AiStreamEvent
import com.example.data.ai.AiTutorUiState
import com.example.data.ai.ClinEdgeAiTutorService
import com.example.data.backup.BackupFolderSummary
import com.example.data.backup.BackupStorageManager
import com.example.data.notifications.AnnouncementCategory
import com.example.data.notifications.AnnouncementPriority
import com.example.data.notifications.ClinEdgeNotificationManager
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDirectNotification
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.CloudEngagementRepository
import com.example.data.notifications.DailyReminderSettingsState
import com.example.data.notifications.DirectNotificationType
import com.example.data.notifications.DoubtAskContext
import com.example.data.notifications.EngagementLogic
import com.example.data.notifications.EngagementUnreadSummary
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
import com.example.data.grandtest.CloudGrandTest
import com.example.data.grandtest.GrandTestLeaderboardLogic
import com.example.data.grandtest.GrandTestLeaderboardSummary
import com.example.data.grandtest.GrandTestRepository
import com.example.data.grandtest.GrandTestStatus
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
import com.example.data.model.DailyFeaturedQuestion
import com.example.data.model.DailyFocusProgress
import com.example.data.model.ExamCountdownLogic
import com.example.data.model.ExamQuestionLink
import com.example.data.model.ExamReadinessEngine
import com.example.data.model.ExamReadinessSummary
import com.example.data.model.ExamRoadmapSummary
import com.example.data.model.ExamSection
import com.example.data.model.ExamStage
import com.example.data.model.Flashcard
import com.example.data.model.UnfoldingClinicalCasesCatalog
import com.example.data.model.FocusTimerLogic
import com.example.data.model.IbqCategory
import com.example.data.model.IbqHubLogic
import com.example.data.model.SpecialtyTrack
import com.example.data.model.FocusTimerPreset
import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.GamificationUpdateResult
import com.example.data.model.MockHistory
import com.example.data.model.MockSelectionLogic
import com.example.data.model.PerformanceAggregationEngine
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.PracticePathway
import com.example.data.model.PrecomputedPoolSummary
import com.example.data.model.Profession
import com.example.data.model.QotdCompletionState
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionNote
import com.example.data.model.QuestionNotesLogic
import com.example.data.model.QuestionOfTheDayCatalog
import com.example.data.model.QuestionSourceType
import com.example.data.model.SessionCheckResult
import com.example.data.model.SessionTimerState
import com.example.data.model.SmartStudyPlanSummary
import com.example.data.model.SmartWeakTopicPlanner
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SrsCardState
import com.example.data.model.SrsNotebookSummary
import com.example.data.model.StartupLaunchMode
import com.example.data.model.StartupProfiler
import com.example.data.model.StartupTimingReport
import com.example.data.model.StudyGoalLogic
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectPracticeLogic
import com.example.data.model.SubjectStat
import com.example.data.model.Topic
import com.example.data.parser.BulkQuestionParser
import com.example.data.parser.PdfTextExtractor
import com.example.data.parser.QuestionSubjectClassifier
import com.example.data.repository.NursingPrepRepository
import com.example.data.subscription.CloudAccessCode
import com.example.data.subscription.CloudPaymentRequest
import com.example.data.subscription.CloudQuestionReport
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.CloudUserEntitlement
import com.example.data.subscription.CodeRedeemResult
import com.example.data.subscription.DeviceIdentity
import com.example.data.subscription.DeviceLockConflictState
import com.example.data.subscription.DeviceLockManager
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.ExamLockUpgradeContext
import com.example.data.subscription.OtaQuestionEditDraft
import com.example.data.subscription.PaymentAndReportRepository
import com.example.data.subscription.PaywallPromptState
import com.example.data.subscription.PlanTenure
import com.example.data.subscription.PlanTier
import com.example.data.subscription.QuestionReportReason
import com.example.data.subscription.QuestionReportStatus
import com.example.data.subscription.SubscriptionRepository
import com.example.data.subscription.SubscriptionStatus
import com.example.data.subscription.SubscriptionUiState
import com.example.data.subscription.TrialClaimResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

    fun labelFor(examId: String?): String = when {
        PlatformCatalogDefaults.isPgimerExam(examId) -> when (this) {
            NURSING -> "Nursing 100%"
            PYQ -> "PGIMER PYQs"
            CA_GK -> "Flashcards"
            NON_TECH -> "Clinical Focus"
        }
        PlatformCatalogDefaults.isNorcetExam(examId) -> when (this) {
            NURSING -> "Nursing 80%"
            PYQ -> "NORCET PYQs"
            CA_GK -> "GK & Health"
            NON_TECH -> "Aptitude & Sci"
        }
        examId == PlatformCatalogDefaults.EXAM_RRB_NURSING || examId.isNullOrBlank() -> when (this) {
            NURSING -> "Nursing 70%"
            PYQ -> "RRB PYQs"
            CA_GK -> "CA & GK"
            NON_TECH -> "Sci/Math 30%"
        }
        else -> {
            val nurPct = PlatformCatalogDefaults.nursingWeightagePercent(examId)
            val shortName = PlatformCatalogDefaults.examById(examId)?.shortName ?: "Exam"
            when (this) {
                NURSING -> "Nursing $nurPct%"
                PYQ -> "$shortName PYQs"
                CA_GK -> "GK & Health"
                NON_TECH -> "Aptitude ${100 - nurPct}%"
            }
        }
    }
}

data class SubjectChaptersNavState(
    val subjectCode: String,
    val isCrossExam: Boolean = false,
    val examIdFilter: String? = null
)

enum class CbtPaletteStatus {
    NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED, ANSWERED_MARKED
}

enum class CbtReviewFilter(val label: String) {
    ALL("All"),
    WRONG("Wrong Only"),
    RIGHT("Correct"),
    SKIPPED("Skipped")
}

@Immutable
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
    val reviewFilter: CbtReviewFilter = CbtReviewFilter.ALL,
    val negativeMarks: Double = 0.33
)

@Immutable
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
    val negativeMarks: Double = 0.33,
    val sectionSize: Int = 0,
    val sectionDurationSeconds: Int = 0,
    val sectionLabels: List<String> = emptyList(),
    val currentSectionIndex: Int = 0,
    val sectionRemainingSeconds: Int = 0,
    val showSectionAdvanceConfirm: Boolean = false,
    val sectionAutoAdvanceNotice: String? = null,
    val grandTestId: String? = null,
    val grandTestExamId: String? = null
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
    val difficultyFilter: DifficultyFilter = DifficultyFilter.ALL,
    val useCrossExamPool: Boolean = false,
    val crossExamFilterId: String? = null
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

@Immutable
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

@Immutable
data class DashboardDerivedState(
    val activeExam: Exam? = PlatformCatalogDefaults.DEFAULT_EXAMS.first(),
    val activeExamSections: List<ExamSection> = PlatformCatalogDefaults.DEFAULT_SECTIONS,
    val availableProfessions: List<Profession> = PlatformCatalogDefaults.DEFAULT_PROFESSIONS,
    val availableExams: List<Exam> = PlatformCatalogDefaults.DEFAULT_EXAMS,
    val examQuestionCounts: Map<String, Int> = emptyMap(),
    val topicsById: Map<String, Topic> = emptyMap(),
    val questions: List<QuestionItem> = emptyList(),
    val activeExamSupportsStages: Boolean = false,
    val activeExamStage: ExamStage = ExamStage.PRELIMS,
    val stageQuestions: List<QuestionItem> = emptyList(),
    val stageSubjectStats: List<SubjectStat> = emptyList(),
    val stageSummary: PrecomputedPoolSummary = PrecomputedPoolSummary(),
    val stageMistakes: List<QuestionItem> = emptyList(),
    val globalIbqCount: Int = 0,
    val prelimsQuestionCount: Int = 0,
    val mainsQuestionCount: Int = 0,
    val prelimsAttemptedCount: Int = 0,
    val mainsAttemptedCount: Int = 0,
    val crossExamQuestions: List<QuestionItem> = emptyList(),
    val crossExamSubjectStats: List<SubjectStat> = emptyList(),
    val crossExamLinks: List<ExamQuestionLink> = emptyList(),
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
    private val backupStorage: BackupStorageManager? = null,
    private val subscriptionRepo: SubscriptionRepository? = null,
    private val deviceLockManager: DeviceLockManager? = null,
    private val engagementRepo: CloudEngagementRepository? = null,
    private val appContext: Context? = null,
    private val paymentReportRepo: PaymentAndReportRepository? = null,
    private val grandTestRepo: GrandTestRepository? = null,
    private val aiTutorService: ClinEdgeAiTutorService = ClinEdgeAiTutorService()
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

    val isOwnerAdmin: Boolean = EntitlementEvaluator.isOwnerAdminEmail(accountEmail)

    val deviceIdentity: DeviceIdentity = deviceLockManager?.resolveDeviceIdentity()
        ?: DeviceIdentity(
            deviceHash = DeviceLockManager.sha256Hex("clinedge_fallback_$currentUserId"),
            shortDeviceCode = DeviceLockManager.formatShortCode(
                DeviceLockManager.sha256Hex("clinedge_fallback_$currentUserId")
            ),
            deviceName = currentDeviceName
        )

    private val _subscriptionConfig = MutableStateFlow(CloudSubscriptionConfig())
    private val _rawEntitlement = MutableStateFlow<CloudUserEntitlement?>(null)
    private val _clockRefreshTick = MutableStateFlow(0L)

    val subscriptionState: StateFlow<SubscriptionUiState> = combine(
        _rawEntitlement,
        _subscriptionConfig,
        settings.dailyFreeQuotaUsed,
        settings.freeSampleMockUsed,
        _clockRefreshTick
    ) { ent, cfg, freeUsed, sampleUsed, _ ->
        val serverNow = subscriptionRepo?.currentServerNowMs() ?: System.currentTimeMillis()
        val evaluated = EntitlementEvaluator.evaluate(
            entitlement = ent,
            config = cfg,
            serverNowMs = serverNow,
            dailyFreeUsed = freeUsed,
            freeSampleMockUsed = sampleUsed,
            deviceShortCode = deviceIdentity.shortDeviceCode,
            deviceHash = deviceIdentity.deviceHash,
            isAdmin = isOwnerAdmin
        )
        settings.setCachedPlanName(evaluated.planDisplayName)
        evaluated
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SubscriptionUiState(
            isAdmin = isOwnerAdmin,
            effectiveTier = if (isOwnerAdmin) PlanTier.ALL_EXAM else PlanTier.FREE,
            unlockedExamIds = if (isOwnerAdmin) setOf("all") else emptySet(),
            deviceShortCode = deviceIdentity.shortDeviceCode,
            deviceHash = deviceIdentity.deviceHash,
            dailyFreeUsed = settings.dailyFreeQuotaUsed.value,
            freeSampleMockUsed = settings.freeSampleMockUsed.value,
            planDisplayName = if (isOwnerAdmin) "Owner Admin (All-Exam)" else settings.getCachedPlanName()
        )
    )

    private val _isPlansOpen = MutableStateFlow(false)
    val isPlansOpen: StateFlow<Boolean> = _isPlansOpen.asStateFlow()

    private val _plansExamLockContext = MutableStateFlow<ExamLockUpgradeContext?>(null)
    val plansExamLockContext: StateFlow<ExamLockUpgradeContext?> = _plansExamLockContext.asStateFlow()

    private val _isAdminOpen = MutableStateFlow(false)
    val isAdminOpen: StateFlow<Boolean> = _isAdminOpen.asStateFlow()

    private val _paywallPrompt = MutableStateFlow<PaywallPromptState?>(null)
    val paywallPrompt: StateFlow<PaywallPromptState?> = _paywallPrompt.asStateFlow()

    private val _deviceLockConflict = MutableStateFlow<DeviceLockConflictState?>(null)
    val deviceLockConflict: StateFlow<DeviceLockConflictState?> = _deviceLockConflict.asStateFlow()

    private val _isTrialBusy = MutableStateFlow(false)
    val isTrialBusy: StateFlow<Boolean> = _isTrialBusy.asStateFlow()

    private val _isRedeemBusy = MutableStateFlow(false)
    val isRedeemBusy: StateFlow<Boolean> = _isRedeemBusy.asStateFlow()

    private val _adminState = MutableStateFlow(AdminConsoleUiState())
    val adminState: StateFlow<AdminConsoleUiState> = _adminState.asStateFlow()

    // ------------------------------------------------------------------------
    // NOTIFICATIONS, DAILY STUDY REMINDERS & STUDENT DOUBT BOX STATE
    // ------------------------------------------------------------------------
    private val _announcements = MutableStateFlow<List<CloudAnnouncement>>(emptyList())
    val announcements: StateFlow<List<CloudAnnouncement>> = _announcements.asStateFlow()

    private val _directNotifications = MutableStateFlow<List<CloudDirectNotification>>(emptyList())
    val directNotifications: StateFlow<List<CloudDirectNotification>> = _directNotifications.asStateFlow()

    private val _studentDoubts = MutableStateFlow<List<CloudDoubt>>(emptyList())
    val studentDoubts: StateFlow<List<CloudDoubt>> = _studentDoubts.asStateFlow()

    private val _adminDoubts = MutableStateFlow<List<CloudDoubt>>(emptyList())
    val adminDoubts: StateFlow<List<CloudDoubt>> = _adminDoubts.asStateFlow()

    private val _isNotificationsOpen = MutableStateFlow(false)
    val isNotificationsOpen: StateFlow<Boolean> = _isNotificationsOpen.asStateFlow()

    private val _isDoubtBoxOpen = MutableStateFlow(false)
    val isDoubtBoxOpen: StateFlow<Boolean> = _isDoubtBoxOpen.asStateFlow()

    private val _highlightedDoubtId = MutableStateFlow<String?>(null)
    val highlightedDoubtId: StateFlow<String?> = _highlightedDoubtId.asStateFlow()

    private val _doubtAskContext = MutableStateFlow<DoubtAskContext?>(null)
    val doubtAskContext: StateFlow<DoubtAskContext?> = _doubtAskContext.asStateFlow()

    private val _isEngagementBusy = MutableStateFlow(false)
    val isEngagementBusy: StateFlow<Boolean> = _isEngagementBusy.asStateFlow()

    // ------------------------------------------------------------------------
    // v2.3: AI CLINICAL TUTOR, ADMIN AI DRAFT, UPI PAYMENTS & QUESTION FLAGS
    // ------------------------------------------------------------------------
    private val _studentPaymentRequests = MutableStateFlow<List<CloudPaymentRequest>>(emptyList())
    val studentPaymentRequests: StateFlow<List<CloudPaymentRequest>> = _studentPaymentRequests.asStateFlow()

    private val _isPaymentSubmitBusy = MutableStateFlow(false)
    val isPaymentSubmitBusy: StateFlow<Boolean> = _isPaymentSubmitBusy.asStateFlow()

    private val _aiTutorQuestion = MutableStateFlow<Question?>(null)
    val aiTutorQuestion: StateFlow<Question?> = _aiTutorQuestion.asStateFlow()
    private var aiTutorSelectedOptionIndex: Int? = null
    private var aiTutorLastFollowUp: String? = null
    private var aiTutorLastExplanation: String? = null
    private var aiTutorJob: Job? = null

    private val _aiTutorState = MutableStateFlow<AiTutorUiState>(AiTutorUiState.Idle)
    val aiTutorState: StateFlow<AiTutorUiState> = _aiTutorState.asStateFlow()

    private val _aiDraftingDoubtId = MutableStateFlow<String?>(null)
    val aiDraftingDoubtId: StateFlow<String?> = _aiDraftingDoubtId.asStateFlow()
    private var aiDraftJob: Job? = null

    private val _flagQuestionTarget = MutableStateFlow<Question?>(null)
    val flagQuestionTarget: StateFlow<Question?> = _flagQuestionTarget.asStateFlow()

    private val _isFlagBusy = MutableStateFlow(false)
    val isFlagBusy: StateFlow<Boolean> = _isFlagBusy.asStateFlow()

    // ------------------------------------------------------------------------
    // v2.4: SPACED REPETITION (SRS) NOTEBOOK, EXAM COUNTDOWN & CLINICAL TOOLS
    // ------------------------------------------------------------------------
    private val _isClinicalToolsOpen = MutableStateFlow(false)
    val isClinicalToolsOpen: StateFlow<Boolean> = _isClinicalToolsOpen.asStateFlow()

    private val _clinicalToolsInitialSection = MutableStateFlow(0)
    val clinicalToolsInitialSection: StateFlow<Int> = _clinicalToolsInitialSection.asStateFlow()

    private val _isEcgStudioOpen = MutableStateFlow(false)
    val isEcgStudioOpen: StateFlow<Boolean> = _isEcgStudioOpen.asStateFlow()

    private val _isIbqHubOpen = MutableStateFlow(false)
    val isIbqHubOpen: StateFlow<Boolean> = _isIbqHubOpen.asStateFlow()

    private val _ibqHubInitialCategory = MutableStateFlow(IbqCategory.ALL)
    val ibqHubInitialCategory: StateFlow<IbqCategory> = _ibqHubInitialCategory.asStateFlow()

    private val _isSpecialtyTracksOpen = MutableStateFlow(false)
    val isSpecialtyTracksOpen: StateFlow<Boolean> = _isSpecialtyTracksOpen.asStateFlow()

    private val _specialtyTrackInitial = MutableStateFlow(SpecialtyTrack.CARDIOLOGY)
    val specialtyTrackInitial: StateFlow<SpecialtyTrack> = _specialtyTrackInitial.asStateFlow()

    private val _isFlashcardStudioOpen = MutableStateFlow(false)
    val isFlashcardStudioOpen: StateFlow<Boolean> = _isFlashcardStudioOpen.asStateFlow()

    private val _isClinicalCasesOpen = MutableStateFlow(false)
    val isClinicalCasesOpen: StateFlow<Boolean> = _isClinicalCasesOpen.asStateFlow()

    val completedClinicalCaseIds: StateFlow<Set<String>> = settings.completedClinicalCaseIds

    // ------------------------------------------------------------------------
    // v2.5: WEEKLY ALL-INDIA GRAND TEST & PERCENTILE (DORMANT BY DEFAULT)
    // ------------------------------------------------------------------------
    private val _isGrandTestHubOpen = MutableStateFlow(false)
    val isGrandTestHubOpen: StateFlow<Boolean> = _isGrandTestHubOpen.asStateFlow()

    private val _publicGrandTests = MutableStateFlow<List<CloudGrandTest>>(emptyList())
    val publicGrandTests: StateFlow<List<CloudGrandTest>> = _publicGrandTests.asStateFlow()

    private val _selectedGrandTestId = MutableStateFlow<String?>(null)
    val selectedGrandTestId: StateFlow<String?> = _selectedGrandTestId.asStateFlow()

    private val _grandTestLeaderboard = MutableStateFlow<GrandTestLeaderboardSummary?>(null)
    val grandTestLeaderboard: StateFlow<GrandTestLeaderboardSummary?> = _grandTestLeaderboard.asStateFlow()

    private val _isGrandTestLoading = MutableStateFlow(false)
    val isGrandTestLoading: StateFlow<Boolean> = _isGrandTestLoading.asStateFlow()

    private var publicGrandTestObserverJob: Job? = null

    val targetExamDateIso: StateFlow<String> = settings.targetExamDateIso
    val srsCards: StateFlow<Map<String, SrsCardState>> = settings.srsCards
    val questionNotes: StateFlow<Map<String, QuestionNote>> = settings.questionNotes
    val masteredFlashcardIds: StateFlow<Set<String>> = settings.masteredFlashcardIds
    val dailyFocusProgress: StateFlow<DailyFocusProgress> = settings.dailyFocusProgress

    val lastSeenAnnouncementMs: StateFlow<Long> = settings.lastSeenAnnouncementMs
    val backgroundPushEnabled: StateFlow<Boolean> = settings.backgroundPushEnabled
    val lastBackgroundSyncMs: StateFlow<Long> = settings.lastBackgroundSyncMs

    val dailyReminderState: StateFlow<DailyReminderSettingsState> = combine(
        settings.dailyReminderEnabled,
        settings.dailyReminderHour,
        settings.dailyReminderMinute,
        settings.dailyReminderSmartNudge
    ) { enabled, hour, minute, smartNudge ->
        DailyReminderSettingsState(
            enabled = enabled,
            hour = hour,
            minute = minute,
            smartNudge = smartNudge
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DailyReminderSettingsState(
            enabled = settings.dailyReminderEnabled.value,
            hour = settings.dailyReminderHour.value,
            minute = settings.dailyReminderMinute.value,
            smartNudge = settings.dailyReminderSmartNudge.value
        )
    )

    val unreadSummary: StateFlow<EngagementUnreadSummary> = combine(
        _announcements,
        _directNotifications,
        _studentDoubts,
        activeExamId,
        settings.lastSeenAnnouncementMs
    ) { anns, direct, doubts, examId, lastSeenMs ->
        EngagementLogic.computeUnreadCounts(
            announcements = anns,
            directNotifications = direct,
            doubts = doubts,
            activeExamId = examId,
            lastSeenAnnouncementMs = lastSeenMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EngagementUnreadSummary()
    )

    val activeUrgentBanner: StateFlow<CloudAnnouncement?> = combine(
        _announcements,
        activeExamId,
        settings.dismissedBannerAnnouncementId
    ) { anns, examId, dismissedId ->
        anns.firstOrNull { ann ->
            ann.priorityEnum == AnnouncementPriority.URGENT &&
                EngagementLogic.announcementMatchesExam(ann, examId) &&
                ann.announcementId != dismissedId
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /** O(1) bookmark lookup stream for PracticeScreen without touching 10k-item mainState lists. */
    val bookmarkedIds: StateFlow<Set<String>> = repository.bookmarkedQuestionIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    private val allPlatformQuestions: StateFlow<List<QuestionItem>> = repository.allQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val dedupedPlatformQuestions: StateFlow<List<QuestionItem>> = repository.dedupedAllQuestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val allPlatformLinks: StateFlow<List<ExamQuestionLink>> = repository.allExamLinks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val examCountsByExamId: StateFlow<Map<String, Int>> = allPlatformLinks
        .map { links ->
            val counts = HashMap<String, Int>(16)
            for (link in links) {
                counts[link.examId] = (counts[link.examId] ?: 0) + 1
            }
            counts as Map<String, Int>
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    private val activeExamQuestions = activeExamId.flatMapLatest { examId ->
        repository.observeQuestionsForExam(examId)
    }.conflate()

    private val activeExamHistory = activeExamId.flatMapLatest { examId ->
        repository.observeMockHistoryForExam(examId)
    }.distinctUntilChanged()

    private val activeExamSections = activeExamId.flatMapLatest { examId ->
        repository.observeExamSections(examId)
    }.distinctUntilChanged()

    private val _activeExamStage = MutableStateFlow(ExamStage.PRELIMS)
    val activeExamStage: StateFlow<ExamStage> = _activeExamStage.asStateFlow()

    private val _practicePathway = MutableStateFlow(PracticePathway.BY_EXAM)
    val practicePathway: StateFlow<PracticePathway> = _practicePathway.asStateFlow()

    private val _subjectPracticeExamFilter = MutableStateFlow<String?>(null)
    val subjectPracticeExamFilter: StateFlow<String?> = _subjectPracticeExamFilter.asStateFlow()

    private val _practiceSession = MutableStateFlow<PracticeSessionState?>(null)
    val practiceSession: StateFlow<PracticeSessionState?> = _practiceSession.asStateFlow()

    private val _cbtSession = MutableStateFlow<CbtSessionState?>(null)
    val cbtSession: StateFlow<CbtSessionState?> = _cbtSession.asStateFlow()

    /** Isolated 1-second session countdown flow — never recomposes full screen or app shell. */
    private val _sessionTimerState = MutableStateFlow(SessionTimerState())
    val sessionTimerState: StateFlow<SessionTimerState> = _sessionTimerState.asStateFlow()

    /**
     * Public boolean StateFlow indicating whether Practice or CBT is active.
     * Collected by the root app shell so question taps & timer ticks inside sessions
     * NEVER recompose the drawer, scaffold, or chrome!
     */
    val isInActiveSessionState: StateFlow<Boolean> = combine(
        _practiceSession,
        _cbtSession
    ) { practice, cbt ->
        practice != null || cbt != null
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private val isInActiveSession: Flow<Boolean> = isInActiveSessionState

    private data class CatalogBundle(
        val activeExamId: String,
        val professions: List<Profession>,
        val exams: List<Exam>,
        val sections: List<ExamSection>,
        val topics: List<Topic>,
        val links: List<ExamQuestionLink>,
        val examCounts: Map<String, Int>,
        val dedupedAllQuestions: List<QuestionItem>,
        val activeStage: ExamStage,
        val subjectExamFilter: String?,
        val pathway: PracticePathway
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
            links = emptyList(),
            examCounts = emptyMap(),
            dedupedAllQuestions = emptyList(),
            activeStage = ExamStage.PRELIMS,
            subjectExamFilter = null,
            pathway = PracticePathway.BY_EXAM
        )
    }.combine(allPlatformLinks) { bundle, links ->
        bundle.copy(links = links)
    }.combine(examCountsByExamId) { bundle, counts ->
        bundle.copy(examCounts = counts)
    }.combine(dedupedPlatformQuestions) { bundle, dedupedQs ->
        bundle.copy(dedupedAllQuestions = dedupedQs)
    }.combine(_activeExamStage) { bundle, stage ->
        bundle.copy(activeStage = stage)
    }.combine(_subjectPracticeExamFilter) { bundle, filterId ->
        bundle.copy(subjectExamFilter = filterId)
    }.combine(_practicePathway) { bundle, pathway ->
        bundle.copy(pathway = pathway)
    }.conflate().flowOn(Dispatchers.Default)

    private data class RawDashboardInputs(
        val items: List<QuestionItem>,
        val history: List<MockHistory>,
        val cards: List<Flashcard>,
        val seeded: Boolean,
        val catalog: CatalogBundle
    )

    private var cachedCrossSubjectStats: List<SubjectStat> = emptyList()
    private var cachedCrossSubjectKey: String = ""

    val mainState: StateFlow<DashboardDerivedState> = combine(
        activeExamQuestions,
        activeExamHistory,
        _flashcards,
        _isSeeded,
        catalogBundle
    ) { items, history, cards, seeded, catalog ->
        RawDashboardInputs(items, history, cards, seeded, catalog)
    }.conflate()
        .combine(isInActiveSession) { inputs, inSession ->
            inputs to inSession
        }
        // Zero-Lag Active Session Coalescing:
        // While inside a live Practice or CBT session, skip heavy 10k-item Dashboard/Hub rebuilds.
        // The instant the session ends, isInActiveSession flips to false and flushes fresh stats once.
        .filter { (_, inSession) -> !inSession }
        .conflate()
        .map { (inputs, _) ->
            val items = inputs.items
            val history = inputs.history
            val cards = inputs.cards
            val seeded = inputs.seeded
            val catalog = inputs.catalog

            val activeExam = catalog.exams.firstOrNull { it.id == catalog.activeExamId }
                ?: catalog.exams.firstOrNull()
                ?: PlatformCatalogDefaults.DEFAULT_EXAMS.first()
            val supportsStages = PlatformCatalogDefaults.supportsStages(activeExam.id)
            val prelimsItems = if (supportsStages) {
                PlatformCatalogDefaults.filterQuestionsForExamStage(activeExam.id, ExamStage.PRELIMS, items)
            } else {
                emptyList()
            }
            val mainsItems = if (supportsStages) {
                PlatformCatalogDefaults.filterQuestionsForExamStage(activeExam.id, ExamStage.MAINS, items)
            } else {
                emptyList()
            }
            val stageItems = if (supportsStages) {
                if (catalog.activeStage == ExamStage.MAINS) mainsItems else prelimsItems
            } else {
                items
            }

            val statsPool = if (supportsStages) stageItems else items
            val poolSummary = PerformanceAggregationEngine.computePoolSummary(
                stagePool = statsPool,
                allExamItems = items
            )
            val stageMistakes = PerformanceAggregationEngine.extractStageMistakes(statsPool)
            val acc = if (poolSummary.totalAttempts > 0) {
                ((poolSummary.totalCorrect.toFloat() / poolSummary.totalAttempts) * 100f).toInt()
            } else 0
            val netScore = poolSummary.latestRight - (poolSummary.latestWrong * activeExam.negativeMarks)

            val dedupedCrossExam = catalog.dedupedAllQuestions.ifEmpty { items }
            val globalIbqCount = if (poolSummary.imageBasedCount > 0) {
                poolSummary.imageBasedCount
            } else {
                dedupedCrossExam.count { it.question.isImageBased }
            }

            // Lazy Cross-Exam Stats: compute immediately when By-Subject pathway is active or cache is empty;
            // reuse cached stats during single-exam navigation to cut background CPU in half.
            val crossStatsKey = "${catalog.subjectExamFilter ?: "ALL"}|${dedupedCrossExam.size}|${poolSummary.totalAttempts}"
            val crossSubjectStats = if (catalog.pathway == PracticePathway.BY_SUBJECT ||
                cachedCrossSubjectStats.isEmpty() ||
                crossStatsKey != cachedCrossSubjectKey
            ) {
                if (catalog.pathway == PracticePathway.BY_SUBJECT || cachedCrossSubjectStats.isEmpty()) {
                    SubjectPracticeLogic.computeCrossExamSubjectStats(
                        allQuestions = dedupedCrossExam,
                        allLinks = catalog.links,
                        examIdFilter = catalog.subjectExamFilter,
                        alreadyDeduplicated = catalog.dedupedAllQuestions.isNotEmpty(),
                        includeVirtualShared = true
                    ).also {
                        cachedCrossSubjectStats = it
                        cachedCrossSubjectKey = crossStatsKey
                    }
                } else {
                    cachedCrossSubjectStats
                }
            } else {
                cachedCrossSubjectStats
            }

            val computedSubjectStats = repository.computeSubjectStats(stageItems)
            val prelimsAttempted = if (supportsStages) prelimsItems.count { it.attemptsCount > 0 } else 0
            val mainsAttempted = if (supportsStages) mainsItems.count { it.attemptsCount > 0 } else 0

            DashboardDerivedState(
                activeExam = activeExam,
                activeExamSections = catalog.sections,
                availableProfessions = catalog.professions,
                availableExams = catalog.exams,
                examQuestionCounts = catalog.examCounts,
                topicsById = catalog.topics.associateBy { it.id },
                questions = items,
                activeExamSupportsStages = supportsStages,
                activeExamStage = catalog.activeStage,
                stageQuestions = stageItems,
                stageSubjectStats = computedSubjectStats,
                stageSummary = poolSummary,
                stageMistakes = stageMistakes,
                globalIbqCount = globalIbqCount,
                prelimsQuestionCount = prelimsItems.size,
                mainsQuestionCount = mainsItems.size,
                prelimsAttemptedCount = prelimsAttempted,
                mainsAttemptedCount = mainsAttempted,
                crossExamQuestions = dedupedCrossExam,
                crossExamSubjectStats = crossSubjectStats,
                crossExamLinks = catalog.links,
                overall = OverallStats(
                    totalQuestions = poolSummary.totalQuestions,
                    attemptedUnique = poolSummary.attemptedUnique,
                    totalAttempts = poolSummary.totalAttempts,
                    totalCorrect = poolSummary.totalCorrect,
                    totalWrong = poolSummary.totalWrong,
                    accuracyPercent = acc,
                    netPracticeScore = netScore,
                    bookmarkCount = poolSummary.bookmarkCount,
                    mistakeCount = poolSummary.latestWrong,
                    customCount = poolSummary.customCount
                ),
                subjectStats = computedSubjectStats,
                categoryStats = repository.computeCategoryStats(stageItems, activeExam.id),
                lowestAccuracyQuestions = repository.findLowestAccuracyQuestions(stageItems),
                flashcards = cards,
                mockHistory = history,
                isLoaded = seeded
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardDerivedState()
        )

    // Phase 0: Offload heavy analytics & SRS summaries off the UI main thread onto Dispatchers.Default
    val srsNotebookSummary: StateFlow<SrsNotebookSummary> = combine(
        mainState,
        srsCards
    ) { st, cards ->
        if (!st.isLoaded || st.questions.isEmpty()) SrsNotebookSummary()
        else SpacedRepetitionLogic.buildSummary(questions = st.questions, storedCards = cards)
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SrsNotebookSummary()
    )

    val examReadiness: StateFlow<ExamReadinessSummary> = combine(
        mainState,
        srsCards,
        studyGoalState
    ) { st, cards, goal ->
        val activeSubjects = if (st.activeExamSupportsStages) {
            st.stageSubjectStats.ifEmpty { st.subjectStats }
        } else {
            st.subjectStats
        }
        ExamReadinessEngine.compute(
            totalQuestions = st.overall.totalQuestions,
            attemptedUnique = st.overall.attemptedUnique,
            totalAttempts = st.overall.totalAttempts,
            accuracyPercent = st.overall.accuracyPercent,
            subjectStats = activeSubjects,
            mockHistory = st.mockHistory,
            srsCards = cards,
            currentStreakDays = goal.effectiveCurrentStreak(),
            dailyGoalMetToday = goal.isGoalMetToday()
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamReadinessEngine.compute(
            totalQuestions = 0,
            attemptedUnique = 0,
            totalAttempts = 0,
            accuracyPercent = 0,
            subjectStats = emptyList(),
            mockHistory = emptyList(),
            srsCards = emptyMap(),
            currentStreakDays = 0,
            dailyGoalMetToday = false
        )
    )

    val smartStudyPlan: StateFlow<SmartStudyPlanSummary?> = combine(
        mainState,
        srsCards,
        studyGoalState
    ) { st, cards, goal ->
        val pool = st.stageQuestions.ifEmpty { st.questions }
        if (!st.isLoaded || pool.isEmpty()) null
        else SmartWeakTopicPlanner.buildPlan(
            questions = pool,
            srsCards = cards,
            dailyGoalQuestions = goal.dailyGoalQuestions
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val examRoadmap: StateFlow<ExamRoadmapSummary> = combine(
        targetExamDateIso,
        mainState,
        studyGoalState
    ) { dateIso, st, goal ->
        ExamCountdownLogic.computeRoadmap(
            targetDateIso = dateIso,
            totalBankCount = st.questions.size,
            attemptedCount = st.overall.attemptedUnique,
            currentDailyGoal = goal.dailyGoalQuestions
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamRoadmapSummary()
    )

    // Phase 0 & 1: Startup instrumentation + Question of the Day state
    private val startupProfiler = StartupProfiler()
    private val _startupTimingReport = MutableStateFlow<StartupTimingReport?>(null)
    val startupTimingReport: StateFlow<StartupTimingReport?> = _startupTimingReport.asStateFlow()

    val qotdCompletion: StateFlow<QotdCompletionState> = settings.qotdCompletion
    val gamificationState: StateFlow<GamificationState> = settings.gamificationState
    val todayQuestionOfTheDay: DailyFeaturedQuestion
        get() = QuestionOfTheDayCatalog.questionForDate(StudyGoalLogic.todayDateKey())

    private val _selectedTab = MutableStateFlow(MainTab.DASHBOARD)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _subjectHubSection = MutableStateFlow(SubjectHubSection.NURSING)
    val subjectHubSection: StateFlow<SubjectHubSection> = _subjectHubSection.asStateFlow()

    private val _difficultyFilter = MutableStateFlow(DifficultyFilter.ALL)
    val difficultyFilter: StateFlow<DifficultyFilter> = _difficultyFilter.asStateFlow()

    private val _subjectChaptersNav = MutableStateFlow<SubjectChaptersNavState?>(null)
    val subjectChaptersNav: StateFlow<SubjectChaptersNavState?> = _subjectChaptersNav.asStateFlow()

    private val _chaptersHighYieldOnly = MutableStateFlow(false)
    val chaptersHighYieldOnly: StateFlow<Boolean> = _chaptersHighYieldOnly.asStateFlow()

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
    private var practiceCloudSyncJob: Job? = null
    private var otaSyncInFlight = false
    private var applyingCloudProfile = false
    private var isSigningOut = false
    private var localSessionId: String = ""
    private var sessionGuardArmed = false
    private var pendingSessionClaimRetry = false

    init {
        startupProfiler.mark("vm_init")
        viewModelScope.launch {
            val previousUid = settings.getLastSignedInUid()
            if (previousUid.isNotBlank() && previousUid != currentUserId) {
                repository.clearUserSpecificLocalDataForAccountSwitch()
                settings.resetGoalStateForAccountSwitch()
                settings.clearLocalSession()
            }
            settings.setLastSignedInUid(currentUserId)
            settings.refreshDailyFreeQuotaForToday()
            if (settings.profileName.value.isBlank() && signedInAccount.displayName.isNotBlank()) {
                settings.setProfileName(signedInAccount.displayName)
            }

            // Phase 0: Critical local path only — fast warm check skips redundant catalog writes
            startupProfiler.mark("seed_start")
            val didColdSeed = repository.ensureSeeded(settings.getSeededVersion()) { newVer ->
                settings.setSeededVersion(newVer)
            }
            startupProfiler.setLaunchMode(
                if (didColdSeed) StartupLaunchMode.COLD_SEED else StartupLaunchMode.WARM_CACHE
            )
            startupProfiler.mark("seed_done")
            _isSeeded.value = true
            startupProfiler.mark("first_data_ready")
            val report = startupProfiler.buildReport()
            _startupTimingReport.value = report
            Log.i("ClinEdgeStartup", report.formatLogLine())

            // Non-blocking background fan-out: load secondary local assets, cloud sync, updates & notifications in parallel
            launch(Dispatchers.IO) {
                _flashcards.value = repository.loadFlashcards()
            }
            launch(Dispatchers.IO) {
                refreshBackupFolderSummary()
            }
            launch(Dispatchers.IO) {
                appContext?.let { ctx ->
                    ClinEdgeNotificationManager.ensureChannelsCreated(ctx)
                    ClinEdgeNotificationManager.scheduleCloudAlertSync(ctx)
                    if (settings.dailyReminderEnabled.value) {
                        ClinEdgeNotificationManager.scheduleDailyReminder(
                            context = ctx,
                            hour = settings.dailyReminderHour.value,
                            minute = settings.dailyReminderMinute.value
                        )
                    }
                }
            }
            launch {
                checkForAppUpdates(userInitiated = false)
            }
            if (currentUserId.isNotBlank()) {
                launch {
                    val allowed = initializeSingleDeviceSession()
                    if (allowed && !isSigningOut) {
                        launch { performBidirectionalSync(showFeedbackToast = false) }
                        launch { initializeSubscriptionAndOta() }
                        startRealtimeCloudObservers()
                    }
                }
            }
        }
        viewModelScope.launch {
            subscriptionState.collect { sub ->
                if (sub.isLoaded && sub.isSingleExamLocked) {
                    val allowedExam = sub.primaryUnlockedExamId ?: return@collect
                    if (settings.activeExamId.value !in sub.unlockedExamIds) {
                        settings.setActiveExamId(allowedExam)
                    }
                    if (sub.isExamLockedForSwitch(_subjectPracticeExamFilter.value)) {
                        _subjectPracticeExamFilter.value = allowedExam
                    }
                }
            }
        }
    }

    private suspend fun initializeSubscriptionAndOta() {
        val repo = subscriptionRepo ?: return
        if (currentUserId.isBlank() || isSigningOut) return
        repo.syncServerClock(currentUserId)
        repo.ensureFreeEntitlement(
            userId = currentUserId,
            email = signedInAccount.email,
            displayName = settings.profileName.value.ifBlank { signedInAccount.displayName }
        )
        repo.fetchEntitlementFromServer(currentUserId).onSuccess { ent ->
            if (ent != null) {
                _rawEntitlement.value = ent
            }
        }
        _clockRefreshTick.value = System.currentTimeMillis()
    }

    private fun triggerOtaPackSyncIfNeeded(remoteCatalogVersion: Long) {
        val repo = subscriptionRepo ?: return
        if (remoteCatalogVersion <= 0L || remoteCatalogVersion <= settings.getSyncedOtaCatalogVersion()) return
        if (otaSyncInFlight || isSigningOut) return
        otaSyncInFlight = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packs = repo.fetchPublishedQuestionPacks().getOrNull() ?: return@launch
                var syncedCount = 0
                for (pack in packs) {
                    val packId = pack.packId.ifBlank { pack.id }
                    if (packId.isBlank()) continue
                    val cloudQs = repo.fetchPublishedQuestionsForPack(packId).getOrNull().orEmpty()
                    val domainQs = cloudQs.mapNotNull { it.toDomainQuestion() }
                    if (domainQs.isNotEmpty()) {
                        repository.saveOtaPackQuestions(examId = pack.examId, questions = domainQs)
                        syncedCount += domainQs.size
                    }
                }
                settings.setSyncedOtaCatalogVersion(remoteCatalogVersion)
                if (syncedCount > 0) {
                    showMessage("Updated question bank with $syncedCount new questions.")
                }
            } finally {
                otaSyncInFlight = false
            }
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
            settings.refreshDailyFreeQuotaForToday()
            subscriptionRepo?.let { repo ->
                repo.syncServerClock(currentUserId)
                repo.fetchEntitlementFromServer(currentUserId).onSuccess { ent ->
                    if (ent != null) _rawEntitlement.value = ent
                }
                _clockRefreshTick.value = System.currentTimeMillis()
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
            todayAnsweredCount = goal.todayAnsweredCount,
            email = signedInAccount.email,
            lastDeviceHash = deviceIdentity.deviceHash,
            gamificationState = settings.gamificationState.value
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
        practiceCloudSyncJob?.cancel()
        practiceCloudSyncJob = null
        aiTutorJob?.cancel()
        aiTutorJob = null
        aiDraftJob?.cancel()
        aiDraftJob = null
        publicGrandTestObserverJob?.cancel()
        publicGrandTestObserverJob = null
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

        val subJobs = if (subscriptionRepo != null) {
            val cfgJob = viewModelScope.launch {
                subscriptionRepo.observeSubscriptionConfig().collect { cfg ->
                    if (!isSigningOut) {
                        _subscriptionConfig.value = cfg
                        triggerOtaPackSyncIfNeeded(cfg.catalogVersion)
                        syncPublicGrandTestObserver(cfg.grandTestEnabled)
                    }
                }
            }
            val entJob = viewModelScope.launch {
                subscriptionRepo.observeEntitlement(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Entitlement observer interrupted", firestoreError ?: error)
                    }
                    .collect { ent ->
                        if (!isSigningOut) {
                            _rawEntitlement.value = ent
                            _clockRefreshTick.value = System.currentTimeMillis()
                        }
                    }
            }
            listOf(cfgJob, entJob)
        } else {
            emptyList()
        }

        val engagementJobs = if (engagementRepo != null) {
            val annJob = viewModelScope.launch {
                engagementRepo.observeAnnouncements()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Announcements stream interrupted", firestoreError ?: error)
                    }
                    .collect { list ->
                        if (isSigningOut) return@collect
                        _announcements.value = list
                        val lastNotified = settings.getLastNotifiedAnnouncementMs()
                        val newestMs = list.maxOfOrNull { it.createdAtMs } ?: 0L
                        if (lastNotified == 0L) {
                            if (newestMs > 0L) settings.setLastNotifiedAnnouncementMs(newestMs)
                        } else if (newestMs > lastNotified) {
                            val activeExam = settings.activeExamId.value
                            val fresh = list.filter {
                                it.createdAtMs > lastNotified &&
                                    EngagementLogic.announcementMatchesExam(it, activeExam) &&
                                    !it.createdByEmail.equals(signedInAccount.email, ignoreCase = true)
                            }.sortedBy { it.createdAtMs }
                            appContext?.let { ctx ->
                                var highestPostedMs = lastNotified
                                fresh.takeLast(2).forEach { ann ->
                                    if (ClinEdgeNotificationManager.postAnnouncementNotification(ctx, ann)) {
                                        highestPostedMs = maxOf(highestPostedMs, ann.createdAtMs)
                                    }
                                }
                                if (highestPostedMs > lastNotified) {
                                    settings.setLastNotifiedAnnouncementMs(highestPostedMs)
                                } else if (fresh.isEmpty() && ClinEdgeNotificationManager.hasNotificationPermission(ctx)) {
                                    settings.setLastNotifiedAnnouncementMs(newestMs)
                                }
                            }
                        }
                    }
            }
            val directJob = viewModelScope.launch {
                engagementRepo.observeDirectNotifications(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Direct notifications stream interrupted", firestoreError ?: error)
                    }
                    .collect { list ->
                        if (isSigningOut) return@collect
                        _directNotifications.value = list
                        val lastNotified = settings.getLastNotifiedDirectMs()
                        val fresh = list.filter { !it.read && it.createdAtMs > lastNotified }
                            .sortedBy { it.createdAtMs }
                        if (fresh.isNotEmpty()) {
                            appContext?.let { ctx ->
                                var highestPostedMs = lastNotified
                                fresh.takeLast(2).forEach { notif ->
                                    if (ClinEdgeNotificationManager.postDirectNotification(ctx, notif)) {
                                        highestPostedMs = maxOf(highestPostedMs, notif.createdAtMs)
                                    }
                                }
                                if (highestPostedMs > lastNotified) {
                                    settings.setLastNotifiedDirectMs(highestPostedMs)
                                }
                            }
                        }
                    }
            }
            val studentDoubtsJob = viewModelScope.launch {
                engagementRepo.observeStudentDoubts(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Student doubts stream interrupted", firestoreError ?: error)
                    }
                    .collect { list ->
                        if (!isSigningOut) _studentDoubts.value = list
                    }
            }
            val adminDoubtsJob = if (isOwnerAdmin) {
                listOf(
                    viewModelScope.launch {
                        engagementRepo.observeAllDoubtsForAdmin()
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                Log.w(TAG, "Admin doubts stream interrupted", firestoreError ?: error)
                            }
                            .collect { list ->
                                if (!isSigningOut) _adminDoubts.value = list
                            }
                    }
                )
            } else {
                emptyList()
            }
            listOf(annJob, directJob, studentDoubtsJob) + adminDoubtsJob
        } else {
            emptyList()
        }

        val payReportJobs = if (paymentReportRepo != null) {
            val studentPayJob = viewModelScope.launch {
                paymentReportRepo.observeStudentPaymentRequests(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Student payment requests stream interrupted", firestoreError ?: error)
                    }
                    .collect { list ->
                        if (!isSigningOut) _studentPaymentRequests.value = list
                    }
            }
            val adminPayAndReportJobs = if (isOwnerAdmin) {
                listOf(
                    viewModelScope.launch {
                        paymentReportRepo.observeAdminPaymentRequests()
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                Log.w(TAG, "Admin payment requests stream interrupted", firestoreError ?: error)
                            }
                            .collect { list ->
                                if (!isSigningOut) _adminState.update { it.copy(paymentRequests = list) }
                            }
                    },
                    viewModelScope.launch {
                        paymentReportRepo.observeAdminQuestionReports()
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                Log.w(TAG, "Admin question reports stream interrupted", firestoreError ?: error)
                            }
                            .collect { list ->
                                if (!isSigningOut) _adminState.update { it.copy(questionReports = list) }
                            }
                    }
                )
            } else {
                emptyList()
            }
            listOf(studentPayJob) + adminPayAndReportJobs
        } else {
            emptyList()
        }

        syncListenerJobs = listOf(profileJob, progressJob, mockJob, customJob) + subJobs + engagementJobs + payReportJobs
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

        // 4. Custom Questions (core question bank writes are restricted to Owner Admin)
        mergeCloudCustomQuestionsIntoLocal(snapshot.customQuestions)
        if (isOwnerAdmin) {
            val cloudCustomIds = snapshot.customQuestions.map { it.id }.toSet()
            val (localCustom, localExamMap) = repository.getCustomQuestionsWithExamIds()
            val missingInCloud = localCustom.filter { it.id !in cloudCustomIds }
            if (missingInCloud.isNotEmpty()) {
                cloudSync.upsertCustomQuestions(currentUserId, missingInCloud, localExamMap)
                    .onFailure { recordSyncFailure(it) }
            }
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

            val hasRemoteGamification = remote.totalXp > 0L ||
                remote.weeklyXp > 0L ||
                remote.weeklyQuestions > 0L ||
                remote.unlockedBadgeIds.isNotEmpty() ||
                remote.streakShields > 0L ||
                remote.lastShieldEarnedStreak > 0L ||
                remote.qotdAnsweredCount > 0L ||
                remote.completedSessionsCount > 0L
            val mergedGamification = if (hasRemoteGamification) {
                val remoteGam = GamificationState(
                    totalXp = remote.totalXp.toInt().coerceIn(0, GamificationLogic.MAX_TOTAL_XP),
                    weeklyXp = remote.weeklyXp.toInt().coerceIn(0, GamificationLogic.MAX_WEEKLY_XP),
                    weeklyKey = GamificationLogic.sanitizeWeekKey(remote.weeklyKey),
                    weeklyQuestions = remote.weeklyQuestions.toInt().coerceIn(0, GamificationLogic.MAX_WEEKLY_QUESTIONS),
                    weeklyCorrect = remote.weeklyCorrect.toInt().coerceIn(0, GamificationLogic.MAX_WEEKLY_QUESTIONS),
                    weeklySessions = remote.weeklySessions.toInt().coerceIn(0, GamificationLogic.MAX_WEEKLY_SESSIONS),
                    weeklyClaimedIds = remote.weeklyClaimedIds,
                    unlockedBadgeIds = remote.unlockedBadgeIds,
                    streakShields = remote.streakShields.toInt().coerceIn(0, GamificationLogic.MAX_STREAK_SHIELDS),
                    lastShieldEarnedStreak = remote.lastShieldEarnedStreak.toInt().coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND),
                    lastStreakRecoveryDate = StudyGoalLogic.sanitizeDateKey(remote.lastStreakRecoveryDate),
                    qotdAnsweredCount = remote.qotdAnsweredCount.toInt().coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    completedSessionsCount = remote.completedSessionsCount.toInt().coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    focusSprintsCount = remote.focusSprintsCount.toInt().coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    srsMasteredCount = remote.srsMasteredCount.toInt().coerceIn(0, GamificationLogic.MAX_COUNTER_BOUND),
                    bestSessionAccuracy = remote.bestSessionAccuracy.toInt().coerceIn(0, 100)
                )
                GamificationLogic.mergeWithCloud(
                    local = settings.gamificationState.value,
                    remote = remoteGam
                )
            } else {
                settings.gamificationState.value
            }

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
                mergedGamificationState = mergedGamification,
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
            initialActiveDeviceName = currentDeviceName,
            email = signedInAccount.email,
            lastDeviceHash = deviceIdentity.deviceHash,
            gamificationState = settings.gamificationState.value
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

    private fun closeAllOverlayScreens() {
        _subjectChaptersNav.value = null
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        _isPlansOpen.value = false
        _isAdminOpen.value = false
        _isNotificationsOpen.value = false
        _isDoubtBoxOpen.value = false
        _isClinicalToolsOpen.value = false
        _isEcgStudioOpen.value = false
        _isIbqHubOpen.value = false
        _isSpecialtyTracksOpen.value = false
        _isFlashcardStudioOpen.value = false
        _isClinicalCasesOpen.value = false
        _isGrandTestHubOpen.value = false
    }

    fun selectTab(tab: MainTab) {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
        closeAllOverlayScreens()
        _selectedTab.value = tab
    }

    fun openSettings() {
        closeAllOverlayScreens()
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openProfile() {
        closeAllOverlayScreens()
        _isProfileOpen.value = true
    }

    fun closeProfile() {
        _isProfileOpen.value = false
    }

    fun openPlans() {
        _plansExamLockContext.value = null
        _paywallPrompt.value = null
        _isExamPickerOpen.value = false
        closeAllOverlayScreens()
        _isPlansOpen.value = true
    }

    fun openPlansForExamLock(requestedExamId: String?) {
        val sub = subscriptionState.value
        _plansExamLockContext.value = ExamLockUpgradeContext(
            requestedExamId = requestedExamId?.takeIf { it.isNotBlank() },
            currentUnlockedExamIds = sub.unlockedExamIds
        )
        _paywallPrompt.value = null
        _isExamPickerOpen.value = false
        closeAllOverlayScreens()
        _isPlansOpen.value = true
    }

    fun closePlans() {
        _isPlansOpen.value = false
        _plansExamLockContext.value = null
    }

    fun openAdminConsole() {
        if (!isOwnerAdmin) {
            showMessage("Admin Console is restricted to the app owner.")
            return
        }
        closeAllOverlayScreens()
        _isAdminOpen.value = true
        refreshAdminDashboard()
    }

    fun closeAdminConsole() {
        _isAdminOpen.value = false
    }

    fun openClinicalTools(initialSection: Int = 0) {
        closeAllOverlayScreens()
        _clinicalToolsInitialSection.value = initialSection.coerceIn(0, 4)
        _isClinicalToolsOpen.value = true
    }

    fun closeClinicalTools() {
        _isClinicalToolsOpen.value = false
    }

    fun openEcgStudio() {
        closeAllOverlayScreens()
        _isEcgStudioOpen.value = true
    }

    fun closeEcgStudio() {
        _isEcgStudioOpen.value = false
    }

    fun openIbqHub(initialCategory: IbqCategory = IbqCategory.ALL) {
        closeAllOverlayScreens()
        _ibqHubInitialCategory.value = initialCategory
        _isIbqHubOpen.value = true
    }

    fun closeIbqHub() {
        _isIbqHubOpen.value = false
    }

    fun openSpecialtyTracks(initialTrack: SpecialtyTrack = SpecialtyTrack.CARDIOLOGY) {
        closeAllOverlayScreens()
        _specialtyTrackInitial.value = initialTrack
        _isSpecialtyTracksOpen.value = true
    }

    fun closeSpecialtyTracks() {
        _isSpecialtyTracksOpen.value = false
    }

    fun recordClinicalModulePracticeAnswer(caseId: String, isCorrect: Boolean) {
        val gamResult = GamificationLogic.recordQuestionAnswer(
            current = settings.gamificationState.value,
            questionId = "clinical_$caseId",
            isCorrect = isCorrect,
            lifetimeAnsweredCount = mainState.value.overall.totalAttempts + 1,
            longestStreakDays = settings.studyGoalState.value.longestStreakDays
        )
        applyGamificationResult(gamResult, announceMilestones = true)
        recordDailyStudyProgress(answeredDelta = 1)
    }

    fun openFlashcardStudio() {
        closeAllOverlayScreens()
        _isFlashcardStudioOpen.value = true
    }

    fun closeFlashcardStudio() {
        _isFlashcardStudioOpen.value = false
    }

    fun openClinicalCases() {
        closeAllOverlayScreens()
        _isClinicalCasesOpen.value = true
    }

    fun closeClinicalCases() {
        _isClinicalCasesOpen.value = false
    }

    fun recordClinicalCaseCompleted(caseId: String, correctStages: Int, totalStages: Int) {
        val alreadyCompleted = caseId in settings.completedClinicalCaseIds.value
        val xpAwarded = UnfoldingClinicalCasesCatalog.xpForCaseCompletion(
            correctStages = correctStages,
            totalStages = totalStages,
            alreadyCompleted = alreadyCompleted
        )
        settings.markClinicalCaseCompleted(caseId)
        if (xpAwarded > 0) {
            val gamResult = GamificationLogic.recordClinicalCaseCompleted(
                current = settings.gamificationState.value,
                xpAwarded = xpAwarded,
                correctStages = correctStages,
                totalStages = totalStages,
                lifetimeAnsweredCount = mainState.value.overall.totalAttempts + totalStages,
                longestStreakDays = settings.studyGoalState.value.longestStreakDays
            )
            applyGamificationResult(gamResult, announceMilestones = false)
            recordDailyStudyProgress(answeredDelta = totalStages)
            showMessage("Clinical Case Mastered! +$xpAwarded XP awarded")
        } else {
            showMessage("Case replay complete ($correctStages/$totalStages stages correct)")
        }
    }

    fun setFlashcardMastered(cardId: String, mastered: Boolean) {
        val current = settings.masteredFlashcardIds.value
        val updated = if (mastered) current + cardId else current - cardId
        settings.setMasteredFlashcardIds(updated)
    }

    fun resetFlashcardMastery() {
        settings.setMasteredFlashcardIds(emptySet())
        showMessage("Rapid Recall mastery progress reset.")
    }

    fun recordFocusSessionCompleted(preset: FocusTimerPreset) {
        val updated = FocusTimerLogic.recordCompletedSession(
            current = settings.dailyFocusProgress.value,
            preset = preset
        )
        settings.setDailyFocusProgress(updated)
        if (!preset.isBreak) {
            val gamResult = GamificationLogic.recordFocusSprintCompleted(
                current = settings.gamificationState.value,
                lifetimeAnsweredCount = mainState.value.overall.totalAttempts,
                longestStreakDays = settings.studyGoalState.value.longestStreakDays
            )
            applyGamificationResult(gamResult, announceMilestones = false)
            showMessage("Focus sprint complete! +${preset.durationMinutes} min · +${gamResult.xpAwarded} XP")
        }
    }

    fun setTargetExamDate(dateIso: String) {
        val clean = dateIso.trim()
        if (clean.isNotEmpty() && ExamCountdownLogic.daysUntil(clean) == null) {
            showMessage("Please enter a valid date (YYYY-MM-DD).")
            return
        }
        settings.setTargetExamDateIso(clean)
        showMessage(
            if (clean.isBlank()) "Target exam date cleared."
            else "Exam countdown set for ${ExamCountdownLogic.formatFriendlyDate(clean)}!"
        )
    }

    fun resetSrsCard(questionId: String) {
        val updated = SpacedRepetitionLogic.resetToBox1(settings.srsCards.value, questionId)
        settings.setSrsCards(updated)
        showMessage("Card reset to Box 1d (Due Now).")
    }

    fun markSrsCardMastered(questionId: String) {
        val updated = SpacedRepetitionLogic.markMastered(settings.srsCards.value, questionId)
        settings.setSrsCards(updated)
        val gamResult = GamificationLogic.recordSrsCardMastered(
            current = settings.gamificationState.value,
            lifetimeAnsweredCount = mainState.value.overall.totalAttempts,
            longestStreakDays = settings.studyGoalState.value.longestStreakDays
        )
        applyGamificationResult(gamResult, announceMilestones = false)
        showMessage("Marked as Mastered! +${gamResult.xpAwarded} XP")
    }

    fun removeSrsCard(questionId: String) {
        val updated = SpacedRepetitionLogic.removeCard(settings.srsCards.value, questionId)
        settings.setSrsCards(updated)
        showMessage("Removed from SRS Mistake Notebook.")
    }

    fun queueQuestionToSrs(questionId: String) {
        if (questionId.isBlank()) return
        val updated = SpacedRepetitionLogic.enqueueManualReview(settings.srsCards.value, questionId)
        settings.setSrsCards(updated)
        showMessage("Added to SRS review queue (due now).")
    }

    fun queueAllBookmarkedToSrs() {
        val bookmarkedIds = (mainState.value.questions + mainState.value.crossExamQuestions)
            .asSequence()
            .filter { it.isBookmarked }
            .map { it.id }
            .distinct()
            .toList()
        if (bookmarkedIds.isEmpty()) {
            showMessage("No bookmarked questions to queue for SRS.")
            return
        }
        val (updated, addedCount) = SpacedRepetitionLogic.enqueueBulkManualReview(
            existing = settings.srsCards.value,
            questionIds = bookmarkedIds
        )
        settings.setSrsCards(updated)
        if (addedCount == 0) {
            showMessage("All ${bookmarkedIds.size} bookmarked questions are already due in SRS.")
        } else {
            showMessage("Added $addedCount bookmarked question${if (addedCount == 1) "" else "s"} to SRS review queue.")
        }
    }

    fun answerQuestionOfTheDay(selectedIndex: Int) {
        val todayKey = StudyGoalLogic.todayDateKey()
        val qotd = QuestionOfTheDayCatalog.questionForDate(todayKey)
        if (settings.qotdCompletion.value.isAnsweredFor(todayKey, qotd.id)) return
        if (selectedIndex !in qotd.options.indices) return

        val isCorrect = selectedIndex == qotd.correctIndex
        settings.setQotdCompletion(
            QotdCompletionState(
                dateKey = todayKey,
                questionId = qotd.id,
                selectedIndex = selectedIndex,
                isCorrect = isCorrect
            )
        )
        val gamResult = GamificationLogic.recordQotdCompleted(
            current = settings.gamificationState.value,
            isCorrect = isCorrect,
            lifetimeAnsweredCount = mainState.value.overall.totalAttempts + 1,
            longestStreakDays = settings.studyGoalState.value.longestStreakDays
        )
        applyGamificationResult(gamResult, announceMilestones = true)
        recordDailyStudyProgress(answeredDelta = 1)
    }

    fun saveQuestionNote(questionId: String, rawNote: String) {
        val clean = QuestionNotesLogic.sanitizeNote(rawNote)
        val updated = QuestionNotesLogic.upsertNote(settings.questionNotes.value, questionId, clean)
        settings.setQuestionNotes(updated)
        showMessage(if (clean.isBlank()) "Personal note cleared." else "Personal note saved to your Deck.")
    }

    fun deleteQuestionNote(questionId: String) {
        val updated = QuestionNotesLogic.removeNote(settings.questionNotes.value, questionId)
        settings.setQuestionNotes(updated)
        showMessage("Personal note deleted.")
    }

    fun startSrsDueDrill() {
        val summary = SpacedRepetitionLogic.buildSummary(
            questions = mainState.value.questions,
            storedCards = settings.srsCards.value
        )
        val pool = summary.dueEntries.map { it.item }.ifEmpty { summary.allEntries.map { it.item } }
        if (pool.isEmpty()) {
            showMessage("No SRS mistakes due right now — great job!")
            return
        }
        val title = if (summary.dueEntries.isNotEmpty()) {
            "SRS Due Review (${summary.dueEntries.size}Q)"
        } else {
            "SRS Mistake Notebook (${pool.size}Q)"
        }
        startPractice(pool, title)
    }

    fun startSmartWeakTopicDrill(subjectCode: String? = null, targetCount: Int = 20) {
        val st = mainState.value
        val pool = st.stageQuestions.ifEmpty { st.questions }
        val clampedCount = targetCount.coerceIn(10, 50)
        val selected = SmartWeakTopicPlanner.selectWeightedAutoDrill(
            questions = pool,
            srsCards = settings.srsCards.value,
            targetCount = clampedCount,
            targetSubjectCode = subjectCode
        )
        if (selected.isEmpty()) {
            showMessage("No questions available yet for your weak-topic drill.")
            return
        }
        val ibqCandidates = (pool.filter { it.question.isImageBased }
            .ifEmpty { st.crossExamQuestions.filter { it.question.isImageBased } })
            .let { list ->
                if (!subjectCode.isNullOrBlank()) list.filter { it.question.subject == subjectCode } else list
            }
        val mixed = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = selected,
            ibqPool = ibqCandidates,
            targetCount = selected.size,
            ibqSharePercent = 15,
            minIbqCount = if (ibqCandidates.isNotEmpty() && selected.size >= 10) 2 else 0,
            maxIbqCount = 4
        )
        val title = if (!subjectCode.isNullOrBlank()) {
            "Recovery Plan • $subjectCode (${mixed.size}Q)"
        } else {
            "Smart Weak-Topic Drill (${mixed.size}Q)"
        }
        startPractice(mixed, title)
    }

    fun startGeneralMixedDrill(
        sourcePool: List<QuestionItem>,
        targetCount: Int,
        title: String
    ) {
        if (sourcePool.isEmpty()) {
            showMessage("No questions available in this selection yet.")
            return
        }
        val st = mainState.value
        val ibqPool = sourcePool.filter { it.question.isImageBased }
            .ifEmpty {
                st.stageQuestions.filter { it.question.isImageBased }
                    .ifEmpty { st.crossExamQuestions.filter { it.question.isImageBased } }
            }
        val shuffledGeneral = sourcePool.shuffled()
        val mixed = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = shuffledGeneral,
            ibqPool = ibqPool.shuffled(),
            targetCount = targetCount.coerceAtLeast(1)
        )
        startPractice(mixed, title)
    }

    fun startImageBasedSpotterDrill() {
        val st = mainState.value
        val activePool = st.stageQuestions.ifEmpty { st.questions }
            .filter { it.question.isImageBased }
        val pool = activePool.ifEmpty {
            st.crossExamQuestions.filter { it.question.isImageBased }
        }
        if (pool.isEmpty()) {
            showMessage("No image-based clinical spotters available yet.")
            return
        }
        val ordered = IbqHubLogic.prioritizeForDrill(pool)
        startPractice(ordered, "Image-Based Spotters (${ordered.size}Q)")
    }

    fun openSubjectChapters(
        subjectCode: String,
        isCrossExam: Boolean = false,
        examIdFilter: String? = null
    ) {
        val sub = subscriptionState.value
        if (isCrossExam && sub.isExamLockedForSwitch(examIdFilter)) {
            openPlansForExamLock(examIdFilter)
            return
        }
        if (isCrossExam && examIdFilter == null && !sub.canAccessCrossExamPractice()) {
            showPaywallPrompt(
                title = "All-Exam Subject Mode",
                message = "Cross-exam combined subject drills across all 29 Indian nursing exams are included in All-Exam Nursing Prep or your 3-Day Free Trial.",
                featureTag = "cross_exam"
            )
            return
        }
        closeAllOverlayScreens()
        _chaptersHighYieldOnly.value = false
        _subjectChaptersNav.value = SubjectChaptersNavState(
            subjectCode = subjectCode,
            isCrossExam = isCrossExam,
            examIdFilter = examIdFilter
        )
    }

    fun closeSubjectChapters() {
        _subjectChaptersNav.value = null
    }

    fun toggleChaptersHighYieldOnly() {
        _chaptersHighYieldOnly.update { !it }
    }

    fun dismissPaywallPrompt() {
        _paywallPrompt.value = null
    }

    fun dismissDeviceLockConflict() {
        _deviceLockConflict.value = null
    }

    private fun showPaywallPrompt(
        title: String,
        message: String,
        featureTag: String,
        targetExamId: String? = settings.activeExamId.value
    ) {
        _paywallPrompt.value = PaywallPromptState(
            title = title,
            message = message,
            lockedFeatureTag = featureTag,
            targetExamId = targetExamId
        )
    }

    fun openExamPicker() {
        _isExamPickerOpen.value = true
    }

    fun closeExamPicker() {
        _isExamPickerOpen.value = false
    }

    fun selectActiveExam(examId: String) {
        if (subscriptionState.value.isExamLockedForSwitch(examId)) {
            _isExamPickerOpen.value = false
            openPlansForExamLock(examId)
            return
        }
        val changed = settings.activeExamId.value != examId
        settings.setActiveExamId(examId)
        _isExamPickerOpen.value = false
        if (changed) {
            _activeExamStage.value = ExamStage.PRELIMS
            _subjectHubSection.value = SubjectHubSection.NURSING
            _subjectChaptersNav.value = null
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

    fun selectExamStage(stage: ExamStage) {
        _activeExamStage.value = stage
        if (stage == ExamStage.MAINS && (_subjectHubSection.value == SubjectHubSection.CA_GK || _subjectHubSection.value == SubjectHubSection.NON_TECH)) {
            _subjectHubSection.value = SubjectHubSection.NURSING
        }
    }

    fun selectPracticePathway(pathway: PracticePathway) {
        if (pathway == PracticePathway.BY_SUBJECT) {
            val sub = subscriptionState.value
            if (sub.isSingleExamLocked && sub.isExamLockedForSwitch(_subjectPracticeExamFilter.value)) {
                _subjectPracticeExamFilter.value = sub.primaryUnlockedExamId ?: settings.activeExamId.value
            }
        }
        _practicePathway.value = pathway
    }

    fun selectSubjectPracticeExamFilter(examId: String?) {
        if (subscriptionState.value.isExamLockedForSwitch(examId)) {
            openPlansForExamLock(examId)
            return
        }
        _subjectPracticeExamFilter.value = examId
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
        _subjectChaptersNav.value = null
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        _practicePathway.value = PracticePathway.BY_EXAM
        _subjectHubSection.value = section
        _selectedTab.value = MainTab.SUBJECTS
    }

    fun openSubjectPracticeMode() {
        stopActiveTimer()
        _practiceSession.value = null
        _cbtSession.value = null
        _subjectChaptersNav.value = null
        _isSettingsOpen.value = false
        _isProfileOpen.value = false
        val sub = subscriptionState.value
        if (sub.isSingleExamLocked && sub.isExamLockedForSwitch(_subjectPracticeExamFilter.value)) {
            _subjectPracticeExamFilter.value = sub.primaryUnlockedExamId ?: settings.activeExamId.value
        }
        _practicePathway.value = PracticePathway.BY_SUBJECT
        _selectedTab.value = MainTab.SUBJECTS
    }

    fun startCrossExamSubjectPractice(
        subjectCode: String,
        examIdFilter: String? = _subjectPracticeExamFilter.value,
        difficultyFilter: DifficultyFilter = _difficultyFilter.value
    ) {
        val sub = subscriptionState.value
        if (sub.isExamLockedForSwitch(examIdFilter)) {
            openPlansForExamLock(examIdFilter)
            return
        }
        if (examIdFilter == null && !sub.canAccessCrossExamPractice()) {
            showPaywallPrompt(
                title = "All-Exam Subject Mode",
                message = "Cross-exam combined subject drills across all 29 Indian nursing exams are included in All-Exam Nursing Prep or your 3-Day Free Trial.",
                featureTag = "cross_exam"
            )
            return
        }
        val state = mainState.value
        val items = SubjectPracticeLogic.collectQuestionsForSubject(
            allQuestions = state.crossExamQuestions,
            allLinks = state.crossExamLinks,
            subjectCode = subjectCode,
            examIdFilter = examIdFilter,
            difficultyFilter = difficultyFilter,
            excludePyqAndMock = true,
            alreadyDeduplicated = true,
            includeVirtualShared = true
        )
        if (items.isEmpty()) {
            showMessage("No questions match this subject filter.")
            return
        }
        val subjectName = SubjectCatalog.displayNameFor(subjectCode)
        val scopeLabel = if (examIdFilter == null) {
            "All Exams"
        } else {
            state.availableExams.firstOrNull { it.id == examIdFilter }?.shortName
                ?: PlatformCatalogDefaults.examById(examIdFilter)?.shortName
                ?: "Filtered"
        }
        startPractice(items = items, title = "$subjectName • $scopeLabel")
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
            if (result.status == AppUpdateStatus.UPDATE_AVAILABLE && result.latestVersion.isNotBlank()) {
                val ctx = appContext
                if (ctx != null && result.latestVersion != settings.getLastNotifiedAppUpdateVersion()) {
                    val posted = ClinEdgeNotificationManager.postAppUpdateNotification(
                        context = ctx,
                        latestVersion = result.latestVersion,
                        releaseTitle = result.releaseTitle,
                        releaseNotes = result.releaseNotes
                    )
                    if (posted) {
                        settings.setLastNotifiedAppUpdateVersion(result.latestVersion)
                    }
                }
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
        val all = mainState.value.stageQuestions.ifEmpty { mainState.value.questions }
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
        // Prioritize mistakes and fresh unattempted questions first, blending in visual IBQs seamlessly
        val mistakesAndFresh = primaryPool.filter { it.lastOutcome == "wrong" || it.attemptsCount == 0 }.shuffled()
        val remainder = primaryPool.filter { it.lastOutcome != "wrong" && it.attemptsCount > 0 }.shuffled()
        val orderedGeneral = mistakesAndFresh + remainder
        val ibqCandidates = primaryPool.filter { it.question.isImageBased }
            .ifEmpty {
                if (focus.isBlank()) {
                    mainState.value.crossExamQuestions.filter { it.question.isImageBased }
                } else {
                    emptyList()
                }
            }
        val picked = IbqHubLogic.mixIbqsIntoGeneralPractice(
            generalPool = orderedGeneral,
            ibqPool = ibqCandidates,
            targetCount = targetCount
        )
        val drillTitle = if (remaining > 0) {
            "Daily Goal Drill (${picked.size}Q remaining)"
        } else {
            "Bonus Daily Drill (${picked.size}Q)"
        }
        startPractice(items = picked, title = drillTitle)
    }

    private fun applyGamificationResult(
        result: GamificationUpdateResult,
        announceMilestones: Boolean
    ) {
        val changed = result.updatedState != settings.gamificationState.value
        if (changed) {
            settings.setGamificationState(result.updatedState)
            scheduleProfileCloudPush()
        }
        if (!announceMilestones) return
        when {
            result.leveledUpTo != null -> {
                val rank = GamificationLogic.rankTitleForLevel(result.leveledUpTo)
                showMessage("Level Up! Level ${result.leveledUpTo} · $rank")
            }
            result.earnedStreakShield -> {
                showMessage("7-Day Milestone! +${GamificationLogic.XP_STREAK_7DAY_BONUS} XP & +1 free Streak Shield earned!")
            }
            result.newlyUnlockedBadges.isNotEmpty() -> {
                val badge = result.newlyUnlockedBadges.first()
                showMessage("Badge unlocked: ${badge.title}!")
            }
            result.newlyClaimedChallenges.isNotEmpty() -> {
                val ch = result.newlyClaimedChallenges.first()
                showMessage("Weekly Challenge complete: ${ch.title} (+${ch.rewardXp} XP)!")
            }
        }
    }

    private fun recordDailyStudyProgress(answeredDelta: Int) {
        if (answeredDelta <= 0) return
        val canUseShield = settings.gamificationState.value.streakShields > 0
        val result = StudyGoalLogic.recordStudyActivity(
            current = settings.studyGoalState.value,
            answeredDelta = answeredDelta,
            allowStreakRecovery = canUseShield
        )
        settings.setStudyGoalState(result.updatedState)

        if (result.justMetDailyGoal || result.usedStreakRecovery) {
            val gamResult = GamificationLogic.recordDailyGoalAndStreakEvent(
                current = settings.gamificationState.value,
                justMetDailyGoal = result.justMetDailyGoal,
                usedStreakRecovery = result.usedStreakRecovery,
                currentStreakDays = result.updatedState.currentStreakDays,
                longestStreakDays = result.updatedState.longestStreakDays,
                lifetimeAnsweredCount = mainState.value.overall.totalAttempts + answeredDelta
            )
            applyGamificationResult(gamResult, announceMilestones = false)
            val days = result.updatedState.currentStreakDays
            val shieldNote = if (result.usedStreakRecovery) " (Streak Shield saved your streak!)" else ""
            val bonusNote = if (gamResult.xpAwarded > 0) " +${gamResult.xpAwarded} XP" else ""
            showMessage("Daily goal complete! $days-day streak$shieldNote$bonusNote")
        } else {
            scheduleProfileCloudPush()
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
        val activeExamId = settings.activeExamId.value
        val sub = subscriptionState.value
        val isPyqSet = title.contains("PYQ", ignoreCase = true) ||
            title.contains("Paper", ignoreCase = true) ||
            (items.isNotEmpty() && items.all { it.question.isPyqTagged })
        if (isPyqSet && !sub.canAccessPyq(activeExamId)) {
            showPaywallPrompt(
                title = "Unlock Previous Year Papers",
                message = "Shift-wise PYQ papers for ${EntitlementEvaluator.shortExamLabel(activeExamId)} require an Exam Plan, All-Exam Prep, or your 3-Day Free Trial.",
                featureTag = "pyq",
                targetExamId = activeExamId
            )
            return
        }
        if (!sub.canAnswerPracticeQuestion(activeExamId)) {
            showPaywallPrompt(
                title = "Daily Free Limit Reached (${sub.dailyFreeLimit}/${sub.dailyFreeLimit})",
                message = "You've completed today's ${sub.dailyFreeLimit} free practice questions. Start your 3-Day Free Trial or redeem an access code for unlimited questions.",
                featureTag = "daily_limit",
                targetExamId = activeExamId
            )
            return
        }
        val gatedItems = if (!sub.hasPremiumForExam(activeExamId)) {
            val remaining = sub.remainingFreeQuestionsToday.coerceAtLeast(1)
            items.take(remaining)
        } else {
            items
        }
        stopActiveTimer()
        _cbtSession.value = null
        val actualCountdown = timedMode && hasCountdown && durationMinutes > 0
        val totalSec = if (actualCountdown) durationMinutes.coerceAtLeast(1) * 60 else 0
        val activeNeg = mainState.value.activeExam?.negativeMarks
            ?: PlatformCatalogDefaults.negativeMarksForExam(activeExamId)
        _sessionTimerState.value = SessionTimerState(
            remainingSeconds = totalSec,
            sectionRemainingSeconds = 0
        )
        _practiceSession.value = PracticeSessionState(
            title = title,
            questions = gatedItems,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            timedMode = timedMode,
            hasCountdown = actualCountdown,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            negativeMarks = activeNeg
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
                val nextSec = (_sessionTimerState.value.remainingSeconds - 1).coerceAtLeast(0)
                _sessionTimerState.value = SessionTimerState(remainingSeconds = nextSec, sectionRemainingSeconds = 0)
                if (nextSec == 0) {
                    _practiceSession.update {
                        it?.copy(remainingSeconds = 0, showTimeoutDialog = true)
                    }
                    break
                }
            }
        }
    }

    fun answerPracticeQuestion(optionIndex: Int) {
        val sess = _practiceSession.value ?: return
        if (sess.isSubmittedSummary) return
        val qItem = sess.questions.getOrNull(sess.currentIndex) ?: return
        if (sess.selectedAnswers.containsKey(qItem.id)) return

        val activeExamId = settings.activeExamId.value
        val sub = subscriptionState.value
        if (!sub.canAnswerPracticeQuestion(activeExamId)) {
            showPaywallPrompt(
                title = "Daily Free Limit Reached (${sub.dailyFreeLimit}/${sub.dailyFreeLimit})",
                message = "Upgrade or start your 3-Day Free Trial to keep practicing without daily limits.",
                featureTag = "daily_limit",
                targetExamId = activeExamId
            )
            return
        }

        // 1. Instant UI state update for 120 FPS animation start
        val updatedMap = sess.selectedAnswers + (qItem.id to optionIndex)
        val isCorrect = optionIndex == qItem.question.correctIndex
        val allAnswered = updatedMap.size == sess.questions.size
        _practiceSession.value = sess.copy(
            selectedAnswers = updatedMap,
            showCompletionPrompt = sess.timedMode && allAnswered
        )

        if (!sub.hasPremiumForExam(activeExamId)) {
            settings.incrementDailyFreeQuotaUsed()
        }
        val practiceGam = GamificationLogic.recordQuestionAnswer(
            current = settings.gamificationState.value,
            questionId = qItem.id,
            isCorrect = isCorrect,
            lifetimeAnsweredCount = mainState.value.overall.totalAttempts + 1,
            longestStreakDays = settings.studyGoalState.value.longestStreakDays
        )
        applyGamificationResult(practiceGam, announceMilestones = true)
        recordDailyStudyProgress(answeredDelta = 1)

        // Update Spaced Repetition (SRS) Mistake Notebook card state
        val qId = qItem.id
        val currentSrs = settings.srsCards.value
        val nextSrs = if (isCorrect) {
            SpacedRepetitionLogic.recordRight(currentSrs, qId)
        } else {
            SpacedRepetitionLogic.recordWrong(currentSrs, qId)
        }
        if (nextSrs !== currentSrs) {
            settings.setSrsCards(nextSrs)
        }

        // 2. Offload Room write to IO and debounce cloud write by 350ms so zero hitching overlaps rationale reveal
        practiceCloudSyncJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val savedProgress = repository.recordAttempt(qId, isCorrect)
            if (currentUserId.isNotBlank() && !isSigningOut) {
                practiceCloudSyncJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(350L)
                    cloudSync.upsertProgress(currentUserId, savedProgress)
                        .onSuccess { markSynced() }
                        .onFailure { recordSyncFailure(it) }
                }
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
        val currentSess = _practiceSession.value
        stopActiveTimer()
        if (currentSess != null && !currentSess.isSubmittedSummary &&
            currentSess.selectedAnswers.size >= GamificationLogic.SESSION_BONUS_MIN_QUESTIONS
        ) {
            val rightCount = currentSess.questions.count { q ->
                currentSess.selectedAnswers[q.id] == q.question.correctIndex
            }
            val acc = ((rightCount.toFloat() / currentSess.selectedAnswers.size.coerceAtLeast(1)) * 100f).toInt()
            val gamResult = GamificationLogic.recordTimedPracticeCompleted(
                current = settings.gamificationState.value,
                answeredCount = currentSess.selectedAnswers.size,
                accuracyPercent = acc,
                lifetimeAnsweredCount = mainState.value.overall.totalAttempts,
                longestStreakDays = settings.studyGoalState.value.longestStreakDays
            )
            applyGamificationResult(gamResult, announceMilestones = true)
        }
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
    private fun timedConfigBasePool(cfg: TimedPracticeConfigState): List<QuestionItem> {
        val st = mainState.value
        if (cfg.useCrossExamPool) {
            val filterId = cfg.crossExamFilterId
            if (filterId.isNullOrBlank()) return st.crossExamQuestions
            val allowedIds = st.crossExamLinks.asSequence()
                .filter { it.examId == filterId }
                .map { it.questionId }
                .toHashSet()
            return st.crossExamQuestions.filter { item ->
                item.id in allowedIds ||
                    PlatformCatalogDefaults.shouldVirtuallyProjectQuestion(filterId, item.question)
            }
        }
        return st.stageQuestions.ifEmpty { st.questions }
    }

    private fun suggestedTimedPracticeMinutes(
        questionCount: Int,
        sourceKey: String,
        difficultyFilter: DifficultyFilter,
        useCrossExamPool: Boolean = false,
        crossExamFilterId: String? = null
    ): Int {
        val base = timedConfigBasePool(
            TimedPracticeConfigState(
                sourceKey = sourceKey,
                difficultyFilter = difficultyFilter,
                useCrossExamPool = useCrossExamPool,
                crossExamFilterId = crossExamFilterId
            )
        )
        val effectiveCount = if (questionCount <= 0) {
            filteredQuestionsForSource(sourceKey, base, difficultyFilter).size
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
        defaultDifficulty: DifficultyFilter = _difficultyFilter.value,
        useCrossExamPool: Boolean = false,
        crossExamFilterId: String? = null
    ) {
        val defaultMins = suggestedTimedPracticeMinutes(
            questionCount = defaultCount,
            sourceKey = defaultSource,
            difficultyFilter = defaultDifficulty,
            useCrossExamPool = useCrossExamPool,
            crossExamFilterId = crossExamFilterId
        )
        _timedConfig.value = TimedPracticeConfigState(
            isVisible = true,
            sourceKey = defaultSource,
            questionCount = defaultCount,
            durationMinutes = defaultMins,
            difficultyFilter = defaultDifficulty,
            useCrossExamPool = useCrossExamPool,
            crossExamFilterId = crossExamFilterId
        )
    }

    fun closeTimedPracticeConfig() {
        _timedConfig.update { it.copy(isVisible = false) }
    }

    fun updateTimedConfigSource(sourceKey: String) {
        _timedConfig.update { cfg ->
            val updatedMins = if (cfg.questionCount <= 0) {
                suggestedTimedPracticeMinutes(cfg.questionCount, sourceKey, cfg.difficultyFilter, cfg.useCrossExamPool, cfg.crossExamFilterId)
            } else {
                cfg.durationMinutes
            }
            cfg.copy(sourceKey = sourceKey, durationMinutes = updatedMins)
        }
    }

    fun updateTimedConfigDifficulty(filter: DifficultyFilter) {
        _timedConfig.update { cfg ->
            val updatedMins = if (cfg.questionCount <= 0) {
                suggestedTimedPracticeMinutes(cfg.questionCount, cfg.sourceKey, filter, cfg.useCrossExamPool, cfg.crossExamFilterId)
            } else {
                cfg.durationMinutes
            }
            cfg.copy(difficultyFilter = filter, durationMinutes = updatedMins)
        }
    }

    fun updateTimedConfigCount(count: Int) {
        _timedConfig.update { cfg ->
            val suggestedMinutes = suggestedTimedPracticeMinutes(count, cfg.sourceKey, cfg.difficultyFilter, cfg.useCrossExamPool, cfg.crossExamFilterId)
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
            "MIXED" -> all.filter { !it.question.isPyqTagged }
            "PYQ" -> all.filter { it.question.isPyqTagged }
            "PREDICTED_MOCK" -> all.filter { it.question.isPredictedMock }
            "NURSING" -> all.filter { !it.question.isPyqTagged && !it.question.isPredictedMock && SubjectCatalog.isNursing(it.question) }
            "CA" -> all.filter { !it.question.isPyqTagged && !it.question.isPredictedMock && SubjectCatalog.isCa(it.question) }
            "GEN" -> all.filter { !it.question.isPyqTagged && !it.question.isPredictedMock && SubjectCatalog.isGen(it.question) }
            else -> all.filter { !it.question.isPyqTagged && !it.question.isPredictedMock && it.question.subject == sourceKey }
        }
        val targetDiff = difficultyFilter.difficulty ?: return bySource
        return bySource.filter { it.question.difficulty == targetDiff }
    }

    fun timedConfigMatchingCount(sourceKey: String, difficultyFilter: DifficultyFilter): Int {
        val cfg = _timedConfig.value
        val base = timedConfigBasePool(cfg)
        return filteredQuestionsForSource(sourceKey, base, difficultyFilter).size
    }

    fun launchConfiguredTimedPractice() {
        val cfg = _timedConfig.value
        val pool = filteredQuestionsForSource(
            sourceKey = cfg.sourceKey,
            all = timedConfigBasePool(cfg),
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
            "PYQ" -> when {
                PlatformCatalogDefaults.isPgimerExam(activeExamId) -> "PGIMER PYQs"
                PlatformCatalogDefaults.isNorcetExam(activeExamId) -> "NORCET Prelims PYQs"
                else -> "PYQs"
            }
            "NURSING" -> "Nursing ($nursingPct%)"
            "CA" -> if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "GK & Health" else "CA & Static GK"
            "GEN" -> if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "Aptitude & Science" else "Sci/Math/Reas"
            else -> SubjectCatalog.displayNameFor(cfg.sourceKey)
        }
        val crossSuffix = if (cfg.useCrossExamPool && cfg.crossExamFilterId == null) " • All Exams" else ""
        val diffSuffix = cfg.difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
        val useLimit = settings.useTimeLimit.value
        val titlePrefix = if (useLimit) "Timed" else "Drill"
        _timedConfig.update { it.copy(isVisible = false) }
        startPractice(
            items = picked,
            title = "$titlePrefix: $sourceLabel$crossSuffix$diffSuffix (${picked.size}Q)",
            timedMode = true,
            durationMinutes = if (useLimit) cfg.durationMinutes else 0,
            hasCountdown = useLimit
        )
    }

    // ------------------------------------------------------------------------
    // CBT MOCK SIMULATOR (STRICT PRELIMS / MAINS SEPARATION FOR NORCET)
    // ------------------------------------------------------------------------
    fun startCbtStandardMock(
        totalQuestions: Int,
        durationMinutes: Int?,
        title: String,
        isOfficialFullMock: Boolean = false
    ) {
        val activeExamId = settings.activeExamId.value
        val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExamId)
        // Strictly restrict NORCET Prelims mocks to Prelims-only non-PYQ pool (prioritizing Predicted Mocks)
        val stageScoped = if (isNorcet) {
            PlatformCatalogDefaults.filterQuestionsForExamStage(
                examId = activeExamId,
                stage = ExamStage.PRELIMS,
                questions = mainState.value.questions
            )
        } else {
            mainState.value.questions
        }
        val selected = MockSelectionLogic.selectStandardMock(
            pool = stageScoped,
            examId = activeExamId,
            totalQuestions = totalQuestions,
            shuffle = true
        )
        if (selected.isEmpty()) {
            showMessage("No mock questions available for this exam yet.")
            return
        }
        val isNorcetFullPrelims = isOfficialFullMock && isNorcet && totalQuestions == 100
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

    fun startNorcetMainsMock(
        totalQuestions: Int = 160,
        durationMinutes: Int = 180,
        title: String = "NORCET Stage II Mains (160Q Clinical CBT)"
    ) {
        // Strictly restrict NORCET Mains mocks to non-PYQ Stage II Mains clinical pool (Predicted Mocks first)
        val selected = MockSelectionLogic.selectNorcetMainsMock(
            allQuestions = mainState.value.questions,
            totalQuestions = totalQuestions,
            shuffle = true
        )
        if (selected.isEmpty()) {
            showMessage("No Mains clinical questions available.")
            return
        }
        val useSectionLock = totalQuestions >= 160 && selected.size >= 160
        startCbtCustom(
            items = selected,
            title = title,
            durationMinutes = durationMinutes,
            isOfficialFullMock = totalQuestions >= 160,
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
        sectionLabels: List<String> = emptyList(),
        hasCountdownOverride: Boolean? = null,
        negativeMarksOverride: Double? = null
    ) {
        if (items.isEmpty()) {
            showMessage("No questions available for this CBT test.")
            return
        }
        val activeExamId = settings.activeExamId.value
        val sub = subscriptionState.value
        if (!sub.canStartFullCbtMock(activeExamId)) {
            val isEligibleSampleMock = !isOfficialFullMock && items.size <= 25
            if (isEligibleSampleMock && sub.canStartSampleMock()) {
                settings.markFreeSampleMockUsed()
                showMessage("Starting your 1 Free Sample CBT Mock. Good luck!")
            } else {
                val reasonMsg = if (isOfficialFullMock || items.size > 25) {
                    "Full-length CBT Mock Exams are unlocked with an Exam Plan, All-Exam Prep, or your 3-Day Free Trial. Free accounts include 1 sample mini-mock."
                } else {
                    "You've already used your 1 free sample CBT mock. Activate a 3-Day Free Trial or redeem an access code for unlimited CBT mocks."
                }
                showPaywallPrompt(
                    title = "Unlock Unlimited CBT Mocks",
                    message = reasonMsg,
                    featureTag = "cbt_mock",
                    targetExamId = activeExamId
                )
                return
            }
        }
        stopActiveTimer()
        _practiceSession.value = null
        val activeExam = mainState.value.activeExam
        val useSectionLock = sectionSize > 0 && sectionDurationMinutes > 0 && items.size > sectionSize
        val hasCountdown = hasCountdownOverride ?: (isOfficialFullMock || useSectionLock || settings.useTimeLimit.value)
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
        val activeNeg = negativeMarksOverride
            ?: activeExam?.negativeMarks
            ?: PlatformCatalogDefaults.negativeMarksForExam(settings.activeExamId.value)
        _sessionTimerState.value = SessionTimerState(
            remainingSeconds = totalSec,
            sectionRemainingSeconds = sectionDurationSec
        )
        _cbtSession.value = CbtSessionState(
            title = title,
            questions = items,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            paletteStatuses = initialStatuses,
            hasCountdown = hasCountdown,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            negativeMarks = activeNeg,
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

                val timerSnapshot = _sessionTimerState.value
                if (current.hasSectionLock) {
                    val nextSectionSec = (timerSnapshot.sectionRemainingSeconds - 1).coerceAtLeast(0)
                    val nextTotalSec = (timerSnapshot.remainingSeconds - 1).coerceAtLeast(0)
                    _sessionTimerState.value = SessionTimerState(
                        remainingSeconds = nextTotalSec,
                        sectionRemainingSeconds = nextSectionSec
                    )
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
                    }
                } else {
                    val nextSec = (timerSnapshot.remainingSeconds - 1).coerceAtLeast(0)
                    _sessionTimerState.value = SessionTimerState(
                        remainingSeconds = nextSec,
                        sectionRemainingSeconds = 0
                    )
                    if (nextSec == 0) {
                        _cbtSession.update { it?.copy(remainingSeconds = 0, showTimeoutDialog = true) }
                        break
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
        _sessionTimerState.value = SessionTimerState(
            remainingSeconds = syncedTotalRemaining,
            sectionRemainingSeconds = session.sectionDurationSeconds
        )
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

        val netScore = right - (wrong * sess.negativeMarks)
        val attempted = right + wrong
        val acc = if (attempted > 0) ((right.toFloat() / attempted) * 100f).toInt() else 0

        _cbtSession.value = sess.copy(
            isSubmitted = true,
            showSubmitConfirm = false,
            showTimeoutDialog = false
        )

        if (batchResults.isNotEmpty()) {
            val batchGam = GamificationLogic.recordBatchSessionCompleted(
                current = settings.gamificationState.value,
                results = batchResults,
                accuracyPercent = acc,
                lifetimeAnsweredCount = mainState.value.overall.totalAttempts + batchResults.size,
                longestStreakDays = settings.studyGoalState.value.longestStreakDays
            )
            applyGamificationResult(batchGam, announceMilestones = true)
            recordDailyStudyProgress(answeredDelta = batchResults.size)
            var updatedSrs = settings.srsCards.value
            batchResults.forEach { (qId, isRight) ->
                updatedSrs = if (isRight) {
                    SpacedRepetitionLogic.recordRight(updatedSrs, qId)
                } else {
                    SpacedRepetitionLogic.recordWrong(updatedSrs, qId)
                }
            }
            if (updatedSrs !== settings.srsCards.value) {
                settings.setSrsCards(updatedSrs)
            }
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

                val gtId = sess.grandTestId
                if (!gtId.isNullOrBlank() && grandTestRepo != null) {
                    val liveRemaining = _sessionTimerState.value.remainingSeconds
                    val elapsedSec = (sess.totalSeconds - liveRemaining).coerceAtLeast(0)
                    val displayName = settings.profileName.value.ifBlank { signedInAccount.displayName }.ifBlank { "Nursing Aspirant" }
                    grandTestRepo.submitGrandTestEntry(
                        testId = gtId,
                        userId = currentUserId,
                        studentName = displayName,
                        examId = sess.grandTestExamId ?: settings.activeExamId.value,
                        netScore = netScore,
                        correctCount = right,
                        wrongCount = wrong,
                        skippedCount = skipped,
                        totalQuestions = sess.questions.size,
                        accuracyPercent = acc,
                        timeTakenSeconds = elapsedSec
                    ).onSuccess {
                        showMessage("Official Grand Test submitted! View your All-India Rank & Percentile.")
                        selectGrandTestForLeaderboard(gtId)
                    }.onFailure { err ->
                        Log.w(TAG, "Grand test entry submission skipped/failed", err)
                    }
                }
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
        if (!isOwnerAdmin) {
            showMessage("Core question bank imports are restricted to Owner Admin.")
            return
        }
        val activeExamId = settings.activeExamId.value
        val defaultShift = when {
            PlatformCatalogDefaults.isPgimerExam(activeExamId) -> "PGIMER-2025"
            PlatformCatalogDefaults.isNorcetExam(activeExamId) -> "NORCET-10"
            else -> "2025-S1"
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
        if (!isOwnerAdmin) return
        _importerState.update(transform)
    }

    fun importPdfFileForImporter(context: android.content.Context, uri: Uri, displayFileName: String) {
        if (!isOwnerAdmin) {
            showMessage("Core question bank imports are restricted to Owner Admin.")
            return
        }
        viewModelScope.launch {
            val result = PdfTextExtractor.extractFromUri(context, uri)
            result.onSuccess { extracted ->
                _importerState.update { st ->
                    st.copy(
                        rawText = extracted.text,
                        sourceNote = st.sourceNote.ifBlank { "Imported PDF: $displayFileName" }
                    )
                }
                if (extracted.isLikelyScannedPdf) {
                    showMessage("This PDF appears image-scanned without selectable text. Paste OCR text below to parse.")
                } else {
                    val st = _importerState.value
                    val parsed = parseImporterDraftOffMain(st)
                    _importerState.update { it.copy(previewQuestions = parsed, hasPreviewed = true) }
                    if (parsed.isNotEmpty()) {
                        showMessage("Extracted $displayFileName — detected ${parsed.size} MCQ(s).")
                    } else {
                        showMessage("Extracted $displayFileName (${extracted.text.length} chars). Review text and tap Parse & Preview.")
                    }
                }
            }.onFailure { err ->
                showMessage("Could not read file: ${err.message}")
            }
        }
    }

    private suspend fun parseImporterDraftOffMain(st: ImporterUiState): List<Question> =
        withContext(Dispatchers.Default) {
            val raw = st.rawText.trim()
            if (raw.isBlank()) return@withContext emptyList()
            val effectiveTopicId = BulkQuestionParser.slugTopicId(st.subject, st.topicName)
            if (raw.startsWith("[") || raw.startsWith("{")) {
                runCatching { BulkQuestionParser.parseJsonBatch(raw, markAsCustom = true) }
                    .getOrDefault(emptyList())
            } else {
                BulkQuestionParser.parseRawPdfText(
                    rawText = raw,
                    category = st.category,
                    subject = st.subject,
                    shiftYear = st.shiftYear,
                    defaultDifficulty = st.difficulty,
                    defaultSourceNote = st.sourceNote,
                    markAsVerified = st.markAsVerified,
                    defaultSourceType = st.sourceType,
                    defaultTopicId = effectiveTopicId
                )
            }
        }

    fun previewPdfTextImport() {
        if (!isOwnerAdmin) {
            showMessage("Core question bank imports are restricted to Owner Admin.")
            return
        }
        val st = _importerState.value
        viewModelScope.launch {
            val parsed = parseImporterDraftOffMain(st)
            _importerState.update { it.copy(previewQuestions = parsed, hasPreviewed = true) }
        }
    }

    fun commitPreviewImport() {
        if (!isOwnerAdmin) {
            showMessage("Core question bank changes are restricted to Owner Admin.")
            return
        }
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
        if (!isOwnerAdmin) {
            showMessage("Core question bank changes are restricted to Owner Admin.")
            return
        }
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
        val sub = subscriptionState.value
        val planInfo = BulkQuestionParser.BackupPlanInfo(
            planName = sub.planDisplayName,
            status = when {
                sub.isSuspended -> "Suspended"
                sub.isExpired -> "Expired"
                else -> "Active"
            },
            tier = sub.effectiveTier.dbValue,
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
                repository.restoreBackupWithDetails(
                    jsonText = jsonText,
                    defaultExamId = settings.activeExamId.value,
                    allowCustomQuestions = isOwnerAdmin
                )
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
                    if (!subscriptionState.value.isExamLockedForSwitch(examId)) {
                        settings.setActiveExamId(examId)
                    }
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
                val customPrefix = if (isOwnerAdmin) "${restored.customQuestions.size} custom Qs, " else ""
                showMessage(
                    "Backup restored (${customPrefix}${restored.progressList.size} progress items$profileSummary$planSummary)."
                )
            }.onFailure {
                showMessage("Failed to restore backup: ${it.message}")
            }
        }
    }

    fun clearCustomQuestions() {
        if (!isOwnerAdmin) {
            showMessage("Core question bank changes are restricted to Owner Admin.")
            return
        }
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
        val activeExamId = settings.activeExamId.value
        if (!subscriptionState.value.canExportPdf(activeExamId)) {
            showPaywallPrompt(
                title = "Unlock Printable PDF Worksheets",
                message = "Watermarked A4 study sheets & offline mock booklets require an Exam Plan, All-Exam Prep, or your 3-Day Free Trial.",
                featureTag = "pdf_export",
                targetExamId = activeExamId
            )
            return
        }
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
            watermarkPlanName = subscriptionState.value.planDisplayName,
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

    // ------------------------------------------------------------------------
    // SUBSCRIPTION, 3-DAY DEVICE-LOCKED TRIAL & ACCESS CODE REDEMPTION
    // ------------------------------------------------------------------------
    fun claimThreeDayFreeTrial() {
        val repo = subscriptionRepo
        if (repo == null || currentUserId.isBlank()) {
            showMessage("Sign in with Google to start your 3-Day Free Trial.")
            return
        }
        if (_isTrialBusy.value) return
        viewModelScope.launch {
            _isTrialBusy.value = true
            val trialDays = _subscriptionConfig.value.trialDays.toInt().coerceIn(1, 3)
            val result = repo.claimThreeDayFreeTrial(
                userId = currentUserId,
                email = signedInAccount.email,
                displayName = settings.profileName.value.ifBlank { signedInAccount.displayName },
                device = deviceIdentity,
                trialDays = trialDays
            )
            _isTrialBusy.value = false
            when (result) {
                is TrialClaimResult.Success -> {
                    _clockRefreshTick.value = System.currentTimeMillis()
                    showMessage("3-Day All-Access Free Trial activated! Ends ${EntitlementEvaluator.formatShortDate(result.expiresAtMs)}.")
                }
                is TrialClaimResult.DeviceAlreadyLocked -> {
                    _deviceLockConflict.value = DeviceLockConflictState(
                        shortDeviceCode = result.shortDeviceCode,
                        maskedOwnerEmail = result.maskedEmail,
                        deviceName = result.deviceName
                    )
                }
                TrialClaimResult.TrialAlreadyUsedByAccount -> {
                    showMessage("Your account has already used its 3-day free trial. Redeem a plan code or contact support.")
                }
                is TrialClaimResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    fun redeemAccessCode(rawCode: String, onSuccess: () -> Unit = {}) {
        val repo = subscriptionRepo
        if (repo == null || currentUserId.isBlank()) {
            showMessage("Sign in with Google before redeeming an access code.")
            return
        }
        if (_isRedeemBusy.value) return
        viewModelScope.launch {
            _isRedeemBusy.value = true
            val result = repo.redeemAccessCode(
                userId = currentUserId,
                email = signedInAccount.email,
                displayName = settings.profileName.value.ifBlank { signedInAccount.displayName },
                rawCode = rawCode
            )
            _isRedeemBusy.value = false
            when (result) {
                is CodeRedeemResult.Success -> {
                    _clockRefreshTick.value = System.currentTimeMillis()
                    showMessage("Activated ${result.planLabel} until ${EntitlementEvaluator.formatShortDate(result.expiresAtMs)}!")
                    onSuccess()
                }
                is CodeRedeemResult.InvalidOrUsed -> showMessage(result.message)
                is CodeRedeemResult.Error -> showMessage(result.message)
            }
        }
    }

    // ------------------------------------------------------------------------
    // OWNER ADMIN CONSOLE (Verified Owner Email Only)
    // ------------------------------------------------------------------------
    fun selectAdminTab(tab: AdminConsoleTab) {
        _adminState.update { it.copy(selectedTab = tab) }
    }

    fun refreshAdminDashboard() {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isLoading = true) }
            val users = repo.adminFetchUserDirectory().getOrDefault(_adminState.value.users)
            val codes = repo.adminFetchAccessCodes().getOrDefault(_adminState.value.accessCodes)
            val locks = repo.adminFetchDeviceLocks().getOrDefault(_adminState.value.deviceLocks)
            val packs = repo.adminFetchAllQuestionPacks().getOrDefault(_adminState.value.questionPacks)
            val grandTests = grandTestRepo?.adminFetchAllGrandTests()?.getOrDefault(_adminState.value.grandTests)
                ?: _adminState.value.grandTests
            _adminState.update {
                it.copy(
                    isLoading = false,
                    users = users,
                    accessCodes = codes,
                    deviceLocks = locks,
                    questionPacks = packs,
                    grandTests = grandTests
                )
            }
        }
    }

    fun adminGrantOrUpdateUserPlan(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        tier: PlanTier,
        examIds: List<String>,
        status: SubscriptionStatus,
        tenureDays: Int?,
        extendFromExisting: Boolean = true
    ) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminUpdateUserEntitlement(
                targetUid = targetUid,
                targetEmail = targetEmail,
                targetDisplayName = targetName,
                planTier = tier,
                unlockedExamIds = examIds,
                status = status,
                tenureDays = tenureDays,
                extendFromCurrentExpiry = extendFromExisting
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Updated plan for ${targetName.ifBlank { targetEmail }}.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Failed to update user: ${err.message}")
            }
        }
    }

    fun adminGenerateAccessCode(
        planTier: PlanTier,
        examId: String?,
        tenure: PlanTenure,
        buyerNote: String
    ) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminGenerateAccessCode(planTier, examId, tenure, buyerNote)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess { code ->
                _adminState.update { it.copy(lastGeneratedCode = code) }
                showMessage("Generated code: ${code.code}")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Could not generate code: ${err.message}")
            }
        }
    }

    fun adminRevokeAccessCode(codeId: String) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminRevokeAccessCode(codeId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Revoked access code $codeId.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Revoke failed: ${err.message}")
            }
        }
    }

    fun adminResetDeviceLock(deviceHash: String, shortCode: String) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminDeleteDeviceLock(deviceHash)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Device lock $shortCode cleared. That phone can start a trial again.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Reset failed: ${err.message}")
            }
        }
    }

    fun adminSavePricingConfig(updated: CloudSubscriptionConfig) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val withVersion = updated.copy(catalogVersion = _subscriptionConfig.value.catalogVersion)
            val res = repo.adminSaveSubscriptionConfig(withVersion)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                _subscriptionConfig.value = withVersion
                showMessage("Live pricing & support config saved!")
            }.onFailure { err ->
                showMessage("Save failed: ${err.message}")
            }
        }
    }

    fun adminUpdateContentDraft(transform: (AdminContentDraftState) -> AdminContentDraftState) {
        _adminState.update { it.copy(contentDraft = transform(it.contentDraft)) }
    }

    fun adminExtractDocumentForPack(context: android.content.Context, uri: Uri, displayFileName: String) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update {
                it.copy(contentDraft = it.contentDraft.copy(isParsingOrExtracting = true))
            }
            val result = PdfTextExtractor.extractFromUri(context, uri)
            result.onSuccess { extracted ->
                val cleanTitle = _adminState.value.contentDraft.title.ifBlank {
                    displayFileName.substringBeforeLast('.').take(80)
                }
                _adminState.update { st ->
                    st.copy(
                        contentDraft = st.contentDraft.copy(
                            title = cleanTitle,
                            rawText = extracted.text,
                            extractedFileName = displayFileName,
                            isParsingOrExtracting = false
                        )
                    )
                }
                if (extracted.isLikelyScannedPdf) {
                    showMessage("This PDF appears scanned (image-only). Paste OCR text into the box below to parse.")
                } else {
                    adminPreviewContentPack()
                }
            }.onFailure { err ->
                _adminState.update {
                    it.copy(contentDraft = it.contentDraft.copy(isParsingOrExtracting = false))
                }
                showMessage("Could not read file: ${err.message}")
            }
        }
    }

    fun adminPreviewContentPack() {
        val draft = _adminState.value.contentDraft
        val raw = draft.rawText.trim()
        if (raw.isBlank()) {
            showMessage("Pick a PDF/TXT/JSON file or paste MCQ text first.")
            return
        }
        viewModelScope.launch {
            _adminState.update {
                it.copy(contentDraft = it.contentDraft.copy(isParsingOrExtracting = true))
            }
            val isAutoSubject = draft.subject.equals(QuestionSubjectClassifier.AUTO_SUBJECT_CODE, ignoreCase = true)
            val parsed = withContext(Dispatchers.Default) {
                val alignedSourceType = when (draft.category) {
                    "pyq" -> QuestionSourceType.OFFICIAL_PYQ
                    "mock" -> QuestionSourceType.PREDICTED_MOCK
                    else -> QuestionSourceType.PRACTICE
                }
                val rawParsed = if (raw.startsWith("[") || raw.startsWith("{")) {
                    runCatching { BulkQuestionParser.parseJsonBatch(raw, markAsCustom = false) }
                        .getOrDefault(emptyList())
                } else {
                    val parserDefaultSubject = if (isAutoSubject) "MSN" else draft.subject
                    BulkQuestionParser.parseRawPdfText(
                        rawText = raw,
                        category = draft.category,
                        subject = parserDefaultSubject,
                        shiftYear = if (draft.category == "pyq") draft.shiftYear else "",
                        defaultDifficulty = draft.difficulty,
                        defaultSourceNote = "ClinEdge Pack: ${draft.title.ifBlank { "Update" }}",
                        markAsVerified = draft.category == "pyq",
                        defaultSourceType = alignedSourceType,
                        defaultTopicId = PlatformCatalogDefaults.defaultTopicIdFor(parserDefaultSubject)
                    )
                }
                if (isAutoSubject) {
                    QuestionSubjectClassifier.autoDistributeBySubject(rawParsed)
                } else {
                    rawParsed
                }
            }
            _adminState.update {
                it.copy(
                    contentDraft = it.contentDraft.copy(
                        previewQuestions = parsed,
                        isParsingOrExtracting = false
                    )
                )
            }
            if (parsed.isEmpty()) {
                showMessage("No MCQs detected. Ensure questions have options A–D (or 1–4) and an answer.")
            } else if (isAutoSubject) {
                val dist = QuestionSubjectClassifier.summarizeSubjectDistribution(parsed)
                showMessage("Scanned ${parsed.size} Qs & auto-distributed across ${dist.size} subjects!")
            } else {
                showMessage("Parsed ${parsed.size} questions ready to review & publish.")
            }
        }
    }

    fun adminRemovePreviewQuestion(index: Int) {
        _adminState.update { st ->
            val updated = st.contentDraft.previewQuestions.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            st.copy(contentDraft = st.contentDraft.copy(previewQuestions = updated))
        }
    }

    fun adminOverridePreviewQuestionSubject(index: Int, newSubjectCode: String) {
        val cleanCode = newSubjectCode.trim().ifBlank { "MSN" }
        _adminState.update { st ->
            val updated = st.contentDraft.previewQuestions.toMutableList()
            if (index in updated.indices) {
                val target = updated[index]
                updated[index] = target.copy(
                    subject = cleanCode,
                    topicId = PlatformCatalogDefaults.defaultTopicIdFor(cleanCode)
                )
            }
            st.copy(contentDraft = st.contentDraft.copy(previewQuestions = updated))
        }
    }

    fun adminPublishContentPack() {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        val draft = _adminState.value.contentDraft
        if (draft.previewQuestions.isEmpty()) {
            showMessage("Parse and preview at least 1 question before publishing.")
            return
        }
        val titleSubjectLabel = if (draft.subject.equals(QuestionSubjectClassifier.AUTO_SUBJECT_CODE, ignoreCase = true)) {
            "Mixed PYQ"
        } else {
            draft.subject
        }
        val packTitle = draft.title.trim().ifBlank { "ClinEdge $titleSubjectLabel Pack" }
        viewModelScope.launch {
            _adminState.update {
                it.copy(contentDraft = it.contentDraft.copy(isPublishing = true))
            }
            val res = repo.adminPublishQuestionPack(
                title = packTitle,
                examId = draft.examId,
                stage = draft.stage,
                subject = draft.subject,
                isPremiumOnly = draft.isPremiumOnly,
                questions = draft.previewQuestions,
                currentConfig = _subscriptionConfig.value
            )
            res.onSuccess { pack ->
                // Also persist immediately to the owner's local Room DB
                repository.saveOtaPackQuestions(examId = pack.examId, questions = draft.previewQuestions)
                _adminState.update {
                    it.copy(
                        contentDraft = AdminContentDraftState(examId = draft.examId, subject = draft.subject)
                    )
                }
                showMessage("Published '${pack.title}' (${pack.questionCount} Qs) live to all students!")
                refreshAdminDashboard()
            }.onFailure { err ->
                _adminState.update {
                    it.copy(contentDraft = it.contentDraft.copy(isPublishing = false))
                }
                showMessage("Publish failed: ${err.message}")
            }
        }
    }

    fun adminDeleteQuestionPack(packId: String, title: String) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminDeleteQuestionPack(packId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Deleted pack '$title'.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Delete failed: ${err.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // DAILY STUDY REMINDERS, NOTIFICATIONS INBOX & STUDENT DOUBT BOX ACTIONS
    // ------------------------------------------------------------------------

    fun openNotifications() {
        closeAllOverlayScreens()
        _isNotificationsOpen.value = true
        val newestAnnMs = _announcements.value.maxOfOrNull { it.createdAtMs } ?: System.currentTimeMillis()
        settings.markAnnouncementsSeenUpTo(maxOf(newestAnnMs, System.currentTimeMillis()))
    }

    fun closeNotifications() {
        _isNotificationsOpen.value = false
    }

    fun markAllNotificationsRead() {
        val newestAnnMs = _announcements.value.maxOfOrNull { it.createdAtMs } ?: System.currentTimeMillis()
        settings.markAnnouncementsSeenUpTo(maxOf(newestAnnMs, System.currentTimeMillis()))
        val unreadIds = _directNotifications.value.filter { !it.read }.map { it.notificationId }
        if (unreadIds.isEmpty()) {
            showMessage("All alerts marked as read.")
            return
        }
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.markAllDirectNotificationsRead(currentUserId, unreadIds).onSuccess {
                showMessage("All notifications marked as read.")
            }.onFailure { err ->
                showMessage("Could not update notifications: ${err.message}")
            }
        }
    }

    fun markDirectNotificationRead(notificationId: String) {
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.markDirectNotificationRead(currentUserId, notificationId)
        }
    }

    fun deleteDirectNotification(notificationId: String) {
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.deleteDirectNotification(currentUserId, notificationId).onFailure { err ->
                showMessage("Could not delete alert: ${err.message}")
            }
        }
    }

    fun dismissUrgentBanner(announcementId: String) {
        settings.dismissUrgentAnnouncementBanner(announcementId)
    }

    fun openDoubtBox(highlightDoubtId: String? = null) {
        closeAllOverlayScreens()
        _highlightedDoubtId.value = highlightDoubtId?.takeIf { it.isNotBlank() }
        _isDoubtBoxOpen.value = true
    }

    fun closeDoubtBox() {
        _isDoubtBoxOpen.value = false
        _highlightedDoubtId.value = null
    }

    fun openAskDoubtSheet(
        examId: String = activeExamId.value,
        subject: String = "MSN",
        questionId: String = "",
        questionPreview: String = ""
    ) {
        _doubtAskContext.value = DoubtAskContext(
            examId = examId.ifBlank { activeExamId.value },
            subject = subject.ifBlank { "MSN" },
            questionId = questionId,
            questionPreview = questionPreview.take(EngagementLogic.MAX_PREVIEW_LENGTH)
        )
    }

    fun dismissAskDoubtSheet() {
        _doubtAskContext.value = null
    }

    fun submitStudentDoubt(
        examId: String,
        subject: String,
        questionId: String,
        questionPreview: String,
        doubtText: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = engagementRepo ?: run {
            showMessage("Sign in with Google to submit doubts.")
            return
        }
        val validationError = EngagementLogic.validateDoubtText(doubtText)
        if (validationError != null) {
            showMessage(validationError)
            return
        }
        viewModelScope.launch {
            _isEngagementBusy.value = true
            val res = repo.submitDoubt(
                userId = currentUserId,
                studentName = settings.profileName.value.ifBlank { signedInAccount.displayName },
                studentEmail = signedInAccount.email,
                examId = examId,
                subject = subject,
                questionId = questionId,
                questionPreview = questionPreview,
                doubtText = doubtText
            )
            _isEngagementBusy.value = false
            res.onSuccess {
                _doubtAskContext.value = null
                showMessage("Doubt submitted! Faculty reply will notify you here.")
                onSuccess()
            }.onFailure { err ->
                showMessage("Could not submit doubt: ${err.message}")
            }
        }
    }

    fun markStudentDoubtRead(doubtId: String) {
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.markDoubtReplyRead(doubtId)
        }
    }

    fun closeStudentDoubt(doubtId: String) {
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.closeDoubt(doubtId).onSuccess {
                showMessage("Doubt marked as resolved.")
            }.onFailure { err ->
                showMessage("Could not close doubt: ${err.message}")
            }
        }
    }

    fun deleteStudentDoubt(doubtId: String) {
        val repo = engagementRepo ?: return
        viewModelScope.launch {
            repo.deleteDoubt(doubtId).onSuccess {
                showMessage("Doubt deleted.")
            }.onFailure { err ->
                showMessage("Could not delete doubt: ${err.message}")
            }
        }
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        settings.setDailyReminderEnabled(enabled)
        appContext?.let { ctx ->
            if (enabled) {
                ClinEdgeNotificationManager.ensureChannelsCreated(ctx)
                ClinEdgeNotificationManager.scheduleDailyReminder(
                    context = ctx,
                    hour = settings.dailyReminderHour.value,
                    minute = settings.dailyReminderMinute.value
                )
                showMessage(
                    "Daily study reminder scheduled for ${
                        EngagementLogic.formatReminderTime(
                            settings.dailyReminderHour.value,
                            settings.dailyReminderMinute.value
                        )
                    }."
                )
            } else {
                ClinEdgeNotificationManager.cancelDailyReminder(ctx)
                showMessage("Daily study reminder paused.")
            }
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        settings.setDailyReminderTime(hour, minute)
        if (settings.dailyReminderEnabled.value) {
            appContext?.let { ctx ->
                ClinEdgeNotificationManager.scheduleDailyReminder(ctx, hour, minute)
            }
        }
        showMessage("Daily reminder time set to ${EngagementLogic.formatReminderTime(hour, minute)}.")
    }

    fun setDailyReminderSmartNudge(enabled: Boolean) {
        settings.setDailyReminderSmartNudge(enabled)
        showMessage(
            if (enabled) "Smart streak & daily-goal nudges enabled."
            else "Standard daily reminder copy selected."
        )
    }

    fun sendTestDailyReminder() {
        val ctx = appContext ?: return
        val todayKey = StudyGoalLogic.todayDateKey()
        val yesterdayKey = StudyGoalLogic.yesterdayDateKey()
        val examName = PlatformCatalogDefaults.examById(activeExamId.value)?.shortName ?: "Nursing"
        val (title, body) = EngagementLogic.buildDailyReminderCopy(
            goalState = settings.studyGoalState.value,
            todayKey = todayKey,
            yesterdayKey = yesterdayKey,
            smartNudge = settings.dailyReminderSmartNudge.value,
            activeExamShortName = examName
        )
        val posted = ClinEdgeNotificationManager.postDailyReminderNotification(ctx, title, body)
        showMessage(
            if (posted) "Test study reminder sent to your notification tray!"
            else "Enable notification permission in Android settings to receive tray alerts."
        )
    }

    fun setBackgroundPushEnabled(enabled: Boolean) {
        settings.setBackgroundPushEnabled(enabled)
        appContext?.let { ctx ->
            if (enabled) {
                ClinEdgeNotificationManager.scheduleCloudAlertSync(
                    context = ctx,
                    triggerImmediateCheck = true
                )
                showMessage("Background push alerts & app-update checks enabled.")
            } else {
                ClinEdgeNotificationManager.cancelCloudAlertSync(ctx)
                showMessage("Background push sync paused.")
            }
        }
    }

    fun triggerBackgroundPushSyncNow() {
        val ctx = appContext ?: return
        if (!ClinEdgeNotificationManager.hasNotificationPermission(ctx)) {
            showMessage("Allow Android notifications first so background alerts can appear in your tray.")
            return
        }
        ClinEdgeNotificationManager.triggerImmediateCloudSync(
            context = ctx,
            notifyIfUpToDate = true
        )
        showMessage("Background sync started — checking alerts & app updates now...")
    }

    fun onAppEnteredBackground() {
        val ctx = appContext ?: return
        if (settings.backgroundPushEnabled.value) {
            ClinEdgeNotificationManager.scheduleCloudAlertSync(
                context = ctx,
                triggerImmediateCheck = false
            )
        }
    }

    fun handleNotificationDeepLink(target: String, doubtId: String) {
        when (target) {
            ClinEdgeNotificationManager.TARGET_NOTIFICATIONS -> openNotifications()
            ClinEdgeNotificationManager.TARGET_DOUBTS -> openDoubtBox(doubtId.takeIf { it.isNotBlank() })
            ClinEdgeNotificationManager.TARGET_APP_UPDATE -> {
                if (_appUpdateState.value.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                    openUpdateDialog()
                } else {
                    checkForAppUpdates(userInitiated = true)
                }
            }
            ClinEdgeNotificationManager.TARGET_DAILY_DRILL -> {
                closeNotifications()
                closeDoubtBox()
                closeClinicalTools()
                selectTab(MainTab.DASHBOARD)
            }
        }
    }

    // Admin Push Notifications & Doubt Desk Actions
    fun adminPublishAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        targetExamId: String,
        priority: AnnouncementPriority,
        actionUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.publishAnnouncement(
                title = title,
                body = body,
                category = category,
                targetExamId = targetExamId,
                priority = priority,
                actionUrl = actionUrl
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                val scopeLabel = if (targetExamId == "all") "all students"
                else PlatformCatalogDefaults.examById(targetExamId)?.shortName ?: targetExamId
                showMessage("Broadcast pushed live to $scopeLabel!")
                onSuccess()
            }.onFailure { err ->
                showMessage("Broadcast failed: ${err.message}")
            }
        }
    }

    fun adminDeleteAnnouncement(announcementId: String) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.deleteAnnouncement(announcementId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Announcement removed.")
            }.onFailure { err ->
                showMessage("Delete failed: ${err.message}")
            }
        }
    }

    fun adminSendDirectNotification(
        targetUserId: String,
        recipientLabel: String,
        title: String,
        body: String,
        actionUrl: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.sendDirectNotification(
                targetUserId = targetUserId,
                title = title,
                body = body,
                type = DirectNotificationType.CUSTOM_ADMIN,
                actionUrl = actionUrl
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Personal notification sent to $recipientLabel!")
                onSuccess()
            }.onFailure { err ->
                showMessage("Send failed: ${err.message}")
            }
        }
    }

    fun adminReplyToDoubt(
        doubt: CloudDoubt,
        replyText: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.replyToDoubt(doubt = doubt, replyText = replyText)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Faculty reply posted & ${doubt.studentName.ifBlank { "student" }} notified!")
                onSuccess()
            }.onFailure { err ->
                showMessage("Reply failed: ${err.message}")
            }
        }
    }

    fun adminCloseDoubt(doubtId: String) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.closeDoubt(doubtId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess { showMessage("Doubt closed.") }
                .onFailure { err -> showMessage("Close failed: ${err.message}") }
        }
    }

    fun adminDeleteDoubt(doubtId: String) {
        val repo = engagementRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.deleteDoubt(doubtId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess { showMessage("Doubt removed.") }
                .onFailure { err -> showMessage("Delete failed: ${err.message}") }
        }
    }

    // ------------------------------------------------------------------------
    // v2.3: AI CLINICAL TUTOR (PRO) & ADMIN AI DOUBT DRAFT HELPER
    // ------------------------------------------------------------------------

    fun openAiTutorForQuestion(item: QuestionItem, selectedOptionIndex: Int?) {
        val sub = subscriptionState.value
        if (!sub.hasAnyActivePaidOrTrial && !sub.isAdmin) {
            showMessage("AI Clinical Tutor is included with Free Trial & Pro plans.")
            openPlans()
            return
        }
        aiTutorSelectedOptionIndex = selectedOptionIndex
        aiTutorLastFollowUp = null
        aiTutorLastExplanation = null
        _aiTutorQuestion.value = item.question
        runAiTutorStream(followUpPrompt = null, optionByOptionMode = false)
    }

    fun openAiOptionBreakdown(item: QuestionItem, selectedOptionIndex: Int?) {
        val sub = subscriptionState.value
        if (!sub.hasAnyActivePaidOrTrial && !sub.isAdmin) {
            showMessage("AI Option-by-Option Breakdown is included with Free Trial & Pro plans.")
            openPlans()
            return
        }
        aiTutorSelectedOptionIndex = selectedOptionIndex
        aiTutorLastFollowUp = null
        aiTutorLastExplanation = null
        _aiTutorQuestion.value = item.question
        runAiTutorStream(followUpPrompt = null, optionByOptionMode = true)
    }

    fun askAiTutorFollowUp(prompt: String) {
        val clean = prompt.trim()
        if (clean.length < 3 || _aiTutorQuestion.value == null) return
        aiTutorLastFollowUp = clean
        runAiTutorStream(followUpPrompt = clean)
    }

    fun retryAiTutor() {
        if (_aiTutorQuestion.value == null) return
        runAiTutorStream(followUpPrompt = aiTutorLastFollowUp)
    }

    fun closeAiTutor() {
        aiTutorJob?.cancel()
        aiTutorJob = null
        _aiTutorQuestion.value = null
        _aiTutorState.value = AiTutorUiState.Idle
        aiTutorSelectedOptionIndex = null
        aiTutorLastFollowUp = null
        aiTutorLastExplanation = null
    }

    private fun runAiTutorStream(
        followUpPrompt: String?,
        optionByOptionMode: Boolean = false
    ) {
        val question = _aiTutorQuestion.value ?: return
        aiTutorJob?.cancel()
        val label = when {
            optionByOptionMode -> "Option-by-Option Distractor Breakdown"
            followUpPrompt.isNullOrBlank() -> "Clinical Concept & Option Elimination"
            else -> followUpPrompt.take(70)
        }
        _aiTutorState.value = AiTutorUiState.Loading(
            promptLabel = when {
                optionByOptionMode -> "Comparing all answer options..."
                followUpPrompt.isNullOrBlank() -> "Analyzing clinical priority & distractors..."
                else -> "Answering follow-up..."
            }
        )
        val examName = PlatformCatalogDefaults.examById(activeExamId.value)?.shortName ?: "Nursing Officer"
        aiTutorJob = viewModelScope.launch {
            try {
                var latestText = ""
                val stream = if (optionByOptionMode && followUpPrompt.isNullOrBlank()) {
                    aiTutorService.explainOptionByOptionEvents(
                        question = question,
                        selectedOptionIndex = aiTutorSelectedOptionIndex,
                        activeExamName = examName
                    )
                } else {
                    aiTutorService.explainMcqEvents(
                        question = question,
                        selectedOptionIndex = aiTutorSelectedOptionIndex,
                        activeExamName = examName,
                        followUpPrompt = followUpPrompt,
                        previousExplanation = aiTutorLastExplanation
                    )
                }
                stream.collect { event ->
                    when (event) {
                        is AiStreamEvent.Status -> {
                            if (latestText.isBlank()) {
                                _aiTutorState.value = AiTutorUiState.Loading(promptLabel = event.message)
                            }
                        }
                        is AiStreamEvent.Partial -> {
                            latestText = event.accumulatedText
                            _aiTutorState.value = AiTutorUiState.Streaming(text = latestText, promptLabel = label)
                        }
                        is AiStreamEvent.Completed -> {
                            latestText = event.finalText
                            if (followUpPrompt.isNullOrBlank()) {
                                aiTutorLastExplanation = latestText
                            }
                            _aiTutorState.value = AiTutorUiState.Success(text = latestText, promptLabel = label)
                        }
                    }
                }
                if (latestText.isBlank()) {
                    _aiTutorState.value = AiTutorUiState.Error("AI Tutor returned an empty response. Please retry.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Tutor stream failed", e)
                _aiTutorState.value = AiTutorUiState.Error(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "AI Clinical Tutor is temporarily unavailable. Check your internet connection and retry."
                )
            }
        }
    }

    fun adminGenerateAiDoubtDraft(
        doubt: CloudDoubt,
        onStreamText: (String) -> Unit
    ) {
        if (!isOwnerAdmin) return
        val dId = doubt.doubtId
        aiDraftJob?.cancel()
        _aiDraftingDoubtId.value = dId
        aiDraftJob = viewModelScope.launch {
            try {
                var latest = ""
                aiTutorService.draftAdminDoubtReplyEvents(doubt).collect { event ->
                    when (event) {
                        is AiStreamEvent.Status -> Unit
                        is AiStreamEvent.Partial -> {
                            latest = event.accumulatedText
                            onStreamText(latest)
                        }
                        is AiStreamEvent.Completed -> {
                            latest = event.finalText
                            onStreamText(latest)
                        }
                    }
                }
                if (latest.isNotBlank()) {
                    showMessage("AI draft ready — review & edit before posting.")
                } else {
                    showMessage("AI draft returned empty text. Please retry.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Admin AI doubt draft failed", e)
                showMessage("AI draft failed: ${e.message ?: "Check connection & retry"}")
            } finally {
                if (_aiDraftingDoubtId.value == dId) {
                    _aiDraftingDoubtId.value = null
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // v2.3: IN-APP UPI UTR PAYMENT SUBMISSION & 1-TAP ADMIN APPROVAL
    // ------------------------------------------------------------------------

    fun submitUpiPaymentRequest(
        tier: PlanTier,
        examIds: List<String>,
        tenureDays: Int,
        amountInr: Int,
        utrNumber: String,
        payerName: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = paymentReportRepo ?: return
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            _isPaymentSubmitBusy.value = true
            val res = repo.submitPaymentRequest(
                uid = currentUserId,
                studentName = profileName.value.ifBlank { signedInAccount.displayName },
                studentEmail = signedInAccount.email,
                planTier = tier,
                unlockedExamIds = examIds,
                tenureDays = tenureDays,
                amountInr = amountInr,
                utrNumber = utrNumber,
                payerName = payerName
            )
            _isPaymentSubmitBusy.value = false
            res.onSuccess {
                showMessage("UTR submitted! Admin will verify & activate your plan shortly.")
                onSuccess()
            }.onFailure { err ->
                showMessage(err.message ?: "Could not submit UPI UTR. Please check and retry.")
            }
        }
    }

    fun adminApprovePaymentRequest(request: CloudPaymentRequest) {
        val repo = paymentReportRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminApprovePaymentRequest(request)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Approved UTR ${request.utrNumber}! Plan activated & student notified.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Approval failed: ${err.message}")
            }
        }
    }

    fun adminRejectPaymentRequest(request: CloudPaymentRequest, reason: String) {
        val repo = paymentReportRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminRejectPaymentRequest(request, reason)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Payment request rejected & student notified.")
            }.onFailure { err ->
                showMessage("Rejection failed: ${err.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // v2.3: QUESTION ERROR FLAGGING & ADMIN REPORTS / LIVE OTA QUESTION EDITOR
    // ------------------------------------------------------------------------

    fun openFlagQuestionSheet(item: QuestionItem) {
        _flagQuestionTarget.value = item.question
    }

    fun closeFlagQuestionSheet() {
        _flagQuestionTarget.value = null
    }

    fun submitQuestionReport(
        question: Question,
        reason: QuestionReportReason,
        note: String
    ) {
        val repo = paymentReportRepo ?: return
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            _isFlagBusy.value = true
            val res = repo.submitQuestionReport(
                uid = currentUserId,
                studentName = profileName.value.ifBlank { signedInAccount.displayName },
                studentEmail = signedInAccount.email,
                examId = activeExamId.value,
                question = question,
                reason = reason,
                note = note
            )
            _isFlagBusy.value = false
            res.onSuccess {
                _flagQuestionTarget.value = null
                showMessage("Issue flagged to ClinEdge Faculty. Thank you for improving the bank!")
            }.onFailure { err ->
                showMessage("Could not submit flag: ${err.message}")
            }
        }
    }

    fun adminResolveQuestionReport(
        report: CloudQuestionReport,
        newStatus: QuestionReportStatus,
        resolutionNote: String,
        notifyStudent: Boolean
    ) {
        val repo = paymentReportRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminUpdateQuestionReportStatus(
                report = report,
                newStatus = newStatus,
                resolutionNote = resolutionNote,
                notifyStudent = notifyStudent
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                val msg = if (newStatus == QuestionReportStatus.RESOLVED) "Report marked resolved" else "Report dismissed"
                showMessage(if (notifyStudent) "$msg & student notified." else "$msg.")
            }.onFailure { err ->
                showMessage("Report update failed: ${err.message}")
            }
        }
    }

    fun adminOpenOtaQuestionEditor(questionId: String, reportId: String) {
        val repo = paymentReportRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val packIds = _adminState.value.questionPacks.map { it.packId.ifBlank { it.id } }
            val res = repo.adminLoadOtaQuestionForEdit(
                localQuestionId = questionId,
                knownPackIds = packIds,
                reportId = reportId
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess { draft ->
                _adminState.update { it.copy(otaEditDraft = draft) }
            }.onFailure { err ->
                showMessage(err.message ?: "Could not load OTA question for editing.")
            }
        }
    }

    fun adminSaveOtaQuestionEdit(draft: OtaQuestionEditDraft) {
        val repo = paymentReportRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val currentCfg = _subscriptionConfig.value
            val res = repo.adminSaveOtaQuestionEdit(draft = draft, currentConfig = currentCfg)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                _adminState.update { it.copy(otaEditDraft = null) }
                val bumped = (currentCfg.catalogVersion + 1L).coerceAtLeast(1L)
                triggerOtaPackSyncIfNeeded(bumped)
                showMessage("Live OTA question updated! Catalog bumped to v$bumped for all students.")
            }.onFailure { err ->
                showMessage("Could not save OTA edit: ${err.message}")
            }
        }
    }

    fun adminDismissOtaQuestionEditor() {
        _adminState.update { it.copy(otaEditDraft = null) }
    }

    // ------------------------------------------------------------------------
    // v2.5: WEEKLY ALL-INDIA GRAND TEST & ADMIN KILL-SWITCH LOGIC
    // ------------------------------------------------------------------------

    private fun syncPublicGrandTestObserver(enabled: Boolean) {
        if (!enabled || grandTestRepo == null || currentUserId.isBlank() || isSigningOut) {
            publicGrandTestObserverJob?.cancel()
            publicGrandTestObserverJob = null
            _publicGrandTests.value = emptyList()
            _grandTestLeaderboard.value = null
            _selectedGrandTestId.value = null
            if (!enabled) {
                _isGrandTestHubOpen.value = false
            }
            return
        }
        if (publicGrandTestObserverJob?.isActive == true) return
        publicGrandTestObserverJob = viewModelScope.launch {
            grandTestRepo.observePublicGrandTests().collect { tests ->
                if (!isSigningOut) {
                    _publicGrandTests.value = tests
                    val currentSel = _selectedGrandTestId.value
                    if (currentSel == null || tests.none { it.effectiveTestId == currentSel }) {
                        val preferred = tests.firstOrNull { it.parsedStatus == GrandTestStatus.LIVE }
                            ?: tests.firstOrNull()
                        _selectedGrandTestId.value = preferred?.effectiveTestId
                    }
                }
            }
        }
    }

    fun openGrandTestHub() {
        if (!_subscriptionConfig.value.grandTestEnabled) {
            showMessage("Weekly All-India Grand Test is currently dormant.")
            return
        }
        closeAllOverlayScreens()
        _isGrandTestHubOpen.value = true
        val targetId = _selectedGrandTestId.value
            ?: _publicGrandTests.value.firstOrNull { it.parsedStatus == GrandTestStatus.LIVE }?.effectiveTestId
            ?: _publicGrandTests.value.firstOrNull()?.effectiveTestId
        if (!targetId.isNullOrBlank()) {
            selectGrandTestForLeaderboard(targetId)
        }
    }

    fun closeGrandTestHub() {
        _isGrandTestHubOpen.value = false
    }

    fun selectGrandTestForLeaderboard(testId: String) {
        val repo = grandTestRepo ?: return
        val test = _publicGrandTests.value.firstOrNull { it.effectiveTestId == testId }
            ?: _adminState.value.grandTests.firstOrNull { it.effectiveTestId == testId }
            ?: return
        _selectedGrandTestId.value = testId
        viewModelScope.launch {
            _isGrandTestLoading.value = true
            val res = repo.fetchLeaderboardEntries(testId)
            _isGrandTestLoading.value = false
            res.onSuccess { entries ->
                _grandTestLeaderboard.value = GrandTestLeaderboardLogic.buildSummary(
                    test = test,
                    entries = entries,
                    currentUserId = currentUserId
                )
            }.onFailure { err ->
                showMessage("Could not load leaderboard: ${err.message}")
            }
        }
    }

    fun refreshSelectedGrandTestLeaderboard() {
        val id = _selectedGrandTestId.value ?: return
        selectGrandTestForLeaderboard(id)
    }

    fun startGrandTestAttempt(test: CloudGrandTest) {
        if (!_subscriptionConfig.value.grandTestEnabled && !isOwnerAdmin) {
            showMessage("Weekly All-India Grand Test is currently dormant.")
            return
        }
        if (test.parsedStatus != GrandTestStatus.LIVE) {
            showMessage("This Grand Test is no longer accepting submissions.")
            return
        }
        val alreadyAttempted = _grandTestLeaderboard.value
            ?.takeIf { it.test.effectiveTestId == test.effectiveTestId }
            ?.currentUserRank != null
        if (alreadyAttempted) {
            showMessage("Your official attempt is already recorded for this Grand Test.")
            return
        }

        val targetCount = test.questionCount.toInt().coerceIn(10, 200)
        val examLinkedIds = allPlatformLinks.value
            .filter { it.examId == test.examId }
            .map { it.questionId }
            .toSet()
        val allPool = dedupedPlatformQuestions.value.ifEmpty { allPlatformQuestions.value }
        val examPool = if (examLinkedIds.isNotEmpty()) {
            allPool.filter { it.id in examLinkedIds }
        } else {
            emptyList()
        }
        // Deterministic ordering seeded by testId so every student receives the same Grand Test paper!
        val seededRandom = java.util.Random(test.effectiveTestId.hashCode().toLong())
        val ordered = (examPool.shuffled(seededRandom) + allPool.filter { it.id !in examLinkedIds }.shuffled(seededRandom))
            .distinctBy { it.id }
            .take(targetCount)

        if (ordered.isEmpty()) {
            showMessage("Question bank is still loading. Please try again in a moment.")
            return
        }

        stopActiveTimer()
        _practiceSession.value = null
        val totalSecs = (test.durationMinutes.toInt().coerceIn(5, 240)) * 60
        _sessionTimerState.value = SessionTimerState(
            remainingSeconds = totalSecs,
            sectionRemainingSeconds = 0
        )
        _cbtSession.value = CbtSessionState(
            title = test.title,
            questions = ordered,
            currentIndex = 0,
            selectedAnswers = emptyMap(),
            paletteStatuses = mapOf(0 to CbtPaletteStatus.NOT_ANSWERED),
            hasCountdown = true,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isSubmitted = false,
            negativeMarks = test.negativeMarks,
            grandTestId = test.effectiveTestId,
            grandTestExamId = test.examId
        )
        startCbtTimer()
    }

    fun adminToggleGrandTestEnabled(enabled: Boolean) {
        val repo = subscriptionRepo ?: return
        if (!isOwnerAdmin) return
        val updated = _subscriptionConfig.value.copy(grandTestEnabled = enabled)
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminSaveSubscriptionConfig(updated)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                _subscriptionConfig.value = updated
                syncPublicGrandTestObserver(enabled)
                showMessage(
                    if (enabled) "Weekly All-India Grand Test is now LIVE for all students!"
                    else "Weekly All-India Grand Test set to DORMANT (hidden from students)."
                )
            }.onFailure { err ->
                showMessage("Could not update Grand Test toggle: ${err.message}")
            }
        }
    }

    fun adminCreateGrandTest(
        title: String,
        examId: String,
        weekLabel: String,
        questionCount: Int,
        durationMinutes: Int,
        status: GrandTestStatus,
        onSuccess: () -> Unit
    ) {
        val repo = grandTestRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminCreateGrandTest(
                title = title,
                examId = examId,
                weekLabel = weekLabel,
                questionCount = questionCount,
                durationMinutes = durationMinutes,
                negativeMarks = 0.33,
                status = status
            )
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess { created ->
                showMessage("Created \"${created.title}\" (${created.parsedStatus.label}).")
                onSuccess()
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Could not create Grand Test: ${err.message}")
            }
        }
    }

    fun adminUpdateGrandTestStatus(testId: String, status: GrandTestStatus) {
        val repo = grandTestRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminUpdateGrandTestStatus(testId, status)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Grand Test status changed to ${status.label}.")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Could not update status: ${err.message}")
            }
        }
    }

    fun adminDeleteGrandTest(testId: String, title: String) {
        val repo = grandTestRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update { it.copy(isMutating = true) }
            val res = repo.adminDeleteGrandTest(testId)
            _adminState.update { it.copy(isMutating = false) }
            res.onSuccess {
                showMessage("Deleted \"$title\".")
                refreshAdminDashboard()
            }.onFailure { err ->
                showMessage("Delete failed: ${err.message}")
            }
        }
    }

    fun adminInspectGrandTestLeaderboard(test: CloudGrandTest) {
        val repo = grandTestRepo ?: return
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _adminState.update {
                it.copy(isGrandTestLeaderboardLoading = true, selectedGrandTestLeaderboard = null)
            }
            val res = repo.fetchLeaderboardEntries(test.effectiveTestId)
            res.onSuccess { entries ->
                val summary = GrandTestLeaderboardLogic.buildSummary(
                    test = test,
                    entries = entries,
                    currentUserId = currentUserId
                )
                _adminState.update {
                    it.copy(
                        isGrandTestLeaderboardLoading = false,
                        selectedGrandTestLeaderboard = summary
                    )
                }
            }.onFailure { err ->
                _adminState.update { it.copy(isGrandTestLeaderboardLoading = false) }
                showMessage("Could not load leaderboard: ${err.message}")
            }
        }
    }

    fun adminDismissGrandTestLeaderboard() {
        _adminState.update {
            it.copy(selectedGrandTestLeaderboard = null, isGrandTestLeaderboardLoading = false)
        }
    }

    private fun stopActiveTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopActiveTimer()
        aiTutorJob?.cancel()
        aiDraftJob?.cancel()
        detachCloudSyncForSignOut()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
