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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsPreferences
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.components.ProfileAvatarCircle
import com.example.ui.components.formatApproxQuestionCount
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.SubjectHubSection

@Composable
fun ProfileDrawerContent(
    profileName: String,
    profileRole: String,
    targetYear: String,
    focusSubject: String,
    state: DashboardDerivedState,
    onOpenFullProfile: () -> Unit,
    onOpenExamPicker: () -> Unit,
    onOpenSubjectSection: (SubjectHubSection) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImporter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = profileName.trim().ifBlank { "ClinEdge Learner" }
    val subtitleRole = profileRole.trim().ifBlank { "Healthcare Exam Aspirant" }
    val activeExam = state.activeExam
    val examName = activeExam?.shortName ?: "RRB Nursing"
    val focusSubjectLabel = if (focusSubject.isBlank()) {
        "All Subjects"
    } else {
        "$focusSubject • ${SubjectCatalog.displayNameFor(focusSubject)}"
    }
    val editorial = MaterialTheme.editorialColors

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxHeight()
            .testTag("profile_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tappable Profile Picture Circle + Personal Summary -> Opens Full Profile Page
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenFullProfile)
                    .testTag("drawer_profile_header_btn"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
                            size = 58.dp,
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitleRole,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (targetYear.isNotBlank()) {
                                Text(
                                    text = "Target $targetYear",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View & edit full profile",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open full profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Plan Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenFullProfile)
                    .testTag("drawer_plan_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ClinEdge Free",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = editorial.correctText.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = editorial.correctText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${formatApproxQuestionCount(state.overall.totalQuestions, verbose = true)}, CBT mocks, analytics & PDF worksheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active Exam & Subject Details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXAM & SUBJECT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                Text("Switch")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
                    style = MaterialTheme.typography.labelSmall,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DrawerActionRow(
                    icon = Icons.Filled.AddCircleOutline,
                    label = "Import & Backup",
                    onClick = onOpenImporter,
                    testTag = "drawer_importer_btn"
                )
                DrawerActionRow(
                    icon = Icons.Filled.Settings,
                    label = "Settings & Appearance",
                    onClick = onOpenSettings,
                    testTag = "drawer_settings_btn"
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
            tint = MaterialTheme.colorScheme.primary,
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
    state: DashboardDerivedState,
    onProfileNameChange: (String) -> Unit,
    onProfileRoleChange: (String) -> Unit,
    onTargetYearChange: (String) -> Unit,
    onFocusSubjectChange: (String) -> Unit,
    onOpenExamPicker: () -> Unit,
    onPracticeSubject: (List<QuestionItem>, String) -> Unit,
    onOpenSettings: () -> Unit,
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
                            text = "Personal details, plan & exam preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileAvatarCircle(
                            name = profileName,
                            size = 78.dp,
                            modifier = Modifier.testTag("profile_hero_avatar_circle")
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append(displayRole)
                                    if (targetYear.isNotBlank()) append("  •  Target $targetYear")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

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
                                label = "Saved",
                                value = "${state.overall.bookmarkCount}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 1. Personal Details
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roleSuggestions.forEach { suggestion ->
                            FilterChip(
                                selected = profileRole.equals(suggestion, ignoreCase = true),
                                onClick = { onProfileRoleChange(suggestion) },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Target Exam Year",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            yearOptions.forEach { yr ->
                                FilterChip(
                                    selected = targetYear == yr,
                                    onClick = { onTargetYearChange(yr) },
                                    label = {
                                        Text(
                                            text = yr,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily)
                                        )
                                    },
                                    modifier = Modifier.testTag("profile_year_$yr")
                                )
                            }
                        }
                    }
                }
            }

            // 2. Plan Details
            item {
                ProfileSectionCard(
                    title = "Plan Details",
                    icon = Icons.Filled.WorkspacePremium,
                    cardTag = "profile_card_plan"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ClinEdge Free",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All core study tools included",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = editorial.correctText.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, editorial.correctText.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = editorial.correctText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    val included = listOf(
                        "${formatApproxQuestionCount(state.overall.totalQuestions, verbose = true)} with rationales",
                        "Full-length CBT mocks & custom timed drills",
                        "Subject accuracy analytics & weak-area revision",
                        "Printable A4 PDF study worksheets & JSON backup"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        included.forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = editorial.correctText,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // 3. Active Exam & Focus Subject
            item {
                ProfileSectionCard(
                    title = "Exam & Focus Subject",
                    icon = Icons.Filled.School,
                    cardTag = "profile_card_subject"
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                    text = "${formatApproxQuestionCount(state.overall.totalQuestions)}  •  ${activeExam?.defaultMockQuestions ?: 100}Q / ${activeExam?.defaultMockMinutes ?: 90}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenExamPicker,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("profile_change_exam_btn")
                            ) {
                                Text("Change")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Primary Focus Subject",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = focusSubject.isBlank(),
                                onClick = { onFocusSubjectChange("") },
                                label = { Text("All Subjects") },
                                modifier = Modifier.testTag("profile_focus_ALL")
                            )
                            SubjectCatalog.ALL_SUBJS.forEach { subj ->
                                FilterChip(
                                    selected = focusSubject == subj.code,
                                    onClick = { onFocusSubjectChange(subj.code) },
                                    label = { Text(subj.code) },
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
