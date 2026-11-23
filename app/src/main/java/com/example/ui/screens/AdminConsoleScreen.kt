package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.model.SubjectCatalog
import com.example.data.subscription.AdminUserDirectoryRow
import com.example.data.subscription.CloudAccessCode
import com.example.data.subscription.CloudDeviceTrialLock
import com.example.data.subscription.CloudQuestionPack
import com.example.data.subscription.CloudSubscriptionConfig
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PlanTenure
import com.example.data.subscription.PlanTier
import com.example.data.subscription.SubscriptionStatus
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.AdminConsoleTab
import com.example.ui.viewmodel.AdminConsoleUiState
import com.example.ui.viewmodel.AdminContentDraftState

@Composable
fun AdminConsoleScreen(
    state: AdminConsoleUiState,
    config: CloudSubscriptionConfig,
    onSelectTab: (AdminConsoleTab) -> Unit,
    onRefresh: () -> Unit,
    onUpdateUserPlan: (
        uid: String,
        email: String,
        name: String,
        tier: PlanTier,
        examIds: List<String>,
        status: SubscriptionStatus,
        tenureDays: Int?,
        extend: Boolean
    ) -> Unit,
    onGenerateCode: (PlanTier, String?, PlanTenure, String) -> Unit,
    onRevokeCode: (String) -> Unit,
    onResetDeviceLock: (deviceHash: String, shortCode: String) -> Unit,
    onSaveConfig: (CloudSubscriptionConfig) -> Unit,
    onPickContentFile: () -> Unit,
    onUpdateContentDraft: ((AdminContentDraftState) -> AdminContentDraftState) -> Unit,
    onPreviewContentPack: () -> Unit,
    onRemovePreviewQuestion: (Int) -> Unit,
    onPublishContentPack: () -> Unit,
    onDeleteQuestionPack: (packId: String, title: String) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    var editingUser by remember { mutableStateOf<AdminUserDirectoryRow?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Owner Admin",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = AcidLime) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = PureBlack,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Users • Access Codes • Device Locks • OTA PDF Push",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh admin data")
                    }
                }
            }

            // Tab Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminConsoleTab.entries.forEach { tab ->
                    MonochromeFilterPill(
                        label = tab.label,
                        selected = state.selectedTab == tab,
                        onClick = { onSelectTab(tab) }
                    )
                }
            }

            HorizontalDivider(color = editorial.borderSubtle, modifier = Modifier.padding(top = 6.dp))

            when (state.selectedTab) {
                AdminConsoleTab.USERS -> AdminUsersTab(
                    users = state.users,
                    onManageUser = { editingUser = it }
                )
                AdminConsoleTab.CODES -> AdminCodesTab(
                    codes = state.accessCodes,
                    lastGenerated = state.lastGeneratedCode,
                    isMutating = state.isMutating,
                    onGenerate = onGenerateCode,
                    onRevoke = onRevokeCode,
                    onShowMessage = onShowMessage
                )
                AdminConsoleTab.LOCKS -> AdminDeviceLocksTab(
                    locks = state.deviceLocks,
                    isMutating = state.isMutating,
                    onResetLock = onResetDeviceLock
                )
                AdminConsoleTab.CONTENT -> AdminContentPushTab(
                    draft = state.contentDraft,
                    packs = state.questionPacks,
                    onPickFile = onPickContentFile,
                    onUpdateDraft = onUpdateContentDraft,
                    onPreview = onPreviewContentPack,
                    onRemovePreview = onRemovePreviewQuestion,
                    onPublish = onPublishContentPack,
                    onDeletePack = onDeleteQuestionPack
                )
                AdminConsoleTab.PRICING -> AdminPricingSupportTab(
                    config = config,
                    isSaving = state.isMutating,
                    onSave = onSaveConfig
                )
            }
        }
    }

    editingUser?.let { target ->
        AdminEditUserPlanDialog(
            user = target,
            onDismiss = { editingUser = null },
            onApply = { tier, exams, status, tenureDays, extend ->
                onUpdateUserPlan(
                    target.uid,
                    target.email,
                    target.displayName,
                    tier,
                    exams,
                    status,
                    tenureDays,
                    extend
                )
                editingUser = null
            }
        )
    }
}

@Composable
private fun AdminUsersTab(
    users: List<AdminUserDirectoryRow>,
    onManageUser: (AdminUserDirectoryRow) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var query by remember { mutableStateOf("") }
    val filtered = remember(users, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) users else users.filter {
            it.displayName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.uid.lowercase().contains(q) ||
                it.phoneNumber.lowercase().contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "users_search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search name, email, phone or UID (${users.size})") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search")
            )
        }
        items(items = filtered, key = { it.uid }) { row ->
            val ent = row.entitlement
            val tier = PlanTier.fromDb(ent?.planTier)
            val status = SubscriptionStatus.fromDb(ent?.status)
            val expMs = ent?.expiresAt?.toDate()?.time
            val expLabel = expMs?.let { "Ends ${EntitlementEvaluator.formatShortDate(it)}" } ?: "No expiry"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.displayName.ifBlank { "Unnamed Student" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = row.email.ifBlank { row.uid },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                status == SubscriptionStatus.SUSPENDED -> editorial.wrongBg
                                tier == PlanTier.FREE -> editorial.elevatedSurface
                                else -> AcidLime
                            }
                        ) {
                            Text(
                                text = if (status == SubscriptionStatus.SUSPENDED) "SUSPENDED" else tier.dbValue.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = when {
                                    status == SubscriptionStatus.SUSPENDED -> editorial.wrongText
                                    tier == PlanTier.FREE -> MaterialTheme.colorScheme.onSurface
                                    else -> PureBlack
                                },
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "$expLabel • Device: ${row.activeDeviceName.ifBlank { "Unknown" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { onManageUser(row) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Manage Plan / Suspend / Extend", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminEditUserPlanDialog(
    user: AdminUserDirectoryRow,
    onDismiss: () -> Unit,
    onApply: (PlanTier, List<String>, SubscriptionStatus, Int?, Boolean) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var tier by remember { mutableStateOf(PlanTier.fromDb(user.entitlement?.planTier)) }
    var examId by remember {
        mutableStateOf(
            user.entitlement?.unlockedExamIds?.firstOrNull { it != "all" }
                ?: PlatformCatalogDefaults.EXAM_AIIMS_NORCET
        )
    }
    var tenure by remember { mutableStateOf(PlanTenure.ONE_MONTH) }
    var status by remember { mutableStateOf(SubscriptionStatus.fromDb(user.entitlement?.status)) }
    var extendExisting by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage ${user.displayName.ifBlank { "User" }}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = user.email.ifBlank { user.uid },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Plan Tier", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PlanTier.FREE, PlanTier.SINGLE_EXAM, PlanTier.ALL_EXAM).forEach { t ->
                        MonochromeFilterPill(label = t.displayLabel, selected = tier == t, onClick = { tier = t })
                    }
                }
                if (tier == PlanTier.SINGLE_EXAM) {
                    Text("Exam Track", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            PlatformCatalogDefaults.EXAM_AIIMS_NORCET to "NORCET",
                            PlatformCatalogDefaults.EXAM_RRB_NURSING to "RRB",
                            PlatformCatalogDefaults.EXAM_PGIMER_NURSING to "PGIMER"
                        ).forEach { (id, lbl) ->
                            MonochromeFilterPill(label = lbl, selected = examId == id, onClick = { examId = id })
                        }
                    }
                }
                if (tier != PlanTier.FREE) {
                    Text("Tenure to Grant / Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlanTenure.entries.forEach { tn ->
                            MonochromeFilterPill(label = tn.shortCode, selected = tenure == tn, onClick = { tenure = tn })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stack onto active expiry", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = extendExisting, onCheckedChange = { extendExisting = it })
                    }
                }
                Text("Account Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MonochromeFilterPill(
                        label = "Active",
                        selected = status == SubscriptionStatus.ACTIVE,
                        onClick = { status = SubscriptionStatus.ACTIVE }
                    )
                    MonochromeFilterPill(
                        label = "Suspended",
                        selected = status == SubscriptionStatus.SUSPENDED,
                        onClick = { status = SubscriptionStatus.SUSPENDED }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exams = when (tier) {
                        PlanTier.ALL_EXAM, PlanTier.TRIAL -> listOf("all")
                        PlanTier.SINGLE_EXAM -> listOf(examId)
                        PlanTier.FREE -> emptyList()
                    }
                    val days = if (tier == PlanTier.FREE || status == SubscriptionStatus.SUSPENDED) null else tenure.days
                    onApply(tier, exams, status, days, extendExisting)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                )
            ) {
                Text("Save Plan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdminCodesTab(
    codes: List<CloudAccessCode>,
    lastGenerated: CloudAccessCode?,
    isMutating: Boolean,
    onGenerate: (PlanTier, String?, PlanTenure, String) -> Unit,
    onRevoke: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val editorial = MaterialTheme.editorialColors
    var tier by remember { mutableStateOf(PlanTier.ALL_EXAM) }
    var examId by remember { mutableStateOf(PlatformCatalogDefaults.EXAM_AIIMS_NORCET) }
    var tenure by remember { mutableStateOf(PlanTenure.THREE_MONTHS) }
    var buyerNote by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "code_generator") {
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
                    Text("Generate Single-Use Access Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonochromeFilterPill(
                            label = "All-Exam Prep",
                            selected = tier == PlanTier.ALL_EXAM,
                            onClick = { tier = PlanTier.ALL_EXAM }
                        )
                        MonochromeFilterPill(
                            label = "Single Exam",
                            selected = tier == PlanTier.SINGLE_EXAM,
                            onClick = { tier = PlanTier.SINGLE_EXAM }
                        )
                    }
                    if (tier == PlanTier.SINGLE_EXAM) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PlatformCatalogDefaults.EXAM_AIIMS_NORCET to "NORCET",
                                PlatformCatalogDefaults.EXAM_RRB_NURSING to "RRB",
                                PlatformCatalogDefaults.EXAM_PGIMER_NURSING to "PGIMER"
                            ).forEach { (id, lbl) ->
                                MonochromeFilterPill(label = lbl, selected = examId == id, onClick = { examId = id })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanTenure.entries.forEach { tn ->
                            MonochromeFilterPill(label = tn.shortCode, selected = tenure == tn, onClick = { tenure = tn })
                        }
                    }
                    OutlinedTextField(
                        value = buyerNote,
                        onValueChange = { buyerNote = it.take(100) },
                        label = { Text("Buyer Note / UPI Ref (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onGenerate(tier, if (tier == PlanTier.SINGLE_EXAM) examId else null, tenure, buyerNote)
                            buyerNote = ""
                        },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = editorial.acidLime, contentColor = editorial.onAcidLime),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_generate_code_btn")
                    ) {
                        Text("Generate Code", fontWeight = FontWeight.Bold)
                    }

                    if (lastGenerated != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LATEST CODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = lastGenerated.code,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = {
                                    copyToClipboard(context, "ClinEdge Code", lastGenerated.code)
                                    onShowMessage("Copied ${lastGenerated.code}")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code")
                                }
                                IconButton(onClick = {
                                    val shareMsg = "Your ClinEdge Access Code:\n${lastGenerated.code}\n\nOpen ClinEdge -> Plans & Access -> Redeem Access Code."
                                    openExternalUrl(
                                        context = context,
                                        url = EntitlementEvaluator.buildWhatsAppUrl("", shareMsg),
                                        fallbackMsg = "WhatsApp not available; code copied instead.",
                                        onError = {
                                            copyToClipboard(context, "ClinEdge Code", shareMsg)
                                            onShowMessage(it)
                                        }
                                    )
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share via WhatsApp")
                                }
                            }
                        }
                    }
                }
            }
        }

        items(items = codes, key = { it.code.ifBlank { it.id } }) { codeObj ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                border = BorderStroke(1.dp, editorial.borderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = codeObj.code,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${codeObj.planTier.uppercase()} • ${codeObj.tenureDays}d • ${codeObj.unlockedExamIds.joinToString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (codeObj.buyerNote.isNotBlank()) {
                            Text(
                                text = "Note: ${codeObj.buyerNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!codeObj.redeemedByEmail.isNullOrBlank()) {
                            Text(
                                text = "Redeemed by ${codeObj.redeemedByEmail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.correctText
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            copyToClipboard(context, "ClinEdge Code", codeObj.code)
                            onShowMessage("Copied ${codeObj.code}")
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                        }
                        if (codeObj.status == "active") {
                            TextButton(onClick = { onRevoke(codeObj.code) }) {
                                Text("Revoke", color = editorial.wrongText)
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(6.dp), color = editorial.elevatedSurface) {
                                Text(
                                    text = codeObj.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDeviceLocksTab(
    locks: List<CloudDeviceTrialLock>,
    isMutating: Boolean,
    onResetLock: (String, String) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var query by remember { mutableStateOf("") }
    val filtered = remember(locks, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) locks else locks.filter {
            it.shortDeviceCode.lowercase().contains(q) ||
                it.ownerUid.lowercase().contains(q) ||
                it.maskedEmail.lowercase().contains(q) ||
                it.deviceName.lowercase().contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "locks_search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Device Lock ID (CE-...) or UID (${locks.size})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(items = filtered, key = { it.deviceHash.ifBlank { it.id } }) { lock ->
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = lock.shortDeviceCode.ifBlank { lock.deviceHash.take(10) },
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${lock.deviceName.ifBlank { "Android" }} • ${lock.maskedEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "UID: ${lock.ownerUid.take(24)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onResetLock(lock.deviceHash.ifBlank { lock.id }, lock.shortDeviceCode) },
                        enabled = !isMutating,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, editorial.wrongText.copy(alpha = 0.5f))
                    ) {
                        Text("Reset Lock", color = editorial.wrongText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminContentPushTab(
    draft: AdminContentDraftState,
    packs: List<CloudQuestionPack>,
    onPickFile: () -> Unit,
    onUpdateDraft: ((AdminContentDraftState) -> AdminContentDraftState) -> Unit,
    onPreview: () -> Unit,
    onRemovePreview: (Int) -> Unit,
    onPublish: () -> Unit,
    onDeletePack: (String, String) -> Unit
) {
    val editorial = MaterialTheme.editorialColors

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "content_upload_card") {
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
                    Text(
                        text = "Push PDF / MCQ Pack Live (OTA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select a PDF, TXT, or JSON question bank from your phone (or paste raw text below). Once published, every student's app auto-syncs the new questions into their exam bank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onPickFile,
                        enabled = !draft.isParsingOrExtracting && !draft.isPublishing,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_pick_pdf_btn")
                    ) {
                        if (draft.isParsingOrExtracting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = if (draft.extractedFileName.isBlank()) {
                                    "Select PDF / TXT / JSON File"
                                } else {
                                    "Loaded: ${draft.extractedFileName}"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { v -> onUpdateDraft { it.copy(title = v.take(100)) } },
                        label = { Text("Pack Title (e.g., NORCET-10 Shift 1 Recall)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Target Exam", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            PlatformCatalogDefaults.EXAM_AIIMS_NORCET to "NORCET",
                            PlatformCatalogDefaults.EXAM_RRB_NURSING to "RRB",
                            PlatformCatalogDefaults.EXAM_PGIMER_NURSING to "PGIMER"
                        ).forEach { (id, lbl) ->
                            MonochromeFilterPill(
                                label = lbl,
                                selected = draft.examId == id,
                                onClick = { onUpdateDraft { it.copy(examId = id) } }
                            )
                        }
                    }

                    if (draft.examId == PlatformCatalogDefaults.EXAM_AIIMS_NORCET) {
                        Text("NORCET Stage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("all" to "Both", "prelims" to "Stage I Prelims", "mains" to "Stage II Mains").forEach { (k, lbl) ->
                                MonochromeFilterPill(
                                    label = lbl,
                                    selected = draft.stage == k,
                                    onClick = { onUpdateDraft { it.copy(stage = k) } }
                                )
                            }
                        }
                    }

                    Text("Default Subject, Type & Difficulty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubjectCatalog.ALL_SUBJS.forEach { subj ->
                            MonochromeFilterPill(
                                label = subj.code,
                                selected = draft.subject == subj.code,
                                onClick = { onUpdateDraft { it.copy(subject = subj.code) } }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "pyq" to "PYQ Paper",
                            "nur" to "Nursing Practice",
                            "mock" to "Predicted Mock"
                        ).forEach { (cat, lbl) ->
                            MonochromeFilterPill(
                                label = lbl,
                                selected = draft.category == cat,
                                onClick = { onUpdateDraft { it.copy(category = cat) } }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuestionDifficulty.entries.forEach { diff ->
                            MonochromeFilterPill(
                                label = diff.label,
                                selected = draft.difficulty == diff,
                                onClick = { onUpdateDraft { it.copy(difficulty = diff) } }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Premium-Only Pack", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = draft.isPremiumOnly,
                            onCheckedChange = { checked -> onUpdateDraft { it.copy(isPremiumOnly = checked) } }
                        )
                    }

                    OutlinedTextField(
                        value = draft.rawText,
                        onValueChange = { v -> onUpdateDraft { it.copy(rawText = v) } },
                        label = { Text("Extracted or Pasted MCQ Text / JSON") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPreview,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_parse_preview_btn")
                        ) {
                            Text(
                                text = "Parse & Preview",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onPublish,
                            enabled = draft.previewQuestions.isNotEmpty() && !draft.isPublishing,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_publish_pack_btn")
                        ) {
                            if (draft.isPublishing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PureBlack)
                            } else {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = "Publish (${draft.previewQuestions.size})",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (draft.previewQuestions.isNotEmpty()) {
            item(key = "preview_title") {
                Text(
                    text = "Preview Parsed Questions (${draft.previewQuestions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            itemsIndexed(
                items = draft.previewQuestions.take(25),
                key = { idx, q -> "${q.id}_$idx" }
            ) { idx, q ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Q${idx + 1}. ${q.questionText}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Ans: ${'A' + q.correctIndex}. ${q.options.getOrElse(q.correctIndex) { "" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = editorial.correctText
                            )
                        }
                        IconButton(onClick = { onRemovePreview(idx) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove question", tint = editorial.wrongText)
                        }
                    }
                }
            }
        }

        item(key = "published_packs_header") {
            Text(
                text = "Published Live OTA Packs (${packs.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        items(items = packs, key = { it.packId.ifBlank { it.id } }) { pack ->
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${EntitlementEvaluator.shortExamLabel(pack.examId)} • ${pack.questionCount} Qs • v${pack.version} • ${pack.stage.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeletePack(pack.packId.ifBlank { pack.id }, pack.title) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete pack", tint = editorial.wrongText)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPricingSupportTab(
    config: CloudSubscriptionConfig,
    isSaving: Boolean,
    onSave: (CloudSubscriptionConfig) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var s1m by remember(config) { mutableStateOf(config.singleExam1M.toString()) }
    var s3m by remember(config) { mutableStateOf(config.singleExam3M.toString()) }
    var s6m by remember(config) { mutableStateOf(config.singleExam6M.toString()) }
    var s1y by remember(config) { mutableStateOf(config.singleExam1Y.toString()) }
    var a1m by remember(config) { mutableStateOf(config.allExam1M.toString()) }
    var a3m by remember(config) { mutableStateOf(config.allExam3M.toString()) }
    var a6m by remember(config) { mutableStateOf(config.allExam6M.toString()) }
    var a1y by remember(config) { mutableStateOf(config.allExam1Y.toString()) }
    var freeLimit by remember(config) { mutableStateOf(config.freeDailyQuestionLimit.toString()) }
    var trialDays by remember(config) { mutableStateOf(config.trialDays.toString()) }
    var whatsapp by remember(config) { mutableStateOf(config.supportWhatsAppNumber) }
    var email by remember(config) { mutableStateOf(config.supportEmail) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "pricing_form") {
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
                    Text("Single Exam Pro Prices (₹ INR)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = s1m, onValueChange = { s1m = it.filter(Char::isDigit) }, label = { Text("1M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = s3m, onValueChange = { s3m = it.filter(Char::isDigit) }, label = { Text("3M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = s6m, onValueChange = { s6m = it.filter(Char::isDigit) }, label = { Text("6M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = s1y, onValueChange = { s1y = it.filter(Char::isDigit) }, label = { Text("1Y ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider(color = editorial.borderSubtle)
                    Text("All-Exam Nursing Prep Prices (₹ INR)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = a1m, onValueChange = { a1m = it.filter(Char::isDigit) }, label = { Text("1M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = a3m, onValueChange = { a3m = it.filter(Char::isDigit) }, label = { Text("3M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = a6m, onValueChange = { a6m = it.filter(Char::isDigit) }, label = { Text("6M ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = a1y, onValueChange = { a1y = it.filter(Char::isDigit) }, label = { Text("1Y ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider(color = editorial.borderSubtle)
                    Text("Free Tier Limits & Support Contacts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = freeLimit, onValueChange = { freeLimit = it.filter(Char::isDigit) }, label = { Text("Free Qs/day") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = trialDays, onValueChange = { trialDays = it.filter(Char::isDigit) }, label = { Text("Trial Days") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it.take(24) },
                        label = { Text("Support WhatsApp Number (with country code)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.take(120) },
                        label = { Text("Support Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSave(
                                config.copy(
                                    singleExam1M = s1m.toLongOrNull() ?: config.singleExam1M,
                                    singleExam3M = s3m.toLongOrNull() ?: config.singleExam3M,
                                    singleExam6M = s6m.toLongOrNull() ?: config.singleExam6M,
                                    singleExam1Y = s1y.toLongOrNull() ?: config.singleExam1Y,
                                    allExam1M = a1m.toLongOrNull() ?: config.allExam1M,
                                    allExam3M = a3m.toLongOrNull() ?: config.allExam3M,
                                    allExam6M = a6m.toLongOrNull() ?: config.allExam6M,
                                    allExam1Y = a1y.toLongOrNull() ?: config.allExam1Y,
                                    freeDailyQuestionLimit = (freeLimit.toLongOrNull() ?: 15L).coerceIn(1L, 200L),
                                    trialDays = (trialDays.toLongOrNull() ?: 3L).coerceIn(1L, 3L),
                                    supportWhatsAppNumber = whatsapp.trim(),
                                    supportEmail = email.trim()
                                )
                            )
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = editorial.acidLime, contentColor = editorial.onAcidLime),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("admin_save_pricing_btn")
                    ) {
                        Text(if (isSaving) "Saving..." else "Save Live Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
