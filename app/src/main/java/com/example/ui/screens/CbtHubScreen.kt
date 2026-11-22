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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsPreferences
import com.example.data.model.ExamStage
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.components.SegmentedPillBar
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
    onSelectExamStage: (ExamStage) -> Unit,
    onStartStandardMock: (Int, Int?, String, Boolean) -> Unit,
    onStartNorcetMainsMock: (Int, Int, String) -> Unit,
    onStartCustomCbt: (List<QuestionItem>, String, Int?) -> Unit,
    onOpenTimedPracticeConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val activeExam = state.activeExam
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExam?.id)
    val isPgimer = PlatformCatalogDefaults.isPgimerExam(activeExam?.id)
    val examShort = activeExam?.shortName ?: "RRB Nursing"
    val fullQ = activeExam?.defaultMockQuestions ?: 100
    val fullMins = activeExam?.defaultMockMinutes ?: 90
    val negMark = activeExam?.negativeMarks ?: 0.33
    val negMarkLabel = String.format(Locale.US, "%.2f", negMark)

    // Strictly stage-separated question pools for NORCET
    val stagePool = state.stageQuestions.ifEmpty { state.questions }
    val nurPool = stagePool.filter { SubjectCatalog.isNursing(it.question) }
    val nonPool = stagePool.filter { !SubjectCatalog.isNursing(it.question) }
    val pyqPool = stagePool.filter { it.question.isPyqTagged }
    val hardClinicalPool = stagePool.filter {
        SubjectCatalog.isNursing(it.question) &&
            (it.question.questionText.contains("[Mains", ignoreCase = true) ||
                it.id.startsWith("P_") ||
                it.question.difficulty == QuestionDifficulty.HARD)
    }.ifEmpty { nurPool }

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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$examShort CBT Mocks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isPgimer -> "100Q · 100m · 100% Core Nursing · +1 / -$negMarkLabel"
                            isNorcet && state.activeExamStage == ExamStage.MAINS -> "Stage II Mains · 160Q · 180m (4×45m locked) · 100% Clinical · +1 / -$negMarkLabel"
                            isNorcet -> "Stage I Prelims · 100Q · 90m (5×18m locked) · 80:20 · +1 / -$negMarkLabel"
                            else -> "100Q · 90m · 70:30 Pattern · +1 / -$negMarkLabel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Strictly separate Stage I Prelims vs Stage II Mains mocks for AIIMS NORCET
            if (state.activeExamSupportsStages) {
                item {
                    SegmentedPillBar(
                        options = ExamStage.entries,
                        selected = state.activeExamStage,
                        labelOf = { stage ->
                            when (stage) {
                                ExamStage.PRELIMS -> "Stage I · Prelims"
                                ExamStage.MAINS -> "Stage II · Mains"
                            }
                        },
                        onSelect = onSelectExamStage,
                        testTagOf = { "cbt_stage_${it.id}" }
                    )
                }
            }

            // Custom Timed Practice Card (Compact)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = if (editorial.isDark) editorial.acidLime else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Custom Timed Drill",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Choose subject, count & timer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onOpenTimedPracticeConfig,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("cbt_hub_timed_btn")
                        ) {
                            Text("Configure", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val cbtOptions = when {
                isPgimer -> listOf(
                    CbtOptionData(
                        badge = "Official Pattern",
                        durationLabel = "$fullMins min",
                        title = "PGIMER Full Mock ($fullQ Qs)",
                        desc = "100% Core Nursing • +1 / -0.25 marking.",
                        btnLabel = "Start Full Mock",
                        isPrimary = true,
                        tag = "cbt_100q_btn",
                        onClick = { onStartStandardMock(fullQ, fullMins, "PGIMER Official ${fullQ}Q CBT Mock", true) }
                    ),
                    CbtOptionData(
                        badge = "PYQ Recall",
                        durationLabel = "100 min",
                        title = "PGIMER 2020–2025 PYQ (${pyqPool.size.coerceAtMost(100)} Qs)",
                        desc = "Memory-based PGIMER Nursing Officer sets.",
                        btnLabel = "Start PYQ Mock",
                        isPrimary = false,
                        tag = "cbt_pgimer_pyq_btn",
                        onClick = { onStartCustomCbt(pyqPool.shuffled().take(100), "PGIMER 2020–2025 PYQ Recall CBT", 100) }
                    ),
                    CbtOptionData(
                        badge = "Clinical Sprint",
                        durationLabel = "50 min",
                        title = "50Q Clinical Nursing Sprint",
                        desc = "High-yield ICU, emergency & bedside MCQs.",
                        btnLabel = "Start 50Q Sprint",
                        isPrimary = false,
                        tag = "cbt_50q_nur_btn",
                        onClick = { onStartCustomCbt(hardClinicalPool.shuffled().take(50), "PGIMER 50Q Clinical Sprint", 50) }
                    ),
                    CbtOptionData(
                        badge = "Express",
                        durationLabel = if (useTimeLimit) "$mini30Mins min" else "Untimed",
                        title = "Mini 30Q Core Nursing",
                        desc = "Quick daily PGIMER-pattern drill.",
                        btnLabel = "Start 30Q Mock",
                        isPrimary = false,
                        tag = "cbt_30q_btn",
                        onClick = { onStartStandardMock(30, null, "PGIMER Mini 30Q Nursing CBT", false) }
                    )
                )

                isNorcet && state.activeExamStage == ExamStage.MAINS -> listOf(
                    CbtOptionData(
                        badge = "4×45m Locked",
                        durationLabel = "180 min",
                        title = "NORCET Mains Full Mock (160 Qs)",
                        desc = "4 sequential sections × 40Q • 100% Clinical Nursing.",
                        btnLabel = "Start 160Q Mains",
                        isPrimary = true,
                        tag = "cbt_norcet_mains_btn",
                        onClick = { onStartNorcetMainsMock(160, 180, "NORCET Stage II Mains (160Q Clinical CBT)") }
                    ),
                    CbtOptionData(
                        badge = "1 Section",
                        durationLabel = "45 min",
                        title = "40Q Mains Clinical Section",
                        desc = "Single 45-minute time-locked clinical block.",
                        btnLabel = "Start 40Q Section",
                        isPrimary = false,
                        tag = "cbt_norcet_mains_sec_btn",
                        onClick = { onStartNorcetMainsMock(40, 45, "NORCET Mains 40Q Section (45m)") }
                    ),
                    CbtOptionData(
                        badge = "Clinical Cases",
                        durationLabel = "30 min",
                        title = "25Q Priority Clinical Drill",
                        desc = "Bedside triage, ABG, ICU & dosage scenarios.",
                        btnLabel = "Start 25Q Cases",
                        isPrimary = false,
                        tag = "cbt_norcet_mains_25_btn",
                        onClick = { onStartNorcetMainsMock(25, 30, "NORCET Mains 25Q Clinical Drill") }
                    )
                )

                isNorcet -> listOf(
                    CbtOptionData(
                        badge = "5×18m Locked",
                        durationLabel = "$fullMins min",
                        title = "NORCET Prelims Full Mock ($fullQ Qs)",
                        desc = "5 sequential sections × 20Q • 80 Nursing + 20 GK/Aptitude.",
                        btnLabel = "Start 100Q Prelims",
                        isPrimary = true,
                        tag = "cbt_100q_btn",
                        onClick = { onStartStandardMock(fullQ, fullMins, "NORCET Stage I Prelims (${fullQ}Q)", true) }
                    ),
                    CbtOptionData(
                        badge = "1 Section",
                        durationLabel = "18 min",
                        title = "20Q Prelims Nursing Section",
                        desc = "Single 18-minute Stage I pacing block.",
                        btnLabel = "Start 20Q Section",
                        isPrimary = false,
                        tag = "cbt_norcet_prelims_sec_btn",
                        onClick = { onStartCustomCbt(nurPool.shuffled().take(20), "NORCET Prelims 20Q Section (18m)", 18) }
                    ),
                    CbtOptionData(
                        badge = "80:20 Express",
                        durationLabel = if (useTimeLimit) "$mini30Mins min" else "Untimed",
                        title = "Mini 30Q Prelims Drill",
                        desc = "24 Nursing + 6 GK/Aptitude speed drill.",
                        btnLabel = "Start 30Q Drill",
                        isPrimary = false,
                        tag = "cbt_30q_btn",
                        onClick = { onStartStandardMock(30, null, "NORCET Mini 30Q Prelims Drill", false) }
                    ),
                    CbtOptionData(
                        badge = "Prelims 20%",
                        durationLabel = if (useTimeLimit) "18 min" else "Untimed",
                        title = "20Q GK & Aptitude Section",
                        desc = "Dedicated Stage I non-nursing section.",
                        btnLabel = "Start 20Q GK Block",
                        isPrimary = false,
                        tag = "cbt_30q_gen_btn",
                        onClick = { onStartCustomCbt(nonPool.shuffled().take(20), "20Q NORCET Prelims GK & Aptitude", 18) }
                    )
                )

                else -> listOf(
                    CbtOptionData(
                        badge = "Official Pattern",
                        durationLabel = "$fullMins min",
                        title = "Full $fullQ-Question Mock",
                        desc = "70% Nursing + 30% Non-Tech & CA • -$negMarkLabel marking.",
                        btnLabel = "Start Full Mock",
                        isPrimary = true,
                        tag = "cbt_100q_btn",
                        onClick = { onStartStandardMock(fullQ, fullMins, "Full Official ${fullQ}Q CBT Mock", true) }
                    ),
                    CbtOptionData(
                        badge = "Express",
                        durationLabel = if (useTimeLimit) "$mini30Mins min" else "Untimed",
                        title = "Mini 30Q Mock",
                        desc = "21 Nursing + 9 Non-Tech/CA daily drill.",
                        btnLabel = "Start 30Q Mock",
                        isPrimary = false,
                        tag = "cbt_30q_btn",
                        onClick = { onStartStandardMock(30, null, "Mini 30Q CBT Mock", false) }
                    ),
                    CbtOptionData(
                        badge = "Clinical Speed",
                        durationLabel = if (useTimeLimit) "$nur50Mins min" else "Untimed",
                        title = "50Q Nursing Speed CBT",
                        desc = "All 9 core clinical nursing subjects.",
                        btnLabel = "Start 50Q Nursing",
                        isPrimary = false,
                        tag = "cbt_50q_nur_btn",
                        onClick = { onStartCustomCbt(nurPool.shuffled().take(50), "50Q Nursing CBT", null) }
                    ),
                    CbtOptionData(
                        badge = "Non-Tech + CA",
                        durationLabel = if (useTimeLimit) "$non30Mins min" else "Untimed",
                        title = "30Q Non-Tech & CA",
                        desc = "Current Affairs, GK, Science, Math & Reasoning.",
                        btnLabel = "Start 30Q Non-Tech",
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (opt.isPrimary) editorial.acidLime else editorial.elevatedSurface
                                    ) {
                                        Text(
                                            text = opt.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (opt.isPrimary) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = opt.durationLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            .heightIn(min = 46.dp)
                                            .testTag(opt.tag),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(opt.btnLabel, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = opt.onClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                            .testTag(opt.tag),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, editorial.borderSubtle)
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
                        text = "Recent Scores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(state.mockHistory) { h ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(h.timestamp))
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
                                Text(h.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$dateStr · ✓ ${h.correctCount}  ✕ ${h.wrongCount}  – ${h.skippedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f / %d", h.netScore, h.totalQuestions),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${h.accuracyPercent}% acc",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = if (h.accuracyPercent >= 70) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant
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
    val btnLabel: String,
    val isPrimary: Boolean,
    val tag: String,
    val onClick: () -> Unit
)
