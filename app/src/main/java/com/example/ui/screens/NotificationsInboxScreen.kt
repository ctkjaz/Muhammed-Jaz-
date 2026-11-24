package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.notifications.AnnouncementPriority
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.CloudDirectNotification
import com.example.data.notifications.DirectNotificationType
import com.example.data.notifications.EngagementLogic
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.SegmentedPillBar
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxSection(val label: String) {
    NEWS_ALERTS("Exam News & Alerts"),
    PERSONAL("Personal & Replies")
}

@Composable
fun NotificationsInboxScreen(
    announcements: List<CloudAnnouncement>,
    directNotifications: List<CloudDirectNotification>,
    activeExamId: String,
    activeExamShortName: String,
    lastSeenAnnouncementMs: Long,
    onMarkAllRead: () -> Unit,
    onMarkDirectRead: (String) -> Unit,
    onDeleteDirectNotification: (String) -> Unit,
    onOpenDoubtFromReply: (String?) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(InboxSection.NEWS_ALERTS) }
    var onlyActiveExam by remember { mutableStateOf(true) }

    val filteredAnnouncements = remember(announcements, activeExamId, onlyActiveExam) {
        if (onlyActiveExam) {
            announcements.filter { EngagementLogic.announcementMatchesExam(it, activeExamId) }
        } else {
            announcements
        }
    }
    val unreadDirectCount = remember(directNotifications) { directNotifications.count { !it.read } }

    fun openSafeLink(rawUrl: String) {
        val safe = EngagementLogic.sanitizeSafeHttpUrl(rawUrl) ?: return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safe)))
        }.onFailure {
            onShowMessage("Could not open link in browser.")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Bar
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
                        modifier = Modifier.testTag("notifications_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column {
                        Text(
                            text = "Notification Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exam alerts, vacancies & personal faculty notices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("notifications_mark_all_read_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Mark read", fontWeight = FontWeight.Bold)
                }
            }

            // Section Toggle
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                val personalLabel = if (unreadDirectCount > 0) {
                    "Personal ($unreadDirectCount)"
                } else {
                    InboxSection.PERSONAL.label
                }
                SegmentedPillBar(
                    options = InboxSection.entries,
                    selected = selectedSection,
                    labelOf = { sec ->
                        when (sec) {
                            InboxSection.NEWS_ALERTS -> sec.label
                            InboxSection.PERSONAL -> personalLabel
                        }
                    },
                    onSelect = { selectedSection = it },
                    testTagOf = { sec -> "inbox_tab_${sec.name.lowercase()}" }
                )
            }

            if (selectedSection == InboxSection.NEWS_ALERTS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonochromeFilterPill(
                        label = "$activeExamShortName + All",
                        selected = onlyActiveExam,
                        onClick = { onlyActiveExam = true },
                        modifier = Modifier.testTag("inbox_filter_active_exam")
                    )
                    MonochromeFilterPill(
                        label = "All 29 Exams (${announcements.size})",
                        selected = !onlyActiveExam,
                        onClick = { onlyActiveExam = false },
                        modifier = Modifier.testTag("inbox_filter_all_exams")
                    )
                }
            }

            HorizontalDivider(color = editorial.borderSubtle, modifier = Modifier.padding(top = 4.dp))

            when (selectedSection) {
                InboxSection.NEWS_ALERTS -> {
                    if (filteredAnnouncements.isEmpty()) {
                        InboxEmptyState(
                            icon = Icons.Filled.Campaign,
                            title = "No exam announcements yet",
                            subtitle = "New vacancies, exam dates, syllabus news, and admin updates will appear here automatically."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredAnnouncements,
                                key = { it.announcementId }
                            ) { ann ->
                                val isUrgent = ann.priorityEnum == AnnouncementPriority.URGENT
                                val isFresh = ann.createdAtMs > lastSeenAnnouncementMs
                                val targetLabel = if (ann.targetExamId == "all" || ann.targetExamId.isBlank()) {
                                    "All Exams"
                                } else {
                                    PlatformCatalogDefaults.examById(ann.targetExamId)?.shortName ?: ann.targetExamId
                                }

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                                    border = BorderStroke(
                                        width = if (isUrgent) 1.5.dp else 1.dp,
                                        color = if (isUrgent) editorial.wrongText.copy(alpha = 0.55f) else editorial.borderSubtle
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
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
                                                if (isUrgent) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = editorial.wrongBg
                                                    ) {
                                                        Text(
                                                            text = "URGENT",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                            color = editorial.wrongText,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = editorial.elevatedSurface
                                                ) {
                                                    Text(
                                                        text = ann.categoryEnum.label.uppercase(Locale.US),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = editorial.elevatedSurface
                                                ) {
                                                    Text(
                                                        text = targetLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isFresh) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(AcidLime)
                                                    )
                                                }
                                                Text(
                                                    text = formatEngagementTimestamp(ann.createdAtMs),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = ann.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = ann.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val safeLink = EngagementLogic.sanitizeSafeHttpUrl(ann.actionUrl)
                                        if (safeLink != null) {
                                            OutlinedButton(
                                                onClick = { openSafeLink(safeLink) },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.size(6.dp))
                                                Text("Open Official Link", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                InboxSection.PERSONAL -> {
                    if (directNotifications.isEmpty()) {
                        InboxEmptyState(
                            icon = Icons.Filled.NotificationsNone,
                            title = "No personal notifications yet",
                            subtitle = "Replies to your Doubt Box questions and direct messages from ClinEdge Faculty will appear here."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = directNotifications,
                                key = { it.notificationId }
                            ) { item ->
                                val isUnread = !item.read
                                val isDoubtReply = item.typeEnum == DirectNotificationType.DOUBT_REPLY
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                                    border = BorderStroke(
                                        width = if (isUnread) 1.5.dp else 1.dp,
                                        color = if (isUnread) AcidLime else editorial.borderSubtle
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            if (isUnread) onMarkDirectRead(item.notificationId)
                                            if (isDoubtReply) {
                                                onOpenDoubtFromReply(item.referenceId.takeIf { it.isNotBlank() })
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isUnread) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = AcidLime
                                                    ) {
                                                        Text(
                                                            text = "NEW",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                            color = PureBlack,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = editorial.elevatedSurface
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isDoubtReply) Icons.Filled.QuestionAnswer else Icons.Filled.MarkEmailUnread,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = item.typeEnum.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = formatEngagementTimestamp(item.createdAtMs),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = { onDeleteDirectNotification(item.notificationId) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "Delete notification",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isDoubtReply) {
                                                OutlinedButton(
                                                    onClick = {
                                                        if (isUnread) onMarkDirectRead(item.notificationId)
                                                        onOpenDoubtFromReply(item.referenceId.takeIf { it.isNotBlank() })
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, editorial.borderSubtle),
                                                    modifier = Modifier.heightIn(min = 44.dp)
                                                ) {
                                                    Text("View in Doubt Box", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            val safeLink = EngagementLogic.sanitizeSafeHttpUrl(item.actionUrl)
                                            if (safeLink != null) {
                                                OutlinedButton(
                                                    onClick = {
                                                        if (isUnread) onMarkDirectRead(item.notificationId)
                                                        openSafeLink(safeLink)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, editorial.borderSubtle),
                                                    modifier = Modifier.heightIn(min = 44.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(Modifier.size(5.dp))
                                                    Text("Open Link", fontWeight = FontWeight.SemiBold)
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
    }
}

@Composable
private fun InboxEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val editorial = MaterialTheme.editorialColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = editorial.elevatedSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

internal fun formatEngagementTimestamp(timeMs: Long): String {
    if (timeMs <= 0L) return "Just now"
    val diffMs = (System.currentTimeMillis() - timeMs).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    return when {
        mins < 1L -> "Just now"
        mins < 60L -> "${mins}m ago"
        hours < 24L -> "${hours}h ago"
        days < 7L -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(timeMs))
    }
}
