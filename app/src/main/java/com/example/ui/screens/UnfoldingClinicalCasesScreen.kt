package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedsideVitals
import com.example.data.model.ClinicalCaseStage
import com.example.data.model.UnfoldingClinicalCase
import com.example.data.model.UnfoldingClinicalCasesCatalog
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnfoldingClinicalCasesScreen(
    completedCaseIds: Set<String>,
    onCaseCompleted: (caseId: String, correctStages: Int, totalStages: Int) -> Unit,
    onBack: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val cases = remember { UnfoldingClinicalCasesCatalog.ALL_CASES }

    var selectedSpecialty by remember { mutableStateOf("All") }
    var activeCase by remember { mutableStateOf<UnfoldingClinicalCase?>(null) }
    var stageIndex by remember { mutableIntStateOf(0) }
    val selectedOptionByStage = remember { mutableStateMapOf<Int, Int>() }
    var showingDebrief by remember { mutableStateOf(false) }
    var earnedXpForRun by remember { mutableIntStateOf(0) }

    fun resetCaseRun(target: UnfoldingClinicalCase?) {
        activeCase = target
        stageIndex = 0
        selectedOptionByStage.clear()
        showingDebrief = false
        earnedXpForRun = 0
    }

    BackHandler {
        if (activeCase != null) {
            resetCaseRun(null)
        } else {
            onBack()
        }
    }

    val filteredCases = remember(selectedSpecialty, cases) {
        if (selectedSpecialty == "All") cases
        else cases.filter { it.specialty == selectedSpecialty }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (activeCase != null) resetCaseRun(null) else onBack()
                            },
                            modifier = Modifier.testTag("clinical_cases_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        val current = activeCase
                        Column {
                            Text(
                                text = current?.title ?: "Unfolding Clinical Cases",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (current == null) {
                                    "NORCET Stage II Mains Bedside Simulator · ${completedCaseIds.size}/${cases.size} Mastered"
                                } else if (showingDebrief) {
                                    "Case Debrief Summary · ${current.specialty}"
                                } else {
                                    "Stage ${stageIndex + 1} of ${current.stages.size} · ${current.stages[stageIndex].elapsedTimeLabel}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
                HorizontalDivider(color = editorial.borderSubtle)
            }
        }
    ) { innerPadding ->
        val currentCase = activeCase
        if (currentCase == null) {
            // Library View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("clinical_cases_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "cases_hero") {
                    ClinicalCasesHeroBanner(
                        masteredCount = completedCaseIds.size,
                        totalCases = cases.size
                    )
                }

                item(key = "cases_filter_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnfoldingClinicalCasesCatalog.FILTER_SPECIALTIES.forEach { specialty ->
                            val selected = selectedSpecialty == specialty
                            FilterChip(
                                selected = selected,
                                onClick = { selectedSpecialty = specialty },
                                label = {
                                    Text(
                                        text = specialty,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = editorial.acidLime,
                                    selectedLabelColor = editorial.onAcidLime
                                ),
                                modifier = Modifier.testTag("cases_filter_${specialty.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }

                items(
                    items = filteredCases,
                    key = { it.id }
                ) { clinicalCase ->
                    ClinicalCaseLibraryCard(
                        clinicalCase = clinicalCase,
                        isMastered = clinicalCase.id in completedCaseIds,
                        onStart = { resetCaseRun(clinicalCase) }
                    )
                }

                item(key = "cases_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (showingDebrief) {
            // Case Debrief Summary View
            val correctCount = currentCase.stages.indices.count { idx ->
                selectedOptionByStage[idx] == currentCase.stages[idx].correctIndex
            }
            CaseDebriefSummaryView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                clinicalCase = currentCase,
                correctStages = correctCount,
                earnedXp = earnedXpForRun,
                onReplay = { resetCaseRun(currentCase) },
                onBackToLibrary = { resetCaseRun(null) }
            )
        } else {
            // Live Unfolding Simulator View with Sticky Patient Chart & Vitals Header
            val currentStage = currentCase.stages[stageIndex]
            val selectedOpt = selectedOptionByStage[stageIndex]
            val isAnswered = selectedOpt != null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Sticky Patient Chart + Evolving Bedside Vitals Header
                StickyPatientChartHeader(
                    clinicalCase = currentCase,
                    stage = currentStage,
                    stageIndex = stageIndex,
                    totalStages = currentCase.stages.size
                )

                // Scrollable Stage Clinical Narrative + Options + Rationale
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stage header + elapsed time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAGE ${currentStage.stageNumber}: ${currentStage.stageTitle.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = editorial.acidLimeForeground,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle)
                        ) {
                            Text(
                                text = currentStage.elapsedTimeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Clinical Evolution Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.cardSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = currentStage.clinicalUpdate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = editorial.elevatedSurface,
                                border = BorderStroke(1.dp, editorial.borderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Science,
                                        contentDescription = "Lab and monitor findings",
                                        tint = editorial.acidLimeForeground,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = currentStage.labOrMonitorClue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = MonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }

                    // Decision Prompt
                    Text(
                        text = currentStage.prompt,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 21.sp
                    )

                    // 4 Options
                    val letters = listOf("A", "B", "C", "D")
                    currentStage.options.forEachIndexed { idx, optionText ->
                        val isSelected = selectedOpt == idx
                        val isCorrectOption = idx == currentStage.correctIndex
                        val borderColor = when {
                            !isAnswered -> editorial.borderSubtle
                            isCorrectOption -> editorial.correctText
                            isSelected -> editorial.wrongText
                            else -> editorial.borderSubtle
                        }
                        val bgColor = when {
                            !isAnswered -> editorial.cardSurface
                            isCorrectOption -> editorial.correctBg
                            isSelected -> editorial.wrongBg
                            else -> editorial.cardSurface.copy(alpha = 0.6f)
                        }

                        Surface(
                            onClick = {
                                if (!isAnswered) {
                                    selectedOptionByStage[stageIndex] = idx
                                }
                            },
                            enabled = !isAnswered,
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(if (isAnswered && (isCorrectOption || isSelected)) 1.5.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("case_stage_opt_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        isAnswered && isCorrectOption -> editorial.correctText
                                        isAnswered && isSelected -> editorial.wrongText
                                        else -> editorial.elevatedSurface
                                    }
                                ) {
                                    Text(
                                        text = letters.getOrElse(idx) { "${idx + 1}" },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnswered && (isCorrectOption || isSelected)) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAnswered && isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Correct choice",
                                        tint = editorial.correctText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isAnswered && isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Incorrect choice",
                                        tint = editorial.wrongText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Instant Clinical Rationale + Pearl
                    AnimatedVisibility(visible = isAnswered, enter = fadeIn()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.acidLimeForeground.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedOpt == currentStage.correctIndex) {
                                        "CORRECT CLINICAL PRIORITY"
                                    } else {
                                        "CLINICAL PRIORITY REVIEW"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedOpt == currentStage.correctIndex) {
                                        editorial.correctText
                                    } else {
                                        editorial.wrongText
                                    }
                                )
                                Text(
                                    text = currentStage.rationale,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 21.sp
                                )
                                HorizontalDivider(color = editorial.borderSubtle)
                                Text(
                                    text = "HIGH-YIELD PEARL: ${currentStage.criticalPearl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = editorial.acidLimeForeground,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Bottom Action Bar
                if (isAnswered) {
                    HorizontalDivider(color = editorial.borderSubtle)
                    val isLastStage = stageIndex >= currentCase.stages.lastIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isLastStage) {
                                    stageIndex += 1
                                } else {
                                    val correctCount = currentCase.stages.indices.count { idx ->
                                        selectedOptionByStage[idx] == currentCase.stages[idx].correctIndex
                                    }
                                    val wasAlreadyCompleted = currentCase.id in completedCaseIds
                                    earnedXpForRun = UnfoldingClinicalCasesCatalog.xpForCaseCompletion(
                                        correctStages = correctCount,
                                        totalStages = currentCase.stages.size,
                                        alreadyCompleted = wasAlreadyCompleted
                                    )
                                    onCaseCompleted(currentCase.id, correctCount, currentCase.stages.size)
                                    showingDebrief = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("case_next_stage_btn")
                        ) {
                            Text(
                                text = if (isLastStage) {
                                    "Complete Case & View Debrief"
                                } else {
                                    "Proceed to Stage ${stageIndex + 2} (Vitals Evolve)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalCasesHeroBanner(
    masteredCount: Int,
    totalCases: Int
) {
    val editorial = MaterialTheme.editorialColors
    val progress = if (totalCases > 0) masteredCount.toFloat() / totalCases.toFloat() else 0f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = editorial.cardSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = editorial.acidLime
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        tint = editorial.onAcidLime,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NORCET STAGE II MAINS SIMULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = editorial.acidLimeForeground
                    )
                    Text(
                        text = "10 Multi-Step Bedside Clinical Cases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Track evolving vitals across 3–4 linked bedside stages — priority stabilization, drug contraindications, antidote rescue, and post-intervention evaluation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cases Mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$masteredCount / $totalCases",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = editorial.acidLimeForeground
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = editorial.acidLimeForeground,
                    trackColor = editorial.elevatedSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ClinicalCaseLibraryCard(
    clinicalCase: UnfoldingClinicalCase,
    isMastered: Boolean,
    onStart: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        onClick = onStart,
        shape = RoundedCornerShape(14.dp),
        color = editorial.cardSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isMastered) editorial.correctText.copy(alpha = 0.5f) else editorial.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clinical_case_card_${clinicalCase.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (clinicalCase.acuityTag == "CRITICAL") {
                            editorial.wrongBg
                        } else {
                            editorial.warningText.copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = clinicalCase.acuityTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (clinicalCase.acuityTag == "CRITICAL") {
                                editorial.wrongText
                            } else {
                                editorial.warningText
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface
                    ) {
                        Text(
                            text = "${clinicalCase.specialty} · ${clinicalCase.stages.size} Stages",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                if (isMastered) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Mastered",
                            tint = editorial.correctText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Mastered",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = editorial.correctText
                        )
                    }
                }
            }

            Text(
                text = clinicalCase.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${clinicalCase.patientProfile} — ${clinicalCase.chiefComplaint}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (isMastered) "Replay Case →" else "Start Bedside Case →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = editorial.acidLimeForeground
                )
            }
        }
    }
}

@Composable
private fun StickyPatientChartHeader(
    clinicalCase: UnfoldingClinicalCase,
    stage: ClinicalCaseStage,
    stageIndex: Int,
    totalStages: Int
) {
    val editorial = MaterialTheme.editorialColors
    val vitals: BedsideVitals = stage.vitals

    Surface(
        color = editorial.elevatedSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sticky_patient_chart_header")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = editorial.acidLimeForeground,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = clinicalCase.patientProfile,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${stageIndex + 1}/$totalStages",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = editorial.acidLimeForeground
                )
            }

            // 6-Tile Bedside Vitals Grid (2 rows x 3 cols)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VitalTile(label = "HR", value = vitals.hr, modifier = Modifier.weight(1f))
                VitalTile(label = "BP", value = vitals.bp, modifier = Modifier.weight(1f))
                VitalTile(label = "SpO2", value = vitals.spo2, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VitalTile(label = "RR", value = vitals.rr, modifier = Modifier.weight(1f))
                VitalTile(label = "TEMP", value = vitals.temp, modifier = Modifier.weight(1f))
                VitalTile(label = "GCS", value = vitals.gcs, modifier = Modifier.weight(1f))
            }

            // Delta Chips (evolving physiological indicators)
            if (vitals.deltaChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vitals.deltaChips.forEach { chip ->
                        val isAlert = chip.contains("↓") || chip.contains("↑") ||
                            chip.contains("Seizure", ignoreCase = true) ||
                            chip.contains("Absent", ignoreCase = true) ||
                            chip.contains("Shock", ignoreCase = true)
                        val isPositive = chip.contains("✓")
                        val chipColor = when {
                            isPositive -> editorial.correctText
                            isAlert -> editorial.wrongText
                            else -> editorial.acidLimeForeground
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = chipColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = editorial.borderSubtle)
}

@Composable
private fun VitalTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = editorial.cardSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CaseDebriefSummaryView(
    modifier: Modifier = Modifier,
    clinicalCase: UnfoldingClinicalCase,
    correctStages: Int,
    earnedXp: Int,
    onReplay: () -> Unit,
    onBackToLibrary: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val totalStages = clinicalCase.stages.size
    val scorePct = UnfoldingClinicalCasesCatalog.computeCaseScorePercent(correctStages, totalStages)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("case_debrief_summary"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = editorial.cardSurface,
            border = BorderStroke(1.dp, editorial.acidLimeForeground.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = editorial.acidLimeForeground,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    text = "CASE DEBRIEF COMPLETE",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = editorial.acidLimeForeground
                )
                Text(
                    text = "$correctStages / $totalStages Clinical Priorities ($scorePct%)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (earnedXp > 0) editorial.acidLime else editorial.elevatedSurface
                ) {
                    Text(
                        text = if (earnedXp > 0) "+$earnedXp XP Awarded" else "Practice Replay · 0 XP (Already Mastered)",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (earnedXp > 0) editorial.onAcidLime else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = editorial.cardSurface,
            border = BorderStroke(1.dp, editorial.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "NORCET MAINS CLINICAL PEARLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = editorial.acidLimeForeground
                )
                clinicalCase.debriefPearls.forEachIndexed { idx, pearl ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = editorial.acidLimeForeground
                        )
                        Text(
                            text = pearl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onReplay,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("case_replay_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replay", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onBackToLibrary,
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .heightIn(min = 50.dp)
                    .testTag("case_back_to_library_btn")
            ) {
                Text("All 10 Cases", fontWeight = FontWeight.Bold)
            }
        }
    }
}
