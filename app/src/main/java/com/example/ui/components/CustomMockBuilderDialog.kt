package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomMockBuilderLogic
import com.example.data.model.CustomMockConfig
import com.example.data.model.CustomMockSourceFilter
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomMockBuilderDialog(
    availableQuestions: List<QuestionItem>,
    defaultSecondsPerQuestion: Int,
    defaultNegativePenalty: Double,
    onDismiss: () -> Unit,
    onLaunchCustomMock: (items: List<QuestionItem>, title: String, hasCountdown: Boolean, durationMinutes: Int?, negativeMarks: Double) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val presentSubjects = remember(availableQuestions) {
        val counts = availableQuestions.groupingBy { it.question.subject }.eachCount()
        SubjectCatalog.ALL_SUBJS.filter { (counts[it.code] ?: 0) > 0 }
    }

    var config by remember {
        mutableStateOf(
            CustomMockConfig(
                questionCount = 25,
                selectedSubjectCodes = emptySet(),
                sourceFilter = CustomMockSourceFilter.ALL_BANK,
                includeIbqs = true,
                timedMode = true,
                durationMinutes = CustomMockBuilderLogic.suggestedMinutesFor(25, defaultSecondsPerQuestion),
                enableNegativeMarking = true,
                negativeMarkPenalty = defaultNegativePenalty.coerceIn(0.0, 1.0)
            )
        )
    }

    val validation = remember(availableQuestions, config) {
        CustomMockBuilderLogic.validate(availableQuestions, config)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, editorial.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("custom_mock_builder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = editorial.acidLime,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Custom Mock Test Builder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure count, subjects, timer & negative marking",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("custom_mock_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Custom Mock Builder"
                        )
                    }
                }

                // Live Pool Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (validation.canStart) editorial.elevatedSurface else editorial.wrongBg,
                    border = BorderStroke(
                        1.dp,
                        if (validation.canStart) editorial.acidLime.copy(alpha = 0.55f) else editorial.wrongText.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validation.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (validation.canStart) MaterialTheme.colorScheme.onSurface else editorial.wrongText,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // 1. Question Count Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUESTION COUNT",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomMockBuilderLogic.COUNT_PRESETS.forEach { count ->
                            MonochromeFilterPill(
                                label = "${count}Q",
                                selected = config.questionCount == count,
                                onClick = {
                                    config = config.copy(
                                        questionCount = count,
                                        durationMinutes = CustomMockBuilderLogic.suggestedMinutesFor(count, defaultSecondsPerQuestion)
                                    )
                                },
                                modifier = Modifier.testTag("custom_mock_count_$count")
                            )
                        }
                    }
                }

                // 2. Source Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUESTION POOL FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomMockSourceFilter.entries.forEach { filter ->
                            MonochromeFilterPill(
                                label = filter.label,
                                selected = config.sourceFilter == filter,
                                onClick = { config = config.copy(sourceFilter = filter) },
                                modifier = Modifier.testTag("custom_mock_src_${filter.name.lowercase(Locale.US)}")
                            )
                        }
                    }
                }

                // 3. Multi-Select Subjects
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUBJECTS (MULTI-SELECT)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (config.selectedSubjectCodes.isEmpty()) "All Selected" else "${config.selectedSubjectCodes.size} Selected",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = editorial.acidLime,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonochromeFilterPill(
                            label = "All",
                            selected = config.selectedSubjectCodes.isEmpty(),
                            onClick = { config = config.copy(selectedSubjectCodes = emptySet()) },
                            modifier = Modifier.testTag("custom_mock_subj_all")
                        )
                        presentSubjects.forEach { subj ->
                            val selected = subj.code in config.selectedSubjectCodes
                            MonochromeFilterPill(
                                label = subj.code,
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        config.selectedSubjectCodes - subj.code
                                    } else {
                                        config.selectedSubjectCodes + subj.code
                                    }
                                    config = config.copy(selectedSubjectCodes = next)
                                },
                                modifier = Modifier.testTag("custom_mock_subj_${subj.code}")
                            )
                        }
                    }
                }

                HorizontalDivider(color = editorial.borderSubtle)

                // 4. Toggles: Include IBQs, Countdown Timer, Negative Marking
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomMockToggleRow(
                        title = "Include Visual IBQs",
                        subtitle = "Mix ECG, CTG, CXR & instrument spotters into this CBT",
                        checked = config.includeIbqs,
                        onCheckedChange = { config = config.copy(includeIbqs = it) },
                        testTag = "custom_mock_toggle_ibq"
                    )

                    CustomMockToggleRow(
                        title = "Countdown Timer",
                        subtitle = if (config.timedMode) "${config.durationMinutes} minutes exam timer" else "Untimed self-paced CBT",
                        checked = config.timedMode,
                        onCheckedChange = { config = config.copy(timedMode = it) },
                        testTag = "custom_mock_toggle_timer"
                    )

                    if (config.timedMode) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = editorial.elevatedSurface,
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Duration",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            config = config.copy(
                                                durationMinutes = (config.durationMinutes - 5).coerceAtLeast(5)
                                            )
                                        },
                                        modifier = Modifier.testTag("custom_mock_time_minus")
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease duration by 5 minutes")
                                    }
                                    Text(
                                        text = "${config.durationMinutes} min",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            config = config.copy(
                                                durationMinutes = (config.durationMinutes + 5).coerceAtMost(240)
                                            )
                                        },
                                        modifier = Modifier.testTag("custom_mock_time_plus")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase duration by 5 minutes")
                                    }
                                }
                            }
                        }
                    }

                    CustomMockToggleRow(
                        title = "Negative Marking",
                        subtitle = if (config.enableNegativeMarking) {
                            String.format(Locale.US, "+1 / -%.2f per wrong answer", config.negativeMarkPenalty)
                        } else {
                            "+1 / 0.00 (Practice scoring, no negative marks)"
                        },
                        checked = config.enableNegativeMarking,
                        onCheckedChange = { config = config.copy(enableNegativeMarking = it) },
                        testTag = "custom_mock_toggle_neg"
                    )
                }

                // Launch CTA
                Button(
                    onClick = {
                        val selectedItems = CustomMockBuilderLogic.buildMockSessionQuestions(
                            allQuestions = availableQuestions,
                            config = config,
                            shuffle = true
                        )
                        if (selectedItems.isNotEmpty()) {
                            val title = CustomMockBuilderLogic.buildTitle(config, selectedItems.size)
                            onLaunchCustomMock(
                                selectedItems,
                                title,
                                config.timedMode,
                                if (config.timedMode) config.durationMinutes else null,
                                config.effectiveNegativeMarks
                            )
                            onDismiss()
                        }
                    },
                    enabled = validation.canStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = editorial.acidLime,
                        contentColor = editorial.onAcidLime
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("custom_mock_launch_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (validation.canStart) {
                            "Launch Custom ${validation.effectiveCount}Q CBT"
                        } else {
                            "Adjust Filters to Launch"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomMockToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val editorial = MaterialTheme.editorialColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = editorial.onAcidLime,
                checkedTrackColor = editorial.acidLime
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
