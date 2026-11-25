package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionNote
import com.example.data.model.QuestionNotesLogic
import com.example.data.model.SpacedRepetitionLogic
import com.example.data.model.SrsNotebookEntry
import com.example.data.model.SrsNotebookSummary
import com.example.data.model.SrsStage
import com.example.ui.components.QuestionNoteEditorDialog
import com.example.ui.components.QuestionSummaryCard
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState

private enum class DeckTab {
    SRS,
    NOTES,
    BOOKMARKS,
    MISTAKES
}

@Composable
fun BookmarksScreen(
    state: DashboardDerivedState,
    srsSummary: SrsNotebookSummary = SrsNotebookSummary(),
    onPracticeDeck: (List<QuestionItem>, String) -> Unit,
    onOpenPdfExport: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onResetSrsCard: (String) -> Unit = {},
    onRemoveSrsCard: (String) -> Unit = {},
    questionNotes: Map<String, QuestionNote> = emptyMap(),
    onSaveQuestionNote: (String, String) -> Unit = { _, _ -> },
    onDeleteQuestionNote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DeckTab.SRS) }
    var notesSearchQuery by remember { mutableStateOf("") }
    var editingNoteItem by remember { mutableStateOf<QuestionItem?>(null) }
    val editorial = MaterialTheme.editorialColors

    val saved = remember(state.questions) { state.questions.filter { it.isBookmarked } }
    val mistakes = remember(state.questions) { state.questions.filter { it.lastOutcome == "wrong" } }
    val allNotedEntries = remember(state.questions, questionNotes) {
        QuestionNotesLogic.searchNotedQuestions(state.questions, questionNotes, "")
    }
    val filteredNotedEntries = remember(state.questions, questionNotes, notesSearchQuery) {
        QuestionNotesLogic.searchNotedQuestions(state.questions, questionNotes, notesSearchQuery)
    }
    val dueSrsItems = remember(srsSummary.dueEntries) { srsSummary.dueEntries.map { it.item } }
    val allSrsItems = remember(srsSummary.allEntries) { srsSummary.allEntries.map { it.item } }

    val activeTitle = when (activeTab) {
        DeckTab.SRS -> "SRS Mistake Notebook"
        DeckTab.NOTES -> "My Margin Notes"
        DeckTab.BOOKMARKS -> "Revision Deck"
        DeckTab.MISTAKES -> "Recent Mistakes"
    }
    val activeSubtitle = when (activeTab) {
        DeckTab.SRS -> "${srsSummary.dueEntries.size} due now · ${srsSummary.upcomingEntries.size} scheduled · ${srsSummary.masteredEntries.size} mastered"
        DeckTab.NOTES -> "${allNotedEntries.size} annotated MCQs with your personal revision hooks"
        DeckTab.BOOKMARKS -> "${saved.size} bookmarked questions ready for revision"
        DeckTab.MISTAKES -> "${mistakes.size} recently missed questions"
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "deck_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                activeTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                activeSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activeTab != DeckTab.SRS) {
                            val listForTab = when (activeTab) {
                                DeckTab.NOTES -> filteredNotedEntries.map { it.first }
                                DeckTab.MISTAKES -> mistakes
                                else -> saved
                            }
                            if (listForTab.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (activeTab != DeckTab.NOTES) {
                                        OutlinedButton(
                                            onClick = {
                                                onOpenPdfExport(if (activeTab == DeckTab.MISTAKES) "MISTAKES" else "BOOKMARKS")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, editorial.borderSubtle),
                                            modifier = Modifier
                                                .heightIn(min = 44.dp)
                                                .testTag("deck_export_pdf_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PictureAsPdf,
                                                contentDescription = "Export PDF"
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onPracticeDeck(listForTab, activeTitle) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .heightIn(min = 44.dp)
                                            .testTag("practice_deck_btn")
                                    ) {
                                        Text("Practice (${listForTab.size})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    SegmentedPillBar(
                        options = DeckTab.entries,
                        selected = activeTab,
                        labelOf = { tab ->
                            when (tab) {
                                DeckTab.SRS -> "SRS (${srsSummary.dueEntries.size})"
                                DeckTab.NOTES -> "Notes (${allNotedEntries.size})"
                                DeckTab.BOOKMARKS -> "Saved (${saved.size})"
                                DeckTab.MISTAKES -> "Missed (${mistakes.size})"
                            }
                        },
                        onSelect = { activeTab = it },
                        testTagOf = { tab ->
                            when (tab) {
                                DeckTab.SRS -> "deck_tab_srs"
                                DeckTab.NOTES -> "deck_tab_notes"
                                DeckTab.BOOKMARKS -> "deck_tab_bookmarks"
                                DeckTab.MISTAKES -> "deck_tab_mistakes"
                            }
                        }
                    )
                }
            }

            when (activeTab) {
                DeckTab.SRS -> {
                    item(key = "srs_summary_panel") {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(
                                width = if (srsSummary.dueEntries.isNotEmpty()) 1.5.dp else 1.dp,
                                color = if (srsSummary.dueEntries.isNotEmpty()) editorial.acidLime else editorial.borderSubtle
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SrsMetricPill(
                                        label = "DUE NOW",
                                        value = "${srsSummary.dueEntries.size}",
                                        highlight = srsSummary.dueEntries.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    SrsMetricPill(
                                        label = "3d–14d",
                                        value = "${srsSummary.upcomingEntries.size}",
                                        highlight = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SrsMetricPill(
                                        label = "MASTERED",
                                        value = "${srsSummary.masteredEntries.size}",
                                        highlight = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Spaced Repetition: Wrong answers enter Box 1d. Each correct recall advances 1d → 3d → 7d → 14d → Mastered.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val drillPool = dueSrsItems.ifEmpty { allSrsItems }
                                            if (drillPool.isNotEmpty()) {
                                                val drillLabel = if (dueSrsItems.isNotEmpty()) {
                                                    "SRS Due Review (${dueSrsItems.size}Q)"
                                                } else {
                                                    "SRS Full Mistake Drill (${drillPool.size}Q)"
                                                }
                                                onPracticeDeck(drillPool, drillLabel)
                                            }
                                        },
                                        enabled = allSrsItems.isNotEmpty(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = editorial.acidLime,
                                            contentColor = editorial.onAcidLime
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("start_srs_due_drill_btn")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            text = if (dueSrsItems.isNotEmpty()) {
                                                "Start Due SRS Drill (${dueSrsItems.size})"
                                            } else {
                                                "Review All SRS (${allSrsItems.size})"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (srsSummary.allEntries.isEmpty()) {
                        item(key = "srs_empty") {
                            DeckEmptyCard(
                                message = "Your Spaced Repetition Mistake Notebook is clear! Any MCQ you miss during practice or CBT mocks automatically enters your 1d → 3d → 7d → 14d mastery queue."
                            )
                        }
                    } else {
                        items(
                            items = srsSummary.allEntries,
                            key = { "srs_${it.item.id}" }
                        ) { entry ->
                            SrsMistakeRowCard(
                                entry = entry,
                                onPracticeSingle = {
                                    onPracticeDeck(listOf(entry.item), "SRS Review · ${entry.item.question.subject}")
                                },
                                onToggleBookmark = { onToggleBookmark(entry.item.id) },
                                onResetToBox1 = { onResetSrsCard(entry.item.id) },
                                onRemoveFromSrs = { onRemoveSrsCard(entry.item.id) }
                            )
                        }
                    }
                }

                DeckTab.NOTES -> {
                    if (allNotedEntries.isNotEmpty()) {
                        item(key = "notes_search_box") {
                            OutlinedTextField(
                                value = notesSearchQuery,
                                onValueChange = { notesSearchQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                label = { Text("Search your notes, subjects, or questions") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deck_notes_search_input")
                            )
                        }
                    }
                    if (filteredNotedEntries.isEmpty()) {
                        item(key = "notes_empty") {
                            DeckEmptyCard(
                                message = if (allNotedEntries.isEmpty()) {
                                    "No personal margin notes yet. Tap the note icon on any MCQ during practice to save your memory hooks here."
                                } else {
                                    "No notes match \"$notesSearchQuery\"."
                                }
                            )
                        }
                    } else {
                        items(
                            items = filteredNotedEntries,
                            key = { "note_${it.first.id}" }
                        ) { (item, note) ->
                            PersonalNoteDeckCard(
                                item = item,
                                note = note,
                                onPracticeSingle = {
                                    onPracticeDeck(listOf(item), "Note Review · ${item.question.subject}")
                                },
                                onEditNote = { editingNoteItem = item },
                                onDeleteNote = { onDeleteQuestionNote(item.id) }
                            )
                        }
                    }
                }

                DeckTab.BOOKMARKS, DeckTab.MISTAKES -> {
                    val activeList = if (activeTab == DeckTab.MISTAKES) mistakes else saved
                    if (activeList.isEmpty()) {
                        item(key = "deck_empty_${activeTab.name}") {
                            DeckEmptyCard(
                                message = if (activeTab == DeckTab.MISTAKES) {
                                    "No recent mistakes yet. Keep practicing."
                                } else {
                                    "No bookmarked questions yet. Tap the bookmark icon during practice to save items here."
                                }
                            )
                        }
                    } else {
                        itemsIndexed(activeList, key = { _, item -> item.id }) { idx, item ->
                            QuestionSummaryCard(
                                item = item,
                                indexLabel = "${idx + 1}.",
                                onToggleBookmark = { onToggleBookmark(item.id) },
                                onClick = { onPracticeDeck(activeList, activeTitle) }
                            )
                        }
                    }
                }
            }
        }

        val targetEdit = editingNoteItem
        if (targetEdit != null) {
            QuestionNoteEditorDialog(
                questionStem = targetEdit.question.questionText,
                subject = targetEdit.question.subject,
                existingNote = questionNotes[targetEdit.id]?.text.orEmpty(),
                onSave = { updated ->
                    onSaveQuestionNote(targetEdit.id, updated)
                    editingNoteItem = null
                },
                onDelete = {
                    onDeleteQuestionNote(targetEdit.id)
                    editingNoteItem = null
                },
                onDismiss = { editingNoteItem = null }
            )
        }
    }
}

@Composable
private fun PersonalNoteDeckCard(
    item: QuestionItem,
    note: QuestionNote,
    onPracticeSingle: () -> Unit,
    onEditNote: () -> Unit,
    onDeleteNote: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle)
                ) {
                    Text(
                        text = item.question.subject,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditNote, modifier = Modifier.testTag("deck_note_edit_${item.id}")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit note", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteNote, modifier = Modifier.testTag("deck_note_delete_${item.id}")) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete note",
                            tint = editorial.wrongText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = item.question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = editorial.acidLime.copy(alpha = if (editorial.isDark) 0.14f else 0.18f),
                border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.StickyNote2,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            OutlinedButton(
                onClick = onPracticeSingle,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Practice Question", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SrsMetricPill(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) editorial.acidLime.copy(alpha = 0.16f) else editorial.elevatedSurface,
        border = BorderStroke(
            1.dp,
            if (highlight) editorial.acidLime.copy(alpha = 0.6f) else editorial.borderSubtle
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SrsMistakeRowCard(
    entry: SrsNotebookEntry,
    onPracticeSingle: () -> Unit,
    onToggleBookmark: () -> Unit,
    onResetToBox1: () -> Unit,
    onRemoveFromSrs: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val dueLabel = remember(entry.card) {
        SpacedRepetitionLogic.formatNextReviewLabel(entry.card)
    }
    val stageBg = when {
        entry.card.stage == SrsStage.MASTERED -> editorial.correctBg
        entry.isDueNow -> editorial.acidLime
        else -> editorial.elevatedSurface
    }
    val stageFg = when {
        entry.card.stage == SrsStage.MASTERED -> editorial.correctText
        entry.isDueNow -> editorial.onAcidLime
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onPracticeSingle,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, if (entry.isDueNow) editorial.acidLime.copy(alpha = 0.45f) else editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = stageBg) {
                        Text(
                            text = "BOX ${entry.card.stage.badgeLabel.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = stageFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Text(
                            text = entry.item.question.subject,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = dueLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isDueNow) editorial.wrongText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = entry.item.question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Streak: ${entry.card.consecutiveCorrect}x right",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (entry.item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Toggle bookmark",
                            tint = if (entry.item.isBookmarked) editorial.acidLime else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onResetToBox1) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset card to Box 1d",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onRemoveFromSrs) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove from SRS notebook",
                            tint = editorial.wrongText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckEmptyCard(message: String) {
    val editorial = MaterialTheme.editorialColors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
