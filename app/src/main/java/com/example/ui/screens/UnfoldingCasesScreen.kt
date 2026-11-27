package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BedsideVitals
import com.example.data.model.ClinicalCaseCompletionResult
import com.example.data.model.UnfoldingClinicalCase
import com.example.data.model.UnfoldingClinicalCaseCatalog
import com.example.data.model.VitalDeltaDirection
import com.example.data.model.VitalReading
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors

@Composable
fun UnfoldingCasesScreen(
    completedCaseIds: Set<String>,
    initialCaseId: String? = null,
    onCaseCompleted: (caseId: String, correctStages: Int, totalStages: Int) -> ClinicalCaseCompletionResult,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCase by remember(initialCaseId) {
        mutableStateOf(UnfoldingClinicalCaseCatalog.findById(initialCaseId))
    }
    var selectedSpecialty by remember { mutableStateOf(UnfoldingClinicalCaseCatalog.SPECIALTY_ALL) }

    BackHandler {
        if (activeCase != null) {
            activeCase = null
        } else {
            onBack()
        }
    }

    val currentCase = activeCase
    if (currentCase == null) {
        UnfoldingCaseLibraryView(
            selectedSpecialty = selectedSpecialty,
            completedCaseIds = completedCaseIds,
            onSelectSpecialty = { selectedSpecialty = it },
            onSelectCase = { activeCase = it },
            onBack = onBack,
            modifier = modifier
        )
    } else {
        UnfoldingCaseSimulatorView(
            clinicalCase = currentCase,
            isAlreadyCompleted = currentCase.id in completedCaseIds,
            onCompleteCase = { correctCount ->
                onCaseCompleted(currentCase.id, correctCount, currentCase.stages.size)
            },
            onNextCase = {
                val all = UnfoldingClinicalCaseCatalog.allCases
                val idx = all.indexOfFirst { it.id == currentCase.id }
                val next = all.getOrNull((idx + 1) % all.size)
                activeCase = next
            },
            onExitCase = { activeCase = null },
            modifier = modifier
        )
    }
}

@Composable
private fun UnfoldingCaseLibraryView(
    selectedSpecialty: String,
    completedCaseIds: Set<String>,
    onSelectSpecialty: (String) -> Unit,
    onSelectCase: (UnfoldingClinicalCase) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val allCases = UnfoldingClinicalCaseCatalog.allCases
    val filteredCases = remember(selectedSpecialty) {
        UnfoldingClinicalCaseCatalog.filterBySpecialty(selectedSpecialty)
    }
    val completedCount = remember(completedCaseIds) {
        allCases.count { it.id in completedCaseIds }
    }
    val totalStages = remember { allCases.sumOf { it.stages.size } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Editorial Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("unfolding_cases_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NORCET Mains Clinical Cases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount/${allCases.size} cases mastered · $totalStages bedside stages",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = editorial.acidLimeMuted,
                border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "OFFLINE",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = editorial.acidLimeForeground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        HorizontalDivider(color = editorial.borderSubtle)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Mastery Banner
            item(key = "unfolding_hero") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                shape = RoundedCornerShape(12.dp),
                                color = editorial.acidLime,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.MonitorHeart,
                                        contentDescription = null,
                                        tint = editorial.onAcidLime,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unfolding Bedside Simulator",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Track evolving vitals across 3–4 sequential clinical decisions per patient — engineered for AIIMS NORCET Stage II Mains.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AcidLimeProgressBar(
                            progress = if (allCases.isNotEmpty()) completedCount.toFloat() / allCases.size else 0f,
                            height = 6.dp
                        )
                    }
                }
            }

            // Specialty Filter Pills
            item(key = "unfolding_specialty_filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnfoldingClinicalCaseCatalog.specialties.forEach { spec ->
                        MonochromeFilterPill(
                            label = spec,
                            selected = spec == selectedSpecialty,
                            onClick = { onSelectSpecialty(spec) },
                            modifier = Modifier.testTag("case_filter_${spec.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Case Cards
            items(
                items = filteredCases,
                key = { it.id },
                contentType = { "clinical_case_card" }
            ) { clinicalCase ->
                val isCompleted = clinicalCase.id in completedCaseIds
                Card(
                    onClick = { onSelectCase(clinicalCase) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) editorial.acidLime.copy(alpha = 0.55f) else editorial.borderSubtle
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clinical_case_card_${clinicalCase.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    color = editorial.wrongBg
                                ) {
                                    Text(
                                        text = clinicalCase.acuityBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = editorial.wrongText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = editorial.elevatedSurface,
                                    border = BorderStroke(1.dp, editorial.borderSubtle)
                                ) {
                                    Text(
                                        text = clinicalCase.specialty,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (isCompleted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Case mastered",
                                        tint = editorial.acidLimeForeground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Mastered",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = editorial.acidLimeForeground
                                    )
                                }
                            }
                        }

                        Text(
                            text = clinicalCase.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = clinicalCase.patientSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${clinicalCase.stages.size} Stages · ~${clinicalCase.estimatedMinutes} min · ${clinicalCase.examTag}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompleted) editorial.elevatedSurface else editorial.acidLime,
                                border = if (isCompleted) BorderStroke(1.dp, editorial.borderSubtle) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Filled.Replay else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCompleted) MaterialTheme.colorScheme.onSurface else editorial.onAcidLime,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isCompleted) "Replay" else "Start",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface else editorial.onAcidLime
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

@Composable
private fun UnfoldingCaseSimulatorView(
    clinicalCase: UnfoldingClinicalCase,
    isAlreadyCompleted: Boolean,
    onCompleteCase: (correctStages: Int) -> ClinicalCaseCompletionResult,
    onNextCase: () -> Unit,
    onExitCase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    var stageIndex by remember(clinicalCase.id) { mutableIntStateOf(0) }
    val selectedByStage = remember(clinicalCase.id) { mutableStateMapOf<Int, Int>() }
    var completionResult by remember(clinicalCase.id) { mutableStateOf<ClinicalCaseCompletionResult?>(null) }
    val bodyScroll = rememberScrollState()

    LaunchedEffect(clinicalCase.id, stageIndex) {
        bodyScroll.scrollTo(0)
    }

    val debrief = completionResult
    if (debrief != null) {
        UnfoldingCaseDebriefView(
            clinicalCase = clinicalCase,
            result = debrief,
            selectedByStage = selectedByStage,
            onReplay = {
                selectedByStage.clear()
                stageIndex = 0
                completionResult = null
            },
            onNextCase = onNextCase,
            onExit = onExitCase,
            modifier = modifier
        )
        return
    }

    val stage = clinicalCase.stages.getOrNull(stageIndex) ?: return
    val selectedOption = selectedByStage[stageIndex]
    val isAnswered = selectedOption != null
    val isLastStage = stageIndex >= clinicalCase.stages.lastIndex
    val progress = (stageIndex + 1).toFloat() / clinicalCase.stages.size.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Case Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitCase,
                modifier = Modifier.testTag("case_sim_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit clinical case")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clinicalCase.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Stage ${stage.stageNumber}/${clinicalCase.stages.size} · ${stage.elapsedTimeLabel} · ${stage.stageTitle}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Text(
                    text = "${stageIndex + 1}/${clinicalCase.stages.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        AcidLimeProgressBar(
            progress = progress,
            height = 4.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // STICKY BEDSIDE PATIENT CHART & EVOLVING VITALS HEADER
        StickyBedsideVitalsHeader(
            patientSummary = clinicalCase.patientSummary,
            vitals = stage.vitals,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = editorial.borderSubtle)

        // Scrollable Decision Body
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(bodyScroll)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bedside Evolution Narrative
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MedicalServices,
                            contentDescription = null,
                            tint = editorial.acidLimeForeground,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BEDSIDE EVOLUTION · ${stage.elapsedTimeLabel}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.acidLimeForeground
                        )
                    }
                    Text(
                        text = stage.clinicalUpdate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Priority Question Prompt
            Text(
                text = stage.questionPrompt,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Options
            stage.options.forEachIndexed { idx, optText ->
                val isSelected = selectedOption == idx
                val isCorrectOption = idx == stage.correctIndex
                val cardBg = when {
                    !isAnswered -> editorial.cardSurface
                    isCorrectOption -> editorial.correctBg
                    isSelected -> editorial.wrongBg
                    else -> editorial.cardSurface
                }
                val borderColor = when {
                    !isAnswered -> editorial.borderSubtle
                    isCorrectOption -> editorial.correctText.copy(alpha = 0.65f)
                    isSelected -> editorial.wrongText.copy(alpha = 0.65f)
                    else -> editorial.borderSubtle
                }
                val badgeBg = when {
                    !isAnswered -> editorial.elevatedSurface
                    isCorrectOption -> editorial.correctText
                    isSelected -> editorial.wrongText
                    else -> editorial.elevatedSurface
                }
                val badgeTextColor = when {
                    !isAnswered -> MaterialTheme.colorScheme.onSurface
                    isCorrectOption || isSelected -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(if (isSelected || (isAnswered && isCorrectOption)) 1.5.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isAnswered) {
                            selectedByStage[stageIndex] = idx
                        }
                        .testTag("case_option_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${'A' + idx}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || (isAnswered && isCorrectOption)) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
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

            // Clinical Rationale & Bedside Pearl Reveal
            AnimatedVisibility(
                visible = isAnswered,
                enter = fadeIn() + expandVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.cardSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (selectedOption == stage.correctIndex) {
                                    "CORRECT CLINICAL JUDGMENT"
                                } else {
                                    "PRIORITY RATIONALE (CORRECT: ${'A' + stage.correctIndex})"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOption == stage.correctIndex) editorial.correctText else editorial.wrongText
                            )
                            Text(
                                text = stage.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.acidLimeMuted,
                        border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = editorial.acidLimeForeground,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "NORCET MAINS PEARL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = editorial.acidLimeForeground
                                )
                                Text(
                                    text = stage.nursingPearl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Action Bar
        if (isAnswered) {
            HorizontalDivider(color = editorial.borderSubtle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (!isLastStage) {
                            stageIndex += 1
                        } else {
                            val correctCount = clinicalCase.stages.indices.count { i ->
                                selectedByStage[i] == clinicalCase.stages[i].correctIndex
                            }
                            completionResult = onCompleteCase(correctCount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("case_next_stage_btn")
                ) {
                    Text(
                        text = if (!isLastStage) {
                            "Evolve Patient to Stage ${stageIndex + 2}"
                        } else if (isAlreadyCompleted) {
                            "Complete Case Debrief"
                        } else {
                            "Complete Case & Claim Mastery XP"
                        },
                        style = MaterialTheme.typography.titleSmall,
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

@Composable
private fun StickyBedsideVitalsHeader(
    patientSummary: String,
    vitals: BedsideVitals,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val tiles = remember(vitals) { vitals.asTileList() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = patientSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Two neat rows of 3 monospace bedside telemetry tiles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tiles.chunked(3).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowTiles.forEach { reading ->
                            VitalTelemetryTile(
                                reading = reading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Key ECG / Lab / Bedside Clue Strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = editorial.wrongText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = vitals.keyClue,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalTelemetryTile(
    reading: VitalReading,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val bg = if (reading.isCritical) editorial.wrongBg else editorial.elevatedSurface
    val border = if (reading.isCritical) editorial.wrongText.copy(alpha = 0.55f) else editorial.borderSubtle
    val valueColor = if (reading.isCritical) editorial.wrongText else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reading.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                reading.delta?.let { d ->
                    val deltaColor = when (reading.direction) {
                        VitalDeltaDirection.CRITICAL -> editorial.wrongText
                        VitalDeltaDirection.UP -> if (reading.isCritical) editorial.wrongText else editorial.correctText
                        VitalDeltaDirection.DOWN -> if (reading.isCritical) editorial.wrongText else editorial.acidLimeForeground
                        VitalDeltaDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontSize = 9.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = reading.value,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reading.unit.isNotBlank()) {
                    Text(
                        text = reading.unit,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun UnfoldingCaseDebriefView(
    clinicalCase: UnfoldingClinicalCase,
    result: ClinicalCaseCompletionResult,
    selectedByStage: Map<Int, Int>,
    onReplay: () -> Unit,
    onNextCase: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val accuracy = if (result.totalStages > 0) {
        ((result.correctStages.toFloat() / result.totalStages) * 100f).toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit, modifier = Modifier.testTag("debrief_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to cases")
            }
            Text(
                text = "Bedside Case Debrief",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
            border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.acidLime,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = editorial.onAcidLime,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clinicalCase.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${result.correctStages}/${result.totalStages} stages accurate ($accuracy%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = editorial.acidLimeForeground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (result.earnedXp > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = editorial.acidLimeMuted,
                        border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+${result.earnedXp} XP Earned · Case Mastered",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.acidLimeForeground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Replay completed for clinical mastery reinforcement.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stage-by-stage breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
            border = BorderStroke(1.dp, editorial.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Stage-by-Stage Decisions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                clinicalCase.stages.forEachIndexed { idx, st ->
                    val correct = selectedByStage[idx] == st.correctIndex
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (correct) Icons.Filled.CheckCircle else Icons.Filled.Close,
                            contentDescription = if (correct) "Correct" else "Missed",
                            tint = if (correct) editorial.correctText else editorial.wrongText,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stage ${st.stageNumber}: ${st.stageTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = st.nursingPearl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // High-Yield Clinical Takeaways
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
            border = BorderStroke(1.dp, editorial.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "NORCET Mains Clinical Pearls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                clinicalCase.debriefTakeaways.forEach { pearl ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", fontWeight = FontWeight.Bold, color = editorial.acidLimeForeground)
                        Text(
                            text = pearl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
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
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("debrief_replay_btn")
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replay", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onNextCase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .heightIn(min = 50.dp)
                    .testTag("debrief_next_case_btn")
            ) {
                Text("Next Case", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
