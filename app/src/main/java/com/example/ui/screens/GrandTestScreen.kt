package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.grandtest.CloudGrandTest
import com.example.data.grandtest.GrandTestLeaderboardSummary
import com.example.data.grandtest.GrandTestStatus
import com.example.data.subscription.EntitlementEvaluator
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors

@Composable
fun GrandTestScreen(
    tests: List<CloudGrandTest>,
    selectedTestId: String?,
    leaderboardSummary: GrandTestLeaderboardSummary?,
    isLoading: Boolean,
    onSelectTest: (String) -> Unit,
    onRefreshLeaderboard: () -> Unit,
    onStartGrandTest: (CloudGrandTest) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    val selectedTest = tests.firstOrNull { it.effectiveTestId == selectedTestId } ?: tests.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("grand_test_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Weekly Grand Test",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = AcidLime) {
                                Text(
                                    text = "AIR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = PureBlack,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "All-India Nursing Rank & Percentile Leaderboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshLeaderboard,
                    enabled = !isLoading && selectedTest != null
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Leaderboard")
                    }
                }
            }

            if (tests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = AcidLime,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No Weekly Grand Test Live Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upcoming All-India Nursing Grand Tests will appear here as soon as they go live.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Test selector strip if multiple tests exist
                if (tests.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tests.forEach { test ->
                            val isSelected = test.effectiveTestId == selectedTest?.effectiveTestId
                            val prefix = if (test.parsedStatus == GrandTestStatus.LIVE) "● " else ""
                            MonochromeFilterPill(
                                label = "$prefix${test.weekLabel.ifBlank { test.title.take(18) }}",
                                selected = isSelected,
                                onClick = { onSelectTest(test.effectiveTestId) }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTest != null) {
                        val isLive = selectedTest.parsedStatus == GrandTestStatus.LIVE
                        val myRank = leaderboardSummary?.currentUserRank

                        // Selected Test Hero Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(
                                    1.dp,
                                    if (isLive) AcidLime.copy(alpha = 0.7f) else editorial.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isLive) AcidLime else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = if (isLive) "LIVE NOW" else "CLOSED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                color = if (isLive) PureBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Text(
                                            text = "${selectedTest.weekLabel} • ${EntitlementEvaluator.shortExamLabel(selectedTest.examId)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = selectedTest.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "${selectedTest.questionCount} MCQs • ${selectedTest.durationMinutes} Minutes • -${selectedTest.negativeMarks} Negative Marking • 1 Official Attempt",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (myRank != null) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = AcidLime.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.55f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = AcidLime,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Official Attempt Submitted",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "AIR #${myRank.rank} of ${leaderboardSummary.totalParticipants} • ${myRank.percentile}%ile • Net ${myRank.entry.netScore}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else if (isLive) {
                                        Button(
                                            onClick = { onStartGrandTest(selectedTest) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("start_grand_test_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AcidLime,
                                                contentColor = PureBlack
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Start Official Grand Test", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "Submissions are closed for this edition. Final All-India rankings are shown below.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Summary Stats Strip
                        if (leaderboardSummary != null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GrandTestStatChip(
                                        label = "PARTICIPANTS",
                                        value = leaderboardSummary.totalParticipants.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    GrandTestStatChip(
                                        label = "TOP SCORE",
                                        value = leaderboardSummary.highestNetScore.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    GrandTestStatChip(
                                        label = "AVG SCORE",
                                        value = leaderboardSummary.averageNetScore.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Leaderboard Header
                            item {
                                Text(
                                    text = "All-India Percentile Leaderboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (leaderboardSummary.rankedEntries.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        border = BorderStroke(1.dp, editorial.borderSubtle)
                                    ) {
                                        Text(
                                            text = "Be the first nursing aspirant to submit this Weekly Grand Test and claim AIR #1!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(leaderboardSummary.rankedEntries, key = { it.entry.userId }) { row ->
                                    val isTopThree = row.rank <= 3
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (row.isCurrentUser) {
                                                AcidLime.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            }
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (row.isCurrentUser || isTopThree) AcidLime.copy(alpha = 0.55f) else editorial.borderSubtle
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isTopThree) AcidLime else MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = "#${row.rank}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isTopThree) PureBlack else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = row.entry.studentName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (row.isCurrentUser) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = AcidLime
                                                            ) {
                                                                Text(
                                                                    text = "YOU",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                                    color = PureBlack,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "${row.entry.correctCount}✓ ${row.entry.wrongCount}✗ • ${row.entry.accuracyPercent}% acc • ${formatDurationShort(row.entry.timeTakenSeconds)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${row.percentile}%ile",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
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
                }
            }
        }
    }
}

@Composable
private fun GrandTestStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, editorial.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDurationShort(seconds: Long): String {
    val mins = (seconds / 60L).coerceAtLeast(0L)
    val secs = (seconds % 60L).coerceAtLeast(0L)
    return "${mins}m ${secs}s"
}
