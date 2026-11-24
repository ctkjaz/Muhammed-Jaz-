package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.notifications.AnnouncementCategory
import com.example.data.notifications.AnnouncementPriority
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDoubt
import com.example.data.notifications.DoubtStatus
import com.example.data.notifications.EngagementLogic
import com.example.data.subscription.AdminUserDirectoryRow
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
internal fun AdminAlertsTabContent(
    announcements: List<CloudAnnouncement>,
    users: List<AdminUserDirectoryRow>,
    preselectedRecipient: AdminUserDirectoryRow?,
    isMutating: Boolean,
    onConsumePreselectedRecipient: () -> Unit,
    onPublishAnnouncement: (
        title: String,
        body: String,
        category: AnnouncementCategory,
        targetExamId: String,
        priority: AnnouncementPriority,
        actionUrl: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSendDirectNotification: (
        targetUserId: String,
        recipientLabel: String,
        title: String,
        body: String,
        actionUrl: String,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    // 0 = Broadcast to All/Exam, 1 = Individual Student Push
    var modeIndex by remember { mutableStateOf(0) }

    // Broadcast form state
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    var broadcastCategory by remember { mutableStateOf(AnnouncementCategory.EXAM_ALERT) }
    var broadcastTargetExam by remember { mutableStateOf("all") }
    var broadcastPriority by remember { mutableStateOf(AnnouncementPriority.NORMAL) }
    var broadcastUrl by remember { mutableStateOf("") }
    var broadcastExamSearch by remember { mutableStateOf("") }
    var broadcastError by remember { mutableStateOf<String?>(null) }

    // Individual push form state
    var userSearch by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf<AdminUserDirectoryRow?>(null) }
    var directTitle by remember { mutableStateOf("") }
    var directBody by remember { mutableStateOf("") }
    var directUrl by remember { mutableStateOf("") }
    var directError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preselectedRecipient) {
        if (preselectedRecipient != null) {
            selectedRecipient = preselectedRecipient
            modeIndex = 1
            onConsumePreselectedRecipient()
        }
    }

    val matchingExams = remember(broadcastExamSearch) {
        val q = broadcastExamSearch.trim().lowercase(Locale.US)
        if (q.isBlank()) PlatformCatalogDefaults.DEFAULT_EXAMS
        else PlatformCatalogDefaults.DEFAULT_EXAMS.filter {
            it.shortName.lowercase(Locale.US).contains(q) ||
                it.name.lowercase(Locale.US).contains(q) ||
                it.id.lowercase(Locale.US).contains(q)
        }
    }

    val matchingUsers = remember(users, userSearch) {
        val q = userSearch.trim().lowercase(Locale.US)
        if (q.isBlank()) users.take(25)
        else users.filter {
            it.displayName.lowercase(Locale.US).contains(q) ||
                it.email.lowercase(Locale.US).contains(q) ||
                it.uid.lowercase(Locale.US).contains(q)
        }.take(25)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "alerts_mode_switch") {
            SegmentedPillBar(
                options = listOf(0, 1),
                selected = modeIndex,
                labelOf = { idx -> if (idx == 0) "Broadcast Alert" else "Individual Student" },
                onSelect = { modeIndex = it },
                testTagOf = { idx -> "admin_alert_mode_$idx" }
            )
        }

        if (modeIndex == 0) {
            item(key = "broadcast_composer") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Push Live News / Exam Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Pushes immediately to students' Android notification tray and in-app Notification Center (100% free Firestore sync).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = {
                                broadcastTitle = it.take(EngagementLogic.MAX_TITLE_LENGTH)
                                broadcastError = null
                            },
                            label = { Text("Headline / Alert Title") },
                            placeholder = { Text("e.g., AIIMS NORCET-10 Registration Dates Out!") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_broadcast_title_input")
                        )

                        OutlinedTextField(
                            value = broadcastBody,
                            onValueChange = {
                                broadcastBody = it.take(EngagementLogic.MAX_BODY_LENGTH)
                                broadcastError = null
                            },
                            label = { Text("Announcement Message") },
                            placeholder = { Text("Share exam dates, vacancy count, syllabus updates, or study tips...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_broadcast_body_input")
                        )

                        Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnnouncementCategory.entries.forEach { cat ->
                                MonochromeFilterPill(
                                    label = cat.label,
                                    selected = broadcastCategory == cat,
                                    onClick = { broadcastCategory = cat }
                                )
                            }
                        }

                        Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnnouncementPriority.entries.forEach { prio ->
                                MonochromeFilterPill(
                                    label = if (prio == AnnouncementPriority.URGENT) "Urgent (+ Top Banner)" else "Normal",
                                    selected = broadcastPriority == prio,
                                    onClick = { broadcastPriority = prio }
                                )
                            }
                        }

                        Text("Target Audience", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = broadcastExamSearch,
                            onValueChange = { broadcastExamSearch = it },
                            label = { Text("Filter 29 exams (or pick All Exams)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MonochromeFilterPill(
                                label = "All 29 Exams",
                                selected = broadcastTargetExam == "all",
                                onClick = { broadcastTargetExam = "all" }
                            )
                            matchingExams.forEach { exam ->
                                MonochromeFilterPill(
                                    label = exam.shortName,
                                    selected = broadcastTargetExam == exam.id,
                                    onClick = { broadcastTargetExam = exam.id }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = broadcastUrl,
                            onValueChange = {
                                broadcastUrl = it.take(EngagementLogic.MAX_URL_LENGTH)
                                broadcastError = null
                            },
                            label = { Text("Official Link URL (optional, https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (broadcastError != null) {
                            Text(
                                text = broadcastError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.wrongText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val err = EngagementLogic.validateNotificationFields(
                                    broadcastTitle,
                                    broadcastBody,
                                    broadcastUrl
                                )
                                if (err != null) {
                                    broadcastError = err
                                } else {
                                    onPublishAnnouncement(
                                        broadcastTitle.trim(),
                                        broadcastBody.trim(),
                                        broadcastCategory,
                                        broadcastTargetExam,
                                        broadcastPriority,
                                        broadcastUrl.trim()
                                    ) {
                                        broadcastTitle = ""
                                        broadcastBody = ""
                                        broadcastUrl = ""
                                        broadcastPriority = AnnouncementPriority.NORMAL
                                    }
                                }
                            },
                            enabled = !isMutating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_push_broadcast_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Push Broadcast Live", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item(key = "recent_broadcasts_header") {
                Text(
                    text = "PUBLISHED ANNOUNCEMENTS (${announcements.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            items(items = announcements, key = { it.announcementId }) { ann ->
                val targetLabel = if (ann.targetExamId == "all") "All Exams"
                else PlatformCatalogDefaults.examById(ann.targetExamId)?.shortName ?: ann.targetExamId
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${ann.categoryEnum.label} • $targetLabel • ${ann.priorityEnum.label} • ${formatEngagementTimestamp(ann.createdAtMs)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = if (ann.priorityEnum == AnnouncementPriority.URGENT) editorial.wrongText
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ann.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ann.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeleteAnnouncement(ann.announcementId) },
                            enabled = !isMutating
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete announcement",
                                tint = editorial.wrongText
                            )
                        }
                    }
                }
            }
        } else {
            // Mode 1: Individual / Custom Student Notification
            item(key = "direct_push_composer") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Send Individual / Custom Notification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Select any student from your directory to send a private push notification straight to their device & Personal Inbox.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selectedRecipient != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = editorial.elevatedSurface,
                                border = BorderStroke(1.dp, AcidLime),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "RECIPIENT SELECTED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = if (editorial.isDark) AcidLime else editorial.correctText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = selectedRecipient!!.displayName.ifBlank { "Unnamed Student" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = selectedRecipient!!.email.ifBlank { selectedRecipient!!.uid },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { selectedRecipient = null }) {
                                        Text("Change")
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = userSearch,
                                onValueChange = { userSearch = it },
                                label = { Text("Search student by name, email, or UID (${users.size})") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_direct_user_search")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                matchingUsers.take(6).forEach { row ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = editorial.elevatedSurface,
                                        border = BorderStroke(1.dp, editorial.borderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedRecipient = row }
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Text(
                                                text = row.displayName.ifBlank { "Unnamed Student" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = row.email.ifBlank { row.uid },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (matchingUsers.isEmpty()) {
                                    Text(
                                        text = "No matching students found. Tap Refresh in the header to reload directory.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = directTitle,
                            onValueChange = {
                                directTitle = it.take(EngagementLogic.MAX_TITLE_LENGTH)
                                directError = null
                            },
                            label = { Text("Notification Title") },
                            placeholder = { Text("e.g., Your NORCET Study Plan Update") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_direct_title_input")
                        )

                        OutlinedTextField(
                            value = directBody,
                            onValueChange = {
                                directBody = it.take(EngagementLogic.MAX_BODY_LENGTH)
                                directError = null
                            },
                            label = { Text("Custom Message") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_direct_body_input")
                        )

                        OutlinedTextField(
                            value = directUrl,
                            onValueChange = {
                                directUrl = it.take(EngagementLogic.MAX_URL_LENGTH)
                                directError = null
                            },
                            label = { Text("Optional Link (https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (directError != null) {
                            Text(
                                text = directError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.wrongText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val rec = selectedRecipient
                                if (rec == null) {
                                    directError = "Select a recipient student first."
                                    return@Button
                                }
                                val err = EngagementLogic.validateNotificationFields(
                                    directTitle,
                                    directBody,
                                    directUrl
                                )
                                if (err != null) {
                                    directError = err
                                } else {
                                    onSendDirectNotification(
                                        rec.uid,
                                        rec.displayName.ifBlank { rec.email.ifBlank { rec.uid } },
                                        directTitle.trim(),
                                        directBody.trim(),
                                        directUrl.trim()
                                    ) {
                                        directTitle = ""
                                        directBody = ""
                                        directUrl = ""
                                    }
                                }
                            },
                            enabled = !isMutating && selectedRecipient != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_send_direct_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Send Personal Notification", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminDoubtsTabContent(
    doubts: List<CloudDoubt>,
    isMutating: Boolean,
    onReplyToDoubt: (CloudDoubt, String, () -> Unit) -> Unit,
    onCloseDoubt: (String) -> Unit,
    onDeleteDoubt: (String) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var statusFilter by remember { mutableStateOf<DoubtStatus?>(DoubtStatus.OPEN) }

    val filtered = remember(doubts, statusFilter) {
        if (statusFilter == null) doubts else doubts.filter { it.statusEnum == statusFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "admin_doubts_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonochromeFilterPill(
                    label = "Open (${doubts.count { it.statusEnum == DoubtStatus.OPEN }})",
                    selected = statusFilter == DoubtStatus.OPEN,
                    onClick = { statusFilter = DoubtStatus.OPEN }
                )
                MonochromeFilterPill(
                    label = "Answered (${doubts.count { it.statusEnum == DoubtStatus.ANSWERED }})",
                    selected = statusFilter == DoubtStatus.ANSWERED,
                    onClick = { statusFilter = DoubtStatus.ANSWERED }
                )
                MonochromeFilterPill(
                    label = "Closed (${doubts.count { it.statusEnum == DoubtStatus.CLOSED }})",
                    selected = statusFilter == DoubtStatus.CLOSED,
                    onClick = { statusFilter = DoubtStatus.CLOSED }
                )
                MonochromeFilterPill(
                    label = "All (${doubts.size})",
                    selected = statusFilter == null,
                    onClick = { statusFilter = null }
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "admin_doubts_empty") {
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
                        Text(
                            text = "No student doubts in this queue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "New doubts submitted from the Doubt Box or MCQ cards appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = filtered, key = { it.doubtId }) { doubt ->
                AdminDoubtCard(
                    doubt = doubt,
                    isMutating = isMutating,
                    onReply = onReplyToDoubt,
                    onClose = { onCloseDoubt(doubt.doubtId) },
                    onDelete = { onDeleteDoubt(doubt.doubtId) }
                )
            }
        }
    }
}

@Composable
private fun AdminDoubtCard(
    doubt: CloudDoubt,
    isMutating: Boolean,
    onReply: (CloudDoubt, String, () -> Unit) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val examShort = PlatformCatalogDefaults.examById(doubt.examId)?.shortName ?: doubt.examId
    var isReplying by remember(doubt.doubtId) { mutableStateOf(doubt.statusEnum == DoubtStatus.OPEN) }
    var replyDraft by remember(doubt.doubtId, doubt.adminReply) { mutableStateOf(doubt.adminReply) }
    var replyError by remember(doubt.doubtId) { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            width = if (doubt.statusEnum == DoubtStatus.OPEN) 1.5.dp else 1.dp,
            color = if (doubt.statusEnum == DoubtStatus.OPEN) AcidLime else editorial.borderSubtle
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
                        text = doubt.studentName.ifBlank { "Student" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = doubt.studentEmail.ifBlank { doubt.userId },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$examShort • ${doubt.subject} • ${formatEngagementTimestamp(doubt.createdAtMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (doubt.statusEnum) {
                        DoubtStatus.OPEN -> AcidLime
                        DoubtStatus.ANSWERED -> editorial.correctBg
                        DoubtStatus.CLOSED -> editorial.elevatedSurface
                    }
                ) {
                    Text(
                        text = doubt.statusEnum.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = when (doubt.statusEnum) {
                            DoubtStatus.OPEN -> PureBlack
                            DoubtStatus.ANSWERED -> editorial.correctText
                            DoubtStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (doubt.questionPreview.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ATTACHED MCQ (${doubt.questionId})",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = doubt.questionPreview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = doubt.doubtText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (doubt.adminReply.isNotBlank() && !isReplying) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = editorial.explanationSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "YOUR REPLY · ${formatEngagementTimestamp(doubt.repliedAtMs)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = editorial.correctText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = doubt.adminReply,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (isReplying) {
                HorizontalDivider(color = editorial.borderSubtle)
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = {
                        replyDraft = it.take(EngagementLogic.MAX_REPLY_LENGTH)
                        replyError = null
                    },
                    label = { Text("Faculty Reply (also sends personal notification)") },
                    minLines = 3,
                    maxLines = 6,
                    isError = replyError != null,
                    supportingText = replyError?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_doubt_reply_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val err = EngagementLogic.validateAdminReply(replyDraft)
                            if (err != null) {
                                replyError = err
                            } else {
                                onReply(doubt, replyDraft.trim()) {
                                    isReplying = false
                                }
                            }
                        },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = editorial.acidLime,
                            contentColor = editorial.onAcidLime
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("admin_send_doubt_reply_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Send Reply & Notify", fontWeight = FontWeight.Bold)
                    }
                    if (doubt.adminReply.isNotBlank()) {
                        OutlinedButton(
                            onClick = { isReplying = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { isReplying = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(if (doubt.adminReply.isBlank()) "Write Reply" else "Edit Reply", fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (doubt.statusEnum != DoubtStatus.CLOSED) {
                            TextButton(onClick = onClose, enabled = !isMutating) {
                                Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Close")
                            }
                        }
                        IconButton(onClick = onDelete, enabled = !isMutating) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete doubt",
                                tint = editorial.wrongText
                            )
                        }
                    }
                }
            }
        }
    }
}
