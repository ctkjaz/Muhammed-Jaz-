package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.SubjectCatalog
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.DoubtAskContext
import com.example.data.notifications.DoubtStatus
import com.example.data.notifications.EngagementLogic
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import java.util.Locale

private enum class DoubtFilterTab(val label: String) {
    ALL("All"),
    OPEN("Open"),
    ANSWERED("Answered"),
    CLOSED("Resolved")
}

@Composable
fun DoubtBoxScreen(
    doubts: List<CloudDoubt>,
    highlightedDoubtId: String?,
    onOpenAskDoubt: () -> Unit,
    onMarkDoubtRead: (String) -> Unit,
    onCloseDoubt: (String) -> Unit,
    onDeleteDoubt: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    var selectedFilter by remember { mutableStateOf(DoubtFilterTab.ALL) }

    val filteredDoubts = remember(doubts, selectedFilter) {
        when (selectedFilter) {
            DoubtFilterTab.ALL -> doubts
            DoubtFilterTab.OPEN -> doubts.filter { it.statusEnum == DoubtStatus.OPEN }
            DoubtFilterTab.ANSWERED -> doubts.filter { it.statusEnum == DoubtStatus.ANSWERED }
            DoubtFilterTab.CLOSED -> doubts.filter { it.statusEnum == DoubtStatus.CLOSED }
        }
    }

    // Automatically mark highlighted doubt's reply as read when deep-linked
    LaunchedEffect(highlightedDoubtId, doubts) {
        if (!highlightedDoubtId.isNullOrBlank()) {
            val match = doubts.firstOrNull { it.doubtId == highlightedDoubtId }
            if (match != null && match.hasUnreadReply) {
                onMarkDoubtRead(match.doubtId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("doubt_box_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Doubt Box",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ask clinical & syllabus doubts directly to ClinEdge Faculty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Status Filter Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DoubtFilterTab.entries.forEach { tab ->
                    val count = when (tab) {
                        DoubtFilterTab.ALL -> doubts.size
                        DoubtFilterTab.OPEN -> doubts.count { it.statusEnum == DoubtStatus.OPEN }
                        DoubtFilterTab.ANSWERED -> doubts.count { it.statusEnum == DoubtStatus.ANSWERED }
                        DoubtFilterTab.CLOSED -> doubts.count { it.statusEnum == DoubtStatus.CLOSED }
                    }
                    MonochromeFilterPill(
                        label = "${tab.label} ($count)",
                        selected = selectedFilter == tab,
                        onClick = { selectedFilter = tab },
                        modifier = Modifier.testTag("doubt_filter_${tab.name}")
                    )
                }
            }

            HorizontalDivider(color = editorial.borderSubtle, modifier = Modifier.padding(top = 6.dp))

            if (filteredDoubts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.QuestionAnswer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = if (doubts.isEmpty()) "No doubts asked yet" else "No doubts in this filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stuck on a drug dosage, nursing priority, or MCQ rationale? Ask here or tap \"Ask Doubt\" on any question.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onOpenAskDoubt,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("doubt_empty_ask_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Ask Your First Doubt", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = filteredDoubts, key = { it.doubtId }) { doubt ->
                        StudentDoubtCard(
                            doubt = doubt,
                            isHighlighted = doubt.doubtId == highlightedDoubtId,
                            onMarkRead = { onMarkDoubtRead(doubt.doubtId) },
                            onClose = { onCloseDoubt(doubt.doubtId) },
                            onDelete = { onDeleteDoubt(doubt.doubtId) }
                        )
                    }
                }
            }
        }

        // Primary FAB to submit a new doubt
        ExtendedFloatingActionButton(
            onClick = onOpenAskDoubt,
            containerColor = editorial.acidLime,
            contentColor = editorial.onAcidLime,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Ask a Doubt", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("doubt_box_ask_fab")
        )
    }
}

@Composable
private fun StudentDoubtCard(
    doubt: CloudDoubt,
    isHighlighted: Boolean,
    onMarkRead: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val examShort = PlatformCatalogDefaults.examById(doubt.examId)?.shortName ?: doubt.examId
    val status = doubt.statusEnum
    val hasUnreadReply = doubt.hasUnreadReply

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            width = if (isHighlighted || hasUnreadReply) 1.5.dp else 1.dp,
            color = if (isHighlighted || hasUnreadReply) AcidLime else editorial.borderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasUnreadReply) { onMarkRead() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges + Status + Timestamp
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
                        shape = RoundedCornerShape(6.dp),
                        color = when (status) {
                            DoubtStatus.ANSWERED -> AcidLime
                            DoubtStatus.OPEN -> editorial.elevatedSurface
                            DoubtStatus.CLOSED -> editorial.correctBg
                        }
                    ) {
                        Text(
                            text = status.label.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = when (status) {
                                DoubtStatus.ANSWERED -> PureBlack
                                DoubtStatus.OPEN -> MaterialTheme.colorScheme.onSurface
                                DoubtStatus.CLOSED -> editorial.correctText
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = editorial.elevatedSurface) {
                        Text(
                            text = examShort,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = editorial.elevatedSurface) {
                        Text(
                            text = doubt.subject,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = formatEngagementTimestamp(doubt.createdAtMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Linked MCQ preview (if submitted from a question card)
            if (doubt.questionPreview.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "LINKED MCQ ${doubt.questionId.take(18)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = doubt.questionPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Student's question
            Text(
                text = doubt.doubtText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Faculty Reply Box (when answered)
            if (doubt.adminReply.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.explanationSurface,
                    border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Faculty verified reply",
                                    tint = if (editorial.isDark) AcidLime else editorial.correctText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CLINEDGE FACULTY REPLY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (editorial.isDark) AcidLime else editorial.correctText
                                )
                            }
                            if (hasUnreadReply) {
                                TextButton(
                                    onClick = onMarkRead,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("NEW · Mark Read", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            } else if (doubt.repliedAtMs > 0L) {
                                Text(
                                    text = formatEngagementTimestamp(doubt.repliedAtMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = doubt.adminReply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (status == DoubtStatus.OPEN) {
                Text(
                    text = "Awaiting faculty review · You'll receive a notification as soon as it's answered.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Card Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status != DoubtStatus.CLOSED) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Mark Resolved", fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete doubt",
                        tint = editorial.wrongText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AskDoubtModalSheet(
    context: DoubtAskContext?,
    isBusy: Boolean,
    onSubmit: (examId: String, subject: String, questionId: String, questionPreview: String, doubtText: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (context == null) return

    val editorial = MaterialTheme.editorialColors
    val isLinkedToQuestion = context.questionId.isNotBlank()
    var selectedExamId by remember(context) { mutableStateOf(context.examId) }
    var selectedSubject by remember(context) { mutableStateOf(context.subject) }
    var doubtText by remember(context) { mutableStateOf("") }
    var validationMsg by remember(context) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.QuestionAnswer, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(
                    text = if (isLinkedToQuestion) "Ask Doubt on MCQ" else "Ask ClinEdge Faculty",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLinkedToQuestion && context.questionPreview.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${PlatformCatalogDefaults.examById(selectedExamId)?.shortName ?: selectedExamId} · $selectedSubject",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = context.questionPreview,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Exam Track",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlatformCatalogDefaults.DEFAULT_EXAMS.forEach { exam ->
                            MonochromeFilterPill(
                                label = exam.shortName,
                                selected = selectedExamId == exam.id,
                                onClick = { selectedExamId = exam.id }
                            )
                        }
                    }
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubjectCatalog.ALL_SUBJS.forEach { subj ->
                            MonochromeFilterPill(
                                label = subj.code,
                                selected = selectedSubject == subj.code,
                                onClick = { selectedSubject = subj.code }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = doubtText,
                    onValueChange = {
                        doubtText = it.take(EngagementLogic.MAX_DOUBT_LENGTH)
                        validationMsg = null
                    },
                    label = { Text("Describe your doubt clearly") },
                    placeholder = {
                        Text(
                            if (isLinkedToQuestion) "e.g., Why is Option B preferred over Option C in this clinical scenario?"
                            else "e.g., Confused about oxytocin vs ergometrine contraindications in PPH..."
                        )
                    },
                    minLines = 4,
                    maxLines = 7,
                    isError = validationMsg != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(validationMsg ?: "Admin reply will notify you automatically.")
                            Text(
                                text = "${doubtText.length}/${EngagementLogic.MAX_DOUBT_LENGTH}",
                                fontFamily = MonoFontFamily
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_doubt_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = EngagementLogic.validateDoubtText(doubtText)
                    if (err != null) {
                        validationMsg = err
                    } else {
                        onSubmit(
                            selectedExamId,
                            selectedSubject,
                            context.questionId,
                            context.questionPreview,
                            doubtText.trim()
                        )
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("ask_doubt_submit_btn")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = editorial.onAcidLime
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Sending...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Submit Doubt", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isBusy,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
