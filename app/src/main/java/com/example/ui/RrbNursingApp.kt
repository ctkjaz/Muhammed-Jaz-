package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.NotificationDeepLink
import com.example.R
import com.example.data.backup.BackupStorageManager
import com.example.data.cloud.CloudSyncRepository
import com.example.data.grandtest.GrandTestRepository
import com.example.data.grandtest.GrandTestStatus
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileAvatarStorage
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.ExamReadinessEngine
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.PracticePathway
import com.example.data.model.SmartWeakTopicPlanner
import com.example.data.notifications.ClinEdgeNotificationManager
import com.example.data.notifications.CloudEngagementRepository
import com.example.data.repository.NursingPrepRepository
import com.example.data.subscription.DeviceLockManager
import com.example.data.subscription.PaymentAndReportRepository
import com.example.data.subscription.SubscriptionRepository
import com.example.data.update.AppUpdateVersionLogic
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.AiClinicalTutorSheet
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.ClinEdgeLogoMark
import com.example.ui.components.ExamPickerModalSheet
import com.example.ui.components.FlagQuestionModalSheet
import com.example.ui.components.ImporterModalSheet
import com.example.ui.components.PdfExportModalSheet
import com.example.ui.components.ProfileAvatarCircle
import com.example.ui.components.TimedPracticeConfigDialog
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.AskDoubtModalSheet
import com.example.ui.screens.AuthScreen
import com.example.data.model.ExamCountdownLogic
import com.example.data.model.SpacedRepetitionLogic
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CbtExamScreen
import com.example.ui.screens.CbtHubScreen
import com.example.ui.screens.ClinicalToolsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceLockSupportDialog
import com.example.ui.screens.DoubtBoxScreen
import com.example.ui.screens.GrandTestScreen
import com.example.ui.screens.NotificationsInboxScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SubjectChaptersScreen
import com.example.ui.screens.PaywallPromptDialog
import com.example.ui.screens.PlansAndAccessScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProfileDrawerContent
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SubjectsHubScreen
import com.example.ui.theme.RrbNursingTheme
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SubjectHubSection
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun ClinEdgeRootApp(
    pendingDeepLink: StateFlow<NotificationDeepLink?> = MutableStateFlow(null),
    onConsumeDeepLink: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val initialTheme = remember(context) { SettingsPreferences(context).themePreference.value }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        val settings = remember(context) { SettingsPreferences(context) }
        val forcedNotice = remember(user) { settings.getForcedSignOutNotice() }
        val skipAuto = remember(user) { settings.consumeSkipAutoSignInOnce() }
        val systemDark = isSystemInDarkTheme()
        val isDark = when (initialTheme) {
            ThemePreference.SYSTEM -> systemDark
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
        }
        RrbNursingTheme(darkTheme = isDark) {
            AuthScreen(
                onAuthSuccess = { /* AuthStateListener drives navigation reactively */ },
                forcedSignOutNotice = forcedNotice,
                skipAutoSignIn = skipAuto
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedClinEdgeApp(
                user = user,
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@Composable
private fun AuthenticatedClinEdgeApp(
    user: FirebaseUser,
    pendingDeepLink: StateFlow<NotificationDeepLink?>,
    onConsumeDeepLink: () -> Unit
) {
    val uid = user.uid
    val displayName = user.displayName.orEmpty()
    val email = user.email.orEmpty()
    val photoUrl = user.photoUrl?.toString()

    val vm: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val localDb = AppDatabase.getInstance(app)
                val repository = NursingPrepRepository(app, localDb.questionDao())
                val settings = SettingsPreferences(app)
                val cloudSync = CloudSyncRepository(firestore)
                val backupStorage = BackupStorageManager(app)
                val subscriptionRepo = SubscriptionRepository(firestore)
                val deviceLockManager = DeviceLockManager(app)
                val engagementRepo = CloudEngagementRepository(firestore)
                val paymentReportRepo = PaymentAndReportRepository(app, subscriptionRepo)
                val grandTestRepo = GrandTestRepository(firestore)
                MainViewModel(
                    repository = repository,
                    settings = settings,
                    cloudSync = cloudSync,
                    currentUserId = uid,
                    accountDisplayName = displayName,
                    accountEmail = email,
                    accountPhotoUrl = photoUrl,
                    backupStorage = backupStorage,
                    subscriptionRepo = subscriptionRepo,
                    deviceLockManager = deviceLockManager,
                    engagementRepo = engagementRepo,
                    appContext = app,
                    paymentReportRepo = paymentReportRepo,
                    grandTestRepo = grandTestRepo
                )
            }
        }
    )

    val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
    LaunchedEffect(deepLink) {
        deepLink?.let { link ->
            vm.handleNotificationDeepLink(link.target, link.doubtId)
            onConsumeDeepLink()
        }
    }

    RrbNursingApp(vm = vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RrbNursingApp(vm: MainViewModel) {
    val themePref by vm.themePreference.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePref) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }

    RrbNursingTheme(darkTheme = isDark) {
        val editorial = MaterialTheme.editorialColors
        val state by vm.mainState.collectAsStateWithLifecycle()
        val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
        val isSettingsOpen by vm.isSettingsOpen.collectAsStateWithLifecycle()
        val isProfileOpen by vm.isProfileOpen.collectAsStateWithLifecycle()
        val isPlansOpen by vm.isPlansOpen.collectAsStateWithLifecycle()
        val plansExamLockContext by vm.plansExamLockContext.collectAsStateWithLifecycle()
        val isAdminOpen by vm.isAdminOpen.collectAsStateWithLifecycle()
        val isExamPickerOpen by vm.isExamPickerOpen.collectAsStateWithLifecycle()
        val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
        val practicePathway by vm.practicePathway.collectAsStateWithLifecycle()
        val subjectExamFilter by vm.subjectPracticeExamFilter.collectAsStateWithLifecycle()
        val subjectSection by vm.subjectHubSection.collectAsStateWithLifecycle()
        val difficultyFilter by vm.difficultyFilter.collectAsStateWithLifecycle()
        val subjectChaptersNav by vm.subjectChaptersNav.collectAsStateWithLifecycle()
        val chaptersHighYieldOnly by vm.chaptersHighYieldOnly.collectAsStateWithLifecycle()
        val practiceSession by vm.practiceSession.collectAsStateWithLifecycle()
        val cbtSession by vm.cbtSession.collectAsStateWithLifecycle()
        val timedConfig by vm.timedConfig.collectAsStateWithLifecycle()
        val importerState by vm.importerState.collectAsStateWithLifecycle()
        val pdfExportConfig by vm.pdfExportConfig.collectAsStateWithLifecycle()
        val negMarking by vm.negativeMarking.collectAsStateWithLifecycle()
        val profileName by vm.profileName.collectAsStateWithLifecycle()
        val profileRole by vm.profileRole.collectAsStateWithLifecycle()
        val targetYear by vm.targetYear.collectAsStateWithLifecycle()
        val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()
        val phoneNumber by vm.phoneNumber.collectAsStateWithLifecycle()
        val profileAvatarPath by vm.profileAvatarPath.collectAsStateWithLifecycle()
        val useTimeLimit by vm.useTimeLimit.collectAsStateWithLifecycle()
        val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()
        val keepScreenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
        val onboardingCompleted by vm.onboardingCompleted.collectAsStateWithLifecycle()
        val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
        val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()
        val cloudSyncState by vm.cloudSyncState.collectAsStateWithLifecycle()
        val backupFolderSummary by vm.backupFolderSummary.collectAsStateWithLifecycle()
        val isBackupBusy by vm.isBackupBusy.collectAsStateWithLifecycle()
        val appUpdateState by vm.appUpdateState.collectAsStateWithLifecycle()
        val forcedSignOutEvent by vm.forcedSignOutEvent.collectAsStateWithLifecycle()
        val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
        val paywallPrompt by vm.paywallPrompt.collectAsStateWithLifecycle()
        val deviceLockConflict by vm.deviceLockConflict.collectAsStateWithLifecycle()
        val isTrialBusy by vm.isTrialBusy.collectAsStateWithLifecycle()
        val isRedeemBusy by vm.isRedeemBusy.collectAsStateWithLifecycle()
        val adminState by vm.adminState.collectAsStateWithLifecycle()
        val announcements by vm.announcements.collectAsStateWithLifecycle()
        val directNotifications by vm.directNotifications.collectAsStateWithLifecycle()
        val studentDoubts by vm.studentDoubts.collectAsStateWithLifecycle()
        val adminDoubts by vm.adminDoubts.collectAsStateWithLifecycle()
        val dailyReminder by vm.dailyReminderState.collectAsStateWithLifecycle()
        val backgroundPushEnabled by vm.backgroundPushEnabled.collectAsStateWithLifecycle()
        val lastBackgroundSyncMs by vm.lastBackgroundSyncMs.collectAsStateWithLifecycle()
        val unreadSummary by vm.unreadSummary.collectAsStateWithLifecycle()
        val activeUrgentBanner by vm.activeUrgentBanner.collectAsStateWithLifecycle()
        val isNotificationsOpen by vm.isNotificationsOpen.collectAsStateWithLifecycle()
        val isDoubtBoxOpen by vm.isDoubtBoxOpen.collectAsStateWithLifecycle()
        val highlightedDoubtId by vm.highlightedDoubtId.collectAsStateWithLifecycle()
        val doubtAskContext by vm.doubtAskContext.collectAsStateWithLifecycle()
        val isEngagementBusy by vm.isEngagementBusy.collectAsStateWithLifecycle()
        val lastSeenAnnouncementMs by vm.lastSeenAnnouncementMs.collectAsStateWithLifecycle()
        val studentPaymentRequests by vm.studentPaymentRequests.collectAsStateWithLifecycle()
        val isPaymentSubmitBusy by vm.isPaymentSubmitBusy.collectAsStateWithLifecycle()
        val aiTutorQuestion by vm.aiTutorQuestion.collectAsStateWithLifecycle()
        val aiTutorState by vm.aiTutorState.collectAsStateWithLifecycle()
        val aiDraftingDoubtId by vm.aiDraftingDoubtId.collectAsStateWithLifecycle()
        val flagQuestionTarget by vm.flagQuestionTarget.collectAsStateWithLifecycle()
        val isFlagBusy by vm.isFlagBusy.collectAsStateWithLifecycle()
        val isClinicalToolsOpen by vm.isClinicalToolsOpen.collectAsStateWithLifecycle()
        val isGrandTestHubOpen by vm.isGrandTestHubOpen.collectAsStateWithLifecycle()
        val publicGrandTests by vm.publicGrandTests.collectAsStateWithLifecycle()
        val selectedGrandTestId by vm.selectedGrandTestId.collectAsStateWithLifecycle()
        val grandTestLeaderboard by vm.grandTestLeaderboard.collectAsStateWithLifecycle()
        val isGrandTestLoading by vm.isGrandTestLoading.collectAsStateWithLifecycle()
        val grandTestEnabled = subscriptionState.config.grandTestEnabled
        val liveGrandTest = remember(publicGrandTests) {
            publicGrandTests.firstOrNull { it.parsedStatus == GrandTestStatus.LIVE }
        }
        val targetExamDateIso by vm.targetExamDateIso.collectAsStateWithLifecycle()
        val srsCards by vm.srsCards.collectAsStateWithLifecycle()
        val srsNotebookSummary = remember(state.questions, srsCards) {
            SpacedRepetitionLogic.buildSummary(
                questions = state.questions,
                storedCards = srsCards
            )
        }
        val examReadiness = remember(
            state.overall,
            state.subjectStats,
            state.stageSubjectStats,
            state.activeExamSupportsStages,
            state.mockHistory,
            srsCards,
            studyGoal
        ) {
            val activeSubjects = if (state.activeExamSupportsStages) {
                state.stageSubjectStats.ifEmpty { state.subjectStats }
            } else {
                state.subjectStats
            }
            ExamReadinessEngine.compute(
                totalQuestions = state.overall.totalQuestions,
                attemptedUnique = state.overall.attemptedUnique,
                totalAttempts = state.overall.totalAttempts,
                accuracyPercent = state.overall.accuracyPercent,
                subjectStats = activeSubjects,
                mockHistory = state.mockHistory,
                srsCards = srsCards,
                currentStreakDays = studyGoal.effectiveCurrentStreak(),
                dailyGoalMetToday = studyGoal.isGoalMetToday()
            )
        }
        val smartStudyPlan = remember(state.stageQuestions, state.questions, srsCards, studyGoal.dailyGoalQuestions) {
            SmartWeakTopicPlanner.buildPlan(
                questions = state.stageQuestions.ifEmpty { state.questions },
                srsCards = srsCards,
                dailyGoalQuestions = studyGoal.dailyGoalQuestions
            )
        }
        val examRoadmap = remember(targetExamDateIso, state.questions.size, state.overall.attemptedUnique, studyGoal.dailyGoalQuestions) {
            ExamCountdownLogic.computeRoadmap(
                targetDateIso = targetExamDateIso,
                totalBankCount = state.questions.size,
                attemptedCount = state.overall.attemptedUnique,
                currentDailyGoal = studyGoal.dailyGoalQuestions
            )
        }
        val signedInAccount = vm.signedInAccount

        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val credentialManager = remember(context) { CredentialManager.create(context) }

        val startupNotifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                ClinEdgeNotificationManager.scheduleCloudAlertSync(
                    context = context,
                    triggerImmediateCheck = true
                )
            }
        }

        LaunchedEffect(signedInAccount.uid) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val prefs = SettingsPreferences(context)
                if (!ClinEdgeNotificationManager.hasNotificationPermission(context) &&
                    !prefs.hasPromptedNotificationPermission()
                ) {
                    prefs.markNotificationPermissionPrompted()
                    startupNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        val performSignOut: () -> Unit = {
            signOut(
                credentialManager = credentialManager,
                scope = scope,
                onBeforeSignOut = { vm.detachCloudSyncForSignOut(isForcedByOtherDevice = false) }
            )
        }

        LaunchedEffect(forcedSignOutEvent) {
            if (forcedSignOutEvent != null) {
                signOut(
                    credentialManager = credentialManager,
                    scope = scope,
                    onBeforeSignOut = { vm.detachCloudSyncForSignOut(isForcedByOtherDevice = true) }
                )
            }
        }

        DisposableEffect(lifecycleOwner, vm) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> vm.verifyActiveSessionOnResume()
                    Lifecycle.Event.ON_STOP -> vm.onAppEnteredBackground()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                vm.clearSnackbar()
            }
        }

        // Zero-permission SAF Backup Directory Picker
        val backupFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { treeUri ->
            if (treeUri != null) {
                vm.onBackupFolderSelected(treeUri)
            }
        }

        // Zero-permission Android Photo Picker for Optional Profile Picture
        val profilePhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { photoUri ->
            if (photoUri != null) {
                scope.launch {
                    ProfileAvatarStorage.savePickedAvatar(context, photoUri)
                        .onSuccess { savedPath -> vm.onProfileAvatarSaved(savedPath) }
                        .onFailure { err -> vm.showMessage("Could not save photo: ${err.message}") }
                }
            }
        }

        val pdfExportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                vm.exportWorksheetPdf {
                    context.contentResolver.openOutputStream(uri)
                }
            }
        }

        val restoreLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!text.isNullOrBlank()) {
                        vm.restoreBackupFromText(text)
                    } else {
                        vm.showMessage("Selected file was empty.")
                    }
                }.onFailure {
                    vm.showMessage("Could not read file: ${it.message}")
                }
            }
        }

        // Zero-permission SAF Document Picker for Owner Admin OTA Question Packs (PDF / TXT / JSON)
        val adminPackFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val fileName = resolveDisplayFileName(context, uri)
                vm.adminExtractDocumentForPack(context, uri, fileName)
            }
        }

        // Zero-permission SAF Document Picker for Local ImporterModalSheet (PDF / TXT / JSON)
        val importerPdfFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                val fileName = resolveDisplayFileName(context, uri)
                vm.importPdfFileForImporter(context, uri, fileName)
            }
        }

        val inActiveSession = practiceSession != null || cbtSession != null
        val inSubScreen = isSettingsOpen || isProfileOpen || isPlansOpen || isAdminOpen ||
            isNotificationsOpen || isDoubtBoxOpen || isClinicalToolsOpen || isGrandTestHubOpen || subjectChaptersNav != null
        val shouldKeepScreenOn = keepScreenOn && inActiveSession

        DisposableEffect(context, shouldKeepScreenOn) {
            val window = context.findActivity()?.window
            if (shouldKeepScreenOn) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // Dynamic Anti-Piracy Screen Protection (FLAG_SECURE):
        // Active ONLY for non-admin students during Practice, CBT, PYQ browsing, or PDF export.
        val shouldSecureScreen = !subscriptionState.isAdmin && (
            practiceSession != null ||
                cbtSession != null ||
                pdfExportConfig.isVisible ||
                (!inSubScreen && selectedTab == MainTab.SUBJECTS &&
                    practicePathway == PracticePathway.BY_EXAM &&
                    subjectSection == SubjectHubSection.PYQ)
        )
        DisposableEffect(context, shouldSecureScreen) {
            val window = context.findActivity()?.window
            if (shouldSecureScreen) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        if (!onboardingCompleted) {
            OnboardingScreen(
                availableExams = state.availableExams,
                initialExamId = activeExamId,
                initialName = profileName.ifBlank { signedInAccount.displayName },
                initialRole = profileRole,
                initialTargetYear = targetYear,
                initialFocusSubject = focusSubject,
                initialPhoneNumber = phoneNumber,
                initialDailyGoal = studyGoal.dailyGoalQuestions,
                secondsPerQuestion = secondsPerQuestion,
                onComplete = vm::completeOnboarding,
                onSkip = vm::skipOnboarding
            )
            return@RrbNursingTheme
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !inActiveSession && !inSubScreen,
            drawerContent = {
                ProfileDrawerContent(
                    profileName = profileName,
                    profileRole = profileRole,
                    targetYear = targetYear,
                    focusSubject = focusSubject,
                    avatarPath = profileAvatarPath,
                    account = signedInAccount,
                    cloudSyncState = cloudSyncState,
                    studyGoal = studyGoal,
                    state = state,
                    subscription = subscriptionState,
                    unreadNotificationsCount = unreadSummary.totalBellUnread,
                    unreadDoubtReplies = unreadSummary.unreadDoubtReplies,
                    onOpenFullProfile = {
                        scope.launch {
                            drawerState.close()
                            vm.openProfile()
                        }
                    },
                    onOpenPlans = {
                        scope.launch {
                            drawerState.close()
                            vm.openPlans()
                        }
                    },
                    onOpenNotifications = {
                        scope.launch {
                            drawerState.close()
                            vm.openNotifications()
                        }
                    },
                    onOpenDoubtBox = {
                        scope.launch {
                            drawerState.close()
                            vm.openDoubtBox()
                        }
                    },
                    onOpenClinicalTools = {
                        scope.launch {
                            drawerState.close()
                            vm.openClinicalTools()
                        }
                    },
                    onOpenExamPicker = {
                        scope.launch {
                            drawerState.close()
                            vm.openExamPicker()
                        }
                    },
                    onOpenSubjectSection = { section ->
                        scope.launch {
                            drawerState.close()
                            vm.openSubjectHub(section)
                        }
                    },
                    onOpenSettings = {
                        scope.launch {
                            drawerState.close()
                            vm.openSettings()
                        }
                    },
                    onOpenImporter = {
                        scope.launch {
                            drawerState.close()
                            vm.openImporter()
                        }
                    },
                    onSyncNow = {
                        scope.launch {
                            drawerState.close()
                            vm.syncNow()
                        }
                    },
                    onSignOut = {
                        scope.launch {
                            drawerState.close()
                            performSignOut()
                        }
                    }
                )
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 720.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (!inActiveSession && !inSubScreen) {
                            Column {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .testTag("top_profile_avatar_btn")
                                        ) {
                                            ProfileAvatarCircle(
                                                name = profileName,
                                                size = 34.dp,
                                                avatarPath = profileAvatarPath
                                            )
                                        }
                                    },
                                    title = {
                                        val activeExamShort = state.activeExam?.shortName ?: "RRB Nursing"
                                        val examPillText = if (state.activeExamSupportsStages) {
                                            "$activeExamShort · ${state.activeExamStage.shortLabel}"
                                        } else {
                                            activeExamShort
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(onClick = vm::openExamPicker)
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                                .testTag("top_exam_picker_btn")
                                        ) {
                                            ClinEdgeLogoMark(size = 26.dp)
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = editorial.elevatedSurface,
                                                border = BorderStroke(1.dp, editorial.borderSubtle)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = examPillText,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                                        contentDescription = "Switch active exam",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = vm::openNotifications,
                                            modifier = Modifier.testTag("top_notifications_btn")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    val bellCount = unreadSummary.totalBellUnread
                                                    if (bellCount > 0) {
                                                        Badge(
                                                            containerColor = editorial.acidLime,
                                                            contentColor = editorial.onAcidLime
                                                        ) {
                                                            Text(
                                                                if (bellCount > 99) "99+" else "$bellCount",
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Notifications,
                                                    contentDescription = "Notifications & Exam Alerts"
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = vm::openSettings,
                                            modifier = Modifier.testTag("top_settings_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = "Settings"
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider(color = editorial.borderSubtle)
                            }
                        }
                    },
                    bottomBar = {
                        if (!inActiveSession && !isAdminOpen && !useRail) {
                            Column {
                                HorizontalDivider(color = editorial.borderSubtle)
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    tonalElevation = 0.dp
                                ) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = !inSubScreen && tab == selectedTab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { vm.selectTab(tab) },
                                            icon = { NavTabIcon(tab, selected) },
                                            label = {
                                                Text(
                                                    text = tab.label,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = editorial.acidLime,
                                                selectedIconColor = editorial.onAcidLime,
                                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!inActiveSession && !isAdminOpen && useRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.background
                            ) {
                                Spacer(modifier = Modifier.weight(0.1f))
                                MainTab.entries.forEach { tab ->
                                    val selected = !inSubScreen && tab == selectedTab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { vm.selectTab(tab) },
                                        icon = { NavTabIcon(tab, selected) },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            indicatorColor = editorial.acidLime,
                                            selectedIconColor = editorial.onAcidLime,
                                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("rail_${tab.name}")
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when {
                                cbtSession != null -> {
                                    CbtExamScreen(
                                        session = cbtSession!!,
                                        onSelectOption = vm::cbtSelectOption,
                                        onClearResponse = vm::cbtClearResponse,
                                        onMoveTo = vm::cbtMoveTo,
                                        onSaveAndNext = vm::cbtSaveAndNext,
                                        onMarkForReviewAndNext = vm::cbtMarkForReviewAndNext,
                                        onRequestSubmitConfirm = vm::requestCbtSubmitConfirm,
                                        onDismissSubmitConfirm = vm::dismissCbtSubmitConfirm,
                                        onConfirmSubmit = vm::submitCbtExam,
                                        onChangeReviewFilter = vm::setCbtReviewFilter,
                                        onExit = vm::exitPracticeOrCbt,
                                        onRequestNextSection = vm::requestCbtNextSection,
                                        onDismissNextSection = vm::dismissCbtNextSection,
                                        onConfirmNextSection = vm::confirmCbtNextSection,
                                        onDismissSectionNotice = vm::dismissCbtSectionNotice
                                    )
                                }

                                practiceSession != null -> {
                                    val activeSession = practiceSession!!
                                    val currentQ = activeSession.questions.getOrNull(activeSession.currentIndex)
                                    val poolForBookmark = state.crossExamQuestions.ifEmpty { state.questions }
                                    val isCurrentBookmarked = remember(currentQ?.id, poolForBookmark) {
                                        if (currentQ == null) false
                                        else poolForBookmark.firstOrNull { it.id == currentQ.id }?.isBookmarked ?: currentQ.isBookmarked
                                    }
                                    PracticeScreen(
                                        session = activeSession,
                                        isCurrentBookmarked = isCurrentBookmarked,
                                        negativeMarking = negMarking,
                                        onToggleNegativeMarking = vm::toggleNegativeMarking,
                                        onSelectOption = vm::answerPracticeQuestion,
                                        onMoveIndex = vm::movePracticeIndex,
                                        onToggleBookmark = vm::toggleBookmark,
                                        onRequestSubmit = vm::requestPracticeSubmitPrompt,
                                        onDismissSubmitPrompt = vm::dismissPracticeSubmitPrompt,
                                        onConfirmSubmit = vm::submitTimedPractice,
                                        onChangeReviewFilter = vm::setPracticeReviewFilter,
                                        onAskDoubt = { item ->
                                            vm.openAskDoubtSheet(
                                                examId = activeExamId,
                                                subject = item.question.subject,
                                                questionId = item.id,
                                                questionPreview = item.question.questionText
                                            )
                                        },
                                        canUseAiTutor = subscriptionState.hasAnyActivePaidOrTrial || subscriptionState.isAdmin,
                                        onExplainWithAi = vm::openAiTutorForQuestion,
                                        onFlagQuestion = vm::openFlagQuestionSheet,
                                        onExit = vm::exitPracticeOrCbt
                                    )
                                }

                                isAdminOpen && subscriptionState.isAdmin -> {
                                    AdminConsoleScreen(
                                        state = adminState,
                                        config = subscriptionState.config,
                                        onSelectTab = vm::selectAdminTab,
                                        onRefresh = vm::refreshAdminDashboard,
                                        onUpdateUserPlan = vm::adminGrantOrUpdateUserPlan,
                                        onGenerateCode = vm::adminGenerateAccessCode,
                                        onRevokeCode = vm::adminRevokeAccessCode,
                                        onResetDeviceLock = vm::adminResetDeviceLock,
                                        onSaveConfig = vm::adminSavePricingConfig,
                                        onPickContentFile = {
                                            runCatching {
                                                adminPackFileLauncher.launch(arrayOf("*/*"))
                                            }.onFailure { err ->
                                                vm.showMessage("Could not open file picker: ${err.message}")
                                            }
                                        },
                                        onUpdateContentDraft = vm::adminUpdateContentDraft,
                                        onPreviewContentPack = vm::adminPreviewContentPack,
                                        onRemovePreviewQuestion = vm::adminRemovePreviewQuestion,
                                        onOverridePreviewSubject = vm::adminOverridePreviewQuestionSubject,
                                        onPublishContentPack = vm::adminPublishContentPack,
                                        onDeleteQuestionPack = vm::adminDeleteQuestionPack,
                                        announcements = announcements,
                                        adminDoubts = adminDoubts,
                                        onPublishAnnouncement = vm::adminPublishAnnouncement,
                                        onDeleteAnnouncement = vm::adminDeleteAnnouncement,
                                        onSendDirectNotification = vm::adminSendDirectNotification,
                                        onReplyToDoubt = vm::adminReplyToDoubt,
                                        onCloseDoubt = vm::adminCloseDoubt,
                                        onDeleteDoubt = vm::adminDeleteDoubt,
                                        aiDraftingDoubtId = aiDraftingDoubtId,
                                        onAiDraftDoubtReply = vm::adminGenerateAiDoubtDraft,
                                        onApprovePaymentRequest = vm::adminApprovePaymentRequest,
                                        onRejectPaymentRequest = vm::adminRejectPaymentRequest,
                                        onResolveQuestionReport = vm::adminResolveQuestionReport,
                                        onOpenOtaQuestionEditor = vm::adminOpenOtaQuestionEditor,
                                        onSaveOtaQuestionEdit = vm::adminSaveOtaQuestionEdit,
                                        onDismissOtaQuestionEditor = vm::adminDismissOtaQuestionEditor,
                                        onToggleGrandTestEnabled = vm::adminToggleGrandTestEnabled,
                                        onCreateGrandTest = vm::adminCreateGrandTest,
                                        onUpdateGrandTestStatus = vm::adminUpdateGrandTestStatus,
                                        onDeleteGrandTest = vm::adminDeleteGrandTest,
                                        onLoadGrandTestLeaderboard = vm::adminInspectGrandTestLeaderboard,
                                        onDismissGrandTestLeaderboard = vm::adminDismissGrandTestLeaderboard,
                                        onShowMessage = vm::showMessage,
                                        onBack = vm::closeAdminConsole
                                    )
                                }

                                isNotificationsOpen -> {
                                    NotificationsInboxScreen(
                                        announcements = announcements,
                                        directNotifications = directNotifications,
                                        activeExamId = activeExamId,
                                        activeExamShortName = state.activeExam?.shortName ?: "RRB Nursing",
                                        lastSeenAnnouncementMs = lastSeenAnnouncementMs,
                                        onMarkAllRead = vm::markAllNotificationsRead,
                                        onMarkDirectRead = vm::markDirectNotificationRead,
                                        onDeleteDirectNotification = vm::deleteDirectNotification,
                                        onOpenDoubtFromReply = { doubtId -> vm.openDoubtBox(doubtId) },
                                        onShowMessage = vm::showMessage,
                                        onBack = vm::closeNotifications
                                    )
                                }

                                isDoubtBoxOpen -> {
                                    DoubtBoxScreen(
                                        doubts = studentDoubts,
                                        highlightedDoubtId = highlightedDoubtId,
                                        onOpenAskDoubt = {
                                            vm.openAskDoubtSheet(
                                                examId = activeExamId,
                                                subject = focusSubject.ifBlank { "MSN" }
                                            )
                                        },
                                        onMarkDoubtRead = vm::markStudentDoubtRead,
                                        onCloseDoubt = vm::closeStudentDoubt,
                                        onDeleteDoubt = vm::deleteStudentDoubt,
                                        onBack = vm::closeDoubtBox
                                    )
                                }

                                isClinicalToolsOpen -> {
                                    ClinicalToolsScreen(
                                        onBack = vm::closeClinicalTools
                                    )
                                }

                                isGrandTestHubOpen && grandTestEnabled -> {
                                    GrandTestScreen(
                                        tests = publicGrandTests,
                                        selectedTestId = selectedGrandTestId,
                                        leaderboardSummary = grandTestLeaderboard,
                                        isLoading = isGrandTestLoading,
                                        onSelectTest = vm::selectGrandTestForLeaderboard,
                                        onRefreshLeaderboard = vm::refreshSelectedGrandTestLeaderboard,
                                        onStartGrandTest = vm::startGrandTestAttempt,
                                        onBack = vm::closeGrandTestHub
                                    )
                                }

                                isPlansOpen -> {
                                    PlansAndAccessScreen(
                                        subscription = subscriptionState,
                                        account = signedInAccount,
                                        deviceName = vm.currentDeviceName,
                                        isTrialBusy = isTrialBusy,
                                        isRedeemBusy = isRedeemBusy,
                                        onStartTrial = vm::claimThreeDayFreeTrial,
                                        onRedeemCode = vm::redeemAccessCode,
                                        onShowMessage = vm::showMessage,
                                        onBack = vm::closePlans,
                                        examLockContext = plansExamLockContext,
                                        paymentRequests = studentPaymentRequests,
                                        isPaymentBusy = isPaymentSubmitBusy,
                                        onSubmitUpiPayment = vm::submitUpiPaymentRequest
                                    )
                                }

                                isSettingsOpen -> {
                                    SettingsScreen(
                                        profileName = profileName,
                                        themePreference = themePref,
                                        useTimeLimit = useTimeLimit,
                                        secondsPerQuestion = secondsPerQuestion,
                                        negativeMarking = negMarking,
                                        keepScreenOn = keepScreenOn,
                                        dailyGoalQuestions = studyGoal.dailyGoalQuestions,
                                        dailyReminder = dailyReminder,
                                        backgroundPushEnabled = backgroundPushEnabled,
                                        lastBackgroundSyncMs = lastBackgroundSyncMs,
                                        backupFolderSummary = backupFolderSummary,
                                        isBackupBusy = isBackupBusy,
                                        appUpdateState = appUpdateState,
                                        subscription = subscriptionState,
                                        activeExamNegativeMarks = state.activeExam?.negativeMarks ?: 0.33,
                                        activeExamDefaultMinutes = state.activeExam?.defaultMockMinutes ?: 90,
                                        onProfileNameChange = vm::setProfileName,
                                        onThemePreferenceChange = vm::setThemePreference,
                                        onUseTimeLimitChange = vm::setUseTimeLimit,
                                        onSecondsPerQuestionChange = vm::setSecondsPerQuestion,
                                        onNegativeMarkingChange = vm::setNegativeMarking,
                                        onKeepScreenOnChange = vm::setKeepScreenOn,
                                        onDailyGoalChange = vm::setDailyGoalQuestions,
                                        onDailyReminderEnabledChange = vm::setDailyReminderEnabled,
                                        onDailyReminderTimeChange = vm::setDailyReminderTime,
                                        onDailyReminderSmartNudgeChange = vm::setDailyReminderSmartNudge,
                                        onSendTestReminder = vm::sendTestDailyReminder,
                                        onBackgroundPushEnabledChange = vm::setBackgroundPushEnabled,
                                        onTriggerBackgroundSyncNow = vm::triggerBackgroundPushSyncNow,
                                        onReopenOnboarding = vm::reopenOnboarding,
                                        onOpenPlans = vm::openPlans,
                                        onOpenAdminConsole = vm::openAdminConsole,
                                        onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                                        onResetBackupFolder = vm::resetBackupFolderToDefault,
                                        onCreateBackup = vm::createBackupInActiveFolder,
                                        onRestoreLatestBackup = vm::restoreLatestFromActiveFolder,
                                        onPickRestoreFile = {
                                            restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        onOpenImporter = vm::openImporter,
                                        onOpenPdfExport = { vm.openPdfExport() },
                                        onCheckForUpdates = { vm.checkForAppUpdates(userInitiated = true) },
                                        onOpenUpdateDialog = vm::openUpdateDialog,
                                        onOpenReleasesPage = {
                                            val url = appUpdateState.releasePageUrl.ifBlank {
                                                AppUpdateVersionLogic.GITHUB_RELEASES_WEB_URL
                                            }
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }.onFailure {
                                                vm.showMessage("Could not open browser for Releases page.")
                                            }
                                        },
                                        onResetStatistics = vm::clearAllStatistics,
                                        onBack = vm::closeSettings
                                    )
                                }

                                isProfileOpen -> {
                                    ProfileScreen(
                                        profileName = profileName,
                                        profileRole = profileRole,
                                        targetYear = targetYear,
                                        focusSubject = focusSubject,
                                        phoneNumber = phoneNumber,
                                        avatarPath = profileAvatarPath,
                                        currentDeviceName = vm.currentDeviceName,
                                        account = signedInAccount,
                                        cloudSyncState = cloudSyncState,
                                        studyGoal = studyGoal,
                                        state = state,
                                        subscription = subscriptionState,
                                        onProfileNameChange = vm::setProfileName,
                                        onProfileRoleChange = vm::setProfileRole,
                                        onTargetYearChange = vm::setTargetYear,
                                        onFocusSubjectChange = vm::setFocusSubject,
                                        onPhoneNumberChange = vm::setPhoneNumber,
                                        onPickProfilePhoto = {
                                            profilePhotoLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onRemoveProfilePhoto = {
                                            scope.launch {
                                                ProfileAvatarStorage.removeAvatar(context, profileAvatarPath)
                                                vm.onProfileAvatarRemoved()
                                            }
                                        },
                                        onDailyGoalChange = vm::setDailyGoalQuestions,
                                        onStartDailyGoalDrill = {
                                            vm.closeProfile()
                                            vm.startDailyGoalDrill()
                                        },
                                        onOpenPlans = vm::openPlans,
                                        onOpenNotifications = vm::openNotifications,
                                        onOpenDoubtBox = { vm.openDoubtBox() },
                                        onOpenExamPicker = vm::openExamPicker,
                                        onPracticeSubject = { list, title ->
                                            vm.closeProfile()
                                            vm.startPractice(list, title)
                                        },
                                        onOpenSettings = vm::openSettings,
                                        onSyncNow = vm::syncNow,
                                        onSignOut = performSignOut,
                                        onBack = vm::closeProfile
                                    )
                                }

                                subjectChaptersNav != null -> {
                                    SubjectChaptersScreen(
                                        navState = subjectChaptersNav!!,
                                        state = state,
                                        difficultyFilter = difficultyFilter,
                                        highYieldOnly = chaptersHighYieldOnly,
                                        secondsPerQuestion = secondsPerQuestion,
                                        onSelectDifficulty = vm::selectDifficultyFilter,
                                        onToggleHighYieldOnly = vm::toggleChaptersHighYieldOnly,
                                        onStartPractice = { list, title -> vm.startPractice(list, title) },
                                        onStartTimedPractice = { list, title, mins ->
                                            vm.startPractice(
                                                items = list,
                                                title = title,
                                                timedMode = true,
                                                durationMinutes = mins,
                                                hasCountdown = true
                                            )
                                        },
                                        onBack = vm::closeSubjectChapters
                                    )
                                }

                                else -> {
                                    when (selectedTab) {
                                        MainTab.DASHBOARD -> DashboardScreen(
                                            state = state,
                                            profileName = profileName.ifBlank { signedInAccount.displayName },
                                            studyGoal = studyGoal,
                                            difficultyFilter = difficultyFilter,
                                            onSelectDifficulty = vm::selectDifficultyFilter,
                                            onSelectExamStage = vm::selectExamStage,
                                            onOpenExamPicker = vm::openExamPicker,
                                            onLaunchFullMock = {
                                                val isNorcetMains = PlatformCatalogDefaults.isNorcetExam(activeExamId) &&
                                                    state.activeExamStage == ExamStage.MAINS
                                                if (isNorcetMains) {
                                                    vm.startNorcetMainsMock()
                                                } else {
                                                    val fullQ = state.activeExam?.defaultMockQuestions ?: 100
                                                    val fullMins = state.activeExam?.defaultMockMinutes ?: 90
                                                    val mockTitle = if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) {
                                                        "NORCET Stage I Prelims (${fullQ}Q)"
                                                    } else {
                                                        "Full Official ${fullQ}Q CBT Mock"
                                                    }
                                                    vm.startCbtStandardMock(
                                                        totalQuestions = fullQ,
                                                        durationMinutes = fullMins,
                                                        title = mockTitle,
                                                        isOfficialFullMock = true
                                                    )
                                                }
                                            },
                                            onOpenTimedPractice = { vm.openTimedPracticeConfig() },
                                            onStartDailyGoalDrill = vm::startDailyGoalDrill,
                                            onAdjustDailyGoal = vm::openSettings,
                                            onOpenSubjectSection = vm::openSubjectHub,
                                            onOpenSubjectPracticeMode = vm::openSubjectPracticeMode,
                                            onSelectTab = vm::selectTab,
                                            onStartDrill = { list, title -> vm.startPractice(list, title) },
                                            subscription = subscriptionState,
                                            onOpenPlans = vm::openPlans,
                                            urgentBanner = activeUrgentBanner,
                                            unreadDoubtReplies = unreadSummary.unreadDoubtReplies,
                                            dailyReminder = dailyReminder,
                                            onDismissUrgentBanner = vm::dismissUrgentBanner,
                                            onOpenNotifications = vm::openNotifications,
                                            onOpenDoubtBox = { vm.openDoubtBox() },
                                            onAskQuickDoubt = {
                                                vm.openAskDoubtSheet(
                                                    examId = activeExamId,
                                                    subject = focusSubject.ifBlank { "MSN" }
                                                )
                                            },
                                            examRoadmap = examRoadmap,
                                            srsDueCount = srsNotebookSummary.dueEntries.size,
                                            onSetTargetExamDate = vm::setTargetExamDate,
                                            onOpenClinicalTools = vm::openClinicalTools,
                                            onStartSrsDueDrill = vm::startSrsDueDrill,
                                            grandTestEnabled = grandTestEnabled,
                                            liveGrandTest = liveGrandTest,
                                            onOpenGrandTestHub = vm::openGrandTestHub,
                                            smartStudyPlan = smartStudyPlan,
                                            onStartSmartWeakDrill = { vm.startSmartWeakTopicDrill() },
                                            onStartRecoveryDayDrill = { subjCode, count ->
                                                vm.startSmartWeakTopicDrill(subjectCode = subjCode, targetCount = count)
                                            },
                                            onStartImageSpotterDrill = vm::startImageBasedSpotterDrill
                                        )

                                        MainTab.SUBJECTS -> SubjectsHubScreen(
                                            state = state,
                                            practicePathway = practicePathway,
                                            subjectExamFilter = subjectExamFilter,
                                            activeSection = subjectSection,
                                            difficultyFilter = difficultyFilter,
                                            onSelectPathway = vm::selectPracticePathway,
                                            onSelectSubjectExamFilter = vm::selectSubjectPracticeExamFilter,
                                            onSelectExamStage = vm::selectExamStage,
                                            onSelectDifficulty = vm::selectDifficultyFilter,
                                            onSelectSection = vm::selectSubjectSection,
                                            onOpenSubjectChapters = vm::openSubjectChapters,
                                            onStartPractice = { list, title -> vm.startPractice(list, title) },
                                            onStartCrossExamSubjectPractice = vm::startCrossExamSubjectPractice,
                                            onStartTimedConfig = { subjCode, useCrossExam, filterId ->
                                                vm.openTimedPracticeConfig(
                                                    defaultSource = subjCode,
                                                    defaultCount = 15,
                                                    useCrossExamPool = useCrossExam,
                                                    crossExamFilterId = filterId
                                                )
                                            },
                                            onStartCbtCustom = { list, title -> vm.startCbtCustom(list, title) },
                                            onOpenImporter = vm::openImporter,
                                            subscription = subscriptionState
                                        )

                                        MainTab.CBT -> CbtHubScreen(
                                            state = state,
                                            useTimeLimit = useTimeLimit,
                                            secondsPerQuestion = secondsPerQuestion,
                                            onSelectExamStage = vm::selectExamStage,
                                            onStartStandardMock = vm::startCbtStandardMock,
                                            onStartNorcetMainsMock = vm::startNorcetMainsMock,
                                            onStartCustomCbt = { list, title, mins -> vm.startCbtCustom(list, title, mins) },
                                            onPracticePredictedBank = { list, title -> vm.startPractice(list, title) },
                                            onOpenTimedPracticeConfig = { vm.openTimedPracticeConfig() },
                                            grandTestEnabled = grandTestEnabled,
                                            liveGrandTest = liveGrandTest,
                                            onOpenGrandTestHub = vm::openGrandTestHub
                                        )

                                        MainTab.STATS -> StatsScreen(
                                            state = state,
                                            onPracticeLowestAccuracy = { list ->
                                                vm.startPractice(list, "Lowest Accuracy Review")
                                            },
                                            onToggleBookmark = vm::toggleBookmark,
                                            onClearStats = vm::clearAllStatistics,
                                            readiness = examReadiness,
                                            onPracticeSubject = { subjectCode ->
                                                vm.openSubjectChapters(subjectCode)
                                            }
                                        )

                                        MainTab.DECK -> BookmarksScreen(
                                            state = state,
                                            srsSummary = srsNotebookSummary,
                                            onPracticeDeck = { list, title -> vm.startPractice(list, title) },
                                            onOpenPdfExport = { sourceKey -> vm.openPdfExport(defaultSource = sourceKey, defaultLimit = 0) },
                                            onToggleBookmark = vm::toggleBookmark,
                                            onResetSrsCard = vm::resetSrsCard,
                                            onRemoveSrsCard = vm::removeSrsCard
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TimedPracticeConfigDialog(
                    state = timedConfig,
                    allQuestions = state.questions,
                    activeExamId = activeExamId,
                    useTimeLimit = useTimeLimit,
                    negativeMarking = negMarking,
                    onToggleNegativeMarking = vm::toggleNegativeMarking,
                    onSelectSource = vm::updateTimedConfigSource,
                    onSelectDifficulty = vm::updateTimedConfigDifficulty,
                    onSelectCount = vm::updateTimedConfigCount,
                    onSelectMinutes = vm::updateTimedConfigMinutes,
                    onStart = vm::launchConfiguredTimedPractice,
                    onDismiss = vm::closeTimedPracticeConfig,
                    filterCountProvider = { key, diff -> vm.timedConfigMatchingCount(key, diff) }
                )

                if (subscriptionState.isAdmin) {
                    ImporterModalSheet(
                        state = importerState,
                        activeExamShortName = state.activeExam?.shortName ?: "RRB Nursing",
                        backupFolderSummary = backupFolderSummary,
                        isBackupBusy = isBackupBusy,
                        onUpdate = vm::updateImporter,
                        onPickPdfFile = {
                            runCatching {
                                importerPdfFileLauncher.launch(arrayOf("*/*"))
                            }.onFailure { err ->
                                vm.showMessage("Could not open file picker: ${err.message}")
                            }
                        },
                        onPreviewPdf = vm::previewPdfTextImport,
                        onCommitPdf = vm::commitPreviewImport,
                        onImportJson = vm::importJsonBatch,
                        onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                        onResetBackupFolder = vm::resetBackupFolderToDefault,
                        onExportBackup = vm::createBackupInActiveFolder,
                        onRestoreBackup = {
                            if (backupFolderSummary.backupCount > 0) {
                                vm.restoreLatestFromActiveFolder()
                            } else {
                                restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        },
                        onOpenPdfExport = {
                            vm.closeImporter()
                            vm.openPdfExport()
                        },
                        onClearCustom = vm::clearCustomQuestions,
                        onDismiss = vm::closeImporter
                    )
                }

                ExamPickerModalSheet(
                    isVisible = isExamPickerOpen,
                    professions = state.availableProfessions,
                    exams = state.availableExams,
                    activeExamId = activeExamId,
                    examQuestionCounts = state.examQuestionCounts,
                    onSelectExam = vm::selectActiveExam,
                    onDismiss = vm::closeExamPicker,
                    subscription = subscriptionState
                )

                val pdfMatchingCount = remember(pdfExportConfig, state.questions, state.lowestAccuracyQuestions) {
                    if (!pdfExportConfig.isVisible) 0
                    else vm.resolvedPdfWorksheetQuestions(pdfExportConfig, state).size
                }
                val suggestedPdfName = remember(pdfExportConfig, pdfMatchingCount) {
                    if (!pdfExportConfig.isVisible) ""
                    else vm.suggestedPdfFileName(pdfExportConfig, state)
                }
                PdfExportModalSheet(
                    state = pdfExportConfig,
                    matchingCount = pdfMatchingCount,
                    suggestedFileName = suggestedPdfName,
                    onUpdate = vm::updatePdfExportConfig,
                    onExportPdf = {
                        pdfExportLauncher.launch(suggestedPdfName)
                    },
                    onDismiss = vm::closePdfExport
                )

                AppUpdateAvailableDialog(
                    updateState = appUpdateState,
                    onDownloadUpdate = {
                        val url = appUpdateState.downloadUrl.ifBlank { appUpdateState.releasePageUrl }
                        vm.dismissUpdateDialog()
                        if (url.startsWith("https://")) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }.onFailure {
                                vm.showMessage("Could not open browser for update link.")
                            }
                        }
                    },
                    onDismiss = vm::dismissUpdateDialog
                )

                paywallPrompt?.let { prompt ->
                    PaywallPromptDialog(
                        prompt = prompt,
                        canStartTrial = !subscriptionState.trialUsed && !subscriptionState.isAdmin,
                        onDismiss = vm::dismissPaywallPrompt,
                        onOpenPlans = vm::openPlans,
                        onStartTrial = vm::claimThreeDayFreeTrial
                    )
                }

                deviceLockConflict?.let { conflict ->
                    DeviceLockSupportDialog(
                        conflict = conflict,
                        subscription = subscriptionState,
                        account = signedInAccount,
                        onDismiss = vm::dismissDeviceLockConflict,
                        onShowMessage = vm::showMessage
                    )
                }

                AskDoubtModalSheet(
                    context = doubtAskContext,
                    isBusy = isEngagementBusy,
                    onSubmit = { examId, subject, questionId, questionPreview, doubtText ->
                        vm.submitStudentDoubt(
                            examId = examId,
                            subject = subject,
                            questionId = questionId,
                            questionPreview = questionPreview,
                            doubtText = doubtText
                        )
                    },
                    onDismiss = vm::dismissAskDoubtSheet
                )

                AiClinicalTutorSheet(
                    question = aiTutorQuestion,
                    uiState = aiTutorState,
                    onAskFollowUp = vm::askAiTutorFollowUp,
                    onRetry = vm::retryAiTutor,
                    onDismiss = vm::closeAiTutor
                )

                FlagQuestionModalSheet(
                    question = flagQuestionTarget,
                    isBusy = isFlagBusy,
                    onSubmit = vm::submitQuestionReport,
                    onDismiss = vm::closeFlagQuestionSheet
                )
            }
        }
    }
}

@Composable
private fun NavTabIcon(tab: MainTab, selected: Boolean) {
    val icon = when (tab) {
        MainTab.DASHBOARD -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        MainTab.SUBJECTS -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
        MainTab.CBT -> if (selected) Icons.Filled.Quiz else Icons.Outlined.Quiz
        MainTab.STATS -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
        MainTab.DECK -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    }
    Icon(imageVector = icon, contentDescription = tab.label)
}

private fun resolveDisplayFileName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty().substringAfterLast('/').ifBlank { "uploaded_pack.pdf" }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
