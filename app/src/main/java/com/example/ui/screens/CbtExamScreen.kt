package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.components.MetricTile
import com.example.ui.components.formatTimer
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PurpleReview
import com.example.ui.theme.RoseWrong
import com.example.ui.viewmodel.CbtPaletteStatus
import com.example.ui.viewmodel.CbtReviewFilter
import com.example.ui.viewmodel.CbtSessionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbtExamScreen(
    session: CbtSessionState,
    onSelectOption: (Int) -> Unit,
    onClearResponse: () -> Unit,
    onMoveTo: (Int) -> Unit,
    onSaveAndNext: () -> Unit,
    onMarkForReviewAndNext: () -> Unit,
    onRequestSubmitConfirm: () -> Unit,
    onDismissSubmitConfirm: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onChangeReviewFilter: (CbtReviewFilter) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var paletteSheetOpen by remember { mutableStateOf(false) }
    var showQuitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (session.isSubmitted) {
            onExit()
        } else {
            showQuitConfirm = true
        }
    }

    if (session.isSubmitted) {
        CbtResultScreen(
            session = session,
            onChangeFilter = onChangeReviewFilter,
            onExit = onExit,
            modifier = modifier
        )
        return
    }

    val currentItem = session.questions.getOrNull(session.currentIndex) ?: return
    val q = currentItem.question
    val chosen = session.selectedAnswers[session.currentIndex]

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CBT Top Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "+1.00 Correct • -0.33 Incorrect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timerBg = when {
                            session.remainingSeconds <= 60 -> RoseWrong
                            session.remainingSeconds <= 300 -> AmberHighlight
                            else -> Color(0xFF0F172A)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = timerBg,
                            modifier = Modifier.testTag("cbt_timer_badge")
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

                        if (!isWide) {
                            OutlinedButton(
                                onClick = { paletteSheetOpen = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("cbt_palette_btn")
                            ) {
                                Icon(Icons.Filled.GridView, contentDescription = "Open question palette", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Palette", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = onRequestSubmitConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("cbt_submit_header_btn")
                        ) {
                            Text("Submit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Body: Question + (Persistent Palette on Tablets)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
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
                                    Text(
                                        text = "Question No. ${session.currentIndex + 1} of ${session.questions.size}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
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
                                }

                                Text(
                                    text = q.questionText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    q.options.forEachIndexed { oi, optionText ->
                                        val isSelected = chosen == oi
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            border = BorderStroke(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onSelectOption(oi) }
                                                .testTag("cbt_option_$oi")
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
                                                        .background(
                                                            if (isSelected) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.surfaceVariant
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${'A' + oi}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
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
                            }
                        }
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onClearResponse,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cbt_clear_btn")
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onMarkForReviewAndNext,
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleReview),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cbt_mark_next_btn")
                        ) {
                            Text("Mark & Next", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        Button(
                            onClick = onSaveAndNext,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cbt_save_next_btn")
                        ) {
                            Text(
                                if (session.currentIndex < session.questions.lastIndex) "Save & Next" else "Save & Finish",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (isWide) {
                    Card(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        CbtPaletteContent(
                            session = session,
                            onSelectQuestion = onMoveTo,
                            onSubmit = onRequestSubmitConfirm,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (paletteSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { paletteSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CbtPaletteContent(
                session = session,
                onSelectQuestion = { idx ->
                    paletteSheetOpen = false
                    onMoveTo(idx)
                },
                onSubmit = {
                    paletteSheetOpen = false
                    onRequestSubmitConfirm()
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }

    if (session.showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Time's Up!") },
            text = { Text("Exam time has expired. Submit now to generate your Subject-Wise Score Card.") },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect)
                ) {
                    Text("View Score Card")
                }
            }
        )
    }

    if (session.showSubmitConfirm && !session.showTimeoutDialog) {
        val ansCount = session.selectedAnswers.size
        AlertDialog(
            onDismissRequest = onDismissSubmitConfirm,
            title = { Text("Submit CBT Exam?") },
            text = {
                Text("You have answered $ansCount of ${session.questions.size} questions. Submit and view Subject-Wise Analytics?")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
                    modifier = Modifier.testTag("cbt_confirm_submit_btn")
                ) {
                    Text("Submit Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSubmitConfirm) {
                    Text("Keep Reviewing")
                }
            }
        )
    }

    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            title = { Text("Quit CBT Exam?") },
            text = { Text("Your active exam timer will stop and unsaved progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitConfirm = false
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseWrong)
                ) {
                    Text("Quit Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitConfirm = false }) { Text("Resume") }
            }
        )
    }
}

@Composable
private fun CbtPaletteContent(
    session: CbtSessionState,
    onSelectQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = remember(session.paletteStatuses) {
        val map = mutableMapOf<CbtPaletteStatus, Int>().withDefault { 0 }
        session.paletteStatuses.values.forEach { st -> map[st] = map.getValue(st) + 1 }
        map
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Question Palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendChip(color = EmeraldCorrect, text = "Ans (${counts.getValue(CbtPaletteStatus.ANSWERED)})", modifier = Modifier.weight(1f))
            LegendChip(color = RoseWrong, text = "Not Ans (${counts.getValue(CbtPaletteStatus.NOT_ANSWERED)})", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val markedTotal = counts.getValue(CbtPaletteStatus.MARKED) + counts.getValue(CbtPaletteStatus.ANSWERED_MARKED)
            LegendChip(color = PurpleReview, text = "Marked ($markedTotal)", modifier = Modifier.weight(1f))
            LegendChip(color = Color(0xFF64748B), text = "Unvisited (${counts.getValue(CbtPaletteStatus.NOT_VISITED)})", modifier = Modifier.weight(1f))
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
        ) {
            items(session.questions.size) { idx ->
                val st = session.paletteStatuses[idx] ?: CbtPaletteStatus.NOT_VISITED
                val bg = when (st) {
                    CbtPaletteStatus.ANSWERED -> EmeraldCorrect
                    CbtPaletteStatus.NOT_ANSWERED -> RoseWrong
                    CbtPaletteStatus.MARKED, CbtPaletteStatus.ANSWERED_MARKED -> PurpleReview
                    CbtPaletteStatus.NOT_VISITED -> MaterialTheme.colorScheme.surfaceVariant
                }
                val fg = if (st == CbtPaletteStatus.NOT_VISITED) MaterialTheme.colorScheme.onSurface else Color.White
                val border = when {
                    idx == session.currentIndex -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    st == CbtPaletteStatus.ANSWERED_MARKED -> BorderStroke(2.dp, EmeraldCorrect)
                    else -> null
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    border = border,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectQuestion(idx) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCorrect),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Exam", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegendChip(color: Color, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CbtResultScreen(
    session: CbtSessionState,
    onChangeFilter: (CbtReviewFilter) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class SubjTotals(var total: Int = 0, var right: Int = 0, var wrong: Int = 0)
    var right = 0
    var wrong = 0
    var skipped = 0
    val subjMap = linkedMapOf<String, SubjTotals>()

    session.questions.forEachIndexed { idx, item ->
        val subj = item.question.subject
        val bucket = subjMap.getOrPut(subj) { SubjTotals() }
        bucket.total++
        val ans = session.selectedAnswers[idx]
        when {
            ans == null -> skipped++
            ans == item.question.correctIndex -> {
                right++
                bucket.right++
            }
            else -> {
                wrong++
                bucket.wrong++
            }
        }
    }

    val netScore = right - (wrong * 0.33)
    val attempted = right + wrong
    val acc = if (attempted > 0) ((right.toFloat() / attempted) * 100f).toInt() else 0

    val filtered = session.questions.mapIndexedNotNull { idx, item ->
        val ans = session.selectedAnswers[idx]
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
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CBT Score Card", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7DD3FC))
                                Text(session.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = onExit) {
                                Text("Dashboard")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Net Marks",
                                value = String.format(Locale.US, "%.2f", netScore),
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
                            MetricTile(label = "Wrong (-0.33)", value = "$wrong", valueColor = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                            MetricTile(label = "Skipped", value = "$skipped", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Subject-Wise Accuracy Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        subjMap.forEach { (subj, d) ->
                            val pct = if (d.total > 0) ((d.right.toFloat() / d.total) * 100f).toInt() else 0
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(subj, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${d.right}/${d.total} ($pct%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detailed Answer Review (${filtered.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            }

            itemsIndexed(filtered) { _, (idx, item, ans) ->
                val q = item.question
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Q${idx + 1} • ${q.subject}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
