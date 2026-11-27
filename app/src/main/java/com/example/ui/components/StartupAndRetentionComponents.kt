package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyFeaturedQuestion
import com.example.data.model.DailyGoalValidation
import com.example.data.model.OptionRationaleAnalyzer
import com.example.data.model.OptionRationaleDetail
import com.example.data.model.QotdCompletionState
import com.example.data.model.Question
import com.example.data.model.StartupNursingPearls
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import kotlinx.coroutines.delay

// ============================================================================
// 1. CALM BRANDED DASHBOARD SKELETON + NON-BLOCKING HIGH-YIELD PEARL BANNER
// ============================================================================

@Composable
fun DashboardLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val transition = rememberInfiniteTransition(label = "clinedge_skeleton")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    // Only reveal a helpful nursing pearl if genuine hydration lasts > 350ms (zero artificial delay)
    var showPearl by remember { mutableStateOf(false) }
    var dismissedPearl by remember { mutableStateOf(false) }
    var pearlIdx by remember { mutableIntStateOf((System.currentTimeMillis() / 10_000L).toInt()) }

    LaunchedEffect(Unit) {
        delay(350L)
        showPearl = true
        while (true) {
            delay(5000L)
            pearlIdx++
        }
    }

    val pulseColor = editorial.borderSubtle.copy(alpha = shimmerAlpha)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_loading_skeleton"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClinEdgeLogoMark(size = 24.dp)
                    Text(
                        text = "Preparing your study bank…",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                color = editorial.acidLime,
                trackColor = editorial.elevatedSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
            )
        }

        if (showPearl && !dismissedPearl) {
            item {
                val pearl = StartupNursingPearls.pearlForIndex(pearlIdx)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("startup_loading_pearl_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = editorial.acidLimeMuted,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = editorial.acidLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "HIGH-YIELD PEARL · ${pearl.tag}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = editorial.accentDustyRoseText
                            )
                            Text(
                                text = pearl.text,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { dismissedPearl = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("startup_loading_pearl_dismiss_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss nursing tip",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Skeleton: Hero Header + Daily Goal Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(pulseColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(pulseColor)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(136.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(pulseColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(pulseColor)
                        )
                    }
                }
            }
        }

        // Skeleton: QOTD + Quick Actions placeholders
        items(2) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.38f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(pulseColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(pulseColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(pulseColor)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. DAILY GOAL QUICK EDITOR DIALOG (PRESETS 10/20/30/50 + CUSTOM 5..200)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalQuickEditorDialog(
    currentGoal: Int,
    onSaveGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var selectedTarget by remember(currentGoal) { mutableIntStateOf(currentGoal) }
    var customInput by remember(currentGoal) { mutableStateOf(currentGoal.toString()) }
    val errorHint = DailyGoalValidation.validationHint(customInput)
    val parsedCustom = DailyGoalValidation.parseCustomTarget(customInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = editorial.cardSurface,
        title = {
            Text(
                text = "Daily Study Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Pick a daily question target that fits your shift or study schedule. Changing your target never penalizes your streak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyGoalValidation.PRESET_TARGETS.forEach { preset ->
                        val isSelected = selectedTarget == preset && parsedCustom == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) editorial.acidLime else editorial.elevatedSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) editorial.acidLime else editorial.borderSubtle
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedTarget = preset
                                    customInput = preset.toString()
                                }
                                .testTag("daily_goal_preset_$preset")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "$preset Qs",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customInput,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter { it.isDigit() }.take(3)
                        customInput = digitsOnly
                        DailyGoalValidation.parseCustomTarget(digitsOnly)?.let { valid ->
                            selectedTarget = valid
                        }
                    },
                    label = { Text("Custom target (${DailyGoalValidation.MIN_CUSTOM_TARGET}–${DailyGoalValidation.MAX_CUSTOM_TARGET} Qs)") },
                    singleLine = true,
                    isError = errorHint != null,
                    supportingText = {
                        if (errorHint != null) {
                            Text(text = errorHint, color = editorial.wrongText)
                        } else {
                            Text(text = "Updates immediately after every MCQ or Question of the Day.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_goal_custom_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTarget = parsedCustom ?: selectedTarget
                    onSaveGoal(finalTarget)
                },
                enabled = parsedCustom != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("daily_goal_save_btn")
            ) {
                Text("Save Target", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// 3. QUESTION OF THE DAY (QOTD) FEATURED DASHBOARD CARD
// ============================================================================

@Composable
fun QuestionOfTheDayCard(
    question: DailyFeaturedQuestion,
    completion: QotdCompletionState,
    todayDateKey: String,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val isAnswered = completion.isAnsweredFor(todayDateKey, question.id)
    val selectedIndex = if (isAnswered) completion.selectedIndex else null
    var showAllOptionRationales by remember(question.id, isAnswered) { mutableStateOf(false) }
    var showDeeperLearning by remember(question.id, isAnswered) { mutableStateOf(false) }

    val letters = listOf("A", "B", "C", "D")
    val optionDetails = remember(question, selectedIndex) {
        question.options.mapIndexed { idx, optText ->
            val r = question.optionRationales.firstOrNull { it.optionIndex == idx }
            OptionRationaleDetail(
                optionIndex = idx,
                label = letters.getOrElse(idx) { "${idx + 1}" },
                optionText = optText,
                isCorrect = idx == question.correctIndex,
                isUserSelected = selectedIndex != null && idx == selectedIndex,
                explanation = r?.whyCorrectOrWrong.orEmpty(),
                whenCorrectElsewhere = r?.whenAppliesElsewhere.orEmpty()
            )
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAnswered) editorial.borderSubtle else editorial.acidLime.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_qotd_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header badges
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
                        color = editorial.acidLime
                    ) {
                        Text(
                            text = "QUESTION OF THE DAY",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.onAcidLime,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Text(
                            text = question.difficulty.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isAnswered) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Question of the Day completed",
                        tint = if (completion.isCorrect) editorial.correctText else editorial.warningText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "${question.topicTag} · ${question.examTag}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Answer Choices
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { idx, optText ->
                    val letter = letters.getOrElse(idx) { "${idx + 1}" }
                    val isRightOpt = idx == question.correctIndex
                    val isPicked = selectedIndex == idx

                    val containerColor = when {
                        !isAnswered -> editorial.elevatedSurface
                        isRightOpt -> editorial.correctBg
                        isPicked -> editorial.wrongBg
                        else -> editorial.elevatedSurface
                    }
                    val borderColor = when {
                        !isAnswered -> editorial.borderSubtle
                        isRightOpt -> editorial.correctText
                        isPicked -> editorial.wrongText
                        else -> editorial.borderSubtle
                    }
                    val badgeBg = when {
                        !isAnswered -> editorial.cardSurface
                        isRightOpt -> editorial.correctText
                        isPicked -> editorial.wrongText
                        else -> editorial.cardSurface
                    }
                    val badgeFg = when {
                        !isAnswered -> MaterialTheme.colorScheme.onSurface
                        isRightOpt || isPicked -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isAnswered) { onSelectOption(idx) }
                            .testTag("qotd_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = badgeBg,
                                border = if (!isAnswered) BorderStroke(1.dp, editorial.borderSubtle) else null,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = badgeFg
                                    )
                                }
                            }
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isAnswered && (isRightOpt || isPicked)) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Post-Answer Structured Rationale, Per-Option Breakdown & Deeper Learning
            AnimatedVisibility(visible = isAnswered) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = editorial.borderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (completion.isCorrect) "SPOT ON · KEY ${letters.getOrElse(question.correctIndex) { "" }}" else "REVIEW KEY · ANS ${letters.getOrElse(question.correctIndex) { "" }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (completion.isCorrect) editorial.correctText else editorial.wrongText
                        )
                        Text(
                            text = "Counted toward today's goal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = question.conciseSummary,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Nursing Priority & Exam Pearl
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.explanationSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalHospital,
                                    contentDescription = null,
                                    tint = editorial.correctText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Nursing Priority: ${question.nursingPriority}",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = editorial.accentDustyRoseText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Exam Pearl: ${question.examPearl}",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = editorial.warningText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Common Trap: ${question.commonTrap}",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Expandable Per-Option Rationales
                    OutlinedButton(
                        onClick = { showAllOptionRationales = !showAllOptionRationales },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("qotd_toggle_option_rationales_btn")
                    ) {
                        Text(
                            text = if (showAllOptionRationales) "Hide Option-by-Option Rationale" else "Review All Options (A–D)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showAllOptionRationales) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showAllOptionRationales) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            optionDetails.forEach { detail ->
                                OptionRationaleItemCard(detail = detail)
                            }
                        }
                    }

                    // Expandable Deeper Learning
                    TextButton(
                        onClick = { showDeeperLearning = !showDeeperLearning },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("qotd_deeper_learning_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (showDeeperLearning) "Hide Deeper Clinical Context" else "Read Deeper Clinical Context",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showDeeperLearning) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showDeeperLearning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question.deeperLearning,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. HYBRID PER-OPTION RATIONALES SECTION FOR PRACTICE & REVIEW
// ============================================================================

@Composable
fun PracticeOptionRationalesSection(
    question: Question,
    selectedIndex: Int?,
    canUseAiTutor: Boolean,
    onRequestAiOptionBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    var expanded by remember(question.id) { mutableStateOf(false) }
    val optionDetails = remember(question.id, selectedIndex) {
        OptionRationaleAnalyzer.buildForQuestion(question = question, selectedIndex = selectedIndex)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, editorial.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("practice_toggle_all_options_rationale_btn")
        ) {
            Text(
                text = if (expanded) "Hide Option-by-Option Review" else "Review All Options (${question.options.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                optionDetails.forEach { detail ->
                    OptionRationaleItemCard(detail = detail)
                }

                OutlinedButton(
                    onClick = onRequestAiOptionBreakdown,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice_ai_option_breakdown_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = editorial.accentDustyRoseText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (canUseAiTutor) {
                            "AI Deep Option-by-Option Breakdown"
                        } else {
                            "AI Deep Option Breakdown · PRO"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRationaleItemCard(
    detail: OptionRationaleDetail
) {
    val editorial = MaterialTheme.editorialColors
    val borderColor = when {
        detail.isCorrect -> editorial.correctText.copy(alpha = 0.65f)
        detail.isUserSelected -> editorial.wrongText.copy(alpha = 0.65f)
        else -> editorial.borderSubtle
    }
    val statusLabel = when {
        detail.isCorrect && detail.isUserSelected -> "CORRECT · YOUR PICK"
        detail.isCorrect -> "CORRECT ANSWER"
        detail.isUserSelected -> "INCORRECT · YOUR PICK"
        else -> "DISTRACTOR"
    }
    val statusColor = when {
        detail.isCorrect -> editorial.correctText
        detail.isUserSelected -> editorial.wrongText
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (detail.isCorrect) editorial.correctBg.copy(alpha = 0.55f) else editorial.elevatedSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Option ${detail.label}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Text(
                text = detail.optionText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail.explanation,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (detail.whenCorrectElsewhere.isNotBlank()) {
                Text(
                    text = "When this applies: ${detail.whenCorrectElsewhere}",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = editorial.accentDustyRoseText
                )
            }
        }
    }
}
