package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryStat
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectStat
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.MetricTile
import com.example.ui.components.QuestionSummaryCard
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.EditorialBannerKpiAccent
import com.example.ui.theme.EditorialBannerKpiDanger
import com.example.ui.theme.EditorialBannerKpiHighlight
import com.example.ui.theme.EditorialBannerKpiSuccess
import com.example.ui.theme.EditorialBannerSubtext
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SemanticWrongFill
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import java.util.Locale

@Composable
fun StatsScreen(
    state: DashboardDerivedState,
    onPracticeLowestAccuracy: (List<QuestionItem>) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onClearStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmReset by remember { mutableStateOf(false) }
    val editorial = MaterialTheme.editorialColors
    val lowest = state.lowestAccuracyQuestions
    val activeExamLabel = state.activeExam?.shortName ?: "RRB Nursing"
    val subtitleText = if (state.activeExamSupportsStages) {
        "$activeExamLabel · ${state.activeExamStage.label} & lifetime accuracy"
    } else {
        "$activeExamLabel · Lifetime accuracy & weak-area tracker"
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 2 else 1

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Summary Card (Graphite + Acid Lime KPIs)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialBannerSurface),
                    border = BorderStroke(1.dp, AmoledBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "ANALYTICS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = EditorialBannerKpiAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Performance Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EditorialBannerTitle,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EditorialBannerSubtext
                                )
                            }
                            IconButton(
                                onClick = { confirmReset = true },
                                modifier = Modifier.testTag("reset_stats_btn")
                            ) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    contentDescription = "Reset statistics",
                                    tint = EditorialBannerKpiDanger
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Accuracy",
                                value = "${state.overall.accuracyPercent}%",
                                valueColor = EditorialBannerKpiAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Total Attempts",
                                value = "${state.overall.totalAttempts}",
                                subValue = "${state.overall.attemptedUnique} Qs",
                                valueColor = EditorialBannerTitle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Correct / Wrong",
                                value = "${state.overall.totalCorrect}",
                                subValue = "/ ${state.overall.totalWrong}",
                                valueColor = EditorialBannerKpiSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Net Score",
                                value = String.format(Locale.US, "%.2f", state.overall.netPracticeScore),
                                valueColor = EditorialBannerKpiHighlight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Accuracy Per Question Category
            item {
                Text(
                    "By Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val catRows = state.categoryStats.chunked(columns)
            items(catRows.size) { rowIdx ->
                val rowItems = catRows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { cat ->
                        CategoryStatCard(cat = cat, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Accuracy Per Subject
            item {
                val activeSubjects = if (state.activeExamSupportsStages) {
                    state.stageSubjectStats.ifEmpty { state.subjectStats }
                } else {
                    state.subjectStats
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.activeExamSupportsStages) "By Subject (${state.activeExamStage.shortLabel})" else "By Subject",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${activeSubjects.size} subjects",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val displaySubjects = if (state.activeExamSupportsStages) {
                state.stageSubjectStats.ifEmpty { state.subjectStats }
            } else {
                state.subjectStats
            }
            val subjRows = displaySubjects.chunked(columns)
            items(subjRows.size) { rowIdx ->
                val rowItems = subjRows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { sub ->
                        SubjectStatCard(sub = sub, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Lowest Accuracy Questions Highlighted for Review
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = editorial.wrongText
                            )
                            Column {
                                Text(
                                    "Weak Questions (${lowest.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Lowest historical accuracy first",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (lowest.isNotEmpty()) {
                            Button(
                                onClick = { onPracticeLowestAccuracy(lowest.take(25)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("review_lowest_accuracy_btn")
                            ) {
                                Text("Drill ${minOf(25, lowest.size)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (lowest.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No weak questions flagged yet. Missed questions from Practice or CBT mocks will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(lowest.take(30), key = { _, item -> item.id }) { idx, item ->
                    val acc = item.accuracyPercent
                    val badgeColor = if (acc < 50) editorial.wrongText else editorial.warningText
                    QuestionSummaryCard(
                        item = item,
                        indexLabel = "#${idx + 1}",
                        badgeOverride = "$acc% (${item.correctCount}/${item.attemptsCount})" to badgeColor,
                        onToggleBookmark = { onToggleBookmark(item.id) },
                        onClick = { onPracticeLowestAccuracy(listOf(item)) }
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = editorial.cardSurface,
            title = { Text("Clear Attempt Statistics?", fontWeight = FontWeight.Bold) },
            text = { Text("This resets attempt counts and accuracy percentages across subjects. Bookmarks and custom imported MCQs stay intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onClearStats()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SemanticWrongFill,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Clear Stats", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryStatCard(cat: CategoryStat, modifier: Modifier = Modifier) {
    val editorial = MaterialTheme.editorialColors
    val pct = cat.accuracyPercent
    val progress = if (cat.totalAttempts > 0) pct / 100f else 0f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cat.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle)
                ) {
                    Text(
                        text = if (cat.totalAttempts > 0) "$pct%" else "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            AcidLimeProgressBar(
                progress = progress,
                height = 5.dp,
                fillColor = if (cat.totalAttempts > 0) editorial.acidLime else editorial.elevatedSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cat.attemptedQuestions}/${cat.totalQuestions} covered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "✓ ${cat.correctAttempts}  ✕ ${cat.wrongAttempts}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubjectStatCard(sub: SubjectStat, modifier: Modifier = Modifier) {
    val editorial = MaterialTheme.editorialColors
    val pct = sub.accuracyPercent
    val accColor = when {
        sub.totalAttempts == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        pct >= 75 -> editorial.correctText
        pct >= 45 -> editorial.warningText
        else -> editorial.wrongText
    }
    val progress = if (sub.totalAttempts > 0) pct / 100f else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${sub.code} · ${sub.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        sub.group.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (sub.totalAttempts > 0) "$pct%" else "—",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                    color = accColor,
                    fontWeight = FontWeight.Bold
                )
            }
            AcidLimeProgressBar(
                progress = progress,
                height = 5.dp,
                fillColor = if (sub.totalAttempts > 0) editorial.acidLime else editorial.elevatedSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mastered ${sub.masteredQuestions}/${sub.totalQuestions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${sub.correctAttempts}/${sub.totalAttempts}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
