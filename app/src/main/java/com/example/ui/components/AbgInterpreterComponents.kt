package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AbgInput
import com.example.data.model.AbgInterpretationResult
import com.example.data.model.AbgInterpreterLogic
import com.example.data.model.AbgPracticeCase
import com.example.data.model.AbgPracticeCatalog
import com.example.data.model.AbgPracticeTier
import com.example.data.model.OxygenationStatus
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
fun AbgInterpreterWorkspace(
    modifier: Modifier = Modifier,
    onPracticeCaseAnswered: ((String, Boolean) -> Unit)? = null
) {
    val editorial = MaterialTheme.editorialColors
    // 0 = Interactive Solver, 1 = Tiered Practice Cases
    var activeMode by remember { mutableIntStateOf(0) }

    // Solver inputs (defaulted to a classic DKA case so first view demonstrates full Winter's output)
    var phText by remember { mutableStateOf("7.22") }
    var paco2Text by remember { mutableStateOf("24") }
    var hco3Text by remember { mutableStateOf("10") }
    var pao2Text by remember { mutableStateOf("94") }
    var showOptionalElectrolytes by remember { mutableStateOf(false) }
    var sao2Text by remember { mutableStateOf("") }
    var naText by remember { mutableStateOf("138") }
    var clText by remember { mutableStateOf("102") }

    // Practice state
    var selectedTier by remember { mutableStateOf<AbgPracticeTier?>(null) }
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }
    val cases = remember(selectedTier) { AbgPracticeCatalog.casesForTier(selectedTier) }

    fun loadIntoSolver(input: AbgInput) {
        phText = String.format(Locale.US, "%.2f", input.ph)
        paco2Text = String.format(Locale.US, "%.1f", input.paco2).removeSuffix(".0")
        hco3Text = String.format(Locale.US, "%.1f", input.hco3).removeSuffix(".0")
        pao2Text = input.pao2?.let { String.format(Locale.US, "%.1f", it).removeSuffix(".0") } ?: ""
        sao2Text = input.sao2?.let { String.format(Locale.US, "%.0f", it) } ?: ""
        if (input.sodium != null && input.chloride != null) {
            naText = String.format(Locale.US, "%.0f", input.sodium)
            clText = String.format(Locale.US, "%.0f", input.chloride)
            showOptionalElectrolytes = true
        } else {
            naText = ""
            clText = ""
        }
        activeMode = 0
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SegmentedPillBar(
            options = listOf(0, 1),
            selected = activeMode,
            labelOf = { if (it == 0) "ABG Solver" else "Practice (${AbgPracticeCatalog.ALL_CASES.size})" },
            onSelect = { activeMode = it },
            testTagOf = { "abg_mode_tab_$it" }
        )

        if (activeMode == 0) {
            // Quick Clinical Presets Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AbgPracticeCatalog.QUICK_PRESETS.forEach { preset ->
                    MonochromeFilterPill(
                        label = preset.label,
                        selected = false,
                        onClick = { loadIntoSolver(preset.input) },
                        modifier = Modifier.testTag("abg_preset_${preset.id}")
                    )
                }
            }

            // Input Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("abg_solver_input_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Biotech,
                            contentDescription = null,
                            tint = editorial.acidLimeForeground,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Arterial Blood Gas Interpreter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Normal: pH 7.35–7.45 · PaCO2 35–45 · HCO3- 22–26 · PaO2 80–100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phText,
                            onValueChange = { phText = it },
                            label = { Text("pH") },
                            placeholder = { Text("7.40") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("abg_input_ph")
                        )
                        OutlinedTextField(
                            value = paco2Text,
                            onValueChange = { paco2Text = it },
                            label = { Text("PaCO2 (mmHg)") },
                            placeholder = { Text("40") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("abg_input_paco2")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hco3Text,
                            onValueChange = { hco3Text = it },
                            label = { Text("HCO3- (mEq/L)") },
                            placeholder = { Text("24") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("abg_input_hco3")
                        )
                        OutlinedTextField(
                            value = pao2Text,
                            onValueChange = { pao2Text = it },
                            label = { Text("PaO2 (opt.)") },
                            placeholder = { Text("92") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("abg_input_pao2")
                        )
                    }

                    TextButton(
                        onClick = { showOptionalElectrolytes = !showOptionalElectrolytes },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("abg_toggle_anion_gap_btn")
                    ) {
                        Icon(
                            imageVector = if (showOptionalElectrolytes) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (showOptionalElectrolytes) "Hide Anion Gap & SaO2 inputs" else "Add Na+, Cl- (Anion Gap) & SaO2",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (showOptionalElectrolytes) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = naText,
                                onValueChange = { naText = it },
                                label = { Text("Na+ (135–145)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("abg_input_na")
                            )
                            OutlinedTextField(
                                value = clText,
                                onValueChange = { clText = it },
                                label = { Text("Cl- (98–106)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("abg_input_cl")
                            )
                            OutlinedTextField(
                                value = sao2Text,
                                onValueChange = { sao2Text = it },
                                label = { Text("SaO2 %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("abg_input_sao2")
                            )
                        }
                    }
                }
            }

            // Live Interpretation Output
            val parsedInput = remember(phText, paco2Text, hco3Text, pao2Text, sao2Text, naText, clText, showOptionalElectrolytes) {
                val ph = phText.trim().toDoubleOrNull()
                val co2 = paco2Text.trim().toDoubleOrNull()
                val hco3 = hco3Text.trim().toDoubleOrNull()
                if (ph == null || co2 == null || hco3 == null) {
                    null
                } else {
                    AbgInput(
                        ph = ph,
                        paco2 = co2,
                        hco3 = hco3,
                        pao2 = pao2Text.trim().toDoubleOrNull(),
                        sao2 = if (showOptionalElectrolytes) sao2Text.trim().toDoubleOrNull() else null,
                        sodium = if (showOptionalElectrolytes) naText.trim().toDoubleOrNull() else null,
                        chloride = if (showOptionalElectrolytes) clText.trim().toDoubleOrNull() else null
                    )
                }
            }

            if (parsedInput == null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter numeric pH, PaCO2, and HCO3- values above (or tap a clinical preset) for instant step-by-step ABG interpretation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                val validationError = AbgInterpreterLogic.validateInput(parsedInput)
                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = editorial.wrongBg,
                        border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = editorial.wrongText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    val result = remember(parsedInput) { AbgInterpreterLogic.interpret(parsedInput) }
                    AbgInterpretationResultCard(result = result)
                }
            }
        } else {
            // Tiered ABG Practice Mode
            val answeredInTier = cases.count { selectedAnswers.containsKey(it.id) }
            val correctInTier = cases.count { selectedAnswers[it.id] == it.correctIndex }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonochromeFilterPill(
                    label = "All (${AbgPracticeCatalog.ALL_CASES.size})",
                    selected = selectedTier == null,
                    onClick = { selectedTier = null },
                    modifier = Modifier.testTag("abg_tier_all")
                )
                AbgPracticeTier.entries.forEach { tier ->
                    MonochromeFilterPill(
                        label = tier.label,
                        selected = selectedTier == tier,
                        onClick = { selectedTier = tier },
                        modifier = Modifier.testTag("abg_tier_${tier.name.lowercase()}")
                    )
                }
            }

            if (answeredInTier > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
                            text = "ABG Practice Score: $correctInTier / $answeredInTier correct",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { cases.forEach { selectedAnswers.remove(it.id) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }

            cases.forEach { abgCase ->
                AbgPracticeCaseCard(
                    abgCase = abgCase,
                    selectedOption = selectedAnswers[abgCase.id],
                    onSelectOption = { idx ->
                        if (!selectedAnswers.containsKey(abgCase.id)) {
                            selectedAnswers[abgCase.id] = idx
                            onPracticeCaseAnswered?.invoke(abgCase.id, idx == abgCase.correctIndex)
                        }
                    },
                    onInspectInSolver = { loadIntoSolver(abgCase.input) }
                )
            }
        }
    }
}

@Composable
private fun AbgInterpretationResultCard(result: AbgInterpretationResult) {
    val editorial = MaterialTheme.editorialColors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.5.dp, editorial.acidLime),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("abg_interpretation_result_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CLINICAL INTERPRETATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = editorial.correctText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (result.mixedDisorderNote != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.wrongBg,
                    border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Mixed disorder alert",
                            tint = editorial.wrongText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = result.mixedDisorderNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = editorial.wrongText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Step-by-Step Breakdown
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = editorial.elevatedSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Step-by-Step Analysis",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    result.stepByStepSummary.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Oxygenation Pill
            if (result.oxygenationStatus != OxygenationStatus.NOT_PROVIDED) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (result.oxygenationStatus.isCritical) editorial.wrongBg else editorial.correctBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Oxygenation: ${result.oxygenationStatus.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (result.oxygenationStatus.isCritical) editorial.wrongText else editorial.correctText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.oxygenationDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Anion Gap (when calculated)
            result.anionGap?.let { ag ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Anion Gap: ${ag.classification}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ag.causesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = editorial.borderSubtle)

            // Priority Nursing Actions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "NORCET Priority Nursing Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                result.nursingPriorities.forEach { action ->
                    Text(
                        text = "• $action",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Common Clinical Causes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                result.commonCauses.forEach { cause ->
                    Text(
                        text = "• $cause",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AbgPracticeCaseCard(
    abgCase: AbgPracticeCase,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit,
    onInspectInSolver: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val answered = selectedOption != null

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("abg_case_${abgCase.id}")
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
                Text(
                    text = abgCase.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.elevatedSurface
                ) {
                    Text(
                        text = abgCase.tier.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = abgCase.vignette,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            abgCase.options.forEachIndexed { idx, optText ->
                val isSelected = selectedOption == idx
                val isCorrectOpt = idx == abgCase.correctIndex
                val bgColor = when {
                    !answered -> editorial.elevatedSurface
                    isCorrectOpt -> editorial.correctBg
                    isSelected -> editorial.wrongBg
                    else -> editorial.elevatedSurface
                }
                val borderColor = when {
                    !answered -> editorial.borderSubtle
                    isCorrectOpt -> editorial.correctText.copy(alpha = 0.6f)
                    isSelected -> editorial.wrongText.copy(alpha = 0.6f)
                    else -> editorial.borderSubtle
                }

                Surface(
                    onClick = { onSelectOption(idx) },
                    enabled = !answered,
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("abg_case_${abgCase.id}_opt_$idx")
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
                            text = optText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (answered && isCorrectOpt) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (answered && isCorrectOpt) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Correct option",
                                tint = editorial.correctText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (answered) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.elevatedSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedOption == abgCase.correctIndex) "Correct Interpretation" else "Clinical Rationale",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedOption == abgCase.correctIndex) editorial.correctText else editorial.wrongText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = abgCase.rationale,
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = onInspectInSolver,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("abg_case_${abgCase.id}_open_solver")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Inspect Values in ABG Solver")
                        }
                    }
                }
            }
        }
    }
}
