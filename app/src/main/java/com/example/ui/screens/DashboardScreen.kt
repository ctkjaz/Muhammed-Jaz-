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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.IconButton
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.grandtest.CloudGrandTest
import com.example.data.model.DailyFeaturedQuestion
import com.example.data.model.DailyStudyGoalState
import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamCountdownLogic
import com.example.data.model.ExamRoadmapSummary
import com.example.data.model.ExamStage
import com.example.data.model.GamificationState
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QotdCompletionState
import com.example.data.model.QuestionItem
import com.example.data.model.SmartStudyPlanSummary
import com.example.data.model.StudyGoalLogic
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.DailyReminderSettingsState
import com.example.data.notifications.EngagementLogic
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PlanTier
import com.example.data.subscription.SubscriptionUiState
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.CompactGamificationBannerCard
import com.example.ui.components.DailyGoalQuickEditorDialog
import com.example.ui.components.DashboardLoadingSkeleton
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.QuestionOfTheDayCard
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SmartWeakTopicDashboardCard
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
    subscription: SubscriptionUiState = SubscriptionUiState(),
    onOpenPlans: () -> Unit = {},
    urgentBanner: CloudAnnouncement? = null,
    unreadDoubtReplies: Int = 0,
    dailyReminder: DailyReminderSettingsState = DailyReminderSettingsState(),
    onDismissUrgentBanner: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenDoubtBox: () -> Unit = {},
    onAskQuickDoubt: () -> Unit = {},
    examRoadmap: ExamRoadmapSummary = ExamRoadmapSummary(),
    srsDueCount: Int = 0,
    onSetTargetExamDate: (String) -> Unit = {},
    onOpenClinicalTools: () -> Unit = {},
    onStartSrsDueDrill: () -> Unit = {},
    grandTestEnabled: Boolean = false,
    liveGrandTest: CloudGrandTest? = null,
    onOpenGrandTestHub: () -> Unit = {},
    smartStudyPlan: SmartStudyPlanSummary? = null,
    onStartSmartWeakDrill: () -> Unit = {},
    onStartRecoveryDayDrill: (String, Int) -> Unit = { _, _ -> },
    onStartImageSpotterDrill: () -> Unit = {},
    onStartGeneralMixedDrill: ((List<QuestionItem>, Int, String) -> Unit)? = null,
    onOpenFlashcardStudio: () -> Unit = {},
    masteredFlashcardCount: Int = 0,
    qotdQuestion: DailyFeaturedQuestion? = null,
    qotdCompletion: QotdCompletionState = QotdCompletionState(),
    onAnswerQotd: (Int) -> Unit = {},
    onUpdateDailyGoal: ((Int) -> Unit)? = null,
    gamificationState: GamificationState = GamificationState(),
    onOpenAbgInterpreter: () -> Unit = {},
    onOpenEcgStudio: () -> Unit = {},
    onOpenSpecialtyTracks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!state.isLoaded) {
        DashboardLoadingSkeleton(modifier = modifier)
        return
    }
    var showExamDateDialog by remember { mutableStateOf(false) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    val todayDateKey = remember { StudyGoalLogic.todayDateKey() }
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
    val imageSpotterCount = remember(stagePool, state.crossExamQuestions) {
        val activeCount = stagePool.count { it.question.isImageBased }
        if (activeCount > 0) activeCount else state.crossExamQuestions.count { it.question.isImageBased }
    }
    val diffLabelSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
    val activeExam = state.activeExam
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExam?.id)
    val isPureNursing = PlatformCatalogDefaults.examIsPureNursing(activeExam?.id)
    val examShortName = activeExam?.shortName ?: "RRB Nursing"
    val fullQ = activeExam?.defaultMockQuestions ?: 100
    val fullMins = activeExam?.defaultMockMinutes ?: 90
    val nurPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExam?.id)
    val nonPct = 100 - nurPct
    val markLabel = activeExam?.let { PlatformCatalogDefaults.formatMarkingLabel(it) } ?: "+1 / -0.33"

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
        isNorcet && state.activeExamStage == ExamStage.MAINS -> "160Q · 180m · 4×45m Clinical · $markLabel"
        isNorcet -> "100Q · 90m · 5×18m (80:20) · $markLabel"
        isPureNursing -> "${fullQ}Q · ${fullMins}m · 100% Core Nursing · $markLabel"
        else -> "${fullQ}Q · ${fullMins}m · ${nurPct}:${nonPct} Pattern · $markLabel"
    }

    val mockCtaLabel = when {
        isNorcet && state.activeExamStage == ExamStage.MAINS -> "160Q Mains"
        isNorcet -> "100Q Prelims"
        else -> "${fullQ}Q Mock"
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val planChipLabel = when {
                            subscription.isAdmin -> "OWNER"
                            subscription.isSuspended -> "PAUSED"
                            subscription.isExpired -> "EXPIRED"
                            subscription.effectiveTier == PlanTier.TRIAL -> "TRIAL ${subscription.daysRemaining}d"
                            subscription.effectiveTier != PlanTier.FREE -> "PRO ${subscription.daysRemaining}d"
                            else -> "FREE ${subscription.dailyFreeUsed}/${subscription.dailyFreeLimit}"
                        }
                        val isProChip = subscription.isAdmin || subscription.effectiveTier != PlanTier.FREE
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                subscription.isExpired || subscription.isSuspended -> editorial.wrongBg
                                isProChip -> MaterialTheme.colorScheme.primary
                                else -> editorial.elevatedSurface
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    subscription.isExpired || subscription.isSuspended -> editorial.wrongText.copy(alpha = 0.45f)
                                    isProChip -> MaterialTheme.colorScheme.primary
                                    else -> editorial.borderSubtle
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenPlans)
                                .testTag("dashboard_plan_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkspacePremium,
                                    contentDescription = "Subscription plan",
                                    tint = when {
                                        subscription.isExpired || subscription.isSuspended -> editorial.wrongText
                                        isProChip -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = planChipLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        subscription.isExpired || subscription.isSuspended -> editorial.wrongText
                                        isProChip -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
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
            }

            // 1a. Live Weekly All-India Grand Test Banner — DORMANT BY DEFAULT (only when Admin turns it ON)
            if (grandTestEnabled && liveGrandTest != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.acidLime.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.7f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenGrandTestHub)
                            .testTag("dashboard_grand_test_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = editorial.acidLime,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "LIVE · ${liveGrandTest.weekLabel.uppercase(Locale.US)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = editorial.acidLime
                                    )
                                    Text(
                                        text = liveGrandTest.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${EntitlementEvaluator.shortExamLabel(liveGrandTest.examId)} • ${liveGrandTest.questionCount}Q • All-India Percentile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenGrandTestHub,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = editorial.acidLime,
                                    contentColor = editorial.onAcidLime
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Join", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1b. Expired / Suspended Auto-Fallback Banner (zero data loss)
            if (subscription.isExpired || subscription.isSuspended) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.elevatedSurface),
                        border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_expired_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (subscription.isSuspended) {
                                        "Premium Paused · Free Tier Active"
                                    } else {
                                        "Plan Expired · Free Tier Active"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = editorial.wrongText
                                )
                                Text(
                                    text = "All your progress & bookmarks are saved. Redeem a renewal code to resume.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onOpenPlans,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = editorial.acidLime,
                                    contentColor = editorial.onAcidLime
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("dashboard_renew_plan_btn")
                            ) {
                                Text("Renew", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1c. Dismissible Urgent Exam Alert Banner (pushed live from Admin Console)
            if (urgentBanner != null) {
                item(key = "urgent_alert_banner_${urgentBanner.announcementId}") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.5.dp, editorial.acidLime),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenNotifications)
                            .testTag("dashboard_urgent_alert_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = editorial.acidLime
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = "Urgent alert",
                                    tint = editorial.onAcidLime,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${urgentBanner.categoryEnum.label.uppercase(Locale.US)} · URGENT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = if (editorial.isDark) editorial.acidLime else editorial.wrongText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = urgentBanner.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = urgentBanner.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDismissUrgentBanner(urgentBanner.announcementId) },
                                modifier = Modifier.testTag("dashboard_dismiss_urgent_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss urgent alert",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                                onClick = {
                                    if (onUpdateDailyGoal != null) {
                                        showDailyGoalDialog = true
                                    } else {
                                        onAdjustDailyGoal()
                                    }
                                },
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

            // 2.0a Compact Gamification Progress Banner (XP, Rank, Shield & Next Weekly Challenge)
            item(key = "dashboard_gamification_overview") {
                CompactGamificationBannerCard(
                    gamification = gamificationState,
                    studyGoal = studyGoal,
                    onOpenHub = { onSelectTab(MainTab.STATS) }
                )
            }

            // 2.0 Question of the Day (Curated High-Yield Clinical MCQ with Full Per-Option Rationales)
            if (qotdQuestion != null) {
                item(key = "dashboard_qotd") {
                    QuestionOfTheDayCard(
                        question = qotdQuestion,
                        completion = qotdCompletion,
                        todayDateKey = todayDateKey,
                        onSelectOption = onAnswerQotd
                    )
                }
            }

            // 2a. Target Exam Countdown & Study Roadmap + Clinical Utility Strip
            item(key = "dashboard_countdown_and_tools") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        onClick = { showExamDateDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(
                            1.dp,
                            if (examRoadmap.isConfigured && examRoadmap.daysRemaining <= 14) editorial.acidLime else editorial.borderSubtle
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_exam_countdown_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (examRoadmap.isConfigured) {
                                                "$examShortName Countdown · ${examRoadmap.formattedDate}"
                                            } else {
                                                "Target Exam Countdown & Roadmap"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = examRoadmap.phaseTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (examRoadmap.isConfigured) editorial.acidLime else editorial.elevatedSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (examRoadmap.isConfigured) editorial.acidLime else editorial.borderSubtle
                                    )
                                ) {
                                    Text(
                                        text = if (examRoadmap.isConfigured) {
                                            if (examRoadmap.isExamDayOrPassed) "EXAM DAY" else "${examRoadmap.daysRemaining}d LEFT"
                                        } else {
                                            "SET DATE"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = if (examRoadmap.isConfigured) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = examRoadmap.phaseGuidance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (examRoadmap.isConfigured) {
                                Text(
                                    text = "Suggested pace: ${examRoadmap.recommendedDailyQs} MCQs/day · ${examRoadmap.unattemptedCount} unseen in bank",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            onClick = onOpenClinicalTools,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_clinical_tools_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = null,
                                    tint = editorial.acidLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Clinical Tools",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "6 Calcs · ABG · Labs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            onClick = {
                                if (srsDueCount > 0) onStartSrsDueDrill() else onSelectTab(MainTab.DECK)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (srsDueCount > 0) editorial.acidLime.copy(alpha = 0.14f) else editorial.cardSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (srsDueCount > 0) editorial.acidLime.copy(alpha = 0.65f) else editorial.borderSubtle
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_srs_due_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = if (srsDueCount > 0) editorial.acidLime else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (srsDueCount > 0) "SRS Due ($srsDueCount)" else "SRS Notebook",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (srsDueCount > 0) "Tap to drill now" else "1d · 3d · 7d · 14d",
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
                        Card(
                            onClick = onOpenAbgInterpreter,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_abg_interpreter_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Biotech,
                                    contentDescription = null,
                                    tint = editorial.acidLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ABG Interpreter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Winter's · 12 Cases",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            onClick = onOpenEcgStudio,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_ecg_studio_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MonitorHeart,
                                    contentDescription = null,
                                    tint = editorial.acidLime,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ECG Studio",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "14 ICU Strips · Zoom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2b. Student Doubt Box & Reminder Status Strip
            item(key = "dashboard_doubt_box_card") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(
                        width = if (unreadDoubtReplies > 0) 1.5.dp else 1.dp,
                        color = if (unreadDoubtReplies > 0) editorial.acidLime else editorial.borderSubtle
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_doubt_box_card")
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QuestionAnswer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Doubt Box",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (unreadDoubtReplies > 0) {
                                            "$unreadDoubtReplies new faculty ${if (unreadDoubtReplies == 1) "reply" else "replies"} waiting!"
                                        } else {
                                            "Ask nursing & clinical doubts directly to faculty"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (unreadDoubtReplies > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = editorial.acidLime
                                ) {
                                    Text(
                                        text = "$unreadDoubtReplies NEW",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = editorial.onAcidLime,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else if (dailyReminder.enabled) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = editorial.elevatedSurface,
                                    border = BorderStroke(1.dp, editorial.borderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onAdjustDailyGoal)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = "Daily reminder active",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = EngagementLogic.formatReminderTime(dailyReminder.hour, dailyReminder.minute),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAskQuickDoubt,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = editorial.acidLime,
                                    contentColor = editorial.onAcidLime
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("dashboard_ask_doubt_btn")
                            ) {
                                Text("Ask a Doubt", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenDoubtBox,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("dashboard_open_doubt_box_btn")
                            ) {
                                Text("My Doubts", fontWeight = FontWeight.SemiBold)
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
                                            ExamStage.PRELIMS -> "Stage I · Prelims"
                                            ExamStage.MAINS -> "Stage II · Mains"
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
                                        "${state.activeExamStage.shortLabel} progress"
                                    } else {
                                        "Bank progress"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$solvedCount practised · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}",
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
                            meta = "$examShortName · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}",
                            icon = Icons.Filled.School,
                            onClick = { onOpenSubjectSection(SubjectHubSection.NURSING) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pathway_by_exam_card")
                        )
                        PathwayCard(
                            title = "By Subject",
                            meta = "All ${state.availableExams.size} Exams · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}",
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

            // 5b. Smart Weak-Topic Auto-Drill & 7-Day Recovery Plan
            if (smartStudyPlan != null && smartStudyPlan.diagnostics.isNotEmpty()) {
                item {
                    SmartWeakTopicDashboardCard(
                        plan = smartStudyPlan,
                        onStartSmartAutoDrill = onStartSmartWeakDrill,
                        onStartSubjectRecoveryDrill = onStartRecoveryDayDrill
                    )
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
                                    val title = "25Q Mix (+IBQ)$diffLabelSuffix"
                                    if (onStartGeneralMixedDrill != null) {
                                        onStartGeneralMixedDrill(diffFiltered, 25, title)
                                    } else {
                                        onStartDrill(diffFiltered.shuffled().take(25), title)
                                    }
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
                            if (imageSpotterCount > 0) {
                                OutlinedButton(
                                    onClick = onStartImageSpotterDrill,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.75f)),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("dashboard_image_spotters_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = if (editorial.isDark) editorial.acidLime else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "IBQ Hub ($imageSpotterCount)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Compact Navigation Shortcuts (Spotters, PYQs & CBT)
            item {
                val pyqCount = stagePool.count { it.question.isPyqTagged }
                val shortcuts = buildList {
                    add(
                        Triple(
                            "Specialty Tracks",
                            "Cardiology · ICU · OBG · Peds",
                            Icons.Filled.LocalHospital
                        ) to onOpenSpecialtyTracks
                    )
                    if (imageSpotterCount > 0) {
                        add(
                            Triple(
                                "Image-Based Hub",
                                "$imageSpotterCount ECG, CTG, CXR & OT",
                                Icons.Filled.Image
                            ) to onStartImageSpotterDrill
                        )
                    }
                    if (pyqCount > 0) {
                        add(
                            Triple(
                                "Previous Year Papers",
                                "Paper-Wise Sets",
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
                    val totalRapidCards = com.example.data.model.RapidRecallCatalog.BUILTIN_CLINICAL_CARDS.size + state.flashcards.size
                    add(
                        Triple(
                            "Rapid Recall Flashcards",
                            if (masteredFlashcardCount > 0) "$masteredFlashcardCount/$totalRapidCards mastered" else "$totalRapidCards flip cards",
                            Icons.Filled.Tune
                        ) to onOpenFlashcardStudio
                    )
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

    if (showDailyGoalDialog && onUpdateDailyGoal != null) {
        DailyGoalQuickEditorDialog(
            currentGoal = targetGoal,
            onSaveGoal = { updatedTarget ->
                onUpdateDailyGoal(updatedTarget)
                showDailyGoalDialog = false
            },
            onDismiss = { showDailyGoalDialog = false }
        )
    }

    if (showExamDateDialog) {
        var dateInput by remember(examRoadmap.targetDateIso) {
            mutableStateOf(examRoadmap.targetDateIso.ifBlank { ExamCountdownLogic.isoDateAfterDays(60) })
        }
        var dateError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showExamDateDialog = false },
            title = {
                Text(
                    text = "Set Target Exam Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a quick countdown preset or enter your exact exam date (YYYY-MM-DD) to personalize your study roadmap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30 to "In 30d", 60 to "In 60d", 90 to "In 90d", 120 to "In 120d").forEach { (days, label) ->
                            val presetIso = ExamCountdownLogic.isoDateAfterDays(days)
                            OutlinedButton(
                                onClick = {
                                    dateInput = presetIso
                                    dateError = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = {
                            dateInput = it.take(10)
                            dateError = null
                        },
                        label = { Text("Exam Date (YYYY-MM-DD)") },
                        placeholder = { Text("2026-09-15") },
                        singleLine = true,
                        isError = dateError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exam_countdown_date_input")
                    )
                    if (dateError != null) {
                        Text(
                            text = dateError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = editorial.wrongText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = dateInput.trim()
                        if (ExamCountdownLogic.daysUntil(trimmed) == null) {
                            dateError = "Use valid YYYY-MM-DD format (e.g., ${ExamCountdownLogic.isoDateAfterDays(45)})"
                        } else {
                            onSetTargetExamDate(trimmed)
                            showExamDateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier.testTag("exam_countdown_save_btn")
                ) {
                    Text("Save Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (examRoadmap.isConfigured) {
                        TextButton(
                            onClick = {
                                onSetTargetExamDate("")
                                showExamDateDialog = false
                            }
                        ) {
                            Text("Clear", color = editorial.wrongText)
                        }
                    }
                    TextButton(onClick = { showExamDateDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
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
