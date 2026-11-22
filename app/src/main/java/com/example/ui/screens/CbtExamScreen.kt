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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Lock
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
import com.example.ui.components.PyqSourceBadge
import com.example.ui.components.formatTimer
import com.example.ui.theme.CalmSageFill
import com.example.ui.theme.EditorialBannerKpiAccent
import com.example.ui.theme.EditorialBannerKpiDanger
import com.example.ui.theme.EditorialBannerKpiSuccess
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MutedCrimsonFill
import com.example.ui.theme.MutedPlumFill
import com.example.ui.theme.WarmOchreFill
import com.example.ui.theme.WarmWhite
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.CbtPaletteStatus
import com.example.ui.viewmodel.CbtReviewFilter
import com.example.ui.viewmodel.CbtSessionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onRequestNextSection: () -> Unit = {},
    onDismissNextSection: () -> Unit = {},
    onConfirmNextSection: () -> Unit = {},
    onDismissSectionNotice: () -> Unit = {},
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
                        val negLabel = String.format(Locale.US, "%.2f", session.negativeMarks)
                        Text(
                            text = "+1.00 Correct • -$negLabel Incorrect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.hasCountdown) {
                            val activeTimerSec = if (session.hasSectionLock) {
                                session.sectionRemainingSeconds
                            } else {
                                session.remainingSeconds
                            }
                            val timerBg = when {
                                activeTimerSec <= 60 -> MutedCrimsonFill
                                activeTimerSec <= 300 -> WarmOchreFill
                                else -> EditorialBannerSurface
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
                                        tint = WarmWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (session.hasSectionLock) {
                                            "S${session.currentSectionIndex + 1} ${formatTimer(activeTimerSec)}"
                                        } else {
                                            formatTimer(activeTimerSec)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                        color = WarmWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CalmSageFill,
                                contentColor = WarmWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("cbt_submit_header_btn")
                        ) {
                            Text("Submit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (session.hasSectionLock) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cbt_section_lock_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Section locked mode",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "${session.currentSectionLabel} (${session.currentSectionIndex + 1}/${session.totalSections})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Q${session.activeSectionRange.first + 1}–Q${session.activeSectionRange.last + 1} active • Total left ${formatTimer(session.remainingSeconds)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        if (!session.isLastSection) {
                            OutlinedButton(
                                onClick = onRequestNextSection,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("cbt_next_section_btn")
                            ) {
                                Text("Next Sec", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            session.sectionAutoAdvanceNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissSectionNotice) {
                            Text("OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    PyqSourceBadge(question = q)
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
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutedPlumFill,
                                contentColor = WarmWhite
                            ),
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
                                when {
                                    session.currentIndex < session.activeSectionRange.last -> "Save & Next"
                                    session.hasSectionLock && !session.isLastSection -> "Lock & Next Sec"
                                    else -> "Save & Finish"
                                },
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalmSageFill,
                        contentColor = WarmWhite
                    )
                ) {
                    Text("View Score Card")
                }
            }
        )
    }

    if (session.showSectionAdvanceConfirm && session.hasSectionLock && !session.isLastSection && !session.showTimeoutDialog) {
        val secRange = session.activeSectionRange
        val secAnswered = secRange.count { session.selectedAnswers.containsKey(it) }
        val secTotal = secRange.count()
        AlertDialog(
            onDismissRequest = onDismissNextSection,
            title = { Text("Lock Section ${session.currentSectionIndex + 1} & Continue?") },
            text = {
                Text(
                    "You have answered $secAnswered of $secTotal questions in ${session.currentSectionLabel}. " +
                        "Per AIIMS NORCET CBT rules, once you advance to Section ${session.currentSectionIndex + 2}, this section is locked and cannot be revisited."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmNextSection,
                    modifier = Modifier.testTag("cbt_confirm_next_section_btn")
                ) {
                    Text("Lock & Next Section")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissNextSection) {
                    Text("Stay in Section")
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalmSageFill,
                        contentColor = WarmWhite
                    ),
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
            text = {
                Text(
                    if (session.hasCountdown) {
                        "Your active exam timer will stop and unsaved progress will be lost."
                    } else {
                        "Unsaved progress in this CBT session will be lost."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitConfirm = false
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedCrimsonFill,
                        contentColor = WarmWhite
                    )
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
    val unvisitedColor = MaterialTheme.editorialColors.unvisitedSwatch

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Question Palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (session.hasSectionLock) {
            Text(
                text = "${session.currentSectionLabel} (Q${session.activeSectionRange.first + 1}–Q${session.activeSectionRange.last + 1}) • Other sections locked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendChip(color = CalmSageFill, text = "Ans (${counts.getValue(CbtPaletteStatus.ANSWERED)})", modifier = Modifier.weight(1f))
            LegendChip(color = MutedCrimsonFill, text = "Not Ans (${counts.getValue(CbtPaletteStatus.NOT_ANSWERED)})", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val markedTotal = counts.getValue(CbtPaletteStatus.MARKED) + counts.getValue(CbtPaletteStatus.ANSWERED_MARKED)
            LegendChip(color = MutedPlumFill, text = "Marked ($markedTotal)", modifier = Modifier.weight(1f))
            LegendChip(color = unvisitedColor, text = "Unvisited (${counts.getValue(CbtPaletteStatus.NOT_VISITED)})", modifier = Modifier.weight(1f))
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
                val inActiveSection = session.isIndexInActiveSection(idx)
                val st = session.paletteStatuses[idx] ?: CbtPaletteStatus.NOT_VISITED
                val baseBg = when (st) {
                    CbtPaletteStatus.ANSWERED -> CalmSageFill
                    CbtPaletteStatus.NOT_ANSWERED -> MutedCrimsonFill
                    CbtPaletteStatus.MARKED, CbtPaletteStatus.ANSWERED_MARKED -> MutedPlumFill
                    CbtPaletteStatus.NOT_VISITED -> MaterialTheme.colorScheme.surfaceVariant
                }
                val bg = if (inActiveSection) baseBg else baseBg.copy(alpha = 0.28f)
                val baseFg = if (st == CbtPaletteStatus.NOT_VISITED) MaterialTheme.colorScheme.onSurface else WarmWhite
                val fg = if (inActiveSection) baseFg else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                val border = when {
                    idx == session.currentIndex -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    st == CbtPaletteStatus.ANSWERED_MARKED && inActiveSection -> BorderStroke(2.dp, CalmSageFill)
                    else -> null
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    border = border,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = inActiveSection) { onSelectQuestion(idx) }
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
            colors = ButtonDefaults.buttonColors(
                containerColor = CalmSageFill,
                contentColor = WarmWhite
            ),
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

@OptIn(ExperimentalLayoutApi::class)
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

    val negLabel = String.format(Locale.US, "%.2f", session.negativeMarks)
    val netScore = right - (wrong * session.negativeMarks)
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
                    colors = CardDefaults.cardColors(containerColor = EditorialBannerSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CBT Score Card", style = MaterialTheme.typography.labelMedium, color = EditorialBannerKpiAccent)
                                Text(session.title, style = MaterialTheme.typography.titleLarge, color = EditorialBannerTitle, fontWeight = FontWeight.Bold)
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
                                valueColor = EditorialBannerKpiAccent,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Accuracy",
                                value = "$acc%",
                                valueColor = EditorialBannerKpiSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(label = "Correct (+1)", value = "$right", valueColor = EditorialBannerKpiSuccess, modifier = Modifier.weight(1f))
                            MetricTile(label = "Wrong (-$negLabel)", value = "$wrong", valueColor = EditorialBannerKpiDanger, modifier = Modifier.weight(1f))
                            MetricTile(label = "Skipped", value = "$skipped", valueColor = EditorialBannerTitle, modifier = Modifier.weight(1f))
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
                val editorial = MaterialTheme.editorialColors
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
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Q${idx + 1} • ${q.subject}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                PyqSourceBadge(question = q)
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
                                    ans == q.correctIndex -> editorial.correctText
                                    else -> editorial.wrongText
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Correct Answer: ${'A' + q.correctIndex}. ${q.options.getOrElse(q.correctIndex) { "" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = editorial.correctText
                        )
                        if (q.explanation.isNotBlank()) {
                            Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (q.isVerifiedSource) {
                            Text(
                                text = "Source: ${q.sourceNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.correctText
                            )
                        }
                    }
                }
            }
        }
    }
}
