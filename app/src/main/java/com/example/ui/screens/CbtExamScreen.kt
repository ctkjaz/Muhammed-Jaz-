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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ResultShareFormatter
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.ClinicalQuestionImageCard
import com.example.ui.components.ImageBasedQuestionBadge
import com.example.ui.components.MetricTile
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.PyqSourceBadge
import com.example.ui.components.formatTimer
import com.example.ui.components.sharePlainText
import com.example.ui.theme.AcidLime
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.EditorialBannerKpiAccent
import com.example.ui.theme.EditorialBannerKpiDanger
import com.example.ui.theme.EditorialBannerKpiSuccess
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.Graphite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SemanticCorrectFill
import com.example.ui.theme.SemanticReviewFill
import com.example.ui.theme.SemanticWarningFill
import com.example.ui.theme.SemanticWrongFill
import com.example.ui.theme.editorialColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionTimerState
import com.example.ui.viewmodel.CbtPaletteStatus
import com.example.ui.viewmodel.CbtReviewFilter
import com.example.ui.viewmodel.CbtSessionState
import kotlinx.coroutines.flow.StateFlow
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
    timerStateFlow: StateFlow<SessionTimerState>? = null,
    modifier: Modifier = Modifier
) {
    var paletteSheetOpen by remember { mutableStateOf(false) }
    var showQuitConfirm by remember { mutableStateOf(false) }
    val editorial = MaterialTheme.editorialColors

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
    val progressFraction = if (session.questions.isNotEmpty()) {
        (session.currentIndex + 1).toFloat() / session.questions.size.toFloat()
    } else 0f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minimal CBT Top Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            text = "+1.00  ·  -$negLabel  ·  ${session.selectedAnswers.size}/${session.questions.size} ans",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.hasCountdown) {
                            CbtTimerBadge(
                                timerStateFlow = timerStateFlow,
                                hasSectionLock = session.hasSectionLock,
                                currentSectionIndex = session.currentSectionIndex,
                                fallbackTotalSeconds = session.remainingSeconds,
                                fallbackSectionSeconds = session.sectionRemainingSeconds
                            )
                        }

                        if (!isWide) {
                            OutlinedButton(
                                onClick = { paletteSheetOpen = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("cbt_palette_btn")
                            ) {
                                Icon(
                                    Icons.Filled.GridView,
                                    contentDescription = "Open question palette",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Palette", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = onRequestSubmitConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
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

            // Thin Acid-Lime Exam Progress Indicator
            AcidLimeProgressBar(progress = progressFraction, height = 4.dp)

            if (session.hasSectionLock) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = editorial.acidLime,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Section locked mode",
                                        tint = editorial.onAcidLime,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "${session.currentSectionLabel} (${session.currentSectionIndex + 1}/${session.totalSections})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CbtSectionTotalTimerSubtitle(
                                    sectionRangeFirst = session.activeSectionRange.first,
                                    sectionRangeLast = session.activeSectionRange.last,
                                    timerStateFlow = timerStateFlow,
                                    fallbackTotalSeconds = session.remainingSeconds
                                )
                            }
                        }
                        if (!session.isLastSection) {
                            OutlinedButton(
                                onClick = onRequestNextSection,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
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
                    color = editorial.acidLimeMuted,
                    border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.6f)),
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
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissSectionNotice) {
                            Text("OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Body: Distraction-free Question + (Persistent Palette on Tablets)
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
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = editorial.acidLime
                                    ) {
                                        Text(
                                            text = "Q${session.currentIndex + 1} / ${session.questions.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = editorial.onAcidLime,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = editorial.elevatedSurface,
                                        border = BorderStroke(1.dp, editorial.borderSubtle)
                                    ) {
                                        Text(
                                            text = q.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    PyqSourceBadge(question = q)
                                    if (q.isImageBased) {
                                        ImageBasedQuestionBadge()
                                    }
                                }

                                if (q.isImageBased) {
                                    ClinicalQuestionImageCard(imageUrl = q.imageUrl)
                                }

                                Text(
                                    text = q.questionText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Options — Acid Lime highlight when selected
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    q.options.forEachIndexed { oi, optionText ->
                                        val isSelected = chosen == oi
                                        val optionBg = if (isSelected) editorial.acidLime else editorial.cardSurface
                                        val optionBorder = if (isSelected) editorial.acidLime else editorial.borderSubtle
                                        val optionTextColor = if (isSelected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface
                                        val badgeBg = if (isSelected) PureBlack else editorial.elevatedSurface
                                        val badgeFg = if (isSelected) AcidLime else MaterialTheme.colorScheme.onSurface

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = optionBg,
                                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, optionBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onSelectOption(oi) }
                                                .testTag("cbt_option_$oi")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
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
                                                        text = "${'A' + oi}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                                        color = badgeFg,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = optionText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = optionTextColor,
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
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("cbt_clear_btn")
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onMarkForReviewAndNext,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SemanticReviewFill),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("cbt_mark_next_btn")
                        ) {
                            Text(
                                "Mark & Next",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = onSaveAndNext,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("cbt_save_next_btn")
                        ) {
                            Text(
                                when {
                                    session.currentIndex < session.activeSectionRange.last -> "Save & Next"
                                    session.hasSectionLock && !session.isLastSection -> "Lock & Next"
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
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = editorial.cardSurface
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
            containerColor = editorial.cardSurface,
            title = { Text("Time's Up!", fontWeight = FontWeight.Bold) },
            text = { Text("Exam time has expired. Submit now to generate your Subject-Wise Score Card.") },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    )
                ) {
                    Text("View Score Card", fontWeight = FontWeight.Bold)
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
            containerColor = editorial.cardSurface,
            title = { Text("Lock Section ${session.currentSectionIndex + 1}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You answered $secAnswered of $secTotal in ${session.currentSectionLabel}. " +
                        "Once you advance to Section ${session.currentSectionIndex + 2}, this section cannot be revisited."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmNextSection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier.testTag("cbt_confirm_next_section_btn")
                ) {
                    Text("Lock & Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissNextSection) {
                    Text("Stay")
                }
            }
        )
    }

    if (session.showSubmitConfirm && !session.showTimeoutDialog) {
        val ansCount = session.selectedAnswers.size
        AlertDialog(
            onDismissRequest = onDismissSubmitConfirm,
            containerColor = editorial.cardSurface,
            title = { Text("Submit CBT Exam?", fontWeight = FontWeight.Bold) },
            text = {
                Text("You answered $ansCount of ${session.questions.size} questions. Submit to view your Score Card and Review?")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier.testTag("cbt_confirm_submit_btn")
                ) {
                    Text("Submit Exam", fontWeight = FontWeight.Bold)
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
            containerColor = editorial.cardSurface,
            title = { Text("Quit CBT Exam?", fontWeight = FontWeight.Bold) },
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
                        containerColor = SemanticWrongFill,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Quit Exam", fontWeight = FontWeight.Bold)
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
    val editorial = MaterialTheme.editorialColors
    val counts = remember(session.paletteStatuses) {
        val map = mutableMapOf<CbtPaletteStatus, Int>().withDefault { 0 }
        session.paletteStatuses.values.forEach { st -> map[st] = map.getValue(st) + 1 }
        map
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Question Palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (session.hasSectionLock) {
            Text(
                text = "${session.currentSectionLabel} (Q${session.activeSectionRange.first + 1}–Q${session.activeSectionRange.last + 1}) · Other sections locked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendChip(color = SemanticCorrectFill, text = "Ans (${counts.getValue(CbtPaletteStatus.ANSWERED)})", modifier = Modifier.weight(1f))
            LegendChip(color = SemanticWrongFill, text = "Not Ans (${counts.getValue(CbtPaletteStatus.NOT_ANSWERED)})", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val markedTotal = counts.getValue(CbtPaletteStatus.MARKED) + counts.getValue(CbtPaletteStatus.ANSWERED_MARKED)
            LegendChip(color = SemanticReviewFill, text = "Marked ($markedTotal)", modifier = Modifier.weight(1f))
            LegendChip(color = editorial.unvisitedSwatch, text = "Unvisited (${counts.getValue(CbtPaletteStatus.NOT_VISITED)})", modifier = Modifier.weight(1f))
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
                val isCurrent = idx == session.currentIndex
                val st = session.paletteStatuses[idx] ?: CbtPaletteStatus.NOT_VISITED
                val baseBg = when {
                    isCurrent -> editorial.acidLime
                    st == CbtPaletteStatus.ANSWERED -> SemanticCorrectFill
                    st == CbtPaletteStatus.NOT_ANSWERED -> SemanticWrongFill
                    st == CbtPaletteStatus.MARKED || st == CbtPaletteStatus.ANSWERED_MARKED -> SemanticReviewFill
                    else -> editorial.elevatedSurface
                }
                val bg = if (inActiveSection) baseBg else baseBg.copy(alpha = 0.25f)
                val baseFg = when {
                    isCurrent -> editorial.onAcidLime
                    st == CbtPaletteStatus.NOT_VISITED -> MaterialTheme.colorScheme.onSurface
                    else -> PureWhite
                }
                val fg = if (inActiveSection) baseFg else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val border = when {
                    isCurrent -> BorderStroke(1.5.dp, PureBlack)
                    st == CbtPaletteStatus.ANSWERED_MARKED && inActiveSection -> BorderStroke(2.dp, editorial.acidLime)
                    st == CbtPaletteStatus.NOT_VISITED && inActiveSection -> BorderStroke(1.dp, editorial.borderSubtle)
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
                containerColor = editorial.acidLime,
                contentColor = editorial.onAcidLime
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
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
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val editorial = MaterialTheme.editorialColors
    val context = LocalContext.current
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
                    border = BorderStroke(1.dp, AmoledBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "CBT SCORE CARD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = EditorialBannerKpiAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    session.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EditorialBannerTitle,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val topSubj = subjMap.entries
                                            .filter { it.value.right + it.value.wrong > 0 }
                                            .maxByOrNull { (_, s) ->
                                                (s.right.toDouble() / (s.right + s.wrong).toDouble())
                                            }
                                            ?.let { (subj, s) ->
                                                val pct = ((s.right.toFloat() / (s.right + s.wrong)) * 100f).toInt()
                                                "$subj ($pct%)"
                                            }
                                        val shareText = ResultShareFormatter.formatCbtResultShare(
                                            title = session.title,
                                            netScore = netScore,
                                            totalQuestions = session.questions.size,
                                            right = right,
                                            wrong = wrong,
                                            skipped = skipped,
                                            accuracyPercent = acc,
                                            negativeMarks = session.negativeMarks,
                                            topSubjectLine = topSubj
                                        )
                                        sharePlainText(
                                            context = context,
                                            subject = "ClinEdge CBT Score · ${session.title}",
                                            text = shareText
                                        )
                                    },
                                    modifier = Modifier.testTag("cbt_share_result_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share CBT score card",
                                        tint = EditorialBannerKpiAccent
                                    )
                                }
                                Button(
                                    onClick = onExit,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AcidLime,
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }
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
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Subject Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        subjMap.forEach { (subj, d) ->
                            val pct = if (d.total > 0) ((d.right.toFloat() / d.total) * 100f).toInt() else 0
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(subj, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${d.right}/${d.total} ($pct%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AcidLimeProgressBar(progress = pct / 100f, height = 5.dp)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Answer Review (${filtered.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CbtReviewFilter.entries.forEach { f ->
                            MonochromeFilterPill(
                                label = f.label,
                                selected = session.reviewFilter == f,
                                onClick = { onChangeFilter(f) }
                            )
                        }
                    }
                }
            }

            itemsIndexed(filtered) { _, (idx, item, ans) ->
                val q = item.question
                Card(
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
                                Text(
                                    "Q${idx + 1} · ${q.subject}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
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
                            "Correct: ${'A' + q.correctIndex}. ${q.options.getOrElse(q.correctIndex) { "" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = editorial.correctText,
                            fontWeight = FontWeight.Bold
                        )
                        if (q.explanation.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = editorial.explanationSurface,
                                border = BorderStroke(1.dp, editorial.borderSubtle)
                            ) {
                                Text(
                                    text = q.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
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

@Composable
private fun CbtTimerBadge(
    timerStateFlow: StateFlow<SessionTimerState>?,
    hasSectionLock: Boolean,
    currentSectionIndex: Int,
    fallbackTotalSeconds: Int,
    fallbackSectionSeconds: Int
) {
    val activeTimerSec = if (timerStateFlow != null) {
        val timerState by timerStateFlow.collectAsStateWithLifecycle()
        if (hasSectionLock) timerState.sectionRemainingSeconds else timerState.remainingSeconds
    } else {
        if (hasSectionLock) fallbackSectionSeconds else fallbackTotalSeconds
    }
    val timerBg = when {
        activeTimerSec <= 60 -> SemanticWrongFill
        activeTimerSec <= 300 -> SemanticWarningFill
        else -> Graphite
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = timerBg,
        border = BorderStroke(1.dp, AmoledBorder),
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
                tint = if (activeTimerSec > 300) AcidLime else PureWhite,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (hasSectionLock) {
                    "S${currentSectionIndex + 1} ${formatTimer(activeTimerSec)}"
                } else {
                    formatTimer(activeTimerSec)
                },
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CbtSectionTotalTimerSubtitle(
    sectionRangeFirst: Int,
    sectionRangeLast: Int,
    timerStateFlow: StateFlow<SessionTimerState>?,
    fallbackTotalSeconds: Int
) {
    val totalSec = if (timerStateFlow != null) {
        val timerState by timerStateFlow.collectAsStateWithLifecycle()
        timerState.remainingSeconds
    } else {
        fallbackTotalSeconds
    }
    Text(
        text = "Q${sectionRangeFirst + 1}–Q${sectionRangeLast + 1} active  ·  Total ${formatTimer(totalSec)}",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

