package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import com.example.data.model.DifficultyFilter
import com.example.data.model.Flashcard
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.ui.theme.EditorialBannerMuted
import com.example.ui.theme.EditorialBannerSubtext
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
fun DifficultyBadge(
    difficulty: QuestionDifficulty,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val badgeColor = when (difficulty) {
        QuestionDifficulty.EASY -> editorial.correctText
        QuestionDifficulty.MEDIUM -> editorial.warningText
        QuestionDifficulty.HARD -> editorial.wrongText
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.14f)
    ) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PyqSourceBadge(
    question: Question,
    modifier: Modifier = Modifier
) {
    val badgeText = question.pyqBadgeText() ?: return
    val isVerified = question.isVerifiedSource
    val editorial = MaterialTheme.editorialColors
    val fgColor = if (isVerified) editorial.correctText else editorial.warningText
    val bgColor = fgColor.copy(alpha = if (isVerified) 0.14f else 0.16f)
    val borderColor = fgColor.copy(alpha = if (isVerified) 0.4f else 0.45f)
    Surface(
        modifier = modifier.testTag("pyq_source_badge"),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DifficultyFilterBar(
    selected: DifficultyFilter,
    onSelect: (DifficultyFilter) -> Unit,
    testTagPrefix: String = "diff",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DifficultyFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected == filter) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("${testTagPrefix}_${filter.name.lowercase()}")
            )
        }
    }
}

fun formatTimer(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color = EditorialBannerTitle,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = EditorialBannerTitle.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EditorialBannerSubtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
                if (subValue != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = EditorialBannerMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectProgressCard(
    title: String,
    accentColor: Color,
    totalCount: Int,
    masteredCount: Int,
    accuracyPercent: Int,
    attemptsCount: Int,
    onPractice: () -> Unit,
    onTimed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$totalCount Qs",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (attemptsCount > 0) "Mastered $masteredCount/$totalCount • $accuracyPercent% acc" else "Mastered $masteredCount/$totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPractice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("practice_${title}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Practice", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onTimed,
                    modifier = Modifier.testTag("timed_${title}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Timed", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun FlashcardItemCard(
    card: Flashcard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = card.tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = card.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionSummaryCard(
    item: QuestionItem,
    indexLabel: String,
    onToggleBookmark: () -> Unit,
    onClick: (() -> Unit)? = null,
    badgeOverride: Pair<String, Color>? = null,
    modifier: Modifier = Modifier
) {
    val q = item.question
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = q.subject,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    PyqSourceBadge(question = q)
                    DifficultyBadge(difficulty = q.difficulty)
                    if (badgeOverride != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeOverride.second.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeOverride.first,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = badgeOverride.second,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_card_${q.id}")
                ) {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (item.isBookmarked) "Remove bookmark" else "Save bookmark",
                        tint = if (item.isBookmarked) MaterialTheme.editorialColors.warningText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$indexLabel ${q.questionText}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            val ansText = q.options.getOrElse(q.correctIndex) { "" }
            Text(
                text = "Ans: ${'A' + q.correctIndex}. $ansText",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.editorialColors.correctText,
                fontWeight = FontWeight.Bold
            )

            if (q.explanation.isNotBlank()) {
                Text(
                    text = q.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.attemptsCount > 0) {
                val editorial = MaterialTheme.editorialColors
                val accColor = when {
                    item.accuracyPercent >= 75 -> editorial.correctText
                    item.accuracyPercent >= 40 -> editorial.warningText
                    else -> editorial.wrongText
                }
                Text(
                    text = "History: ${item.correctCount}/${item.attemptsCount} correct (${item.accuracyPercent}% accuracy)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = accColor
                )
            }
        }
    }
}
