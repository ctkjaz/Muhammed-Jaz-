package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EcgCaseItem
import com.example.data.model.EcgCategory
import com.example.data.model.EcgNinePointBreakdown
import com.example.data.model.EcgStudioCatalog
import com.example.data.model.EcgUrgency
import com.example.data.model.EcgWaveformEngine
import com.example.data.model.EcgWaveformPattern
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
fun EcgLearningStudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPracticeAnswer: ((String, Boolean) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val editorial = MaterialTheme.editorialColors

    // 0 = Atlas & 9-Point Study, 1 = Interpret-Before-Reveal Practice
    var activeMode by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<EcgCategory?>(null) }
    val filteredCases = remember(selectedCategory) {
        EcgStudioCatalog.casesForCategory(selectedCategory)
    }
    var selectedCaseId by remember { mutableStateOf(EcgStudioCatalog.ALL_CASES.first().id) }
    val activeCase = remember(selectedCaseId, filteredCases) {
        filteredCases.firstOrNull { it.id == selectedCaseId } ?: filteredCases.first()
    }

    // Practice state
    val practiceDeck = remember { EcgStudioCatalog.buildPracticeDeck() }
    var quizIndex by remember { mutableIntStateOf(0) }
    val quizAnswers = remember { mutableStateMapOf<String, Int>() }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("ecg_studio_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ECG Learning Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${EcgStudioCatalog.ALL_CASES.size} ICU strips · Calibrated 25 mm/s grid · 9-point breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = editorial.borderSubtle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "ecg_mode_switch") {
                SegmentedPillBar(
                    options = listOf(0, 1),
                    selected = activeMode,
                    labelOf = { if (it == 0) "Rhythm Atlas (14)" else "Interpret & Reveal" },
                    onSelect = { activeMode = it },
                    testTagOf = { "ecg_studio_mode_$it" }
                )
            }

            if (activeMode == 0) {
                item(key = "ecg_category_filter") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonochromeFilterPill(
                            label = "All (${EcgStudioCatalog.ALL_CASES.size})",
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            modifier = Modifier.testTag("ecg_cat_all")
                        )
                        EcgCategory.entries.forEach { cat ->
                            MonochromeFilterPill(
                                label = cat.label,
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    val firstInCat = EcgStudioCatalog.casesForCategory(cat).firstOrNull()
                                    if (firstInCat != null) selectedCaseId = firstInCat.id
                                },
                                modifier = Modifier.testTag("ecg_cat_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                item(key = "ecg_case_selector") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCases.forEach { item ->
                            MonochromeFilterPill(
                                label = item.shortLabel,
                                selected = item.id == activeCase.id,
                                onClick = { selectedCaseId = item.id },
                                modifier = Modifier.testTag("ecg_case_pill_${item.id}")
                            )
                        }
                    }
                }

                item(key = "ecg_atlas_canvas_${activeCase.id}") {
                    EcgStripCard(
                        pattern = activeCase.pattern,
                        headerTitle = activeCase.title,
                        leadLabel = activeCase.leadLabel,
                        bpmBadge = activeCase.displayBpm,
                        urgency = activeCase.urgency,
                        isBlinded = false
                    )
                }

                item(key = "ecg_atlas_cue_${activeCase.id}") {
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
                                text = "Bedside Clinical Scenario",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeCase.clinicalCue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = editorial.elevatedSurface,
                                border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "HIGH-YIELD NORCET PEARL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = editorial.correctText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeCase.examPearl,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "ecg_nine_point_${activeCase.id}") {
                    EcgNinePointBreakdownCard(breakdown = activeCase.breakdown)
                }
            } else {
                // INTERPRET-BEFORE-REVEAL PRACTICE MODE
                val currentQ = practiceDeck[quizIndex.coerceIn(0, practiceDeck.lastIndex)]
                val chosenOpt = quizAnswers[currentQ.caseItem.id]
                val revealed = chosenOpt != null
                val totalAnswered = quizAnswers.size
                val totalCorrect = practiceDeck.count { quizAnswers[it.caseItem.id] == it.correctIndex }

                item(key = "ecg_quiz_status") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strip ${quizIndex + 1} / ${practiceDeck.size}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Score: $totalCorrect / $totalAnswered",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                color = editorial.correctText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item(key = "ecg_quiz_canvas_${currentQ.caseItem.id}_$revealed") {
                    EcgStripCard(
                        pattern = currentQ.caseItem.pattern,
                        headerTitle = if (revealed) currentQ.caseItem.title else "Identify This ECG Strip",
                        leadLabel = currentQ.caseItem.leadLabel,
                        bpmBadge = if (revealed) currentQ.caseItem.displayBpm else "Interpret First",
                        urgency = if (revealed) currentQ.caseItem.urgency else null,
                        isBlinded = !revealed
                    )
                }

                item(key = "ecg_quiz_options_${currentQ.caseItem.id}") {
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
                                text = "Bedside Clue",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentQ.caseItem.clinicalCue,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            currentQ.options.forEachIndexed { idx, optLabel ->
                                val isChosen = chosenOpt == idx
                                val isRight = idx == currentQ.correctIndex
                                val bgColor = when {
                                    !revealed -> editorial.elevatedSurface
                                    isRight -> editorial.correctBg
                                    isChosen -> editorial.wrongBg
                                    else -> editorial.elevatedSurface
                                }
                                val borderColor = when {
                                    !revealed -> editorial.borderSubtle
                                    isRight -> editorial.correctText.copy(alpha = 0.65f)
                                    isChosen -> editorial.wrongText.copy(alpha = 0.65f)
                                    else -> editorial.borderSubtle
                                }

                                Surface(
                                    onClick = {
                                        if (!revealed) {
                                            quizAnswers[currentQ.caseItem.id] = idx
                                            onPracticeAnswer?.invoke(currentQ.caseItem.id, idx == currentQ.correctIndex)
                                        }
                                    },
                                    enabled = !revealed,
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("ecg_quiz_opt_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${('A' + idx)}.",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = optLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (revealed && isRight) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (revealed && isRight) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Correct rhythm",
                                                tint = editorial.correctText,
                                                modifier = Modifier.size(18.dp)
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
                                    onClick = { quizIndex = (quizIndex - 1).coerceAtLeast(0) },
                                    enabled = quizIndex > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ecg_quiz_prev_btn")
                                ) {
                                    Text("Previous")
                                }
                                Button(
                                    onClick = {
                                        if (quizIndex < practiceDeck.lastIndex) {
                                            quizIndex++
                                        } else {
                                            quizAnswers.clear()
                                            quizIndex = 0
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("ecg_quiz_next_btn")
                                ) {
                                    Text(if (quizIndex < practiceDeck.lastIndex) "Next Strip" else "Restart Deck")
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (revealed) {
                    item(key = "ecg_quiz_pearl_${currentQ.caseItem.id}") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (chosenOpt == currentQ.correctIndex) editorial.correctBg else editorial.wrongBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (chosenOpt == currentQ.correctIndex) "Correct! ${currentQ.caseItem.title}" else "Answer: ${currentQ.caseItem.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (chosenOpt == currentQ.correctIndex) editorial.correctText else editorial.wrongText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentQ.caseItem.examPearl,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item(key = "ecg_quiz_breakdown_${currentQ.caseItem.id}") {
                        EcgNinePointBreakdownCard(breakdown = currentQ.caseItem.breakdown)
                    }
                }
            }
        }
    }
}

@Composable
fun EcgStripCard(
    pattern: EcgWaveformPattern,
    headerTitle: String,
    leadLabel: String,
    bpmBadge: String,
    urgency: EcgUrgency?,
    isBlinded: Boolean,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    var zoomScale by remember(pattern) { mutableFloatStateOf(1f) }
    var panOffsetPx by remember(pattern) { mutableFloatStateOf(0f) }

    val samples = remember(pattern) {
        EcgWaveformEngine.sampleStrip(pattern = pattern)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            width = if (urgency == EcgUrgency.STAT) 1.5.dp else 1.dp,
            color = if (urgency == EcgUrgency.STAT) editorial.wrongText.copy(alpha = 0.65f) else editorial.borderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ecg_strip_card_${pattern.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (urgency == EcgUrgency.STAT) Icons.Filled.WarningAmber else Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        tint = if (urgency == EcgUrgency.STAT) editorial.wrongText else editorial.acidLimeForeground,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = leadLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (urgency == EcgUrgency.STAT) editorial.wrongBg else editorial.elevatedSurface
                ) {
                    Text(
                        text = bpmBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = if (urgency == EcgUrgency.STAT) editorial.wrongText else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (urgency != null && !isBlinded) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (urgency) {
                        EcgUrgency.STAT -> editorial.wrongBg
                        EcgUrgency.URGENT -> editorial.elevatedSurface
                        EcgUrgency.MONITOR -> editorial.elevatedSurface
                        EcgUrgency.NORMAL -> editorial.correctBg
                    }
                ) {
                    Text(
                        text = urgency.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = when (urgency) {
                            EcgUrgency.STAT -> editorial.wrongText
                            EcgUrgency.NORMAL -> editorial.correctText
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Calibrated Clinical ECG Paper Canvas (Pinch-to-zoom + Pan)
            CalibratedEcgCanvas(
                samples = samples,
                zoomScale = zoomScale,
                panOffsetPx = panOffsetPx,
                isDarkMode = editorial.isDark,
                onGestureTransform = { zoomChange, panX ->
                    val nextZoom = (zoomScale * zoomChange).coerceIn(1f, 3f)
                    zoomScale = nextZoom
                    panOffsetPx = if (nextZoom <= 1.02f) 0f else (panOffsetPx + panX).coerceIn(-1200f, 1200f)
                }
            )

            // Zoom & Calibration Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pinch or tap to zoom (${String.format(Locale.US, "%.1fx", zoomScale)}) · 1 box = 0.04s",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = (zoomScale - 0.5f).coerceIn(1f, 3f)
                            zoomScale = next
                            if (next <= 1.02f) panOffsetPx = 0f
                        },
                        modifier = Modifier.testTag("ecg_zoom_out_btn")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom out ECG", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            zoomScale = 1f
                            panOffsetPx = 0f
                        },
                        modifier = Modifier.testTag("ecg_zoom_reset_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset ECG zoom", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.5f).coerceIn(1f, 3f) },
                        modifier = Modifier.testTag("ecg_zoom_in_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom in ECG", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibratedEcgCanvas(
    samples: FloatArray,
    zoomScale: Float,
    panOffsetPx: Float,
    isDarkMode: Boolean,
    onGestureTransform: (zoomChange: Float, panX: Float) -> Unit
) {
    // Classic clinical thermal ECG paper colors (light) vs ICU monitor grid (dark)
    val paperBg = if (isDarkMode) Color(0xFF0B1410) else Color(0xFFFFF6F6)
    val minorGrid = if (isDarkMode) Color(0xFF173024) else Color(0xFFF5D1D4)
    val majorGrid = if (isDarkMode) Color(0xFF254D3A) else Color(0xFFE59CA3)
    val traceColor = if (isDarkMode) Color(0xFFC8FF3D) else Color(0xFF111111)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(paperBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onGestureTransform(zoom, pan.x)
                }
            }
            .testTag("ecg_calibrated_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || samples.size < 2) return@Canvas

            // 30 large boxes horizontally = 6.0 sec (0.20s/large box) at 1x zoom
            val largeBoxesTotal = 30f
            val baseLargeBoxPx = (w / largeBoxesTotal) * zoomScale
            val smallBoxPx = (baseLargeBoxPx / 5f).coerceAtLeast(3f)
            val maxPan = ((w * zoomScale) - w).coerceAtLeast(0f)
            val clampedOffset = panOffsetPx.coerceIn(-maxPan, 0f)

            // Draw 1 mm minor & 5 mm major vertical grid lines
            val totalVerticalSmallBoxes = ((w * zoomScale) / smallBoxPx).toInt() + 2
            for (i in 0..totalVerticalSmallBoxes) {
                val x = clampedOffset + i * smallBoxPx
                if (x in -2f..(w + 2f)) {
                    val isMajor = i % 5 == 0
                    drawLine(
                        color = if (isMajor) majorGrid else minorGrid,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = if (isMajor) 1.5f else 0.75f
                    )
                }
            }

            // Draw 1 mm minor & 5 mm major horizontal grid lines anchored around isoelectric midline
            val baselineY = h * 0.56f
            val totalHorizHalf = (h / smallBoxPx).toInt() + 2
            for (j in -totalHorizHalf..totalHorizHalf) {
                val y = baselineY + j * smallBoxPx
                if (y in -2f..(h + 2f)) {
                    val isMajor = j % 5 == 0
                    drawLine(
                        color = if (isMajor) majorGrid else minorGrid,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (isMajor) 1.5f else 0.75f
                    )
                }
            }

            // Standard 1.0 mV (10 small boxes = 2 large boxes) calibration pulse at left edge
            val pxPerMv = smallBoxPx * 10f
            val calStart = clampedOffset + smallBoxPx * 1.5f
            val calWidth = smallBoxPx * 5f
            val calPath = Path().apply {
                moveTo(calStart - smallBoxPx, baselineY)
                lineTo(calStart, baselineY)
                lineTo(calStart, baselineY - pxPerMv)
                lineTo(calStart + calWidth, baselineY - pxPerMv)
                lineTo(calStart + calWidth, baselineY)
                lineTo(calStart + calWidth + smallBoxPx, baselineY)
            }
            drawPath(
                path = calPath,
                color = traceColor.copy(alpha = 0.75f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Miter)
            )

            // Draw calibrated ECG waveform trace right after calibration pulse
            val traceStartX = clampedOffset + smallBoxPx * 9f
            val traceWidth = (w * zoomScale) - (smallBoxPx * 10f)
            val path = Path()
            var started = false
            val stepX = traceWidth / (samples.size - 1).coerceAtLeast(1)

            for (idx in samples.indices) {
                val x = traceStartX + idx * stepX
                val y = (baselineY - samples[idx] * pxPerMv).coerceIn(6f, h - 6f)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = traceColor,
                style = Stroke(
                    width = 2.6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
private fun EcgNinePointBreakdownCard(breakdown: EcgNinePointBreakdown) {
    val editorial = MaterialTheme.editorialColors
    val rows = listOf(
        "1. Heart Rate" to breakdown.rate,
        "2. Rhythm" to breakdown.rhythm,
        "3. P Waves" to breakdown.pWaves,
        "4. PR Interval" to breakdown.prInterval,
        "5. QRS Complex" to breakdown.qrsComplex,
        "6. ST Segment" to breakdown.stSegment,
        "7. T Wave" to breakdown.tWave,
        "8. QT Interval" to breakdown.qtInterval
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ecg_nine_point_breakdown_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "9-POINT SYSTEMATIC ECG BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            rows.forEachIndexed { idx, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.38f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.62f)
                    )
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(color = editorial.borderSubtle.copy(alpha = 0.5f))
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.acidLime),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "9. PRIORITY NURSING ACTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = editorial.correctText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = breakdown.nursingPriority,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
