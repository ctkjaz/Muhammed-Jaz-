package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileAvatarStorage
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.notifications.ClinEdgeNotificationManager
import com.example.data.notifications.CloudEngagementRepository
import com.example.data.repository.NursingPrepRepository
import com.example.data.subscription.DeviceLockManager
import com.example.data.subscription.PaymentAndReportRepository
import com.example.data.subscription.SubscriptionRepository
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.RrbNursingTheme
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
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
    val sharedSettings = remember(context) { SettingsPreferences.getInstance(context) }
    val initialTheme = remember(sharedSettings) { sharedSettings.themePreference.value }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        val forcedNotice = remember(user) { sharedSettings.getForcedSignOutNotice() }
        val skipAuto = remember(user) { sharedSettings.consumeSkipAutoSignInOnce() }
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
                val settings = SettingsPreferences.getInstance(app)
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

/**
 * Slim Zero-Lag App Shell: collects ONLY lightweight boolean/enum routing flags.
 * Heavy 10k-item dashboard snapshots, practice/CBT sessions, 1-second timers,
 * drawer telemetry, and 10 modal sheets each collect strictly inside their own
 * scoped leaf hosts in [AppShellHosts.kt].
 */
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
        val onboardingCompleted by vm.onboardingCompleted.collectAsStateWithLifecycle()
        val forcedSignOutEvent by vm.forcedSignOutEvent.collectAsStateWithLifecycle()
        val inActiveSession by vm.isInActiveSessionState.collectAsStateWithLifecycle()
        val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()

        // Lightweight overlay routing flags only
        val isSettingsOpen by vm.isSettingsOpen.collectAsStateWithLifecycle()
        val isProfileOpen by vm.isProfileOpen.collectAsStateWithLifecycle()
        val isPlansOpen by vm.isPlansOpen.collectAsStateWithLifecycle()
        val isAdminOpen by vm.isAdminOpen.collectAsStateWithLifecycle()
        val isNotificationsOpen by vm.isNotificationsOpen.collectAsStateWithLifecycle()
        val isDoubtBoxOpen by vm.isDoubtBoxOpen.collectAsStateWithLifecycle()
        val isClinicalToolsOpen by vm.isClinicalToolsOpen.collectAsStateWithLifecycle()
        val isEcgStudioOpen by vm.isEcgStudioOpen.collectAsStateWithLifecycle()
        val isIbqHubOpen by vm.isIbqHubOpen.collectAsStateWithLifecycle()
        val isSpecialtyTracksOpen by vm.isSpecialtyTracksOpen.collectAsStateWithLifecycle()
        val isFlashcardStudioOpen by vm.isFlashcardStudioOpen.collectAsStateWithLifecycle()
        val isClinicalCasesOpen by vm.isClinicalCasesOpen.collectAsStateWithLifecycle()
        val isGrandTestHubOpen by vm.isGrandTestHubOpen.collectAsStateWithLifecycle()
        val subjectChaptersNav by vm.subjectChaptersNav.collectAsStateWithLifecycle()

        val inSubScreen = isSettingsOpen || isProfileOpen || isPlansOpen || isAdminOpen ||
            isNotificationsOpen || isDoubtBoxOpen || isClinicalToolsOpen || isEcgStudioOpen ||
            isIbqHubOpen || isSpecialtyTracksOpen || isFlashcardStudioOpen ||
            isClinicalCasesOpen || isGrandTestHubOpen || subjectChaptersNav != null

        val signedInAccount = vm.signedInAccount
        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val credentialManager = remember(context) { CredentialManager.create(context) }

        ClinEdgeSnackbarObserver(vm = vm, snackbarHostState = snackbarHostState)
        ClinEdgeWindowFlagEffects(
            vm = vm,
            inActiveSession = inActiveSession,
            inSubScreen = inSubScreen,
            selectedTab = selectedTab
        )

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
                val prefs = SettingsPreferences.getInstance(context)
                if (!ClinEdgeNotificationManager.hasNotificationPermission(context) &&
                    !prefs.hasPromptedNotificationPermission()
                ) {
                    prefs.markNotificationPermissionPrompted()
                    startupNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        val performSignOut: () -> Unit = remember(credentialManager, scope, vm) {
            {
                signOut(
                    credentialManager = credentialManager,
                    scope = scope,
                    onBeforeSignOut = { vm.detachCloudSyncForSignOut(isForcedByOtherDevice = false) }
                )
            }
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

        // Zero-permission Activity Result Launchers
        val backupFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { treeUri ->
            if (treeUri != null) {
                vm.onBackupFolderSelected(treeUri)
            }
        }

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

        val importerPdfFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                val fileName = resolveDisplayFileName(context, uri)
                vm.importPdfFileForImporter(context, uri, fileName)
            }
        }

        if (!onboardingCompleted) {
            ClinEdgeOnboardingHost(vm = vm)
            return@RrbNursingTheme
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !inActiveSession && !inSubScreen,
            drawerContent = {
                ClinEdgeDrawerHost(
                    vm = vm,
                    drawerState = drawerState,
                    scope = scope,
                    onSignOut = performSignOut
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
                            ClinEdgeTopBarHost(
                                vm = vm,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
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
                                inActiveSession -> ClinEdgeActiveSessionHost(vm = vm)
                                inSubScreen -> ClinEdgeOverlayScreenHost(
                                    vm = vm,
                                    isAdminOpen = isAdminOpen,
                                    isNotificationsOpen = isNotificationsOpen,
                                    isDoubtBoxOpen = isDoubtBoxOpen,
                                    isClinicalToolsOpen = isClinicalToolsOpen,
                                    isEcgStudioOpen = isEcgStudioOpen,
                                    isIbqHubOpen = isIbqHubOpen,
                                    isSpecialtyTracksOpen = isSpecialtyTracksOpen,
                                    isFlashcardStudioOpen = isFlashcardStudioOpen,
                                    isClinicalCasesOpen = isClinicalCasesOpen,
                                    isGrandTestHubOpen = isGrandTestHubOpen,
                                    isPlansOpen = isPlansOpen,
                                    isSettingsOpen = isSettingsOpen,
                                    isProfileOpen = isProfileOpen,
                                    subjectChaptersNav = subjectChaptersNav,
                                    adminPackFileLauncher = adminPackFileLauncher,
                                    backupFolderLauncher = backupFolderLauncher,
                                    restoreLauncher = restoreLauncher,
                                    profilePhotoLauncher = profilePhotoLauncher,
                                    scope = scope,
                                    onSignOut = performSignOut
                                )
                                else -> ClinEdgeMainTabHost(
                                    vm = vm,
                                    selectedTab = selectedTab
                                )
                            }
                        }
                    }
                }

                ClinEdgeModalDialogsHost(
                    vm = vm,
                    backupFolderLauncher = backupFolderLauncher,
                    restoreLauncher = restoreLauncher,
                    pdfExportLauncher = pdfExportLauncher,
                    importerPdfFileLauncher = importerPdfFileLauncher
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

internal fun resolveDisplayFileName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty().substringAfterLast('/').ifBlank { "uploaded_pack.pdf" }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
