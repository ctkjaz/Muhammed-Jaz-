package com.example.ui

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.grandtest.GrandTestStatus
import com.example.data.local.ProfileAvatarStorage
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.PracticePathway
import com.example.data.update.AppUpdateVersionLogic
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
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CbtExamScreen
import com.example.ui.screens.CbtHubScreen
import com.example.ui.screens.ClinicalToolsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceLockSupportDialog
import com.example.ui.screens.DoubtBoxScreen
import com.example.ui.screens.EcgLearningStudioScreen
import com.example.ui.screens.FlashcardDeckScreen
import com.example.ui.screens.GrandTestScreen
import com.example.ui.screens.IbqHubScreen
import com.example.ui.screens.NotificationsInboxScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PaywallPromptDialog
import com.example.ui.screens.PlansAndAccessScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProfileDrawerContent
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpecialtyTracksScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SubjectChaptersScreen
import com.example.ui.screens.SubjectsHubScreen
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PracticeSessionState
import com.example.ui.viewmodel.SubjectChaptersNavState
import com.example.ui.viewmodel.SubjectHubSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ClinEdgeSnackbarObserver(
    vm: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }
}

@Composable
internal fun ClinEdgeWindowFlagEffects(
    vm: MainViewModel,
    inActiveSession: Boolean,
    inSubScreen: Boolean,
    selectedTab: MainTab
) {
    val context = LocalContext.current
    val keepScreenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val pdfExportConfig by vm.pdfExportConfig.collectAsStateWithLifecycle()
    val practicePathway by vm.practicePathway.collectAsStateWithLifecycle()
    val subjectSection by vm.subjectHubSection.collectAsStateWithLifecycle()

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

    val shouldSecureScreen = !subscriptionState.isAdmin && (
        inActiveSession ||
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
}

@Composable
internal fun ClinEdgeOnboardingHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileRole by vm.profileRole.collectAsStateWithLifecycle()
    val targetYear by vm.targetYear.collectAsStateWithLifecycle()
    val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()
    val phoneNumber by vm.phoneNumber.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()

    OnboardingScreen(
        availableExams = state.availableExams,
        initialExamId = activeExamId,
        initialName = profileName.ifBlank { vm.signedInAccount.displayName },
        initialRole = profileRole,
        initialTargetYear = targetYear,
        initialFocusSubject = focusSubject,
        initialPhoneNumber = phoneNumber,
        initialDailyGoal = studyGoal.dailyGoalQuestions,
        secondsPerQuestion = secondsPerQuestion,
        onComplete = vm::completeOnboarding,
        onSkip = vm::skipOnboarding
    )
}

@Composable
internal fun ClinEdgeDrawerHost(
    vm: MainViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onSignOut: () -> Unit
) {
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileRole by vm.profileRole.collectAsStateWithLifecycle()
    val targetYear by vm.targetYear.collectAsStateWithLifecycle()
    val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()
    val profileAvatarPath by vm.profileAvatarPath.collectAsStateWithLifecycle()
    val cloudSyncState by vm.cloudSyncState.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val state by vm.mainState.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val unreadSummary by vm.unreadSummary.collectAsStateWithLifecycle()

    ProfileDrawerContent(
        profileName = profileName,
        profileRole = profileRole,
        targetYear = targetYear,
        focusSubject = focusSubject,
        avatarPath = profileAvatarPath,
        account = vm.signedInAccount,
        cloudSyncState = cloudSyncState,
        studyGoal = studyGoal,
        state = state,
        subscription = subscriptionState,
        unreadNotificationsCount = unreadSummary.totalBellUnread,
        unreadDoubtReplies = unreadSummary.unreadDoubtReplies,
        onOpenFullProfile = { scope.launch { drawerState.close(); vm.openProfile() } },
        onOpenPlans = { scope.launch { drawerState.close(); vm.openPlans() } },
        onOpenNotifications = { scope.launch { drawerState.close(); vm.openNotifications() } },
        onOpenDoubtBox = { scope.launch { drawerState.close(); vm.openDoubtBox() } },
        onOpenClinicalTools = { scope.launch { drawerState.close(); vm.openClinicalTools() } },
        onOpenExamPicker = { scope.launch { drawerState.close(); vm.openExamPicker() } },
        onOpenSubjectSection = { section -> scope.launch { drawerState.close(); vm.openSubjectHub(section) } },
        onOpenSettings = { scope.launch { drawerState.close(); vm.openSettings() } },
        onOpenImporter = { scope.launch { drawerState.close(); vm.openImporter() } },
        onSyncNow = { scope.launch { drawerState.close(); vm.syncNow() } },
        onSignOut = { scope.launch { drawerState.close(); onSignOut() } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClinEdgeTopBarHost(
    vm: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileAvatarPath by vm.profileAvatarPath.collectAsStateWithLifecycle()
    val state by vm.mainState.collectAsStateWithLifecycle()
    val unreadSummary by vm.unreadSummary.collectAsStateWithLifecycle()

    Column {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            navigationIcon = {
                IconButton(
                    onClick = onOpenDrawer,
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

@Composable
internal fun ClinEdgeActiveSessionHost(vm: MainViewModel) {
    val cbtSession by vm.cbtSession.collectAsStateWithLifecycle()
    val practiceSession by vm.practiceSession.collectAsStateWithLifecycle()

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
                onDismissSectionNotice = vm::dismissCbtSectionNotice,
                timerStateFlow = vm.sessionTimerState
            )
        }
        practiceSession != null -> {
            PracticeSessionLeafHost(vm = vm, activeSession = practiceSession!!)
        }
    }
}

@Composable
private fun PracticeSessionLeafHost(
    vm: MainViewModel,
    activeSession: PracticeSessionState
) {
    val bookmarkedIds by vm.bookmarkedIds.collectAsStateWithLifecycle()
    val negMarking by vm.negativeMarking.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val questionNotes by vm.questionNotes.collectAsStateWithLifecycle()
    val srsCards by vm.srsCards.collectAsStateWithLifecycle()

    val currentQ = activeSession.questions.getOrNull(activeSession.currentIndex)
    val isCurrentBookmarked = currentQ != null && currentQ.id in bookmarkedIds

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
        onExplainOptionsWithAi = vm::openAiOptionBreakdown,
        onFlagQuestion = vm::openFlagQuestionSheet,
        questionNotes = questionNotes,
        onSaveQuestionNote = vm::saveQuestionNote,
        onDeleteQuestionNote = vm::deleteQuestionNote,
        isCurrentInSrs = currentQ != null && currentQ.id in srsCards,
        onQueueToSrs = vm::queueQuestionToSrs,
        onExit = vm::exitPracticeOrCbt,
        timerStateFlow = vm.sessionTimerState
    )
}

@Composable
internal fun ClinEdgeOverlayScreenHost(
    vm: MainViewModel,
    isAdminOpen: Boolean,
    isNotificationsOpen: Boolean,
    isDoubtBoxOpen: Boolean,
    isClinicalToolsOpen: Boolean,
    isEcgStudioOpen: Boolean,
    isIbqHubOpen: Boolean,
    isSpecialtyTracksOpen: Boolean,
    isFlashcardStudioOpen: Boolean,
    isGrandTestHubOpen: Boolean,
    isPlansOpen: Boolean,
    isSettingsOpen: Boolean,
    isProfileOpen: Boolean,
    subjectChaptersNav: SubjectChaptersNavState?,
    adminPackFileLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    backupFolderLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    restoreLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    profilePhotoLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    scope: CoroutineScope,
    onSignOut: () -> Unit
) {
    when {
        isAdminOpen -> AdminOverlayHost(vm = vm, adminPackFileLauncher = adminPackFileLauncher)
        isNotificationsOpen -> NotificationsOverlayHost(vm = vm)
        isDoubtBoxOpen -> DoubtBoxOverlayHost(vm = vm)
        isClinicalToolsOpen -> ClinicalToolsOverlayHost(vm = vm)
        isEcgStudioOpen -> EcgLearningStudioScreen(
            onBack = vm::closeEcgStudio,
            onPracticeAnswer = vm::recordClinicalModulePracticeAnswer
        )
        isIbqHubOpen -> IbqHubOverlayHost(vm = vm)
        isSpecialtyTracksOpen -> SpecialtyTracksOverlayHost(vm = vm)
        isFlashcardStudioOpen -> FlashcardStudioOverlayHost(vm = vm)
        isGrandTestHubOpen -> GrandTestOverlayHost(vm = vm)
        isPlansOpen -> PlansOverlayHost(vm = vm)
        isSettingsOpen -> SettingsOverlayHost(
            vm = vm,
            backupFolderLauncher = backupFolderLauncher,
            restoreLauncher = restoreLauncher
        )
        isProfileOpen -> ProfileOverlayHost(
            vm = vm,
            profilePhotoLauncher = profilePhotoLauncher,
            scope = scope,
            onSignOut = onSignOut
        )
        subjectChaptersNav != null -> SubjectChaptersOverlayHost(vm = vm, navState = subjectChaptersNav)
    }
}

@Composable
private fun AdminOverlayHost(
    vm: MainViewModel,
    adminPackFileLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    if (!subscriptionState.isAdmin) return
    val adminState by vm.adminState.collectAsStateWithLifecycle()
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val adminDoubts by vm.adminDoubts.collectAsStateWithLifecycle()
    val aiDraftingDoubtId by vm.aiDraftingDoubtId.collectAsStateWithLifecycle()

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

@Composable
private fun NotificationsOverlayHost(vm: MainViewModel) {
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val directNotifications by vm.directNotifications.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val state by vm.mainState.collectAsStateWithLifecycle()
    val lastSeenAnnouncementMs by vm.lastSeenAnnouncementMs.collectAsStateWithLifecycle()

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

@Composable
private fun DoubtBoxOverlayHost(vm: MainViewModel) {
    val studentDoubts by vm.studentDoubts.collectAsStateWithLifecycle()
    val highlightedDoubtId by vm.highlightedDoubtId.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()

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

@Composable
private fun ClinicalToolsOverlayHost(vm: MainViewModel) {
    val clinicalToolsInitialSection by vm.clinicalToolsInitialSection.collectAsStateWithLifecycle()
    val dailyFocusProgress by vm.dailyFocusProgress.collectAsStateWithLifecycle()

    ClinicalToolsScreen(
        onBack = vm::closeClinicalTools,
        initialSection = clinicalToolsInitialSection,
        dailyFocusProgress = dailyFocusProgress,
        onFocusSessionCompleted = vm::recordFocusSessionCompleted,
        onOpenEcgStudio = vm::openEcgStudio,
        onAbgPracticeAnswered = vm::recordClinicalModulePracticeAnswer
    )
}

@Composable
private fun IbqHubOverlayHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val ibqHubInitialCategory by vm.ibqHubInitialCategory.collectAsStateWithLifecycle()
    val ibqBank = remember(state.stageQuestions, state.questions, state.crossExamQuestions) {
        val primary = state.stageQuestions.ifEmpty { state.questions }
            .filter { it.question.isImageBased }
        val fallback = state.crossExamQuestions.filter { it.question.isImageBased }
        (primary + fallback).distinctBy { it.id }
    }

    IbqHubScreen(
        allIbqQuestions = ibqBank,
        initialCategory = ibqHubInitialCategory,
        onBack = vm::closeIbqHub,
        onStartPractice = { list, title -> vm.startPractice(list, title) },
        onOpenEcgStudio = vm::openEcgStudio
    )
}

@Composable
private fun SpecialtyTracksOverlayHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val specialtyTrackInitial by vm.specialtyTrackInitial.collectAsStateWithLifecycle()
    val unifiedPool = remember(state.stageQuestions, state.questions, state.crossExamQuestions) {
        val primary = state.stageQuestions.ifEmpty { state.questions }
        (primary + state.crossExamQuestions).distinctBy { it.id }
    }

    SpecialtyTracksScreen(
        allQuestions = unifiedPool,
        initialTrack = specialtyTrackInitial,
        onBack = vm::closeSpecialtyTracks,
        onStartPractice = { list, title -> vm.startPractice(list, title) },
        onOpenEcgStudio = vm::openEcgStudio,
        onOpenAbgInterpreter = { vm.openClinicalTools(1) },
        onOpenClinicalCalcs = { vm.openClinicalTools(0) },
        onOpenIbqHubCategory = { cat -> vm.openIbqHub(cat) },
        onOpenFlashcardStudio = vm::openFlashcardStudio
    )
}

@Composable
private fun FlashcardStudioOverlayHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val masteredFlashcardIds by vm.masteredFlashcardIds.collectAsStateWithLifecycle()

    FlashcardDeckScreen(
        caFlashcards = state.flashcards,
        masteredIds = masteredFlashcardIds,
        onMarkCardMastered = vm::setFlashcardMastered,
        onResetMastery = vm::resetFlashcardMastery,
        onBack = vm::closeFlashcardStudio
    )
}

@Composable
private fun GrandTestOverlayHost(vm: MainViewModel) {
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    if (!subscriptionState.config.grandTestEnabled) return
    val publicGrandTests by vm.publicGrandTests.collectAsStateWithLifecycle()
    val selectedGrandTestId by vm.selectedGrandTestId.collectAsStateWithLifecycle()
    val grandTestLeaderboard by vm.grandTestLeaderboard.collectAsStateWithLifecycle()
    val isGrandTestLoading by vm.isGrandTestLoading.collectAsStateWithLifecycle()

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

@Composable
private fun PlansOverlayHost(vm: MainViewModel) {
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val isTrialBusy by vm.isTrialBusy.collectAsStateWithLifecycle()
    val isRedeemBusy by vm.isRedeemBusy.collectAsStateWithLifecycle()
    val plansExamLockContext by vm.plansExamLockContext.collectAsStateWithLifecycle()
    val studentPaymentRequests by vm.studentPaymentRequests.collectAsStateWithLifecycle()
    val isPaymentSubmitBusy by vm.isPaymentSubmitBusy.collectAsStateWithLifecycle()

    PlansAndAccessScreen(
        subscription = subscriptionState,
        account = vm.signedInAccount,
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

@Composable
private fun SettingsOverlayHost(
    vm: MainViewModel,
    backupFolderLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    restoreLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    val context = LocalContext.current
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val themePref by vm.themePreference.collectAsStateWithLifecycle()
    val useTimeLimit by vm.useTimeLimit.collectAsStateWithLifecycle()
    val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()
    val negMarking by vm.negativeMarking.collectAsStateWithLifecycle()
    val keepScreenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val dailyReminder by vm.dailyReminderState.collectAsStateWithLifecycle()
    val backgroundPushEnabled by vm.backgroundPushEnabled.collectAsStateWithLifecycle()
    val lastBackgroundSyncMs by vm.lastBackgroundSyncMs.collectAsStateWithLifecycle()
    val backupFolderSummary by vm.backupFolderSummary.collectAsStateWithLifecycle()
    val isBackupBusy by vm.isBackupBusy.collectAsStateWithLifecycle()
    val appUpdateState by vm.appUpdateState.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val state by vm.mainState.collectAsStateWithLifecycle()

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

@Composable
private fun ProfileOverlayHost(
    vm: MainViewModel,
    profilePhotoLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    scope: CoroutineScope,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val profileRole by vm.profileRole.collectAsStateWithLifecycle()
    val targetYear by vm.targetYear.collectAsStateWithLifecycle()
    val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()
    val phoneNumber by vm.phoneNumber.collectAsStateWithLifecycle()
    val profileAvatarPath by vm.profileAvatarPath.collectAsStateWithLifecycle()
    val cloudSyncState by vm.cloudSyncState.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val state by vm.mainState.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()

    ProfileScreen(
        profileName = profileName,
        profileRole = profileRole,
        targetYear = targetYear,
        focusSubject = focusSubject,
        phoneNumber = phoneNumber,
        avatarPath = profileAvatarPath,
        currentDeviceName = vm.currentDeviceName,
        account = vm.signedInAccount,
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
        onSignOut = onSignOut,
        onBack = vm::closeProfile
    )
}

@Composable
private fun SubjectChaptersOverlayHost(
    vm: MainViewModel,
    navState: SubjectChaptersNavState
) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val difficultyFilter by vm.difficultyFilter.collectAsStateWithLifecycle()
    val chaptersHighYieldOnly by vm.chaptersHighYieldOnly.collectAsStateWithLifecycle()
    val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()

    SubjectChaptersScreen(
        navState = navState,
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

@Composable
internal fun ClinEdgeMainTabHost(
    vm: MainViewModel,
    selectedTab: MainTab
) {
    when (selectedTab) {
        MainTab.DASHBOARD -> DashboardTabHost(vm)
        MainTab.SUBJECTS -> SubjectsTabHost(vm)
        MainTab.CBT -> CbtTabHost(vm)
        MainTab.STATS -> StatsTabHost(vm)
        MainTab.DECK -> DeckTabHost(vm)
    }
}

@Composable
private fun DashboardTabHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val profileName by vm.profileName.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val difficultyFilter by vm.difficultyFilter.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val activeUrgentBanner by vm.activeUrgentBanner.collectAsStateWithLifecycle()
    val unreadSummary by vm.unreadSummary.collectAsStateWithLifecycle()
    val dailyReminder by vm.dailyReminderState.collectAsStateWithLifecycle()
    val focusSubject by vm.focusSubject.collectAsStateWithLifecycle()
    val examRoadmap by vm.examRoadmap.collectAsStateWithLifecycle()
    val srsNotebookSummary by vm.srsNotebookSummary.collectAsStateWithLifecycle()
    val publicGrandTests by vm.publicGrandTests.collectAsStateWithLifecycle()
    val smartStudyPlan by vm.smartStudyPlan.collectAsStateWithLifecycle()
    val masteredFlashcardIds by vm.masteredFlashcardIds.collectAsStateWithLifecycle()
    val qotdCompletion by vm.qotdCompletion.collectAsStateWithLifecycle()
    val gamificationState by vm.gamificationState.collectAsStateWithLifecycle()

    val grandTestEnabled = subscriptionState.config.grandTestEnabled
    val liveGrandTest = remember(publicGrandTests) {
        publicGrandTests.firstOrNull { it.parsedStatus == GrandTestStatus.LIVE }
    }

    DashboardScreen(
        state = state,
        profileName = profileName.ifBlank { vm.signedInAccount.displayName },
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
        onOpenClinicalTools = { vm.openClinicalTools(0) },
        onStartSrsDueDrill = vm::startSrsDueDrill,
        grandTestEnabled = grandTestEnabled,
        liveGrandTest = liveGrandTest,
        onOpenGrandTestHub = vm::openGrandTestHub,
        smartStudyPlan = smartStudyPlan,
        onStartSmartWeakDrill = { vm.startSmartWeakTopicDrill() },
        onStartRecoveryDayDrill = { subjCode, count ->
            vm.startSmartWeakTopicDrill(subjectCode = subjCode, targetCount = count)
        },
        onStartImageSpotterDrill = { vm.openIbqHub() },
        onStartGeneralMixedDrill = { pool, count, title ->
            vm.startGeneralMixedDrill(pool, count, title)
        },
        onOpenFlashcardStudio = vm::openFlashcardStudio,
        masteredFlashcardCount = masteredFlashcardIds.size,
        qotdQuestion = vm.todayQuestionOfTheDay,
        qotdCompletion = qotdCompletion,
        onAnswerQotd = vm::answerQuestionOfTheDay,
        onUpdateDailyGoal = vm::setDailyGoalQuestions,
        gamificationState = gamificationState,
        onOpenAbgInterpreter = { vm.openClinicalTools(1) },
        onOpenEcgStudio = vm::openEcgStudio,
        onOpenSpecialtyTracks = { vm.openSpecialtyTracks() }
    )
}

@Composable
private fun SubjectsTabHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val practicePathway by vm.practicePathway.collectAsStateWithLifecycle()
    val subjectExamFilter by vm.subjectPracticeExamFilter.collectAsStateWithLifecycle()
    val subjectSection by vm.subjectHubSection.collectAsStateWithLifecycle()
    val difficultyFilter by vm.difficultyFilter.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()

    SubjectsHubScreen(
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
        onOpenIbqHub = { vm.openIbqHub() },
        onOpenSpecialtyTracks = { vm.openSpecialtyTracks() },
        subscription = subscriptionState
    )
}

@Composable
private fun CbtTabHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val useTimeLimit by vm.useTimeLimit.collectAsStateWithLifecycle()
    val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    val publicGrandTests by vm.publicGrandTests.collectAsStateWithLifecycle()

    val grandTestEnabled = subscriptionState.config.grandTestEnabled
    val liveGrandTest = remember(publicGrandTests) {
        publicGrandTests.firstOrNull { it.parsedStatus == GrandTestStatus.LIVE }
    }

    CbtHubScreen(
        state = state,
        useTimeLimit = useTimeLimit,
        secondsPerQuestion = secondsPerQuestion,
        onSelectExamStage = vm::selectExamStage,
        onStartStandardMock = vm::startCbtStandardMock,
        onStartNorcetMainsMock = vm::startNorcetMainsMock,
        onStartCustomCbt = { list, title, mins -> vm.startCbtCustom(list, title, mins) },
        onStartConfiguredCustomCbt = { list, title, hasTimer, mins, neg ->
            vm.startCbtCustom(
                items = list,
                title = title,
                durationMinutes = mins,
                hasCountdownOverride = hasTimer,
                negativeMarksOverride = neg
            )
        },
        onPracticePredictedBank = { list, title -> vm.startPractice(list, title) },
        onOpenTimedPracticeConfig = { vm.openTimedPracticeConfig() },
        grandTestEnabled = grandTestEnabled,
        liveGrandTest = liveGrandTest,
        onOpenGrandTestHub = vm::openGrandTestHub
    )
}

@Composable
private fun StatsTabHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val examReadiness by vm.examReadiness.collectAsStateWithLifecycle()
    val studyGoal by vm.studyGoalState.collectAsStateWithLifecycle()
    val gamificationState by vm.gamificationState.collectAsStateWithLifecycle()

    StatsScreen(
        state = state,
        onPracticeLowestAccuracy = { list ->
            vm.startPractice(list, "Lowest Accuracy Review")
        },
        onToggleBookmark = vm::toggleBookmark,
        onClearStats = vm::clearAllStatistics,
        readiness = examReadiness,
        streakDays = studyGoal.effectiveCurrentStreak(),
        onPracticeSubject = { subjectCode ->
            vm.openSubjectChapters(subjectCode)
        },
        gamificationState = gamificationState,
        studyGoal = studyGoal
    )
}

@Composable
private fun DeckTabHost(vm: MainViewModel) {
    val state by vm.mainState.collectAsStateWithLifecycle()
    val srsNotebookSummary by vm.srsNotebookSummary.collectAsStateWithLifecycle()
    val questionNotes by vm.questionNotes.collectAsStateWithLifecycle()

    BookmarksScreen(
        state = state,
        srsSummary = srsNotebookSummary,
        onPracticeDeck = { list, title -> vm.startPractice(list, title) },
        onOpenPdfExport = { sourceKey -> vm.openPdfExport(defaultSource = sourceKey, defaultLimit = 0) },
        onToggleBookmark = vm::toggleBookmark,
        onResetSrsCard = vm::resetSrsCard,
        onMarkSrsMastered = vm::markSrsCardMastered,
        onRemoveSrsCard = vm::removeSrsCard,
        onQueueQuestionToSrs = vm::queueQuestionToSrs,
        onQueueAllBookmarkedToSrs = vm::queueAllBookmarkedToSrs,
        questionNotes = questionNotes,
        onSaveQuestionNote = vm::saveQuestionNote,
        onDeleteQuestionNote = vm::deleteQuestionNote
    )
}

@Composable
internal fun ClinEdgeModalDialogsHost(
    vm: MainViewModel,
    backupFolderLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    restoreLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    pdfExportLauncher: ManagedActivityResultLauncher<String, Uri?>,
    importerPdfFileLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    TimedConfigModalHost(vm)
    ImporterModalHost(
        vm = vm,
        backupFolderLauncher = backupFolderLauncher,
        restoreLauncher = restoreLauncher,
        importerPdfFileLauncher = importerPdfFileLauncher
    )
    ExamPickerModalHost(vm)
    PdfExportModalHost(vm = vm, pdfExportLauncher = pdfExportLauncher)
    AppUpdateModalHost(vm)
    PaywallModalHost(vm)
    DeviceLockModalHost(vm)
    AskDoubtModalHost(vm)
    AiTutorModalHost(vm)
    FlagQuestionModalHost(vm)
}

@Composable
private fun TimedConfigModalHost(vm: MainViewModel) {
    val timedConfig by vm.timedConfig.collectAsStateWithLifecycle()
    if (!timedConfig.isVisible) return
    val state by vm.mainState.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val useTimeLimit by vm.useTimeLimit.collectAsStateWithLifecycle()
    val negMarking by vm.negativeMarking.collectAsStateWithLifecycle()

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
}

@Composable
private fun ImporterModalHost(
    vm: MainViewModel,
    backupFolderLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    restoreLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    importerPdfFileLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    val importerState by vm.importerState.collectAsStateWithLifecycle()
    if (!importerState.isVisible) return
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()
    if (!subscriptionState.isAdmin) return
    val state by vm.mainState.collectAsStateWithLifecycle()
    val backupFolderSummary by vm.backupFolderSummary.collectAsStateWithLifecycle()
    val isBackupBusy by vm.isBackupBusy.collectAsStateWithLifecycle()

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

@Composable
private fun ExamPickerModalHost(vm: MainViewModel) {
    val isExamPickerOpen by vm.isExamPickerOpen.collectAsStateWithLifecycle()
    if (!isExamPickerOpen) return
    val state by vm.mainState.collectAsStateWithLifecycle()
    val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()

    ExamPickerModalSheet(
        isVisible = true,
        professions = state.availableProfessions,
        exams = state.availableExams,
        activeExamId = activeExamId,
        examQuestionCounts = state.examQuestionCounts,
        onSelectExam = vm::selectActiveExam,
        onDismiss = vm::closeExamPicker,
        subscription = subscriptionState
    )
}

@Composable
private fun PdfExportModalHost(
    vm: MainViewModel,
    pdfExportLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    val pdfExportConfig by vm.pdfExportConfig.collectAsStateWithLifecycle()
    if (!pdfExportConfig.isVisible) return
    val state by vm.mainState.collectAsStateWithLifecycle()
    val pdfMatchingCount = remember(pdfExportConfig, state.questions, state.lowestAccuracyQuestions) {
        vm.resolvedPdfWorksheetQuestions(pdfExportConfig, state).size
    }
    val suggestedPdfName = remember(pdfExportConfig, pdfMatchingCount) {
        vm.suggestedPdfFileName(pdfExportConfig, state)
    }

    PdfExportModalSheet(
        state = pdfExportConfig,
        matchingCount = pdfMatchingCount,
        suggestedFileName = suggestedPdfName,
        onUpdate = vm::updatePdfExportConfig,
        onExportPdf = { pdfExportLauncher.launch(suggestedPdfName) },
        onDismiss = vm::closePdfExport
    )
}

@Composable
private fun AppUpdateModalHost(vm: MainViewModel) {
    val context = LocalContext.current
    val appUpdateState by vm.appUpdateState.collectAsStateWithLifecycle()
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
}

@Composable
private fun PaywallModalHost(vm: MainViewModel) {
    val paywallPrompt by vm.paywallPrompt.collectAsStateWithLifecycle()
    val prompt = paywallPrompt ?: return
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()

    PaywallPromptDialog(
        prompt = prompt,
        canStartTrial = !subscriptionState.trialUsed && !subscriptionState.isAdmin,
        onDismiss = vm::dismissPaywallPrompt,
        onOpenPlans = vm::openPlans,
        onStartTrial = vm::claimThreeDayFreeTrial
    )
}

@Composable
private fun DeviceLockModalHost(vm: MainViewModel) {
    val deviceLockConflict by vm.deviceLockConflict.collectAsStateWithLifecycle()
    val conflict = deviceLockConflict ?: return
    val subscriptionState by vm.subscriptionState.collectAsStateWithLifecycle()

    DeviceLockSupportDialog(
        conflict = conflict,
        subscription = subscriptionState,
        account = vm.signedInAccount,
        onDismiss = vm::dismissDeviceLockConflict,
        onShowMessage = vm::showMessage
    )
}

@Composable
private fun AskDoubtModalHost(vm: MainViewModel) {
    val doubtAskContext by vm.doubtAskContext.collectAsStateWithLifecycle()
    if (doubtAskContext == null) return
    val isEngagementBusy by vm.isEngagementBusy.collectAsStateWithLifecycle()

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
}

@Composable
private fun AiTutorModalHost(vm: MainViewModel) {
    val aiTutorQuestion by vm.aiTutorQuestion.collectAsStateWithLifecycle()
    if (aiTutorQuestion == null) return
    val aiTutorState by vm.aiTutorState.collectAsStateWithLifecycle()

    AiClinicalTutorSheet(
        question = aiTutorQuestion,
        uiState = aiTutorState,
        onAskFollowUp = vm::askAiTutorFollowUp,
        onRetry = vm::retryAiTutor,
        onDismiss = vm::closeAiTutor
    )
}

@Composable
private fun FlagQuestionModalHost(vm: MainViewModel) {
    val flagQuestionTarget by vm.flagQuestionTarget.collectAsStateWithLifecycle()
    if (flagQuestionTarget == null) return
    val isFlagBusy by vm.isFlagBusy.collectAsStateWithLifecycle()

    FlagQuestionModalSheet(
        question = flagQuestionTarget,
        isBusy = isFlagBusy,
        onSubmit = vm::submitQuestionReport,
        onDismiss = vm::closeFlagQuestionSheet
    )
}
