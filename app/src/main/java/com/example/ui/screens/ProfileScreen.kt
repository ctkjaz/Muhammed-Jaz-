package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CloudSyncUiState
import com.example.data.cloud.SignedInAccountInfo
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.SettingsPreferences
import com.example.data.model.AccountSessionLogic
import com.example.data.model.DailyStudyGoalState
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionItem
import com.example.data.model.StudyGoalLogic
import com.example.data.model.SubjectCatalog
import com.example.data.subscription.PlanTier
import com.example.data.subscription.SubscriptionUiState
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.ProfileAvatarCircle
import com.example.ui.theme.AcidLime
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.EditorialBannerSubtext
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.SubjectHubSection

@Composable
fun ProfileDrawerContent(
    profileName: String,
    profileRole: String,
    targetYear: String,
    focusSubject: String,
    avatarPath: String = "",
    account: SignedInAccountInfo,
    cloudSyncState: CloudSyncUiState,
    studyGoal: DailyStudyGoalState,
    state: DashboardDerivedState,
    subscription: SubscriptionUiState = SubscriptionUiState(),
    unreadNotificationsCount: Int = 0,
    unreadDoubtReplies: Int = 0,
    onOpenFullProfile: () -> Unit,
    onOpenPlans: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenDoubtBox: () -> Unit = {},
    onOpenClinicalTools: () -> Unit = {},
    onOpenExamPicker: () -> Unit,
    onOpenSubjectSection: (SubjectHubSection) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImporter: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = profileName.trim().ifBlank { "ClinEdge Learner" }
    val subtitleRole = profileRole.trim().ifBlank { "Healthcare Exam Aspirant" }
    val activeExam = state.activeExam
    val examName = activeExam?.shortName ?: "RRB Nursing"
    val focusSubjectLabel = if (focusSubject.isBlank()) {
        "All Subjects"
    } else {
        "$focusSubject · ${SubjectCatalog.displayNameFor(focusSubject)}"
    }
    val editorial = MaterialTheme.editorialColors

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxHeight()
            .testTag("profile_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Summary Card (Graphite + Acid Lime accent)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenFullProfile)
                    .testTag("drawer_profile_header_btn"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EditorialBannerSurface),
                border = BorderStroke(1.dp, AmoledBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileAvatarCircle(
                            name = profileName,
                            size = 56.dp,
                            avatarPath = avatarPath,
                            modifier = Modifier.testTag("drawer_profile_avatar_circle")
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialBannerTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitleRole,
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialBannerSubtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (account.email.isNotBlank()) {
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EditorialBannerSubtext.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (targetYear.isNotBlank()) {
                                Text(
                                    text = "Target $targetYear",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = AcidLime,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = AmoledBorder)

                    val todayCount = studyGoal.effectiveTodayCount()
                    val activeStreak = studyGoal.effectiveCurrentStreak()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today: $todayCount/${studyGoal.dailyGoalQuestions} Qs",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialBannerTitle
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AcidLime
                        ) {
                            Text(
                                text = "${activeStreak}d streak",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = PureBlack,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View & edit profile",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open full profile",
                            tint = AcidLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Plan Details Card (Live Subscription State)
            val drawerBadgeLabel = when {
                subscription.isAdmin -> "Owner"
                subscription.isSuspended -> "Paused"
                subscription.isExpired -> "Expired"
                subscription.effectiveTier == PlanTier.TRIAL -> "Trial"
                subscription.effectiveTier != PlanTier.FREE -> "Pro"
                else -> "Free"
            }
            val drawerBadgeActive = subscription.isAdmin || subscription.effectiveTier != PlanTier.FREE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenPlans)
                    .testTag("drawer_plan_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = subscription.planDisplayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                subscription.isExpired || subscription.isSuspended -> editorial.wrongBg
                                drawerBadgeActive -> editorial.acidLime
                                else -> editorial.elevatedSurface
                            }
                        ) {
                            Text(
                                text = drawerBadgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    subscription.isExpired || subscription.isSuspended -> editorial.wrongText
                                    drawerBadgeActive -> editorial.onAcidLime
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = subscription.expirySummaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Manage plans, redeem access code or support →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (editorial.isDark) AcidLime else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Active Exam & Subject Details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXAM & SUBJECT",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Exam",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = examName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = onOpenExamPicker,
                                modifier = Modifier.testTag("drawer_switch_exam_btn")
                            ) {
                                Text("Switch", fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = editorial.borderSubtle)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable(onClick = onOpenFullProfile),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Focus Subject",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = focusSubjectLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Change focus subject",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Quick Study Sections
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "STUDY SECTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                SubjectHubSection.entries.forEach { section ->
                    DrawerActionRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = when (section) {
                            SubjectHubSection.PYQ -> "Previous Year Papers"
                            else -> section.labelFor(activeExam?.id)
                        },
                        onClick = { onOpenSubjectSection(section) },
                        testTag = "drawer_section_${section.name}"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(color = editorial.borderSubtle)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DrawerActionRow(
                    icon = Icons.Filled.MedicalServices,
                    label = "Clinical Calculators & Lab Values",
                    onClick = onOpenClinicalTools,
                    testTag = "drawer_clinical_tools_btn"
                )
                DrawerActionRow(
                    icon = Icons.Filled.Notifications,
                    label = if (unreadNotificationsCount > 0) "Notification Center ($unreadNotificationsCount)" else "Notification Center",
                    onClick = onOpenNotifications,
                    testTag = "drawer_notifications_btn"
                )
                DrawerActionRow(
                    icon = Icons.Filled.QuestionAnswer,
                    label = if (unreadDoubtReplies > 0) "Doubt Box ($unreadDoubtReplies new)" else "Doubt Box",
                    onClick = onOpenDoubtBox,
                    testTag = "drawer_doubt_box_btn"
                )
                DrawerActionRow(
                    icon = if (cloudSyncState.status == SyncBadgeState.OFFLINE) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                    label = when (cloudSyncState.status) {
                        SyncBadgeState.SYNCING -> "Syncing to cloud..."
                        SyncBadgeState.SYNCED -> "Cloud Synced · Sync Now"
                        SyncBadgeState.OFFLINE -> "Offline · Tap to Sync"
                        else -> "Sync Cloud Progress"
                    },
                    onClick = onSyncNow,
                    testTag = "drawer_sync_now_btn"
                )
                if (subscription.isAdmin) {
                    DrawerActionRow(
                        icon = Icons.Filled.AddCircleOutline,
                        label = "Import & Backup",
                        onClick = onOpenImporter,
                        testTag = "drawer_importer_btn"
                    )
                }
                DrawerActionRow(
                    icon = Icons.Filled.Settings,
                    label = "Settings & Appearance",
                    onClick = onOpenSettings,
                    testTag = "drawer_settings_btn"
                )
                DrawerActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Sign Out",
                    onClick = onSignOut,
                    testTag = "drawer_sign_out_btn"
                )
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileName: String,
    profileRole: String,
    targetYear: String,
    focusSubject: String,
    phoneNumber: String,
    avatarPath: String = "",
    currentDeviceName: String,
    account: SignedInAccountInfo,
    cloudSyncState: CloudSyncUiState,
    studyGoal: DailyStudyGoalState,
    state: DashboardDerivedState,
    subscription: SubscriptionUiState = SubscriptionUiState(),
    onProfileNameChange: (String) -> Unit,
    onProfileRoleChange: (String) -> Unit,
    onTargetYearChange: (String) -> Unit,
    onFocusSubjectChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPickProfilePhoto: () -> Unit = {},
    onRemoveProfilePhoto: () -> Unit = {},
    onDailyGoalChange: (Int) -> Unit,
    onStartDailyGoalDrill: () -> Unit,
    onOpenPlans: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenDoubtBox: () -> Unit = {},
    onOpenExamPicker: () -> Unit,
    onPracticeSubject: (List<QuestionItem>, String) -> Unit,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeExam = state.activeExam
    val editorial = MaterialTheme.editorialColors
    val displayName = profileName.trim().ifBlank { "ClinEdge Learner" }
    val displayRole = profileRole.trim().ifBlank { "Healthcare Exam Aspirant" }
    val roleSuggestions = listOf("B.Sc. Nursing", "GNM", "Post Basic B.Sc.", "Staff Nurse")
    val yearOptions = listOf("2026", "2027", "2028")

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Details, study goal & account sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onOpenDoubtBox,
                        modifier = Modifier.testTag("profile_open_doubts_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QuestionAnswer,
                            contentDescription = "Doubt Box"
                        )
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("profile_open_notifications_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notification Center"
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("profile_open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }

            // Hero Avatar & Study Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(92.dp)
                                .clickable(onClick = onPickProfilePhoto)
                                .testTag("profile_avatar_picker_trigger")
                        ) {
                            ProfileAvatarCircle(
                                name = profileName,
                                size = 86.dp,
                                avatarPath = avatarPath,
                                modifier = Modifier.testTag("profile_hero_avatar_circle")
                            )
                            Surface(
                                shape = CircleShape,
                                color = editorial.acidLime,
                                border = BorderStroke(2.dp, editorial.cardSurface),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AddAPhoto,
                                        contentDescription = "Add or change profile picture",
                                        tint = editorial.onAcidLime,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onPickProfilePhoto,
                                modifier = Modifier.testTag("profile_pick_photo_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (avatarPath.isBlank()) "Add Photo" else "Change Photo")
                            }
                            if (avatarPath.isNotBlank()) {
                                TextButton(
                                    onClick = onRemoveProfilePhoto,
                                    modifier = Modifier.testTag("profile_remove_photo_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = editorial.wrongText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", color = editorial.wrongText)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append(displayRole)
                                    if (targetYear.isNotBlank()) append("  ·  Target $targetYear")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val activeStreak = studyGoal.effectiveCurrentStreak()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfileStatPill(
                                label = "Practised",
                                value = "${state.overall.attemptedUnique}",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatPill(
                                label = "Accuracy",
                                value = "${state.overall.accuracyPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatPill(
                                label = "Streak",
                                value = "${activeStreak}d",
                                highlight = activeStreak > 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Daily Study Goal & Streak Card
            item {
                val todayCount = studyGoal.effectiveTodayCount()
                val activeStreak = studyGoal.effectiveCurrentStreak()
                val bestStreak = studyGoal.longestStreakDays
                val goalProgress = if (studyGoal.dailyGoalQuestions > 0) {
                    (todayCount.toFloat() / studyGoal.dailyGoalQuestions).coerceIn(0f, 1f)
                } else 0f

                ProfileSectionCard(
                    title = "Daily Study Goal",
                    icon = Icons.Filled.Flag,
                    cardTag = "profile_card_daily_goal"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$todayCount / ${studyGoal.dailyGoalQuestions} questions today",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Streak: ${activeStreak}d  ·  Best: ${bestStreak}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (activeStreak > 0) editorial.acidLime else editorial.elevatedSurface,
                            border = BorderStroke(1.dp, if (activeStreak > 0) editorial.acidLime else editorial.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Study streak",
                                    tint = if (activeStreak > 0) editorial.onAcidLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${activeStreak}d",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeStreak > 0) editorial.onAcidLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    AcidLimeProgressBar(progress = goalProgress, height = 6.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudyGoalLogic.DAILY_GOAL_PRESETS.forEach { preset ->
                            MonochromeFilterPill(
                                label = "$preset Qs/day",
                                selected = studyGoal.dailyGoalQuestions == preset,
                                onClick = { onDailyGoalChange(preset) },
                                modifier = Modifier.testTag("profile_daily_goal_$preset")
                            )
                        }
                    }

                    Button(
                        onClick = onStartDailyGoalDrill,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_start_daily_goal_btn")
                    ) {
                        val rem = studyGoal.remainingQuestions()
                        Text(
                            text = if (rem > 0) "Finish Remaining ($rem Qs)" else "Start Bonus Daily Drill",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Google Account & Cloud Sync
            item {
                ProfileSectionCard(
                    title = "Google Account & Sync",
                    icon = if (cloudSyncState.status == SyncBadgeState.OFFLINE) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                    cardTag = "profile_card_cloud_account"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.email.ifBlank { "Google Account Connected" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cloudSyncState.detailMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeLabel = when (cloudSyncState.status) {
                            SyncBadgeState.SYNCING -> "Syncing"
                            SyncBadgeState.SYNCED -> "Synced"
                            SyncBadgeState.OFFLINE -> "Offline"
                            SyncBadgeState.ERROR -> "Paused"
                            SyncBadgeState.IDLE -> "Ready"
                        }
                        val isSynced = cloudSyncState.status == SyncBadgeState.SYNCED
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSynced) editorial.acidLime else editorial.elevatedSurface,
                            border = BorderStroke(1.dp, if (isSynced) editorial.acidLime else editorial.borderSubtle)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSynced) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_single_device_status")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhonelinkLock,
                                contentDescription = "Single-device lock active",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1-Phone Active Lock · $currentDeviceName",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Signing in on another phone signs this device out.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncNow,
                            enabled = cloudSyncState.status != SyncBadgeState.SYNCING,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("profile_sync_now_btn")
                        ) {
                            if (cloudSyncState.status == SyncBadgeState.SYNCING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("profile_sign_out_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            // Personal Details
            item {
                ProfileSectionCard(
                    title = "Personal Details",
                    icon = Icons.Filled.Person,
                    cardTag = "profile_card_personal"
                ) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { onProfileNameChange(it.take(SettingsPreferences.MAX_PROFILE_NAME_LENGTH)) },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    val phoneInvalid = phoneNumber.isNotBlank() &&
                        !AccountSessionLogic.isPhoneNumberValidOrEmpty(phoneNumber)
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { onPhoneNumberChange(it) },
                        label = { Text("Mobile Number (Optional)") },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                        },
                        isError = phoneInvalid,
                        supportingText = {
                            Text(
                                text = if (phoneInvalid) {
                                    "Enter a valid 10-digit mobile number or leave blank."
                                } else {
                                    "Synced privately to your profile."
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input")
                    )

                    OutlinedTextField(
                        value = profileRole,
                        onValueChange = { onProfileRoleChange(it.take(SettingsPreferences.MAX_PROFILE_ROLE_LENGTH)) },
                        label = { Text("Qualification / Role") },
                        placeholder = { Text("e.g. B.Sc. Nursing, GNM, Staff Nurse") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_role_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roleSuggestions.forEach { suggestion ->
                            MonochromeFilterPill(
                                label = suggestion,
                                selected = profileRole.equals(suggestion, ignoreCase = true),
                                onClick = { onProfileRoleChange(suggestion) }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Target Exam Year",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            yearOptions.forEach { yr ->
                                MonochromeFilterPill(
                                    label = yr,
                                    selected = targetYear == yr,
                                    onClick = { onTargetYearChange(yr) },
                                    modifier = Modifier.testTag("profile_year_$yr")
                                )
                            }
                        }
                    }
                }
            }

            // Plan Details (Live Subscription & Access Code CTA)
            item {
                val planBadge = when {
                    subscription.isAdmin -> "Owner"
                    subscription.isSuspended -> "Paused"
                    subscription.isExpired -> "Expired"
                    subscription.effectiveTier == PlanTier.TRIAL -> "3-Day Trial"
                    subscription.effectiveTier != PlanTier.FREE -> "Pro Active"
                    else -> "Free Tier"
                }
                val isProTier = subscription.isAdmin || subscription.effectiveTier != PlanTier.FREE
                ProfileSectionCard(
                    title = "Plan & Support",
                    icon = Icons.Filled.WorkspacePremium,
                    cardTag = "profile_card_plan"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subscription.planDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subscription.expirySummaryLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (subscription.isExpired || subscription.isSuspended) {
                                    editorial.wrongText
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                subscription.isExpired || subscription.isSuspended -> editorial.wrongBg
                                isProTier -> editorial.acidLime
                                else -> editorial.elevatedSurface
                            }
                        ) {
                            Text(
                                text = planBadge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    subscription.isExpired || subscription.isSuspended -> editorial.wrongText
                                    isProTier -> editorial.onAcidLime
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = editorial.borderSubtle)

                    val included = if (isProTier) {
                        listOf(
                            "Unlimited daily practice across unlocked exam bank(s)",
                            "Full Previous Year Papers (PYQs) & unlimited CBT mocks",
                            "Printable watermarked A4 PDF worksheets & backup/restore",
                            "OTA new question packs synced automatically"
                        )
                    } else {
                        listOf(
                            "Up to ${subscription.dailyFreeLimit} practice questions per day",
                            "1 free sample CBT mock test",
                            "Upgrade for unlimited practice, PYQs, full mocks & PDF export"
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        included.forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isProTier) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(text = benefit, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Button(
                        onClick = onOpenPlans,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProTier) MaterialTheme.colorScheme.primary else editorial.acidLime,
                            contentColor = if (isProTier) MaterialTheme.colorScheme.onPrimary else editorial.onAcidLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_manage_plan_btn")
                    ) {
                        Text(
                            text = if (isProTier) "Manage Plan & Support" else "Upgrade, Start Trial or Redeem Code",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active Exam & Focus Subject
            item {
                ProfileSectionCard(
                    title = "Exam & Focus Subject",
                    icon = Icons.Filled.School,
                    cardTag = "profile_card_subject"
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeExam?.name ?: "RRB Nursing Superintendent & Staff Nurse",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}  ·  ${activeExam?.defaultMockQuestions ?: 100}Q / ${activeExam?.defaultMockMinutes ?: 90}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenExamPicker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier.testTag("profile_change_exam_btn")
                            ) {
                                Text("Change")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Primary Focus Subject",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MonochromeFilterPill(
                                label = "All Subjects",
                                selected = focusSubject.isBlank(),
                                onClick = { onFocusSubjectChange("") },
                                modifier = Modifier.testTag("profile_focus_ALL")
                            )
                            SubjectCatalog.ALL_SUBJS.forEach { subj ->
                                MonochromeFilterPill(
                                    label = subj.code,
                                    selected = focusSubject == subj.code,
                                    onClick = { onFocusSubjectChange(subj.code) },
                                    modifier = Modifier.testTag("profile_focus_${subj.code}")
                                )
                            }
                        }
                    }

                    val targetPool = if (focusSubject.isBlank()) {
                        state.questions
                    } else {
                        state.questions.filter { it.question.subject == focusSubject }
                    }
                    val targetLabel = if (focusSubject.isBlank()) {
                        "Mixed Focus Practice"
                    } else {
                        "${SubjectCatalog.displayNameFor(focusSubject)} Focus"
                    }
                    Button(
                        onClick = { onPracticeSubject(targetPool.shuffled().take(25), targetLabel) },
                        enabled = targetPool.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_start_focus_practice_btn")
                    ) {
                        Text(
                            text = if (focusSubject.isBlank()) {
                                "Start 25Q Mixed Practice"
                            } else {
                                "Practice ${SubjectCatalog.displayNameFor(focusSubject)}"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProfileStatPill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) editorial.acidLime else editorial.elevatedSurface,
        border = BorderStroke(1.dp, if (highlight) editorial.acidLime else editorial.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = if (highlight) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) editorial.onAcidLime.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    cardTag: String,
    content: @Composable () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}
