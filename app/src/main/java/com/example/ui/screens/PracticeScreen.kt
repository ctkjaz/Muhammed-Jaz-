package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.ClinicalQuestionImageCard
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.ImageBasedQuestionBadge
import com.example.ui.components.MetricTile
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.PracticeOptionRationalesSection
import com.example.ui.components.PyqSourceBadge
import com.example.ui.components.QuestionNoteBannerCard
import com.example.ui.components.QuestionNoteEditorDialog
import com.example.ui.components.formatTimer
import com.example.ui.theme.AcidLime
import com.example.ui.theme.EditorialBannerKpiAccent
import com.example.ui.theme.EditorialBannerKpiDanger
import com.example.ui.theme.EditorialBannerKpiSuccess
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SemanticCorrectFill
import com.example.ui.theme.SemanticWarningFill
import com.example.ui.theme.SemanticWrongFill
import com.example.ui.theme.editorialColors
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionNote
import com.example.ui.viewmodel.CbtReviewFilter
import com.example.ui.viewmodel.PracticeSessionState
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    session: PracticeSessionState,
    isCurrentBookmarked: Boolean,
    negativeMarking: Boolean,
    onToggleNegativeMarking: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onMoveIndex: (Int) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onRequestSubmit: () -> Unit,
    onDismissSubmitPrompt: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onChangeReviewFilter: (CbtReviewFilter) -> Unit,
    onAskDoubt: (QuestionItem) -> Unit = {},
    canUseAiTutor: Boolean = false,
    onExplainWithAi: (QuestionItem, Int?) -> Unit = { _, _ -> },
    onExplainOptionsWithAi: (QuestionItem, Int?) -> Unit = onExplainWithAi,
    onFlagQuestion: (QuestionItem) -> Unit = {},
    questionNotes: Map<String, QuestionNote> = emptyMap(),
    onSaveQuestionNote: (String, String) -> Unit = { _, _ -> },
    onDeleteQuestionNote: (String) -> Unit = {},
    isCurrentInSrs: Boolean = false,
    onQueueToSrs: (String) -> Unit = {},
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitConfirm by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }

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
    val q = currentItem.question
    val chosen = session.selectedAnswers[q.id]
    val answered = chosen != null
    val editorial = MaterialTheme.editorialColors

    val questionScrollState = rememberScrollState()
    val stripListState = rememberLazyListState()

    // Reset scroll cleanly when moving to a new question and keep active pill centered
    LaunchedEffect(session.currentIndex) {
        questionScrollState.scrollTo(0)
        if (session.questions.isNotEmpty()) {
            stripListState.animateScrollToItem(
                index = (session.currentIndex - 2).coerceAtLeast(0)
            )
        }
    }

    // Gently bring rationale into view after the GPU-composited reveal settles
    LaunchedEffect(q.id, answered) {
        if (answered) {
            delay(60L)
            val target = (questionScrollState.maxValue).coerceAtMost(questionScrollState.value + 360)
            if (target > questionScrollState.value) {
                questionScrollState.animateScrollTo(
                    value = target,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val (sRight, sWrong, sNet) = remember(session.questions, session.selectedAnswers, negativeMarking, session.negativeMarks) {
        var r = 0
        var w = 0
        session.questions.forEach { item ->
            val sel = session.selectedAnswers[item.id]
            if (sel != null) {
                if (sel == item.question.correctIndex) r++ else w++
            }
        }
        val net = if (negativeMarking) r - (w * session.negativeMarks) else r.toDouble()
        Triple(r, w, net)
    }
    val negLabel = remember(session.negativeMarks) {
        String.format(Locale.US, "%.2f", session.negativeMarks)
    }
    val progressFrac = if (session.questions.isNotEmpty()) {
        (session.currentIndex + 1).toFloat() / session.questions.size
    } else {
        0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minimalist Top Bar + Acid Lime Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        if (session.timedMode && session.hasCountdown) {
                            val timerBg = when {
                                session.remainingSeconds <= 60 -> SemanticWrongFill
                                session.remainingSeconds <= 300 -> SemanticWarningFill
                                else -> EditorialBannerSurface
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
                                        tint = PureWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatTimer(session.remainingSeconds),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (!session.timedMode) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (negativeMarking) editorial.wrongBg else editorial.elevatedSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (negativeMarking) editorial.wrongText.copy(alpha = 0.4f) else editorial.borderSubtle
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onToggleNegativeMarking)
                                    .testTag("neg_marking_toggle")
                            ) {
                                Text(
                                    text = if (negativeMarking) "-$negLabel" else "No Neg",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (negativeMarking) editorial.wrongText else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", sNet),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                AcidLimeProgressBar(progress = progressFrac, height = 4.dp)
            }

            // Distraction-Free Question Surface (60/120Hz zero-relayout scroll container)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(questionScrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = editorial.elevatedSurface,
                                    border = BorderStroke(1.dp, editorial.borderSubtle)
                                ) {
                                    Text(
                                        text = "${session.currentIndex + 1} / ${session.questions.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = editorial.elevatedSurface
                                ) {
                                    Text(
                                        text = q.subject,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                PyqSourceBadge(question = q)
                                DifficultyBadge(difficulty = q.difficulty)
                                if (q.isImageBased) {
                                    ImageBasedQuestionBadge()
                                }
                            }

                            val currentNote = questionNotes[q.id]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showNoteEditor = true },
                                    modifier = Modifier.testTag("practice_note_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = if (currentNote != null) Icons.Filled.StickyNote2 else Icons.Outlined.StickyNote2,
                                        contentDescription = if (currentNote != null) "Edit personal note" else "Add personal note",
                                        tint = if (currentNote != null) {
                                            if (editorial.isDark) AcidLime else PureBlack
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onFlagQuestion(currentItem) },
                                    modifier = Modifier.testTag("practice_flag_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = "Flag an error in this question",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onAskDoubt(currentItem) },
                                    modifier = Modifier.testTag("practice_ask_doubt_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QuestionAnswer,
                                        contentDescription = "Ask a doubt on this question",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onQueueToSrs(q.id) },
                                    modifier = Modifier.testTag("practice_srs_queue_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Psychology,
                                        contentDescription = if (isCurrentInSrs) "Refresh question in SRS review queue" else "Add question to SRS review queue",
                                        tint = if (isCurrentInSrs) {
                                            if (editorial.isDark) AcidLime else PureBlack
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleBookmark(q.id) },
                                    modifier = Modifier.testTag("practice_bookmark_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (isCurrentBookmarked) "Remove bookmark" else "Bookmark question",
                                        tint = if (isCurrentBookmarked) {
                                            if (editorial.isDark) AcidLime else PureBlack
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        if (q.isImageBased) {
                            ClinicalQuestionImageCard(imageUrl = q.imageUrl)
                        }

                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Clean Option Cards with smooth GPU color transitions
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            q.options.forEachIndexed { oi, optionText ->
                                val isCorrectOpt = oi == q.correctIndex
                                val isChosenOpt = oi == chosen
                                val isDimmed = answered && !isCorrectOpt && !isChosenOpt

                                val targetBorderColor = when {
                                    !answered -> editorial.borderSubtle
                                    isCorrectOpt -> editorial.correctText
                                    isChosenOpt -> editorial.wrongText
                                    else -> editorial.borderSubtle.copy(alpha = 0.45f)
                                }
                                val targetBgColor = when {
                                    !answered -> editorial.cardSurface
                                    isCorrectOpt -> editorial.correctBg
                                    isChosenOpt -> editorial.wrongBg
                                    else -> editorial.cardSurface.copy(alpha = 0.55f)
                                }
                                val targetBadgeBg = when {
                                    !answered -> editorial.elevatedSurface
                                    isCorrectOpt -> SemanticCorrectFill
                                    isChosenOpt -> SemanticWrongFill
                                    else -> editorial.elevatedSurface.copy(alpha = 0.6f)
                                }
                                val borderColor by animateColorAsState(
                                    targetValue = targetBorderColor,
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                    label = "opt_border_$oi"
                                )
                                val bgColor by animateColorAsState(
                                    targetValue = targetBgColor,
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                    label = "opt_bg_$oi"
                                )
                                val badgeBg by animateColorAsState(
                                    targetValue = targetBadgeBg,
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                    label = "opt_badge_$oi"
                                )
                                val badgeFg = if (answered && (isCorrectOpt || isChosenOpt)) {
                                    PureWhite
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
                                    border = BorderStroke(
                                        width = if (answered && (isCorrectOpt || isChosenOpt)) 1.5.dp else 1.dp,
                                        color = borderColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = !answered) { onSelectOption(oi) }
                                        .testTag("practice_option_$oi")
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
                                                text = badgeLabel,
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                                color = badgeFg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = optionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDimmed) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Separate GPU-composited Explanation Card (outside parent Card -> zero parent relayout stutter!)
                AnimatedVisibility(
                    visible = answered,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { fullHeight -> (fullHeight / 5).coerceAtMost(72) }
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 90))
                ) {
                    val isRight = chosen == q.correctIndex
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = editorial.explanationSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { clip = true }
                            .testTag("practice_rationale_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXPLANATION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isRight) {
                                        "Correct (+1.00)"
                                    } else {
                                        "Incorrect (${if (negativeMarking) "-$negLabel" else "0"}) · Ans ${'A' + q.correctIndex}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isRight) editorial.correctText else editorial.wrongText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(color = editorial.borderSubtle)
                            Text(
                                text = q.explanation.ifBlank { "No rationale provided." },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (q.isVerifiedSource) {
                                Text(
                                    text = "Source: ${q.sourceNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = editorial.correctText
                                )
                            }
                            PracticeOptionRationalesSection(
                                question = q,
                                selectedIndex = chosen,
                                canUseAiTutor = canUseAiTutor,
                                onRequestAiOptionBreakdown = { onExplainOptionsWithAi(currentItem, chosen) }
                            )
                            HorizontalDivider(color = editorial.borderSubtle)
                            Button(
                                onClick = { onExplainWithAi(currentItem, chosen) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = editorial.acidLime,
                                    contentColor = editorial.onAcidLime
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("practice_ai_tutor_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = if (canUseAiTutor) {
                                        "  Explain with AI Clinical Tutor"
                                    } else {
                                        "  Explain with AI Tutor · PRO"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onFlagQuestion(currentItem) },
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("practice_rationale_flag_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = " Flag Error",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { onAskDoubt(currentItem) },
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("practice_rationale_ask_doubt_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QuestionAnswer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = " Ask Doubt",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                val savedNote = questionNotes[q.id]
                if (savedNote != null) {
                    QuestionNoteBannerCard(
                        note = savedNote,
                        onEdit = { showNoteEditor = true }
                    )
                }
            }

            if (showNoteEditor) {
                QuestionNoteEditorDialog(
                    questionStem = q.questionText,
                    subject = q.subject,
                    existingNote = questionNotes[q.id]?.text.orEmpty(),
                    onSave = { text ->
                        onSaveQuestionNote(q.id, text)
                        showNoteEditor = false
                    },
                    onDelete = {
                        onDeleteQuestionNote(q.id)
                        showNoteEditor = false
                    },
                    onDismiss = { showNoteEditor = false }
                )
            }

            // Clean Bottom Navigation Strip with stable keys
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(
                    state = stripListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = session.questions,
                        key = { _, item -> item.id }
                    ) { idx, item ->
                        val ans = session.selectedAnswers[item.id]
                        val isCurrent = idx == session.currentIndex
                        val bg = when {
                            isCurrent -> editorial.acidLime
                            ans == item.question.correctIndex -> SemanticCorrectFill
                            ans != null -> SemanticWrongFill
                            else -> editorial.elevatedSurface
                        }
                        val fg = when {
                            isCurrent -> editorial.onAcidLime
                            ans != null -> PureWhite
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = if (!isCurrent && ans == null) BorderStroke(1.dp, editorial.borderSubtle) else null,
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
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("practice_prev_btn")
                    ) {
                        Text("Prev")
                    }

                    if (session.timedMode) {
                        OutlinedButton(
                            onClick = onRequestSubmit,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("practice_timed_submit_btn")
                        ) {
                            Text("Submit (${session.selectedAnswers.size}/${session.questions.size})")
                        }
                    }

                    if (session.currentIndex < session.questions.lastIndex) {
                        Button(
                            onClick = { onMoveIndex(session.currentIndex + 1) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("practice_next_btn")
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { if (session.timedMode) onRequestSubmit() else onExit() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("practice_finish_btn")
                        ) {
                            Text(if (session.timedMode) "Finish & Submit" else "Finish", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (session.showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Time's Up") },
            text = {
                Text("Timer ended. You answered ${session.selectedAnswers.size} of ${session.questions.size} questions.")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier.testTag("timeout_submit_btn")
                ) {
                    Text("View Report", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (session.showCompletionPrompt && !session.showTimeoutDialog) {
        val remainingQs = session.questions.size - session.selectedAnswers.size
        AlertDialog(
            onDismissRequest = onDismissSubmitPrompt,
            title = { Text("Submit Practice?") },
            text = {
                Text(
                    if (remainingQs == 0) {
                        "All ${session.questions.size} questions answered. View your report?"
                    } else {
                        "${session.selectedAnswers.size}/${session.questions.size} answered ($remainingQs unanswered). Submit now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier.testTag("confirm_timed_submit_btn")
                ) {
                    Text("Submit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSubmitPrompt) {
                    Text("Continue")
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit Practice?") },
            text = { Text("Your answered questions are already saved.") },
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
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimedPracticeSummaryView(
    session: PracticeSessionState,
    negativeMarking: Boolean,
    onChangeFilter: (CbtReviewFilter) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val (right, wrong, skipped) = remember(session.questions, session.selectedAnswers) {
        var r = 0
        var w = 0
        var s = 0
        session.questions.forEach { item ->
            val sel = session.selectedAnswers[item.id]
            when {
                sel == null -> s++
                sel == item.question.correctIndex -> r++
                else -> w++
            }
        }
        Triple(r, w, s)
    }
    val net = if (negativeMarking) right - (wrong * session.negativeMarks) else right.toDouble()
    val attempted = right + wrong
    val acc = if (attempted > 0) ((right.toFloat() / attempted) * 100f).toInt() else 0

    val filtered = remember(session.questions, session.selectedAnswers, session.reviewFilter) {
        session.questions.mapIndexedNotNull { idx, item ->
            val ans = session.selectedAnswers[item.id]
            val keep = when (session.reviewFilter) {
                CbtReviewFilter.ALL -> true
                CbtReviewFilter.WRONG -> ans != null && ans != item.question.correctIndex
                CbtReviewFilter.RIGHT -> ans == item.question.correctIndex
                CbtReviewFilter.SKIPPED -> ans == null
            }
            if (keep) Triple(idx, item, ans) else null
        }
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
            item(key = "summary_header") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialBannerSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
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
                                Text("SCORE REPORT", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = AcidLime)
                                Text(session.title, style = MaterialTheme.typography.titleLarge, color = EditorialBannerTitle, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onExit,
                                colors = ButtonDefaults.buttonColors(containerColor = AcidLime, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Net Score",
                                value = String.format(Locale.US, "%.2f", net),
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
                            MetricTile(label = "Correct", value = "$right", valueColor = EditorialBannerKpiSuccess, modifier = Modifier.weight(1f))
                            MetricTile(label = "Wrong", value = "$wrong", valueColor = EditorialBannerKpiDanger, modifier = Modifier.weight(1f))
                            MetricTile(label = "Skipped", value = "$skipped", valueColor = EditorialBannerTitle, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "summary_filters") {
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

            itemsIndexed(
                items = filtered,
                key = { _, (_, item, _) -> item.id }
            ) { _, (idx, item, ans) ->
                val q = item.question
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                Text("Q${idx + 1} · ${q.subject}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                PyqSourceBadge(question = q)
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
                            Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
