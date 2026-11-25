package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyFocusProgress
import com.example.data.model.FocusTimerLogic
import com.example.data.model.FocusTimerPreset
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import kotlinx.coroutines.delay

@Composable
fun FocusStudyTimerCard(
    dailyProgress: DailyFocusProgress,
    onSessionCompleted: (FocusTimerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    var activePreset by remember { mutableStateOf(FocusTimerPreset.POMODORO_25) }
    var remainingSeconds by remember(activePreset) { mutableIntStateOf(activePreset.totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var justCompletedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRunning, activePreset) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
            if (remainingSeconds == 0) {
                isRunning = false
                onSessionCompleted(activePreset)
                justCompletedMessage = if (activePreset.isBreak) {
                    "Recharge break complete — ready for your next revision sprint!"
                } else {
                    "${activePreset.label} complete! +${activePreset.durationMinutes} min logged to today's focus total."
                }
            }
        }
    }

    val totalSecs = activePreset.totalSeconds.coerceAtLeast(1)
    val elapsedProgress = ((totalSecs - remainingSeconds).toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
    val todayMinutes = dailyProgress.effectiveMinutes()
    val todaySessions = dailyProgress.effectiveSessions()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today's focus summary strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.cardSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "TODAY'S FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$todayMinutes min",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.cardSurface,
                border = BorderStroke(1.dp, editorial.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SPRINTS DONE",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$todaySessions",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Preset selector pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusTimerPreset.entries.forEach { preset ->
                MonochromeFilterPill(
                    label = preset.label,
                    selected = activePreset == preset,
                    onClick = {
                        isRunning = false
                        activePreset = preset
                        remainingSeconds = preset.totalSeconds
                        justCompletedMessage = null
                    },
                    modifier = Modifier.testTag("focus_preset_${preset.name.lowercase()}")
                )
            }
        }

        // Main Timer Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
            border = BorderStroke(
                width = if (isRunning) 1.5.dp else 1.dp,
                color = if (isRunning) editorial.acidLime else editorial.borderSubtle
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = activePreset.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = FocusTimerLogic.formatClock(remainingSeconds),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 52.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("focus_timer_clock_text")
                )

                AcidLimeProgressBar(
                    progress = elapsedProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                if (justCompletedMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.correctBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = editorial.correctText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = justCompletedMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = editorial.correctText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (remainingSeconds == 0) {
                                remainingSeconds = activePreset.totalSeconds
                            }
                            justCompletedMessage = null
                            isRunning = !isRunning
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = editorial.acidLime,
                            contentColor = editorial.onAcidLime
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("focus_timer_start_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = if (isRunning) "Pause" else if (remainingSeconds < activePreset.totalSeconds && remainingSeconds > 0) "Resume" else "Start Sprint",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            remainingSeconds = activePreset.totalSeconds
                            justCompletedMessage = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        modifier = Modifier
                            .heightIn(min = 50.dp)
                            .testTag("focus_timer_reset_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
