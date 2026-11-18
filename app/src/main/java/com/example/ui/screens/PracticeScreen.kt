package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QuestionItem
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.MetricTile
import com.example.ui.components.formatTimer
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.RoseWrong
import com.example.ui.viewmodel.CbtReviewFilter
import com.example.ui.viewmodel.PracticeSessionState
import java.util.Locale

@Composable
fun PracticeScreen(
    session: PracticeSessionState,
    allQuestions: List<QuestionItem>,
    negativeMarking: Boolean,
    onToggleNegativeMarking: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onMoveIndex: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onRequestSubmit: () -> Unit,
    onDismissSubmitPrompt: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onChangeReviewFilter: (CbtReviewFilter) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (session.timedMode && !session.isSubmittedSummary) {
            showExitConfirm = true
        } else {
            onExit()
        }
    }

    if (session.isSubmittedSummary) {
        TimedPracticeSummaryView(
            session = session,
            negativeMarking = negativeMarking,
            onChangeFilter = onChangeReviewFilter,
            onExit = onExit,
            modifier = modifier
        )
        return
    }

    val currentItem = session.questions.getOrNull(session.currentIndex) ?: return
    val liveProgressItem = allQuestions.firstOrNull { it.id == currentItem.id } ?: currentItem
    val q = currentItem.question
    val chosen = session.selectedAnswers[q.id]
    val answered = chosen != null
    val isBookmarked = liveProgressItem.isBookmarked

    var sRight = 0
    var sWrong = 0
    session.questions.forEach { item ->
        val sel = session.selectedAnswers[item.id]
        if (sel != null) {
            if (sel == item.question.correctIndex) sRight++ else sWrong++
        }
    }
    val sNet = if (negativeMarking) sRight - (sWrong * 0.33) else sRight.toDouble()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                if (session.timedMode) showExitConfirm = true else onExit()
                            },
                            modifier = Modifier.testTag("practice_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit practice")
                        }
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.timedMode) {
                            val timerBg = when {
                                session.remainingSeconds <= 60 -> RoseWrong
                                session.remainingSeconds <= 300 -> AmberHighlight
                                else -> Color(0xFF0F172A)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = timerBg,
                                modifier = Modifier.testTag("practice_timer_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        contentDescription = "Remaining time",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatTimer(session.remainingSeconds),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (negativeMarking) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onToggleNegativeMarking)
                                    .testTag("neg_marking_toggle")
                            ) {
                                Text(
                                    text = if (negativeMarking) "-0.33 ON" else "-0.33 OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (negativeMarking) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", sNet),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Question Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Q ${session.currentIndex + 1} / ${session.questions.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = q.subject,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (q.shiftYear.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "PYQ ${q.shiftYear}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                DifficultyBadge(difficulty = q.difficulty)
                            }

                            IconButton(
                                onClick = { onToggleBookmark(q.id) },
                                modifier = Modifier.testTag("practice_bookmark_btn")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark question",
                                    tint = if (isBookmarked) AmberHighlight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            q.options.forEachIndexed { oi, optionText ->
                                val isCorrectOpt = oi == q.correctIndex
                                val isChosenOpt = oi == chosen
                                val borderColor = when {
                                    !answered -> MaterialTheme.colorScheme.outline
                                    isCorrectOpt -> EmeraldCorrect
                                    isChosenOpt -> RoseWrong
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                }
                                val bgColor = when {
                                    !answered -> MaterialTheme.colorScheme.surface
                                    isCorrectOpt -> EmeraldCorrect.copy(alpha = 0.12f)
                                    isChosenOpt -> RoseWrong.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                }
                                val badgeBg = when {
                                    !answered -> MaterialTheme.colorScheme.surfaceVariant
                                    isCorrectOpt -> EmeraldCorrect
                                    isChosenOpt -> RoseWrong
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val badgeFg = if (answered && (isCorrectOpt || isChosenOpt)) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                val badgeLabel = when {
                                    answered && isCorrectOpt -> "✓"
                                    answered && isChosenOpt -> "✕"
                                    else -> "${'A' + oi}"
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = bgColor,
                                    border = BorderStroke(if (answered && (isCorrectOpt || isChosenOpt)) 1.5.dp else 1.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = !answered) { onSelectOption(oi) }
                                        .testTag("practice_option_$oi")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = badgeFg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = optionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = answered) {
                            val isRight = chosen == q.correctIndex
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isRight) {
                                            "✓ Correct (+1.00)"
                                        } else {
                                            "✕ Incorrect (${if (negativeMarking) "-0.33" else "0.00"}) • Correct: ${'A' + q.correctIndex}"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isRight) EmeraldCorrect else RoseWrong,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Clinical Rationale / Solution: ${q.explanation.ifBlank { "Verified answer." }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Strip
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(session.questions) { idx, item ->
                        val ans = session.selectedAnswers[item.id]
                        val bg = when {
                            idx == session.currentIndex -> MaterialTheme.colorScheme.primary
                            ans == item.question.correctIndex -> EmeraldCorrect
                            ans != null -> RoseWrong
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val fg = if (idx == session.currentIndex || ans != null) Color.White else MaterialTheme.colorScheme.onSurface
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMoveIndex(idx) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = fg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onMoveIndex(session.currentIndex - 1) },
                        enabled = session.currentIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("practice_prev_btn")
                    ) {
                        Text("← Prev")
                    }

                    if (session.timedMode) {
                        OutlinedButton(
                            onClick = onRequestSubmit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("practice_timed_submit_btn")
                        ) {
                            Text("Submit (${session.selectedAnswers.size}/${session.questions.size})")
                        }
                    }

                    if (session.currentIndex < session.questions.lastIndex) {
                        Button(
                            onClick = { onMoveIndex(session.currentIndex + 1) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("practice_next_btn")
                        ) {
                            Text("Next →")
                        }
                    } else {
                        Button(
                            onClick = { if (session.timedMode) onRequestSubmit() else onExit() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("practice_finish_btn")
                        ) {
                            Text(if (session.timedMode) "Finish & Submit" else "Finish ✓")
                        }
                    }
                }
            }
        }
    }

    // Timeout AlertDialog (non-dismissible until user submits)
    if (session.showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Time's Up!") },
            text = {
                Text(
                    "Your configured timer has expired. You answered ${session.selectedAnswers.size} of ${session.questions.size} questions. Submit now to view your score and review solutions."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
                    modifier = Modifier.testTag("timeout_submit_btn")
                ) {
                    Text("Submit & View Report")
                }
            }
        )
    }

    // Completion / Early Submit Prompt
    if (session.showCompletionPrompt && !session.showTimeoutDialog) {
        val remainingQs = session.questions.size - session.selectedAnswers.size
        AlertDialog(
            onDismissRequest = onDismissSubmitPrompt,
            title = { Text("Submit Timed Practice?") },
            text = {
                Text(
                    if (remainingQs == 0) {
                        "You have answered all ${session.questions.size} questions with ${formatTimer(session.remainingSeconds)} remaining. Submit now to view your detailed report?"
                    } else {
                        "You have answered ${session.selectedAnswers.size}/${session.questions.size} questions ($remainingQs unanswered). Submit now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
                    modifier = Modifier.testTag("confirm_timed_submit_btn")
                ) {
                    Text("Submit Practice")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSubmitPrompt) {
                    Text("Keep Practicing")
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit Timed Practice?") },
            text = { Text("Your active countdown timer will stop. Do you want to submit first or exit?") },
            confirmButton = {
                Button(onClick = {
                    showExitConfirm = false
                    onExit()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
private fun TimedPracticeSummaryView(
    session: PracticeSessionState,
    negativeMarking: Boolean,
    onChangeFilter: (CbtReviewFilter) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var right = 0
    var wrong = 0
    var skipped = 0
    session.questions.forEach { item ->
        val sel = session.selectedAnswers[item.id]
        when {
            sel == null -> skipped++
            sel == item.question.correctIndex -> right++
            else -> wrong++
        }
    }
    val net = if (negativeMarking) right - (wrong * 0.33) else right.toDouble()
    val attempted = right + wrong
    val acc = if (attempted > 0) ((right.toFloat() / attempted) * 100f).toInt() else 0

    val filtered = session.questions.mapIndexedNotNull { idx, item ->
        val ans = session.selectedAnswers[item.id]
        val keep = when (session.reviewFilter) {
            CbtReviewFilter.ALL -> true
            CbtReviewFilter.WRONG -> ans != null && ans != item.question.correctIndex
            CbtReviewFilter.RIGHT -> ans == item.question.correctIndex
            CbtReviewFilter.SKIPPED -> ans == null
        }
        if (keep) Triple(idx, item, ans) else null
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                Text("Timed Practice Score Card", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7DD3FC))
                                Text(session.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = onExit) {
                                Text("Done")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Net Score",
                                value = String.format(Locale.US, "%.2f", net),
                                subValue = "/ ${session.questions.size}",
                                valueColor = Color(0xFF7DD3FC),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Accuracy",
                                value = "$acc%",
                                valueColor = Color(0xFF6EE7B7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(label = "Correct (+1)", value = "$right", valueColor = Color(0xFF6EE7B7), modifier = Modifier.weight(1f))
                            MetricTile(label = "Wrong", value = "$wrong", valueColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                            MetricTile(label = "Skipped", value = "$skipped", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CbtReviewFilter.entries.forEach { f ->
                        FilterChip(
                            selected = session.reviewFilter == f,
                            onClick = { onChangeFilter(f) },
                            label = { Text(f.label) }
                        )
                    }
                }
            }

            itemsIndexed(filtered) { _, (idx, item, ans) ->
                val q = item.question
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Q${idx + 1} • ${q.subject}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                DifficultyBadge(difficulty = q.difficulty)
                            }
                            Text(
                                text = when {
                                    ans == null -> "Skipped"
                                    ans == q.correctIndex -> "✓ Correct"
                                    else -> "✕ Marked ${'A' + ans}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    ans == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    ans == q.correctIndex -> EmeraldCorrect
                                    else -> RoseWrong
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Correct Answer: ${'A' + q.correctIndex}. ${q.options.getOrElse(q.correctIndex) { "" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldCorrect
                        )
                        if (q.explanation.isNotBlank()) {
                            Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
