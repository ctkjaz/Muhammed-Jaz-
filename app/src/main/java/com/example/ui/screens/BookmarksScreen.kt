package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.QuestionItem
import com.example.ui.components.QuestionSummaryCard
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState

@Composable
fun BookmarksScreen(
    state: DashboardDerivedState,
    onPracticeDeck: (List<QuestionItem>, String) -> Unit,
    onOpenPdfExport: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMistakesOnly by remember { mutableStateOf(false) }
    val editorial = MaterialTheme.editorialColors

    val saved = remember(state.questions) { state.questions.filter { it.isBookmarked } }
    val mistakes = remember(state.questions) { state.questions.filter { it.lastOutcome == "wrong" } }
    val activeList = if (showMistakesOnly) mistakes else saved
    val activeTitle = if (showMistakesOnly) "Mistakes Deck" else "Revision Deck"

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
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
                                "${activeList.size} questions ready for revision",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activeList.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onOpenPdfExport(if (showMistakesOnly) "MISTAKES" else "BOOKMARKS")
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
                                Button(
                                    onClick = { onPracticeDeck(activeList, activeTitle) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("practice_deck_btn")
                                ) {
                                    Text("Practice (${activeList.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    SegmentedPillBar(
                        options = listOf(false, true),
                        selected = showMistakesOnly,
                        labelOf = { isMistakes ->
                            if (isMistakes) "Recent Mistakes (${mistakes.size})" else "Bookmarks (${saved.size})"
                        },
                        onSelect = { showMistakesOnly = it },
                        testTagOf = { isMistakes ->
                            if (isMistakes) "deck_tab_mistakes" else "deck_tab_bookmarks"
                        }
                    )
                }
            }

            if (activeList.isEmpty()) {
                item {
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
                                Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (showMistakesOnly) {
                                    "No recent mistakes yet. Keep practicing."
                                } else {
                                    "No bookmarked questions yet. Tap the bookmark icon during practice to save items here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
