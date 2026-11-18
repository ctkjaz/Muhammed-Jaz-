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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryStat
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectStat
import com.example.ui.components.MetricTile
import com.example.ui.components.QuestionSummaryCard
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.RoseWrong
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
    val lowest = state.lowestAccuracyQuestions

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 2 else 1

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overall Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
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
                                    "Detailed Performance Analytics",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Lifetime accuracy per subject, category & weak-question tracker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            IconButton(
                                onClick = { confirmReset = true },
                                modifier = Modifier.testTag("reset_stats_btn")
                            ) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    contentDescription = "Reset statistics",
                                    tint = Color(0xFFFCA5A5)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Overall Accuracy",
                                value = "${state.overall.accuracyPercent}%",
                                valueColor = Color(0xFF6EE7B7),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Total Attempts",
                                value = "${state.overall.totalAttempts}",
                                subValue = "(${state.overall.attemptedUnique} Qs)",
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
                                subValue = "/ ${state.overall.totalWrong} err",
                                valueColor = Color(0xFF7DD3FC),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Net Latest Score",
                                value = String.format(Locale.US, "%.2f", state.overall.netPracticeScore),
                                valueColor = Color(0xFFFCD34D),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Accuracy Per Question Category
            item {
                Text("Accuracy by Question Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text(
                    "Accuracy by Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            val subjRows = state.subjectStats.chunked(columns)
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, RoseWrong.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(top = 6.dp)
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
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = RoseWrong)
                            Column {
                                Text(
                                    "Lowest Accuracy Questions (${lowest.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Sorted by lowest historical accuracy for targeted revision",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (lowest.isNotEmpty()) {
                            Button(
                                onClick = { onPracticeLowestAccuracy(lowest.take(25)) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseWrong),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("review_lowest_accuracy_btn")
                            ) {
                                Text("Drill Top ${minOf(25, lowest.size)}")
                            }
                        }
                    }
                }
            }

            if (lowest.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No weak questions flagged yet! Practice MCQs or take a CBT Mock — any missed questions will appear here ranked by accuracy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(lowest.take(30), key = { _, item -> item.id }) { idx, item ->
                    val acc = item.accuracyPercent
                    val badgeColor = if (acc < 50) RoseWrong else AmberHighlight
                    QuestionSummaryCard(
                        item = item,
                        indexLabel = "#${idx + 1}",
                        badgeOverride = "$acc% Acc (${item.correctCount}/${item.attemptsCount})" to badgeColor,
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
            title = { Text("Clear Attempt Statistics?") },
            text = { Text("This resets attempt counts and accuracy percentages across subjects. Your bookmarked questions and custom imported MCQs will be kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onClearStats()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseWrong)
                ) {
                    Text("Clear Stats")
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
    val pct = cat.accuracyPercent
    val progress = if (cat.totalAttempts > 0) pct / 100f else 0f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    color = cat.accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (cat.totalAttempts > 0) "$pct%" else "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = cat.accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = cat.accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Covered: ${cat.attemptedQuestions}/${cat.totalQuestions} Qs",
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
    val pct = sub.accuracyPercent
    val accColor = when {
        sub.totalAttempts == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        pct >= 75 -> EmeraldCorrect
        pct >= 45 -> AmberHighlight
        else -> RoseWrong
    }
    val progress = if (sub.totalAttempts > 0) pct / 100f else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${sub.code} • ${sub.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        sub.group.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (sub.totalAttempts > 0) "$pct%" else "No data",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                    color = accColor,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (sub.totalAttempts > 0) accColor else sub.accentColor.copy(alpha = 0.35f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                    "${sub.correctAttempts}/${sub.totalAttempts} attempts",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
