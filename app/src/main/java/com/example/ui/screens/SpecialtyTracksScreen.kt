package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.IbqCategory
import com.example.data.model.QuestionItem
import com.example.data.model.SpecialtyTrack
import com.example.data.model.SpecialtyTrackLogic
import com.example.data.model.SpecialtyTrackProjection
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialtyTracksScreen(
    allQuestions: List<QuestionItem>,
    initialTrack: SpecialtyTrack = SpecialtyTrack.CARDIOLOGY,
    onBack: () -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onOpenEcgStudio: () -> Unit,
    onOpenAbgInterpreter: () -> Unit,
    onOpenClinicalCalcs: () -> Unit,
    onOpenIbqHubCategory: (IbqCategory) -> Unit,
    onOpenFlashcardStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    var selectedTrack by rememberSaveable(initialTrack) { mutableStateOf(initialTrack) }

    val projections = remember(allQuestions) {
        SpecialtyTrackLogic.projectAllTracks(allQuestions)
    }
    val activeProjection = remember(projections, selectedTrack) {
        projections.firstOrNull { it.track == selectedTrack }
            ?: SpecialtyTrackLogic.projectTrack(allQuestions, selectedTrack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clinical Specialty Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Unified MCQs, IBQs, Tools & Weak Topics · Zero Duplication",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("specialty_tracks_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back from Specialty Tracks"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 980.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Specialty Track Selector Pills
                item(key = "specialty_selector") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        projections.forEach { proj ->
                            MonochromeFilterPill(
                                label = "${proj.track.shortTitle} (${proj.totalCount})",
                                selected = selectedTrack == proj.track,
                                onClick = { selectedTrack = proj.track },
                                modifier = Modifier.testTag("specialty_tab_${proj.track.id}")
                            )
                        }
                    }
                }

                // 2. Active Specialty Hero Card & Rapid 5-Min Revision CTA
                item(key = "specialty_hero_${activeProjection.track.id}") {
                    ActiveSpecialtyHeroCard(
                        projection = activeProjection,
                        onStartRapidRevision = {
                            val rapid = activeProjection.buildRapidRevisionDrill(10)
                            if (rapid.isNotEmpty()) {
                                onStartPractice(
                                    rapid,
                                    "5-Min Rapid Drill • ${activeProjection.track.shortTitle} (${rapid.size}Q)"
                                )
                            }
                        },
                        onStartFullPractice = {
                            val ordered = (activeProjection.weakQuestions + activeProjection.unseenQuestions + activeProjection.questions)
                                .distinctBy { it.id }
                            if (ordered.isNotEmpty()) {
                                onStartPractice(
                                    ordered,
                                    "${activeProjection.track.title} (${ordered.size}Q)"
                                )
                            }
                        },
                        onStartWeakDrill = {
                            if (activeProjection.weakQuestions.isNotEmpty()) {
                                onStartPractice(
                                    activeProjection.weakQuestions,
                                    "Weak Topics • ${activeProjection.track.shortTitle} (${activeProjection.weakQuestions.size}Q)"
                                )
                            }
                        },
                        onStartIbqDrill = {
                            if (activeProjection.ibqQuestions.isNotEmpty()) {
                                onStartPractice(
                                    activeProjection.ibqQuestions,
                                    "Visual IBQs • ${activeProjection.track.shortTitle} (${activeProjection.ibqQuestions.size}Q)"
                                )
                            }
                        }
                    )
                }

                // 3. Linked Clinical Tools & Rapid Recall for Active Specialty
                item(key = "specialty_tools_${activeProjection.track.id}") {
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
                            Text(
                                text = "LINKED CLINICAL STUDIO TOOLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = editorial.acidLimeForeground
                            )

                            when (activeProjection.track) {
                                SpecialtyTrack.CARDIOLOGY -> {
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.MonitorHeart,
                                        title = "ECG Learning Studio",
                                        subtitle = "14 calibrated ICU strips, 9-point breakdown & blinded drill",
                                        onClick = onOpenEcgStudio,
                                        testTag = "specialty_tool_ecg"
                                    )
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.Image,
                                        title = "ECG Visual Spotters (IBQ Hub)",
                                        subtitle = "VTach, VFib, AFib, STEMI, AV Block, Hyperkalemia & Torsades",
                                        onClick = { onOpenIbqHubCategory(IbqCategory.ECG) },
                                        testTag = "specialty_tool_ibq_ecg"
                                    )
                                }
                                SpecialtyTrack.CRITICAL_CARE -> {
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.Biotech,
                                        title = "ABG Interpreter & 12 ICU Cases",
                                        subtitle = "Winter's formula, mixed acid-base, oxygenation & Anion Gap",
                                        onClick = onOpenAbgInterpreter,
                                        testTag = "specialty_tool_abg"
                                    )
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.MonitorHeart,
                                        title = "ECG Learning Studio",
                                        subtitle = "ACLS rhythms, complete heart block & electrolyte strips",
                                        onClick = onOpenEcgStudio,
                                        testTag = "specialty_tool_icu_ecg"
                                    )
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.Image,
                                        title = "Chest X-Ray & Trauma Visuals",
                                        subtitle = "Tension pneumothorax, pleural effusion & Rule of Nines",
                                        onClick = { onOpenIbqHubCategory(IbqCategory.CLINICAL_IMAGING) },
                                        testTag = "specialty_tool_ibq_cxr"
                                    )
                                }
                                SpecialtyTrack.OBSTETRICS -> {
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.Image,
                                        title = "OBG & CTG Visual Spotters",
                                        subtitle = "Late & Variable decelerations, WHO Partograph & Cusco's speculum",
                                        onClick = { onOpenIbqHubCategory(IbqCategory.OBG_CTG) },
                                        testTag = "specialty_tool_ibq_obg"
                                    )
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.MedicalServices,
                                        title = "Clinical Calculators (EDD & Infusions)",
                                        subtitle = "Naegele's rule, drip rate, MgSO₄ & oxytocin dosing checks",
                                        onClick = onOpenClinicalCalcs,
                                        testTag = "specialty_tool_obg_calcs"
                                    )
                                }
                                SpecialtyTrack.PEDIATRICS -> {
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.MedicalServices,
                                        title = "Pediatric Dosage & IV Calculators",
                                        subtitle = "Weight-based mg/kg/day dosing, Holliday-Segar & drip rates",
                                        onClick = onOpenClinicalCalcs,
                                        testTag = "specialty_tool_peds_calcs"
                                    )
                                    LinkedToolActionRow(
                                        icon = Icons.Filled.Image,
                                        title = "Cannula, BMW & Device Spotters",
                                        subtitle = "22G Blue pediatric cannula, O₂ masks & BMW color codes",
                                        onClick = { onOpenIbqHubCategory(IbqCategory.DEVICES_COLOR) },
                                        testTag = "specialty_tool_ibq_peds"
                                    )
                                }
                            }

                            LinkedToolActionRow(
                                icon = Icons.Filled.Style,
                                title = "Rapid Recall Clinical Flashcards",
                                subtitle = "High-yield antidotes, lab ranges, mnemonics & bedside pearls",
                                onClick = onOpenFlashcardStudio,
                                testTag = "specialty_tool_flashcards"
                            )
                        }
                    }
                }

                // 4. All 4 Specialty Overview Cards for Quick Switching
                item(key = "all_tracks_header") {
                    Text(
                        text = "All Clinical Specialty Lenses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item(key = "all_tracks_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        projections.forEach { proj ->
                            val isSelected = proj.track == selectedTrack
                            Card(
                                onClick = { selectedTrack = proj.track },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) editorial.elevatedSurface else editorial.cardSurface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) editorial.acidLime else editorial.borderSubtle
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("specialty_overview_${proj.track.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) editorial.acidLime else editorial.elevatedSurface
                                        ) {
                                            Icon(
                                                imageVector = iconForTrack(proj.track),
                                                contentDescription = null,
                                                tint = if (isSelected) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = proj.track.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${proj.totalCount} MCQs · ${proj.ibqQuestions.size} IBQs · ${proj.weakQuestions.size} Weak",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (proj.attemptedCount > 0) "${proj.accuracyPercent}%" else "NEW",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) editorial.acidLimeForeground else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "specialty_bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveSpecialtyHeroCard(
    projection: SpecialtyTrackProjection,
    onStartRapidRevision: () -> Unit,
    onStartFullPractice: () -> Unit,
    onStartWeakDrill: () -> Unit,
    onStartIbqDrill: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val progress = if (projection.totalCount == 0) 0f else {
        (projection.attemptedCount.toFloat() / projection.totalCount.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("specialty_hero_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.acidLime
                    ) {
                        Icon(
                            imageVector = iconForTrack(projection.track),
                            contentDescription = null,
                            tint = editorial.onAcidLime,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = projection.track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = projection.track.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TRACK COVERAGE · ${projection.attemptedCount}/${projection.totalCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (projection.attemptedCount > 0) "${projection.accuracyPercent}% ACC" else "UNSTARTED",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = editorial.acidLimeForeground
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = editorial.acidLime,
                    trackColor = editorial.elevatedSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // Primary 5-Min Rapid Revision CTA
            Button(
                onClick = onStartRapidRevision,
                enabled = projection.totalCount > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("specialty_rapid_drill_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "5-Min Rapid Revision (${projection.totalCount.coerceAtMost(10)}Q)",
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary specialty actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartFullPractice,
                    enabled = projection.totalCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("specialty_full_practice_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All (${projection.totalCount})",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (projection.ibqQuestions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onStartIbqDrill,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("specialty_ibq_drill_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IBQs (${projection.ibqQuestions.size})",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (projection.weakQuestions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onStartWeakDrill,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("specialty_weak_drill_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = editorial.wrongText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Weak (${projection.weakQuestions.size})",
                            color = editorial.wrongText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedToolActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = editorial.acidLimeForeground,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "OPEN →",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = editorial.acidLimeForeground
            )
        }
    }
}

private fun iconForTrack(track: SpecialtyTrack): ImageVector = when (track) {
    SpecialtyTrack.CARDIOLOGY -> Icons.Filled.MonitorHeart
    SpecialtyTrack.CRITICAL_CARE -> Icons.Filled.LocalHospital
    SpecialtyTrack.OBSTETRICS -> Icons.Filled.PregnantWoman
    SpecialtyTrack.PEDIATRICS -> Icons.Filled.ChildCare
}
