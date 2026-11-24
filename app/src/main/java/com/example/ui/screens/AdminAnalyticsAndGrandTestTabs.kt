package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.grandtest.AdminAnalyticsCalculator
import com.example.data.grandtest.CloudGrandTest
import com.example.data.grandtest.GrandTestLeaderboardSummary
import com.example.data.grandtest.GrandTestStatus
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDoubt
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.EntitlementEvaluator
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.AdminConsoleUiState

@Composable
fun AdminAnalyticsTabContent(
    state: AdminConsoleUiState,
    config: CloudSubscriptionConfig,
    announcements: List<CloudAnnouncement>,
    doubts: List<CloudDoubt>,
    onToggleGrandTestEnabled: (Boolean) -> Unit,
    onOpenGrandTestTab: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val snapshot = remember(
        state.users,
        state.paymentRequests,
        state.accessCodes,
        state.questionReports,
        state.questionPacks,
        doubts,
        announcements,
        state.grandTests,
        config.grandTestEnabled
    ) {
        AdminAnalyticsCalculator.compute(
            users = state.users,
            paymentRequests = state.paymentRequests,
            accessCodes = state.accessCodes,
            questionReports = state.questionReports,
            questionPacks = state.questionPacks,
            doubts = doubts,
            announcements = announcements,
            grandTests = state.grandTests,
            grandTestEnabled = config.grandTestEnabled
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Live Executive Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real-time metrics computed from synced Firestore users, UPI payments, doubts, OTA packs, and reports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // KPI Grid — Row 1: Users & Paid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "TOTAL LEARNERS",
                    value = snapshot.totalUsers.toString(),
                    subtitle = "All-Exam ${snapshot.allExamPaidUsers} • Single ${snapshot.singleExamPaidUsers}",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "ACTIVE PAID PRO",
                    value = snapshot.activePaidUsers.toString(),
                    subtitle = "${snapshot.overallPaidPercent}% of total users",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid — Row 2: UPI Revenue & Pipeline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "APPROVED UPI REV",
                    value = "₹${snapshot.approvedRevenueInr}",
                    subtitle = "${snapshot.approvedPaymentsCount} verified UTRs",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "PENDING UPI",
                    value = "₹${snapshot.pendingRevenueInr}",
                    subtitle = "${snapshot.pendingPaymentsCount} awaiting approval",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid — Row 3: Trial Conversion & Support SLA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "TRIAL → PAID CONV",
                    value = "${snapshot.trialToPaidConversionPercent}%",
                    subtitle = "${snapshot.activeTrialUsers} active 3-day trials",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "DOUBT SLA",
                    value = "${snapshot.doubtResolutionPercent}%",
                    subtitle = "${snapshot.openDoubts} open • ${snapshot.resolvedDoubts} answered",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Grand Test Kill-Switch Quick Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (config.grandTestEnabled) AcidLime else editorial.borderSubtle
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (config.grandTestEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (config.grandTestEnabled) AcidLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Weekly All-India Grand Test",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (config.grandTestEnabled) {
                                    "LIVE • Visible to students (${snapshot.liveGrandTests} live / ${snapshot.totalGrandTests} total)"
                                } else {
                                    "DORMANT • Hidden from students until user base grows"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.grandTestEnabled,
                            onCheckedChange = onToggleGrandTestEnabled,
                            enabled = !state.isMutating,
                            modifier = Modifier.testTag("analytics_grand_test_toggle")
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenGrandTestTab,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Manage Weekly Grand Tests & Leaderboards")
                    }
                }
            }
        }

        // Subscription Tier Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Subscription Tier Mix",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    snapshot.tierDistribution.forEach { metric ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = metric.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${metric.count} (${metric.sharePercent}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (metric.sharePercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = AcidLime,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }
            }
        }

        // Exam Popularity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Top Target Nursing Exams",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (snapshot.topExams.isEmpty()) {
                        Text(
                            text = "No learner exam preferences recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        snapshot.topExams.forEach { exam ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = exam.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${exam.userCount} users (${exam.sharePercent}%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (exam.sharePercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }
                        }
                    }
                }
            }
        }

        // Content & Operations Health
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Operations & Content Health",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AnalyticsStatLine("Live OTA Packs", "${snapshot.liveQuestionPacks} packs (${snapshot.totalOtaMcqs} MCQs)")
                    AnalyticsStatLine("MCQ Error Flags", "${snapshot.openQuestionReports} open • ${snapshot.resolvedQuestionReports} resolved")
                    AnalyticsStatLine("Access Codes", "${snapshot.activeAccessCodes} active • ${snapshot.redeemedAccessCodes} redeemed")
                    AnalyticsStatLine("Broadcast Alerts", "${snapshot.totalAnnouncements} sent")
                    AnalyticsStatLine("Rejected UPI UTRs", "${snapshot.rejectedPaymentsCount}")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) AcidLime.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, if (highlight) AcidLime.copy(alpha = 0.6f) else editorial.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = if (highlight) AcidLime else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalyticsStatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// OWNER ADMIN WEEKLY ALL-INDIA GRAND TEST MANAGER (DORMANT BY DEFAULT)
// ============================================================================

@Composable
fun AdminGrandTestTabContent(
    state: AdminConsoleUiState,
    config: CloudSubscriptionConfig,
    onToggleGrandTestEnabled: (Boolean) -> Unit,
    onCreateGrandTest: (
        title: String,
        examId: String,
        weekLabel: String,
        questionCount: Int,
        durationMinutes: Int,
        status: GrandTestStatus,
        onSuccess: () -> Unit
    ) -> Unit,
    onUpdateGrandTestStatus: (testId: String, status: GrandTestStatus) -> Unit,
    onDeleteGrandTest: (testId: String, title: String) -> Unit,
    onLoadLeaderboard: (CloudGrandTest) -> Unit,
    onDismissLeaderboard: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var title by remember { mutableStateOf("") }
    var weekLabel by remember { mutableStateOf("Week 1") }
    var selectedExamId by remember { mutableStateOf(PlatformCatalogDefaults.EXAM_AIIMS_NORCET) }
    var questionCount by remember { mutableIntStateOf(50) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var initialStatus by remember { mutableStateOf(GrandTestStatus.DRAFT) }
    var deleteCandidate by remember { mutableStateOf<CloudGrandTest?>(null) }

    val featuredExams = remember {
        listOf(
            PlatformCatalogDefaults.EXAM_AIIMS_NORCET,
            PlatformCatalogDefaults.EXAM_RRB_NURSING,
            PlatformCatalogDefaults.EXAM_PGIMER_NURSING,
            PlatformCatalogDefaults.EXAM_ESIC_NURSING,
            PlatformCatalogDefaults.EXAM_JIPMER_NURSING,
            PlatformCatalogDefaults.EXAM_DSSSB_NURSING
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Visibility Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (config.grandTestEnabled) {
                        AcidLime.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (config.grandTestEnabled) AcidLime else editorial.borderSubtle
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Student Visibility",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (config.grandTestEnabled) AcidLime else MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = if (config.grandTestEnabled) "ACTIVE" else "DORMANT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = if (config.grandTestEnabled) PureBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (config.grandTestEnabled) {
                                    "Students can now see LIVE & CLOSED Weekly Grand Tests on Dashboard and CBT Hub, submit 1 official attempt, and view their All-India Rank & Percentile."
                                } else {
                                    "Dormant mode is ON (recommended while user base is small). Weekly Grand Test UI is completely hidden from students with zero Firestore reads. You can prepare drafts now and flip this ON anytime."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.grandTestEnabled,
                            onCheckedChange = onToggleGrandTestEnabled,
                            enabled = !state.isMutating,
                            modifier = Modifier.testTag("admin_grand_test_master_switch")
                        )
                    }
                }
            }
        }

        // Create New Weekly Grand Test Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Schedule Weekly All-India Grand Test",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        label = { Text("Grand Test Title") },
                        placeholder = { Text("e.g. All-India NORCET Mega Mock #1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_gt_title_input")
                    )

                    OutlinedTextField(
                        value = weekLabel,
                        onValueChange = { weekLabel = it.take(40) },
                        label = { Text("Week / Edition Label") },
                        placeholder = { Text("e.g. Week 1 • Dec 2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Target Exam",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        featuredExams.forEach { examId ->
                            MonochromeFilterPill(
                                label = EntitlementEvaluator.shortExamLabel(examId),
                                selected = selectedExamId == examId,
                                onClick = { selectedExamId = examId }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Questions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(25, 50, 100).forEach { count ->
                                    MonochromeFilterPill(
                                        label = "${count}Q",
                                        selected = questionCount == count,
                                        onClick = { questionCount = count }
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(25, 45, 90).forEach { mins ->
                                    MonochromeFilterPill(
                                        label = "${mins}m",
                                        selected = durationMinutes == mins,
                                        onClick = { durationMinutes = mins }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Initial Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(GrandTestStatus.DRAFT, GrandTestStatus.LIVE).forEach { st ->
                            MonochromeFilterPill(
                                label = st.label,
                                selected = initialStatus == st,
                                onClick = { initialStatus = st }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCreateGrandTest(
                                title.trim(),
                                selectedExamId,
                                weekLabel.trim(),
                                questionCount,
                                durationMinutes,
                                initialStatus
                            ) {
                                title = ""
                            }
                        },
                        enabled = !state.isMutating && title.trim().length >= 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_create_grand_test_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Weekly Grand Test", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Existing Grand Tests List
        item {
            Text(
                text = "Scheduled & Past Grand Tests (${state.grandTests.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.grandTests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, editorial.borderSubtle)
                ) {
                    Text(
                        text = "No Weekly Grand Tests created yet. Draft your first All-India test above — it stays hidden until you toggle Student Visibility ON.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.grandTests, key = { it.effectiveTestId }) { test ->
                val status = test.parsedStatus
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(
                        1.dp,
                        if (status == GrandTestStatus.LIVE) AcidLime.copy(alpha = 0.6f) else editorial.borderSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (status) {
                                            GrandTestStatus.LIVE -> AcidLime
                                            GrandTestStatus.CLOSED -> MaterialTheme.colorScheme.secondaryContainer
                                            GrandTestStatus.DRAFT -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    ) {
                                        Text(
                                            text = status.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = if (status == GrandTestStatus.LIVE) PureBlack else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = test.weekLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = test.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${EntitlementEvaluator.shortExamLabel(test.examId)} • ${test.questionCount} Qs • ${test.durationMinutes} min • -${test.negativeMarks}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { deleteCandidate = test },
                                enabled = !state.isMutating
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Grand Test",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (status != GrandTestStatus.LIVE) {
                                OutlinedButton(
                                    onClick = { onUpdateGrandTestStatus(test.effectiveTestId, GrandTestStatus.LIVE) },
                                    enabled = !state.isMutating,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Set Live", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (status == GrandTestStatus.LIVE) {
                                OutlinedButton(
                                    onClick = { onUpdateGrandTestStatus(test.effectiveTestId, GrandTestStatus.CLOSED) },
                                    enabled = !state.isMutating,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Close Test", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (status != GrandTestStatus.DRAFT) {
                                OutlinedButton(
                                    onClick = { onUpdateGrandTestStatus(test.effectiveTestId, GrandTestStatus.DRAFT) },
                                    enabled = !state.isMutating,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Draft", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Button(
                                onClick = { onLoadLeaderboard(test) },
                                enabled = !state.isGrandTestLeaderboardLoading,
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidLime,
                                    contentColor = PureBlack
                                )
                            ) {
                                Icon(Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Ranks", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Grand Test?") },
            text = {
                Text("This permanently deletes \"${target.title}\" and all its leaderboard entries.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGrandTest(target.effectiveTestId, target.title)
                        deleteCandidate = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Admin Leaderboard Inspector Dialog
    val summary: GrandTestLeaderboardSummary? = state.selectedGrandTestLeaderboard
    if (state.isGrandTestLeaderboardLoading || summary != null) {
        AlertDialog(
            onDismissRequest = onDismissLeaderboard,
            title = {
                Text(
                    text = summary?.test?.title ?: "Loading Leaderboard...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (state.isGrandTestLeaderboardLoading && summary == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else if (summary != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Participants: ${summary.totalParticipants} • Avg: ${summary.averageNetScore} • Top: ${summary.highestNetScore}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        if (summary.rankedEntries.isEmpty()) {
                            Text(
                                text = "No student attempts submitted yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(summary.rankedEntries, key = { it.entry.userId }) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${row.rank}  ${row.entry.studentName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Acc ${row.entry.accuracyPercent}% • ${row.entry.correctCount}/${row.entry.totalQuestions} correct",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${row.percentile}%ile",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                                fontWeight = FontWeight.Bold,
                                                color = AcidLime
                                            )
                                            Text(
                                                text = "Net ${row.entry.netScore}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissLeaderboard) {
                    Text("Close")
                }
            }
        )
    }
}
