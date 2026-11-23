package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.backup.BackupFolderSummary
import com.example.data.export.PdfWorksheetWriter
import com.example.data.model.DifficultyFilter
import com.example.data.model.Exam
import com.example.data.model.ExamCategory
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.Profession
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionSourceType
import com.example.data.model.SubjectCatalog
import com.example.data.update.AppUpdateUiState
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SemanticWrongFill
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.ImporterUiState
import com.example.ui.viewmodel.PdfExportConfigState
import com.example.ui.viewmodel.TimedPracticeConfigState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimedPracticeConfigDialog(
    state: TimedPracticeConfigState,
    @Suppress("UNUSED_PARAMETER") allQuestions: List<QuestionItem>,
    activeExamId: String = "rrb_nursing",
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

    val editorial = MaterialTheme.editorialColors
    val activeExam = remember(activeExamId) { PlatformCatalogDefaults.examById(activeExamId) }
    val examShort = activeExam?.shortName ?: "Exam"
    val isPureNursing = PlatformCatalogDefaults.examIsPureNursing(activeExamId)
    val supportsCaGk = PlatformCatalogDefaults.examSupportsCaGk(activeExamId)
    val supportsNonTech = PlatformCatalogDefaults.examSupportsNonTech(activeExamId)
    val nurPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExamId)
    val nonPct = 100 - nurPct
    val negMarks = PlatformCatalogDefaults.negativeMarksForExam(activeExamId)
    val markingSummary = if (state.useCrossExamPool) {
        "+1 / -0.33"
    } else {
        activeExam?.let { PlatformCatalogDefaults.formatMarkingLabel(it) } ?: "+1 / -0.33"
    }
    val negLabel = if (state.useCrossExamPool) {
        "0.33"
    } else if (negMarks <= 0.0) {
        "0"
    } else {
        String.format(java.util.Locale.US, "%.2f", negMarks).trimEnd('0').trimEnd('.')
    }
    val sourceOptions = remember(activeExamId, state.useCrossExamPool) {
        val visibleSubjs = if (state.useCrossExamPool) {
            SubjectCatalog.ALL_SUBJS
        } else {
            SubjectCatalog.ALL_SUBJS.filter { PlatformCatalogDefaults.isSharedSubjectForExam(activeExamId, it.code) }
        }
        val baseGroups = if (state.useCrossExamPool) {
            listOf(
                "MIXED" to "All Subjects",
                "NURSING" to "Nursing Core",
                "PYQ" to "All PYQs"
            )
        } else {
            buildList {
                add("MIXED" to "Mixed (All)")
                add("PYQ" to "$examShort PYQs")
                add("NURSING" to "Nursing ($nurPct%)")
                if (!isPureNursing && supportsCaGk) {
                    add("CA" to if (PlatformCatalogDefaults.isNorcetExam(activeExamId)) "GK & Health" else "CA & GK")
                }
                if (!isPureNursing && supportsNonTech) {
                    add("GEN" to "Non-Nursing ($nonPct%)")
                }
            }
        }
        baseGroups + visibleSubjs.map { it.code to "${it.code} (${it.name})" }
    }
    val availableCount = filterCountProvider(state.sourceKey, state.difficultyFilter)
    val effectiveCount = if (state.questionCount == 0) availableCount else minOf(state.questionCount, availableCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = editorial.cardSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Text("Timed Practice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (state.useCrossExamPool) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.acidLime
                    ) {
                        val scopeTxt = state.crossExamFilterId?.let { id ->
                            PlatformCatalogDefaults.DEFAULT_EXAMS.firstOrNull { it.id == id }?.shortName ?: "Filtered Exam"
                        } ?: "All Exams Combined"
                        Text(
                            text = "By Subject · $scopeTxt",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = editorial.onAcidLime,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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
                    text = "Select Subject or Set",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sourceOptions.forEach { (key, label) ->
                        MonochromeFilterPill(
                            label = label,
                            selected = state.sourceKey == key,
                            onClick = { onSelectSource(key) },
                            modifier = Modifier.testTag("timed_src_$key")
                        )
                    }
                }

                Text(
                    text = "Difficulty Level",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                DifficultyFilterBar(
                    selected = state.difficultyFilter,
                    onSelect = onSelectDifficulty,
                    testTagPrefix = "timed_diff"
                )

                Text(
                    text = "Questions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15 Qs", 30 to "30 Qs", 50 to "50 Qs", 100 to "Random 100").forEach { (cnt, lbl) ->
                        MonochromeFilterPill(
                            label = lbl,
                            selected = state.questionCount == cnt,
                            onClick = { onSelectCount(cnt) },
                            modifier = Modifier.testTag("timed_cnt_$cnt")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timer Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (useTimeLimit) "${state.durationMinutes} min" else "Off (Settings)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                        color = if (useTimeLimit) {
                            if (editorial.isDark) AcidLime else PureBlack
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!useTimeLimit) {
                    Text(
                        text = "Time limit is off in Settings. Drill will run untimed.",
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
                        colors = SliderDefaults.colors(
                            thumbColor = if (editorial.isDark) AcidLime else PureBlack,
                            activeTrackColor = if (editorial.isDark) AcidLime else PureBlack,
                            inactiveTrackColor = editorial.elevatedSurface
                        ),
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
                        MonochromeFilterPill(
                            label = "${m}m",
                            selected = useTimeLimit && state.durationMinutes == m,
                            onClick = { if (useTimeLimit) onSelectMinutes(m) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (negMarks <= 0.0 && !state.useCrossExamPool) {
                                "Negative Marking (Optional)"
                            } else {
                                "Negative Marking (-$negLabel)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$markingSummary exam scoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = negativeMarking,
                        onCheckedChange = { onToggleNegativeMarking() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = AcidLime,
                            checkedBorderColor = AcidLime
                        ),
                        modifier = Modifier.testTag("timed_neg_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                enabled = effectiveCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("start_timed_practice_button")
            ) {
                Text(
                    if (useTimeLimit) {
                        "Start ($effectiveCount Qs · ${state.durationMinutes}m)"
                    } else {
                        "Start ($effectiveCount Qs)"
                    },
                    fontWeight = FontWeight.Bold
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
    backupFolderSummary: BackupFolderSummary = BackupFolderSummary(),
    isBackupBusy: Boolean = false,
    onUpdate: ((ImporterUiState) -> ImporterUiState) -> Unit,
    onPickPdfFile: () -> Unit = {},
    onPreviewPdf: () -> Unit,
    onCommitPdf: () -> Unit,
    onImportJson: () -> Unit,
    onChooseBackupFolder: () -> Unit = {},
    onResetBackupFolder: () -> Unit = {},
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onClearCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    val editorial = MaterialTheme.editorialColors
    var confirmClear by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = editorial.cardSurface,
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Import & Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Import shift questions or back up profile, plan & progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Text(
                            text = "Active Exam: $activeExamShortName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close importer")
                }
            }

            SegmentedPillBar(
                options = listOf(0, 1),
                selected = state.selectedTab,
                labelOf = { idx -> if (idx == 0) "PDF Text Parser" else "Backup & Batch" },
                onSelect = { idx -> onUpdate { it.copy(selectedTab = idx) } }
            )

            if (state.selectedTab == 0) {
                PdfTextImportPane(
                    state = state,
                    onUpdate = onUpdate,
                    onPickPdfFile = onPickPdfFile,
                    onPreviewPdf = onPreviewPdf,
                    onCommitPdf = onCommitPdf
                )
            } else {
                JsonBackupPane(
                    state = state,
                    backupFolderSummary = backupFolderSummary,
                    isBackupBusy = isBackupBusy,
                    onUpdate = onUpdate,
                    onImportJson = onImportJson,
                    onChooseBackupFolder = onChooseBackupFolder,
                    onResetBackupFolder = onResetBackupFolder,
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
            containerColor = editorial.cardSurface,
            title = { Text("Reset Custom Questions?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete all custom imported questions? Built-in MCQs stay untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClear = false
                        onClearCustom()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SemanticWrongFill,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Delete Custom", fontWeight = FontWeight.Bold)
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
    onPickPdfFile: () -> Unit,
    onPreviewPdf: () -> Unit,
    onCommitPdf: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val categories = listOf(
        "pyq" to "PYQ",
        "mock" to "Predicted Mock",
        "nur" to "Nursing Core",
        "ca" to "GK & Health",
        "gen" to "Aptitude / Sci"
    )
    val subjects = SubjectCatalog.ALL_SUBJS.map { it.code }
    var catExpanded by remember { mutableStateOf(false) }
    var subjExpanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = onPickPdfFile,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("importer_pick_pdf_btn")
    ) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Select PDF / TXT / JSON File", fontWeight = FontWeight.SemiBold)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Source Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuestionSourceType.entries.forEach { st ->
                MonochromeFilterPill(
                    label = st.label,
                    selected = state.sourceType == st,
                    onClick = {
                        onUpdate { current ->
                            val alignedCategory = when (st) {
                                QuestionSourceType.OFFICIAL_PYQ,
                                QuestionSourceType.MEMORY_BASED -> "pyq"
                                QuestionSourceType.PREDICTED_MOCK -> "mock"
                                QuestionSourceType.CURRENT_AFFAIRS -> "ca"
                                QuestionSourceType.PRACTICE -> if (current.category == "gen") "gen" else "nur"
                            }
                            current.copy(sourceType = st, category = alignedCategory)
                        }
                    },
                    modifier = Modifier.testTag("import_source_${st.dbValue}")
                )
            }
        }
        if (state.sourceType == QuestionSourceType.OFFICIAL_PYQ && state.sourceNote.isBlank()) {
            Text(
                text = "Add a source note below to mark as Official PYQ.",
                style = MaterialTheme.typography.bodySmall,
                color = editorial.warningText
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
                                    "mock" -> QuestionSourceType.PREDICTED_MOCK
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
        label = { Text("Source Note (for Verified status)") },
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
            Text("Mark Imported as Verified", style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (state.sourceNote.isBlank()) "Requires Source Note above." else "Saved as Verified.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = state.markAsVerified && state.sourceNote.isNotBlank(),
            enabled = state.sourceNote.isNotBlank(),
            onCheckedChange = { checked -> onUpdate { it.copy(markAsVerified = checked) } },
            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AcidLime),
            modifier = Modifier.testTag("pdf_mark_verified_switch")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Default Difficulty", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestionDifficulty.entries.forEach { diff ->
                MonochromeFilterPill(
                    label = diff.label,
                    selected = state.difficulty == diff,
                    onClick = { onUpdate { it.copy(difficulty = diff) } },
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
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("parse_preview_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Parse & Preview", fontWeight = FontWeight.Bold)
    }

    if (state.hasPreviewed) {
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
                    "Parsed ${state.previewQuestions.size} MCQ(s)",
                    color = editorial.correctText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onCommitPdf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_imported_button")
                ) {
                    Text("Save Permanently", fontWeight = FontWeight.Bold)
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
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        colors = CardDefaults.cardColors(containerColor = editorial.elevatedSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PyqSourceBadge(question = q)
                            Text("${idx + 1}. ${q.questionText}", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Ans: ${'A' + q.correctIndex} · ${q.options.getOrElse(q.correctIndex) { "" }}",
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
    backupFolderSummary: BackupFolderSummary,
    isBackupBusy: Boolean,
    onUpdate: ((ImporterUiState) -> ImporterUiState) -> Unit,
    onImportJson: () -> Unit,
    onChooseBackupFolder: () -> Unit,
    onResetBackupFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onRequestClearCustom: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Backup & Restore Folder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = backupFolderSummary.displayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onChooseBackupFolder,
                    modifier = Modifier.testTag("importer_choose_backup_folder_btn")
                ) {
                    Text("Change", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Includes student profile, plan, study goal, bookmarks, progress, and custom MCQs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportBackup,
                    enabled = !isBackupBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("export_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestoreBackup,
                    enabled = !isBackupBusy,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("restore_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore")
                }
            }

            if (backupFolderSummary.isCustomFolder) {
                TextButton(
                    onClick = onResetBackupFolder,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Use Default Folder")
                }
            }
        }
    }

    OutlinedTextField(
        value = state.jsonText,
        onValueChange = { txt -> onUpdate { it.copy(jsonText = txt) } },
        label = { Text("Batch Question Import") },
        placeholder = {
            Text(
                """[{"c":"pyq","y":"2025-S2","s":"MSN","d":"Medium","q":"...","o":["A","B","C","D"],"a":0,"e":"...","sourceStatus":"verified","sourceNote":"Official RRB 2025-S2 key"}]""",
                style = MaterialTheme.typography.bodySmall
            )
        },
        minLines = 4,
        maxLines = 7,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("json_batch_input")
    )

    OutlinedButton(
        onClick = onImportJson,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .testTag("import_json_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Import Question Batch", fontWeight = FontWeight.SemiBold)
    }

    OutlinedButton(
        onClick = onOpenPdfExport,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .testTag("importer_open_pdf_export_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Printable PDF Worksheet", fontWeight = FontWeight.SemiBold)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onRequestClearCustom,
            colors = ButtonDefaults.textButtonColors(contentColor = editorial.wrongText)
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

    val editorial = MaterialTheme.editorialColors
    val scopes = remember {
        listOf(
            "MIXED" to "All MCQs",
            "BOOKMARKS" to "Bookmarks",
            "MISTAKES" to "Mistakes",
            "LOWEST_ACCURACY" to "Lowest Accuracy",
            "PYQ" to "PYQs",
            "NURSING" to "Nursing Core",
            "CA" to "GK & Health",
            "GEN" to "Aptitude & Sci"
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
        containerColor = editorial.cardSurface,
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column {
                        Text("Export PDF Worksheet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "A4 study sheets with ClinEdge watermark",
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
                Text("Question Scope", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scopes.forEach { (key, label) ->
                        MonochromeFilterPill(
                            label = label,
                            selected = state.sourceKey == key,
                            onClick = { onUpdate { it.copy(sourceKey = key) } },
                            modifier = Modifier.testTag("pdf_scope_$key")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Difficulty Level", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                DifficultyFilterBar(
                    selected = state.difficultyFilter,
                    onSelect = { diff -> onUpdate { it.copy(difficultyFilter = diff) } },
                    testTagPrefix = "pdf_diff"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Worksheet Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    limits.forEach { (limit, label) ->
                        MonochromeFilterPill(
                            label = label,
                            selected = state.questionLimit == limit,
                            onClick = { onUpdate { it.copy(questionLimit = limit) } },
                            modifier = Modifier.testTag("pdf_limit_$limit")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Layout Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                SegmentedPillBar(
                    options = listOf(false, true),
                    selected = state.inlineAnswers,
                    labelOf = { inline -> if (inline) "Inline Study Sheet" else "Mock Booklet" },
                    onSelect = { inline -> onUpdate { it.copy(inlineAnswers = inline) } },
                    testTagOf = { inline -> if (inline) "pdf_mode_study" else "pdf_mode_mock" }
                )

                if (!state.inlineAnswers) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Answer Key & Rationales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Appends answer grid at the end",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.includeAnswerKey,
                            onCheckedChange = { checked -> onUpdate { it.copy(includeAnswerKey = checked) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AcidLime),
                            modifier = Modifier.testTag("pdf_answer_key_switch")
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (matchingCount > 0) {
                            "$matchingCount MCQs ready  ·  ~$estimatedPages A4 pages"
                        } else {
                            "No matching MCQs found"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (matchingCount > 0) editorial.correctText else editorial.wrongText
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
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
                        color = editorial.onAcidLime
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
    @Suppress("UNUSED_PARAMETER") professions: List<Profession>,
    exams: List<Exam>,
    activeExamId: String,
    @Suppress("UNUSED_PARAMETER") examQuestionCounts: Map<String, Int>,
    onSelectExam: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val editorial = MaterialTheme.editorialColors
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExamCategory?>(null) }

    val filteredExams = remember(exams, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        exams.filter { exam ->
            val catOk = selectedCategory == null ||
                PlatformCatalogDefaults.examCategoryFor(exam.id) == selectedCategory
            val qOk = q.isEmpty() ||
                exam.name.lowercase().contains(q) ||
                exam.shortName.lowercase().contains(q) ||
                exam.description.lowercase().contains(q) ||
                PlatformCatalogDefaults.examCategoryFor(exam.id).label.lowercase().contains(q)
            catOk && qOk
        }
    }
    val groupedByCategory = remember(filteredExams) {
        ExamCategory.entries.mapNotNull { cat ->
            val list = filteredExams.filter { PlatformCatalogDefaults.examCategoryFor(it.id) == cat }
            if (list.isEmpty()) null else cat to list
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = editorial.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("exam_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column {
                        Text("Active Exam Track", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${exams.size} All-India Nursing exams · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}",
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search AIIMS, ESIC, DSSSB, NEIGRIHMS, GIMS...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear exam search")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = editorial.borderSubtle,
                    focusedContainerColor = editorial.elevatedSurface,
                    unfocusedContainerColor = editorial.elevatedSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_picker_search")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonochromeFilterPill(
                    label = "All (${exams.size})",
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    modifier = Modifier.testTag("exam_cat_all")
                )
                ExamCategory.entries.forEach { cat ->
                    MonochromeFilterPill(
                        label = cat.shortLabel,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        modifier = Modifier.testTag("exam_cat_${cat.name.lowercase()}")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (groupedByCategory.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.elevatedSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("No matching nursing exams", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Try searching by institute, state, or exam short name.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    groupedByCategory.forEach { (category, catExams) ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${catExams.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            catExams.forEach { exam ->
                                val isSelected = exam.id == activeExamId
                                val hasStages = PlatformCatalogDefaults.supportsStages(exam.id)
                                val markingLabel = PlatformCatalogDefaults.formatMarkingLabel(exam)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clickable { onSelectExam(exam.id) }
                                        .testTag("exam_option_${exam.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) editorial.elevatedSurface else editorial.cardSurface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (editorial.isDark) editorial.acidLime else PureBlack
                                        } else {
                                            editorial.borderSubtle
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = exam.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (hasStages) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = editorial.acidLime
                                                    ) {
                                                        Text(
                                                            text = "2-Stage",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = editorial.onAcidLime,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}  ·  ${exam.defaultMockQuestions}Q / ${exam.defaultMockMinutes}m  ·  $markingLabel",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (exam.description.isNotBlank()) {
                                                Text(
                                                    text = exam.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Active exam",
                                                tint = if (editorial.isDark) editorial.acidLime else PureBlack,
                                                modifier = Modifier.size(22.dp)
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
    }
}

@Composable
fun AppUpdateAvailableDialog(
    updateState: AppUpdateUiState,
    onDownloadUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!updateState.showDialog || updateState.downloadUrl.isBlank()) return
    val editorial = MaterialTheme.editorialColors
    val isDirectApk = updateState.downloadUrl.endsWith(".apk", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = editorial.cardSurface,
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = if (editorial.isDark) AcidLime else PureBlack
            )
        },
        title = {
            Text(
                text = updateState.releaseTitle.ifBlank { "Update Available" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.acidLime
                ) {
                    Text(
                        text = "v${updateState.currentVersion}  →  v${updateState.latestVersion}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = editorial.onAcidLime,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = updateState.releaseNotes.ifBlank {
                        "A newer version of ClinEdge is ready to download."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isDirectApk) {
                    Text(
                        text = "Tap Download APK, then open the file to update with all study data intact.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownloadUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("update_dialog_download_btn")
            ) {
                Text(
                    text = if (isDirectApk) "Download APK" else "Open Release",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("update_dialog_later_btn")
            ) {
                Text("Later")
            }
        }
    )
}
