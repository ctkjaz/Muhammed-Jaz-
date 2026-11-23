package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DifficultyFilter
import com.example.data.model.Flashcard
import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.ui.theme.AcidLime
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.EditorialBannerMuted
import com.example.ui.theme.EditorialBannerSubtext
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.Graphite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.editorialColors
import java.io.File
import java.text.NumberFormat
import java.util.Locale

fun formatApproxQuestionCount(total: Int, verbose: Boolean = false): String {
    val suffix = if (verbose) "questions" else "Qs"
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    return when {
        total >= 1000 -> "${formatter.format((total / 1000) * 1000)}+ $suffix"
        total >= 500 -> "500+ $suffix"
        total >= 100 -> "${(total / 100) * 100}+ $suffix"
        else -> "$total $suffix"
    }
}

@Composable
fun ClinEdgeLogoMark(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val corner = RoundedCornerShape((size.value * 0.26f).dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(corner)
            .background(PureBlack)
            .border(1.dp, AmoledBorder, corner),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_clinedge_lime_logo_1795347301816),
            contentDescription = "ClinEdge logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AcidLimeProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    fillColor: Color = MaterialTheme.editorialColors.acidLime
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "acid_lime_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(fillColor)
            )
        }
    }
}

@Composable
fun <T> SegmentedPillBar(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    testTagOf: (T) -> String = { "seg_${labelOf(it)}" },
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) editorial.acidLime else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "seg_bg"
                )
                val fgColor = if (isSelected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onSelect(option) }
                        .testTag(testTagOf(option))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelOf(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fgColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileAvatarCircle(
    name: String,
    size: Dp = 40.dp,
    avatarPath: String = "",
    modifier: Modifier = Modifier
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val avatarBitmap = remember(avatarPath) {
        if (avatarPath.isBlank()) {
            null
        } else {
            runCatching {
                val file = File(avatarPath)
                if (file.exists() && file.length() > 0L) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    val editorial = MaterialTheme.editorialColors
    val circleBg = if (editorial.isDark) Color(0xFF1C1C1C) else Graphite
    val ringColor = if (size >= 56.dp) editorial.acidLime else editorial.borderSubtle

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circleBg)
            .border(
                width = if (size >= 56.dp) 2.dp else 1.dp,
                color = ringColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                maxLines = 1
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}

@Composable
fun DifficultyBadge(
    difficulty: QuestionDifficulty,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val badgeColor = when (difficulty) {
        QuestionDifficulty.EASY -> editorial.correctText
        QuestionDifficulty.MEDIUM -> MaterialTheme.colorScheme.onSurfaceVariant
        QuestionDifficulty.HARD -> editorial.wrongText
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PyqSourceBadge(
    question: Question,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    if (question.isPredictedMock) {
        Surface(
            modifier = modifier.testTag("predicted_mock_badge"),
            shape = RoundedCornerShape(8.dp),
            color = editorial.acidLime,
            border = BorderStroke(1.dp, editorial.acidLime)
        ) {
            Text(
                text = "Predicted Mock",
                style = MaterialTheme.typography.labelSmall,
                color = editorial.onAcidLime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        return
    }
    if (!question.isPyqTagged) return
    val isVerified = question.isVerifiedSource
    val shift = question.effectiveShiftYear
    val badgeText = when {
        isVerified && shift.isNotBlank() -> "Official PYQ • $shift"
        isVerified -> "Official PYQ"
        shift.isNotBlank() -> "PYQ • $shift"
        else -> "Memory-Based PYQ"
    }
    val fgColor = if (isVerified) editorial.correctText else MaterialTheme.colorScheme.onSurface
    val bgColor = if (isVerified) editorial.correctBg else editorial.elevatedSurface
    val borderColor = if (isVerified) editorial.correctText.copy(alpha = 0.35f) else editorial.borderSubtle
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
    val editorial = MaterialTheme.editorialColors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DifficultyFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) editorial.acidLime else editorial.cardSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) editorial.acidLime else editorial.borderSubtle
                ),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(filter) }
                    .testTag("${testTagPrefix}_${filter.name.lowercase()}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MonochromeFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) editorial.acidLime else editorial.cardSurface,
        border = BorderStroke(1.dp, if (selected) editorial.acidLime else editorial.borderSubtle),
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        color = Color(0xFF1A1A1A),
        border = BorderStroke(1.dp, AmoledBorder)
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
    @Suppress("UNUSED_PARAMETER") accentColor: Color,
    totalCount: Int,
    masteredCount: Int,
    accuracyPercent: Int,
    attemptsCount: Int,
    onPractice: () -> Unit,
    onTimed: () -> Unit,
    subtitleBadge: String? = null,
    chapterCount: Int? = null,
    hideTotalCeiling: Boolean = true,
    primaryButtonLabel: String = "Chapters",
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val progress = if (totalCount > 0) masteredCount.toFloat() / totalCount else 0f
    val badgeText = when {
        !hideTotalCeiling -> "$totalCount Qs"
        chapterCount != null && chapterCount > 0 -> "$chapterCount Chapters"
        else -> "Syllabus Unit"
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitleBadge.isNullOrBlank()) {
                        Text(
                            text = subtitleBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AcidLimeProgressBar(
                progress = progress,
                height = 5.dp,
                fillColor = if (progress > 0f) editorial.acidLime else editorial.elevatedSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (attemptsCount > 0) {
                        if (hideTotalCeiling) "$masteredCount mastered" else "$masteredCount/$totalCount mastered"
                    } else {
                        "Not started"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attemptsCount > 0) {
                    Text(
                        text = "$accuracyPercent% acc",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (accuracyPercent >= 75) editorial.correctText else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPractice,
                    enabled = totalCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("practice_${title}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(primaryButtonLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onTimed,
                    enabled = totalCount > 0,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("timed_${title}"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Timed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Text(
                    text = card.tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
    val editorial = MaterialTheme.editorialColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Text(
                            text = q.subject,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (q.topicId.isNotBlank() && !q.topicId.endsWith("_general")) {
                        val topicLabel = q.topicId
                            .substringAfter("_", q.topicId)
                            .split("_")
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                        if (topicLabel.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = editorial.elevatedSurface
                            ) {
                                Text(
                                    text = topicLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
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
                        tint = if (item.isBookmarked) {
                            if (editorial.isDark) AcidLime else PureBlack
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                color = editorial.correctText,
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
                val accColor = when {
                    item.accuracyPercent >= 75 -> editorial.correctText
                    item.accuracyPercent >= 40 -> editorial.warningText
                    else -> editorial.wrongText
                }
                Text(
                    text = "${item.correctCount}/${item.attemptsCount} correct (${item.accuracyPercent}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = accColor
                )
            }
        }
    }
}
