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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.model.ExamStage
import com.example.data.model.Flashcard
import com.example.data.model.PlatformCatalogDefaults
import com.example.data.model.PracticePathway
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.data.model.SubjectMeta
import com.example.data.model.SubjectPracticeLogic
import com.example.data.model.SyllabusCatalog
import com.example.data.subscription.SubscriptionUiState
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.FlashcardItemCard
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SubjectProgressCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.SubjectHubSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsHubScreen(
    state: DashboardDerivedState,
    practicePathway: PracticePathway,
    subjectExamFilter: String?,
    activeSection: SubjectHubSection,
    difficultyFilter: DifficultyFilter,
    onSelectPathway: (PracticePathway) -> Unit,
    onSelectSubjectExamFilter: (String?) -> Unit,
    onSelectExamStage: (ExamStage) -> Unit,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onSelectSection: (SubjectHubSection) -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onStartCrossExamSubjectPractice: (String) -> Unit,
    onOpenSubjectChapters: (String, Boolean, String?) -> Unit,
    onStartTimedConfig: (String, Boolean, String?) -> Unit,
    onStartCbtCustom: (List<QuestionItem>, String) -> Unit,
    onOpenImporter: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenIbqHub: () -> Unit = {},
    subscription: SubscriptionUiState = SubscriptionUiState()
) {
    val editorial = MaterialTheme.editorialColors
    val activeExamId = state.activeExam?.id
    val isNorcet = PlatformCatalogDefaults.isNorcetExam(activeExamId)
    val totalIbqCount = remember(state.stageQuestions, state.crossExamQuestions) {
        val active = state.stageQuestions.count { it.question.isImageBased }
        if (active > 0) active else state.crossExamQuestions.count { it.question.isImageBased }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Primary Pathway Switch: By Exam vs By Subject (Req 16)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SegmentedPillBar(
                options = PracticePathway.entries,
                selected = practicePathway,
                labelOf = { it.label },
                onSelect = onSelectPathway,
                testTagOf = { "hub_pathway_${it.name.lowercase()}" }
            )

            // When in By Exam mode and exam supports stages (NORCET), show strict Prelims | Mains switch
            if (practicePathway == PracticePathway.BY_EXAM && state.activeExamSupportsStages) {
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
                    testTagOf = { "hub_stage_${it.id}" }
                )
            }

            if (totalIbqCount > 0) {
                Surface(
                    onClick = onOpenIbqHub,
                    shape = RoundedCornerShape(14.dp),
                    color = editorial.cardSurface,
                    border = BorderStroke(1.dp, editorial.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subjects_ibq_hub_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                tint = editorial.acidLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Image-Based Questions Hub ($totalIbqCount)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ECG · Airway/OT · OBG/CTG · CXR · Devices",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "OPEN →",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = editorial.acidLime
                        )
                    }
                }
            }
        }

        when (practicePathway) {
            PracticePathway.BY_SUBJECT -> {
                val effectiveSubjectExamFilter = if (subscription.isExamLockedForSwitch(subjectExamFilter)) {
                    subscription.primaryUnlockedExamId ?: state.activeExam?.id
                } else {
                    subjectExamFilter
                }
                CrossExamSubjectPracticeView(
                    state = state,
                    subscription = subscription,
                    examFilter = effectiveSubjectExamFilter,
                    difficultyFilter = difficultyFilter,
                    onSelectExamFilter = onSelectSubjectExamFilter,
                    onSelectDifficulty = onSelectDifficulty,
                    onOpenSubjectChapters = { code -> onOpenSubjectChapters(code, true, effectiveSubjectExamFilter) },
                    onStartTimed = { code -> onStartTimedConfig(code, true, effectiveSubjectExamFilter) }
                )
            }

            PracticePathway.BY_EXAM -> {
                ExamScopedSubjectsView(
                    state = state,
                    activeExamId = activeExamId,
                    isNorcet = isNorcet,
                    isAdmin = subscription.isAdmin,
                    activeSection = activeSection,
                    difficultyFilter = difficultyFilter,
                    onSelectDifficulty = onSelectDifficulty,
                    onSelectSection = onSelectSection,
                    onStartPractice = onStartPractice,
                    onOpenSubjectChapters = { code -> onOpenSubjectChapters(code, false, null) },
                    onStartTimedConfig = { code -> onStartTimedConfig(code, false, null) },
                    onStartCbtCustom = onStartCbtCustom,
                    onOpenImporter = onOpenImporter
                )
            }
        }
    }
}

@Composable
private fun CrossExamSubjectPracticeView(
    state: DashboardDerivedState,
    subscription: SubscriptionUiState,
    examFilter: String?,
    difficultyFilter: DifficultyFilter,
    onSelectExamFilter: (String?) -> Unit,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onOpenSubjectChapters: (String) -> Unit,
    onStartTimed: (String) -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    var searchQuery by remember { mutableStateOf("") }

    val groupedSubjects = remember(
        state.crossExamQuestions,
        state.crossExamLinks,
        examFilter,
        difficultyFilter
    ) {
        SubjectPracticeLogic.groupQuestionsBySubjectForPractice(
            allQuestions = state.crossExamQuestions,
            allLinks = state.crossExamLinks,
            examIdFilter = examFilter,
            difficultyFilter = difficultyFilter,
            excludePyqAndMock = true,
            alreadyDeduplicated = true,
            includeVirtualShared = true
        )
    }

    val visibleSubjects = remember(groupedSubjects, searchQuery) {
        val qTrim = searchQuery.trim().lowercase()
        if (qTrim.isEmpty()) {
            groupedSubjects
        } else {
            groupedSubjects.filter { (meta, _) ->
                meta.name.lowercase().contains(qTrim) || meta.code.lowercase().contains(qTrim)
            }
        }
    }

    val orderedExams = remember(state.availableExams, state.activeExam?.id, subscription.unlockedExamIds, subscription.isSingleExamLocked) {
        val activeId = state.activeExam?.id
        state.availableExams.sortedBy { exam ->
            when {
                subscription.isSingleExamLocked && !subscription.isExamLockedForSwitch(exam.id) -> 0
                exam.id == activeId -> 1
                else -> 2
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 3
            maxWidth >= 560.dp -> 2
            else -> 1
        }
        val rows = visibleSubjects.chunked(columns)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search subjects...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = editorial.borderSubtle,
                            focusedContainerColor = editorial.cardSurface,
                            unfocusedContainerColor = editorial.cardSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_search_input")
                    )

                    // Exam Filter (locked exams redirect Single-Exam subscribers to Plans & Access)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allExamsLocked = subscription.isExamLockedForSwitch(null)
                        MonochromeFilterPill(
                            label = if (allExamsLocked) "🔒 All Exams" else "All Exams",
                            selected = examFilter == null && !allExamsLocked,
                            onClick = { onSelectExamFilter(null) },
                            modifier = Modifier.testTag("subj_exam_filter_all")
                        )
                        orderedExams.forEach { exam ->
                            val isLocked = subscription.isExamLockedForSwitch(exam.id)
                            MonochromeFilterPill(
                                label = if (isLocked) "🔒 ${exam.shortName}" else exam.shortName,
                                selected = examFilter == exam.id && !isLocked,
                                onClick = { onSelectExamFilter(exam.id) },
                                modifier = Modifier.testTag("subj_exam_filter_${exam.id}")
                            )
                        }
                    }

                    DifficultyFilterBar(
                        selected = difficultyFilter,
                        onSelect = onSelectDifficulty,
                        testTagPrefix = "cross_diff"
                    )

                    val examFilterName = examFilter?.let { id ->
                        state.availableExams.firstOrNull { it.id == id }?.shortName
                    }
                    Text(
                        text = if (examFilterName == null) {
                            "${visibleSubjects.size} syllabus subjects · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL} across ${state.availableExams.size} exams"
                        } else {
                            "${visibleSubjects.size} syllabus subjects · $examFilterName · ${PlatformCatalogDefaults.EVERGREEN_BANK_LABEL}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("subject_pool_summary")
                    )
                }
            }

            if (visibleSubjects.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No matching subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Try clearing your search or difficulty filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rows.size) { rowIdx ->
                    val rowItems = rows[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { (sub, list) ->
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
                                subtitleBadge = if (examFilter == null) "All Exams · ${sub.code}" else sub.code,
                                chapterCount = SyllabusCatalog.chapterCountForSubject(sub.code),
                                hideTotalCeiling = true,
                                primaryButtonLabel = "Chapters",
                                onPractice = { onOpenSubjectChapters(sub.code) },
                                onTimed = { onStartTimed(sub.code) },
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
    }
}

@Composable
private fun ExamScopedSubjectsView(
    state: DashboardDerivedState,
    activeExamId: String?,
    isNorcet: Boolean,
    isAdmin: Boolean,
    activeSection: SubjectHubSection,
    difficultyFilter: DifficultyFilter,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onSelectSection: (SubjectHubSection) -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onOpenSubjectChapters: (String) -> Unit,
    onStartTimedConfig: (String) -> Unit,
    onStartCbtCustom: (List<QuestionItem>, String) -> Unit,
    onOpenImporter: () -> Unit
) {
    val activeExam = state.activeExam
    val examShort = activeExam?.shortName ?: "Nursing"
    val isNorcetMains = isNorcet && state.activeExamStage == ExamStage.MAINS
    val isPureNursing = PlatformCatalogDefaults.examIsPureNursing(activeExamId)
    val supportsCaGk = PlatformCatalogDefaults.examSupportsCaGk(activeExamId)
    val supportsNonTech = PlatformCatalogDefaults.examSupportsNonTech(activeExamId)
    val nurPct = PlatformCatalogDefaults.nursingWeightagePercent(activeExamId)
    val nonPct = 100 - nurPct
    val markLabel = activeExam?.let { PlatformCatalogDefaults.formatMarkingLabel(it) } ?: "+1 / -0.33"

    val sections = when {
        isNorcetMains -> listOf(SubjectHubSection.NURSING)
        isPureNursing -> listOf(SubjectHubSection.NURSING, SubjectHubSection.PYQ)
        else -> buildList {
            add(SubjectHubSection.NURSING)
            add(SubjectHubSection.PYQ)
            if (supportsCaGk) add(SubjectHubSection.CA_GK)
            if (supportsNonTech) add(SubjectHubSection.NON_TECH)
        }
    }
    val effectiveSection = if (activeSection in sections) activeSection else SubjectHubSection.NURSING

    // Strictly uses stageQuestions so NORCET Prelims & Mains are NEVER mixed
    val stagePool = state.stageQuestions.ifEmpty { state.questions }
    val filteredPool = remember(stagePool, difficultyFilter) {
        difficultyFilter.difficulty?.let { d ->
            stagePool.filter { it.question.difficulty == d }
        } ?: stagePool
    }
    // Strictly keep Subject Practice separate from PYQs (PYQ tab) and Predicted Mocks (CBT tab)
    val practicePool = remember(filteredPool) {
        filteredPool.filter { !it.question.isPyqTagged && !it.question.isPredictedMock }
    }
    val pyqOnlyPool = remember(filteredPool) {
        filteredPool.filter { it.question.isPyqTagged }
    }
    val diffSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        if (sections.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { sec ->
                    val label = if (isNorcet && sec == SubjectHubSection.PYQ) {
                        "Prelims PYQs"
                    } else {
                        sec.labelFor(activeExamId)
                    }
                    MonochromeFilterPill(
                        label = label,
                        selected = sec == effectiveSection,
                        onClick = { onSelectSection(sec) },
                        modifier = Modifier.testTag("subject_tab_${sec.name}")
                    )
                }
            }
        }

        DifficultyFilterBar(
            selected = difficultyFilter,
            onSelect = onSelectDifficulty,
            testTagPrefix = "hub_diff",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth >= 840.dp -> 3
                maxWidth >= 560.dp -> 2
                else -> 1
            }

            when (effectiveSection) {
                SubjectHubSection.NURSING -> SubjectListGrid(
                    title = when {
                        isNorcetMains -> "NORCET Mains · Clinical Nursing"
                        isNorcet -> "NORCET Prelims · Nursing"
                        isPureNursing -> "$examShort Core Nursing"
                        else -> "$examShort Nursing Subjects"
                    },
                    subtitle = when {
                        isNorcetMains -> "100% Clinical scenarios & priority care · $markLabel"
                        isNorcet -> "80% Stage I Prelims nursing chapters · $markLabel"
                        isPureNursing -> "100% Core Nursing syllabus chapters · $markLabel"
                        else -> "INC syllabus chapters & clinical practice · $nurPct% weightage"
                    },
                    subjects = SubjectCatalog.NURSING_SUBJS,
                    allQuestions = practicePool,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    allFilter = { SubjectCatalog.isNursing(it.question) },
                    onStartPractice = onStartPractice,
                    onOpenSubjectChapters = onOpenSubjectChapters,
                    onStartTimedConfig = onStartTimedConfig
                )

                SubjectHubSection.PYQ -> PyqShiftList(
                    allQuestions = pyqOnlyPool,
                    activeExamShortName = examShort,
                    isNorcet = isNorcet,
                    isAdmin = isAdmin,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onStartCbtCustom = onStartCbtCustom,
                    onOpenImporter = onOpenImporter
                )

                SubjectHubSection.CA_GK -> CaAndFlashcardsList(
                    allQuestions = practicePool,
                    activeExamId = activeExamId,
                    isNorcet = isNorcet,
                    diffSuffix = diffSuffix,
                    flashcards = state.flashcards,
                    columns = columns,
                    onStartPractice = onStartPractice,
                    onOpenSubjectChapters = onOpenSubjectChapters,
                    onStartTimedConfig = onStartTimedConfig
                )

                SubjectHubSection.NON_TECH -> SubjectListGrid(
                    title = if (isNorcet) "Prelims Aptitude & Science" else "Science, Math & Reasoning",
                    subtitle = if (isNorcet) {
                        "20% Stage I Prelims non-nursing chapters"
                    } else {
                        "Syllabus chapters & drills · $nonPct% weightage"
                    },
                    subjects = SubjectCatalog.GEN_SUBJS,
                    allQuestions = practicePool,
                    diffSuffix = diffSuffix,
                    columns = columns,
                    allFilter = { SubjectCatalog.isGen(it.question) },
                    onStartPractice = onStartPractice,
                    onOpenSubjectChapters = onOpenSubjectChapters,
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
    onOpenSubjectChapters: (String) -> Unit,
    onStartTimedConfig: (String) -> Unit
) {
    val bySubject = remember(allQuestions) { allQuestions.groupBy { it.question.subject } }
    val combined = remember(allQuestions, subjects) { allQuestions.filter(allFilter) }
    val nonEmptySubjects = remember(bySubject, subjects) {
        subjects.filter { sub -> !bySubject[sub.code].isNullOrEmpty() }
    }
    val rows = remember(nonEmptySubjects, columns) { nonEmptySubjects.chunked(columns) }

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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        onStartPractice(combined.shuffled().take(100), "$title · Random 100$diffSuffix")
                    },
                    enabled = combined.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("practice_all_${subjects.firstOrNull()?.code}")
                ) {
                    Text("Random 100", fontWeight = FontWeight.Bold)
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
                    val list = bySubject[sub.code].orEmpty()
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
                        chapterCount = SyllabusCatalog.chapterCountForSubject(sub.code),
                        hideTotalCeiling = true,
                        primaryButtonLabel = "Chapters",
                        onPractice = { onOpenSubjectChapters(sub.code) },
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
    activeExamShortName: String,
    isNorcet: Boolean,
    isAdmin: Boolean,
    diffSuffix: String,
    columns: Int,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onStartCbtCustom: (List<QuestionItem>, String) -> Unit,
    onOpenImporter: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val byShift = remember(allQuestions) {
        allQuestions
            .filter { it.question.shiftYear.isNotBlank() }
            .groupBy { it.question.shiftYear }
    }
    val shifts = remember(byShift) { byShift.keys.sortedDescending() }
    val allPyqs = remember(allQuestions) {
        allQuestions.filter { it.question.isPyqTagged }
    }
    val rows = remember(shifts, columns) { shifts.chunked(columns) }

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
                    text = if (isNorcet) {
                        "NORCET Stage I Prelims Papers"
                    } else {
                        "$activeExamShortName Previous Year Papers"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onStartPractice(allPyqs.shuffled().take(100), "Previous Year Papers · Random 100$diffSuffix")
                        },
                        enabled = allPyqs.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("practice_all_pyqs")
                    ) {
                        Text("Random 100", fontWeight = FontWeight.Bold)
                    }
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onOpenImporter,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("pyq_add_pdf_btn")
                        ) {
                            Text("+ Import")
                        }
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
                    val list = byShift[sh].orEmpty()
                    val solved = list.count { it.attemptsCount > 0 }
                    val allVerified = list.isNotEmpty() && list.all { it.question.isVerifiedSource }
                    val shiftBadgeText = if (allVerified) "Official PYQ" else "Memory-Based"
                    val title = SubjectCatalog.formatPaperTitle(sh)
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
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = editorial.elevatedSurface,
                                        border = BorderStroke(1.dp, editorial.borderSubtle)
                                    ) {
                                        Text(
                                            text = sh,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (allVerified) editorial.correctBg else editorial.elevatedSurface,
                                        modifier = Modifier.testTag("shift_badge_$sh")
                                    ) {
                                        Text(
                                            text = shiftBadgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (allVerified) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$solved/${list.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onStartPractice(list, "$title$diffSuffix") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Practice", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onStartCbtCustom(list, "$sh CBT$diffSuffix") },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, editorial.borderSubtle),
                                    modifier = Modifier.heightIn(min = 44.dp)
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
    activeExamId: String?,
    isNorcet: Boolean,
    diffSuffix: String,
    flashcards: List<Flashcard>,
    columns: Int,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onOpenSubjectChapters: (String) -> Unit,
    onStartTimedConfig: (String) -> Unit
) {
    val bySubject = remember(allQuestions) { allQuestions.groupBy { it.question.subject } }
    val allCa = remember(allQuestions) { allQuestions.filter { SubjectCatalog.isCa(it.question) } }
    val tags = remember(flashcards) { listOf("All") + flashcards.map { it.tag }.distinct() }
    var selectedTag by remember { mutableStateOf("All") }
    val visibleCards = remember(flashcards, selectedTag) {
        if (selectedTag == "All") flashcards else flashcards.filter { it.tag == selectedTag }
    }
    val visibleCaSubjs = remember(activeExamId) {
        SubjectCatalog.CA_SUBJS.filter { PlatformCatalogDefaults.isSharedSubjectForExam(activeExamId, it.code) }
    }
    val caRows = remember(visibleCaSubjs, columns) { visibleCaSubjs.chunked(columns) }
    val cardRows = remember(visibleCards, columns) { visibleCards.chunked(columns) }

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
                        text = if (isNorcet) "GK & Health Programs" else "Current Affairs & GK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        onStartPractice(allCa.shuffled().take(100), "Current Affairs & GK · Random 100$diffSuffix")
                    },
                    enabled = allCa.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Random 100", fontWeight = FontWeight.Bold)
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
                    val list = bySubject[sub.code].orEmpty()
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
                        chapterCount = SyllabusCatalog.chapterCountForSubject(sub.code),
                        hideTotalCeiling = true,
                        primaryButtonLabel = "Chapters",
                        onPractice = { onOpenSubjectChapters(sub.code) },
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
                    "Fact Cards (${flashcards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { t ->
                        MonochromeFilterPill(
                            label = t,
                            selected = selectedTag == t,
                            onClick = { selectedTag = t }
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
