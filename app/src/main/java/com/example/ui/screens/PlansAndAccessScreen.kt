package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.example.data.cloud.SignedInAccountInfo
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.subscription.DeviceLockConflictState
import com.example.data.subscription.EntitlementEvaluator
import com.example.data.subscription.PaywallPromptState
import com.example.data.subscription.PlanTenure
import com.example.data.subscription.PlanTier
import com.example.data.subscription.SubscriptionUiState
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.AcidLime
import com.example.ui.theme.EditorialBannerKpiAccent
import com.example.ui.theme.EditorialBannerSurface
import com.example.ui.theme.EditorialBannerTitle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.editorialColors

@Composable
fun PlansAndAccessScreen(
    subscription: SubscriptionUiState,
    account: SignedInAccountInfo,
    deviceName: String,
    isTrialBusy: Boolean,
    isRedeemBusy: Boolean,
    onStartTrial: () -> Unit,
    onRedeemCode: (String, () -> Unit) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val editorial = MaterialTheme.editorialColors

    var selectedPlanGroup by remember { mutableStateOf(PlanTier.ALL_EXAM) }
    var selectedExamId by remember { mutableStateOf(PlatformCatalogDefaults.EXAM_AIIMS_NORCET) }
    var selectedTenure by remember { mutableStateOf(PlanTenure.THREE_MONTHS) }
    var codeInput by remember { mutableStateOf("") }

    val activePrice = subscription.config.priceFor(selectedPlanGroup, selectedTenure)
    val selectedPlanSummary = if (selectedPlanGroup == PlanTier.ALL_EXAM) {
        "All-Exam Nursing Prep • ${selectedTenure.label} (₹$activePrice)"
    } else {
        "${EntitlementEvaluator.shortExamLabel(selectedExamId)} Pro • ${selectedTenure.label} (₹$activePrice)"
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            item(key = "plans_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("plans_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plans & Access",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exam-specific plans, All-Exam Prep & Access Codes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current Status Hero Banner
            item(key = "plans_status_card") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialBannerSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (subscription.hasAnyActivePaidOrTrial || subscription.isAdmin) {
                                    AcidLime
                                } else {
                                    EditorialBannerTitle.copy(alpha = 0.14f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        subscription.isAdmin -> "OWNER ADMIN"
                                        subscription.isSuspended -> "SUSPENDED"
                                        subscription.isExpired -> "EXPIRED"
                                        subscription.effectiveTier == PlanTier.TRIAL -> "FREE TRIAL ACTIVE"
                                        subscription.hasAnyActivePaidOrTrial -> "PRO ACTIVE"
                                        else -> "FREE TIER"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = if (subscription.hasAnyActivePaidOrTrial || subscription.isAdmin) {
                                        PureBlack
                                    } else {
                                        EditorialBannerTitle
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = subscription.deviceShortCode,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = EditorialBannerTitle.copy(alpha = 0.72f)
                            )
                        }

                        Text(
                            text = subscription.planDisplayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = EditorialBannerTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subscription.expirySummaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialBannerKpiAccent
                        )

                        if (subscription.isExpired || subscription.isSuspended) {
                            Text(
                                text = "Your bookmarks, attempt history & mock scores remain 100% safe. Redeem a renewal code to resume instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialBannerTitle.copy(alpha = 0.78f)
                            )
                        }

                        if (!subscription.trialUsed && !subscription.hasAnyActivePaidOrTrial && !subscription.isAdmin) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onStartTrial,
                                enabled = !isTrialBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidLime,
                                    contentColor = PureBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("start_free_trial_btn")
                            ) {
                                if (isTrialBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PureBlack
                                    )
                                } else {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        text = "Start ${subscription.config.trialDays}-Day All-Access Free Trial",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Unlocks all exams, PYQs, full CBT mocks & PDF export for ${subscription.config.trialDays} days. Limited to 1 trial per device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialBannerTitle.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            }

            // Redeem Post-Payment Access Code Card
            item(key = "redeem_code_card") {
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
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Have an Access Code?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Received your code after payment? Enter it below to unlock immediately. Renewals automatically stack days onto your active plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase().take(32) },
                            label = { Text("Access Code") },
                            placeholder = { Text("CE-ALL-3M-XXXX-XXXX") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("access_code_input")
                        )
                        Button(
                            onClick = {
                                onRedeemCode(codeInput) { codeInput = "" }
                            },
                            enabled = codeInput.trim().length >= 6 && !isRedeemBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("redeem_code_btn")
                        ) {
                            if (isRedeemBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Activate Access Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Plan Selector (All-Exam vs Exam-Specific + Tenures)
            item(key = "plan_picker_card") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose Your Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Tier Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlanChoiceCard(
                                title = "All-Exam Prep",
                                subtitle = "RRB + NORCET + PGIMER",
                                badge = "BEST VALUE",
                                selected = selectedPlanGroup == PlanTier.ALL_EXAM,
                                onClick = { selectedPlanGroup = PlanTier.ALL_EXAM },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("plan_tier_all_exam")
                            )
                            PlanChoiceCard(
                                title = "Single Exam",
                                subtitle = "Pick 1 target exam",
                                badge = "BUDGET",
                                selected = selectedPlanGroup == PlanTier.SINGLE_EXAM,
                                onClick = { selectedPlanGroup = PlanTier.SINGLE_EXAM },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("plan_tier_single_exam")
                            )
                        }

                        if (selectedPlanGroup == PlanTier.SINGLE_EXAM) {
                            Text(
                                text = "Select Exam Track",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    PlatformCatalogDefaults.EXAM_AIIMS_NORCET to "NORCET",
                                    PlatformCatalogDefaults.EXAM_RRB_NURSING to "RRB",
                                    PlatformCatalogDefaults.EXAM_PGIMER_NURSING to "PGIMER"
                                ).forEach { (id, label) ->
                                    MonochromeFilterPill(
                                        label = label,
                                        selected = selectedExamId == id,
                                        onClick = { selectedExamId = id }
                                    )
                                }
                            }
                        }

                        // Tenure Grid (1M / 3M / 6M / 1Y)
                        Text(
                            text = "Select Tenure",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlanTenure.entries.forEach { tenure ->
                                val price = subscription.config.priceFor(selectedPlanGroup, tenure)
                                val isSel = selectedTenure == tenure
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) editorial.acidLime else editorial.elevatedSurface,
                                    border = BorderStroke(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) editorial.onAcidLime.copy(alpha = 0.25f) else editorial.borderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedTenure = tenure }
                                        .testTag("tenure_${tenure.shortCode}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tenure.shortCode,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = if (isSel) editorial.onAcidLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "₹$price",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                            color = if (isSel) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = editorial.borderSubtle)

                        // Benefits List
                        val features = if (selectedPlanGroup == PlanTier.ALL_EXAM) {
                            listOf(
                                "Unlimited questions across RRB, AIIMS NORCET (Prelims + Mains) & PGIMER",
                                "Full PYQ shift-wise vaults & Cross-Exam Subject Practice mode",
                                "Unlimited Full CBT Mock Exams with section locks & score reports",
                                "Watermarked A4 PDF study worksheets & OTA new question packs"
                            )
                        } else {
                            listOf(
                                "Unlimited practice questions for ${EntitlementEvaluator.shortExamLabel(selectedExamId)}",
                                "All ${EntitlementEvaluator.shortExamLabel(selectedExamId)} Previous Year shift papers",
                                "Unlimited Full CBT Mock Exams for ${EntitlementEvaluator.shortExamLabel(selectedExamId)}",
                                "Watermarked A4 PDF worksheets & OTA new question packs"
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            features.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = if (editorial.isDark) AcidLime else PureBlack,
                                        modifier = Modifier.padding(top = 2.dp).size(15.dp)
                                    )
                                    Text(
                                        text = feat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val msg = EntitlementEvaluator.buildSupportMessage(
                                    reason = "Purchase Access Code",
                                    uid = account.uid,
                                    email = account.email,
                                    planLabel = subscription.planDisplayName,
                                    shortDeviceCode = subscription.deviceShortCode,
                                    deviceName = deviceName,
                                    requestedPlanSummary = selectedPlanSummary
                                )
                                openExternalUrl(
                                    context = context,
                                    url = EntitlementEvaluator.buildWhatsAppUrl(
                                        subscription.config.supportWhatsAppNumber,
                                        msg
                                    ),
                                    fallbackMsg = "WhatsApp not available — use Email Support below.",
                                    onError = onShowMessage
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("request_plan_whatsapp_btn")
                        ) {
                            Text(
                                text = "Get Code on WhatsApp • ₹$activePrice (${selectedTenure.label})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Free vs Pro Comparison + Support Card
            item(key = "support_card") {
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
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Customer Support & Account Recovery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Need help activating a code, switching accounts on this phone, or clearing a device trial lock? Reach out with your pre-filled IDs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "UID: ${account.uid.take(22)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Device Lock ID: ${subscription.deviceShortCode} ($deviceName)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val msg = EntitlementEvaluator.buildSupportMessage(
                                        reason = "Account / Plan / Device Lock Support",
                                        uid = account.uid,
                                        email = account.email,
                                        planLabel = subscription.planDisplayName,
                                        shortDeviceCode = subscription.deviceShortCode,
                                        deviceName = deviceName
                                    )
                                    openExternalUrl(
                                        context = context,
                                        url = EntitlementEvaluator.buildWhatsAppUrl(
                                            subscription.config.supportWhatsAppNumber,
                                            msg
                                        ),
                                        fallbackMsg = "WhatsApp not installed.",
                                        onError = onShowMessage
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("support_whatsapp_btn")
                            ) {
                                Text("WhatsApp", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val body = EntitlementEvaluator.buildSupportMessage(
                                        reason = "Account / Plan / Device Lock Support",
                                        uid = account.uid,
                                        email = account.email,
                                        planLabel = subscription.planDisplayName,
                                        shortDeviceCode = subscription.deviceShortCode,
                                        deviceName = deviceName
                                    )
                                    openExternalUrl(
                                        context = context,
                                        url = EntitlementEvaluator.buildMailtoUri(
                                            supportEmail = subscription.config.supportEmail,
                                            subject = "ClinEdge Support (${subscription.deviceShortCode})",
                                            body = body
                                        ),
                                        fallbackMsg = "No email app found. Copy your IDs instead.",
                                        onError = onShowMessage
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("support_email_btn")
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Email", fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(
                                onClick = {
                                    val copyText = EntitlementEvaluator.buildSupportMessage(
                                        reason = "ClinEdge Account Details",
                                        uid = account.uid,
                                        email = account.email,
                                        planLabel = subscription.planDisplayName,
                                        shortDeviceCode = subscription.deviceShortCode,
                                        deviceName = deviceName
                                    )
                                    copyToClipboard(context, "ClinEdge Support IDs", copyText)
                                    onShowMessage("Account & Device Lock IDs copied!")
                                },
                                modifier = Modifier.testTag("copy_support_ids_btn")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy account IDs")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanChoiceCard(
    title: String,
    subtitle: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else editorial.elevatedSurface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else editorial.borderSubtle
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (selected) AcidLime else editorial.cardSurface
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = if (selected) PureBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PaywallPromptDialog(
    prompt: PaywallPromptState,
    canStartTrial: Boolean,
    onDismiss: () -> Unit,
    onOpenPlans: () -> Unit,
    onStartTrial: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(prompt.title, fontWeight = FontWeight.Bold) },
        text = { Text(prompt.message) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenPlans()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier.testTag("paywall_view_plans_btn")
            ) {
                Text("View Plans / Redeem", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (canStartTrial) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onStartTrial()
                        },
                        modifier = Modifier.testTag("paywall_start_trial_btn")
                    ) {
                        Text("3-Day Free Trial", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Not now")
                }
            }
        }
    )
}

@Composable
fun DeviceLockSupportDialog(
    conflict: DeviceLockConflictState,
    subscription: SubscriptionUiState,
    account: SignedInAccountInfo,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val editorial = MaterialTheme.editorialColors
    val supportBody = remember(conflict, account) {
        EntitlementEvaluator.buildSupportMessage(
            reason = "Request Device Trial Lock Reset (Previous account: ${conflict.maskedOwnerEmail})",
            uid = account.uid,
            email = account.email,
            planLabel = subscription.planDisplayName,
            shortDeviceCode = conflict.shortDeviceCode,
            deviceName = conflict.deviceName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PhonelinkLock, contentDescription = null) },
        title = { Text("Free Trial Already Used on This Phone", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A 3-day free trial was already claimed on this device by account ${conflict.maskedOwnerEmail}."
                )
                Text(
                    "If you switched Google accounts or bought a second-hand phone, contact Support to manually clear this device lock:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = editorial.elevatedSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle)
                ) {
                    Text(
                        text = "Device Lock ID: ${conflict.shortDeviceCode}\nAccount UID: ${account.uid.take(24)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openExternalUrl(
                        context = context,
                        url = EntitlementEvaluator.buildWhatsAppUrl(
                            subscription.config.supportWhatsAppNumber,
                            supportBody
                        ),
                        fallbackMsg = "WhatsApp unavailable — try Email Support.",
                        onError = onShowMessage
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorial.acidLime,
                    contentColor = editorial.onAcidLime
                ),
                modifier = Modifier.testTag("device_lock_whatsapp_btn")
            ) {
                Text("Contact WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        openExternalUrl(
                            context = context,
                            url = EntitlementEvaluator.buildMailtoUri(
                                supportEmail = subscription.config.supportEmail,
                                subject = "Device Trial Lock Reset (${conflict.shortDeviceCode})",
                                body = supportBody
                            ),
                            fallbackMsg = "No email app found.",
                            onError = onShowMessage
                        )
                    },
                    modifier = Modifier.testTag("device_lock_email_btn")
                ) {
                    Text("Email Support")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun openExternalUrl(
    context: Context,
    url: String,
    fallbackMsg: String,
    onError: (String) -> Unit
) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        onError(fallbackMsg)
    }
}
