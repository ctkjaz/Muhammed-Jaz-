package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.backup.BackupFolderSummary
import com.example.data.local.SettingsPreferences
import com.example.data.local.ThemePreference
import com.example.data.model.StudyGoalLogic
import com.example.data.update.AppUpdateStatus
import com.example.data.update.AppUpdateUiState
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MutedCrimsonFill
import com.example.ui.theme.WarmWhite
import com.example.ui.theme.editorialColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    profileName: String,
    themePreference: ThemePreference,
    useTimeLimit: Boolean,
    secondsPerQuestion: Int,
    negativeMarking: Boolean,
    keepScreenOn: Boolean,
    dailyGoalQuestions: Int,
    backupFolderSummary: BackupFolderSummary = BackupFolderSummary(),
    isBackupBusy: Boolean = false,
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onProfileNameChange: (String) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onUseTimeLimitChange: (Boolean) -> Unit,
    onSecondsPerQuestionChange: (Int) -> Unit,
    onNegativeMarkingChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onReopenOnboarding: () -> Unit,
    onChooseBackupFolder: () -> Unit = {},
    onResetBackupFolder: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onRestoreLatestBackup: () -> Unit = {},
    onPickRestoreFile: () -> Unit = {},
    onOpenImporter: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onResetStatistics: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = remember(context) { readAppVersionName(context) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personal preferences, timer, study options & data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Profile
            item {
                SettingsSectionCard(
                    title = "Profile",
                    icon = Icons.Filled.Person,
                    cardTag = "settings_card_profile"
                ) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { input ->
                            onProfileNameChange(input.take(SettingsPreferences.MAX_PROFILE_NAME_LENGTH))
                        },
                        label = { Text("Your name (optional)") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shown on your profile")
                                Text(
                                    text = "${profileName.length}/${SettingsPreferences.MAX_PROFILE_NAME_LENGTH}",
                                    fontFamily = MonoFontFamily
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_profile_name_input")
                    )
                }
            }

            // 2. Appearance
            item {
                SettingsSectionCard(
                    title = "Appearance",
                    icon = Icons.Filled.Palette,
                    cardTag = "settings_card_appearance"
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        val themeOptions = listOf(
                            Triple(ThemePreference.LIGHT, "Light (Default)", "settings_theme_light"),
                            Triple(ThemePreference.DARK, "Dark", "settings_theme_dark"),
                            Triple(ThemePreference.SYSTEM, "Match my phone", "settings_theme_system")
                        )
                        themeOptions.forEach { (pref, label, tag) ->
                            val selected = themePreference == pref
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .selectable(
                                        selected = selected,
                                        onClick = { onThemePreferenceChange(pref) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp)
                                    .testTag(tag),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // 3. Timer
            item {
                val example30Mins = SettingsPreferences.calculateSuggestedMinutes(30, secondsPerQuestion)
                val sliderRange = SettingsPreferences.MIN_SECONDS_PER_QUESTION.toFloat()..
                    SettingsPreferences.MAX_SECONDS_PER_QUESTION.toFloat()
                val sliderSteps = ((SettingsPreferences.MAX_SECONDS_PER_QUESTION -
                    SettingsPreferences.MIN_SECONDS_PER_QUESTION) /
                    SettingsPreferences.SECONDS_PER_QUESTION_STEP) - 1

                SettingsSectionCard(
                    title = "Timer",
                    icon = Icons.Filled.Timer,
                    cardTag = "settings_card_timer"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Use a time limit",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Turn off to run timed practice and custom CBT tests without a countdown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useTimeLimit,
                            onCheckedChange = onUseTimeLimitChange,
                            modifier = Modifier.testTag("settings_timer_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val contentAlpha = if (useTimeLimit) 1f else 0.45f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time per question",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                            )
                            Text(
                                text = "$secondsPerQuestion sec",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                                modifier = Modifier.testTag("settings_seconds_value")
                            )
                        }

                        Slider(
                            value = secondsPerQuestion.toFloat(),
                            onValueChange = { raw ->
                                val step = SettingsPreferences.SECONDS_PER_QUESTION_STEP
                                val snapped = ((raw / step).roundToInt() * step).coerceIn(
                                    SettingsPreferences.MIN_SECONDS_PER_QUESTION,
                                    SettingsPreferences.MAX_SECONDS_PER_QUESTION
                                )
                                onSecondsPerQuestionChange(snapped)
                            },
                            valueRange = sliderRange,
                            steps = sliderSteps,
                            enabled = useTimeLimit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_seconds_slider")
                        )

                        Text(
                            text = if (useTimeLimit) {
                                "30 questions = $example30Mins min"
                            } else {
                                "Timer is off — 30 questions = untimed"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                            modifier = Modifier.testTag("settings_timer_example")
                        )

                        Text(
                            text = "The Full Official 100Q mock always stays fixed at 90 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Marking
            item {
                SettingsSectionCard(
                    title = "Marking",
                    icon = Icons.Filled.CheckCircle,
                    cardTag = "settings_card_marking"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Negative marking in practice",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Deduct 0.33 marks for each wrong answer in practice drills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = negativeMarking,
                            onCheckedChange = onNegativeMarkingChange,
                            modifier = Modifier.testTag("settings_neg_marking_switch")
                        )
                    }

                    Text(
                        text = "CBT mocks always score +1 for correct and -0.33 for wrong answers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Study
            item {
                SettingsSectionCard(
                    title = "Study",
                    icon = Icons.Filled.BrightnessHigh,
                    cardTag = "settings_card_study"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val estDailyMins = SettingsPreferences.calculateSuggestedMinutes(
                            dailyGoalQuestions,
                            secondsPerQuestion
                        ).coerceAtLeast(5)
                        Text(
                            text = "Daily study goal",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Target $dailyGoalQuestions questions/day (~$estDailyMins min) to build your daily study streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudyGoalLogic.DAILY_GOAL_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = dailyGoalQuestions == preset,
                                    onClick = { onDailyGoalChange(preset) },
                                    label = {
                                        Text(
                                            text = "$preset Qs",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily)
                                        )
                                    },
                                    modifier = Modifier.testTag("settings_daily_goal_$preset")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Keep screen on while practising",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Active only during practice and CBT mock sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange,
                            modifier = Modifier.testTag("settings_keep_screen_on_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionRow(
                        icon = Icons.Filled.Tune,
                        title = "Run setup wizard again",
                        subtitle = "Re-pick your exam track, focus subject, and daily goal",
                        onClick = onReopenOnboarding,
                        testTag = "settings_reopen_onboarding_row"
                    )
                }
            }

            // 6. Backup, Restore & Study Data
            item {
                SettingsSectionCard(
                    title = "Backup, Restore & Study Data",
                    icon = Icons.Filled.Storage,
                    cardTag = "settings_card_data"
                ) {
                    // Active Backup Folder Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backup_folder_card")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Backup Directory",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = backupFolderSummary.displayLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onChooseBackupFolder,
                                    modifier = Modifier.testTag("settings_choose_backup_folder_btn")
                                ) {
                                    Text("Choose")
                                }
                            }

                            val folderMeta = when {
                                backupFolderSummary.backupCount > 0 && !backupFolderSummary.latestBackupName.isNullOrBlank() ->
                                    "${backupFolderSummary.backupCount} backup(s) saved • Latest: ${backupFolderSummary.latestBackupName}"
                                else ->
                                    "No backups saved in this folder yet. Backups include student profile, plan, progress & custom MCQs."
                            }
                            Text(
                                text = folderMeta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onCreateBackup,
                                    enabled = !isBackupBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_backup_now_btn")
                                ) {
                                    if (isBackupBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Working...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Backup,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backup", fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (backupFolderSummary.backupCount > 0) {
                                            showRestoreOptionsDialog = true
                                        } else {
                                            onPickRestoreFile()
                                        }
                                    },
                                    enabled = !isBackupBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_restore_backup_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (backupFolderSummary.isCustomFolder) {
                                TextButton(
                                    onClick = onResetBackupFolder,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("settings_reset_backup_folder_btn")
                                ) {
                                    Text("Reset to Default Directory")
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    SettingsActionRow(
                        icon = Icons.Filled.AddCircleOutline,
                        title = "Import questions",
                        subtitle = "Paste shift PDF text or bulk MCQ sets into your bank",
                        onClick = onOpenImporter,
                        testTag = "settings_open_importer_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsActionRow(
                        icon = Icons.Filled.PictureAsPdf,
                        title = "Export PDF worksheet",
                        subtitle = "Generate watermarked A4 study sheets & mock booklets offline",
                        onClick = onOpenPdfExport,
                        testTag = "settings_open_pdf_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsActionRow(
                        icon = Icons.Filled.DeleteOutline,
                        title = "Reset my statistics",
                        subtitle = "Clear attempt counts and mock history. Bookmarks are kept.",
                        tint = MaterialTheme.editorialColors.wrongText,
                        onClick = { showResetStatsDialog = true },
                        testTag = "settings_reset_stats_row"
                    )
                }
            }

            // 7. About & In-App Updates
            item {
                val editorial = MaterialTheme.editorialColors
                SettingsSectionCard(
                    title = "About & Updates",
                    icon = Icons.Filled.Info,
                    cardTag = "settings_card_about"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version $versionName",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings_version_text")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable(
                                enabled = appUpdateState.status != AppUpdateStatus.CHECKING,
                                onClick = {
                                    if (appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                                        onOpenUpdateDialog()
                                    } else {
                                        onCheckForUpdates()
                                    }
                                }
                            )
                            .padding(vertical = 6.dp)
                            .testTag("settings_check_updates_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = if (appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                                editorial.correctText
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                                    "Update available (v${appUpdateState.latestVersion})"
                                } else {
                                    "Check for app updates"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                                    editorial.correctText
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = appUpdateState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (appUpdateState.status == AppUpdateStatus.CHECKING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = "Practice material only. Always check clinical answers against your textbooks and current guidelines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Made by Jaz and Co.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showRestoreOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreOptionsDialog = false },
            title = { Text("Restore Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Restore your student profile, active plan, study progress, and custom questions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!backupFolderSummary.latestBackupName.isNullOrBlank()) {
                        Text(
                            text = "Latest in folder: ${backupFolderSummary.latestBackupName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreOptionsDialog = false
                        onRestoreLatestBackup()
                    },
                    modifier = Modifier.testTag("settings_restore_latest_btn")
                ) {
                    Text("Restore Latest")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreOptionsDialog = false
                        onPickRestoreFile()
                    },
                    modifier = Modifier.testTag("settings_restore_pick_file_btn")
                ) {
                    Text("Choose File...")
                }
            }
        )
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("Reset my statistics?") },
            text = {
                Text(
                    "This clears all question attempt counts, accuracy stats, and CBT mock history. Your bookmarks and imported questions will be kept."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStatsDialog = false
                        onResetStatistics()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedCrimsonFill,
                        contentColor = WarmWhite
                    ),
                    modifier = Modifier.testTag("settings_confirm_reset_stats_btn")
                ) {
                    Text("Reset Statistics")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetStatsDialog = false },
                    modifier = Modifier.testTag("settings_cancel_reset_stats_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
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

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun readAppVersionName(context: Context): String {
    return runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName?.takeIf { it.isNotBlank() } ?: "1.0"
    }.getOrDefault("1.0")
}
