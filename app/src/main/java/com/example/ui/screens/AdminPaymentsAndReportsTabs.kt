package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionDifficulty
import com.example.data.subscription.CloudPaymentRequest
import com.example.data.subscription.CloudQuestionReport
import com.example.data.subscription.OtaQuestionEditDraft
import com.example.data.subscription.PaymentRequestStatus
import com.example.data.subscription.QuestionReportStatus
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
internal fun AdminPaymentsTabContent(
    requests: List<CloudPaymentRequest>,
    isMutating: Boolean,
    onApprove: (CloudPaymentRequest) -> Unit,
    onReject: (CloudPaymentRequest, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf<PaymentRequestStatus?>(PaymentRequestStatus.PENDING) }
    var rejectingRequest by remember { mutableStateOf<CloudPaymentRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val filtered = remember(requests, statusFilter) {
        if (statusFilter == null) requests else requests.filter { it.parsedStatus == statusFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "payments_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonochromeFilterPill(
                    label = "Pending (${requests.count { it.parsedStatus == PaymentRequestStatus.PENDING }})",
                    selected = statusFilter == PaymentRequestStatus.PENDING,
                    onClick = { statusFilter = PaymentRequestStatus.PENDING }
                )
                MonochromeFilterPill(
                    label = "Approved (${requests.count { it.parsedStatus == PaymentRequestStatus.APPROVED }})",
                    selected = statusFilter == PaymentRequestStatus.APPROVED,
                    onClick = { statusFilter = PaymentRequestStatus.APPROVED }
                )
                MonochromeFilterPill(
                    label = "Rejected (${requests.count { it.parsedStatus == PaymentRequestStatus.REJECTED }})",
                    selected = statusFilter == PaymentRequestStatus.REJECTED,
                    onClick = { statusFilter = PaymentRequestStatus.REJECTED }
                )
                MonochromeFilterPill(
                    label = "All (${requests.size})",
                    selected = statusFilter == null,
                    onClick = { statusFilter = null }
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "payments_empty") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No UPI payment requests here",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Students submitting UPI UTR numbers from Plans & Access appear here in real time for 1-tap approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = filtered, key = { it.requestId.ifBlank { it.id } }) { req ->
                val isPending = req.parsedStatus == PaymentRequestStatus.PENDING
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(
                        width = if (isPending) 1.5.dp else 1.dp,
                        color = if (isPending) AcidLime else editorial.borderSubtle
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = req.studentName.ifBlank { "Nursing Aspirant" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = req.studentEmail.ifBlank { req.userId },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (req.parsedStatus) {
                                    PaymentRequestStatus.PENDING -> AcidLime
                                    PaymentRequestStatus.APPROVED -> editorial.correctBg
                                    PaymentRequestStatus.REJECTED -> editorial.wrongBg
                                }
                            ) {
                                Text(
                                    text = req.parsedStatus.label.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = when (req.parsedStatus) {
                                        PaymentRequestStatus.PENDING -> PureBlack
                                        PaymentRequestStatus.APPROVED -> editorial.correctText
                                        PaymentRequestStatus.REJECTED -> editorial.wrongText
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
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
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = req.planSummaryLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹${req.amountInr}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = if (editorial.isDark) AcidLime else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "UTR: ${req.utrNumber}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (req.payerName.isNotBlank()) {
                                            Text(
                                                text = "Payer: ${req.payerName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("UPI UTR", req.utrNumber))
                                            onShowMessage("Copied UTR ${req.utrNumber}")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy UTR",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Submitted ${formatEngagementTimestamp(req.createdAt?.toDate()?.time ?: 0L)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (req.adminNote.isNotBlank()) {
                            Text(
                                text = "Admin note: ${req.adminNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isPending) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(req) },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = editorial.acidLime,
                                        contentColor = editorial.onAcidLime
                                    ),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_approve_pay_btn")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Approve & Activate", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        rejectReason = ""
                                        rejectingRequest = req
                                    },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_reject_pay_btn")
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = editorial.wrongText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text("Reject", color = editorial.wrongText, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejectingRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingRequest = null },
            title = { Text("Reject UPI Payment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "UTR ${req.utrNumber} (${req.studentName} · ₹${req.amountInr}). The student will receive a notification with your reason.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it.take(280) },
                        label = { Text("Reason for rejection") },
                        placeholder = { Text("e.g., UTR not found in bank statement") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(req, rejectReason.trim())
                        rejectingRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.wrongText,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reject & Notify", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRequest = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
internal fun AdminReportsTabContent(
    reports: List<CloudQuestionReport>,
    isMutating: Boolean,
    otaEditDraft: OtaQuestionEditDraft?,
    onResolveReport: (CloudQuestionReport, QuestionReportStatus, String, Boolean) -> Unit,
    onOpenOtaEditor: (questionId: String, reportId: String) -> Unit,
    onSaveOtaEdit: (OtaQuestionEditDraft) -> Unit,
    onDismissOtaEditor: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var statusFilter by remember { mutableStateOf<QuestionReportStatus?>(QuestionReportStatus.OPEN) }
    var resolvingPair by remember { mutableStateOf<Pair<CloudQuestionReport, QuestionReportStatus>?>(null) }
    var resolutionNote by remember { mutableStateOf("") }
    var notifyStudent by remember { mutableStateOf(true) }

    val filtered = remember(reports, statusFilter) {
        if (statusFilter == null) reports else reports.filter { it.parsedStatus == statusFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "reports_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonochromeFilterPill(
                    label = "Open (${reports.count { it.parsedStatus == QuestionReportStatus.OPEN }})",
                    selected = statusFilter == QuestionReportStatus.OPEN,
                    onClick = { statusFilter = QuestionReportStatus.OPEN }
                )
                MonochromeFilterPill(
                    label = "Resolved (${reports.count { it.parsedStatus == QuestionReportStatus.RESOLVED }})",
                    selected = statusFilter == QuestionReportStatus.RESOLVED,
                    onClick = { statusFilter = QuestionReportStatus.RESOLVED }
                )
                MonochromeFilterPill(
                    label = "Dismissed (${reports.count { it.parsedStatus == QuestionReportStatus.DISMISSED }})",
                    selected = statusFilter == QuestionReportStatus.DISMISSED,
                    onClick = { statusFilter = QuestionReportStatus.DISMISSED }
                )
                MonochromeFilterPill(
                    label = "All (${reports.size})",
                    selected = statusFilter == null,
                    onClick = { statusFilter = null }
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "reports_empty") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No flagged questions in this queue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Student MCQ error reports (wrong answer keys, typos, outdated guidelines) appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = filtered, key = { it.reportId.ifBlank { it.id } }) { report ->
                val isOpen = report.parsedStatus == QuestionReportStatus.OPEN
                val isOtaQuestion = report.questionId.startsWith("OTA_")
                val examShort = PlatformCatalogDefaults.examById(report.examId)?.shortName ?: report.examId

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(
                        width = if (isOpen) 1.5.dp else 1.dp,
                        color = if (isOpen) editorial.wrongText.copy(alpha = 0.6f) else editorial.borderSubtle
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = report.parsedReason.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOpen) editorial.wrongText else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$examShort • ${report.subject} • ${report.questionId}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "By ${report.studentName.ifBlank { "Student" }} • ${formatEngagementTimestamp(report.createdAt?.toDate()?.time ?: 0L)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (report.parsedStatus) {
                                    QuestionReportStatus.OPEN -> editorial.wrongBg
                                    QuestionReportStatus.RESOLVED -> editorial.correctBg
                                    QuestionReportStatus.DISMISSED -> editorial.elevatedSurface
                                }
                            ) {
                                Text(
                                    text = report.parsedStatus.label.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = when (report.parsedStatus) {
                                        QuestionReportStatus.OPEN -> editorial.wrongText
                                        QuestionReportStatus.RESOLVED -> editorial.correctText
                                        QuestionReportStatus.DISMISSED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = report.questionPreview.ifBlank { "Question ID: ${report.questionId}" },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (report.note.isNotBlank()) {
                            Text(
                                text = "Student note: ${report.note}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (report.adminResolutionNote.isNotBlank()) {
                            Text(
                                text = "Resolution: ${report.adminResolutionNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.correctText
                            )
                        }

                        if (isOtaQuestion) {
                            OutlinedButton(
                                onClick = {
                                    onOpenOtaEditor(report.questionId, report.reportId.ifBlank { report.id })
                                },
                                enabled = !isMutating,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AcidLime),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("admin_edit_ota_q_btn")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Edit Live OTA Question", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isOpen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        resolutionNote = ""
                                        notifyStudent = true
                                        resolvingPair = report to QuestionReportStatus.RESOLVED
                                    },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = editorial.acidLime,
                                        contentColor = editorial.onAcidLime
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("admin_resolve_report_btn")
                                ) {
                                    Text("Mark Resolved", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        resolutionNote = ""
                                        notifyStudent = false
                                        resolvingPair = report to QuestionReportStatus.DISMISSED
                                    },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    resolvingPair?.let { (report, targetStatus) ->
        AlertDialog(
            onDismissRequest = { resolvingPair = null },
            title = {
                Text(
                    if (targetStatus == QuestionReportStatus.RESOLVED) "Resolve Question Report"
                    else "Dismiss Question Report"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${report.subject} • ${report.questionId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily)
                    )
                    OutlinedTextField(
                        value = resolutionNote,
                        onValueChange = { resolutionNote = it.take(450) },
                        label = { Text("Resolution note (optional)") },
                        placeholder = { Text("e.g., Updated answer key to Option B in OTA v4.") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send thank-you alert to student", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = notifyStudent, onCheckedChange = { notifyStudent = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveReport(report, targetStatus, resolutionNote.trim(), notifyStudent)
                        resolvingPair = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingPair = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    otaEditDraft?.let { initialDraft ->
        AdminOtaQuestionEditorDialog(
            initialDraft = initialDraft,
            isSaving = isMutating,
            onSave = onSaveOtaEdit,
            onDismiss = onDismissOtaEditor
        )
    }
}

@Composable
private fun AdminOtaQuestionEditorDialog(
    initialDraft: OtaQuestionEditDraft,
    isSaving: Boolean,
    onSave: (OtaQuestionEditDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var questionText by remember(initialDraft) { mutableStateOf(initialDraft.questionText) }
    var options by remember(initialDraft) {
        mutableStateOf(
            initialDraft.options.ifEmpty { listOf("", "", "", "") }
        )
    }
    var correctIndex by remember(initialDraft) { mutableStateOf(initialDraft.correctIndex) }
    var difficulty by remember(initialDraft) { mutableStateOf(initialDraft.difficulty) }
    var explanation by remember(initialDraft) { mutableStateOf(initialDraft.explanation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Edit Live OTA Question", fontWeight = FontWeight.Bold)
                Text(
                    text = "${initialDraft.packId}/${initialDraft.docId}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it.take(1900) },
                    label = { Text("Question Stem") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Options", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                options.forEachIndexed { idx, optVal ->
                    OutlinedTextField(
                        value = optVal,
                        onValueChange = { next ->
                            options = options.toMutableList().also { it[idx] = next.take(400) }
                        },
                        label = { Text("Option ${'A' + idx}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Correct Answer Key", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.indices.forEach { idx ->
                        MonochromeFilterPill(
                            label = "Option ${'A' + idx}",
                            selected = correctIndex == idx,
                            onClick = { correctIndex = idx }
                        )
                    }
                }

                Text("Difficulty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestionDifficulty.entries.forEach { diff ->
                        MonochromeFilterPill(
                            label = diff.label,
                            selected = difficulty == diff,
                            onClick = { difficulty = diff }
                        )
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it.take(3800) },
                    label = { Text("Clinical Rationale / Explanation") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = editorial.borderSubtle)
                Text(
                    text = "Saving bumps catalogVersion so all students automatically sync this fix.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialDraft.copy(
                            questionText = questionText,
                            options = options,
                            correctIndex = correctIndex,
                            difficulty = difficulty,
                            explanation = explanation
                        )
                    )
                },
                enabled = !isSaving && questionText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                )
            ) {
                Text(if (isSaving) "Saving..." else "Save & Publish Fix", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
