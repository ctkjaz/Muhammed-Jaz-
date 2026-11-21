package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ThemePreference
import com.example.ui.components.ExamPickerModalSheet
import com.example.ui.components.ImporterModalSheet
import com.example.ui.components.PdfExportModalSheet
import com.example.ui.components.ProfileAvatarCircle
import com.example.ui.components.TimedPracticeConfigDialog
import com.example.ui.components.formatApproxQuestionCount
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.CbtExamScreen
import com.example.ui.screens.CbtHubScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProfileDrawerContent
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SubjectsHubScreen
import com.example.ui.theme.RrbNursingTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RrbNursingApp(vm: MainViewModel = viewModel()) {
    val themePref by vm.themePreference.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePref) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }

    RrbNursingTheme(darkTheme = isDark) {
        val state by vm.mainState.collectAsStateWithLifecycle()
        val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
        val isSettingsOpen by vm.isSettingsOpen.collectAsStateWithLifecycle()
        val isProfileOpen by vm.isProfileOpen.collectAsStateWithLifecycle()
        val isExamPickerOpen by vm.isExamPickerOpen.collectAsStateWithLifecycle()
        val activeExamId by vm.activeExamId.collectAsStateWithLifecycle()
        val subjectSection by vm.subjectHubSection.collectAsStateWithLifecycle()
        val difficultyFilter by vm.difficultyFilter.collectAsStateWithLifecycle()
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
        val useTimeLimit by vm.useTimeLimit.collectAsStateWithLifecycle()
        val secondsPerQuestion by vm.secondsPerQuestion.collectAsStateWithLifecycle()
        val keepScreenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
        val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                vm.clearSnackbar()
            }
        }

        // Zero-permission SAF Backup Export & Restore
        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                runCatching {
                    val json = vm.buildBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    vm.showMessage("Backup exported successfully!")
                }.onFailure {
                    vm.showMessage("Export failed: ${it.message}")
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

        val inActiveSession = practiceSession != null || cbtSession != null
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

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !inActiveSession && !isSettingsOpen && !isProfileOpen,
            drawerContent = {
                ProfileDrawerContent(
                    profileName = profileName,
                    profileRole = profileRole,
                    targetYear = targetYear,
                    focusSubject = focusSubject,
                    state = state,
                    onOpenFullProfile = {
                        scope.launch {
                            drawerState.close()
                            vm.openProfile()
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
                    }
                )
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 720.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (!inActiveSession && !isSettingsOpen && !isProfileOpen) {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
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
                                            size = 36.dp
                                        )
                                    }
                                },
                                title = {
                                    val activeExamShort = state.activeExam?.shortName ?: "RRB Nursing"
                                    val approxCount = formatApproxQuestionCount(state.overall.totalQuestions)
                                    Column(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(onClick = vm::openExamPicker)
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                            .testTag("top_exam_picker_btn"),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "ClinEdge",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$activeExamShort • $approxCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowDown,
                                                contentDescription = "Switch active exam",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                            actions = {
                                IconButton(
                                    onClick = { vm.openPdfExport() },
                                    modifier = Modifier.testTag("top_export_pdf_btn")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF worksheet")
                                }
                                FilledTonalIconButton(
                                    onClick = { vm.openImporter() },
                                    modifier = Modifier.testTag("top_import_btn")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Import questions")
                                }
                                IconButton(
                                    onClick = { vm.selectTab(MainTab.DECK) },
                                    modifier = Modifier.testTag("top_bookmarks_btn")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (state.overall.bookmarkCount > 0) {
                                                Badge { Text("${state.overall.bookmarkCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Bookmark, contentDescription = "Revision Deck")
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
                    }
                },
                bottomBar = {
                    if (!inActiveSession && !useRail) {
                        NavigationBar {
                            MainTab.entries.forEach { tab ->
                                val selected = !isSettingsOpen && !isProfileOpen && tab == selectedTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { vm.selectTab(tab) },
                                    icon = { NavTabIcon(tab, selected) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.name}")
                                )
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
                    if (!inActiveSession && useRail) {
                        NavigationRail {
                            Spacer(modifier = Modifier.weight(0.1f))
                            MainTab.entries.forEach { tab ->
                                val selected = !isSettingsOpen && !isProfileOpen && tab == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { vm.selectTab(tab) },
                                    icon = { NavTabIcon(tab, selected) },
                                    label = { Text(tab.label) },
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
                                    onExit = vm::exitPracticeOrCbt
                                )
                            }

                            practiceSession != null -> {
                                PracticeScreen(
                                    session = practiceSession!!,
                                    allQuestions = state.questions,
                                    negativeMarking = negMarking,
                                    onToggleNegativeMarking = vm::toggleNegativeMarking,
                                    onSelectOption = vm::answerPracticeQuestion,
                                    onMoveIndex = vm::movePracticeIndex,
                                    onToggleBookmark = vm::toggleBookmark,
                                    onRequestSubmit = vm::requestPracticeSubmitPrompt,
                                    onDismissSubmitPrompt = vm::dismissPracticeSubmitPrompt,
                                    onConfirmSubmit = vm::submitTimedPractice,
                                    onChangeReviewFilter = vm::setPracticeReviewFilter,
                                    onExit = vm::exitPracticeOrCbt
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
                                    onProfileNameChange = vm::setProfileName,
                                    onThemePreferenceChange = vm::setThemePreference,
                                    onUseTimeLimitChange = vm::setUseTimeLimit,
                                    onSecondsPerQuestionChange = vm::setSecondsPerQuestion,
                                    onNegativeMarkingChange = vm::setNegativeMarking,
                                    onKeepScreenOnChange = vm::setKeepScreenOn,
                                    onOpenImporter = vm::openImporter,
                                    onOpenPdfExport = { vm.openPdfExport() },
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
                                    state = state,
                                    onProfileNameChange = vm::setProfileName,
                                    onProfileRoleChange = vm::setProfileRole,
                                    onTargetYearChange = vm::setTargetYear,
                                    onFocusSubjectChange = vm::setFocusSubject,
                                    onOpenExamPicker = vm::openExamPicker,
                                    onPracticeSubject = { list, title ->
                                        vm.closeProfile()
                                        vm.startPractice(list, title)
                                    },
                                    onOpenSettings = vm::openSettings,
                                    onBack = vm::closeProfile
                                )
                            }

                            else -> {
                                when (selectedTab) {
                                    MainTab.DASHBOARD -> DashboardScreen(
                                        state = state,
                                        difficultyFilter = difficultyFilter,
                                        onSelectDifficulty = vm::selectDifficultyFilter,
                                        onLaunchFullMock = {
                                            val fullQ = state.activeExam?.defaultMockQuestions ?: 100
                                            val fullMins = state.activeExam?.defaultMockMinutes ?: 90
                                            vm.startCbtStandardMock(
                                                totalQuestions = fullQ,
                                                durationMinutes = fullMins,
                                                title = "Full Official ${fullQ}Q CBT Mock",
                                                isOfficialFullMock = true
                                            )
                                        },
                                        onOpenTimedPractice = { vm.openTimedPracticeConfig() },
                                        onOpenSubjectSection = vm::openSubjectHub,
                                        onSelectTab = vm::selectTab,
                                        onOpenImporter = vm::openImporter,
                                        onOpenPdfExport = { vm.openPdfExport() },
                                        onStartDrill = { list, title -> vm.startPractice(list, title) }
                                    )

                                    MainTab.SUBJECTS -> SubjectsHubScreen(
                                        state = state,
                                        activeSection = subjectSection,
                                        difficultyFilter = difficultyFilter,
                                        onSelectDifficulty = vm::selectDifficultyFilter,
                                        onSelectSection = vm::selectSubjectSection,
                                        onStartPractice = { list, title -> vm.startPractice(list, title) },
                                        onStartTimedConfig = { subjCode -> vm.openTimedPracticeConfig(defaultSource = subjCode, defaultCount = 15) },
                                        onStartCbtCustom = { list, title -> vm.startCbtCustom(list, title) },
                                        onOpenImporter = vm::openImporter
                                    )

                                    MainTab.CBT -> CbtHubScreen(
                                        state = state,
                                        useTimeLimit = useTimeLimit,
                                        secondsPerQuestion = secondsPerQuestion,
                                        onStartStandardMock = vm::startCbtStandardMock,
                                        onStartCustomCbt = { list, title, mins -> vm.startCbtCustom(list, title, mins) },
                                        onOpenTimedPracticeConfig = { vm.openTimedPracticeConfig() }
                                    )

                                    MainTab.STATS -> StatsScreen(
                                        state = state,
                                        onPracticeLowestAccuracy = { list ->
                                            vm.startPractice(list, "Lowest Accuracy Review")
                                        },
                                        onToggleBookmark = vm::toggleBookmark,
                                        onClearStats = vm::clearAllStatistics
                                    )

                                    MainTab.DECK -> BookmarksScreen(
                                        state = state,
                                        onPracticeDeck = { list, title -> vm.startPractice(list, title) },
                                        onOpenPdfExport = { sourceKey -> vm.openPdfExport(defaultSource = sourceKey, defaultLimit = 0) },
                                        onToggleBookmark = vm::toggleBookmark
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
                useTimeLimit = useTimeLimit,
                negativeMarking = negMarking,
                onToggleNegativeMarking = vm::toggleNegativeMarking,
                onSelectSource = vm::updateTimedConfigSource,
                onSelectDifficulty = vm::updateTimedConfigDifficulty,
                onSelectCount = vm::updateTimedConfigCount,
                onSelectMinutes = vm::updateTimedConfigMinutes,
                onStart = vm::launchConfiguredTimedPractice,
                onDismiss = vm::closeTimedPracticeConfig,
                filterCountProvider = { key, diff -> vm.filteredQuestionsForSource(key, state.questions, diff).size }
            )

            ImporterModalSheet(
                state = importerState,
                activeExamShortName = state.activeExam?.shortName ?: "RRB Nursing",
                onUpdate = vm::updateImporter,
                onPreviewPdf = vm::previewPdfTextImport,
                onCommitPdf = vm::commitPreviewImport,
                onImportJson = vm::importJsonBatch,
                onExportBackup = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("clinedge-backup-$dateStr.json")
                },
                onRestoreBackup = {
                    restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                onOpenPdfExport = {
                    vm.closeImporter()
                    vm.openPdfExport()
                },
                onClearCustom = vm::clearCustomQuestions,
                onDismiss = vm::closeImporter
            )

            ExamPickerModalSheet(
                isVisible = isExamPickerOpen,
                professions = state.availableProfessions,
                exams = state.availableExams,
                activeExamId = activeExamId,
                examQuestionCounts = state.examQuestionCounts,
                onSelectExam = vm::selectActiveExam,
                onDismiss = vm::closeExamPicker
            )

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
                onExportPdf = {
                    pdfExportLauncher.launch(suggestedPdfName)
                },
                onDismiss = vm::closePdfExport
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
