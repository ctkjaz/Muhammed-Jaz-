package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.example.data.model.DifficultyFilter
import com.example.data.model.Flashcard
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectMeta
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.FlashcardItemCard
import com.example.ui.components.SubjectProgressCard
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.SubjectHubSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsHubScreen(
    state: DashboardDerivedState,
    activeSection: SubjectHubSection,
    difficultyFilter: DifficultyFilter,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onSelectSection: (SubjectHubSection) -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartTimedConfig: (String) -> Unit,
    onStartCbtCustom: (List<QuestionItem>, String) -> Unit,
    onOpenImporter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = SubjectHubSection.entries
    val filteredPool = remember(state.questions, difficultyFilter) {
        difficultyFilter.difficulty?.let { d ->
            state.questions.filter { it.question.difficulty == d }
        } ?: state.questions
    }
    val diffSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = sections.indexOf(activeSection),
            edgePadding = 12.dp
        ) {
            sections.forEach { sec ->
                Tab(
                    selected = sec == activeSection,
                    onClick = { onSelectSection(sec) },
                    text = { Text(sec.label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("subject_tab_${sec.name}")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Difficulty:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DifficultyFilterBar(
                selected = difficultyFilter,
                onSelect = onSelectDifficulty,
                testTagPrefix = "hub_diff",
                modifier = Modifier.weight(1f)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth >= 840.dp -> 3
                maxWidth >= 560.dp -> 2
                else -> 1
            }

            when (activeSection) {
                SubjectHubSection.NURSING -> SubjectListGrid(
                    title = "Subject-Wise Nursing (70% Weightage)",
                    subtitle = "Core clinical & nursing subjects aligned with INC & RRB syllabus",
                    subjects = SubjectCatalog.NURSING_SUBJS,
                    allQuestions = filteredPool,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    allFilter = { SubjectCatalog.isNursing(it.question) },
                    onStartPractice = onStartPractice,
                    onStartTimedConfig = onStartTimedConfig
                )

                SubjectHubSection.PYQ -> PyqShiftList(
                    allQuestions = filteredPool,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onStartCbtCustom = onStartCbtCustom,
                    onOpenImporter = onOpenImporter
                )

                SubjectHubSection.CA_GK -> CaAndFlashcardsList(
                    allQuestions = filteredPool,
                    diffSuffix = diffSuffix,
                    flashcards = state.flashcards,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onStartTimedConfig = onStartTimedConfig
                )

                SubjectHubSection.NON_TECH -> SubjectListGrid(
                    title = "Science, Arithmetic & Reasoning (30%)",
                    subtitle = "NCERT Science, clinical dosage & aptitude math, and logical reasoning",
                    subjects = SubjectCatalog.GEN_SUBJS,
                    allQuestions = filteredPool,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    allFilter = { SubjectCatalog.isGen(it.question) },
                    onStartPractice = onStartPractice,
                    onStartTimedConfig = onStartTimedConfig
                )
            }
        }
    }
}

@Composable
private fun SubjectListGrid(
    title: String,
    subtitle: String,
    subjects: List<SubjectMeta>,
    allQuestions: List<QuestionItem>,
    diffSuffix: String,
    columns: Int,
    allFilter: (QuestionItem) -> Boolean,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartTimedConfig: (String) -> Unit
) {
    val combined = remember(allQuestions, subjects) { allQuestions.filter(allFilter) }
    val rows = subjects.chunked(columns)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onStartPractice(combined, "$title$diffSuffix — All") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("practice_all_${subjects.firstOrNull()?.code}")
                ) {
                    Text("All (${combined.size})")
                }
            }
        }

        items(rows.size) { rowIdx ->
            val rowItems = rows[rowIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { sub ->
                    val list = allQuestions.filter { it.question.subject == sub.code }
                    val mastered = list.count { it.lastOutcome == "right" }
                    val attempts = list.sumOf { it.attemptsCount }
                    val correct = list.sumOf { it.correctCount }
                    val acc = if (attempts > 0) ((correct.toFloat() / attempts) * 100f).toInt() else 0
                    SubjectProgressCard(
                        title = sub.name,
                        accentColor = sub.accentColor,
                        totalCount = list.size,
                        masteredCount = mastered,
                        accuracyPercent = acc,
                        attemptsCount = attempts,
                        onPractice = { onStartPractice(list, "${sub.name}$diffSuffix") },
                        onTimed = { onStartTimedConfig(sub.code) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PyqShiftList(
    allQuestions: List<QuestionItem>,
    diffSuffix: String,
    columns: Int,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartCbtCustom: (List<QuestionItem>, String) -> Unit,
    onOpenImporter: () -> Unit
) {
    val shifts = remember(allQuestions) {
        allQuestions.mapNotNull { it.question.shiftYear.takeIf { sy -> sy.isNotBlank() } }
            .distinct()
            .sortedDescending()
    }
    val allPyqs = remember(allQuestions) {
        allQuestions.filter { it.question.isPyqTagged }
    }
    val verifiedPyqCount = remember(allPyqs) { allPyqs.count { it.question.isVerifiedSource } }
    val unverifiedPyqCount = remember(allPyqs, verifiedPyqCount) { allPyqs.size - verifiedPyqCount }
    val rows = shifts.chunked(columns)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PYQ-Tagged Practice Sets", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Questions tagged by exam year and shift. Unverified tags have not yet been verified against official RRB question papers or answer keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldCorrect.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EmeraldCorrect.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("pyq_verified_count")
                    ) {
                        Text(
                            text = "Verified: $verifiedPyqCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = EmeraldCorrect,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberHighlight.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AmberHighlight.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("pyq_unverified_count")
                    ) {
                        Text(
                            text = "Unverified: $unverifiedPyqCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = AmberHighlight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStartPractice(allPyqs, "All PYQ-Tagged Sets$diffSuffix") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("practice_all_pyqs")
                    ) {
                        Text("Practice All Tagged (${allPyqs.size})")
                    }
                    OutlinedButton(
                        onClick = onOpenImporter,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("pyq_add_pdf_btn")
                    ) {
                        Text("+ Add Shift PDF")
                    }
                }
            }
        }

        items(rows.size) { rowIdx ->
            val rowItems = rows[rowIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { sh ->
                    val list = allQuestions.filter { it.question.shiftYear == sh }
                    val solved = list.count { it.attemptsCount > 0 }
                    val shiftVerified = list.count { it.question.isVerifiedSource }
                    val shiftUnverified = list.size - shiftVerified
                    val allVerified = list.isNotEmpty() && shiftUnverified == 0
                    val shiftBadgeText = if (allVerified) "Verified PYQ • $sh" else "Unverified PYQ tag"
                    val shiftBadgeColor = if (allVerified) EmeraldCorrect else AmberHighlight
                    val title = SubjectCatalog.SHIFT_NAMES[sh] ?: "Tagged Set $sh"
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
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = sh,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = shiftBadgeColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, shiftBadgeColor.copy(alpha = 0.45f)),
                                        modifier = Modifier.testTag("shift_badge_$sh")
                                    ) {
                                        Text(
                                            text = shiftBadgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = shiftBadgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$solved/${list.size} done",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Verified: $shiftVerified • Unverified: $shiftUnverified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onStartPractice(list, "$title$diffSuffix") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Practice", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { onStartCbtCustom(list, "$sh Timed CBT$diffSuffix") },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("CBT", style = MaterialTheme.typography.labelMedium)
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
    }
}

@Composable
private fun CaAndFlashcardsList(
    allQuestions: List<QuestionItem>,
    diffSuffix: String,
    flashcards: List<Flashcard>,
    columns: Int,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartTimedConfig: (String) -> Unit
) {
    val allCa = remember(allQuestions) { allQuestions.filter { SubjectCatalog.isCa(it.question) } }
    val tags = remember(flashcards) { listOf("All") + flashcards.map { it.tag }.distinct() }
    var selectedTag by remember { mutableStateOf("All") }
    val visibleCards = remember(flashcards, selectedTag) {
        if (selectedTag == "All") flashcards else flashcards.filter { it.tag == selectedTag }
    }
    val caRows = SubjectCatalog.CA_SUBJS.chunked(columns)
    val cardRows = visibleCards.chunked(columns)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current Affairs (2025–26) & Static GK", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Union Budget 2026–27, Kavach 4.0, HPV Drive, Awards & Polity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onStartPractice(allCa, "Current Affairs & GK Complete$diffSuffix") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("All (${allCa.size})")
                }
            }
        }

        items(caRows.size) { rowIdx ->
            val rowItems = caRows[rowIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { sub ->
                    val list = allQuestions.filter { it.question.subject == sub.code }
                    val mastered = list.count { it.lastOutcome == "right" }
                    val attempts = list.sumOf { it.attemptsCount }
                    val correct = list.sumOf { it.correctCount }
                    val acc = if (attempts > 0) ((correct.toFloat() / attempts) * 100f).toInt() else 0
                    SubjectProgressCard(
                        title = sub.name,
                        accentColor = sub.accentColor,
                        totalCount = list.size,
                        masteredCount = mastered,
                        accuracyPercent = acc,
                        attemptsCount = attempts,
                        onPractice = { onStartPractice(list, "${sub.name}$diffSuffix") },
                        onTimed = { onStartTimedConfig(sub.code) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    "Rapid-Revision Fact Cards (${flashcards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { t ->
                        FilterChip(
                            selected = selectedTag == t,
                            onClick = { selectedTag = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        items(cardRows.size) { rowIdx ->
            val rowItems = cardRows[rowIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { card ->
                    FlashcardItemCard(card = card, modifier = Modifier.weight(1f))
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
