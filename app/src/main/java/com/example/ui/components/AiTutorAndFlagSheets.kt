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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiTutorUiState
import com.example.data.model.Question
import com.example.data.subscription.QuestionReportReason
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiClinicalTutorSheet(
    question: Question?,
    uiState: AiTutorUiState,
    onAskFollowUp: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    if (question == null) return
    val editorial = MaterialTheme.editorialColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var followUpInput by remember(question.id) { mutableStateOf("") }
    val isBusy = uiState is AiTutorUiState.Loading || uiState is AiTutorUiState.Streaming

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = editorial.cardSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = editorial.acidLime
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = editorial.onAcidLime,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AI Clinical Tutor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = editorial.elevatedSurface,
                                border = BorderStroke(1.dp, editorial.borderSubtle)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${question.subject} • Clinical priority & option elimination",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("ai_tutor_close_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close AI Clinical Tutor")
                }
            }

            // Question Preview Strip
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
                        text = question.questionText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    val correctOpt = question.options.getOrNull(question.correctIndex).orEmpty()
                    if (correctOpt.isNotBlank()) {
                        Text(
                            text = "Key: ${'A' + question.correctIndex}) $correctOpt",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = editorial.correctText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = editorial.acidLime,
                    trackColor = editorial.elevatedSurface
                )
            }

            // Response Surface
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = editorial.explanationSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (uiState) {
                        AiTutorUiState.Idle -> {
                            Text(
                                text = "Preparing clinical explanation...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiTutorUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = uiState.promptLabel.ifBlank { "Analyzing clinical concept..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is AiTutorUiState.Streaming -> {
                            if (uiState.promptLabel.isNotBlank()) {
                                Text(
                                    text = uiState.promptLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = uiState.text + " ▌",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("ai_tutor_streaming_text")
                            )
                        }
                        is AiTutorUiState.Success -> {
                            if (uiState.promptLabel.isNotBlank()) {
                                Text(
                                    text = uiState.promptLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = editorial.correctText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = uiState.text,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("ai_tutor_response_text")
                            )
                        }
                        is AiTutorUiState.Error -> {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = editorial.wrongText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Could not load AI Tutor explanation",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = editorial.wrongText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = onRetry,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .heightIn(min = 44.dp)
                                            .testTag("ai_tutor_retry_btn")
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text("Retry", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Follow-up Prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickChips = listOf(
                    "Simplify this" to "Simplify this clinical concept in 4 short beginner-friendly bullet points.",
                    "NORCET trap" to "What is the most common NORCET/RRB clinical trap or distractor tested on this topic?",
                    "Nursing priority" to "Give the exact step-by-step priority nursing interventions (1st, 2nd, 3rd) for this bedside scenario."
                )
                quickChips.forEachIndexed { idx, (label, prompt) ->
                    OutlinedButton(
                        onClick = { onAskFollowUp(prompt) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("ai_tutor_chip_$idx")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Custom 1-Line Follow-Up Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = followUpInput,
                    onValueChange = { followUpInput = it.take(240) },
                    placeholder = { Text("Ask a quick follow-up...") },
                    singleLine = true,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_tutor_followup_input")
                )
                Button(
                    onClick = {
                        val trimmed = followUpInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAskFollowUp(trimmed)
                            followUpInput = ""
                        }
                    },
                    enabled = !isBusy && followUpInput.trim().length >= 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcidLime,
                        contentColor = PureBlack
                    ),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("ai_tutor_followup_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send follow-up",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlagQuestionModalSheet(
    question: Question?,
    isBusy: Boolean,
    onSubmit: (Question, QuestionReportReason, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (question == null) return
    val editorial = MaterialTheme.editorialColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember(question.id) { mutableStateOf(QuestionReportReason.WRONG_KEY) }
    var noteText by remember(question.id) { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = editorial.cardSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = editorial.wrongText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Flag Question Issue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close flag sheet")
                }
            }

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
                        text = "${question.subject} • ID: ${question.id}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "What seems wrong with this MCQ?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestionReportReason.entries.forEach { reason ->
                    MonochromeFilterPill(
                        label = reason.label,
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        modifier = Modifier.testTag("flag_reason_${reason.dbValue}")
                    )
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it.take(800) },
                label = { Text("Textbook reference or details (optional)") },
                placeholder = { Text("e.g., Option B is correct per Brunner & Suddarth 15th ed.") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flag_note_input")
            )

            HorizontalDivider(color = editorial.borderSubtle)

            Button(
                onClick = { onSubmit(question, selectedReason, noteText.trim()) },
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("flag_submit_btn")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = editorial.onAcidLime
                    )
                } else {
                    Text("Submit Report to Faculty", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
