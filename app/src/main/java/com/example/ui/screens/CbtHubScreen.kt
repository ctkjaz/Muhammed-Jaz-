package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsPreferences
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CbtHubScreen(
    state: DashboardDerivedState,
    useTimeLimit: Boolean,
    secondsPerQuestion: Int,
    onStartStandardMock: (Int, Int?, String, Boolean) -> Unit,
    onStartNorcetMainsMock: () -> Unit,
    onStartCustomCbt: (List<QuestionItem>, String, Int?) -> Unit,
    onOpenTimedPracticeConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val all = state.questions
    val activeExam = state.activeExam
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExam?.id)
    val examShort = activeExam?.shortName ?: "RRB Nursing"
    val fullQ = activeExam?.defaultMockQuestions ?: 100
    val fullMins = activeExam?.defaultMockMinutes ?: 90
    val negMark = activeExam?.negativeMarks ?: 0.33
    val nurPool = all.filter { SubjectCatalog.isNursing(it.question) }
    val nonPool = all.filter { !SubjectCatalog.isNursing(it.question) }
    val hardClinicalPool = all.filter {
        SubjectCatalog.isNursing(it.question) &&
            (it.id.startsWith("N_") || it.question.difficulty == QuestionDifficulty.HARD)
    }.ifEmpty { nurPool }
    val primaryColor = MaterialTheme.colorScheme.primary
    val editorial = MaterialTheme.editorialColors
    val mini30Mins = SettingsPreferences.calculateSuggestedMinutes(30, secondsPerQuestion)
    val nur50Mins = SettingsPreferences.calculateSuggestedMinutes(50, secondsPerQuestion)
    val non30Mins = SettingsPreferences.calculateSuggestedMinutes(30, secondsPerQuestion)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 640.dp) 2 else 1

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$examShort CBT & Timed Practice Hub", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (isNorcet) {
                            "Stage I Prelims (100Q / 90m • 80:20) & Stage II Mains (160Q / 180m • 100% Clinical) • +1 / -$negMark"
                        } else {
                            "Exam-style question palette, countdown timer, +1 / -$negMark marking & custom timed drills"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isNorcet) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("norcet_pattern_info_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AIIMS NORCET Two-Stage Exam Blueprint (Official Pattern)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Stage I (Prelims — Qualifying): 100 MCQs • 90 min • 5 time-locked sequential sections (20Q / 18m each) • 80 Nursing + 20 GK & Aptitude • +1 / -0.33.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Stage II (Mains — Final Merit): 160 MCQs • 180 min • 4 time-locked sequential sections (40Q / 45m each) • 100% Clinical & Priority Nursing • +1 / -0.33.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Qualifying Cut-off: 50th percentile UR/EWS, 45th OBC, 40th SC/ST (1:5 shortlist for Stage II Mains).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Custom Timed Practice Highlight Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Custom Timed Practice Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            "Pick any specific subject (e.g. MSN, Pharmacology) or a Mixed 15 / 30 / 50Q set with a configurable timer (e.g. 30m for 30Q, 45m for 50Q) and instant answer feedback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = onOpenTimedPracticeConfig,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cbt_hub_timed_btn")
                        ) {
                            Text("Configure Timed Practice")
                        }
                    }
                }
            }

            val cbtOptions = if (isNorcet) {
                listOf(
                    CbtOptionData(
                        badge = "Stage I • 5×18m Lock",
                        durationLabel = "$fullMins Minutes",
                        title = "NORCET Prelims Full Mock ($fullQ Qs)",
                        desc = "Official 5 time-locked sections × 20Q (18m each): 4 Nursing sections + 1 GK/Aptitude section. +1 / -$negMark.",
                        badgeColor = primaryColor,
                        btnLabel = "Start Prelims Mock ($fullQ Qs)",
                        isPrimary = true,
                        tag = "cbt_100q_btn",
                        onClick = { onStartStandardMock(fullQ, fullMins, "NORCET Stage I Prelims (${fullQ}Q)", true) }
                    ),
                    CbtOptionData(
                        badge = "Stage II • 4×45m Lock",
                        durationLabel = "180 Minutes",
                        title = "NORCET Mains Clinical CBT (160 Qs)",
                        desc = "Official 4 time-locked sections × 40Q (45m each): 100% Clinical Scenarios, ICU/ER, OBG, Peds & Dosage Math. +1 / -$negMark.",
                        badgeColor = editorial.wrongText,
                        btnLabel = "Start Mains Mock (160 Qs)",
                        isPrimary = true,
                        tag = "cbt_norcet_mains_btn",
                        onClick = onStartNorcetMainsMock
                    ),
                    CbtOptionData(
                        badge = "Mains 1 Section",
                        durationLabel = "45 Minutes",
                        title = "40Q Stage II Clinical Section Drill",
                        desc = "Simulate ONE exact 45-minute NORCET Mains clinical block (bedside priority, ABG, ECG, high-alert meds).",
                        badgeColor = editorial.warningText,
                        btnLabel = "Start 40Q Mains Section",
                        isPrimary = false,
                        tag = "cbt_norcet_mains_sec_btn",
                        onClick = { onStartCustomCbt(hardClinicalPool.shuffled().take(40), "NORCET Mains 40Q Section (45m)", 45) }
                    ),
                    CbtOptionData(
                        badge = "Prelims 1 Section",
                        durationLabel = "18 Minutes",
                        title = "20Q Stage I Speed Section Sprint",
                        desc = "Simulate ONE exact 18-minute NORCET Prelims nursing section to sharpen time-locked pacing.",
                        badgeColor = editorial.correctText,
                        btnLabel = "Start 20Q Prelims Section",
                        isPrimary = false,
                        tag = "cbt_norcet_prelims_sec_btn",
                        onClick = { onStartCustomCbt(nurPool.shuffled().take(20), "NORCET Prelims 20Q Section (18m)", 18) }
                    ),
                    CbtOptionData(
                        badge = "80:20 Express",
                        durationLabel = if (useTimeLimit) "$mini30Mins Minutes" else "Untimed",
                        title = "Mini 30Q Prelims Drill",
                        desc = "Exact 80:20 split (24 Nursing + 6 GK/Aptitude) for focused daily speed training.",
                        badgeColor = editorial.correctText,
                        btnLabel = "Start Mini Mock (30 Qs)",
                        isPrimary = false,
                        tag = "cbt_30q_btn",
                        onClick = { onStartStandardMock(30, null, "NORCET Mini 30Q Drill", false) }
                    ),
                    CbtOptionData(
                        badge = "Prelims 20%",
                        durationLabel = if (useTimeLimit) "18 Minutes" else "Untimed",
                        title = "20Q Prelims GK & Aptitude Block",
                        desc = "Exact 20-question General Knowledge, Health Programs, Quantitative Aptitude & Reasoning section.",
                        badgeColor = editorial.accentDustyRoseText,
                        btnLabel = "Start 20Q GK & Aptitude",
                        isPrimary = false,
                        tag = "cbt_30q_gen_btn",
                        onClick = { onStartCustomCbt(nonPool.shuffled().take(20), "20Q NORCET GK & Aptitude CBT", 18) }
                    )
                )
            } else {
                listOf(
                    CbtOptionData(
                        badge = "Official Pattern",
                        durationLabel = "$fullMins Minutes",
                        title = "Full $fullQ-Question CBT Mock",
                        desc = "70% Nursing + 30% Non-Tech (Science, Math, Reasoning & Current Affairs/GK). Negative marking: -$negMark.",
                        badgeColor = primaryColor,
                        btnLabel = "Start Full Mock ($fullQ Qs)",
                        isPrimary = true,
                        tag = "cbt_100q_btn",
                        onClick = { onStartStandardMock(fullQ, fullMins, "Full Official ${fullQ}Q CBT Mock", true) }
                    ),
                    CbtOptionData(
                        badge = "Express Drill",
                        durationLabel = if (useTimeLimit) "$mini30Mins Minutes" else "Untimed",
                        title = "Mini 30-Question CBT Mock",
                        desc = "Exact 70:30 ratio (21 Nursing + 9 Non-Tech/CA). Ideal for quick daily practice.",
                        badgeColor = editorial.correctText,
                        btnLabel = "Start Mini Mock (30 Qs)",
                        isPrimary = false,
                        tag = "cbt_30q_btn",
                        onClick = { onStartStandardMock(30, null, "Mini 30Q CBT Mock", false) }
                    ),
                    CbtOptionData(
                        badge = "Clinical Speed",
                        durationLabel = if (useTimeLimit) "$nur50Mins Minutes" else "Untimed",
                        title = "50Q Nursing-Only Speed CBT",
                        desc = "Focused 50-question CBT covering all 9 clinical nursing subjects.",
                        badgeColor = editorial.wrongText,
                        btnLabel = "Start 50Q Nursing CBT",
                        isPrimary = false,
                        tag = "cbt_50q_nur_btn",
                        onClick = { onStartCustomCbt(nurPool.shuffled().take(50), "50Q Nursing CBT", null) }
                    ),
                    CbtOptionData(
                        badge = "Non-Tech + CA",
                        durationLabel = if (useTimeLimit) "$non30Mins Minutes" else "Untimed",
                        title = "30Q Non-Tech & CA CBT",
                        desc = "2025–26 Current Affairs, Static GK, Science, Arithmetic & Reasoning.",
                        badgeColor = editorial.accentDustyRoseText,
                        btnLabel = "Start 30Q Non-Tech CBT",
                        isPrimary = false,
                        tag = "cbt_30q_gen_btn",
                        onClick = { onStartCustomCbt(nonPool.shuffled().take(30), "30Q Non-Tech CBT", null) }
                    )
                )
            }

            val rows = cbtOptions.chunked(columns)
            items(rows.size) { rowIdx ->
                val rowItems = rows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { opt ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = opt.badgeColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            opt.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = opt.badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        opt.durationLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(opt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    opt.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (opt.isPrimary) {
                                    Button(
                                        onClick = opt.onClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(opt.tag),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(opt.btnLabel, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = opt.onClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(opt.tag),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(opt.btnLabel, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (state.mockHistory.isNotEmpty()) {
                item {
                    Text(
                        "Recent CBT Mock Scores",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(state.mockHistory) { h ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(h.timestamp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "$dateStr • ✓ ${h.correctCount}  ✕ ${h.wrongCount}  Skipped ${h.skippedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f / %d", h.netScore, h.totalQuestions),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${h.accuracyPercent}% acc",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = editorial.correctText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CbtOptionData(
    val badge: String,
    val durationLabel: String,
    val title: String,
    val desc: String,
    val badgeColor: Color,
    val btnLabel: String,
    val isPrimary: Boolean,
    val tag: String,
    val onClick: () -> Unit
)
