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
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectMeta
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.FlashcardItemCard
import com.example.ui.components.SubjectProgressCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
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
    val activeExamId = state.activeExam?.id
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExamId)
    val isPgimer = PlatformCatalogDefaults.isPgimerExam(activeExamId)
    val sections = if (isPgimer) {
        listOf(SubjectHubSection.NURSING, SubjectHubSection.PYQ)
    } else {
        SubjectHubSection.entries
    }
    val effectiveSection = if (activeSection in sections) activeSection else SubjectHubSection.NURSING
    val filteredPool = remember(state.questions, difficultyFilter) {
        difficultyFilter.difficulty?.let { d ->
            state.questions.filter { it.question.difficulty == d }
        } ?: state.questions
    }
    val diffSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = sections.indexOf(effectiveSection).coerceAtLeast(0),
            edgePadding = 12.dp
        ) {
            sections.forEach { sec ->
                Tab(
                    selected = sec == effectiveSection,
                    onClick = { onSelectSection(sec) },
                    text = { Text(sec.labelFor(activeExamId), fontWeight = FontWeight.SemiBold) },
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

            when (effectiveSection) {
                SubjectHubSection.NURSING -> SubjectListGrid(
                    title = when {
                        isPgimer -> "PGIMER Core Nursing (100% Weightage)"
                        isNorcet -> "Nursing Core & Mains Clinical (80% / 100%)"
                        else -> "Subject-Wise Nursing (70% Weightage)"
                    },
                    subtitle = when {
                        isPgimer -> "All 9 clinical & foundational nursing subjects for PGIMER Chandigarh Nursing Officer CBT (+1 / -0.25)"
                        isNorcet -> "Core clinical nursing subjects & scenario-based priority questions for AIIMS NORCET"
                        else -> "Core clinical & nursing subjects aligned with INC & RRB syllabus"
                    },
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
                    isNorcet = isNorcet,
                    isPgimer = isPgimer,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onStartCbtCustom = onStartCbtCustom,
                    onOpenImporter = onOpenImporter
                )

                SubjectHubSection.CA_GK -> CaAndFlashcardsList(
                    allQuestions = filteredPool,
                    isNorcet = isNorcet,
                    diffSuffix = diffSuffix,
                    flashcards = state.flashcards,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onStartTimedConfig = onStartTimedConfig
                )

                SubjectHubSection.NON_TECH -> SubjectListGrid(
                    title = if (isNorcet) {
                        "Aptitude, Dosage Math & Science (20%)"
                    } else {
                        "Science, Arithmetic & Reasoning (30%)"
                    },
                    subtitle = "Dosage calculations, quantitative aptitude, logical reasoning & general science",
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
    isNorcet: Boolean,
    isPgimer: Boolean,
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
                Text(
                    text = when {
                        isPgimer -> "PGIMER Chandigarh Memory-Based Papers"
                        isNorcet -> "AIIMS NORCET Memory-Based Papers"
                        else -> "Previous Year Papers"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = when {
                        isPgimer -> "PGIMER Chandigarh 2020–2025 Nursing Officer recall sets (+1 / -0.25 pattern). PGIMER does not release official CBT question papers, so recalled sets are labeled Memory-Based."
                        isNorcet -> "NORCET 5–10 Prelims & Mains recall sets. AIIMS does not publish official CBT keys, so recalled papers are labeled Memory-Based."
                        else -> "Shift-wise exam sets (2015–2026). Memory-based papers are clearly marked alongside official papers."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val editorial = MaterialTheme.editorialColors
                if (verifiedPyqCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = editorial.correctText.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, editorial.correctText.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("pyq_verified_count")
                        ) {
                            Text(
                                text = "Official PYQ: $verifiedPyqCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = editorial.correctText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        if (unverifiedPyqCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = editorial.warningText.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, editorial.warningText.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("pyq_unverified_count")
                            ) {
                                Text(
                                    text = "Memory-Based: $unverifiedPyqCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = editorial.warningText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStartPractice(allPyqs, "Previous Year Papers$diffSuffix") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("practice_all_pyqs")
                    ) {
                        Text("Practice All Papers")
                    }
                    OutlinedButton(
                        onClick = onOpenImporter,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("pyq_add_pdf_btn")
                    ) {
                        Text("+ Add Paper")
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
                    val editorial = MaterialTheme.editorialColors
                    val shiftBadgeText = if (allVerified) "Official PYQ" else "Memory-Based"
                    val shiftBadgeColor = if (allVerified) editorial.correctText else editorial.warningText
                    val title = SubjectCatalog.SHIFT_NAMES[sh] ?: "Exam Set $sh"
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
                                        color = shiftBadgeColor.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, shiftBadgeColor.copy(alpha = 0.35f)),
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
    isNorcet: Boolean,
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
    val visibleCaSubjs = remember(isNorcet) {
        if (isNorcet) {
            SubjectCatalog.CA_SUBJS.filter { PlatformCatalogDefaults.isNorcetSharedSubject(it.code) }
        } else {
            SubjectCatalog.CA_SUBJS
        }
    }
    val caRows = visibleCaSubjs.chunked(columns)
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
                    Text(
                        text = if (isNorcet) "General Knowledge & Health Programs" else "Current Affairs (2025–26) & Static GK",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isNorcet) {
                            "National health missions (LaQshya, SUMAN, NTEP, UIP), Static GK & 2025–26 awareness"
                        } else {
                            "Union Budget 2026–27, Kavach 4.0, HPV Drive, Awards & Polity"
                        },
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
