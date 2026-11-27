package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ClinicalCalculatorLogic
import com.example.data.model.ClinicalMnemonicsCatalog
import com.example.data.model.DailyFocusProgress
import com.example.data.model.FocusTimerPreset
import com.example.data.model.LabValueCategory
import com.example.data.model.MnemonicCategory
import com.example.data.model.NormalLabValue
import com.example.data.model.NormalLabValuesCatalog
import com.example.ui.components.AbgInterpreterWorkspace
import com.example.ui.components.ClinicalMnemonicCard
import com.example.ui.components.FocusStudyTimerCard
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

private enum class ClinicalCalcType(val label: String) {
    IV_DRIP("IV Drip"),
    PEDS_DOSE("Peds Dose"),
    PARKLAND("Parkland Burn"),
    GCS("GCS"),
    APGAR("APGAR"),
    BISHOP("Bishop")
}

@Composable
fun ClinicalToolsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSection: Int = 0,
    dailyFocusProgress: DailyFocusProgress = DailyFocusProgress(),
    onFocusSessionCompleted: (FocusTimerPreset) -> Unit = {},
    onOpenEcgStudio: (() -> Unit)? = null,
    onAbgPracticeAnswered: ((String, Boolean) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val editorial = MaterialTheme.editorialColors
    // 0 = Calcs, 1 = ABG, 2 = Labs, 3 = Mnemonics, 4 = Focus Timer
    var activeSection by remember { mutableIntStateOf(initialSection.coerceIn(0, 4)) }
    LaunchedEffect(initialSection) {
        activeSection = initialSection.coerceIn(0, 4)
    }
    var activeCalc by remember { mutableStateOf(ClinicalCalcType.IV_DRIP) }
    var labQuery by remember { mutableStateOf("") }
    var labCategory by remember { mutableStateOf<LabValueCategory?>(null) }
    var mnemonicQuery by remember { mutableStateOf("") }
    var mnemonicCategory by remember { mutableStateOf<MnemonicCategory?>(null) }

    val filteredLabs = remember(labQuery, labCategory) {
        val q = labQuery.trim().lowercase(Locale.US)
        NormalLabValuesCatalog.ALL_VALUES.filter { entry ->
            (labCategory == null || entry.category == labCategory) &&
                (q.isBlank() ||
                    entry.name.lowercase(Locale.US).contains(q) ||
                    entry.normalRange.lowercase(Locale.US).contains(q) ||
                    entry.examPearl.lowercase(Locale.US).contains(q))
        }
    }

    val filteredMnemonics = remember(mnemonicQuery, mnemonicCategory) {
        ClinicalMnemonicsCatalog.search(mnemonicQuery, mnemonicCategory)
    }

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
                modifier = Modifier.testTag("clinical_tools_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clinical Tools & Lab Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Calculators, NORCET labs, mnemonics & Pomodoro focus timer",
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
            if (onOpenEcgStudio != null) {
                item(key = "ecg_studio_launcher_banner") {
                    Card(
                        onClick = onOpenEcgStudio,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.acidLime),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clinical_tools_open_ecg_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MonitorHeart,
                                    contentDescription = null,
                                    tint = editorial.acidLimeForeground,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "ECG Learning Studio",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "14 calibrated ICU strips · Zoomable grid · Interpret & Reveal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open ECG Studio",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item(key = "tools_section_switch") {
                SegmentedPillBar(
                    options = listOf(0, 1, 2, 3, 4),
                    selected = activeSection,
                    labelOf = {
                        when (it) {
                            0 -> "Calcs"
                            1 -> "ABG"
                            2 -> "Labs"
                            3 -> "Tricks"
                            else -> "Focus"
                        }
                    },
                    onSelect = { activeSection = it },
                    testTagOf = { "clinical_tools_tab_$it" }
                )
            }

            if (activeSection == 0) {
                item(key = "calc_selector") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClinicalCalcType.entries.forEach { calc ->
                            MonochromeFilterPill(
                                label = calc.label,
                                selected = activeCalc == calc,
                                onClick = { activeCalc = calc },
                                modifier = Modifier.testTag("calc_pill_${calc.name.lowercase()}")
                            )
                        }
                    }
                }

                item(key = "calc_body_${activeCalc.name}") {
                    when (activeCalc) {
                        ClinicalCalcType.IV_DRIP -> IvDripCalculatorCard()
                        ClinicalCalcType.PEDS_DOSE -> PediatricDoseCalculatorCard()
                        ClinicalCalcType.PARKLAND -> ParklandBurnCalculatorCard()
                        ClinicalCalcType.GCS -> GcsScorerCard()
                        ClinicalCalcType.APGAR -> ApgarScorerCard()
                        ClinicalCalcType.BISHOP -> BishopScorerCard()
                    }
                }
            } else if (activeSection == 1) {
                item(key = "abg_interpreter_workspace") {
                    AbgInterpreterWorkspace(
                        onPracticeCaseAnswered = onAbgPracticeAnswered
                    )
                }
            } else if (activeSection == 2) {
                item(key = "lab_search") {
                    OutlinedTextField(
                        value = labQuery,
                        onValueChange = { labQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Search lab test, electrolyte, or clinical pearl") },
                        placeholder = { Text("e.g., Potassium, ABG, INR, Digoxin, FHR...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lab_values_search_input")
                    )
                }

                item(key = "lab_categories") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MonochromeFilterPill(
                            label = "All",
                            selected = labCategory == null,
                            onClick = { labCategory = null }
                        )
                        LabValueCategory.entries.forEach { cat ->
                            MonochromeFilterPill(
                                label = cat.label,
                                selected = labCategory == cat,
                                onClick = { labCategory = cat }
                            )
                        }
                    }
                }

                items(items = filteredLabs, key = { it.name }) { entry ->
                    LabValueCard(entry)
                }
            } else if (activeSection == 3) {
                item(key = "mnemonic_search") {
                    OutlinedTextField(
                        value = mnemonicQuery,
                        onValueChange = { mnemonicQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Search mnemonic, drug, triad, or exam trap") },
                        placeholder = { Text("e.g., MONA, RIPES, VEAL CHOP, Cushing, BMW...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mnemonics_search_input")
                    )
                }

                item(key = "mnemonic_categories") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MonochromeFilterPill(
                            label = "All (${ClinicalMnemonicsCatalog.ALL_MNEMONICS.size})",
                            selected = mnemonicCategory == null,
                            onClick = { mnemonicCategory = null },
                            modifier = Modifier.testTag("mnemonic_cat_all")
                        )
                        MnemonicCategory.entries.forEach { cat ->
                            MonochromeFilterPill(
                                label = cat.label,
                                selected = mnemonicCategory == cat,
                                onClick = { mnemonicCategory = cat },
                                modifier = Modifier.testTag("mnemonic_cat_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                items(items = filteredMnemonics, key = { it.id }) { mnemonic ->
                    ClinicalMnemonicCard(mnemonic = mnemonic)
                }
            } else {
                item(key = "focus_study_timer") {
                    FocusStudyTimerCard(
                        dailyProgress = dailyFocusProgress,
                        onSessionCompleted = onFocusSessionCompleted
                    )
                }
            }
        }
    }
}

@Composable
private fun IvDripCalculatorCard() {
    val editorial = MaterialTheme.editorialColors
    var volumeText by remember { mutableStateOf("1000") }
    var hoursText by remember { mutableStateOf("8") }
    var dropFactor by remember { mutableIntStateOf(15) }

    val result = remember(volumeText, hoursText, dropFactor) {
        val vol = volumeText.toDoubleOrNull() ?: 0.0
        val hrs = hoursText.toDoubleOrNull() ?: 0.0
        ClinicalCalculatorLogic.calculateIvDrip(vol, hrs, dropFactor)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("IV Drip Rate Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Formula: gtt/min = (Volume in mL × Drop Factor) ÷ (Time in minutes)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = volumeText,
                    onValueChange = { volumeText = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
                    label = { Text("Volume (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("iv_volume_input")
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Duration (hrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("iv_hours_input")
                )
            }
            Text("Tubing Drop Factor (gtt/mL)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10 to "10 (Blood)", 15 to "15 (Macro)", 20 to "20 (Macro)", 60 to "60 (Micro)").forEach { (factor, label) ->
                    MonochromeFilterPill(
                        label = label,
                        selected = dropFactor == factor,
                        onClick = { dropFactor = factor }
                    )
                }
            }
            if (result != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.acidLime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DRIP RATE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = editorial.onAcidLime)
                            Text(
                                "${result.dropsPerMinute} gtt/min",
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = editorial.onAcidLime
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("INFUSION PUMP", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = editorial.onAcidLime)
                            Text(
                                String.format(Locale.US, "%.1f mL/hr", result.mlPerHour),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = editorial.onAcidLime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PediatricDoseCalculatorCard() {
    val editorial = MaterialTheme.editorialColors
    var weightText by remember { mutableStateOf("14") }
    var mgPerKgText by remember { mutableStateOf("40") }
    var dosesPerDay by remember { mutableIntStateOf(3) }
    var adultDoseText by remember { mutableStateOf("500") }
    var ageYearsText by remember { mutableStateOf("3") }

    val result = remember(weightText, mgPerKgText, dosesPerDay, adultDoseText, ageYearsText) {
        ClinicalCalculatorLogic.calculatePediatricDose(
            weightKg = weightText.toDoubleOrNull() ?: 0.0,
            mgPerKgPerDay = mgPerKgText.toDoubleOrNull() ?: 0.0,
            dosesPerDay = dosesPerDay,
            adultDoseMg = adultDoseText.toDoubleOrNull(),
            childAgeYears = ageYearsText.toDoubleOrNull()
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pediatric Dosage (Weight, Clark's & Young's)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = mgPerKgText,
                    onValueChange = { mgPerKgText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("mg / kg / day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Divided Doses / Day", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "OD (1×)", 2 to "BD (2×)", 3 to "TDS (3×)", 4 to "QID (4×)").forEach { (d, lbl) ->
                    MonochromeFilterPill(label = lbl, selected = dosesPerDay == d, onClick = { dosesPerDay = d })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = adultDoseText,
                    onValueChange = { adultDoseText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Adult Dose (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ageYearsText,
                    onValueChange = { ageYearsText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Child Age (yrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (result != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Single Dose: ${String.format(Locale.US, "%.1f mg", result.singleDoseMg)}  (Total ${String.format(Locale.US, "%.1f mg/day", result.totalDailyMg)})",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.correctText
                        )
                        result.clarksRuleMg?.let {
                            Text("Clark's Rule (Weight lbs ÷ 150 × Adult): ${String.format(Locale.US, "%.1f mg", it)}", style = MaterialTheme.typography.bodySmall)
                        }
                        result.youngsRuleMg?.let {
                            Text("Young's Rule (Age ÷ [Age+12] × Adult): ${String.format(Locale.US, "%.1f mg", it)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParklandBurnCalculatorCard() {
    val editorial = MaterialTheme.editorialColors
    var weightText by remember { mutableStateOf("65") }
    var tbsaText by remember { mutableStateOf("36") }

    val result = remember(weightText, tbsaText) {
        ClinicalCalculatorLogic.calculateParklandBurns(
            weightKg = weightText.toDoubleOrNull() ?: 0.0,
            tbsaPercent = tbsaText.toDoubleOrNull() ?: 0.0
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Parkland Burn Fluid Resuscitation (Ringer Lactate)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "4 mL × Weight (kg) × %TBSA • Half in first 8h from time of burn, half in next 16h",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tbsaText,
                    onValueChange = { tbsaText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Burn %TBSA (1–100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (result != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.acidLime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "24-Hour Total RL: ${result.total24hMl.toInt()} mL",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.onAcidLime
                        )
                        Text(
                            "First 8 hrs: ${result.first8hMl.toInt()} mL (${String.format(Locale.US, "%.0f mL/hr", result.first8hMlPerHr)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = editorial.onAcidLime
                        )
                        Text(
                            "Next 16 hrs: ${result.next16hMl.toInt()} mL (${String.format(Locale.US, "%.0f mL/hr", result.next16hMlPerHr)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = editorial.onAcidLime
                        )
                    }
                }
                Text(
                    "Exam Pearl: Always measure first 8 hours from the TIME OF BURN INJURY (not hospital arrival). Target urine output ≥ 0.5 mL/kg/hr (adults).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GcsScorerCard() {
    val editorial = MaterialTheme.editorialColors
    var eye by remember { mutableIntStateOf(4) }
    var verbal by remember { mutableIntStateOf(5) }
    var motor by remember { mutableIntStateOf(6) }
    val eval = remember(eye, verbal, motor) { ClinicalCalculatorLogic.evaluateGcs(eye, verbal, motor) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Glasgow Coma Scale (GCS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScoreSelectorRow("Eye Opening (E1–E4)", listOf(4 to "4 Spont", 3 to "3 Speech", 2 to "2 Pain", 1 to "1 None"), eye) { eye = it }
            ScoreSelectorRow("Verbal Response (V1–V5)", listOf(5 to "5 Oriented", 4 to "4 Confused", 3 to "3 Words", 2 to "2 Sounds", 1 to "1 None"), verbal) { verbal = it }
            ScoreSelectorRow("Motor Response (M1–M6)", listOf(6 to "6 Obeys", 5 to "5 Localizes", 4 to "4 Withdraws", 3 to "3 Flexion", 2 to "2 Extension", 1 to "1 None"), motor) { motor = it }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (eval.airwayAlert) editorial.wrongBg else editorial.elevatedSurface,
                border = BorderStroke(1.dp, if (eval.airwayAlert) editorial.wrongText.copy(alpha = 0.45f) else editorial.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "GCS ${eval.total}/15  (E$eye V$verbal M$motor)",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (eval.airwayAlert) editorial.wrongText else MaterialTheme.colorScheme.onSurface
                    )
                    Text(eval.severity, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ApgarScorerCard() {
    val editorial = MaterialTheme.editorialColors
    var a by remember { mutableIntStateOf(2) }
    var p by remember { mutableIntStateOf(2) }
    var g by remember { mutableIntStateOf(2) }
    var act by remember { mutableIntStateOf(2) }
    var r by remember { mutableIntStateOf(2) }
    val eval = remember(a, p, g, act, r) { ClinicalCalculatorLogic.evaluateApgar(a, p, g, act, r) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("APGAR Newborn Score (1 min & 5 min)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScoreSelectorRow("Appearance (Color)", listOf(0 to "0 Blue/Pale", 1 to "1 Acrocyanosis", 2 to "2 Pink"), a) { a = it }
            ScoreSelectorRow("Pulse (Heart Rate)", listOf(0 to "0 Absent", 1 to "1 <100 bpm", 2 to "2 ≥100 bpm"), p) { p = it }
            ScoreSelectorRow("Grimace (Reflex)", listOf(0 to "0 None", 1 to "1 Grimace", 2 to "2 Vigorous cry"), g) { g = it }
            ScoreSelectorRow("Activity (Muscle Tone)", listOf(0 to "0 Limp", 1 to "1 Some flex", 2 to "2 Active"), act) { act = it }
            ScoreSelectorRow("Respiration", listOf(0 to "0 Absent", 1 to "1 Slow/Irreg", 2 to "2 Strong cry"), r) { r = it }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (eval.total < 4) editorial.wrongBg else editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "APGAR Score: ${eval.total}/10",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (eval.total < 4) editorial.wrongText else editorial.correctText
                    )
                    Text(eval.interpretation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BishopScorerCard() {
    val editorial = MaterialTheme.editorialColors
    var dil by remember { mutableIntStateOf(2) }
    var eff by remember { mutableIntStateOf(2) }
    var sta by remember { mutableIntStateOf(2) }
    var con by remember { mutableIntStateOf(2) }
    var pos by remember { mutableIntStateOf(1) }
    val eval = remember(dil, eff, sta, con, pos) { ClinicalCalculatorLogic.evaluateBishop(dil, eff, sta, con, pos) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bishop Score (Cervical Favorability for Induction)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScoreSelectorRow("Dilation (cm)", listOf(0 to "0 Closed", 1 to "1 (1–2)", 2 to "2 (3–4)", 3 to "3 (≥5)"), dil) { dil = it }
            ScoreSelectorRow("Effacement (%)", listOf(0 to "0 (0–30)", 1 to "1 (40–50)", 2 to "2 (60–70)", 3 to "3 (≥80)"), eff) { eff = it }
            ScoreSelectorRow("Fetal Station", listOf(0 to "0 (-3)", 1 to "1 (-2)", 2 to "2 (-1/0)", 3 to "3 (+1/+2)"), sta) { sta = it }
            ScoreSelectorRow("Consistency", listOf(0 to "0 Firm", 1 to "1 Medium", 2 to "2 Soft"), con) { con = it }
            ScoreSelectorRow("Position", listOf(0 to "0 Posterior", 1 to "1 Mid", 2 to "2 Anterior"), pos) { pos = it }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Bishop Score: ${eval.total}/13",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (eval.total >= 8) editorial.correctText else MaterialTheme.colorScheme.onSurface
                    )
                    Text(eval.readiness, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScoreSelectorRow(
    label: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, text) ->
                MonochromeFilterPill(
                    label = text,
                    selected = selected == value,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun LabValueCard(entry: NormalLabValue) {
    val editorial = MaterialTheme.editorialColors
    Card(
        shape = RoundedCornerShape(14.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.MedicalInformation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(entry.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.acidLime
                ) {
                    Text(
                        text = entry.normalRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = editorial.onAcidLime,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = editorial.wrongText, modifier = Modifier.size(14.dp))
                Text(
                    text = "Critical: ${entry.criticalPanic}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = editorial.wrongText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = entry.examPearl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
