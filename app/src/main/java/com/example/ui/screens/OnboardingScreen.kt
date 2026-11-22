package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsPreferences
import com.example.data.model.AccountSessionLogic
import com.example.data.model.Exam
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.StudyGoalLogic
import com.example.data.model.SubjectCatalog
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    availableExams: List<Exam>,
    initialExamId: String,
    initialName: String,
    initialRole: String,
    initialTargetYear: String,
    initialFocusSubject: String,
    initialPhoneNumber: String,
    initialDailyGoal: Int,
    secondsPerQuestion: Int,
    onComplete: (
        examId: String,
        name: String,
        role: String,
        targetYear: String,
        focusSubject: String,
        phoneNumber: String,
        dailyGoalQuestions: Int
    ) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exams = remember(availableExams) {
        availableExams.ifEmpty { PlatformCatalogDefaults.DEFAULT_EXAMS }
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedExamId by rememberSaveable {
        mutableStateOf(initialExamId.ifBlank { PlatformCatalogDefaults.EXAM_RRB_NURSING })
    }
    var displayName by rememberSaveable { mutableStateOf(initialName.take(SettingsPreferences.MAX_PROFILE_NAME_LENGTH)) }
    var role by rememberSaveable { mutableStateOf(initialRole.take(SettingsPreferences.MAX_PROFILE_ROLE_LENGTH)) }
    var targetYear by rememberSaveable {
        mutableStateOf(initialTargetYear.ifBlank { SettingsPreferences.DEFAULT_TARGET_YEAR })
    }
    var focusSubject by rememberSaveable { mutableStateOf(initialFocusSubject) }
    var phoneNumber by rememberSaveable {
        mutableStateOf(initialPhoneNumber.take(AccountSessionLogic.MAX_PHONE_LENGTH))
    }
    var dailyGoal by rememberSaveable {
        mutableIntStateOf(
            initialDailyGoal.coerceIn(StudyGoalLogic.MIN_DAILY_GOAL, StudyGoalLogic.MAX_DAILY_GOAL)
        )
    }

    val editorial = MaterialTheme.editorialColors
    val totalSteps = 3

    BackHandler(enabled = step > 0) {
        step = (step - 1).coerceAtLeast(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = { step -= 1 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("onboarding_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous step",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                if (step < totalSteps - 1) {
                                    step += 1
                                } else {
                                    onComplete(
                                        selectedExamId,
                                        displayName.trim(),
                                        role.trim(),
                                        targetYear.trim().ifBlank { SettingsPreferences.DEFAULT_TARGET_YEAR },
                                        focusSubject,
                                        AccountSessionLogic.sanitizePhoneNumber(phoneNumber),
                                        dailyGoal
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("onboarding_next_btn")
                        ) {
                            Text(
                                text = if (step < totalSteps - 1) "Continue" else "Start Studying",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Brand + Step Progress Bar + Skip
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.MonitorHeart,
                                        contentDescription = "ClinEdge",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "ClinEdge Setup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Step ${step + 1} of $totalSteps",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_skip_btn")
                        ) {
                            Text("Skip", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            val filled = index <= step
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (filled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                        }
                                    )
                            )
                        }
                    }
                }

                // Animated Step Body
                AnimatedContent(
                    targetState = step,
                    label = "onboarding_step_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { activeStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeStep) {
                            0 -> {
                                OnboardingStepHeader(
                                    badge = "EXAM TRACK",
                                    title = "Which exam are you preparing for?",
                                    subtitle = "We'll tailor your dashboard, PYQ papers, and CBT mocks to match. You can switch tracks anytime."
                                )

                                exams.forEach { exam ->
                                    val isSelected = selectedExamId == exam.id
                                    val isNorcet = PlatformCatalogDefaults.isNorcetExam(exam.id)
                                    val isPgimer = PlatformCatalogDefaults.isPgimerExam(exam.id)
                                    val patternLine = when {
                                        isPgimer -> "100% Core Nursing • 100Q / 100m • +1 / -0.25 Marking"
                                        isNorcet -> "80:20 Prelims & 160Q Mains • +1 / -0.33 Marking"
                                        else -> "70:30 Nursing & General • 100Q / 90 min CBT"
                                    }
                                    val highlightTag = when {
                                        isPgimer -> "PGIMER 2020–25 PYQs"
                                        isNorcet -> "NORCET 5–10 PYQs"
                                        else -> "2015–2026 Shifts"
                                    }

                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { selectedExamId = exam.id }
                                            .testTag("onboarding_exam_${exam.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isNorcet) Icons.Filled.MedicalServices else Icons.Filled.School,
                                                        contentDescription = null,
                                                        tint = if (isSelected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.primary
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = exam.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = patternLine,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = "1,000+ Qs",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = editorial.warningText.copy(alpha = 0.14f)
                                                    ) {
                                                        Text(
                                                            text = highlightTag,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = editorial.warningText,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                OnboardingStepHeader(
                                    badge = "LEARNER PROFILE",
                                    title = "Personalize your study focus",
                                    subtitle = "Optional details to tailor your profile and one-tap daily focus drills."
                                )

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = displayName,
                                            onValueChange = {
                                                displayName = it.take(SettingsPreferences.MAX_PROFILE_NAME_LENGTH)
                                            },
                                            label = { Text("Your Name") },
                                            placeholder = { Text("e.g. Ananya Sharma") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("onboarding_name_input")
                                        )

                                        val phoneInvalid = phoneNumber.isNotBlank() &&
                                            !AccountSessionLogic.isPhoneNumberValidOrEmpty(phoneNumber)
                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { raw ->
                                                phoneNumber = raw
                                                    .filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                                                    .take(AccountSessionLogic.MAX_PHONE_LENGTH)
                                            },
                                            label = { Text("Mobile Number (Optional)") },
                                            placeholder = { Text("e.g. +91 98765 43210") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Phone,
                                                    contentDescription = null
                                                )
                                            },
                                            isError = phoneInvalid,
                                            supportingText = {
                                                Text(
                                                    text = if (phoneInvalid) {
                                                        "Enter a valid 10-digit mobile number or leave blank."
                                                    } else {
                                                        "Optional • Saved privately to your profile (no OTP required)."
                                                    }
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("onboarding_phone_input")
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Qualification / Role",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val roles = listOf("B.Sc. Nursing", "GNM", "Post Basic B.Sc.", "Staff Nurse")
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                roles.forEach { itemRole ->
                                                    FilterChip(
                                                        selected = role.equals(itemRole, ignoreCase = true),
                                                        onClick = { role = itemRole },
                                                        label = { Text(itemRole) },
                                                        modifier = Modifier.testTag("onboarding_role_${itemRole.replace(" ", "_")}")
                                                    )
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Target Exam Year",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("2026", "2027", "2028").forEach { year ->
                                                    FilterChip(
                                                        selected = targetYear == year,
                                                        onClick = { targetYear = year },
                                                        label = {
                                                            Text(
                                                                text = year,
                                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily)
                                                            )
                                                        },
                                                        modifier = Modifier.testTag("onboarding_year_$year")
                                                    )
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Primary Focus Subject",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = focusSubject.isBlank(),
                                                    onClick = { focusSubject = "" },
                                                    label = { Text("All Subjects") },
                                                    modifier = Modifier.testTag("onboarding_focus_ALL")
                                                )
                                                SubjectCatalog.ALL_SUBJS.forEach { subj ->
                                                    FilterChip(
                                                        selected = focusSubject == subj.code,
                                                        onClick = { focusSubject = subj.code },
                                                        label = { Text(subj.code) },
                                                        modifier = Modifier.testTag("onboarding_focus_${subj.code}")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                OnboardingStepHeader(
                                    badge = "DAILY HABIT",
                                    title = "Set your daily question goal",
                                    subtitle = "Consistency beats cramming. Answer your daily target in Practice or CBT Mocks to build your study streak."
                                )

                                val presets = listOf(
                                    Triple(15, "Light Warmup", "Great for busy shift days"),
                                    Triple(25, "Balanced Daily (Recommended)", "Steady mastery across all subjects"),
                                    Triple(50, "Exam Sprint", "Serious preparation pace"),
                                    Triple(100, "Full Mock Pace", "High-intensity final revision")
                                )

                                presets.forEach { (count, title, subtitle) ->
                                    val isSelected = dailyGoal == count
                                    val estMins = SettingsPreferences.calculateSuggestedMinutes(count, secondsPerQuestion)
                                        .coerceAtLeast(5)
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { dailyGoal = count }
                                            .testTag("onboarding_goal_$count")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$count",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$subtitle • ~$estMins min/day",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = editorial.warningText.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, editorial.warningText.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = editorial.warningText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Your daily goal and study streak sync automatically with your Google account and can be adjusted anytime in Settings.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
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
private fun OnboardingStepHeader(
    badge: String,
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
