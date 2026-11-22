package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyStudyGoalState
import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionItem
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SubjectHubSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: DashboardDerivedState,
    profileName: String,
    studyGoal: DailyStudyGoalState,
    difficultyFilter: DifficultyFilter,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onSelectExamStage: (ExamStage) -> Unit,
    onOpenExamPicker: () -> Unit,
    onLaunchFullMock: () -> Unit,
    onOpenTimedPractice: () -> Unit,
    onStartDailyGoalDrill: () -> Unit,
    onAdjustDailyGoal: () -> Unit,
    onOpenSubjectSection: (SubjectHubSection) -> Unit,
    onOpenSubjectPracticeMode: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onStartDrill: (List<QuestionItem>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val stagePool = state.stageQuestions.ifEmpty { state.questions }
    val diffFiltered = remember(stagePool, difficultyFilter) {
        difficultyFilter.difficulty?.let { d ->
            stagePool.filter { it.question.difficulty == d }
        } ?: stagePool
    }
    val filteredMistakes = remember(diffFiltered) { diffFiltered.filter { it.lastOutcome == "wrong" } }
    val filteredLowest = remember(state.lowestAccuracyQuestions, difficultyFilter) {
        difficultyFilter.difficulty?.let { d ->
            state.lowestAccuracyQuestions.filter { it.question.difficulty == d }
        } ?: state.lowestAccuracyQuestions
    }
    val diffLabelSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
    val activeExam = state.activeExam
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExam?.id)
    val isPgimer = PlatformCatalogDefaults.isPgimerExam(activeExam?.id)
    val examShortName = activeExam?.shortName ?: "RRB Nursing"

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val todayDateText = remember {
        SimpleDateFormat("EEE, dd MMM", Locale.US).format(Date())
    }
    val firstName = profileName.trim().substringBefore(" ").takeIf { it.isNotBlank() }

    val todayCount = studyGoal.effectiveTodayCount()
    val targetGoal = studyGoal.dailyGoalQuestions
    val remaining = studyGoal.remainingQuestions()
    val goalMet = studyGoal.isGoalMetToday()
    val goalProgress = studyGoal.progressFraction()
    val activeStreak = studyGoal.effectiveCurrentStreak()

    val stagePatternMeta = when {
        isPgimer -> "100Q · 100m · 100% Core Nursing · -0.25"
        isNorcet && state.activeExamStage == ExamStage.MAINS -> "160Q · 180m · 4×45m Clinical · -0.33"
        isNorcet -> "100Q · 90m · 5×18m (80:20) · -0.33"
        else -> "100Q · 90m · 70:30 Pattern · -0.33"
    }

    val mockCtaLabel = when {
        isPgimer -> "100Q Mock"
        isNorcet && state.activeExamStage == ExamStage.MAINS -> "160Q Mains"
        isNorcet -> "100Q Prelims"
        else -> "100Q Mock"
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 980.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clean Greeting & Date Context
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = todayDateText.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (firstName != null) "$greeting, $firstName" else greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (activeStreak > 0) editorial.acidLime else editorial.elevatedSurface,
                        border = BorderStroke(
                            1.dp,
                            if (activeStreak > 0) editorial.acidLime else editorial.borderSubtle
                        ),
                        modifier = Modifier.testTag("dashboard_streak_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                                color = if (activeStreak > 0) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Daily Progress Card (Acid Lime Progress Accent)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_daily_goal_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (goalMet) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Goal complete",
                                        tint = editorial.correctText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = if (goalMet) "Daily goal complete" else "Daily progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$todayCount / $targetGoal Qs",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (goalMet) editorial.correctText else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        AcidLimeProgressBar(
                            progress = goalProgress,
                            height = 8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onStartDailyGoalDrill,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("dashboard_daily_goal_btn")
                            ) {
                                Text(
                                    text = if (remaining > 0) "Continue ($remaining left)" else "Bonus ${targetGoal}Q",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = onAdjustDailyGoal,
                                modifier = Modifier.testTag("dashboard_adjust_goal_btn")
                            ) {
                                Text(
                                    text = "Edit goal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Statistics Row (Real Live Data Only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactStatCard(
                        label = "Practised",
                        value = "${state.overall.attemptedUnique}",
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        label = "Accuracy",
                        value = "${state.overall.accuracyPercent}%",
                        highlight = state.overall.accuracyPercent >= 75,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        label = "Saved",
                        value = "${state.overall.bookmarkCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Prominent Active Exam Card (With Separate Prelims / Mains for NORCET)
            item {
                val solvedCount = state.overall.attemptedUnique
                val bankSize = stagePool.size
                val bankProgress = if (bankSize > 0) solvedCount.toFloat() / bankSize else 0f

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_active_exam_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ACTIVE EXAM",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = examShortName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stagePatternMeta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenExamPicker,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("dashboard_switch_exam_btn")
                            ) {
                                Text("Switch", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (state.activeExamSupportsStages) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SegmentedPillBar(
                                    options = ExamStage.entries,
                                    selected = state.activeExamStage,
                                    labelOf = { stage ->
                                        when (stage) {
                                            ExamStage.PRELIMS -> "Prelims (${state.prelimsQuestionCount})"
                                            ExamStage.MAINS -> "Mains (${state.mainsQuestionCount})"
                                        }
                                    },
                                    onSelect = onSelectExamStage,
                                    testTagOf = { "dash_stage_${it.id}" }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.activeExamSupportsStages) {
                                        "${state.activeExamStage.shortLabel} coverage"
                                    } else {
                                        "Bank coverage"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$solvedCount / $bankSize",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            AcidLimeProgressBar(progress = bankProgress, height = 5.dp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val fresh = diffFiltered.filter { it.attemptsCount == 0 }.shuffled()
                                    val fallback = diffFiltered.shuffled()
                                    val picked = (fresh + fallback).distinctBy { it.id }.take(25)
                                    val stageTag = if (state.activeExamSupportsStages) " ${state.activeExamStage.shortLabel}" else ""
                                    onStartDrill(picked, "$examShortName$stageTag Practice$diffLabelSuffix")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dashboard_continue_practising_btn")
                            ) {
                                Text(
                                    text = "Continue Practising",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onLaunchFullMock,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .heightIn(min = 48.dp)
                                    .testTag("launch_100q_mock_btn")
                            ) {
                                Text(
                                    text = mockCtaLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. Clear Practice Pathways: By Exam vs By Subject (Req 16)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Practice Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PathwayCard(
                            title = "By Exam",
                            meta = "$examShortName · ${stagePool.size} Qs",
                            icon = Icons.Filled.School,
                            onClick = { onOpenSubjectSection(SubjectHubSection.NURSING) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pathway_by_exam_card")
                        )
                        PathwayCard(
                            title = "By Subject",
                            meta = "All Exams · ${state.crossExamQuestions.size} Qs",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentDot = true,
                            onClick = onOpenSubjectPracticeMode,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pathway_by_subject_card")
                        )
                    }
                }
            }

            // 6. Focused Quick Actions & Difficulty Filter
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Drills",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onOpenTimedPractice,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_timed_practice_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Timed", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        DifficultyFilterBar(
                            selected = difficultyFilter,
                            onSelect = onSelectDifficulty,
                            testTagPrefix = "dash_diff"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onStartDrill(filteredMistakes, "Mistakes Review$diffLabelSuffix") },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("drill_mistakes_btn")
                            ) {
                                Text(
                                    text = "Mistakes (${filteredMistakes.size})",
                                    color = if (filteredMistakes.isNotEmpty()) editorial.wrongText else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = { onStartDrill(filteredLowest.take(25), "Weakest Questions$diffLabelSuffix") },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("drill_lowest_acc_btn")
                            ) {
                                Text("Weakest (${filteredLowest.size})")
                            }
                            Button(
                                onClick = {
                                    onStartDrill(
                                        diffFiltered.shuffled().take(25),
                                        "25Q Random Drill$diffLabelSuffix"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("drill_25q_random_btn")
                            ) {
                                Text("25Q Mix", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. Compact Navigation Shortcuts (PYQs & CBT)
            item {
                val pyqCount = stagePool.count { it.question.isPyqTagged }
                val shortcuts = buildList {
                    if (pyqCount > 0) {
                        add(
                            Triple(
                                "Previous Year Papers",
                                "$pyqCount Qs",
                                Icons.Filled.History
                            ) to { onOpenSubjectSection(SubjectHubSection.PYQ) }
                        )
                    }
                    add(
                        Triple(
                            "CBT Mock Tests",
                            if (isNorcet) "Prelims & Mains" else "Timed Mocks",
                            Icons.Filled.Quiz
                        ) to { onSelectTab(MainTab.CBT) }
                    )
                    if (!isPgimer && (!isNorcet || state.activeExamStage == ExamStage.PRELIMS)) {
                        add(
                            Triple(
                                "Fact Flashcards",
                                "${state.flashcards.size} cards",
                                Icons.Filled.Tune
                            ) to { onOpenSubjectSection(SubjectHubSection.CA_GK) }
                        )
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        shortcuts.forEach { (meta, action) ->
                            CompactShortcutRow(
                                title = meta.first,
                                trailing = meta.second,
                                icon = meta.third,
                                onClick = action,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shortcuts.forEach { (meta, action) ->
                            CompactShortcutRow(
                                title = meta.first,
                                trailing = meta.second,
                                icon = meta.third,
                                onClick = action,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatCard(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = when {
                    highlight && editorial.isDark -> editorial.acidLime
                    highlight -> editorial.correctText
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PathwayCard(
    title: String,
    meta: String,
    icon: ImageVector,
    accentDot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (accentDot) editorial.acidLime else editorial.elevatedSurface
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (accentDot) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactShortcutRow(
    title: String,
    trailing: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
