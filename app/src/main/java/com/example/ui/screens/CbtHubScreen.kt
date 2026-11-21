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
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
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
    onStartCustomCbt: (List<QuestionItem>, String, Int?) -> Unit,
    onOpenTimedPracticeConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val all = state.questions
    val nurPool = all.filter { SubjectCatalog.isNursing(it.question) }
    val nonPool = all.filter { !SubjectCatalog.isNursing(it.question) }
    val primaryColor = MaterialTheme.colorScheme.primary
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
                    Text("RRB CBT & Timed Practice Hub", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "TCS iON-style palette, countdown timer, +1 / -0.33 negative marking & configurable timed drills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            val cbtOptions = listOf(
                CbtOptionData(
                    badge = "Official Pattern",
                    durationLabel = "90 Minutes",
                    title = "Full 100-Question CBT Mock",
                    desc = "70 Nursing + 30 Non-Tech (Science, Math, Reasoning & Current Affairs/GK). Negative marking: -0.33.",
                    badgeColor = primaryColor,
                    btnLabel = "Start Full Mock (100 Qs)",
                    isPrimary = true,
                    tag = "cbt_100q_btn",
                    onClick = { onStartStandardMock(100, 90, "Full Official 100Q CBT Mock", true) }
                ),
                CbtOptionData(
                    badge = "Express Drill",
                    durationLabel = if (useTimeLimit) "$mini30Mins Minutes" else "Untimed",
                    title = "Mini 30-Question CBT Mock",
                    desc = "Exact 70:30 ratio (21 Nursing + 9 Non-Tech/CA). Ideal for quick daily practice.",
                    badgeColor = EmeraldCorrect,
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
                    badgeColor = Color(0xFFE11D48),
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
                    badgeColor = Color(0xFF0891B2),
                    btnLabel = "Start 30Q Non-Tech CBT",
                    isPrimary = false,
                    tag = "cbt_30q_gen_btn",
                    onClick = { onStartCustomCbt(nonPool.shuffled().take(30), "30Q Non-Tech CBT", null) }
                )
            )

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
                                    color = EmeraldCorrect
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
