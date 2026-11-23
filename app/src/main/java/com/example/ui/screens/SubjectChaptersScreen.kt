package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChapterBucket
import com.example.data.model.DifficultyFilter
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectPracticeLogic
import com.example.data.model.SyllabusCatalog
import androidx.compose.ui.graphics.Color
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.SubjectChaptersNavState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectChaptersScreen(
    navState: SubjectChaptersNavState,
    state: DashboardDerivedState,
    difficultyFilter: DifficultyFilter,
    highYieldOnly: Boolean,
    secondsPerQuestion: Int,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onToggleHighYieldOnly: () -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartTimedPractice: (List<QuestionItem>, String, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    val subjectName = remember(navState.subjectCode) {
        SubjectCatalog.displayNameFor(navState.subjectCode)
    }

    val scopeLabel = remember(navState, state.activeExam, state.activeExamSupportsStages, state.activeExamStage, state.availableExams) {
        if (navState.isCrossExam) {
            if (navState.examIdFilter == null) {
                "All 29 Nursing Exams"
            } else {
                state.availableExams.firstOrNull { it.id == navState.examIdFilter }?.shortName
                    ?: PlatformCatalogDefaults.examById(navState.examIdFilter)?.shortName
                    ?: "Selected Exam"
            }
        } else {
            val base = state.activeExam?.shortName ?: "Active Exam"
            if (state.activeExamSupportsStages) {
                val stg = if (state.activeExamStage == ExamStage.MAINS) "Stage II Mains" else "Stage I Prelims"
                "$base · $stg"
            } else {
                base
            }
        }
    }

    val subjectPool = remember(
        navState,
        state.stageQuestions,
        state.questions,
        state.crossExamQuestions,
        state.crossExamLinks
    ) {
        if (navState.isCrossExam) {
            SubjectPracticeLogic.collectQuestionsForSubject(
                allQuestions = state.crossExamQuestions,
                allLinks = state.crossExamLinks,
                subjectCode = navState.subjectCode,
                examIdFilter = navState.examIdFilter,
                difficultyFilter = DifficultyFilter.ALL,
                excludePyqAndMock = false,
                alreadyDeduplicated = true,
                includeVirtualShared = true
            )
        } else {
            val base = if (state.activeExamSupportsStages) state.stageQuestions else state.questions
            base.filter { it.question.subject == navState.subjectCode }
        }
    }

    val chapters = remember(subjectPool, navState.subjectCode, highYieldOnly, difficultyFilter) {
        SyllabusCatalog.buildChaptersForSubject(
            subjectCode = navState.subjectCode,
            items = subjectPool,
            highYieldOnly = highYieldOnly,
            difficultyFilter = difficultyFilter
        )
    }

    val expandedChapters = remember(navState.subjectCode) { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subject_chapters_screen")
    ) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chapters_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Subjects"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$scopeLabel • ${chapters.size} Syllabus Units",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Quick Random 50 Drill for this subject
                    val filteredFlat = remember(chapters) { chapters.flatMap { it.items } }
                    if (filteredFlat.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = editorial.acidLime,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    val picked = filteredFlat.shuffled().take(50)
                                    val suffix = if (highYieldOnly) "High-Yield" else "Mixed"
                                    onStartPractice(picked, "$subjectName · Random ${picked.size} ($suffix)")
                                }
                                .testTag("chapters_random_50_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = "Random 50 drill",
                                    tint = editorial.onAcidLime,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Random 50",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = editorial.onAcidLime
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = editorial.borderSubtle)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High-Yield Important Questions Toggle + Difficulty Filter
                item(key = "chapters_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (highYieldOnly) editorial.acidLime else editorial.elevatedSurface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (highYieldOnly) editorial.acidLime else editorial.borderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onToggleHighYieldOnly)
                                .testTag("chapters_high_yield_toggle")
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "High-Yield Filter",
                                        tint = if (highYieldOnly) editorial.onAcidLime else editorial.acidLime,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "High-Yield & Important Questions Only",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (highYieldOnly) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (highYieldOnly) {
                                                "Showing repeated PYQ concepts, predicted mocks & critical priority MCQs"
                                            } else {
                                                "Tap to filter for repeated exam concepts, antidotes & clinical priorities"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (highYieldOnly) {
                                                editorial.onAcidLime.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (highYieldOnly) editorial.onAcidLime else editorial.cardSurface
                                ) {
                                    Text(
                                        text = if (highYieldOnly) "ON" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (highYieldOnly) editorial.acidLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        DifficultyFilterBar(
                            selected = difficultyFilter,
                            onSelect = onSelectDifficulty
                        )
                    }
                }

                if (chapters.isEmpty()) {
                    item(key = "chapters_empty") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No chapters match these filters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try switching difficulty to All or turning off High-Yield Only to view all INC syllabus units.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(chapters, key = { it.meta.id }) { chapter ->
                        val isExpanded = expandedChapters[chapter.meta.id] ?: false
                        ChapterUnitCard(
                            chapter = chapter,
                            subjectName = subjectName,
                            highYieldOnly = highYieldOnly,
                            isExpanded = isExpanded,
                            secondsPerQuestion = secondsPerQuestion,
                            onToggleExpand = { expandedChapters[chapter.meta.id] = !isExpanded },
                            onPracticeChapter = {
                                val title = "Unit ${chapter.meta.unitNumber}: ${chapter.meta.title}"
                                val ordered = chapter.items.sortedByDescending { SyllabusCatalog.isHighYield(it.question) }
                                onStartPractice(ordered, title)
                            },
                            onTimedChapter = {
                                val count = chapter.items.size.coerceAtMost(30)
                                val picked = chapter.items.shuffled().take(count)
                                val mins = ((picked.size * secondsPerQuestion.coerceAtLeast(30)) / 60).coerceIn(5, 60)
                                onStartTimedPractice(
                                    picked,
                                    "Timed · Unit ${chapter.meta.unitNumber}: ${chapter.meta.title}",
                                    mins
                                )
                            },
                            onPracticeTopic = { topicLabel, topicItems ->
                                onStartPractice(
                                    topicItems,
                                    "$subjectName · $topicLabel"
                                )
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterUnitCard(
    chapter: ChapterBucket,
    subjectName: String,
    highYieldOnly: Boolean,
    isExpanded: Boolean,
    secondsPerQuestion: Int,
    onToggleExpand: () -> Unit,
    onPracticeChapter: () -> Unit,
    onTimedChapter: () -> Unit,
    onPracticeTopic: (String, List<QuestionItem>) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val total = chapter.items.size
    val progress = if (total > 0) chapter.masteredCount.toFloat() / total.toFloat() else 0f

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.meta.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Unit badge + exact chapter question count + High-Yield badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChapterPillBadge(
                        text = "UNIT ${chapter.meta.unitNumber}",
                        containerColor = editorial.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    ChapterPillBadge(
                        text = "$total Qs",
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    )
                    if (!highYieldOnly && chapter.highYieldCount > 0) {
                        ChapterPillBadge(
                            text = "${chapter.highYieldCount} High-Yield",
                            containerColor = editorial.acidLimeMuted,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (chapter.attemptedCount > 0) {
                    Text(
                        text = "${chapter.accuracyPercent}% acc",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = chapter.meta.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = chapter.meta.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mastery Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = editorial.acidLime,
                    trackColor = editorial.cardSurface,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${chapter.masteredCount}/$total mastered • ${chapter.topics.size} topics",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPracticeChapter,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("chapter_practice_${chapter.meta.id}"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Practice", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onTimedChapter,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("chapter_timed_${chapter.meta.id}"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Timed chapter drill",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Timed", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                if (chapter.topics.size > 1) {
                    OutlinedButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("chapter_topics_toggle_${chapter.meta.id}"),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "Topics",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse topics" else "Expand topics",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable Nested Topics List with exact topic counts
            AnimatedVisibility(
                visible = isExpanded && chapter.topics.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = editorial.borderSubtle)
                    Text(
                        text = "TOPICS IN UNIT ${chapter.meta.unitNumber} ($subjectName)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chapter.topics.forEach { topic ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = editorial.cardSurface,
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPracticeTopic(topic.label, topic.items) }
                                    .testTag("topic_chip_${topic.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = topic.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = editorial.elevatedSurface
                                    ) {
                                        Text(
                                            text = "${topic.items.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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

@Composable
private fun ChapterPillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
