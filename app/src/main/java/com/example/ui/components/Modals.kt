package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.PdfWorksheetWriter
import com.example.data.model.DifficultyFilter
import com.example.data.model.Exam
import com.example.data.model.Profession
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionSourceType
import com.example.data.model.SubjectCatalog
import com.example.ui.theme.CalmSageFill
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MutedCrimsonFill
import com.example.ui.theme.WarmWhite
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.ImporterUiState
import com.example.ui.viewmodel.PdfExportConfigState
import com.example.ui.viewmodel.TimedPracticeConfigState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimedPracticeConfigDialog(
    state: TimedPracticeConfigState,
    allQuestions: List<QuestionItem>,
    useTimeLimit: Boolean,
    negativeMarking: Boolean,
    onToggleNegativeMarking: () -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onSelectCount: (Int) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    filterCountProvider: (String, DifficultyFilter) -> Int
) {
    if (!state.isVisible) return

    val sourceOptions = remember {
        listOf(
            "MIXED" to "Mixed (All)",
            "PYQ" to "PYQs Only",
            "NURSING" to "Nursing (70%)",
            "CA" to "CA & GK",
            "GEN" to "Sci/Math/Reas (30%)"
        ) + SubjectCatalog.ALL_SUBJS.map { it.code to "${it.code} (${it.name})" }
    }
    val availableCount = filterCountProvider(state.sourceKey, state.difficultyFilter)
    val effectiveCount = if (state.questionCount == 0) availableCount else minOf(state.questionCount, availableCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Timed Practice Mode", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select Subject or Mixed Set",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sourceOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = state.sourceKey == key,
                            onClick = { onSelectSource(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("timed_src_$key")
                        )
                    }
                }

                Text(
                    text = "Difficulty Level",
                    style = MaterialTheme.typography.labelLarge
                )
                DifficultyFilterBar(
                    selected = state.difficultyFilter,
                    onSelect = onSelectDifficulty,
                    testTagPrefix = "timed_diff"
                )

                Text(
                    text = "Question Count ($availableCount available)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15 Qs", 30 to "30 Qs", 50 to "50 Qs", 0 to "All").forEach { (cnt, lbl) ->
                        FilterChip(
                            selected = state.questionCount == cnt,
                            onClick = { onSelectCount(cnt) },
                            label = { Text(lbl, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("timed_cnt_$cnt")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timer Duration", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (useTimeLimit) "${state.durationMinutes} mins" else "Off (Settings)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                        color = if (useTimeLimit) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!useTimeLimit) {
                    Text(
                        text = "Time limit is turned off in Settings. This practice drill will run without a countdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onSelectMinutes(state.durationMinutes - 5) },
                        enabled = useTimeLimit,
                        modifier = Modifier.testTag("timed_minus_5")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease 5 minutes")
                    }
                    Slider(
                        value = state.durationMinutes.toFloat(),
                        onValueChange = { onSelectMinutes(it.toInt()) },
                        valueRange = 5f..120f,
                        enabled = useTimeLimit,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onSelectMinutes(state.durationMinutes + 5) },
                        enabled = useTimeLimit,
                        modifier = Modifier.testTag("timed_plus_5")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase 5 minutes")
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { m ->
                        FilterChip(
                            selected = useTimeLimit && state.durationMinutes == m,
                            enabled = useTimeLimit,
                            onClick = { onSelectMinutes(m) },
                            label = { Text("${m}m", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Negative Marking (-0.33)", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "RRB official +1 / -0.33 scoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = negativeMarking,
                        onCheckedChange = { onToggleNegativeMarking() },
                        modifier = Modifier.testTag("timed_neg_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                enabled = effectiveCount > 0,
                modifier = Modifier.testTag("start_timed_practice_button")
            ) {
                Text(
                    if (useTimeLimit) {
                        "Start ($effectiveCount Qs • ${state.durationMinutes}m)"
                    } else {
                        "Start ($effectiveCount Qs • Untimed)"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImporterModalSheet(
    state: ImporterUiState,
    activeExamShortName: String = "RRB Nursing",
    onUpdate: ((ImporterUiState) -> ImporterUiState) -> Unit,
    onPreviewPdf: () -> Unit,
    onCommitPdf: () -> Unit,
    onImportJson: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onClearCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    var confirmClear by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Add & Manage Questions", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Import shift PDFs, JSON sets, or back up your progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Active Exam: $activeExamShortName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close importer")
                }
            }

            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { onUpdate { it.copy(selectedTab = 0) } },
                    text = { Text("PDF Text Parser") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { onUpdate { it.copy(selectedTab = 1) } },
                    text = { Text("JSON / Backup") }
                )
            }

            if (state.selectedTab == 0) {
                PdfTextImportPane(
                    state = state,
                    onUpdate = onUpdate,
                    onPreviewPdf = onPreviewPdf,
                    onCommitPdf = onCommitPdf
                )
            } else {
                JsonBackupPane(
                    state = state,
                    onUpdate = onUpdate,
                    onImportJson = onImportJson,
                    onExportBackup = onExportBackup,
                    onRestoreBackup = onRestoreBackup,
                    onOpenPdfExport = onOpenPdfExport,
                    onRequestClearCustom = { confirmClear = true }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Reset Custom Questions?") },
            text = { Text("Delete all custom imported questions? The built-in MCQs will remain untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClear = false
                        onClearCustom()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedCrimsonFill,
                        contentColor = WarmWhite
                    )
                ) {
                    Text("Delete Custom")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PdfTextImportPane(
    state: ImporterUiState,
    onUpdate: ((ImporterUiState) -> ImporterUiState) -> Unit,
    onPreviewPdf: () -> Unit,
    onCommitPdf: () -> Unit
) {
    val categories = listOf("pyq" to "PYQ", "nur" to "Nursing 70%", "ca" to "CA / GK", "gen" to "Sci/Math 30%")
    val subjects = SubjectCatalog.ALL_SUBJS.map { it.code }
    var catExpanded by remember { mutableStateOf(false) }
    var subjExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Source Type", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuestionSourceType.entries.forEach { st ->
                FilterChip(
                    selected = state.sourceType == st,
                    onClick = {
                        onUpdate { current ->
                            val alignedCategory = when (st) {
                                QuestionSourceType.OFFICIAL_PYQ,
                                QuestionSourceType.MEMORY_BASED -> "pyq"
                                QuestionSourceType.CURRENT_AFFAIRS -> "ca"
                                QuestionSourceType.PRACTICE -> if (current.category == "gen") "gen" else "nur"
                            }
                            current.copy(sourceType = st, category = alignedCategory)
                        }
                    },
                    label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("import_source_${st.dbValue}")
                )
            }
        }
        if (state.sourceType == QuestionSourceType.OFFICIAL_PYQ && state.sourceNote.isBlank()) {
            Text(
                text = "Add a source note below to mark as Official PYQ; otherwise saved as Memory-Based PYQ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.editorialColors.warningText
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = categories.firstOrNull { it.first == state.category }?.second ?: state.category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                categories.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUpdate { current ->
                                val alignedSource = when (code) {
                                    "pyq" -> if (current.sourceType == QuestionSourceType.OFFICIAL_PYQ) {
                                        QuestionSourceType.OFFICIAL_PYQ
                                    } else {
                                        QuestionSourceType.MEMORY_BASED
                                    }
                                    "ca" -> QuestionSourceType.CURRENT_AFFAIRS
                                    else -> QuestionSourceType.PRACTICE
                                }
                                current.copy(category = code, sourceType = alignedSource)
                            }
                            catExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = subjExpanded,
            onExpandedChange = { subjExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = state.subject,
                onValueChange = {},
                readOnly = true,
                label = { Text("Subject") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                subjects.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(code) },
                        onClick = {
                            onUpdate { it.copy(subject = code) }
                            subjExpanded = false
                        }
                    )
                }
            }
        }
    }

    OutlinedTextField(
        value = state.topicName,
        onValueChange = { txt -> onUpdate { it.copy(topicName = txt) } },
        label = { Text("Topic (optional)") },
        placeholder = { Text("e.g. Cardiology, Fluids & Electrolytes") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_topic_input")
    )

    OutlinedTextField(
        value = state.shiftYear,
        onValueChange = { txt -> onUpdate { it.copy(shiftYear = txt) } },
        label = { Text("Year / Shift Tag (e.g. 2025-S2)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = state.sourceNote,
        onValueChange = { txt ->
            onUpdate {
                it.copy(
                    sourceNote = txt,
                    markAsVerified = if (txt.isBlank()) false else it.markAsVerified
                )
            }
        },
        label = { Text("Source Note (required to mark PYQ as Verified)") },
        placeholder = { Text("e.g. Official RRB 2025 Shift 1 answer key PDF") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_source_note_input")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Mark Imported Questions as Verified", style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (state.sourceNote.isBlank()) {
                    "Add a non-empty Source Note above to enable Verified status; otherwise saved as Unverified."
                } else {
                    "Will be saved as Verified with the Source Note above."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = state.markAsVerified && state.sourceNote.isNotBlank(),
            enabled = state.sourceNote.isNotBlank(),
            onCheckedChange = { checked -> onUpdate { it.copy(markAsVerified = checked) } },
            modifier = Modifier.testTag("pdf_mark_verified_switch")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Default Difficulty", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestionDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = state.difficulty == diff,
                    onClick = { onUpdate { it.copy(difficulty = diff) } },
                    label = { Text(diff.label) },
                    modifier = Modifier.testTag("import_diff_${diff.name.lowercase()}")
                )
            }
        }
    }

    OutlinedTextField(
        value = state.rawText,
        onValueChange = { txt -> onUpdate { it.copy(rawText = txt) } },
        label = { Text("Paste Raw PDF Text") },
        placeholder = {
            Text(
                "Q.1 Which cranial nerve controls vision?\n1. Olfactory\n2. Optic\n3. Oculomotor\n4. Trochlear\nAns: 2\nExplanation: CN II transmits visual signals.",
                style = MaterialTheme.typography.bodySmall
            )
        },
        minLines = 5,
        maxLines = 8,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_raw_input")
    )

    Button(
        onClick = onPreviewPdf,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parse_preview_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Parse & Preview")
    }

    if (state.hasPreviewed) {
        val editorial = MaterialTheme.editorialColors
        if (state.previewQuestions.isEmpty()) {
            Text(
                "Could not detect questions. Format as 'Q.1 Question' followed by '1. Opt' and 'Ans: 1'.",
                color = editorial.wrongText,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Parsed ${state.previewQuestions.size} valid MCQ(s)",
                    color = editorial.correctText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onCommitPdf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalmSageFill,
                        contentColor = WarmWhite
                    ),
                    modifier = Modifier.testTag("save_imported_button")
                ) {
                    Text("Save Permanently")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.previewQuestions.forEachIndexed { idx, q ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PyqSourceBadge(question = q)
                            Text("${idx + 1}. ${q.questionText}", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Ans: ${'A' + q.correctIndex} • ${q.options.getOrElse(q.correctIndex) { "" }}",
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
private fun JsonBackupPane(
    state: ImporterUiState,
    onUpdate: ((ImporterUiState) -> ImporterUiState) -> Unit,
    onImportJson: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onRequestClearCustom: () -> Unit
) {
    OutlinedTextField(
        value = state.jsonText,
        onValueChange = { txt -> onUpdate { it.copy(jsonText = txt) } },
        label = { Text("Compact JSON Array") },
        placeholder = {
            Text(
                """[{"c":"pyq","y":"2025-S2","s":"MSN","d":"Medium","q":"...","o":["A","B","C","D"],"a":0,"e":"...","sourceStatus":"verified","sourceNote":"Official RRB 2025-S2 key"}]""",
                style = MaterialTheme.typography.bodySmall
            )
        },
        supportingText = {
            Text(
                "PYQ items are saved as verified only when sourceStatus is \"verified\" and sourceNote is non-empty; otherwise saved as unverified.",
                style = MaterialTheme.typography.bodySmall
            )
        },
        minLines = 5,
        maxLines = 8,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("json_batch_input")
    )

    Button(
        onClick = onImportJson,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_json_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Import JSON Questions")
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onExportBackup,
            modifier = Modifier
                .weight(1f)
                .testTag("export_backup_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Export JSON")
        }
        OutlinedButton(
            onClick = onRestoreBackup,
            modifier = Modifier
                .weight(1f)
                .testTag("restore_backup_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Restore JSON")
        }
    }

    FilledTonalButton(
        onClick = onOpenPdfExport,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("importer_open_pdf_export_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Printable PDF Worksheet")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onRequestClearCustom,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.editorialColors.wrongText)
        ) {
            Text("Reset Custom Questions")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PdfExportModalSheet(
    state: PdfExportConfigState,
    matchingCount: Int,
    suggestedFileName: String,
    onUpdate: ((PdfExportConfigState) -> PdfExportConfigState) -> Unit,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    val scopes = remember {
        listOf(
            "MIXED" to "All MCQs",
            "BOOKMARKS" to "Bookmarks",
            "MISTAKES" to "Mistakes",
            "LOWEST_ACCURACY" to "Lowest Accuracy",
            "PYQ" to "PYQs",
            "NURSING" to "Nursing 70%",
            "CA" to "CA & GK",
            "GEN" to "Sci/Math 30%"
        ) + SubjectCatalog.ALL_SUBJS.map { it.code to it.code }
    }
    val limits = listOf(15 to "15 Qs", 30 to "30 Qs", 50 to "50 Qs", 100 to "100 Qs", 0 to "All")
    val estimatedPages = remember(matchingCount, state.inlineAnswers, state.includeAnswerKey) {
        PdfWorksheetWriter.estimatePageCount(
            questionCount = matchingCount,
            inlineAnswers = state.inlineAnswers,
            includeAnswerKey = state.includeAnswerKey
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!state.isExporting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Export Printable PDF Worksheet", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Generate clean A4 study booklets offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { if (!state.isExporting) onDismiss() },
                    modifier = Modifier.testTag("close_pdf_export_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close PDF export")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Question Scope", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scopes.forEach { (key, label) ->
                        FilterChip(
                            selected = state.sourceKey == key,
                            onClick = { onUpdate { it.copy(sourceKey = key) } },
                            label = { Text(label) },
                            modifier = Modifier.testTag("pdf_scope_$key")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Difficulty Level", style = MaterialTheme.typography.labelLarge)
                DifficultyFilterBar(
                    selected = state.difficultyFilter,
                    onSelect = { diff -> onUpdate { it.copy(difficultyFilter = diff) } },
                    testTagPrefix = "pdf_diff"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Worksheet Size", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    limits.forEach { (limit, label) ->
                        FilterChip(
                            selected = state.questionLimit == limit,
                            onClick = { onUpdate { it.copy(questionLimit = limit) } },
                            label = { Text(label) },
                            modifier = Modifier.testTag("pdf_limit_$limit")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Layout Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.inlineAnswers,
                        onClick = { onUpdate { it.copy(inlineAnswers = false) } },
                        label = { Text("Mock Booklet") },
                        modifier = Modifier.testTag("pdf_mode_mock")
                    )
                    FilterChip(
                        selected = state.inlineAnswers,
                        onClick = { onUpdate { it.copy(inlineAnswers = true) } },
                        label = { Text("Inline Study Sheet") },
                        modifier = Modifier.testTag("pdf_mode_study")
                    )
                }

                if (!state.inlineAnswers) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Answer Key & Rationales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Appends answer grid and clinical rationales at the end",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.includeAnswerKey,
                            onCheckedChange = { checked -> onUpdate { it.copy(includeAnswerKey = checked) } },
                            modifier = Modifier.testTag("pdf_answer_key_switch")
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (matchingCount > 0) {
                            "$matchingCount MCQs ready  •  ~$estimatedPages A4 pages"
                        } else {
                            "No matching MCQs found for this filter"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = MonoFontFamily,
                        color = if (matchingCount > 0) {
                            MaterialTheme.editorialColors.correctText
                        } else {
                            MaterialTheme.editorialColors.wrongText
                        }
                    )
                    Text(
                        text = "File: $suggestedFileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onExportPdf,
                enabled = matchingCount > 0 && !state.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_pdf_worksheet_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating PDF...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save PDF Worksheet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPickerModalSheet(
    isVisible: Boolean,
    professions: List<Profession>,
    exams: List<Exam>,
    activeExamId: String,
    examQuestionCounts: Map<String, Int>,
    onSelectExam: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
                .testTag("exam_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Active Exam Track", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Choose your exam track",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_exam_picker_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close exam picker")
                }
            }

            val examsByProfession = exams.groupBy { it.professionId }
            professions.forEach { profession ->
                val profExams = examsByProfession[profession.id].orEmpty()
                if (profExams.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = profession.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        profExams.forEach { exam ->
                            val isSelected = exam.id == activeExamId
                            val qCount = examQuestionCounts[exam.id] ?: 0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clickable { onSelectExam(exam.id) }
                                    .testTag("exam_option_${exam.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = exam.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = "${formatApproxQuestionCount(qCount)}  •  ${exam.defaultMockQuestions}Q / ${exam.defaultMockMinutes}m  •  -${exam.negativeMarks}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Active exam",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "More healthcare exams are coming soon. Your progress and bookmarks stay synced across exams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

